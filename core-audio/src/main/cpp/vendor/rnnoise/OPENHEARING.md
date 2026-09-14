# RNNoise provenance and local changes

Vendored from the official Xiph RNNoise source, revision
`70f1d256acd4b34a572f999a05c87bf00b67730d`. See `PROVENANCE.txt` for source and model
URLs and archive SHA-256 hashes. The model archive was verified against upstream
`model_version` before extraction. `src/rnnoise_data.c` and `.h` are the **full**
model from that archive, unchanged; the smaller alternate model is not used.
The generated C weights occupy about 78 MB of source, not 78 MB of runtime PCM
or necessarily 78 MB of APK space. Measure final ABI/package sizes on a build host.

Upstream license and authors are retained in `COPYING` and `AUTHORS`. Preserve
these notices in binary distributions as required by the license.

## Local patch

Only `src/denoise.c` and `include/rnnoise.h` change the upstream implementation.
`rnnoise_process_frame_with_dry_mix` saves the delayed input spectrum and blends
it with the enhanced spectrum before the existing synthesis/overlap-add step.
This aligns both paths through the same high-pass, spectral delay and windowing.
Mixing raw current-frame PCM into RNNoise output would introduce a timing error.

The original `rnnoise_process_frame` delegates with zero dry contribution and
retains the upstream behavior. Model inference, recurrent state, pitch filter
and gain smoothing are unchanged. No VAD-controlled output gate is added.
Dry contribution is not a mathematical lower bound on output amplitude: complex
spectra can partially cancel, and the model's high-pass/bandwidth still apply.

The local CMake source list follows upstream `Makefile.am`. CPU dispatch is not
enabled; the compiler's baseline SIMD implementation is used. All model files
are present locally, with no configure-time downloads or weight generation.
`SHA256SUMS` records the bundled source, including the two local modifications.

For updates, re-check frame size, PCM scaling, spectral delay, model/header pairing
and the local blend patch. Run `neural_speech_test` and the Kotlin front-end tests
on a machine with build tools, then compare actual speech recordings. A successful
compile alone is not an audio-quality evaluation.
