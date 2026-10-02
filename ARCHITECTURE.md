# Inside EarCast

EarCast connects an editable audiogram to two listening features: microphone
assist and media playback effects. This guide describes the code currently
included by [settings.gradle.kts](settings.gradle.kts), with entry points for
following each operation.

## A chart becomes a saved profile

The Compose navigation has four destinations: Home, Audiogram, Listen and
Settings. `ProfileSetupScreen` offers a guided tone check and manual entry.
`AudiogramCard` displays the selected profile on Home and the Audiogram screen.

The manual editor uses `ProfilePlotEditor` and `ProfileChart` to modify thresholds
for a selected ear and frequency. Chart gestures and slider edits update the same
values in `ProfileEditorModel`; preview tones are handled separately from saving.
The guided check instead collects responses through `ToneCheckStateModel`, using
the threshold-search logic in `sound-profile`.

Both routes produce a `HearingCurve`: a collection of `HearingPoint` values,
each containing an ear, frequency and hearing level. `ProfileStorage` exposes
saved profiles and the active selection as flows. Its DataStore implementation
encodes the profile list in preferences using the hearing-curve codec. Settings
such as microphone choice, comfort ceiling and noise-reduction engine use
`PreferenceStorage` in the same persistence module.

The chart's displayed hearing levels must not be confused with calibrated output
sound pressure. The tone-check path uses consumer headphones without a clinical
calibration. [Fitting](docs/FITTING.md) and
[calibration](docs/CALIBRATION.md) document those constraints.

## Starting a microphone session

`LiveSessionBuilder` is shared by the listening screen and quick-settings tile.
It checks consent and the active profile, fits a gain curve for each ear, applies
the selected environment preset, and reads the comfort and microphone settings.
If only one ear can be fitted, that curve supplies both channels.

`LiveAudioController` coordinates the session with `LiveAudioService`, which
owns the foreground notification and screen-off listening lifecycle.
`AndroidStreamEngine` exchanges PCM samples with Android's `AudioRecord` and
`AudioTrack`. `LiveAudioRoute` handles focus and device selection, including
communication-device routing on newer Android versions and legacy Bluetooth SCO.

Processing is constructed for the selected transport's sample rate. The engine
checks the actual capture and playback routes before sending processed audio and
reports connection or failure state to the controller. Route cleanup is part of
session shutdown. The [routing guide](docs/HEADSET_MICROPHONE.md) lists the device
cases that still need physical testing.

## What happens to a microphone block

Input enhancement operates before the separate ear channels. A session selects
one of the bundled RNNoise, DPDFNet8, SpeexDSP or Adaptive Wiener implementations;
turning noise reduction off bypasses that enhancement. JNI bridges connect the
Kotlin frame adapter to native processing. Neural engines adapt capture rates to
their model rate; DPDFNet8 loads bundled models through sherpa-onnx. RNNoise can
also apply quiet-speech boost.

After input enhancement, `StereoListeningChain` runs a `MonoListeningChain` for
each ear. Each chain applies these operations in order:

1. Optional high-pass filtering for the selected preset.
2. Equalization derived from that ear's `FrequencyGainCurve`.
3. Multiband dynamic-range compression.
4. Optional speech-presence filtering.
5. Feedback suppression.
6. Smoothed master gain and compensation for the presence filter's added gain.
7. `PeakLimiter` enforcement of the configured digital ceiling.

The stereo processor reuses scratch buffers and handles larger buffers in chunks.
Live volume changes update both ear chains. Classic SCO playback averages the
limited ear channels into mono because the transport cannot carry independent
left and right outputs.

`AudioLimits` in `foundation` supplies shared bounds, and the per-ear limiters
provide the last processing step before transport conversion. These are digital
signal constraints; headphone sensitivity and device volume still affect
physical loudness. The app has no microphone-recording path: live audio remains
in memory for immediate playback.

## Media playback takes a separate route

Home's Media sound controls feed `MediaSoundController` and the Android effect
session in `audio-engine`. `MediaCurvePlanner` maps the hearing profile to that
path's settings. Balanced and Speech clarity modes provide different dynamics
spectral headroom budgets, with an additional quiet-sound boost. Both modes use
four compression bands followed by the per-ear profile and a linked limiter;
boost changes ramp on the existing effect. The [amplifier design](docs/MEDIA_AMPLIFIER.md)
documents the transfer law, regression results and RMS limiter limitations.

This feature uses Android playback effects rather than the microphone session's
native speech engine. Device and player support determine whether an effect can
be attached. Changes here should be evaluated separately from microphone DSP.

## Code ownership and dependencies

| Directory | Responsibility | Internal dependencies |
| --- | --- | --- |
| `app` | Compose screens, state models, dependency injection, session service, media controls | All four library modules |
| `sound-profile` | Hearing thresholds, screening protocols, curve encoding and fitting | `foundation` |
| `audio-engine` | Signal generation, Android streams and routes, Kotlin DSP, JNI and native speech engines | `foundation`, `sound-profile` |
| `local-storage` | Profile selection and preference persistence with DataStore | `foundation`, `sound-profile` |
| `foundation` | Unit types and common audio bounds | None |
| `audio-quality` | Offline renderers, native regression checks and Python evaluation tools | Links or exercises production processing; not an app module |

`foundation` and `sound-profile` build as Kotlin/JVM libraries. `audio-engine` and
`local-storage` are Android libraries. The app uses Compose, coroutines and Hilt;
the build uses Java 17, compile SDK 37, target SDK 36 and min SDK 26.
Native sources and third-party notices are under `audio-engine/src/main/cpp`;
bundled speech-model information is under
[audio-engine/src/main/assets/speech-models](audio-engine/src/main/assets/speech-models/README.md).

## Boundaries for verification and data

Profile and DSP changes have local Kotlin tests. Route policy checks cover the
selection rules, but cannot demonstrate behavior on a particular headset.
`audio-quality` adds native processing checks and replay of prepared speech/noise
recordings; its Kotlin cases are included in the audio-engine test source set.
The [workbench guide](audio-quality/README.md) documents commands, dependencies
and the limits of each measurement.

Audiograms and settings are stored locally. `ProfileImageExporter` prepares an
image for the explicit share-preview flow. All builds are free of billing and
advertising SDKs. See [privacy](docs/PRIVACY.md) for the user data
policy and the [Play checklist](docs/GOOGLE_PLAY.md) for release prerequisites.
