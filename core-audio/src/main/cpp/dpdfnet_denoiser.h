#pragma once

#include <array>
#include <memory>
#include <vector>
#include "speex/speex_resampler.h"
#include "vendor/sherpa-onnx/c-api.h"

namespace openhearing {

// Complete upstream streaming STFT + recurrent model + deep filtering + synthesis.
// One instance per audio worker. Never cascade with RNNoise.
class DpdfnetDenoiser {
public:
    DpdfnetDenoiser(int rate, int model_rate, int suppression_db, const char* model_path);
    void process(float* samples, int count);
    int frame_size() const { return frame_size_; }
    int delay_samples() const { return delay_samples_; }

private:
    using ModelPtr = std::unique_ptr<const SherpaOnnxOnlineSpeechDenoiser,
        decltype(&SherpaOnnxDestroyOnlineSpeechDenoiser)>;
    using ResamplerPtr = std::unique_ptr<SpeexResamplerState, decltype(&speex_resampler_destroy)>;
    static ResamplerPtr make_resampler(int from, int to);
    static void resample(SpeexResamplerState* state, const float* input, int input_size,
                         float* output, int output_size);
    int frame_size_;
    int model_rate_;
    int model_frame_;
    int delay_samples_;
    float dry_mix_;
    bool started_ = false;
    int dry_position_ = 0;
    ModelPtr model_{nullptr, SherpaOnnxDestroyOnlineSpeechDenoiser};
    ResamplerPtr up_{nullptr, speex_resampler_destroy};
    ResamplerPtr down_{nullptr, speex_resampler_destroy};
    std::vector<float> input_;
    std::vector<float> output_;
    // Mask delay (2) + deep-filter centre (2) + analysis/synthesis (1), at model rate.
    std::vector<float> dry_delay_;
};
}  // namespace openhearing
