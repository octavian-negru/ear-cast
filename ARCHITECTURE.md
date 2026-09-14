# Architecture

OpenHearing is a multi-module Kotlin/Android app following MVVM + clean layering.
The guiding principle: **the safety-critical and signal-processing logic lives in
Kotlin/JVM DSP classes and a native speech-processing library, decoupled from
Android audio/BLE I/O, so the algorithms can be tested without a device or emulator.**

## Modules and dependency direction

```
                 ┌─────────────┐
                 │     :app     │  Compose UI · Hilt · navigation · onboarding
                 └──────┬──────┘
        ┌───────┬───────┼────────┬─────────────┐
        ▼       ▼       ▼        ▼             ▼
 :core-audiogram :core-audio :airpods-protocol :data
        │       │       │        │             │
        └───────┴───────┴────────┴─────────────┘
                        ▼
                  :core-common   units · SAFETY constants
```

- `:app` depends on the cores; **no core depends back on `:app`.**
- Everything depends on `:core-common`; `:core-common` depends on nothing app-specific.
- `:core-common` and `:core-audiogram` are plain Kotlin/JVM modules (fast JUnit5 tests).
- `:core-audio`, `:airpods-protocol`, `:data` are Android library modules (they
  touch Android audio/BLE/persistence APIs) but keep their core logic pure where possible.

### `:core-common`
Strongly-typed units (`Hertz`, `DecibelsHl`, `DecibelsSpl`, `DecibelsFs`, `Ear`)
and **`SafetyConstants`** — the single source of truth for every output-loudness
limit. Anything that produces sound must respect these.

### `:core-audiogram`
The audiogram domain: the `Audiogram`/`Threshold` model, the pure-tone
threshold-seeking staircase (Phase 1), and audiogram→gain-curve fitting (Phase 1).
Pure Kotlin — no Android dependency.

### `:core-audio`
The real-time DSP core: multiband gain, wide dynamic range compression (WDRC),
feedback/howl guard, and the **SAFETY-CRITICAL output limiter**. The DSP math is
pure Kotlin behind the `AudioEngine`/`AudioProcessor` interfaces; the concrete
AudioRecord/AudioTrack engine is just the I/O shell. This is what makes the limiter
unit-testable.

`AssistAudioRoute` owns microphone/output selection, audio focus, and Bluetooth
communication routing (API 31+ device selection; legacy SCO below API 31).
`AndroidAudioEngine` confirms actual input/output routes before submitting
processed audio, reports connecting/running/failure state, and releases routing
on every exit. The processor factory receives the transport's sample rate before
building DSP. Classic SCO uses mono device output, with a bounded average of the
two limited ear channels. The microphone preference is persisted in `:data` and
shared by the assist screen and tile; choosing the phone microphone provides the
remote-listening use case inside the same Hearing Assist session.
See [headset microphone routing](docs/HEADSET_MICROPHONE.md) for platform limits
and required hardware validation.

Optional speech enhancement uses either the bundled full RNNoise model or full
DPDFNet8 models through the pinned sherpa-onnx C API, before per-ear processing.
Both have worker-owned native state; DPDFNet has a separate JNI library and uses
bundled model files verified before loading. Stateful SpeexDSP resampling adapts Bluetooth capture rates
to the model's 48 kHz input; the original and enhanced spectra are mixed with
matching delay to retain ambience. Optional speech-confidence-based upward gain
raises quiet speech with RNNoise before the per-ear chains and output limiters.
Speech clarity is a high shelf after WDRC, preventing compression from undoing
the requested consonant lift. Feedback protection and final limiting follow it.
An opt-in bounded recorder copies processing taps to a separate writer thread;
normal audio processing performs no diagnostic file I/O. See [audio clarity next steps](docs/AUDIO_CLARITY_NEXT_STEPS.md)
for the library comparison and pending quality validation; the current focus is
[speech understanding](docs/SPEECH_UNDERSTANDING.md).

### `:airpods-protocol`
AirPods Pro 2/3 detection, battery/state, and transparency routing over BLE /
L2CAP CoC. The protocol is **reverse-engineered and UNVERIFIED** (see
[docs/PROTOCOL.md](docs/PROTOCOL.md)); everything protocol-specific is behind
interfaces. Non-root path first.

### `:data`
Persistence for audiograms, profiles, and settings (DataStore/Room, Phase 4).

## Data flow

```
 Pure-tone screening ─► Audiogram ─► Gain curve / fitting ─► DSP chain ─► AudioEngine ─► earbuds
 (:core-audiogram)      (:core-     (:core-audiogram)        (:core-      (:core-audio)
                         audiogram)                           audio)            │
                                                                                ▼
                                                          (optional, best-effort) :airpods-protocol
                                                          tunes transparency/route — never required
```

The DSP chain's **final stage is always an `OutputLimiter`**, so nothing can
exceed the safety ceiling on the way to the device, regardless of upstream gain.

## Testing strategy

- **Pure-Kotlin modules** (`:core-common`, `:core-audiogram`): JUnit5 unit tests,
  no Android. This is where the screening, fitting, and safety-math tests live.
- **Android library modules**: JUnit5 unit tests (via the `android-junit5` plugin)
  for pure logic; Robolectric/instrumented tests for Android-touching code.
- **`:airpods-protocol`**: can only be partially unit-tested; the protocol itself
  is validated on real hardware using the scripts in `docs/PROTOCOL.md`.

New clarity, route-policy and diagnostic tests live together in
[`audio-quality/`](audio-quality/README.md): native tests link production DSP, JVM
tests are wired into `:core-audio`, and Python tools prepare real-speech corpora,
evaluate reference metrics and export blind listening comparisons. The suite never
automatically compiles a renderer or downloads a model.

## Tech stack

Kotlin · Jetpack Compose + Material 3 · MVVM + clean layering · Hilt · coroutines/Flow ·
AudioRecord/AudioTrack behind an interface · Gradle Kotlin DSL + version catalog · JUnit5 +
Turbine + Robolectric · detekt + ktlint. minSdk 26, compile/target SDK 35.
