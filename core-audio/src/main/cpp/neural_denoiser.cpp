#include "neural_denoiser.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <new>
#include <stdexcept>

namespace openhearing {

NeuralDenoiser::NeuralDenoiser(int sample_rate, int suppression_db)
    : frame_size_(sample_rate / 100), dry_mix_(1.0f) {
    if ((sample_rate != 8000 && sample_rate != 16000 && sample_rate != kModelRate) ||
        suppression_db < 0 || suppression_db > 18) {
        throw std::invalid_argument("Unsupported neural speech processor configuration");
    }
    dry_mix_ = std::pow(10.0f, -suppression_db / 20.0f);
    if (rnnoise_get_frame_size() != kModelFrame) {
        throw std::runtime_error("RNNoise model frame size does not match the adapter");
    }
    // Allocate explicitly: upstream rnnoise_create does not check malloc before init.
    auto* state = static_cast<DenoiseState*>(std::calloc(1, rnnoise_get_size()));
    if (!state) throw std::bad_alloc();
    rnn_.reset(state);
    if (rnnoise_init(state, nullptr) != 0) {
        throw std::runtime_error("Could not initialize the bundled RNNoise model");
    }
    if (sample_rate != kModelRate) {
        up_ = make_resampler(sample_rate, kModelRate);
        down_ = make_resampler(kModelRate, sample_rate);
    }
}

NeuralDenoiser::ResamplerPtr NeuralDenoiser::make_resampler(int input_rate, int output_rate) {
    int error = RESAMPLER_ERR_SUCCESS;
    ResamplerPtr state(speex_resampler_init(1, input_rate, output_rate, 10, &error), speex_resampler_destroy);
    if (!state || error != RESAMPLER_ERR_SUCCESS) {
        throw std::runtime_error("Could not initialize the speech resampler");
    }
    // Keep initial filter delay. skip_zeros would produce short startup frames.
    return state;
}

void NeuralDenoiser::resample(SpeexResamplerState* state, const float* input, int input_count,
                             float* output, int output_count) {
    auto consumed = static_cast<spx_uint32_t>(input_count);
    auto produced = static_cast<spx_uint32_t>(output_count);
    const int error = speex_resampler_process_float(state, 0, input, &consumed, output, &produced);
    // All supported rates have an exact integer ratio and use continuous 10 ms frames.
    if (error != RESAMPLER_ERR_SUCCESS || consumed != static_cast<spx_uint32_t>(input_count) ||
        produced != static_cast<spx_uint32_t>(output_count)) {
        throw std::runtime_error("Speech resampler returned an incomplete frame");
    }
}

void NeuralDenoiser::process(float* samples, int count) {
    if (!samples || count != frame_size_) throw std::invalid_argument("Invalid neural speech frame");
    for (int i = 0; i < count; ++i) {
        samples[i] = std::isfinite(samples[i]) ? std::clamp(samples[i], -1.0f, 1.0f) : 0.0f;
    }
    if (up_) {
        resample(up_.get(), samples, count, model_input_.data(), kModelFrame);
    } else {
        std::copy_n(samples, count, model_input_.begin());
    }
    // RNNoise uses float samples in PCM16 amplitude units, NOT normalized [-1, 1].
    // Do not quantize to int16: keep weak captured detail in floating point.
    for (float& sample : model_input_) sample *= 32768.0f;
    // Blend in the SAME delayed spectrum before synthesis (see vendor patch).
    // A VAD score never switches off the audio or decides which speaker is audible.
    rnnoise_process_frame_with_dry_mix(rnn_.get(), model_output_.data(), model_input_.data(), dry_mix_);
    for (float& sample : model_output_) sample /= 32768.0f;
    if (down_) {
        resample(down_.get(), model_output_.data(), kModelFrame, samples, count);
    } else {
        std::copy_n(model_output_.begin(), count, samples);
    }
    for (int i = 0; i < count; ++i) {
        if (!std::isfinite(samples[i])) throw std::runtime_error("Neural speech output is not finite");
    }
}

}  // namespace openhearing
