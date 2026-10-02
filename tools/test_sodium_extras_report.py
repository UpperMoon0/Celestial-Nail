import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from run_sodium_extras_compat import CASES, validate_report
import runtime_artifacts

def complete_report():
    return {"passed": True, "complete": True, "extrasPresent": True,
            "expectedCases": len(CASES), "cases": [
                {"case": name, "passed": True, "tracked": True, "summonAge": 300,
                 "expectedVisible": not name.endswith("-control"),
                 "visiblePixels": not name.endswith("-control"),
                 "renders": 0 if name.endswith("-control") else 45}
                for name in sorted(CASES)]}

class CompatEvidenceTest(unittest.TestCase):
    def test_complete_report(self):
        validate_report(complete_report(), True)

    def test_rejects_missing_or_duplicate_camera(self):
        for duplicate in (False, True):
            report = complete_report()
            if duplicate: report["cases"][-1] = copy.deepcopy(report["cases"][0])
            else: report["cases"].pop()
            with self.assertRaises(ValueError): validate_report(report, True)

    def test_rejects_false_positive_control_or_missing_visible_pixels(self):
        for name in CASES:
            report = complete_report()
            case = next(case for case in report["cases"] if case["case"] == name)
            case["visiblePixels"] = not case["visiblePixels"]
            with self.assertRaises(ValueError): validate_report(report, True)

    def test_rejects_untracked_stale_clock_and_draw_only_evidence(self):
        for key, value in (("tracked", False), ("summonAge", -1), ("renders", 0)):
            report = complete_report()
            case = next(case for case in report["cases"] if not case["case"].endswith("-control"))
            case[key] = value
            with self.assertRaises(ValueError): validate_report(report, True)

    def test_requires_correct_optional_mod_mode_and_completion(self):
        report = complete_report()
        with self.assertRaises(ValueError): validate_report(report, False)
        report["complete"] = False
        with self.assertRaises(ValueError): validate_report(report, True)

    def test_rejects_tampered_cached_artifact_before_network(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "mod.jar").write_bytes(b"tampered")
            artifact = runtime_artifacts.Artifact("mod.jar", "https://invalid.example/mod.jar",
                                                  hashlib.sha512(b"expected").hexdigest())
            with patch.object(runtime_artifacts, "ROOT", root), patch.object(runtime_artifacts.urllib.request, "urlopen") as download:
                with self.assertRaises(ValueError): runtime_artifacts.prepare(artifact)
                download.assert_not_called()

if __name__ == "__main__": unittest.main()
