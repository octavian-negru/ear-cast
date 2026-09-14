"""Replay identical input through production native DSP and an optional local DFN3 model."""
import argparse
import importlib.metadata
import json
from pathlib import Path
import random
import re
import subprocess
import tempfile
import time

import numpy as np
from scipy.io import wavfile

from .signals import RATES, activity_mask, convert_rate, file_sha256, listening_versions, metrics, read_wav

VARIANTS = {
    "off": ("off", 0, 0),
    "speex_gentle": ("speex", 6, 0),
    "speex_strong": ("speex", 12, 0),
    "rnnoise_gentle": ("rnnoise", 6, 0),
    "rnnoise_strong": ("rnnoise", 12, 0),
    "rnnoise_quiet_6db": ("rnnoise", 6, 6),
    "rnnoise_quiet_12db": ("rnnoise", 6, 12),
}

DPDFNET_VARIANTS = {
    "dpdfnet8_gentle": ("dpdfnet", 6, 0),
    "dpdfnet8_strong": ("dpdfnet", 12, 0),
}

DPDFNET_HASHES = {
    "dpdfnet8.onnx": "2751c1f5a4e849d23a07c675b4c838158b249b42152f10cc318522dd339134f0",
    "dpdfnet8_8khz.onnx": "c061bcc56b803fa2fa97d448a45db6d966f7d17aff1304e464455d748745ea62",
    "dpdfnet8_48khz_hr.onnx": "7b3afbb260a08fe9af3d16e3bda992971be1e7e951d1dee7c2d235f5c43f5631",
}


def native_render(renderer, samples, rate, variant, dpdfnet_models=None):
    with tempfile.TemporaryDirectory(prefix="openhearing-quality-") as temporary:
        source, target = Path(temporary) / "input.f32", Path(temporary) / "output.f32"
        np.asarray(samples, dtype="<f4").tofile(source)
        backend, suppression, boost = (VARIANTS | DPDFNET_VARIANTS)[variant]
        command = [str(renderer), backend, str(rate), str(suppression), str(boost), str(source), str(target)]
        if backend == "dpdfnet":
            if dpdfnet_models is None:
                raise ValueError("DPDFNet requires the bundled model directory")
            model = ("dpdfnet8_8khz.onnx" if rate == 8000 else
                     "dpdfnet8.onnx" if rate == 16000 else "dpdfnet8_48khz_hr.onnx")
            command.append(str((dpdfnet_models / model).resolve(strict=True)))
        result = subprocess.run(
            command,
            check=True, capture_output=True, text=True, timeout=max(120, int(len(samples) / rate * 100)),
        )
        info = json.loads(result.stdout)
        output = np.fromfile(target, dtype="<f4").astype(np.float64)
        delay = int(info["delay_samples"])
        if delay < 0 or len(output) < delay + len(samples):
            raise ValueError("Renderer did not supply the complete delayed tail")
        if info["input_frames"] != len(samples) or info["output_frames"] != len(output):
            raise ValueError("Renderer frame counters do not match its output")
        # Known algorithm delay, not per-file correlation tuning that could inflate scores.
        return output[delay:delay + len(samples)], info


def deepfilter_render(model_directory, samples, rate):
    # Absolute directory is essential: upstream treats model names as download requests.
    model_directory = model_directory.resolve(strict=True)
    if not (model_directory / "config.ini").is_file() or not (model_directory / "checkpoints").is_dir():
        raise ValueError("Supply an already-downloaded DFN3 directory with config.ini and checkpoints")
    import torch
    from df.enhance import enhance, init_df

    model, state, *details = init_df(str(model_directory), post_filter=False, log_file=None)
    epoch = details[1] if len(details) > 1 else None
    model_rate = int(state.sr())
    if model_rate != 48000:
        raise ValueError("This comparison expects the standard full-band DFN3 model")
    signal = convert_rate(samples, rate, model_rate)
    started = time.perf_counter()
    enhanced = enhance(model, state, torch.from_numpy(signal.astype(np.float32)).unsqueeze(0),
                       pad=True, atten_lim_db=12)
    seconds = time.perf_counter() - started
    output = convert_rate(enhanced.squeeze(0).cpu().numpy(), model_rate, rate)
    if len(output) < len(samples):
        raise ValueError("DFN returned a short output")
    fingerprints = {str(file.relative_to(model_directory)): file_sha256(file)
                    for file in sorted(model_directory.rglob("*")) if file.is_file()}
    return output[:len(samples)], {
        "processing_seconds": seconds, "epoch": epoch, "model_files_sha256": fingerprints,
        "deepfilternet_version": importlib.metadata.version("deepfilternet"),
        "delay_handling": "upstream_pad_true_plus_offline_polyphase_resampling",
    }


