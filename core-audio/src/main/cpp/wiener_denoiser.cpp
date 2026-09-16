#include "wiener_denoiser.h"

#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>

extern "C" {
#include "fftwrap.h"
}

namespace openhearing {
namespace {
int checked_hop(int rate, int suppression) {
    if ((rate != 8000 && rate != 16000 && rate != 24000 && rate != 32000 && rate != 44100 && rate != 48000) ||
        suppression < 0 || suppression > 18) {
        throw std::invalid_argument("Unsupported Wiener processor configuration");
    }
    return rate / 100;
}
constexpr float kPi = 3.14159265358979323846f;
constexpr float kEpsilon = 1e-20f;
constexpr int kMinimumWindowFrames = 50;
}

WienerDenoiser::WienerDenoiser(int rate, int suppression_db)
    : hop_(checked_hop(rate, suppression_db)), floor_(std::pow(10.0f, -suppression_db / 20.0f)),
      fft_(spx_fft_init(2 * hop_), spx_fft_destroy), window_(2 * hop_), history_(hop_), overlap_(hop_),
      time_(2 * hop_), spectrum_(2 * hop_), power_(hop_ + 1), smooth_(hop_ + 1),
      minimum_(hop_ + 1, std::numeric_limits<float>::max()),
      previous_minimum_(hop_ + 1, std::numeric_limits<float>::max()), noise_(hop_ + 1),
      posterior_(hop_ + 1), gain_(hop_ + 1, 1.0f), target_(hop_ + 1) {
    if (!fft_) throw std::bad_alloc();
    // Sine analysis/synthesis windows have squared overlap sum exactly one.
    for (int i = 0; i < 2 * hop_; ++i) window_[i] = std::sin(kPi * (i + 0.5f) / (2 * hop_));
}

void WienerDenoiser::estimate_gains() {
    // Rolling minima of smoothed power span 0.5–1 second. A conservative
    // startup estimate avoids assuming that the user starts in silence.
    for (int k = 0; k <= hop_; ++k) {
        smooth_[k] = frames_ == 0 ? power_[k] : 0.8f * smooth_[k] + 0.2f * power_[k];
        minimum_[k] = std::min(minimum_[k], smooth_[k]);
        const float estimate = 1.5f * std::min(minimum_[k], previous_minimum_[k]);
        noise_[k] = frames_ == 0 ? 0.1f * power_[k] : 0.95f * noise_[k] + 0.05f * estimate;
        const float posterior = std::min(power_[k] / std::max(noise_[k], kEpsilon), 1000.0f);
        const float prior = 0.95f * gain_[k] * gain_[k] * posterior_[k] +
                            0.05f * std::max(posterior - 1.0f, 0.0f);
        target_[k] = std::clamp(prior / (1.0f + prior), floor_, 1.0f);
        posterior_[k] = posterior;
    }
    for (int k = 0; k <= hop_; ++k) {
        // Smooth across neighboring bins and time to reduce musical noise.
        const float target = 0.25f * target_[std::max(0, k - 1)] + 0.5f * target_[k] +
                             0.25f * target_[std::min(hop_, k + 1)];
        const float retention = target > gain_[k] ? 0.6f : 0.9f;
        gain_[k] = retention * gain_[k] + (1.0f - retention) * target;
    }
    if (++frames_ == kMinimumWindowFrames) {
        previous_minimum_ = minimum_;
        std::fill(minimum_.begin(), minimum_.end(), std::numeric_limits<float>::max());
        // Keep the counter bounded without re-entering the startup branch.
        frames_ = 1;
    }
}

void WienerDenoiser::process(float* samples, int count) {
    if (!samples || count != hop_) throw std::invalid_argument("Invalid Wiener frame");
    for (int i = 0; i < hop_; ++i) {
        const float input = std::isfinite(samples[i]) ? std::clamp(samples[i], -1.0f, 1.0f) : 0.0f;
        time_[i] = history_[i] * window_[i];
        time_[i + hop_] = input * window_[i + hop_];
        history_[i] = input;
    }
    spx_fft(fft_.get(), time_.data(), spectrum_.data());
    // SMALLFT packs DC, then real/imag pairs, then the real Nyquist bin.
    power_[0] = spectrum_[0] * spectrum_[0];
    power_[hop_] = spectrum_.back() * spectrum_.back();
    for (int k = 1; k < hop_; ++k) {
        power_[k] = spectrum_[2 * k - 1] * spectrum_[2 * k - 1] + spectrum_[2 * k] * spectrum_[2 * k];
    }
    estimate_gains();
    spectrum_[0] *= gain_[0];
    spectrum_.back() *= gain_[hop_];
    for (int k = 1; k < hop_; ++k) {
        spectrum_[2 * k - 1] *= gain_[k];
        spectrum_[2 * k] *= gain_[k];
    }
    spx_ifft(fft_.get(), spectrum_.data(), time_.data());
    for (int i = 0; i < hop_; ++i) {
        samples[i] = overlap_[i] + time_[i] * window_[i];
        overlap_[i] = time_[i + hop_] * window_[i + hop_];
    }
}
} // namespace openhearing
