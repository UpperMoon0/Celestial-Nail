"""Prepare pinned shader artifacts and run the real Forge/Oculus final-image regression."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = (
    (".dependencies/shaders/oculus-mc1.20.1-1.8.0.jar",
     "https://cdn.modrinth.com/data/GchcoXML/versions/iQ1SwGc3/oculus-mc1.20.1-1.8.0.jar",
     "1bb4ac77400d6684347988ed298a692c2cb15cf7923693607eb8739b171a20fef7412259e9e157111d9ce21779badab386029956f7d2283a9e611722a373e9d5"),
    (".dependencies/shaders/embeddium-0.3.31+mc1.20.1.jar",
     "https://cdn.modrinth.com/data/sk9rgfiA/versions/UTbfe5d1/embeddium-0.3.31%2Bmc1.20.1.jar",
     "ffbf2da4685260a4d5c14c621708bd20722563f084f042d3dfb0a7b87f048e39299648c854a93939129da0d23a15a91ec628560d601e76074b08e275f6e132e9"),
    ("forge-1.20.1/run/client/shaderpacks/ComplementaryReimagined_r5.9.3.zip",
     "https://cdn.modrinth.com/data/HVnmMxH1/versions/Bqen1mJX/ComplementaryReimagined_r5.9.3.zip",
     "45304b1d7862afdb2177b7e6226fc9c6737911d17a4ad46cc9c519ca4deb935f05072cd337cd8b19a15bb8062b57b54eceec7bdee4cd572d94858e2946d37d85"),
)
JCPP_HASH = "e876bb9cb6405108850aed7659194e4124a95f7697ec95f2fb4d3a5c92069bc611a06c90752bcb06030c47f9956aee5adb0ba2687d095b2eab83027cd936c143"


def verify(data, expected):
    if hashlib.sha512(data).hexdigest() != expected:
        raise ValueError("Pinned artifact SHA-512 mismatch")


def prepare():
    for relative, url, digest in ARTIFACTS:
        path = ROOT / relative
        if path.exists():
            verify(path.read_bytes(), digest)
        else:
            request = urllib.request.Request(url, headers={"User-Agent": "Celestial-Nail-live-render-regression/1.0"})
            with urllib.request.urlopen(request, timeout=60) as response:
                data = response.read()
            verify(data, digest)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        print("Verified", path.name, flush=True)
    with zipfile.ZipFile(ROOT / ARTIFACTS[0][0]) as jar:
        jcpp = jar.read("META-INF/jars/jcpp-1.4.14.jar")
    verify(jcpp, JCPP_HASH)
    destination = ROOT / ".dependencies/shader-libraries/jcpp-1.4.14.jar"
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        verify(destination.read_bytes(), JCPP_HASH)
    else:
        destination.write_bytes(jcpp)


def validate_report(report):
    cases = report.get("cases", [])
    if report.get("complete") is not True or len(cases) != report.get("expectedCases") or len(cases) != 40:
        raise ValueError("Live fixture incomplete; every scene and both shader modes are required")
    names = {"idle-near", "idle-close", "idle-inside", "idle-underneath", "embedded-dark", "idle-far", "minimum-scale", "maximum-scale", "portal-opening", "emerging",
             "descending", "impact", "embedded-buried-anchor", "crumbling", "terrain-occluded-control", "offscreen-control",
             "outside-fade-control", "missing-draw-control", "lost-final-composite-control", "missing-tracking-control"}
    expected = {mode + "-" + name for mode in ("vanilla", "complementary") for name in names}
    if {case["case"] for case in cases} != expected:
        raise ValueError("Duplicate or missing live test cases")
    failed = [case["case"] for case in cases if case.get("passed") is not True]
    if failed or report.get("failures") != 0:
        raise ValueError("Real live render failures: " + ", ".join(failed))
    for case in cases:
        if case.get("shaderPackInUse") is not case["case"].startswith("complementary-"):
            raise ValueError("Actual shader-pack state does not match the tested mode")
        control = case["case"].endswith("-control")
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
            if any(case.get(key, 0) <= 0 for key in ("renders", "vertices", "shaderApplies")):
                raise ValueError("Visible fixture lacks real draw evidence")
            if case.get("wrongPrograms") != 0 or case.get("blendDisabled") != 0 or case.get("colorWritesDisabled") != 0:
                raise ValueError("Unexpected shader program, color-write mask or blend state")
        if case["case"].endswith("embedded-dark") and case.get("meanVisibleBrightness", 0) < 100:
            raise ValueError("Pinned Nail is dark in the zero-light nighttime fixture")
    return cases


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    prepare()
    if args.prepare_only:
        return 0
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(wrapper), ":forge-1.20.1:runRenderRegression", "--console=plain", "--no-daemon"]
    result = subprocess.run(command, cwd=ROOT)
    report_path = ROOT / "forge-1.20.1/run/render-regression/render-test-results.json"
    try:
        cases = validate_report(json.loads(report_path.read_text(encoding="utf-8")))
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
