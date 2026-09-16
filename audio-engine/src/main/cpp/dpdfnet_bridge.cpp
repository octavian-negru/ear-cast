#include <jni.h>
#include <exception>
#include <vector>
#include "dpdfnet_filter.h"

namespace {
struct Session {
    earcast::DpdfnetFilter denoiser;
    std::vector<float> frame;
    Session(int rate, int model_rate, int suppression, const char* path)
        : denoiser(rate, model_rate, suppression, path), frame(denoiser.frame_size()) {}
};

void fail(JNIEnv* env, const char* message) {
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type) env->ThrowNew(type, message);
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_app_earcast_core_audio_speech_DpdfnetBridge_create(
    JNIEnv* env, jobject, jint rate, jint model_rate, jint suppression, jstring path) {
    if (!path) { fail(env, "Detailed speech model path is missing"); return 0; }
    const char* model_path = env->GetStringUTFChars(path, nullptr);
    if (!model_path) return 0; // Preserve the pending JVM exception.
    Session* session = nullptr;
    try {
        session = new Session(rate, model_rate, suppression, model_path);
    } catch (const std::exception& e) {
        fail(env, e.what());
    }
    env->ReleaseStringUTFChars(path, model_path);
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jint JNICALL
Java_app_earcast_core_audio_speech_DpdfnetBridge_delaySamples(
    JNIEnv* env, jobject, jlong handle) {
    auto* session = reinterpret_cast<Session*>(handle);
    if (!session) {
        fail(env, "Speech processor is closed");
        return 0;
    }
    return session->denoiser.delay_samples();
}

extern "C" JNIEXPORT void JNICALL
Java_app_earcast_core_audio_speech_DpdfnetBridge_processFrame(
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
Java_app_earcast_core_audio_speech_DpdfnetBridge_destroy(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Session*>(handle);
}
