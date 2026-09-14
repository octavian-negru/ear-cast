#include "neural_denoiser.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <iostream>
#include <limits>
#include <memory>
#include <stdexcept>
#include <vector>

namespace {
constexpr double kPi = 3.14159265358979323846;

void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}

using RnnPtr = std::unique_ptr<DenoiseState, decltype(&rnnoise_destroy)>;
RnnPtr model() {
    RnnPtr state(static_cast<DenoiseState*>(std::calloc(1, rnnoise_get_size())), rnnoise_destroy);
    require(state != nullptr, "Model allocation failed");
    require(rnnoise_init(state.get(), nullptr) == 0, "Bundled model initialization failed");
    return state;
}

// This would fail if dry audio were mixed from a different frame or filter path.
void aligned_mix() {
    auto wet = model();
    auto dry = model();
    auto mixed = model();
    const int size = rnnoise_get_frame_size();
    std::vector<float> input(size), wet_out(size), dry_out(size), mix_out(size);
    for (int frame = 0; frame < 100; ++frame) {
        for (int i = 0; i < size; ++i) {
            const int t = frame * size + i;
            input[i] = static_cast<float>(300 * std::sin(2 * kPi * 997 * t / 48000));
            if (t % 2039 == 0) input[i] += 2000;
        }
        rnnoise_process_frame(wet.get(), wet_out.data(), input.data());
        rnnoise_process_frame_with_dry_mix(dry.get(), dry_out.data(), input.data(), 1.0f);
        rnnoise_process_frame_with_dry_mix(mixed.get(), mix_out.data(), input.data(), 0.5f);
        for (int i = 0; i < size; ++i) {
            const float expected = 0.5f * (wet_out[i] + dry_out[i]);
            require(std::abs(mix_out[i] - expected) < 0.02f, "Dry/wet paths are not aligned");
        }
    }
}

void rates_and_weak_input() {
    for (int rate : {8000, 16000, 24000, 32000, 44100, 48000}) {
        // Internal 0 dB setting is a delayed dry reference for adapter tests.
        openhearing::NeuralDenoiser reference(rate, 0);
        std::vector<float> frame(reference.frame_size());
        double energy = 0;
        int measured = 0;
        for (int block = 0; block < 100; ++block) {
            for (int i = 0; i < reference.frame_size(); ++i) {
                const int t = block * reference.frame_size() + i;
                frame[i] = static_cast<float>(1e-5 * std::sin(2 * kPi * 997 * t / rate));
            }
            reference.process(frame.data(), static_cast<int>(frame.size()));
            if (block > 50) {
                for (float value : frame) energy += value * value;
                measured += static_cast<int>(frame.size());
            }
        }
        const double rms = std::sqrt(energy / measured);
        require(rms > 6e-6 && rms < 8e-6, "Resampling lost or amplified weak input");

        for (int strength : {6, 12}) {
            openhearing::NeuralDenoiser processor(rate, strength);
            for (int block = 0; block < 30; ++block) {
                std::fill(frame.begin(), frame.end(), 0.0f);
                processor.process(frame.data(), static_cast<int>(frame.size()));
                for (float value : frame) require(value == 0.0f, "Silence produced output");
            }
            frame[0] = std::numeric_limits<float>::quiet_NaN();
            frame[1] = std::numeric_limits<float>::infinity();
            frame[2] = -std::numeric_limits<float>::infinity();
            frame[3] = 1.0f;
            for (int block = 0; block < 30; ++block) {
                processor.process(frame.data(), static_cast<int>(frame.size()));
                for (float value : frame) require(std::isfinite(value), "Non-finite output");
                std::fill(frame.begin(), frame.end(), 0.0f);
            }
            bool rejected = false;
            try {
                processor.process(frame.data(), static_cast<int>(frame.size()) - 1);
            } catch (const std::invalid_argument&) {
                rejected = true;
            }
            require(rejected, "Wrong frame size accepted");
        }
    }
}

void declared_delay_matches_impulse() {
    for (int rate : {8000, 16000, 24000, 32000, 44100, 48000}) {
        openhearing::NeuralDenoiser processor(rate, 0);
        const int size = processor.frame_size();
        const int impulse = 2 * size;
        std::vector<float> frame(size);
        float maximum = 0;
        int maximum_index = -1;
        for (int block = 0; block < 30; ++block) {
            std::fill(frame.begin(), frame.end(), 0.0f);
            if (block == 2) frame[0] = 0.01f;
            processor.process(frame.data(), size);
            for (int i = 0; i < size; ++i) {
                if (std::abs(frame[i]) > maximum) {
                    maximum = std::abs(frame[i]);
                    maximum_index = block * size + i;
                }
            }
        }
        require(maximum > 0.001f, "Dry impulse disappeared");
        require(std::abs(maximum_index - impulse - processor.delay_samples()) <= 1,
                "Declared model/resampler delay is incorrect");
    }
}
}

int main() {
    try {
        aligned_mix();
        rates_and_weak_input();
        declared_delay_matches_impulse();
        std::cout << "Neural speech adapter checks passed\n";
        return 0;
    } catch (const std::exception& e) {
        std::cerr << e.what() << '\n';
        return 1;
    }
}
