# Headset microphone hearing assist

Hearing assist now offers **Phone microphone** and **Headset microphone**.
Choose the source before starting; the choice is saved and also used by the
quick-settings tile. For remote listening, choose **Phone microphone** and place
the phone near the sound source; there is no separate remote-microphone mode.

With headset input, the signal path is:

```
Headset microphone -> optional denoise + bass/presence shaping
                  -> per-ear fitted gain + three-band compression + feedback guard
                  -> output limiter -> same headset speakers
```

The phone must remain connected and run OpenHearing. This does not install a
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

## Sound clarity (CMF Buds 2 / OnePlus Nord 3 starting point)

Start with **Natural** capture, **Gentle** speech clarity, **Gentle** bass reduction,
and noise reduction **Off**. These are the defaults; settings are saved and used
by both the assist screen and quick-settings tile. Stop assist to change them.
Try Strong speech clarity only if Gentle still sounds dull, and Off bass reduction
if voices sound thin. Use Call mode if the phone cannot route Natural capture.

Natural requests Android's `UNPROCESSED` source when the platform advertises it,
otherwise `VOICE_RECOGNITION`. Call mode requests `VOICE_COMMUNICATION`.
Android's raw-source capability flag is not proof that a Bluetooth microphone is
unprocessed: headset firmware can still alter it before Android receives it.

The CMF Buds 2 microphone is designed for calls. Nothing describes its Clear Voice
Technology as reducing ambient noise while recording voice. **Inference:** this can
work against listening to other people around the wearer; those sounds may already
be suppressed before OpenHearing receives them. Neither EQ, denoising nor changing
the phone's audio library can recover information removed at capture.
The app cannot access the buds' private transparency/ANC microphone feeds.
For surrounding speech, compare the phone microphone placed near the talker, which
also avoids the classic Bluetooth two-way call playback path.

- [Nothing's CMF Buds 2 microphone description](https://iq.nothing.tech/en/products/cmf-buds-2)
- [Android raw-capture guidance](https://developer.android.com/media/platform/mediarecorder)

## Processing and verification

Speech clarity is a broad +3 or +6 dB presence filter near 2.4–2.5 kHz. Bass
reduction is a -6 or -12 dB low shelf at 450 Hz, applied to all sound; it does not
identify or cancel the wearer's voice. Neither control creates missing bandwidth.

Each ear now uses independent three-band WDRC envelopes, split at 700 and 2400 Hz
with phase-aligned fourth-order Linkwitz-Riley crossovers. Loud bass compresses
the bass band without applying the same attenuation to higher speech detail.
The existing -35 dBFS threshold, 3:1 ratio and soft knee remain digital level
settings, not a calibrated hearing-aid prescription. The low-band release is
120 ms; the speech bands retain 80 ms release and 5 ms attack. The fitted EQ is
upstream; the feedback guard, master cap and final lookahead limiter remain downstream.

Optional RNNoise neural enhancement runs once on the mono microphone signal,
before per-ear fitting. Gentle retains about 50% dry contribution, Strong about
25%, blended in the same delayed spectrum before synthesis. These are not hard
attenuation limits. The full model runs at 48 kHz; quality-10 SpeexDSP resampling
adapts 8/16/24/32/44.1 kHz capture without recreating missing bandwidth. Optional
Quiet speech boost adds up to 6 or 12 dB when the model reports speech, with
slow gain changes and peak headroom control. It defaults to Off and requires
noise reduction; it never gates the signal. The final output limiter remains downstream. The adapter buffers 10 ms frames across capture block boundaries;
model and resampler delays are additional. Off bypasses this stage without frame
delay. The worker releases native state on stop, connection or capture failure.
The old Speex denoiser remains a source-level comparison baseline. See
[audio clarity research and next steps](AUDIO_CLARITY_NEXT_STEPS.md) for the library
comparison, deferred tests and the five-metre recording protocol.

JVM signal tests check flat crossover reconstruction, preservation of a quiet
high-frequency tone alongside loud bass, block continuity/reset, filter response,
denoiser frame ordering and final output ceilings at 8, 16 and 48 kHz. Synthetic
tests establish DSP behavior, not subjective intelligibility or device compatibility.
CMF Buds 2 / Nord 3 listening validation is still required.

Oboe remains a possible future I/O improvement. Its purpose is high-performance,
low-latency Android streams; it does not replace speech processing or change the
Bluetooth headset's microphone bandwidth or firmware suppression.
See [Google Oboe](https://github.com/google/oboe),
[Linkwitz crossover design](https://www.linkwitzlab.com/crossovers.htm), and the
[RBJ biquad reference](https://www.w3.org/TR/audio-eq-cookbook/).

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
These source changes still require hardware validation.

The listed PCM rates are app I/O configurations, not measurements of the negotiated
Bluetooth codec or acoustic bandwidth. A 16 kHz stream can carry audio that the
headset/phone negotiated at a narrower bandwidth.

Classic Bluetooth and devices advertising mono-only output process both ear profiles and averages their limited outputs
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

- Verify phone mode still records near the phone, including with a wired mic attached.
- Verify headset mode records near the headset with the phone across the room.
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
streamed locally between the connected headset and phone. Saving is off by default.
The optional Sound comparison recording control records one session for up to
30 seconds in private local storage; Export opens the Android share chooser and
Delete removes local recordings and cached exports. Nothing uploads automatically.
See the [audio quality workbench](../audio-quality/README.md) for extraction,
reference-based evaluation and blind listening instructions.
