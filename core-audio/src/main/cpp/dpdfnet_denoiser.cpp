#include "dpdfnet_denoiser.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>

namespace openhearing {
DpdfnetDenoiser::DpdfnetDenoiser(int rate, int model_rate, int suppression_db, const char* model_path)
    : frame_size_(rate / 100), model_rate_(model_rate), model_frame_(model_rate / 100),
      delay_samples_(5 * frame_size_), dry_mix_(1.0f) {
    if ((rate != 8000 && rate != 16000 && rate != 24000 && rate != 32000 && rate != 44100 && rate != 48000) ||
        (model_rate != 8000 && model_rate != 16000 && model_rate != 48000) ||
        !model_path || !*model_path || suppression_db < 0 || suppression_db > 18) {
        throw std::invalid_argument("Invalid detailed speech configuration");
    }
    dry_mix_ = std::pow(10.0f, -suppression_db / 20.0f);
    input_.resize(model_frame_);
    output_.resize(model_frame_);
    dry_delay_.resize(5 * model_frame_);
    SherpaOnnxOnlineSpeechDenoiserConfig config{};
    config.model.dpdfnet.model = model_path;
    // v1.13.8 applies attenuation_limit_db only offline. Our aligned mix below
    // implements strength for streaming; never rely on that ignored setting.
    config.model.dpdfnet.attenuation_limit_db = 0;
    config.model.num_threads = 2;
    config.model.provider = "cpu";
    model_.reset(SherpaOnnxCreateOnlineSpeechDenoiser(&config));
    if (!model_ || SherpaOnnxOnlineSpeechDenoiserGetSampleRate(model_.get()) != model_rate_ ||
        SherpaOnnxOnlineSpeechDenoiserGetFrameShiftInSamples(model_.get()) != model_frame_) {
        throw std::runtime_error("Detailed speech model has an incompatible streaming contract");
    }
    if (rate != model_rate) {
        up_ = make_resampler(rate, model_rate);
        down_ = make_resampler(model_rate, rate);
        delay_samples_ += speex_resampler_get_input_latency(up_.get()) +
                          speex_resampler_get_output_latency(down_.get());
    }
}

DpdfnetDenoiser::ResamplerPtr DpdfnetDenoiser::make_resampler(int from, int to) {
    int error = RESAMPLER_ERR_SUCCESS;
    ResamplerPtr state(speex_resampler_init(1, from, to, 10, &error), speex_resampler_destroy);
    if (!state || error != RESAMPLER_ERR_SUCCESS) throw std::runtime_error("Speech resampling could not initialize");
    return state;
}

void DpdfnetDenoiser::resample(SpeexResamplerState* state, const float* input, int input_size,
                              float* output, int output_size) {
    auto consumed = static_cast<spx_uint32_t>(input_size);
    auto produced = static_cast<spx_uint32_t>(output_size);
    if (speex_resampler_process_float(state, 0, input, &consumed, output, &produced) != RESAMPLER_ERR_SUCCESS ||
        consumed != static_cast<spx_uint32_t>(input_size) || produced != static_cast<spx_uint32_t>(output_size)) {
        throw std::runtime_error("Detailed speech resampler returned an incomplete frame");
    }
}

void DpdfnetDenoiser::process(float* samples, int count) {
    if (!samples || count != frame_size_) throw std::invalid_argument("Invalid detailed speech frame");
    for (int i = 0; i < count; ++i) {
        samples[i] = std::isfinite(samples[i]) ? std::clamp(samples[i], -1.0f, 1.0f) : 0.0f;
    }
    if (up_) resample(up_.get(), samples, count, input_.data(), model_frame_);
    else std::copy_n(samples, count, input_.begin());

    using AudioPtr = std::unique_ptr<const SherpaOnnxDenoisedAudio, decltype(&SherpaOnnxDestroyDenoisedAudio)>;
    AudioPtr enhanced(SherpaOnnxOnlineSpeechDenoiserRun(model_.get(), input_.data(), model_frame_, model_rate_),
                      SherpaOnnxDestroyDenoisedAudio);
    const int received = enhanced ? enhanced->n : 0;
    // Upstream omits the first analysis hop; emit silence once to preserve the clock.
    if ((!started_ && received != 0) || (started_ && received != model_frame_) ||
        (enhanced && enhanced->sample_rate != model_rate_) || (received && !enhanced->samples)) {
        throw std::runtime_error("Detailed speech model returned an unexpected frame count or rate");
    }
    for (int i = 0; i < model_frame_; ++i) {
        const float wet = received ? enhanced->samples[i] : 0.0f;
        if (!std::isfinite(wet)) throw std::runtime_error("Detailed speech output is not finite");
        const int index = dry_position_ * model_frame_ + i;
        output_[i] = dry_mix_ * dry_delay_[index] + (1.0f - dry_mix_) * wet;
        dry_delay_[index] = input_[i];
    }
    dry_position_ = (dry_position_ + 1) % 5;
    started_ = true;
    if (down_) resample(down_.get(), output_.data(), model_frame_, samples, count);
    else std::copy_n(output_.begin(), count, samples);
    for (int i = 0; i < count; ++i) {
        if (!std::isfinite(samples[i])) throw std::runtime_error("Detailed speech output is not finite");
    }
}
}  // namespace openhearing
