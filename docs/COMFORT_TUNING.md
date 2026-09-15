# Gentler gain and speech contrast

The subsequent [media buzz fix](MEDIA_BUZZ_FIX.md) addresses the independent
media-EQ path, double processing, and measured limiter distortion.

This update addresses the report that amplification remains aggressive across
settings and that speech clarity is still limited.

## Processing changes

- Fitted EQ now compensates for neighbouring filters' contributions. Previously,
  every peaking filter applied its full target, so their overlapping responses
  added excess gain before compression. A bounded setup-time fit now approaches
  the requested levels at measured frequencies. Between-frequency response remains
  approximate, especially for sharply varying or closely spaced measurements.
- Speech Clarity keeps the 3/6 dB Gentle/Strong upper-to-lower frequency contrast
  after WDRC, with a matching 3/6 dB output trim. The high band stays near its
  Clarity Off level while lower frequencies are softened. This avoids the previous
  extra treble amplification. Feedback detection still sees the untrimmed shelf;
  compensation is applied after the guard and before the final limiter.
- Conversation's additional fitted speech-band gain is reduced from 4 to 2 dB.
- RNNoise Quiet Speech Boost is limited to 3/6 dB for Gentle/Strong, previously
  6/12 dB. Off remains the default.
- The volume control now spans −12 to +40 dB and displays signed values. New
  profiles start at +6 dB instead of +20 dB. Saved volume choices are preserved.
- Master gain fades in from silence and follows live changes with sample-based
  smoothing: a 60 ms rise time constant and a faster 15 ms fall time constant.
  About 95% of an increase is reached in 180 ms. The output limiter remains last.

These changes apply to both microphone sources and both speech engines. They do
not change the neural models or recover bandwidth missing from captured audio.
Diagnostics identify the EQ revision and record the clarity trim and boost cap.

## Verification and listening comparison

Regression coverage checks EQ response and clarity contrast at 8, 16, 24, 32,
44.1 and 48 kHz, negative volume, smooth gain transitions, reset and block
continuity, stereo processing, feedback protection and the final output ceiling.
Synthetic response checks do not establish improved word recognition.

Validation completed on this workspace: `:core-audio:testDebugUnitTest`,
`:data:testDebugUnitTest`, `:core-common:test`, `ktlintCheck` and `detekt` for
the changed modules, and `:app:assembleDebug`, all using the installed offline
toolchain. The APK is `app/build/outputs/apk/debug/app-debug.apk`. Device listening
and real-speech intelligibility comparisons have not been performed.

After installing the updated build, restart assist to apply the new processing.
Existing profiles retain their saved volume, so begin with Volume adjustment at
0 dB and lower it if needed. Compare Clarity Gentle and Strong with the same
talker, microphone position and comfortable perceived volume. Keep Quiet Speech
Boost Off for the first comparison; assess word endings, consonants, boominess
and sudden changes in level. The existing diagnostic recordings can capture the
same conditions for the [audio quality workbench](../audio-quality/README.md).
