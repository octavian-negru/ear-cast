# JNI symbols use this class and its native method names, including release/R8 builds.
-keep class app.openhearing.core.audio.speech.NativeSpeexDenoiser { native <methods>; }
-keep class app.openhearing.core.audio.speech.NativeRnnoiseDenoiser { native <methods>; }
-keep class app.openhearing.core.audio.speech.NativeDpdfnetDenoiser { native <methods>; }
-keep class app.openhearing.core.audio.speech.NativeWienerDenoiser { native <methods>; }
