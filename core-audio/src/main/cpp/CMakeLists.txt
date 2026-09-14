cmake_minimum_required(VERSION 3.22)
project(openhearing_speech LANGUAGES C CXX)
set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(SPEEX vendor/speexdsp)
add_library(speex_preprocess STATIC
    ${SPEEX}/libspeexdsp/preprocess.c
    ${SPEEX}/libspeexdsp/mdf.c
    ${SPEEX}/libspeexdsp/fftwrap.c
    ${SPEEX}/libspeexdsp/filterbank.c
    ${SPEEX}/libspeexdsp/smallft.c)
target_include_directories(speex_preprocess PUBLIC ${SPEEX}/include PRIVATE ${SPEEX}/libspeexdsp)
target_compile_definitions(speex_preprocess PRIVATE FLOATING_POINT USE_SMALLFT EXPORT=)
if(NOT MSVC)
    target_link_libraries(speex_preprocess PUBLIC m)
endif()

# Resampling is independent of the legacy Speex denoiser: never cascade both
# noise suppressors. Quality 10 favors fidelity over CPU and filter delay.
add_library(speex_resampler STATIC ${SPEEX}/libspeexdsp/resample.c)
target_include_directories(speex_resampler PUBLIC ${SPEEX}/include PRIVATE ${SPEEX}/libspeexdsp)
target_compile_definitions(speex_resampler PRIVATE FLOATING_POINT EXPORT=)

# Pinned sources and full model are bundled; no network or model generation at build time.
set(RNNOISE vendor/rnnoise)
add_library(rnnoise STATIC
    ${RNNOISE}/src/denoise.c
    ${RNNOISE}/src/rnn.c
    ${RNNOISE}/src/pitch.c
    ${RNNOISE}/src/kiss_fft.c
    ${RNNOISE}/src/celt_lpc.c
    ${RNNOISE}/src/nnet.c
    ${RNNOISE}/src/nnet_default.c
    ${RNNOISE}/src/parse_lpcnet_weights.c
    ${RNNOISE}/src/rnnoise_data.c
    ${RNNOISE}/src/rnnoise_tables.c)
target_include_directories(rnnoise PUBLIC ${RNNOISE}/include PRIVATE ${RNNOISE}/src)
target_compile_definitions(rnnoise PRIVATE RNNOISE_BUILD OPUS_BUILD OPUS_INLINE=inline)

add_library(neural_speech STATIC neural_denoiser.cpp speech_leveler.cpp)
target_include_directories(neural_speech PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(neural_speech PUBLIC rnnoise speex_resampler)
if(NOT MSVC)
    target_link_libraries(neural_speech PUBLIC m)
endif()

option(OPENHEARING_BUILD_JNI "Build the Android/JVM bridge" ON)
# The upstream runtime is already compiled. Unpack only when CMake is configured
# on a build host; no network, Rust toolchain or model export is involved.
set(OPENHEARING_SHERPA_RUNTIME_DIR "" CACHE PATH "Existing Linux sherpa-onnx/ONNX Runtime shared libraries")
if(ANDROID OR OPENHEARING_SHERPA_RUNTIME_DIR)
    if(ANDROID)
        set(SHERPA ${CMAKE_CURRENT_SOURCE_DIR}/vendor/sherpa-onnx)
        set(SHERPA_RUNTIME ${CMAKE_CURRENT_BINARY_DIR}/sherpa-runtime)
        file(SHA256 ${SHERPA}/android-runtime.tar.xz SHERPA_ARCHIVE_SHA256)
        if(NOT SHERPA_ARCHIVE_SHA256 STREQUAL "dd5a7a13b445200b633ea80df1a5926dff014f08112a48871cb49f0af8b76dd2")
            message(FATAL_ERROR "Bundled speech runtime checksum mismatch")
        endif()
        file(ARCHIVE_EXTRACT INPUT ${SHERPA}/android-runtime.tar.xz DESTINATION ${SHERPA_RUNTIME})
        set(SHERPA_LIB_DIR ${SHERPA_RUNTIME}/${ANDROID_ABI})
    else()
        set(SHERPA_LIB_DIR ${OPENHEARING_SHERPA_RUNTIME_DIR})
    endif()
    add_library(sherpa_runtime SHARED IMPORTED)
    set_target_properties(sherpa_runtime PROPERTIES
        IMPORTED_LOCATION ${SHERPA_LIB_DIR}/libsherpa-onnx-c-api.so)
    add_library(onnx_runtime SHARED IMPORTED)
    set_target_properties(onnx_runtime PROPERTIES
        IMPORTED_LOCATION ${SHERPA_LIB_DIR}/libonnxruntime.so)
    add_library(detailed_speech STATIC dpdfnet_denoiser.cpp)
    target_include_directories(detailed_speech PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
    target_link_libraries(detailed_speech PUBLIC sherpa_runtime onnx_runtime speex_resampler m)
    if(ANDROID AND OPENHEARING_BUILD_JNI)
        add_library(openhearing_detailed_speech SHARED dpdfnet_jni.cpp)
        target_link_libraries(openhearing_detailed_speech PRIVATE detailed_speech)
        target_link_options(openhearing_detailed_speech PRIVATE "-Wl,-z,max-page-size=16384")
    endif()
endif()
if(OPENHEARING_BUILD_JNI)
    if(NOT ANDROID)
        find_package(JNI REQUIRED)
    endif()
    add_library(openhearing_speech SHARED speech_jni.cpp neural_jni.cpp)
    target_include_directories(openhearing_speech PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(openhearing_speech PRIVATE speex_preprocess neural_speech)
    if(ANDROID)
        target_link_options(openhearing_speech PRIVATE "-Wl,-z,max-page-size=16384")
    endif()
endif()
