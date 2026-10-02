# Headset microphone hearing assist

Hearing assist offers **Phone microphone** and **Headset microphone**.
Choose the source before starting; the choice is saved and also used by the
quick-settings tile. For remote listening, choose **Phone microphone** and place
the phone near the sound source; there is no separate remote-microphone mode.

With headset input, the signal path is:

```
Headset microphone -> optional RNNoise/DPDFNet8 + bass shaping
                  -> per-ear fitted gain + compression + speech shelf + feedback guard
                  -> output limiter -> same headset speakers
```

The phone must remain connected and run EarCast. This does not install a
standalone hearing-aid program on the headset or require an AirPods protocol.
Only microphones exposed by Android are available; private ANC/transparency
microphone feeds cannot be selected through these APIs.

## Use

1. Pair and connect the headset in Android settings. For classic Bluetooth,
   enable its call audio option as well as media audio.
2. Open Hearing assist with a saved hearing profile.
3. Select **Headset microphone** or **Phone microphone**, then tap **Start assist**.
4. Allow microphone access. Wait for **Connecting audio…** to finish.
5. The screen shows the routed microphone name once both input and output are
   verified, and identifies classic Bluetooth mono call audio. Adjust amplification as usual.
6. Stop assist before changing microphones. Use **Phone microphone** if the
   headset does not provide a compatible two-way connection, or when using the
   phone as a remote microphone.

The service continues with the screen off. Stop is available during connection,
on the assist screen, in the notification, and through the tile. Losing the
selected route or audio focus ends the session; reconnect and explicitly restart.
Connection failure is shown on the screen rather than silently using the phone mic.

## Capture and processing

Natural requests `UNPROCESSED` when supported, otherwise `VOICE_RECOGNITION`.
Call mode requests `VOICE_COMMUNICATION`. Headset firmware can still suppress
surrounding speech before Android receives it; processing cannot restore removed
information. Compare phone input near the talker when headset pickup is poor.

Noise reduction runs before per-ear fitting. Speech clarity follows compression;
bass reduction is separate. The feedback guard and output limiter remain downstream.
See [speech processing](SPEECH_UNDERSTANDING.md) for engine behavior and comparisons.

## Compatibility and latency

| Connection | Implementation | Limits |
|---|---|---|
| Wired headset / USB headset | Explicit microphone and matching output preference | Requires an input device exposed by Android |
| Classic Bluetooth HFP/SCO, Android 12+ | Communication device selection; tries 16 then 8 kHz mono I/O | Call bandwidth, mono playback; headset/phone support varies |
| Classic Bluetooth HFP/SCO, Android 8–11 | Wait for SCO connection; tries 16 then 8 kHz mono I/O | Actual bandwidth varies; call audio must be available outside phone calls |
| Bluetooth LE Audio headset, Android 12+ | Communication device selection when exposed by Android | Requires compatible phone, headset and OS; stereo is not guaranteed |
| A2DP-only headphones / output-only hearing aids | Phone microphone mode | No microphone uplink to process |

Non-SCO routes prefer mutually advertised rates, with fallbacks among 48, 44.1,
32, 24, 16 and 8 kHz. Natural capture can retry voice-recognition capture if raw
capture cannot start; it does not silently select Call processing. Device matching
accepts missing addresses only when the remaining identity is unambiguous.

The listed PCM rates are app I/O configurations, not measurements of the negotiated
Bluetooth codec or acoustic bandwidth. A 16 kHz stream can carry audio that the
headset/phone negotiated at a narrower bandwidth.

Classic Bluetooth and devices advertising mono-only output process both ear profiles and average their limited outputs
for mono playback. It cannot deliver independent left/right gain to the ears on
that route. Unsupported EQ bands above the selected rate's Nyquist frequency are
omitted by the existing equalizer. Device I/O uses PCM16 and DSP uses float samples.

Assist retains approximately 4 ms processing blocks (192 frames at 48 kHz,
64 at 16 kHz, 32 at 8 kHz), an urgent audio worker, and a low-latency AudioTrack
request. Capture buffers use the largest of Android's minimum, two blocks and
100 ms of mono PCM to tolerate short scheduling stalls. Playback buffers use the
larger of Android's minimum and two blocks. **Block duration is not end-to-end
latency.** Radio transport, headset firmware, Android resampling/buffering and
the DSP limiter all add delay. No measured latency or universal compatibility is
claimed. Wired connections are the first hardware baseline to measure.

## Hardware validation (required before claiming device support)

- Verify phone mode still captures near the phone, including with a wired mic attached.
- Verify headset mode captures near the headset with the phone across the room.
  Check both physical microphones independently; a displayed name alone is insufficient.
- Exercise classic Bluetooth on Android 8–11 and 12+, wired/USB, and LE Audio where available.
- Disable Bluetooth call audio or use an output-only device: expect an actionable
  failure, no amplified speaker output and no fallback microphone.
- Disconnect during connection and active playback. Cancel connection, deny/revoke
  microphone permission, receive a call, and repeatedly start/stop. Verify silence,
  released call routing/audio focus, correct UI state and removal of the notification.
- Switch back to phone input and verify normal media playback after stopping.
- Verify screen-off operation and the tile's saved-source behavior.
- Test asymmetric profiles: classic SCO must report mono and play the combined
  result; confirm actual left/right separation separately for wired/LE output.
- Measure acoustic round-trip delay with an external impulse/recording setup for
  each phone/headset pair, and listen for dropouts during extended screen-off use.

JVM tests cover the rate/block policy, limited mono mix, DSP stability at SCO
rates and saved selection. They do not validate Android's actual routing or radio latency.

## Platform references

- [Android communication routing guide](https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager)
- [AudioManager SCO restrictions](https://developer.android.com/reference/android/media/AudioManager#startBluetoothSco())
- [LE Audio recording](https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-recording)

Routing uses AudioManager with the existing `MODIFY_AUDIO_SETTINGS` permission.
No scanning, account, root access or new network permission is needed. Audio is
streamed locally between the connected headset and phone and discarded after
processing. The app does not record or export microphone audio. See the
[audio quality workbench](../audio-quality/README.md) for evaluation of externally
prepared WAV files and archives exported by older versions.
