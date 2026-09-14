#include <jni.h>
#include <exception>
#include <vector>
#include "neural_denoiser.h"

namespace {
struct Session {
    openhearing::NeuralDenoiser denoiser;
    std::vector<float> frame;
    Session(int rate, int suppression) : denoiser(rate, suppression), frame(denoiser.frame_size()) {}
};

void fail(JNIEnv* env, const char* message) {
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type) env->ThrowNew(type, message);
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_app_openhearing_core_audio_speech_NativeRnnoiseDenoiser_create(
    JNIEnv* env, jobject, jint rate, jint suppression) {
    try {
        return reinterpret_cast<jlong>(new Session(rate, suppression));
    } catch (const std::exception& e) {
        fail(env, e.what());
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_app_openhearing_core_audio_speech_NativeRnnoiseDenoiser_processFrame(
    JNIEnv* env, jobject, jlong handle, jfloatArray frame) {
    auto* session = reinterpret_cast<Session*>(handle);
    if (!session || !frame || env->GetArrayLength(frame) != static_cast<jsize>(session->frame.size())) {
        fail(env, "Invalid neural speech processor frame");
        return;
    }
    auto& samples = session->frame;
    env->GetFloatArrayRegion(frame, 0, static_cast<jsize>(samples.size()), samples.data());
    if (env->ExceptionCheck()) return;
    try {
        session->denoiser.process(samples.data(), static_cast<int>(samples.size()));
    } catch (const std::exception& e) {
        fail(env, e.what());
        return;
    }
    env->SetFloatArrayRegion(frame, 0, static_cast<jsize>(samples.size()), samples.data());
}

extern "C" JNIEXPORT void JNICALL
Java_app_openhearing_core_audio_speech_NativeRnnoiseDenoiser_destroy(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Session*>(handle);
}
