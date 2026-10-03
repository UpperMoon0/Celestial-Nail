"""Prepare pinned shader artifacts and run the real Forge/Oculus final-image regression."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

from runtime_artifacts import Artifact, EMBEDDIUM, ROOT, prepare as prepare_artifact, verify
OCULUS = Artifact(
    ".dependencies/shaders/oculus-mc1.20.1-1.8.0.jar",
    "https://cdn.modrinth.com/data/GchcoXML/versions/iQ1SwGc3/oculus-mc1.20.1-1.8.0.jar",
    "1bb4ac77400d6684347988ed298a692c2cb15cf7923693607eb8739b171a20fef7412259e9e157111d9ce21779badab386029956f7d2283a9e611722a373e9d5")
COMPLEMENTARY = Artifact(
    "forge-1.20.1/run/client/shaderpacks/ComplementaryReimagined_r5.9.3.zip",
    "https://cdn.modrinth.com/data/HVnmMxH1/versions/Bqen1mJX/ComplementaryReimagined_r5.9.3.zip",
    "45304b1d7862afdb2177b7e6226fc9c6737911d17a4ad46cc9c519ca4deb935f05072cd337cd8b19a15bb8062b57b54eceec7bdee4cd572d94858e2946d37d85")
SHADER_ARTIFACTS = (OCULUS, EMBEDDIUM, COMPLEMENTARY)
JCPP_HASH = "e876bb9cb6405108850aed7659194e4124a95f7697ec95f2fb4d3a5c92069bc611a06c90752bcb06030c47f9956aee5adb0ba2687d095b2eab83027cd936c143"


def prepare():
    for artifact in SHADER_ARTIFACTS:
        path = prepare_artifact(artifact)
        print("Verified", path.name, flush=True)
    with zipfile.ZipFile(ROOT / OCULUS.path) as jar:
        jcpp = jar.read("META-INF/jars/jcpp-1.4.14.jar")
    verify(jcpp, JCPP_HASH)
    destination = ROOT / ".dependencies/shader-libraries/jcpp-1.4.14.jar"
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        verify(destination.read_bytes(), JCPP_HASH)
    else:
        destination.write_bytes(jcpp)


def validate_report(report, only_case=None):
    cases = report.get("cases", [])
    if report.get("complete") is not True or len(cases) != report.get("expectedCases") or len(cases) != (2 if only_case else 76):
        raise ValueError("Live fixture incomplete; every scene and both shader modes are required")
    names = {"idle-near", "idle-close", "idle-inside", "idle-underneath", "embedded-dark", "cloud-overlap", "idle-far", "minimum-scale", "maximum-scale", "portal-opening", "emerging",
             "descending", "impact", "embedded-buried-anchor", "crumbling", "terrain-occluded-control", "offscreen-control",
             "outside-fade-control", "missing-draw-control", "lost-final-composite-control", "missing-tracking-control", "cinematic-impact-frame", "cinematic-sequence-white", "cinematic-sequence-invert", "cinematic-sequence-gold", "cinematic-sequence-ink", "cinematic-sequence-recovery", "cinematic-sequence-reduced", "cinematic-sequence-off-control", "cinematic-delayed-impact-frame", "cinematic-peripheral-frame", "cinematic-camera-shake", "cinematic-dust-below-ledge", "cinematic-dust-curtain", "cinematic-lingering", "cinematic-dust-occluded-control", "cinematic-expired-impact-control", "cinematic-disabled-control"}
    if only_case:
        if only_case not in names: raise ValueError("Unknown focused case")
        names={only_case}
    expected = {mode + "-" + name for mode in ("vanilla", "complementary") for name in names}
    if {case["case"] for case in cases} != expected:
        raise ValueError("Duplicate or missing live test cases")
    failed = [case["case"] for case in cases if case.get("passed") is not True]
    if failed or report.get("failures") != 0:
        raise ValueError("Real live render failures: " + ", ".join(failed))
    for case in cases:
        if case.get("shaderPackInUse") is not case["case"].startswith("complementary-"):
            raise ValueError("Actual shader-pack state does not match the tested mode")
        if case["case"].endswith("cinematic-camera-shake") and any(case.get(key,0)<.1 for key in ("maxCameraYawDelta","maxCameraPitchDelta")):
            raise ValueError("Camera shake never moved the real camera")
        control = case["case"].endswith("-control")
        cinematic = "-cinematic-" in case["case"]
        if cinematic:
            draws=case.get("cinematicDraws",0)
            if (not control or case["case"].endswith("dust-occluded-control")) and draws<=0:
                raise ValueError("Cinematic final-image case never composited its shader")
            if case["case"].endswith(("expired-impact-control","disabled-control","sequence-off-control")) and draws!=0:
                raise ValueError("Expired or disabled cinematic replayed")
        if control:
            if case.get("visiblePixels") is not False or case.get("expectedVisible") is not False:
                raise ValueError("An invisible control produced a false positive")
            if ("lost-final-composite" in case["case"] or "terrain-occluded" in case["case"]) and any(case.get(key, 0) <= 0 for key in ("renders", "vertices", "shaderApplies")):
                raise ValueError("Composite-loss control never performed a real geometry draw")
            if "missing-tracking" in case["case"] and case.get("tracked") is not False:
                raise ValueError("Missing-tracking control did not remove tracking")
        else:
            if not all(case.get(key) is True for key in ("expectedVisible", "visiblePixels", "tracked")):
                raise ValueError("Visible fixture lacks final-composite pixel evidence")
            if not cinematic and any(case.get(key, 0) <= 0 for key in ("renders", "vertices", "shaderApplies")):
                raise ValueError("Visible fixture lacks real draw evidence")
            if case.get("wrongPrograms") != 0 or case.get("blendDisabled") != 0 or case.get("colorWritesDisabled") != 0:
                raise ValueError("Unexpected shader program, color-write mask or blend state")
        if case.get("tracked"):
            ages=case.get("lifecycle",{})
            for phase in ("summon", "launch", "impact", "crumble"):
                actual=ages.get(phase+"Age")
                expected=ages.get("expected"+phase.title()+"Age")
                if actual is None or expected is None or abs(actual-expected)>.01:
                    raise ValueError("Incorrect lifecycle clock in "+case["case"])
            if ages["summonAge"]<0:
                raise ValueError("Fixture used a missing summon clock")
            name=case["case"].split("-",1)[1]
            if name=="crumbling" and ages["crumbleAge"]<0:
                raise ValueError("Crumble fixture used a missing crumble clock")
            if (cinematic or name in ("descending","impact","embedded-dark","embedded-buried-anchor","crumbling")) and ages["launchAge"]<0:
                raise ValueError("Post-launch fixture left its portal open")
            if (cinematic or name in ("impact","embedded-dark","embedded-buried-anchor")) and ages["impactAge"]<0:
                raise ValueError("Impact fixture used a missing-impact sentinel")
        if not control and not cinematic and not case["case"].endswith("portal-opening"):
            if case.get("bodyDraws",0)<=0 or case.get("bodyDepthDisabled")!=0:
                raise ValueError("Body did not render with depth writes")
        if case["case"].endswith(("idle-near","embedded-dark","cloud-overlap")):
            if not 0<=case.get("clippedWhiteFraction",1)<=.15:
                raise ValueError("Body was overexposed")
        if case["case"].endswith("cloud-overlap") and case.get("cloudDepthPixels",0)<500:
            raise ValueError("Cloud-overlap fixture lacks final body depth")
        if case["case"].endswith("embedded-dark") and case.get("meanVisibleBrightness", 0) < 100:
            raise ValueError("Pinned Nail is dark in the zero-light nighttime fixture")
    return cases


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare-only", action="store_true")
    parser.add_argument("--case", help="Run one named scene in both shader modes")
    args = parser.parse_args()
    prepare()
    if args.prepare_only:
        return 0
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(wrapper), ":forge-1.20.1:runRenderRegression", "--console=plain", "--no-daemon"]
    if args.case: command.append("-PrenderTestFilter="+args.case)
    result = subprocess.run(command, cwd=ROOT)
    report_path = ROOT / "forge-1.20.1/run/render-regression/render-test-results.json"
    try:
        cases = validate_report(json.loads(report_path.read_text(encoding="utf-8")),args.case)
    except (ValueError, OSError) as error:
        print("FAIL:", error, file=sys.stderr)
        return 1
    if result.returncode != 0:
        print("FAIL: Gradle client task failed", file=sys.stderr)
        return 1
    print(f"PASS: {len(cases)} real final-image and fault-control cases; evidence: {report_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
