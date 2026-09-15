# Media buzz and microphone distortion

## Follow-up: independent media boost

The cut-only update below made media too quiet for the user, while assist now
sounds acceptable. Media therefore has a separate **Media boost** slider in
Settings: **0–9 dB, default +6 dB**, persisted independently of assist settings.
This replaces the fixed −3 dB input attenuation. The relative EQ curve remains
unchanged; boost runs before the platform limiter, with no post-limiter makeup.
Already-loud peaks may be limited rather than receiving the full requested gain.

Boost changes update the existing effect after releasing the slider, avoiding
effect recreation during adjustments. The latest boost is restored after an app
playback bypass. Hearing Assist DSP and microphone settings are unchanged.
Device support still varies, so confirm the result with the same media track.

The sections below describe the preceding distortion fix and its measurements.

## Report and confirmed code issues

The user reports buzzy/muffled music and video with **Media EQ enabled**, similar
artifacts through the phone microphone, and much poorer speech understanding
through CMF Buds 2 microphones despite good normal calls.

Two app-side problems were found:

1. Media EQ independently clamped every prescribed band to 0–12 dB. High-gain
   profiles could become nearly flat +12 dB amplification, losing their tonal
   contrast while driving the platform limiter. It also remained attached to the
   global output mix during the app's already-processed microphone playback.
2. The Kotlin limiter calculated gain from the current input while outputting a
   delayed sample. It could release before a buffered peak left the delay, relying
   on the final hard clamp. A settled 100 Hz tone at 8 kHz, amplitude 2.0 and ceiling
   0.25, produced **6.863% residual waveform distortion** after fitting/removing
   the fundamental. This is a synthetic regression measurement, not a measurement
   of the user's recording or proof of its sole cause.

## Changes

- Media EQ subtracts a common peak reference across both ears **before** limiting
  the range to −12..0 dB. High-loss spectral differences survive; no band boosts
  mastered media. Both channels receive 3 dB of input headroom before EQ. The
  platform limiter remains enabled.
- A shared, synchronous playback lease releases global Media EQ before microphone
  playback, demo audio, or test/preview tones start. Nested players keep it paused
  until all have stopped. Changes made while paused apply on restoration; turning
  it off while paused prevents restoration. Unchanged profiles do not recreate
  the effect on every volume-slider update.
- The Kotlin limiter tracks the largest buffered peak with a preallocated queue,
  holding at least 10 ms of peaks and recovering smoothly. The existing 2 ms
  output delay remains. Peak holding prevents premature release and repeated
  waveform flattening. Non-finite input is silenced without poisoning the state.
- Diagnostics identify the new limiter and the media-effect bypass.

No new speech model, amplitude gate, or speech/non-speech mute was added. Media
EQ is tonal shaping; it does not detect or separate speech in music and video.

## Sibling project inspection

`../AudioApp` was inspected as requested. Its simple noise reducer zeros samples
below a threshold, changing the waveform itself. Its FFT path reads PCM16 values
as doubles without normalizing, then treats reconstructed values as normalized
audio and multiplies by `Short.MAX_VALUE` again. It also processes just one fixed
FFT frame from a larger input block and lacks consistent hop-size handling.
These implementations are unsuitable as replacements for the current processing.

## CMF Buds 2 and calls

[Nothing's product documentation](https://in.nothing.tech/products/cmf-buds-2?Colour=Light+Green)
states that Clear Voice Technology reduces microphone ambient noise during calls
and voice recording. This makes clear pickup of the wearer compatible with reduced
pickup of surrounding speakers; it does not establish that the buds are defective.
Android's classic headset path also differs from media playback. Requested PCM
sample rate does not establish the negotiated codec or recover missing bandwidth.

[Android's DynamicsProcessing documentation](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing)
describes input gain before EQ and the downstream limiter. Global-session behavior
still depends on the phone; JVM tests cannot validate an OEM's audio effects.

## Validation and next device comparison

The core audio tests, app Kotlin compilation, Kotlin formatting and static
analysis pass. The distortion regression covers 100, 500, 1000 and 3000 Hz at
8, 16, 24, 32, 44.1 and 48 kHz; each settled residual is below **0.2%**. Existing
limiter ceiling, route, stereo, reset and processing-boundary tests also pass.
Media tests check preserved high-loss contrast, ear balance, nested bypass,
settings changes during playback, and failure/retry behavior.

This does not prove better speech recognition on the headset. After installing
the new build, compare the same music/video with Media EQ off and on. Then compare
phone and headset microphones with Noise Reduction Off first, holding talker
position and listening level constant. If only headset capture remains muffled,
record a diagnostic session with the same talker: the raw microphone tap separates
capture/firmware problems from app enhancement problems. Compare Gentle neural
reduction afterward; louder output is not evidence of clearer speech.