def evaluate(manifest_path, renderer, output_directory, deepfilter_model=None, seed=2026, dpdfnet_models=None):
    manifest = json.loads(manifest_path.read_text())
    cases = manifest["cases"]
    if not cases:
        raise ValueError("Manifest has no cases")
    renderer = renderer.resolve(strict=True)
    output_directory.mkdir(parents=True, exist_ok=False)
    results, blind_key, seen, input_hashes = [], {}, set(), {}
    variants = VARIANTS | DPDFNET_VARIANTS if dpdfnet_models is not None else VARIANTS
    model_hashes = {}
    if dpdfnet_models is not None:
        model_hashes = {name: file_sha256(dpdfnet_models / name) for name in DPDFNET_HASHES}
        if model_hashes != DPDFNET_HASHES:
            raise ValueError("DPDFNet replay must use the same pinned models as the app")
    rng = random.Random(seed)
    for case in cases:
        name = case["id"]
        if not re.fullmatch(r"[A-Za-z0-9_-]+", name) or name in seen:
            raise ValueError(f"Invalid or duplicate case id: {name}")
        seen.add(name)
        noisy_path = (manifest_path.parent / case["noisy"]).resolve(strict=True)
        input_hashes[name] = {"noisy": file_sha256(noisy_path)}
        rate, noisy = read_wav(noisy_path)
        if rate not in RATES or np.max(np.abs(noisy)) > 1:
            raise ValueError("Input must use a supported PCM rate and fit normalized full scale")
        reference = None
        if case.get("reference"):
            reference_path = manifest_path.parent / case["reference"]
            reference_rate, reference = read_wav(reference_path)
            input_hashes[name]["reference"] = file_sha256(reference_path)
            if reference_rate != rate or len(reference) != len(noisy):
                raise ValueError("Reference and input must have identical rate and length")
        case_directory = output_directory / name
        case_directory.mkdir()
        outputs = {}
        for variant in variants:
            audio, timing = native_render(renderer, noisy, rate, variant, dpdfnet_models)
            outputs[variant] = audio
            wavfile.write(case_directory / f"{variant}.wav", rate, audio.astype(np.float32))
            results.append({"case": name, "variant": variant, "timing": timing,
                            "metrics": metrics(noisy, audio, rate, reference)})
        if deepfilter_model:
            audio, timing = deepfilter_render(deepfilter_model, noisy, rate)
            outputs["deepfilternet3"] = audio
            wavfile.write(case_directory / "deepfilternet3.wav", rate, audio.astype(np.float32))
            results.append({"case": name, "variant": "deepfilternet3", "timing": timing,
                            "metrics": metrics(noisy, audio, rate, reference)})
        active = activity_mask(reference, rate) if reference is not None else np.ones(len(noisy), dtype=bool)
        listening = listening_versions(outputs, active)
        names = list(listening)
        rng.shuffle(names)
        blind = case_directory / "blind"
        blind.mkdir()
        blind_key[name] = {}
        for index, variant in enumerate(names):
            label = chr(ord("A") + index)
            wavfile.write(blind / f"{label}.wav", rate, listening[variant].astype(np.float32))
            blind_key[name][label] = variant
    report = {
        "schema": 1, "manifest": manifest, "results": results,
        "renderer_sha256": file_sha256(renderer), "evaluated_inputs_sha256": input_hashes,
        "dpdfnet_models_sha256": model_hashes,
        "notes": "No automatic pass verdict: listening and per-condition acceptance criteria are required. "
                 "Native timings exclude initialization and I/O; DFN uses offline inference and is not comparable latency.",
    }
    (output_directory / "report.json").write_text(json.dumps(report, indent=2, allow_nan=False))
    (output_directory / "blind-key.json").write_text(json.dumps(blind_key, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--renderer", type=Path, required=True, help="Existing audio_render executable; never built here")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--deepfilter-model", type=Path)
    parser.add_argument("--dpdfnet-models", type=Path, help="Bundled models; renderer needs DPDFNet support")
    parser.add_argument("--seed", type=int, default=2026)
    args = parser.parse_args()
    evaluate(args.manifest.resolve(), args.renderer, args.output, args.deepfilter_model, args.seed, args.dpdfnet_models)


if __name__ == "__main__":
    main()
