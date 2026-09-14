import unittest
from audio_quality.check_report import failures


class AcceptanceTest(unittest.TestCase):
    def test_silence_cannot_pass_on_noise_attenuation_alone(self):
        report = {"results": [{"case": "quiet", "variant": "neural",
                                "metrics": {"output_silent": True, "noise_only_attenuation_db": 100}}]}
        criteria = {"cases": ["quiet"], "variants": {"neural": {"noise_only_attenuation_db": {"min": 3}}}}
        self.assertTrue(failures(report, criteria))

    def test_empty_or_nonfinite_bounds_cannot_pass(self):
        report = {"results": [{"case": "quiet", "variant": "neural", "metrics": {"output_peak": 0.5}}]}
        for bounds in ({}, {"max": float("nan")}, {"minimum": 0}):
            criteria = {"cases": ["quiet"], "variants": {"neural": {"output_peak": bounds}}}
            self.assertTrue(failures(report, criteria))

    def test_missing_quality_scores_cannot_pass(self):
        report = {"results": [{"case": "quiet", "variant": "neural", "metrics": {"output_peak": 0.5}}]}
        criteria = {"cases": ["quiet"], "variants": {"neural": {"estoi_delta": {"min": 0}}}}
        self.assertTrue(failures(report, criteria))

    def test_regressions_fail_per_condition(self):
        report = {"results": [{"case": "quiet", "variant": "neural",
                                "metrics": {"estoi_input": 0.8, "estoi_output": 0.7}}]}
        criteria = {"cases": ["quiet"], "variants": {"neural": {"estoi_delta": {"min": 0}}}}
        self.assertEqual(len(failures(report, criteria)), 1)


if __name__ == "__main__":
    unittest.main()
