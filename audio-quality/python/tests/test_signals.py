import unittest
import numpy as np

from audio_quality.signals import activity_mask, listening_versions, metrics, rms, si_sdr


class MetricsTest(unittest.TestCase):
    def setUp(self):
        self.rate = 16000
        generator = np.random.default_rng(12)
        self.clean = generator.normal(0, 0.01, self.rate * 2)
        self.noisy = self.clean + generator.normal(0, 0.01, len(self.clean))

    def test_si_sdr_does_not_reward_turning_up_volume(self):
        self.assertAlmostEqual(si_sdr(self.clean, self.noisy), si_sdr(self.clean, self.noisy * 4), places=8)

    def test_reference_metrics_are_not_fabricated_for_recordings(self):
        result = metrics(self.noisy, self.noisy, self.rate)
        self.assertEqual(result["reference_metrics"], "unavailable_without_clean_reference")
        self.assertNotIn("si_sdr_delta_db", result)

    def test_blind_files_share_level_and_headroom(self):
        active = activity_mask(self.clean, self.rate)
        versions = listening_versions({"a": self.noisy, "b": self.noisy * 4}, active)
        np.testing.assert_allclose(versions["a"], versions["b"], atol=1e-12)
        self.assertLessEqual(np.max(np.abs(versions["a"])), 0.900001)
        self.assertGreater(rms(versions["a"]), 0)

    def test_short_or_nonfinite_output_is_rejected(self):
        with self.assertRaises(ValueError):
            metrics(self.noisy, self.noisy[:-1], self.rate)
        broken = self.noisy.copy()
        broken[10] = np.nan
        with self.assertRaises(ValueError):
            metrics(self.noisy, broken, self.rate)

    def test_silent_output_has_no_intelligibility_improvement_score(self):
        self.assertIsNone(si_sdr(self.clean, np.zeros_like(self.clean)))
        versions = listening_versions({"failed": np.zeros_like(self.clean)}, np.ones(len(self.clean), dtype=bool))
        self.assertEqual(rms(versions["failed"]), 0.0)


if __name__ == "__main__":
    unittest.main()
