# Audio quality workbench

All new evaluation code lives here, outside production source directories.
Nothing in this directory builds, installs dependencies, downloads models or
plays audio automatically. **The code and tests have been written, not executed.**

## Layout

- `native/`: adapter and leveler regression tests, plus an offline PCM renderer.
  The renderer links the actual production `NeuralDenoiser` and `SpeechLeveler`.
  It also provides Off and the previous Speex preprocessor as baselines.
- `kotlin/`: framing, downstream limiter integration, route policy, headset
  identity and diagnostic-recording tests. The core-audio test source set includes
  this directory. Existing unrelated module tests retain their original locations.
- `python/audio_quality/`: real-speech corpus creation, replay, metrics, blind
  listening exports, recording extraction and explicit acceptance gates.
- `python/tests/`: tests of the measurement tools themselves. These do not
  establish neural-model quality; the real-speech replay comparisons do that.

## On-device recordings after a future app build

1. In Hearing Assist, enable **Record next session**. Add distance, room and
   headset/firmware information in Notes. This is a one-start setting, not a
   persistent permission to record subsequent sessions.
2. Start assist. Recording ends after at most 30 seconds or on Stop. If the writer
   falls behind, recording ends at the last contiguous block; playback continues.
3. Stop assist and wait for the saved message. **Export latest** opens the Android
   share sheet with a ZIP. **Delete recordings** deletes local sessions and their
   cached exports; copies already exported elsewhere are not affected.

The ZIP contains a four-channel float WAV, `blocks.csv` and `metadata.json`.
Channels are captured mono, enhanced mono, limited left and limited right. These
are synchronous processing-clock taps, not automatically aligned acoustic events.
Enhanced includes neural leveling and bass/presence shaping. Limited channels are
before PCM16 output conversion; classic SCO averages the two limited ears for playback.
The fixed model/resampler delay plus the Kotlin frame adapter is recorded. It
does not include Android/Bluetooth buffering. Final limiter/filter delay is not
removed by the extraction tool.

Diagnostics include input levels/clipping, per-block processing time, Android
PCM rates, selected source, routed device names/types, output underruns and an
estimate of capture backlog from timestamps. Android does not expose a reliable
input-overrun counter here: it is explicitly reported as unknown. A requested or
reported PCM rate is not proof of the negotiated Bluetooth codec bandwidth.
Headset firmware is not available through these audio APIs; use Notes.

Files stay in app-private storage excluded from backup. Recording storage is
bounded by session duration, queue size and a quota checked before recording.
An interrupted process may leave an incomplete session; never evaluate such a
session as a complete capture. The extractor validates WAV/metadata frame counts.

## Deferred build and execution

These commands are instructions for a separate machine with the necessary tools.
They were **not run** during implementation. The native workbench does not need
Android, Gradle or JNI headers:

```bash
cmake -S audio-quality -B /tmp/openhearing-audio-quality -DCMAKE_BUILD_TYPE=Release
cmake --build /tmp/openhearing-audio-quality
ctest --test-dir /tmp/openhearing-audio-quality --output-on-failure
```

The app's Kotlin tests are included in the usual `:core-audio:testDebugUnitTest`
task on an Android build host. Release/R8, JNI, all four ABIs and device routes
still require their own build/device checks. The native CLI does not exercise
Kotlin fitting/WDRC/limiters: use those tests and the recorded post-limiter taps.

Use an isolated Python environment with the packages in `requirements.txt` on the
evaluation machine. No installation script is invoked by this workbench. From
the repository root, supply the package location:

```bash
export PYTHONPATH="$PWD/audio-quality/python"
python -m unittest discover -s audio-quality/python/tests
```

## Real-speech quality comparisons

Provide a mono clean recording and a separate noise recording. Use diverse
talkers, consonants, quiet onsets and pauses; noise should include fans, babble,
handling noise and other actual target environments. Use consented recordings.
Noise must be longer than clean speech plus two seconds; it is not looped with
artificial seams. Omitting noise produces seeded white noise only as a sanity case.

```bash
python -m audio_quality.prepare_corpus --clean /path/speech.wav --noise /path/room.wav --output /tmp/corpus
python -m audio_quality.evaluate /tmp/corpus/manifest.json --renderer /tmp/openhearing-audio-quality/audio_render --output /tmp/comparison
python -m audio_quality.check_report /tmp/comparison/report.json audio-quality/acceptance.example.json
```

Every output directory must be new. Mixtures use declared SNR, speech level and
random seed, with common headroom applied to clean and noisy signals. Source hashes
are recorded. Optional `--rir /path/response.wav` convolves both the speech target
and its contribution to the mixture; those cases evaluate denoising with room
reflections, not removal of reverberation. None of these synthetic mixtures is
labeled as a measured five-metre result.

The native renderer flushes the delayed tail. Evaluation removes its declared
algorithm delay, and rejects length mismatches. Native impulse tests check that
delay at 8, 16, 24, 32, 44.1 and 48 kHz. The evaluator never searches for a new
per-output delay that would improve a model's score.

`report.json` includes SI-SDR change, STOI/ESTOI when installed, noise-only
attenuation, output levels/clipping and processing time. Missing/undefined scores
do not pass an acceptance gate. `acceptance.example.json` contains illustrative
goals, not measured or tuned claims. Set criteria and case coverage **before**
evaluating a change; retain failures per condition.

Blind audio appears under each case's `blind/` directory. Files have matched
active-region RMS and share a common headroom factor. Failed silent outputs
remain silent and fail the acceptance gate. The random assignment is
in `blind-key.json`; keep it from listeners until ratings are collected. Record
word recognition, preference, onset loss, noise pumping and environmental-sound
retention. Lower noise RMS or louder output alone is not a quality improvement.
Unmatched float output WAVs are analytical artifacts and are not playback-limited.

For actual diagnostic recordings:

```bash
python -m audio_quality.extract_recording /path/sound-recording.zip --output /tmp/capture
python -m audio_quality.evaluate /tmp/capture/manifest.json --renderer /tmp/openhearing-audio-quality/audio_render --output /tmp/capture-comparison
```

The raw microphone tap is not a clean reference. Such reports intentionally omit
reference-based intelligibility scores. Extraction produces a delay-aligned raw /
enhanced pair over their shared interval; it cannot restore an unrecorded tail.

## DeepFilterNet challenger

The evaluator can call the full upstream Python pipeline, including feature
normalization, recurrent-state reset, complex filtering and analysis/synthesis.
It requires a compatible local `deepfilternet`/PyTorch/torchaudio installation and
an already-downloaded standard DFN3 directory containing `config.ini` and
`checkpoints/`. The full directory path prevents upstream's model-name download
behavior. No model name, download, placeholder tensor graph or random weights are used.

```bash
python -m audio_quality.evaluate /tmp/corpus/manifest.json --renderer /tmp/openhearing-audio-quality/audio_render --deepfilter-model /path/DeepFilterNet3 --output /tmp/dfn-comparison
```

The evaluator uses the [upstream enhancement API](https://github.com/Rikorose/DeepFilterNet/blob/main/DeepFilterNet/df/enhance.py),
with delay compensation and 12 dB ambience retention, and records package version
and model-file hashes. This is an offline quality challenger, not an Android DFN
implementation or a directly comparable CPU/latency benchmark. Android integration
still requires pinned per-ABI native libraries and golden-output validation.
