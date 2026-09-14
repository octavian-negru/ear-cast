# Pinned Android speech runtime

Upstream: [sherpa-onnx v1.13.8](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8).
Only its C API and ONNX Runtime shared libraries are included, for arm64-v8a,
armeabi-v7a, x86_64 and x86. The Java API, ASR/TTS models and example apps are not
packaged. The C API library itself contains upstream's other compiled features.

Source artifact:
`https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar`

Upstream AAR SHA256:
`633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`

`android-runtime.tar.xz` losslessly repackages the eight unmodified `.so` files
from that AAR's `jni/` directories. CMake checks its digest before extracting it
on a build host. `RUNTIME_SHA256SUMS` records the individual library digests;
`SHA256SUMS` covers files in this directory. No binaries were executed or rebuilt
during integration. Static ELF inspection found 16 KB PT_LOAD alignment in all
eight libraries. APK packaging and loading still require a future build/device check.

`c-api.h` is unmodified from the same upstream tag. The C ABI avoids exposing
upstream C++ types to our adapter. ONNX Runtime is 1.28.2, as specified by all four
Android dependency files in the tagged sherpa-onnx source.

Licenses and corresponding upstream source:

- sherpa-onnx: Apache-2.0, `LICENSE`, [source](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8).
- ONNX Runtime: MIT, `ONNXRUNTIME-LICENSE` and `ONNXRUNTIME-ThirdPartyNotices.txt`,
  [source](https://github.com/microsoft/onnxruntime/tree/v1.28.2).
- The runtime includes Piper phonemization and eSpeak NG code even though this
  app only invokes speech enhancement. See `PIPER-PHONEMIZE-LICENSE.md` (MIT) and
  `ESPEAK-NG-COPYING` (GPLv3), with sources at
  [Piper commit f3ff95a](https://github.com/csukuangfj/piper-phonemize/tree/f3ff95afc03640bc1399e113e83361192a2fafb4) and
  [eSpeak NG commit 7251208](https://github.com/csukuangfj/espeak-ng/tree/7251208ee427ef87e94f39d0e99b15d287224ed6).

Preserve these notices and corresponding-source availability when distributing
the app (whose existing root license is GPLv3). A future minimal runtime build
can remove unused feature families; it is not required to try speech enhancement.

The native streaming API does **not** apply `attenuation_limit_db` in this release.
Our adapter implements strength with a delay-matched dry contribution instead.
The model's four internal delay hops must be counted in addition to the streaming
STFT hop. Do not replace the models without rechecking this contract.
