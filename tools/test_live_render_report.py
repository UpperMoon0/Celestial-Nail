import unittest
from run_render_regression import validate_report, verify


def report():
    names = ("idle-near", "idle-close", "idle-inside", "idle-underneath", "embedded-dark", "cloud-overlap", "idle-far", "minimum-scale", "maximum-scale", "portal-opening", "emerging",
             "descending", "impact", "embedded-buried-anchor", "crumbling", "terrain-occluded-control", "offscreen-control",
             "outside-fade-control", "missing-draw-control", "lost-final-composite-control", "missing-tracking-control", "cinematic-impact-frame", "cinematic-sequence-white", "cinematic-sequence-invert", "cinematic-sequence-gold", "cinematic-sequence-ink", "cinematic-sequence-recovery", "cinematic-sequence-reduced", "cinematic-sequence-off-control", "cinematic-delayed-impact-frame", "cinematic-peripheral-frame", "cinematic-camera-shake", "cinematic-dust-below-ledge", "cinematic-dust-curtain", "cinematic-lingering", "cinematic-dust-occluded-control", "cinematic-expired-impact-control", "cinematic-disabled-control")
    cases = []
    for mode in ("vanilla", "complementary"):
        for name in names:
            control = name.endswith("-control")
            cases.append(dict(case=mode + "-" + name, passed=True, expectedVisible=not control,
                              shaderPackInUse=mode == "complementary",
                              visiblePixels=not control, tracked=name != "missing-tracking-control",
                              maxCameraYawDelta=1, maxCameraPitchDelta=1, cinematicDraws=1 if name.startswith("cinematic-") and name not in ("cinematic-expired-impact-control","cinematic-disabled-control","cinematic-sequence-off-control") else 0, renders=1, vertices=400, shaderApplies=1, wrongPrograms=0, blendDisabled=0, colorWritesDisabled=0, meanVisibleBrightness=128, bodyDraws=1, bodyDepthDisabled=0, clippedWhiteFraction=0, cloudDepthPixels=1000))
            phase=name.startswith("cinematic-") or name in ("descending","impact","embedded-dark","embedded-buried-anchor","crumbling")
            impact=2000 if name.startswith("embedded") or name=="cinematic-lingering" else 0 if name.startswith("cinematic-") else 12 if name=="impact" else -1
            launch=45+max(0,impact)+(20 if name=="crumbling" else 0) if phase else -1
            ages=dict(summonAge=2300 if name.startswith("embedded") else 300, launchAge=launch,impactAge=impact,crumbleAge=20 if name=="crumbling" else -1)
            cases[-1]["lifecycle"]={**ages,**{"expected"+k[0].upper()+k[1:]:v for k,v in ages.items()}}
    return dict(complete=True, expectedCases=76, failures=0, cases=cases)


class LiveRenderReportTest(unittest.TestCase):
    def test_focused_run_requires_both_modes_and_is_never_a_full_matrix(self):
        r=report();r['cases']=[c for c in r['cases'] if c['case'].endswith('cinematic-sequence-off-control')];r['expectedCases']=2
        self.assertEqual(2,len(validate_report(r,'cinematic-sequence-off-control')))
        with self.assertRaises(ValueError): validate_report(r)
        r['cases'].pop()
        with self.assertRaises(ValueError): validate_report(r,'cinematic-sequence-off-control')

    def test_exact_matrix_and_completed_run_required(self):
        self.assertEqual(76, len(validate_report(report())))
        for mutation in (lambda r: r.update(complete=False),
                         lambda r: r["cases"].pop(),
                         lambda r: r["cases"][0].update(case=r["cases"][1]["case"]),
                         lambda r: r["cases"][0].update(passed=False)):
            bad = report(); mutation(bad)
            with self.assertRaises(ValueError): validate_report(bad)

    def test_cannot_pass_invisible_geometry_or_wrong_pipeline_even_if_flagged_passed(self):
        for key, value in (("tracked", False), ("visiblePixels", False), ("renders", 0), ("vertices", 0),
                           ("shaderApplies", 0), ("wrongPrograms", 1), ("blendDisabled", 1), ("colorWritesDisabled", 1), ("shaderPackInUse", False)):
            bad = report(); next(c for c in bad["cases"] if c["case"]=="complementary-idle-near")[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError): validate_report(bad)

    def test_dark_pinned_body_cannot_pass_with_only_draw_counters(self):
        bad=report()
        next(case for case in bad["cases"] if case["case"]=="complementary-embedded-dark")["meanVisibleBrightness"]=20
        with self.assertRaises(ValueError):validate_report(bad)

    def test_review_clock_failures_cannot_pass_even_when_flagged_passed(self):
        for phase in ("summon","launch","impact","crumble"):
            bad=report()
            case=next(c for c in bad["cases"] if c["case"]=="complementary-embedded-dark")
            case["lifecycle"][phase+"Age"]=-1 if phase!="crumble" else 20
            with self.subTest(phase=phase),self.assertRaises(ValueError):validate_report(bad)
        for name,phase in (("embedded-dark","impact"),("embedded-dark","launch"),("crumbling","crumble"),("idle-near","summon")):
            bad=report()
            case=next(c for c in bad["cases"] if c["case"]=="complementary-"+name)
            case["lifecycle"].update({phase+"Age":-1,"expected"+phase.title()+"Age":-1})
            with self.subTest(name=name,phase=phase),self.assertRaises(ValueError):validate_report(bad)

    def test_glow_and_cloud_depth_regressions_cannot_pass(self):
        for key,value in (("bodyDraws",0),("bodyDepthDisabled",1),("clippedWhiteFraction",.9),("cloudDepthPixels",0)):
            bad=report()
            case=next(c for c in bad["cases"] if c["case"]=="complementary-cloud-overlap")
            case[key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):validate_report(bad)

    def test_negative_controls_must_exercise_the_intended_failure(self):
        for name, key, value in (("lost-final-composite-control", "vertices", 0),
                                 ("terrain-occluded-control", "vertices", 0),
                                 ("terrain-occluded-control", "visiblePixels", True),
                                 ("missing-tracking-control", "tracked", True),
                                 ("missing-draw-control", "visiblePixels", True)):
            bad = report()
            next(case for case in bad["cases"] if case["case"] == "complementary-" + name)[key] = value
            with self.subTest(name=name), self.assertRaises(ValueError): validate_report(bad)

    def test_cinematic_controls_require_shader_and_final_pixel_evidence(self):
        for name,key,value in (("cinematic-impact-frame","cinematicDraws",0),
                               ("cinematic-dust-curtain","visiblePixels",False),
                               ("cinematic-dust-below-ledge","visiblePixels",False),
                               ("cinematic-camera-shake","maxCameraYawDelta",0),
                               ("cinematic-camera-shake","maxCameraPitchDelta",0),
                               ("cinematic-dust-occluded-control","cinematicDraws",0),
                               ("cinematic-dust-occluded-control","visiblePixels",True),
                               ("cinematic-expired-impact-control","cinematicDraws",1),
                               ("cinematic-disabled-control","cinematicDraws",1)):
            bad=report()
            next(c for c in bad["cases"] if c["case"]=="complementary-"+name)[key]=value
            with self.subTest(name=name,key=key),self.assertRaises(ValueError): validate_report(bad)

    def test_artifact_integrity_mismatch_is_a_failure(self):
        with self.assertRaises(ValueError): verify(b"modified jar", "0" * 128)


if __name__ == "__main__":
    unittest.main()
