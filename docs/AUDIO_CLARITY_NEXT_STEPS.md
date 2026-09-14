# Audio clarity: implementation, library review and next steps

Research and source review: 2026-09-14. **No builds, compiler invocations, Gradle
tasks or executable DSP tests were run, and no build tools were installed.**

## What changed

Hearing Assist now selects the bundled **full RNNoise neural model** whenever
Noise reduction is Gentle or Strong. It replaces SpeexDSP suppression in the
live path; the older processor remains in the source for comparison. This is
speech enhancement, not speech recognition or an environmental-event classifier.

After a future build, stop assist, choose **Natural** capture and **Gentle** noise
reduction, then restart. Existing settings, including Off, are preserved; Off
continues to bypass the denoiser. Compare both microphone sources using the same
talker before increasing strength. Bass reduction can be turned Off if voices
sound thin. Presence EQ and fitted amplification are separate from denoising.

The implementation supplies the model's 48 kHz, mono, 480-sample contract. SCO
8/16 kHz input is converted with the stateful **SpeexDSP quality-10 resampler**,
enhanced, then converted back. All intermediate samples remain floating point;
RNNoise receives PCM16-scale floats, not normalized floats or re-quantized shorts.
Resampling does not recreate frequencies absent from the capture.

Gentle retains approximately 50% dry contribution, Strong approximately 25%.
The blend happens in the same delayed spectrum before synthesis, preventing a
timing mismatch between paths. These are blend settings, not guaranteed 6/12 dB
attenuation limits. There is no VAD gate, speaker-selection rule, added AGC, or
second denoiser. The existing per-ear fitting, compression, feedback guard,
master-gain cap and final output limiters remain downstream.

Native state is owned by the audio worker and released on session exit. Invalid
frames or initialization failures become session errors, rather than silently
pretending enhancement is active. JNI keep rules cover release shrinking. Sources,
licenses, model weights and checksums are bundled for an offline native build.
The generated full-model C weights add about 78 MB to the source tree; final
library/APK size has not been measured. Full model selection prioritizes quality;
do not substitute the smaller model without listening comparisons.

The frame adapter adds 10 ms buffering, in addition to RNNoise's analysis/model
delay and the resampler filter delay. Transport and platform buffering add more.
No claim of a 10 ms total delay is made, and latency optimization is deferred.

## Why the example is not a quality reference

The sibling `../AudioApp` has a fallback detector that converts normalized power
to dBFS (normally nonpositive), then compares it with a positive linear-looking
threshold. This prevents ordinary quiet speech from passing its energy test.
Its amplitude gate can attenuate weak signals further. Its VAD frame is described
as 160 samples at 16 kHz while capture requests 44.1 kHz. None of those mechanisms
establishes reliable distant-speech detection. The example was inspected, not
modified or copied into this app.

The present app previously offered presence EQ plus optional traditional Speex
noise suppression. An EQ boost raises noise in that band too; it is not evidence
of improved intelligibility. Simply turning up gain or tightening a voice gate
does not solve this problem.

## Library comparison

