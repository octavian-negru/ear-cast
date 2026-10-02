# Dependency versions

Reviewed against official release metadata on October 2, 2026. The version catalog
in `gradle/libs.versions.toml` pins the latest stable releases; preview releases
are excluded. Compose libraries use the stable Compose BOM.

| Build tool | Version |
| --- | --- |
| Gradle | 9.8.0 |
| Android Gradle plugin | 9.4.1 |
| Kotlin / Compose compiler | 2.4.20 |
| KSP | 2.3.12 |
| Android NDK | 30.0.16248370 (r30) |
| Android SDK CMake | 4.1.2 |
| Compile SDK | 37 |

AGP 9 provides built-in Kotlin for Android modules. JVM modules and the Compose
compiler use the catalog's Kotlin version. JUnit Jupiter 6 uses an explicit
JUnit Platform launcher in every test module. The Android test plugin uses its
current `de.mannodermaus.android-junit` ID. Audio-quality Kotlin tests are attached
through the Android variant API.

The Python evaluation requirements specify the current stable NumPy 2.5.3,
SciPy 1.18.1 and pystoi 0.4.1 as their minimum versions.

## Bundled native libraries

- RNNoise is already at upstream's latest commit,
  `70f1d256acd4b34a572f999a05c87bf00b67730d`, which is newer than tag `v0.2`.
  Preserve the documented EarCast dry-mix patch and model checksum when updating.
- SpeexDSP's resampler already uses the latest release, `SpeexDSP-1.2.1`.
- sherpa-onnx already uses the latest release, `v1.13.8`; keep its bundled ONNX
  Runtime libraries paired with that upstream Android package.

## Release sources

- [Google Maven metadata](https://dl.google.com/dl/android/maven2/master-index.xml)
  and [AndroidX releases](https://developer.android.com/jetpack/androidx/versions)
- [Maven Central](https://repo.maven.apache.org/maven2/)
- [Gradle current release](https://services.gradle.org/versions/current)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [Android SDK repository](https://dl.google.com/android/repository/repository2-3.xml)
- [ktlint Gradle plugin releases](https://github.com/JLLeitschuh/ktlint-gradle/releases)
- [RNNoise source](https://github.com/xiph/rnnoise)
- [SpeexDSP releases](https://github.com/xiph/speexdsp/tags)
- [sherpa-onnx releases](https://github.com/k2-fsa/sherpa-onnx/releases)
