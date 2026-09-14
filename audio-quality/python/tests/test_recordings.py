"""Checks recording alignment and corpus calibration, not model intelligibility."""
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import numpy as np
from scipy.io import wavfile

from audio_quality.extract_recording import extract
from audio_quality.prepare_corpus import prepare
from audio_quality.signals import activity_mask, db_ratio, read_wav, rms


class RecordingTest(unittest.TestCase):
    def test_extract_uses_declared_delay_and_does_not_invent_a_clean_reference(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            raw = np.linspace(-0.1, 0.1, 1000, dtype=np.float32)
            enhanced = np.concatenate([np.zeros(20, dtype=np.float32), raw[:-20]])
            channels = np.column_stack([raw, enhanced, enhanced, enhanced])
            audio = io.BytesIO()
            wavfile.write(audio, 16000, channels)
            metadata = {"schema": "1", "sample_rate": "16000", "frames_written": "1000",
                        "enhancement_delay_samples": "20", "error": ""}
            archive = root / "recording.zip"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("stages.wav", audio.getvalue())
                output.writestr("metadata.json", json.dumps(metadata))
                output.writestr("blocks.csv", "start_frame,frame_count,processing_ns\n0,1000,100\n")
            extract(archive, root / "extracted")
            _, before = read_wav(root / "extracted/raw_aligned.wav")
            _, after = read_wav(root / "extracted/enhanced_aligned.wav")
            np.testing.assert_array_equal(before, after)
            self.assertEqual(len(before), 980)
            manifest = json.loads((root / "extracted/manifest.json").read_text())
            self.assertIsNone(manifest["cases"][0]["reference"])

    def test_mixture_preserves_declared_active_level_and_snr(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            rate = 16000
            # A tone only checks mixture arithmetic; quality evaluation requires real speech.
            time = np.arange(rate * 3) / rate
            clean = (0.1 * np.sin(2 * np.pi * 997 * time)).astype(np.float32)
            wavfile.write(root / "clean.wav", rate, clean)
            prepare(root / "clean.wav", None, root / "corpus", [rate], [0], [-50], 42)
            manifest = json.loads((root / "corpus/manifest.json").read_text())
            case = manifest["cases"][0]
            _, mixture = read_wav(root / "corpus" / case["noisy"])
            _, reference = read_wav(root / "corpus" / case["reference"])
            active = activity_mask(reference, rate)
            self.assertAlmostEqual(db_ratio(rms(reference[active]), 1), -50, places=4)
            self.assertAlmostEqual(db_ratio(rms(reference[active]), rms((mixture - reference)[active])), 0, places=4)
            self.assertEqual(len(reference), rate * 5)
            self.assertLessEqual(np.max(np.abs(mixture)), 0.900001)


if __name__ == "__main__":
    unittest.main()
