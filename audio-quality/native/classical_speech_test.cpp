#include "wiener_denoiser.h"
#include "speex/speex_preprocess.h"

#include <algorithm>
#include <cmath>
#include <iostream>
#include <limits>
#include <memory>
#include <random>
#include <stdexcept>
#include <vector>

namespace {
constexpr double kPi = 3.14159265358979323846;
void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}

void reconstruction(int rate) {
    openhearing::WienerDenoiser processor(rate, 0);
    const int hop = processor.frame_size();
    std::vector<float> frame(hop), previous(hop);
    for (int block = 0; block < 120; ++block) {
        std::vector<float> input(hop);
        for (int i = 0; i < hop; ++i) {
            const int t = block * hop + i;
            input[i] = 0.2f * std::sin(2 * kPi * 997 * t / rate);
            if (t % 547 == 0) input[i] += 0.5f;
        }
        frame = input;
        processor.process(frame.data(), hop);
        for (int i = 0; i < hop; ++i) {
            require(std::abs(frame[i] - previous[i]) < 2e-6f, "Wiener overlap reconstruction or delay mismatch");
        }
        previous = input;
    }
    require(processor.delay_samples() == hop, "Incorrect declared delay");
}

double noise_attenuation(int rate, int strength) {
    openhearing::WienerDenoiser processor(rate, strength);
    std::vector<float> frame(processor.frame_size());
    std::mt19937 random(42);
    std::uniform_real_distribution<float> noise(-0.03f, 0.03f);
    double input_energy = 0, output_energy = 0;
    for (int block = 0; block < 400; ++block) {
        for (float& x : frame) x = noise(random);
        if (block > 200) for (float x : frame) input_energy += x * x;
        processor.process(frame.data(), static_cast<int>(frame.size()));
        if (block > 200) for (float x : frame) output_energy += x * x;
    }
    const double db = 10 * std::log10(output_energy / input_energy);
    require(db < -2.0, "Steady noise was not suppressed");
    require(db > -strength - 0.3, "Suppression exceeded configured floor");
    // A newly arriving voiced component should survive the learned noise floor.
    double sine = 0, cosine = 0;
    int measured = 0;
    for (int block = 0; block < 30; ++block) {
        for (int i = 0; i < static_cast<int>(frame.size()); ++i) {
            const int t = block * static_cast<int>(frame.size()) + i;
            frame[i] = noise(random) + 0.1f * std::sin(2 * kPi * 997 * t / rate);
        }
        processor.process(frame.data(), static_cast<int>(frame.size()));
        if (block > 10) for (int i = 0; i < static_cast<int>(frame.size()); ++i) {
            const int t = (block - 1) * static_cast<int>(frame.size()) + i;
            sine += frame[i] * std::sin(2 * kPi * 997 * t / rate);
            cosine += frame[i] * std::cos(2 * kPi * 997 * t / rate);
            ++measured;
        }
    }
    require(2 * std::hypot(sine, cosine) / measured > 0.07, "New voiced component was muffled");
    return db;
}

void invalid_and_silent_input(int rate) {
    openhearing::WienerDenoiser processor(rate, 12);
    std::vector<float> frame(processor.frame_size());
    for (int block = 0; block < 100; ++block) {
        processor.process(frame.data(), static_cast<int>(frame.size()));
        for (float x : frame) require(x == 0, "Silence produced sound");
    }
    frame[0] = std::numeric_limits<float>::quiet_NaN();
    frame[1] = std::numeric_limits<float>::infinity();
    frame[2] = std::numeric_limits<float>::max();
    for (int block = 0; block < 20; ++block) {
        processor.process(frame.data(), static_cast<int>(frame.size()));
        for (float x : frame) require(std::isfinite(x), "Non-finite output");
        std::fill(frame.begin(), frame.end(), 0);
    }
    bool rejected = false;
    try { processor.process(frame.data(), static_cast<int>(frame.size()) - 1); }
    catch (const std::invalid_argument&) { rejected = true; }
    require(rejected, "Invalid frame accepted");
}

void speex_rates_and_delay(int rate) {
    const int hop = rate / 100;
    using State = std::unique_ptr<SpeexPreprocessState, decltype(&speex_preprocess_state_destroy)>;
    State state(speex_preprocess_state_init(hop, rate), speex_preprocess_state_destroy);
    require(state != nullptr, "Speex initialization failed");
    int enabled = 1, disabled = 0, suppression = 0;
    speex_preprocess_ctl(state.get(), SPEEX_PREPROCESS_SET_DENOISE, &enabled);
    speex_preprocess_ctl(state.get(), SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &suppression);
    speex_preprocess_ctl(state.get(), SPEEX_PREPROCESS_SET_AGC, &disabled);
    speex_preprocess_ctl(state.get(), SPEEX_PREPROCESS_SET_DEREVERB, &disabled);
    std::vector<spx_int16_t> frame(hop);
    int peak = -1, maximum = 0;
    for (int block = 0; block < 5; ++block) {
        std::fill(frame.begin(), frame.end(), 0);
        if (block == 0) frame[hop / 2] = 12000;
        speex_preprocess_run(state.get(), frame.data());
        for (int i = 0; i < hop; ++i) {
            if (std::abs(frame[i]) > maximum) {
                maximum = std::abs(frame[i]);
                peak = block * hop + i;
            }
        }
    }
    require(maximum > 10000 && peak == hop + hop / 2, "Speex delay or level mismatch");
}
}

int main() {
    try {
        for (int rate : {8000, 16000, 24000, 32000, 44100, 48000}) {
            reconstruction(rate);
            invalid_and_silent_input(rate);
            speex_rates_and_delay(rate);
            const double gentle = noise_attenuation(rate, 6);
            const double strong = noise_attenuation(rate, 12);
            require(strong < gentle - 1, "Strength controls do not differ");
            std::cout << rate << " Hz: gentle " << gentle << " dB, strong " << strong << " dB\n";
        }
        bool rejected = false;
        try { openhearing::WienerDenoiser invalid(0, 12); }
        catch (const std::invalid_argument&) { rejected = true; }
        require(rejected, "Invalid sample rate accepted");
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
