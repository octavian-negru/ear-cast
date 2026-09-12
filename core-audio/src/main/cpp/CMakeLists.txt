cmake_minimum_required(VERSION 3.22)
project(openhearing_speech LANGUAGES C CXX)
set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

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

if(NOT ANDROID)
    find_package(JNI REQUIRED)
endif()
add_library(openhearing_speech SHARED speech_jni.cpp)
target_include_directories(openhearing_speech PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(openhearing_speech PRIVATE speex_preprocess)
if(ANDROID)
    # Support devices with 16 KB memory pages as well as 4 KB pages.
    target_link_options(openhearing_speech PRIVATE "-Wl,-z,max-page-size=16384")
endif()

if(NOT ANDROID)
    enable_testing()
    add_executable(speech_quality_test tests/speech_quality_test.cpp)
    target_link_libraries(speech_quality_test PRIVATE speex_preprocess)
    add_test(NAME speech_quality COMMAND speech_quality_test)
endif()
