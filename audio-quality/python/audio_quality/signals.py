"""File handling and reference metrics, separate from production processing."""
from pathlib import Path
import hashlib
import math

import numpy as np
from scipy.io import wavfile
from scipy.signal import resample_poly

RATES = (8000, 16000, 24000, 32000, 44100, 48000)


def file_sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def read_wav(path: Path, *, mono: bool = True):
    rate, pcm = wavfile.read(path)
    if np.issubdtype(pcm.dtype, np.signedinteger):
        pcm = pcm.astype(np.float64) / (float(np.iinfo(pcm.dtype).max) + 1)
    elif pcm.dtype == np.uint8:
        pcm = (pcm.astype(np.float64) - 128) / 128
    elif np.issubdtype(pcm.dtype, np.floating):
        pcm = pcm.astype(np.float64)
    else:
        raise ValueError(f"Unsupported WAV encoding: {path}")
    if not pcm.size or not np.isfinite(pcm).all():
        raise ValueError(f"Empty or nonfinite audio: {path}")
    if mono and pcm.ndim != 1:
        raise ValueError(f"Supply mono audio, or explicitly extract a diagnostic channel: {path}")
    return int(rate), pcm


def convert_rate(audio, source_rate, target_rate):
    if source_rate == target_rate:
        return audio.copy()
    divisor = math.gcd(source_rate, target_rate)
    return resample_poly(audio, target_rate // divisor, source_rate // divisor, axis=0)


def rms(audio):
    return float(np.sqrt(np.mean(np.square(audio, dtype=np.float64)))) if audio.size else 0.0


def db_ratio(numerator, denominator):
    return float(20 * np.log10(max(numerator, 1e-12) / max(denominator, 1e-12)))


def activity_mask(reference, rate):
    """Reference-only 10 ms energy mask. Never infer clean speech from enhanced audio."""
    hop = rate // 100
    padded = np.pad(reference, (0, (-len(reference)) % hop))
    power = np.mean(padded.reshape(-1, hop) ** 2, axis=1)
    threshold = max(float(power.max()) * 1e-4, 1e-12)
    return np.repeat(power > threshold, hop)[:len(reference)]


def si_sdr(reference, estimate):
    reference = reference - np.mean(reference)
    estimate = estimate - np.mean(estimate)
    energy = float(np.dot(reference, reference))
    if energy < 1e-16 or float(np.dot(estimate, estimate)) < 1e-16:
        return None
    target = reference * (np.dot(reference, estimate) / energy)
    return db_ratio(rms(target), rms(estimate - target))


def metrics(noisy, enhanced, rate, reference=None):
    if len(enhanced) != len(noisy) or not np.isfinite(enhanced).all():
        raise ValueError("Output must be finite and exactly aligned with input")
    result = {
        "output_peak": float(np.max(np.abs(enhanced))),
        "output_rms_dbfs": db_ratio(rms(enhanced), 1),
        "samples_at_or_above_full_scale": int(np.count_nonzero(np.abs(enhanced) >= 1)),
        "output_silent": rms(enhanced) < 1e-10,
    }
    if reference is None:
        result["reference_metrics"] = "unavailable_without_clean_reference"
        return result
    if len(reference) != len(noisy):
        raise ValueError("Reference length must match; do not silently truncate evaluation data")
    active = activity_mask(reference, rate)
    if not active.any():
        raise ValueError("Clean reference contains no measurable speech")
    noise_only = ~active
    noise_only[:min(len(noisy), rate // 2)] = False  # exclude initial adaptation from noise-only score
    before, after = si_sdr(reference, noisy), si_sdr(reference, enhanced)
    result.update({
        "si_sdr_input_db": before, "si_sdr_output_db": after,
        "si_sdr_delta_db": after - before if before is not None and after is not None else None,
        "active_output_to_reference_db": db_ratio(rms(enhanced[active]), rms(reference[active])),
        "noise_only_attenuation_db": db_ratio(rms(noisy[noise_only]), rms(enhanced[noise_only]))
            if noise_only.any() else None,
    })
    try:
        from pystoi import stoi
    except ImportError:
        result["stoi_status"] = "pystoi_not_installed"
    else:
        result["stoi_input"] = float(stoi(reference, noisy, rate, extended=False))
        result["stoi_output"] = float(stoi(reference, enhanced, rate, extended=False))
        result["estoi_input"] = float(stoi(reference, noisy, rate, extended=True))
        result["estoi_output"] = float(stoi(reference, enhanced, rate, extended=True))
        # Undefined scores must be visible, not converted to a fabricated success.
        for key in ("stoi_input", "stoi_output", "estoi_input", "estoi_output"):
            if not np.isfinite(result[key]):
                result[key] = None
    return result


def listening_versions(outputs, active):
    """Match active RMS and common headroom. Failed silent outputs remain silent."""
    normalized = {}
    for name, samples in outputs.items():
        level = rms(samples[active])
        if level < 1e-10:
            normalized[name] = samples.copy()
        else:
            normalized[name] = samples * (0.05 / level)
    peak = max(float(np.max(np.abs(value))) for value in normalized.values())
    common_gain = min(1.0, 0.9 / max(peak, 1e-12))
    return {name: value * common_gain for name, value in normalized.items()}