| Library | Appropriate role | Decision for this app |
|---|---|---|
| [Xiph RNNoise](https://github.com/xiph/rnnoise) | Neural speech enhancement combining learned suppression and DSP | Integrated with the current full model and controlled ambience retention. Its C implementation fits the existing native layer and permits inspecting the exact processing. This is an implementable baseline, not a proven winner on this headset. |
| [DeepFilterNet](https://github.com/Rikorose/DeepFilterNet) | Full-band speech enhancement using multi-frame complex filtering | First quality challenger to evaluate. The [research](https://arxiv.org/abs/2305.08227) motivates using temporal information beyond spectral gains. Compare the standard DFN3 model, not just its low-latency variant. |
| [WebRTC Audio Processing Module](https://webrtc.googlesource.com/src/+/refs/heads/main/modules/audio_processing/g3doc/audio_processing_module.md) | A coordinated VoIP front end with noise suppression, echo cancellation and gain control | Useful comparison, especially if recordings show level-management or actual echo problems. Do not stack its suppression/AGC blindly with headset processing, RNNoise and fitted WDRC. |
| [Picovoice Koala](https://picovoice.ai/docs/quick-start/koala-android/) | Commercial SDK with documented Android integration | Candidate if vendor support is a requirement. Requires an account/AccessKey and a licensing/deployment decision. No proprietary dependency or secret was added. Vendor benchmarks are not a substitute for this headset's recordings. |
| [SpeexDSP](https://www.speex.org/docs/manual/speex-manual/node7.html) | Conventional preprocessing and sample-rate conversion | Retain the resampler and an old-denoiser baseline. Stronger traditional suppression alone is not the quality strategy. |

DeepFilterNet has an actual [C API](https://github.com/Rikorose/DeepFilterNet/blob/main/libDF/src/capi.rs),
including frame-length discovery, attenuation control and complete frame
processing. Its [Cargo configuration](https://github.com/Rikorose/DeepFilterNet/blob/main/libDF/Cargo.toml)
uses Rust and the tract inference runtime for that API. It is not just a model
that can be dropped into a generic Android ONNX call: model configuration,
feature normalization, STFT/ISTFT, temporal state and filter application must
agree. A proper integration should package pinned Android libraries for each ABI
and reuse its complete processing implementation, or reproduce that contract
against golden outputs. This remains future work; this change does not include
a placeholder DFN backend or claim a quality ranking based on popularity.

## Next steps, in priority order

### 1. Establish what the headset actually captures

Add an explicitly enabled diagnostic recording mode with synchronized taps before
enhancement, after enhancement and after limiting. Keep files local. Record the
routed input/output, requested and actual stream rate, Android source, headset
model, firmware, and processing settings. Record input RMS/peak/clipped-sample
count and capture overruns; output level alone cannot reveal missing input.

Use a repeatable speaker at 0.5, 2 and 5 metres, in quiet, fan noise, speech babble
and a reverberant room. Include soft consonants, speech onsets after silence,
two simultaneous talkers, wearer speech, music and important environmental sounds.
Capture the headset microphone and phone microphone at the same listener position;
then separately capture the phone near the speaker. The latter is a placement
comparison, not a software-quality comparison.

Android [documents](https://developer.android.com/media/platform/mediarecorder)
requesting unprocessed capture where supported, with voice-recognition capture
as an alternative. That API choice does not prove the Bluetooth firmware feed is
raw. If the distant voice is already absent in the pre-enhancement tap, prioritize
an ambient-capable microphone, external/remote mic, or microphone placement.
Do not invent a five-metre sensitivity guarantee or claim that upsampling restores
information. The app currently receives mono capture, not a synchronized raw
earbud microphone array suitable for beamforming.

### 2. Run the deferred correctness checks on a build machine

- Build debug and release for the four configured Android ABIs; check JNI loading,
  model initialization, release/R8 retention and 16 KB native-library alignment.
- Run `SpeechFrontEndTest` and the existing downstream limiter/WDRC tests. The
  extended framing test covers 8, 16 and 48 kHz with irregular and oversized blocks.
- Run the added `neural_speech_test` CMake/CTest target on a host with a C/C++
  toolchain and JDK/JNI headers. It checks bundled-model initialization, dry/wet
  temporal alignment, weak-signal float preservation in the dry reference,
  resampler frame counts, silence, invalid input and incorrect frame lengths.
- Exercise start/stop, route loss, initialization failure and repeated sessions on
  real hardware. Check recording continuity while the full model runs; tolerating
  lag does not mean accepting dropped capture frames.

These tests are authored but **not run**. Static source inspection, resource XML
parsing, source/model checksums and path/interface checks are the only validation
performed in this environment. They cannot certify compilability or sound quality.

### 3. Compare quality before tuning defaults

Replay the same captured input through Off, the old Speex baseline, RNNoise
Gentle/Strong and standard DeepFilterNet3. Use blind, level-matched listening;
louder output can masquerade as clearer output. Listen for missing words,
consonants, pumping, tonal artifacts and loss of useful surrounding sounds.

With clean aligned references, use [STOI/ESTOI](https://github.com/mpariente/pystoi)
as supplementary intelligibility measurements. For non-reference recordings,
[DNSMOS and DNS challenge tools](https://github.com/microsoft/DNS-Challenge) can
provide another signal. Do not select a model solely because it makes background
noise quieter or scores better on one metric. Predeclare acceptance criteria:
improved distant-word recognition and listener preference in target conditions,
no onset loss in quiet speech, and acceptable environmental-sound retention.
Report failures per condition, not just an overall average.

### 4. Improve audibility and reverberation based on that evidence

If speech survives capture but stays too quiet, evaluate bounded, slowly varying
speech-aware input leveling and refit the multiband WDRC knees/gains using measured
levels. Limit gain growth during noise-only periods without muting low-confidence
frames. Preserve the output limiter and evaluate own-voice comfort and pumping.
Current digital gains are not calibrated hearing-aid prescriptions.

If room reflections dominate, compare DeepFilterNet and training/adaptation with
measured room responses and actual SCO-filtered speech. RNNoise's upstream
[training instructions](https://github.com/xiph/rnnoise#readme) support room-response
augmentation. A denoiser is not automatically a dereverberator, and clean 48 kHz
benchmark results do not establish performance on an 8/16 kHz headset feed.

Keep speech focus and general environmental listening as distinct quality goals.
A model trained to remove non-speech can remove sounds a hearing-assistance user
wants to hear. Preserve an accessible Off option and evaluate that tradeoff explicitly.

### 5. Optimize deployment and latency only after quality selection

Measure per-frame processing time, capture loss, temperature/battery behavior and
end-to-end delay. Then address worker buffering and CPU dispatch, package/model
size and possibly Oboe I/O. Keep model changes subject to the same recording-based
comparison; lower latency alone is not an intelligibility improvement.
