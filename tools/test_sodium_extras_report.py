import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from run_sodium_extras_compat import CASES, CAMERAS, BASE_ASSERTIONS, EXTRAS_ASSERTIONS, TARGETS, validate_report, runtime_artifact_paths
import runtime_artifacts

def complete_report():
    return {"passed": True, "complete": True, "extrasPresent": True,
            "cullingAssertions": sorted(BASE_ASSERTIONS | EXTRAS_ASSERTIONS),
            "expectedCases": len(CASES), "cases": [
                {"case": name, "camera": CAMERAS[name]+[0,0], "passed": True, "tracked": True, "summonAge": 300,
                 "expectedVisible": not name.endswith("-control"),
                 "visiblePixels": not name.endswith("-control"),
                 "renders": 0 if name.endswith("-control") else 45}
                for name in sorted(CASES)]}

class CompatEvidenceTest(unittest.TestCase):
    def test_current_version_fixture_and_configured_boom_ignore_stale_artifacts(self):
        for target in TARGETS:
            with self.subTest(target=target), tempfile.TemporaryDirectory() as directory:
                root=Path(directory); repository=root/'maven'
                (root/'gradle.properties').write_text('mod_version = 0.1.5\nboom_version = 1.2.0\n',encoding='utf-8')
                libs=root/target/'build/libs'; libs.mkdir(parents=True)
                suffix='-compat-test-dev.jar' if target.startswith('neoforge') else '-compat-test.jar'
                for version in ('0.1.4','0.1.5'):
                    (libs/('celestial-nail-'+target+'-'+version+suffix)).touch()
                loader,game=target.split('-',1)
                artifact='perfomant_boom-'+(loader if game=='1.20.1' else target)
                for version in ('1.1.3','1.2.0'):
                    boom=repository/'com/nstut'/artifact/version/(artifact+'-'+version+'.jar')
                    boom.parent.mkdir(parents=True); boom.touch()
                nail,fixture,boom=runtime_artifact_paths(target,root,repository)
                self.assertEqual(libs/('celestial-nail-'+target+'-0.1.5.jar'),nail)
                self.assertEqual(libs/('celestial-nail-'+target+'-0.1.5'+suffix),fixture)
                self.assertEqual('1.2.0',boom.parent.name)
                boom.unlink()
                with self.assertRaisesRegex(ValueError,'Missing matching Boom artifact.*1.2.0'):
                    runtime_artifact_paths(target,root,repository)
                fixture.unlink()
                with self.assertRaisesRegex(ValueError,'Missing compatibility fixture.*0.1.5'):
                    runtime_artifact_paths(target,root,repository)

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

    def test_requires_culling_assertions_and_real_cutoff_camera(self):
        report = complete_report()
        report["cullingAssertions"].pop()
        with self.assertRaises(ValueError): validate_report(report, True)
        report = complete_report()
        report["cases"][0]["camera"] = [0,180,-40,0,0]
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
