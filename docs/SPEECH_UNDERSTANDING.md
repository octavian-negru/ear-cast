# Speech understanding update

For the subsequent gain and clarity retuning, see [Gentler gain and speech
contrast](COMFORT_TUNING.md). The notes below describe the earlier engine update.

The user reports a substantial improvement with the earlier RNNoise integration,
but remaining muffling. Their best headset settings are **Standard, Natural,
Speech Strong, Bass Gentle, Noise Reduction Strong, Quiet Speech Boost Off**.
The phone microphone sounds better. These are the comparison baseline, not
measurements of this new update, which has not been compiled or run here.

## Changes aimed at the muffling

Previously, speech presence was a peaking filter before per-ear WDRC. With active
3:1 compression, much of an added boost can be compressed away. Speech clarity
now uses a broad upper-frequency shelf **after WDRC**, before feedback protection,
master gain and the final limiter. Gentle/Strong retain their 3/6 dB settings.
The shelf transitions around 1.8 kHz (1.6 kHz at 8 kHz capture), preserving
consonant-band emphasis beyond the old presence peak. Bass reduction remains
separate; the user's fitted gains and output ceilings remain in place.

This addresses a concrete signal-chain issue. Extra high-frequency energy alone
does not prove better word recognition; it still needs listening comparison.

Outdoors has been removed from the speech-profile selector. An old saved
`OUTDOORS` value resolves to Standard, including starts through the tile. Music
processing has not been expanded as part of this speech-focused work.

## New complete speech engine

**DPDFNet8** is now selectable alongside RNNoise. Actual pretrained eight-block
models are bundled for 8, 16 and 48 kHz, together with the pinned sherpa-onnx
v1.13.8 C API / ONNX Runtime 1.28.2 libraries for all four Android ABIs.
24/32/44.1 kHz capture uses the full-band model through SpeexDSP quality-10
resampling. Inference, recurrent state, normalization, STFT/ISTFT and complex
deep filtering use the upstream implementation. There is no cascade of two
denoisers, model download at runtime, or microphone upload.

DPDFNet extends DeepFilterNet2 with dual-path recurrent processing. That makes it
a technically relevant challenger for speech, not a demonstrated winner on this
headset. The full eight-block model prioritizes quality over CPU; the 48 kHz
variant is particularly demanding. RNNoise remains the existing default and an
available comparison. If the new engine cannot initialize, the session reports
failure rather than secretly changing engines. [Project and model profiles](https://github.com/ceva-ip/DPDFNet),
[paper](https://arxiv.org/abs/2512.16420).

Strong/Gentle still retain about 25%/50% original signal. In this runtime,
the streaming API ignores the offline attenuation setting, so our adapter
implements an explicit aligned mix. The pinned model's Mask adds two
delay hops, its deep-filter centre another two, and analysis/synthesis one:
50 ms before resampling and the extra 10 ms Kotlin adapter. This is source-derived
delay, not measured acoustic latency. Offline replay flushes this entire tail.
See [runtime provenance](../audio-engine/src/main/cpp/vendor/sherpa-onnx/README.md)
and [model provenance](../audio-engine/src/main/assets/speech-models/README.md).

The model does not expose speech confidence through this API. Quiet Speech Boost
is therefore explicitly unavailable with DPDFNet8; fitted amplification and the
volume control remain active. That matches the user's preferred Boost Off setting.
Model checksums remain pinned in the source. The app no longer records audio or diagnostic metadata.

## Phonak and the sibling project

Phonak describes Spheric Speech Clarity as using its proprietary DEEPSONIC chip.
I found no public Android SDK supplying that algorithm. It cannot honestly be
presented as an importable library or reproduced by renaming a general-purpose
denoiser. Hearing-aid hardware and its microphone/fitting system are also different
from an Android app receiving headset call audio. [Sonova announcement](https://www.sonova.com/sites/default/files/2024-08/Media%20Release_Infinio_240806_1.pdf).

`../AudioApp` is the audio-related sibling available in this workspace. Its
amplitude gate zeros low-amplitude samples; its FFT path has inconsistent PCM
scaling and incomplete block handling. Those mechanisms would risk discarding
the weak speech cues this app needs, so they were inspected rather than adopted.

## Next comparison

1. Keep the exact baseline settings above with RNNoise and compare the new
   post-compression Speech Strong behavior at the same comfortable output level.
2. Change only the engine to DPDFNet8 and restart. Keep Noise Reduction Strong
   and Boost Off. Compare consonants, quiet word endings and complete sentences.
   If Strong loses quiet words, compare Gentle without increasing amplification.
3. Record the same talker and placement with each engine and each microphone.
   Inspect the raw tap before attributing missing detail to processing. A higher
   app PCM rate cannot restore frequencies removed by headset firmware or SCO.
4. Run the [workbench](../audio-quality/README.md) on a build host: native DPDFNet
   adapter tests, Kotlin post-WDRC response/limiter tests, and real-speech replay
   with fixed delay alignment, STOI/ESTOI and blind level-matched listening.
5. Validate release/R8/JNI packaging, all ABIs, model initialization, repeated
   stop/start, route loss, processing time and capture continuity. The prebuilt
   ELF files have verified 16 KB load alignment; APK packaging is still untested.
   Capture scheduling stalls must not be mistaken for poor speech enhancement.

No new build, compiler invocation, dependency installation or executable DSP test
was performed. Source syntax/metadata/checksum inspection does not establish
compilability, runtime performance or a new intelligibility score.
