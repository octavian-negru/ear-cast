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

add_library(neural_speech STATIC neural_denoiser.cpp)
target_include_directories(neural_speech PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(neural_speech PUBLIC rnnoise speex_resampler)
if(NOT MSVC)
    target_link_libraries(neural_speech PUBLIC m)
endif()

if(NOT ANDROID)
    find_package(JNI REQUIRED)
endif()
add_library(openhearing_speech SHARED speech_jni.cpp neural_jni.cpp)
target_include_directories(openhearing_speech PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(openhearing_speech PRIVATE speex_preprocess neural_speech)
if(ANDROID)
    # Support devices with 16 KB memory pages as well as 4 KB pages.
    target_link_options(openhearing_speech PRIVATE "-Wl,-z,max-page-size=16384")
endif()

if(NOT ANDROID)
    enable_testing()
    add_executable(neural_speech_test tests/neural_speech_test.cpp)
    target_link_libraries(neural_speech_test PRIVATE neural_speech)
    add_test(NAME neural_speech COMMAND neural_speech_test)
endif()
