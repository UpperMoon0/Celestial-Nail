"""Run real Forge dispatcher regressions with Sodium Extras absent and present."""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import urllib.request

from run_render_regression import ARTIFACTS, ROOT

EXTRAS = (
    ".dependencies/compat/sodiumextras-forge-1.0.7-1.20.1.jar",
    "https://cdn.modrinth.com/data/vqqx0QiE/versions/VNFB2Vgv/sodiumextras-forge-1.0.7-1.20.1.jar",
    "47f298943079eb6b8f5222cbb093fec0c29c86a6f68e8000545059d64ff5c4cbf76fa774565a0d4d89d1fb0a278b62292f7619fb818e4fdfd9f01a637e7fcbfb",
)


def prepare(artifact):
    relative, url, digest = artifact
    path = ROOT / relative
    if path.exists():
        data = path.read_bytes()
    else:
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
    if hashlib.sha512(data).hexdigest() != digest:
        raise ValueError("Artifact hash mismatch: " + relative)
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    prepare(ARTIFACTS[1])  # Sodium Extras requires Embeddium.
    extras = prepare(EXTRAS)
    if args.prepare_only:
        return
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    evidence = ROOT / "build/reports/sodium-extras-compat"
    evidence.mkdir(parents=True, exist_ok=True)
    for present in (False, True):
        mode = "present" if present else "absent"
        command = [str(wrapper), ":forge-1.20.1:runSodiumExtrasCompat", "--console=plain", "--max-workers=2"]
        if present:
            command.append("-PsodiumExtrasJar=" + str(extras))
        with (evidence / (mode + ".log")).open("w", encoding="utf-8") as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=360)
        report = ROOT / "forge-1.20.1/run/sodium-extras-compat/sodium-extras-compat-results.json"
        if report.exists():
            shutil.copyfile(report, evidence / (mode + ".json"))
        if result.returncode != 0:
            raise RuntimeError("Forge compatibility fixture failed: " + str(evidence / (mode + ".log")))
        results = json.loads(report.read_text(encoding="utf-8"))
        if results.get("passed") is not True or results.get("extrasPresent") is not present:
            raise ValueError("Incorrect compatibility evidence: " + str(report))
        print("PASS: Sodium Extras " + mode, flush=True)


if __name__ == "__main__":
    main()
