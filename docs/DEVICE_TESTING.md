# Device testing

Use an Android phone (API 26+) with wired/USB and Bluetooth headsets. Start at a
low, comfortable volume. Stop immediately if sound is unpleasant. Automated
checks and emulator runs do not validate acoustic loudness, latency or clarity.

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Hearing checks and profiles

- Confirm safety terms and the uncalibrated-estimate notice appear before testing.
- Check left/right routing with headphones, clean tone ramps and volume control.
- Confirm responses change the tone level and advance through both ears.
- Stop/mute mid-tone; output must stop immediately.
- Complete a check, save/select a profile, restart the app and verify persistence.
- Test manual entry, tone preview, profile deletion and the results share preview.

## Live listening

- Compare phone and headset input with the microphones physically separated;
  verify the selected microphone captures the sound.
- Start/stop repeatedly and cancel while connecting. Deny/revoke microphone
  permission, interrupt with a call, and disconnect the headset. Expect silence,
  released routing and a cleared notification; never speaker fallback.
- Test wired/USB, classic Bluetooth on Android 8–11 and 12+, and LE Audio where
  available. Check mono reporting on SCO and per-ear separation on stereo routes.
- Compare processing engines at the same comfortable perceived volume. Check
  quiet words, consonants, distortion, feedback and sudden level changes.
- Measure acoustic delay with external equipment. Check extended playback for
  dropouts, heat and battery drain; PCM rate and block size are not latency measurements.
- Test the saved microphone/settings through the Quick Settings tile.

See [headset routing](HEADSET_MICROPHONE.md) for connection-specific checks and
[speech processing](SPEECH_UNDERSTANDING.md) for engine comparisons.

## Background listening

- Fresh install: battery setup appears once after safety acceptance. Not now and
  Back dismiss it; restarting must not repeat it.
- Open battery settings, change optimization/restriction, then return. The app
  must report the actual OS state, including after cancelling a change.
- Verify unavailable settings destinations fall back without crashing.
- Run assist for an hour with the screen locked and while using other apps.
  Test an OEM with aggressive battery management. Notification Stop must release
  microphone access and the wake lock.

## Media and interface

- Compare Media sound Off, Balanced and Speech clarity with the same track.
  Check boost, distortion, unsupported players and restoration after assist or tones.
- Check sound preview switching for clicks and confirm playback stops on navigation.
- Exercise large fonts, high contrast, dark mode, tablets, system navigation
  insets and the fixed Stop/mute controls.
- Repeat critical flows in the minified release build on API 26, a current Android
  version, and a 16 KB page-size device where available.

Record device/OS/headset, build version, settings, reproduction steps and results.
Report unsafe loudness through [SECURITY.md](../SECURITY.md). Complete
[calibration](CALIBRATION.md) before making loudness claims.
