#pragma once

#include <array>
#include <memory>
#include "rnnoise.h"
#include "speex/speex_resampler.h"
#include "voice_leveler.h"

namespace earcast {

// One audio-worker-owned stream. All allocation happens during construction.
// The caller must retain the existing hearing-assist output limiter downstream.
class RnnoiseFilter {
public:
    RnnoiseFilter(int sample_rate, int suppression_db, float maximum_gain_db = 0);
    void process(float* samples, int count);
    int frame_size() const { return frame_size_; }
    int delay_samples() const { return delay_samples_; }

private:
    using RnnPtr = std::unique_ptr<DenoiseState, decltype(&rnnoise_destroy)>;
    using ResamplerPtr = std::unique_ptr<SpeexResamplerState, decltype(&speex_resampler_destroy)>;
    static ResamplerPtr make_resampler(int input_rate, int output_rate);
    static void resample(SpeexResamplerState* state, const float* input, int input_count,
                         float* output, int output_count);

    static constexpr int kModelRate = 48000;
    static constexpr int kModelFrame = 480;
    int frame_size_;
    float dry_mix_;
    int delay_samples_;
    VoiceLeveler leveler_;
    RnnPtr rnn_{nullptr, rnnoise_destroy};
    ResamplerPtr up_{nullptr, speex_resampler_destroy};
    ResamplerPtr down_{nullptr, speex_resampler_destroy};
    std::array<float, kModelFrame> model_input_{};
    std::array<float, kModelFrame> model_output_{};
};

}  // namespace earcast
