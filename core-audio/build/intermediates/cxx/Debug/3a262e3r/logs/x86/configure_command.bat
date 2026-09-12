@echo off
"C:\\Users\\Utilizator\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HF:\\Oct\\temp\\core-audio\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=26" ^
  "-DANDROID_PLATFORM=android-26" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\Utilizator\\AppData\\Local\\Android\\Sdk\\ndk\\27.2.12479018" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\Utilizator\\AppData\\Local\\Android\\Sdk\\ndk\\27.2.12479018" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\Utilizator\\AppData\\Local\\Android\\Sdk\\ndk\\27.2.12479018\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\Utilizator\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=F:\\Oct\\temp\\core-audio\\build\\intermediates\\cxx\\Debug\\3a262e3r\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=F:\\Oct\\temp\\core-audio\\build\\intermediates\\cxx\\Debug\\3a262e3r\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BF:\\Oct\\temp\\core-audio\\.cxx\\Debug\\3a262e3r\\x86" ^
  -GNinja
