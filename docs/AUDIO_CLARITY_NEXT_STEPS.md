# Audio clarity: implementation, library review and next steps

## Latest: speech understanding and remaining muffling

The user has now tested the earlier changes and reports substantially better sound.
The latest source update moves speech clarity after WDRC, adds the complete
DPDFNet8 Android engine with three pinned full models, and removes Outdoors from
the selector. The user's Standard/Natural/Speech Strong/Bass Gentle/Noise Strong/
Boost Off combination is preserved as the baseline. **This newest update has not
been built or run.** See [speech understanding changes and next comparison](SPEECH_UNDERSTANDING.md)
for the current implementation, library decision and prioritized validation.

The sections below retain the earlier RNNoise/DeepFilterNet research and workbench
history; DPDFNet8 is now an additional integrated engine, not an unimplemented proposal.

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

The implementation supplies the model's 48 kHz, mono, 480-sample contract. Capture
at 8, 16, 24, 32 or 44.1 kHz is converted with the **SpeexDSP quality-10 resampler**,
enhanced, then converted back. All intermediate samples remain floating point;
RNNoise receives PCM16-scale floats, not normalized floats or re-quantized shorts.
Resampling does not recreate frequencies absent from the capture.

Gentle retains approximately 50% dry contribution, Strong approximately 25%.
The blend happens in the same delayed spectrum before synthesis, preventing a
timing mismatch between paths. These are blend settings, not guaranteed 6/12 dB
attenuation limits. There is no VAD gate, speaker-selection rule or second denoiser.
An optional quiet-speech leveler is available as described below. The existing
per-ear fitting, compression, feedback guard, master-gain cap and final output
limiters remain downstream.

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
against golden outputs. Android deployment remains future work. The separate workbench now includes an
offline comparison using the complete upstream Python pipeline, not a placeholder
Android backend or a quality ranking based on popularity.

## Implementation update: quality workbench and compatibility

The subsequent implementation adds these pieces without building or executing tests:

- **Local diagnostic recording:** a one-session UI switch, notes, three processing
  taps (four WAV channels), timing/level/route metadata, ZIP export and deletion.
  Disk I/O runs on a separate worker behind a preallocated bounded queue. A full
  queue ends recording without delaying playback. Recordings last at most 30
  seconds and stay in app-private storage excluded from backup.
- **Quiet speech boost:** an opt-in 6/12 dB upward leveler after RNNoise. Its
  confidence-weighted target grows gradually, relaxes when speech ends and is
  constrained by each frame's peak. It never attenuates below unity or gates
  low-confidence audio. Off remains the default; it requires noise reduction.
  Existing fitted gains and final output limiters remain downstream.
- **Compatibility:** SCO attempts 16 kHz then 8 kHz on both older and newer Android.
  Other routes can try 48, 44.1, 32, 24, 16 and 8 kHz, prioritizing mutually
  advertised PCM rates. Unsupported stream creation/start configurations are
  released before trying another. Natural capture can fall back from Unprocessed
  to Voice recognition; it never silently selects Call processing. The selected
  microphone/output must still be verified before processed audio is submitted.
- **Headset discovery:** wait for the input endpoint after connection, handle a
  missing address through an unambiguous matching name, and reject conflicting
  known addresses or indistinguishable candidates. This does not add access to
  private transparency microphones or A2DP microphone capture.
- **Evaluation:** a dedicated [audio-quality workbench](../audio-quality/README.md)
  contains native tests, Kotlin regression tests, a production-linked renderer,
  real-speech mixture generation, reference metrics, blind level-matched listening
  files, diagnostic extraction and explicit acceptance gates. An optional offline
  DeepFilterNet3 backend invokes its complete upstream pipeline with a local model.

The capture ring now reserves at least 100 ms of PCM to absorb short inference
bursts. This is capacity, not an intentional 100 ms playback delay. Sustained
processing slower than capture still needs device profiling. Per-block timings,
output underruns and timestamp-derived capture backlog help expose that problem;
input overrun count and headset firmware remain explicitly unknown where Android
cannot provide them. Notes can carry the firmware and physical test conditions.

### Remaining work, in priority order

1. On a build machine, run the workbench's native tests and the app's Kotlin tests.
   Build debug/release for all configured ABIs, check R8/JNI/model loading and
   16 KB library alignment, and exercise failure/stop/reconnect paths on hardware.
   **No build or runtime test result is claimed yet.**
2. Capture actual headset and phone-microphone recordings at 0.5, 2 and 5 metres
   in quiet, fan noise, babble and a reverberant room. Use identical source
   positions for microphone comparisons; separately assess a phone placed near
   the talker. If speech is missing in the raw tap, prioritize capture hardware
   or placement. Neither model enhancement nor resampling can restore an absent
   captured signal.
3. Run the production-linked Off/Speex/RNNoise/quiet-boost comparisons and optional
   DFN3 challenger on the same real speech. Predeclare acceptance criteria. Use
   blind, level-matched listening and per-condition word/onset retention alongside
   [STOI/ESTOI](https://github.com/mpariente/pystoi). The supplied numeric gate is
   illustrative; its thresholds have not been validated on this headset.
4. Integrate DFN3 on Android only after evaluating its quality advantage: pin the
   model, complete C API implementation and per-ABI libraries, then compare against
   golden offline outputs. No Android DFN runtime is bundled by this update.
5. Use measured levels to tune quiet-speech gain and WDRC fitting. Compare models
   trained with actual room responses and SCO filtering if reverberation dominates.
   Preserve general environmental listening as a separate goal from speech focus.
6. Measure CPU/battery/temperature and capture continuity before changing worker
   scheduling, SIMD dispatch, model size or Oboe I/O. Latency remains secondary to
   intelligibility and uninterrupted capture.

See [the workbench README](../audio-quality/README.md) for file formats, deferred
commands, limitations and an end-to-end evaluation procedure. No build tools or
Python dependencies were installed, and no evaluation code was executed here.
Source-only checks covered Python syntax trees, JSON/XML parsing and diff
whitespace. Kotlin/C++ compilation, Android lint and runtime behavior remain
unverified until the deferred build and test work is performed.
