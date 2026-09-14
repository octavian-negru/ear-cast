"""Extract app diagnostic taps without treating microphone audio as clean ground truth."""
import argparse
import io
import json
from pathlib import Path
import zipfile

import numpy as np
from scipy.io import wavfile


def extract(archive, output):
    with zipfile.ZipFile(archive) as source:
        # Read known names only; never extract paths supplied by an archive.
        for name in ("metadata.json", "stages.wav", "blocks.csv"):
            if source.getinfo(name).file_size > 64 * 1024 * 1024:
                raise ValueError("Recording exceeds the app's bounded session size")
        metadata = json.loads(source.read("metadata.json"))
        rate, channels = wavfile.read(io.BytesIO(source.read("stages.wav")))
        blocks = source.read("blocks.csv")
    if metadata.get("schema") != "1" or int(metadata["sample_rate"]) != rate:
        raise ValueError("Unknown recording schema or inconsistent PCM rate")
    if metadata.get("error"):
        raise ValueError(f"Recorder reported a failure: {metadata['error']}")
    if (channels.ndim != 2 or channels.shape[1] != 4 or not channels.size or
            not np.issubdtype(channels.dtype, np.floating) or not np.isfinite(channels).all()):
        raise ValueError("Expected four finite float channels")
    if len(channels) != int(metadata["frames_written"]):
        raise ValueError("WAV and metadata disagree; recording may be incomplete")
    output.mkdir(parents=True, exist_ok=False)
    for index, name in enumerate(("raw", "enhanced", "limited_left", "limited_right")):
        wavfile.write(output / f"{name}.wav", rate, channels[:, index])
    delay_text = metadata.get("enhancement_delay_samples", "")
    if delay_text:
        delay = int(delay_text)
        if delay < 0 or delay >= len(channels):
            raise ValueError("Recording is shorter than its algorithm delay")
        # Recorder does not flush the model tail after Stop: compare only the shared interval.
        length = len(channels) - delay
        wavfile.write(output / "raw_aligned.wav", rate, channels[:length, 0])
        wavfile.write(output / "enhanced_aligned.wav", rate, channels[delay:delay + length, 1])
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2))
    (output / "blocks.csv").write_bytes(blocks)
    (output / "manifest.json").write_text(json.dumps({
        "schema": 1, "cases": [{"id": "recorded_microphone", "noisy": "raw.wav", "reference": None}],
        "notes": "No clean reference. Do not interpret reduced RMS as improved intelligibility.",
    }, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    extract(args.archive, args.output)


if __name__ == "__main__":
    main()
