#include "rnnoise_filter.h"
#include "wiener_filter.h"
#ifdef EARCAST_HAS_DPDFNET
#include "dpdfnet_filter.h"
#endif
#include "speex/speex_preprocess.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <iostream>
#include <memory>
#include <limits>
#include <stdexcept>
#include <string>
#include <vector>

// Offline only. Raw files use IEEE float32 little endian, mono. No audio device,
// compiler launch, automatic download, or second implementation of the neural DSP.
static_assert(sizeof(float) == 4 && std::numeric_limits<float>::is_iec559,
              "The workbench requires IEEE float32");
int main(int argc, char** argv) {
    try {
        if (argc != 7 && argc != 8) throw std::invalid_argument(
            "Usage: render_audio off|speex|wiener|rnnoise|dpdfnet rate suppression_db boost_db input.f32 output.f32 [model.onnx]");
        const std::string mode(argv[1]);
        const int rate = std::stoi(argv[2]);
        const int suppression = std::stoi(argv[3]);
        const float boost = std::stof(argv[4]);
        if (rate != 8000 && rate != 16000 && rate != 24000 && rate != 32000 && rate != 44100 && rate != 48000)
            throw std::invalid_argument("Unsupported PCM rate");
        if (suppression < 0 || suppression > 18 || !std::isfinite(boost) || boost < 0 || boost > 12)
            throw std::invalid_argument("Invalid enhancement strength");
        if (mode != "off" && mode != "speex" && mode != "rnnoise" && mode != "dpdfnet" && mode != "wiener")
            throw std::invalid_argument("Unknown backend");
        if ((mode == "dpdfnet") != (argc == 8)) throw std::invalid_argument("Only DPDFNet requires a model path");
        if (mode != "rnnoise" && boost != 0) throw std::invalid_argument("Boost requires RNNoise confidence");
        if (std::ifstream(argv[6]).good()) throw std::invalid_argument("Output already exists");
        std::ifstream input(argv[5], std::ios::binary | std::ios::ate);
        if (!input) throw std::runtime_error("Cannot open input");
        const auto length = input.tellg();
        if (length <= 0 || static_cast<std::uint64_t>(length) % 4) throw std::runtime_error("Invalid float PCM length");
        const auto total_samples = static_cast<std::uint64_t>(length) / 4;
        input.seekg(0);
        std::ofstream output(argv[6], std::ios::binary);
        if (!output) throw std::runtime_error("Cannot create output");
        const int frames = rate / 100;
        int delay = 0;
        std::unique_ptr<earcast::RnnoiseFilter> neural;
        std::unique_ptr<earcast::WienerFilter> wiener;
#ifdef EARCAST_HAS_DPDFNET
        std::unique_ptr<earcast::DpdfnetFilter> detailed;
#endif
        using SpeexPtr = std::unique_ptr<SpeexPreprocessState, decltype(&speex_preprocess_state_destroy)>;
        SpeexPtr speex(nullptr, speex_preprocess_state_destroy);
        if (mode == "dpdfnet") {
#ifdef EARCAST_HAS_DPDFNET
            const int model_rate = rate <= 16000 ? rate : 48000;
            detailed = std::make_unique<earcast::DpdfnetFilter>(rate, model_rate, suppression, argv[7]);
            delay = detailed->delay_samples();
#else
            throw std::runtime_error("Build the renderer with EARCAST_SHERPA_RUNTIME_DIR to enable DPDFNet");
#endif
        } else if (mode == "rnnoise") {
            neural = std::make_unique<earcast::RnnoiseFilter>(rate, suppression, boost);
            delay = neural->delay_samples();
        } else if (mode == "wiener") {
            wiener = std::make_unique<earcast::WienerFilter>(rate, suppression);
            delay = wiener->delay_samples();
        } else if (mode == "speex") {
            speex.reset(speex_preprocess_state_init(frames, rate));
            if (!speex) throw std::runtime_error("Cannot create Speex baseline");
            int enabled = 1, disabled = 0, floor = -suppression;
            speex_preprocess_ctl(speex.get(), SPEEX_PREPROCESS_SET_DENOISE, &enabled);
            speex_preprocess_ctl(speex.get(), SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &floor);
            speex_preprocess_ctl(speex.get(), SPEEX_PREPROCESS_SET_AGC, &disabled);
            speex_preprocess_ctl(speex.get(), SPEEX_PREPROCESS_SET_DEREVERB, &disabled);
            delay = frames; // This pinned floating-point Speex version uses ps_size == frame_size.
        }
        const auto blocks = (total_samples + frames - 1) / frames + (delay + frames - 1) / frames;
        std::vector<float> frame(frames);
        std::vector<spx_int16_t> pcm(frames);
        std::uint64_t elapsed_ns = 0, maximum_ns = 0;
        for (std::uint64_t block = 0; block < blocks; ++block) {
            std::fill(frame.begin(), frame.end(), 0.0f);
            for (int i = 0; i < frames && block * frames + i < total_samples; ++i) {
                unsigned char bytes[4];
                if (!input.read(reinterpret_cast<char*>(bytes), 4)) throw std::runtime_error("Truncated input");
                std::uint32_t bits = bytes[0] | (std::uint32_t(bytes[1]) << 8) |
                                     (std::uint32_t(bytes[2]) << 16) | (std::uint32_t(bytes[3]) << 24);
                std::memcpy(&frame[i], &bits, 4);
                if (!std::isfinite(frame[i]) || std::abs(frame[i]) > 1.0f)
                    throw std::runtime_error("Input must be finite normalized PCM");
            }
            const auto started = std::chrono::steady_clock::now();
            if (neural) neural->process(frame.data(), frames);
            if (wiener) wiener->process(frame.data(), frames);
#ifdef EARCAST_HAS_DPDFNET
            if (detailed) detailed->process(frame.data(), frames);
#endif
            if (speex) {
                for (int i = 0; i < frames; ++i)
                    pcm[i] = static_cast<spx_int16_t>(std::clamp(frame[i] * 32768.0f, -32768.0f, 32767.0f));
                speex_preprocess_run(speex.get(), pcm.data());
                for (int i = 0; i < frames; ++i) frame[i] = pcm[i] / 32768.0f;
            }
            const auto ns = static_cast<std::uint64_t>(std::chrono::duration_cast<std::chrono::nanoseconds>(
                std::chrono::steady_clock::now() - started).count());
            elapsed_ns += ns;
            maximum_ns = std::max(maximum_ns, ns);
            for (float sample : frame) {
                if (!std::isfinite(sample)) throw std::runtime_error("Non-finite output");
                std::uint32_t bits;
                std::memcpy(&bits, &sample, 4);
                const unsigned char bytes[] = {static_cast<unsigned char>(bits), static_cast<unsigned char>(bits >> 8),
                    static_cast<unsigned char>(bits >> 16), static_cast<unsigned char>(bits >> 24)};
                output.write(reinterpret_cast<const char*>(bytes), 4);
            }
        }
        output.flush();
        if (!output) throw std::runtime_error("Output write failed");
        std::cout << "{\"delay_samples\":" << delay << ",\"input_frames\":" << total_samples
                  << ",\"output_frames\":" << blocks * frames << ",\"mean_processing_ns\":" << elapsed_ns / blocks
                  << ",\"max_processing_ns\":" << maximum_ns << "}\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
