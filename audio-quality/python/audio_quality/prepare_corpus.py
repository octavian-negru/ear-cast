"""Build repeatable mixtures from real, user-supplied mono speech (no downloads)."""
import argparse
import json
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import fftconvolve

from .signals import RATES, activity_mask, convert_rate, file_sha256, read_wav, rms


def prepare(clean_path, noise_path, output, rates, snrs, levels, seed, rir_path=None):
    if not rates or any(rate not in RATES for rate in rates) or len(set(rates)) != len(rates):
        raise ValueError("Supply distinct supported PCM rates")
    for values, low, high in ((levels, -90, 0), (snrs, -40, 60)):
        if not values or len(set(values)) != len(values) or any(not low <= value <= high for value in values):
            raise ValueError("Supply distinct finite speech levels [-90, 0] and SNRs [-40, 60]")
    clean_rate, clean_source = read_wav(clean_path)
    if len(clean_source) / clean_rate < 3:
        raise ValueError("Use at least three seconds of real speech for intelligibility metrics")
    noise_rate, noise_source = read_wav(noise_path) if noise_path else (clean_rate, None)
    rir_rate, rir = read_wav(rir_path) if rir_path else (clean_rate, None)
    output.mkdir(parents=True, exist_ok=False)
    cases, identifiers = [], set()
    rng = np.random.default_rng(seed)
    for rate in rates:
        if rate not in RATES:
            raise ValueError(f"Unsupported PCM rate {rate}")
        speech = convert_rate(clean_source, clean_rate, rate)
        if rir is not None:
            response = convert_rate(rir, rir_rate, rate)
            if rms(response) < 1e-12:
                raise ValueError("Room response is silent")
            # Reference includes room reflections: this evaluates noise removal, not dereverberation.
            speech = fftconvolve(speech, response)
        speech = np.pad(speech, (rate, rate))  # adaptation prefix and a noise-only suffix
        active = activity_mask(speech, rate)
        if not active.any():
            raise ValueError("Clean speech is silent")
        if noise_source is None:
            # Synthetic noise is a sanity condition, never a substitute for room/babble recordings.
            noise = rng.standard_normal(len(speech))
        else:
            recorded_noise = convert_rate(noise_source, noise_rate, rate)
            if len(recorded_noise) < len(speech):
                raise ValueError("Provide noise long enough for speech plus two seconds; no artificial loop seams")
            start = int(rng.integers(0, len(recorded_noise) - len(speech) + 1))
            noise = recorded_noise[start:start + len(speech)]
        noise_level = rms(noise[active])
        if noise_level < 1e-12:
            raise ValueError("Noise is silent")
        for level in levels:
            reference = speech * (10 ** (level / 20) / rms(speech[active]))
            for snr in snrs:
                scaled_noise = noise * (rms(reference[active]) / (10 ** (snr / 20) * noise_level))
                mixture = reference + scaled_noise
                headroom = min(1.0, 0.9 / max(float(np.max(np.abs(mixture))), float(np.max(np.abs(reference)))))
                case_id = f"r{rate}_level{level:g}_snr{snr:g}".replace("-", "m").replace(".", "p")
                if case_id in identifiers:
                    raise ValueError("Parameters are too close to produce distinct case identifiers")
                identifiers.add(case_id)
                wavfile.write(output / f"{case_id}_noisy.wav", rate, (mixture * headroom).astype(np.float32))
                wavfile.write(output / f"{case_id}_reference.wav", rate, (reference * headroom).astype(np.float32))
                cases.append({
                    "id": case_id, "noisy": f"{case_id}_noisy.wav", "reference": f"{case_id}_reference.wav",
                    "snr_db": snr, "speech_rms_dbfs_before_headroom": level, "headroom_gain": headroom,
                    "reference_includes_room_response": rir is not None,
                })
    sources = {"clean": str(clean_path), "noise": str(noise_path) if noise_path else "seeded_white_noise"}
    hashes = {str(path): file_sha256(path)
              for path in (clean_path, noise_path, rir_path) if path is not None}
    manifest = {"schema": 1, "seed": seed, "sources": sources, "source_sha256": hashes, "cases": cases}
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2, allow_nan=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", type=Path, required=True)
    parser.add_argument("--noise", type=Path)
    parser.add_argument("--rir", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--rates", type=int, nargs="+", default=[16000, 48000])
    parser.add_argument("--snrs", type=float, nargs="+", default=[-5, 0, 10])
    parser.add_argument("--levels", type=float, nargs="+", default=[-50, -35])
    parser.add_argument("--seed", type=int, default=2026)
    args = parser.parse_args()
    prepare(args.clean, args.noise, args.output, args.rates, args.snrs, args.levels, args.seed, args.rir)


if __name__ == "__main__":
    main()
