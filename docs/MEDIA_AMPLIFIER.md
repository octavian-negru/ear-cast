# Spectral headroom amplifier

Media sound now uses four coordinated compression bands in both Balanced and
Speech clarity modes. This replaces commit `657f148`'s broadband Balanced
compressor and independently targeted Speech clarity compressors. It runs in
Android's existing native DynamicsProcessing effect; no new JNI library, media
capture permission or audio rerouting is involved.

The processing order is unity input gain → spectral compression and boost →
per-ear profile cuts → linked final limiter. Moving the profile after compression
matters: a 9 dB interaural adjustment before a 3:1 compressor can shrink to 3 dB.
After compression, that adjustment survives for identical input to both channels.
Different stereo content can still produce different band gains: Android does
not expose stereo linking for the multiband compressors. Only the limiter links.

## Transfer law

The bands end at 250 Hz, 1 kHz, 4 kHz and 20 kHz. Their power shares are
`[0.25, 0.25, 0.30, 0.20]` for Balanced and `[0.125, 0.25, 0.425, 0.20]` for
Speech clarity. Both sum to one. Speech clarity gives the voice band more room
when compressing loud material; quiet material gets the same gain in every band.
These are initial design weights, not a validated intelligibility prescription.

For requested boost `G`, band share `w` and compression range `D = 12 dB`:

```
threshold = 10 log10(w) - G - D
ratio     = 1 + G/D
makeup    = G
```

A 6 dB quadratic knee joins unity gain to compression with continuous slope.
Above the knee, the settled power transfer simplifies to
`Pout = w × (Pin/w)^(1/ratio)`. Since the exponent is at most one, concavity
and the sum of the shares bound total output power to one whenever total input
power is at most one. A soft knee only reduces this upper bound. Relative profile
cuts cannot add power. This accounts for the band sum before the final limiter;
the previous design gave every band its own near-full-scale target.

At zero boost the band stage is exactly unity, including through the knee.
Its ratio changes continuously from 1:1 to about 3.08:1 at +25 dB, instead of
reaching 10:1. Inputs at or below −50 dBFS receive the entire requested gain in
both modes. Higher levels trade some boost for retained level contrast and
headroom; +25 dB is not a promise to add 25 dB to already-loud media.

Attacks are 1.5 ms on the platform's frame RMS detectors. Releases are
240/180/120/100 ms from bass to treble. A slower bass attack let its initial
overshoot engage the broadband limiter and duck voices; the transient regression
caught this. Gain changes advance at most 0.5 dB per 10 ms callback, or 1 dB
downward. Startup ramps from zero, new targets replace pending targets, and
releasing the effect cancels its callbacks. A delayed callback cannot catch up
with one large jump.

## Limiter and platform limits

The final linked limiter uses a −6 dBFS RMS threshold, 100:1 ratio, 1 ms attack
and 100 ms release. The extra 4 dB below the previous −2 dB setting reserves
room for waveform crests. Loud, already-mastered material can consequently be
quieter than before, including at zero boost. This is deliberate headroom,
not a loudness-normalized comparison.

The [AOSP frequency-domain implementation](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/libeffects/dynamicsproc/dsp/DPFrequency.cpp)
detects RMS in both MBC and limiter. Its limiter is **not** a sample clamp or an
oversampled true-peak limiter. Crest factor, attack/release history and FFT
reconstruction can exceed the nominal threshold. Neither the power proof nor
the added reserve guarantees a sample/true-peak ceiling. Native custom peak
processing would require owning the PCM route; an ordinary app's C++ library
cannot be inserted into other apps' global playback path.

The [Android stage API](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing)
provides the native backend. Global session support still depends on the device
and player. No claim of worldwide best quality, universal clipping prevention or
improved word recognition follows from this implementation.

## Verification

`MediaDynamicsQualityTest` exercises the production plans with an independent
band-envelope reference of the platform transfer law. It checks quiet gain,
zero-boost continuity, knee slope, maximum-boost contrast, and the summed-power
bound over 404 spectra at all 251 tenth-dB settings in both modes.

The bass-burst fixture has a −32 dBFS vocal band and a bass band that jumps from
−45 to −6 dBFS, at +20 dB boost. The old broadband curve's settled vocal ducking
is 17.60 dB; the new reference's worst vocal ducking during the 500 ms burst,
including its final limiter, is 0.63 dB. This comparison isolates cross-band
gain modulation. It is **not** a PCM rendering, device measurement or matched
loudness listening result. At +25 dB, a 12 dB input-level difference above the
knee retains 3.89 dB instead of the old curve's 1.2 dB, before final limiting.

The Android configuration tests inspect actual framework parameter objects to
verify post-EQ placement, independent ear cuts, a common limiter group, zero
startup boost, and live parameter mapping. They do not play audio or measure
the native effect. Session/ramp tests cover gain changes, asynchronous failure,
restoration, bypass leases and invalid controls.

Validation on 2026-10-02: repository `test`, `ktlintCheck` and `detekt` passed,
as did debug APK assembly and release lint. The audio-engine debug suite ran
132 tests with no failures; both framework configuration tests passed on an
Android 15 (API 35) emulator with audio disabled.

```sh
./gradlew :audio-engine:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

For a listening comparison, retain a build of `657f148`, use the same profile,
and match measured playback loudness externally. Include quiet speech over bass,
percussion, sustained bass, bright sibilants, dense music, asymmetric stereo and
abrupt silence/loud transitions. Capture output externally and measure sample
peaks, oversampled peaks, distortion and left/right response at 0/6/15/25 dB.
Also check clicks while changing boost, switching modes and restoring after app
playback. Those physical-device and blind-listening comparisons remain pending.
