# Speech processing

RNNoise and DPDFNet8 enhance the mono microphone signal before per-ear fitting.
Only one engine runs. Noise reduction Off bypasses enhancement; Gentle and Strong
retain about 50% and 25% of the aligned dry signal respectively.

RNNoise runs at 48 kHz with quality-10 SpeexDSP resampling for other capture rates.
Optional quiet-speech boost is limited to 3/6 dB for Gentle/Strong and defaults to
Off. DPDFNet8 uses bundled eight-block models for 8, 16 and 48 kHz; other rates
use the full-band model through resampling. Quiet-speech boost is unavailable
with DPDFNet8 because its API does not expose speech confidence. Initialization
failure ends the session instead of silently switching engines.

DPDFNet8's streaming API ignores offline attenuation, so the adapter mixes the
aligned dry signal explicitly. The pinned model adds approximately 50 ms of delay
before resampling and the 10 ms Kotlin adapter. This is source-derived processing
delay, not measured acoustic latency. Offline replay must flush the tail.

Speech clarity applies a post-compression upper-frequency shelf with matching
output trim. Bass reduction is separate. The feedback guard and final output
limiter remain downstream. Enhancement cannot restore detail removed by headset
firmware or Bluetooth transport.

## Compare engines

1. Fix the talker, microphone placement, profile and comfortable output level.
2. Compare RNNoise and DPDFNet8 with identical noise-reduction settings and
   quiet-speech boost Off. Restart assist after changing engines.
3. Check consonants, quiet word endings, complete sentences and dropouts. Compare
   Gentle if Strong removes quiet speech cues.
4. Use external recordings and the [audio workbench](../audio-quality/README.md)
   for aligned, level-matched replay and STOI/ESTOI measurements.
5. Check model initialization, repeated start/stop, route loss, CPU/heat and
   screen-off continuity on real hardware. Synthetic tests do not prove better
   speech understanding.

See [runtime provenance](../audio-engine/src/main/cpp/vendor/sherpa-onnx/README.md),
[model provenance](../audio-engine/src/main/assets/speech-models/README.md), and
[headset compatibility](HEADSET_MICROPHONE.md).
