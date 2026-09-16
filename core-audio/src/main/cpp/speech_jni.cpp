#include <jni.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <vector>
#include "speex/speex_preprocess.h"

namespace {
struct Session {
    SpeexPreprocessState* state;
    std::vector<float> floats;
    std::vector<spx_int16_t> pcm;
    Session(int rate, int frames, int suppression)
        : state(speex_preprocess_state_init(frames, rate)), floats(frames), pcm(frames) {
        if (!state) throw std::bad_alloc();
        int enabled = 1, disabled = 0, floor = -suppression;
        speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DENOISE, &enabled);
        speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &floor);
        // No automatic makeup gain, voice gating, dereverberation, or echo cancellation.
        speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_AGC, &disabled);
        speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DEREVERB, &disabled);
    }
    ~Session() { speex_preprocess_state_destroy(state); }
};
void fail(JNIEnv* env, const char* message) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_app_openhearing_core_audio_speech_NativeSpeexDenoiser_create(
    JNIEnv* env, jobject, jint rate, jint frames, jint suppression) {
    if ((rate != 8000 && rate != 16000 && rate != 24000 && rate != 32000 && rate != 44100 && rate != 48000) || frames != rate / 100 ||
        suppression < 0 || suppression > 18) {
        fail(env, "Unsupported speech processor configuration");
        return 0;
    }
    try {
        return reinterpret_cast<jlong>(new Session(rate, frames, suppression));
    } catch (const std::exception&) {
        fail(env, "Could not allocate the speech processor");
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_app_openhearing_core_audio_speech_NativeSpeexDenoiser_processFrame(
    JNIEnv* env, jobject, jlong handle, jfloatArray frame) {
    auto* session = reinterpret_cast<Session*>(handle);
    if (!session || !frame || env->GetArrayLength(frame) != static_cast<jsize>(session->floats.size())) {
        fail(env, "Invalid speech processor frame");
        return;
    }
    auto& samples = session->floats;
    env->GetFloatArrayRegion(frame, 0, static_cast<jsize>(samples.size()), samples.data());
    if (env->ExceptionCheck()) return;
    for (size_t i = 0; i < samples.size(); ++i) {
        const float value = std::isfinite(samples[i]) ? samples[i] : 0.0f;
        session->pcm[i] = static_cast<spx_int16_t>(std::clamp(value * 32768.0f, -32768.0f, 32767.0f));
    }
    speex_preprocess_run(session->state, session->pcm.data());
    for (size_t i = 0; i < samples.size(); ++i) samples[i] = session->pcm[i] / 32768.0f;
    env->SetFloatArrayRegion(frame, 0, static_cast<jsize>(samples.size()), samples.data());
}

extern "C" JNIEXPORT void JNICALL
Java_app_openhearing_core_audio_speech_NativeSpeexDenoiser_destroy(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Session*>(handle);
}
