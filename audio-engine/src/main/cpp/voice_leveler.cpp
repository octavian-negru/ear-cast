#include "voice_leveler.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>

namespace earcast {

VoiceLeveler::VoiceLeveler(float maximum_gain_db) : maximum_gain_(1.0f) {
    if (!std::isfinite(maximum_gain_db) || maximum_gain_db < 0 || maximum_gain_db > 12) {
        throw std::invalid_argument("Quiet speech gain must be between 0 and 12 dB");
    }
    maximum_gain_ = std::pow(10.0f, maximum_gain_db / 20.0f);
}

void VoiceLeveler::process(float* samples, int count, float speech_probability) {
    if (!samples || count != 480) throw std::invalid_argument("Leveler requires a 480-sample model frame");
    if (maximum_gain_ == 1.0f) return;
    double energy = 0;
    float peak = 0;
    for (int i = 0; i < count; ++i) {
        energy += static_cast<double>(samples[i]) * samples[i];
        peak = std::max(peak, std::abs(samples[i]));
    }
    const float rms = static_cast<float>(std::sqrt(energy / count));
    float target = 1.0f;
    // -75 dBFS is an anti-runaway floor, not a noise gate. The output always passes.
    // Smooth confidence weighting avoids an audible binary VAD decision.
    if (std::isfinite(speech_probability) && rms > 0.00017783f) {
        const float confidence = std::clamp((speech_probability - 0.5f) / 0.4f, 0.0f, 1.0f);
        const float desired = std::clamp(0.03162278f / rms, 1.0f, maximum_gain_); // -30 dBFS target
        target += confidence * (desired - 1.0f);
    }
    // Anticipate the frame's peak before raising any sample; final limiters stay downstream.
    const float peak_limit = peak > 0 ? std::max(1.0f, 0.8f / peak) : maximum_gain_;
    target = std::min(target, peak_limit);
    gain_ = std::min(gain_, peak_limit);
    // Fixed model rate: increase over 500 ms, relax over 100 ms. No sample-loop exp/pow.
    const float coefficient = target > gain_ ? 0.9999583342f : 0.9997916884f;
    for (int i = 0; i < count; ++i) {
        gain_ = coefficient * gain_ + (1.0f - coefficient) * target;
        samples[i] *= gain_;
    }
}

}  // namespace earcast
