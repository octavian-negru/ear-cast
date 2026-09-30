# Release APK size audit

Measured locally on 2026-09-30. MB below means 1,000,000 bytes; the original APK
was 214,925,119 bytes (204.97 MiB). File managers may use different units.

| Package | APK size | Reduction from original |
|---|---:|---:|
| Original universal | 214.93 MB | — |
| Optimized universal (all ABIs) | 210.60 MB | 2.0% |
| arm64-v8a | 83.68 MB | 61.1% |
| armeabi-v7a | 75.23 MB | 65.0% |
| x86 | 88.48 MB | 58.8% |
| x86_64 | 87.49 MB | 59.3% |

Each architecture-specific file is a full standalone app. Install only the one
matching the device; combining their file sizes is not the user’s download size.
Use `just build-prod` and choose an output as described in [release instructions](RELEASE.md).
`just build-prod-universal` retains an all-architectures option.

## What changed

- The universal APK included native audio libraries for ARM64, ARM32, x86-64 and
  x86. Each smaller APK contains only the matching native code. All four device
  architectures remain supported by separate packages.
- App packaging now uses the same pinned NDK as the audio module. This fixes
  native symbol stripping: packaged `.debug_*` and `.symtab` sections are removed.
  Runtime-required dynamic symbols remain. Unstripped native build outputs remain
  available for debugging.
- R8 and resource shrinking were already enabled and remain enabled. DEX code is
  only about 1 MB packed, so native packaging provides most of the savings.
- Native libraries remain uncompressed and directly loadable from the APK. This
  avoids trading a smaller download for an extra extracted native-library copy.

## Audio preservation checks

The original APK’s assets and each native library’s file-backed allocated ELF
sections were hashed before rebuilding. Across all four reduced APKs:

- All non-ART assets, including all three DPDFNet8 ONNX models and licenses, matched
  their original SHA-256 hashes exactly. Model precision and weights are unchanged.
- Every retained native library’s runtime section hashes matched the original
  architecture’s library, including executable code and embedded RNNoise weights.
  Only non-runtime debug/symbol data and other architectures were removed.
- Each ABI retained the complete original set of six native libraries. There were
  no missing audio engines, downgraded resamplers, or changes to DSP/limiter code.
- All APK signatures and ZIP alignment checks passed. Both 64-bit APKs passed the
  native 16 KB ELF LOAD-segment alignment check.

The 122 audio-engine and 4 app JVM regression tests passed, as did the affected
ktlint checks and release packaging checks. A local audit App Bundle build was
inspected and retained all four CPU architectures. This is not a store-ready
publication build.

These binary checks establish that the runtime audio code and model data were
preserved. They do not replace installation and listening tests on actual phones.

## Recheck size

```sh
just apk-size
# Inspect one APK explicitly:
python3 scripts/apk_size.py app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

The report uses Gradle’s output metadata, so it reports the most recently built
release outputs instead of relying on a stale APK filename.

App Bundles use a separate build invocation with `-PearcastSplitApks=false` because
AGP 8.10 cannot consume multiple shrunk APK-resource outputs in its bundle task.
This does not limit bundle ABIs; the bundle still includes all four architectures.
