"""Launch production jars with Extras absent/present on every applicable loader."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid
import zipfile
from runtime_artifacts import COMPAT, EMBEDDIUM, ROOT, prepare

TARGETS = {"forge-1.20.1": "47.4.0", "fabric-1.20.1": "0.18.4",
           "fabric-1.21.1": "0.18.4", "neoforge-1.21.1": "21.1.228"}
CASES = {"horizontal-cutoff", "vertical-cutoff", "angle-left", "angle-right",
         "offscreen-control", "missing-draw-control"}

BASE_ASSERTIONS = {"optional mod presence", "Nail survives vertical anchor cutoff",
                   "Nail survives horizontal anchor cutoff", "Nail still respects frustum",
                   "Nail still respects its own distance limit"}
EXTRAS_ASSERTIONS = {"Nail exemption on first lookup", "Nail exemption on cached lookup",
                    "ordinary entity remains subject to culling", "user whitelist remains respected",
                    "ordinary control passes renderer visibility", "ordinary entity vertical cutoff",
                    "ordinary entity horizontal cutoff", "compatibility preserves exact user whitelist",
                    "configured entityDistanceCulling", "configured entityCullingDistanceX", "configured entityCullingDistanceY"}
CAMERAS = {"horizontal-cutoff": [0,180,-150], "vertical-cutoff": [0,115,-40],
           "angle-left": [-100,180,-100], "angle-right": [100,180,-100],
           "offscreen-control": [0,180,-150], "missing-draw-control": [0,180,-150]}

def validate_report(report, present):
    cases = report.get("cases", [])
    if report.get("passed") is not True or report.get("complete") is not True or report.get("extrasPresent") is not present:
        raise ValueError("Incomplete compatibility evidence: " + str(report.get("failure")))
    if len(cases) != len(CASES) or {case["case"] for case in cases} != CASES or report.get("expectedCases") != len(CASES):
        raise ValueError("Missing or duplicate real camera cases")
    assertions = report.get("cullingAssertions", [])
    expected_assertions = BASE_ASSERTIONS | (EXTRAS_ASSERTIONS if present else set())
    if len(assertions) != len(expected_assertions) or set(assertions) != expected_assertions:
        raise ValueError("Missing transformed culling/configuration assertions")
    for case in cases:
        expected = not case["case"].endswith("-control")
        if case.get("camera", [])[:3] != CAMERAS[case["case"]]:
            raise ValueError("Camera did not exercise the required cutoff/angle")
        if any(case.get(key) is not value for key, value in
               (("passed", True), ("tracked", True), ("expectedVisible", expected), ("visiblePixels", expected))):
            raise ValueError("Invalid image evidence: " + case["case"])
        if abs(case.get("summonAge", -1) - 300) > .01 or (case.get("renders", 0) > 0) is not expected:
            raise ValueError("Incorrect lifecycle/draw control: " + case["case"])

def production_jar(target):
    props = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                 if "=" in line and not line.startswith("#"))
    return ROOT / target / "build/libs" / ("celestial-nail-" + target + "-" + props["mod_version"] + ".jar")

def dependencies(target):
    loader, game = target.split("-", 1)
    names = ["sodium-options-api-" + target]
    if loader == "forge": names += ["architectury-api-" + target]
    paths = [prepare(EMBEDDIUM)] if loader == "forge" else []
    if loader != "forge": names += ["sodium-" + target, "reeses-sodium-options-" + target]
    if loader == "fabric": names += ["fabric-api-" + target, "architectury-api-" + target]
    return paths + [prepare(COMPAT[name]) for name in names]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=TARGETS, action="append")
    parser.add_argument("--prepare-only", action="store_true")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--java", default=str(Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java")) if "JAVA_HOME" in os.environ else "java")
    args = parser.parse_args()
    from minecraft_launcher_lib import command, mod_loader
    targets = args.target or list(TARGETS)
    run_root = ROOT / "build/compat-runtime"
    launcher = ROOT / "build/compat-launcher"
    evidence = ROOT / "build/reports/sodium-extras-compat"
    evidence.mkdir(parents=True, exist_ok=True)
    for target in targets:
        loader, game = target.split("-", 1)
        mods = dependencies(target)
        extras = prepare(COMPAT["extras-" + target])
        installer = mod_loader.get_mod_loader(loader)
        version = installer.get_installed_version(game, TARGETS[target])
        if not (launcher / "versions" / version / (version + ".json")).exists():
            print("Installing", target, flush=True)
            installer.install(game, launcher, loader_version=TARGETS[target], java=args.java)
        if args.prepare_only: continue
        if not args.skip_build:
            subprocess.run([str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")),
                            ":" + target + ":build", ":" + target + ":compatTestProductionJar",
                            "--console=plain", "--max-workers=2"], cwd=ROOT, check=True)
        nail = production_jar(target)
        fixture = list((ROOT / target / "build/libs").glob("*-compat-test-dev.jar" if loader == "neoforge" else "*-compat-test.jar"))
        if len(fixture) != 1: raise ValueError("Expected one separate compatibility fixture jar")
        boom_name = "perfomant_boom-" + (loader if game == "1.20.1" else target)
        boom = Path.home() / ".m2/repository/com/nstut" / boom_name / "1.1.3" / (boom_name + "-1.1.3.jar")
        with zipfile.ZipFile(nail) as jar:
            if any("compattest/" in name for name in jar.namelist()): raise ValueError("Test driver leaked into release jar")
            if not any(name.endswith("SodiumExtrasEntityTypeMixin.class") for name in jar.namelist()): raise ValueError("Packaged compatibility hook missing")
        for present in (False, True):
            mode = "present" if present else "absent"
            game_dir = run_root / target / (mode + "-" + uuid.uuid4().hex)
            (game_dir / "mods").mkdir(parents=True)
            (game_dir / "config").mkdir()
            for mod in mods + [nail, fixture[0], boom] + ([extras] if present else []):
                shutil.copyfile(mod, game_dir / "mods" / mod.name)
            (game_dir / "config/sodiumextras-client.toml").write_text(
                '[embeddiumextras.performance.distanceCulling.entities]\nenable = true\ncullingMaxDistanceX = 4096\ncullingMaxDistanceY = 32\nwhitelist = ["minecraft:ghast"]\n')
            options = {"username": "NailCompat", "uuid": "fa37c12b098c413f91b05fffd2013805", "token": "",
                       "executablePath": args.java, "gameDirectory": str(game_dir), "launcherName": "NailCompat",
                       "jvmArguments": ["-Xmx2G", "-Dcelestial_nail.packagedCompatTest=true",
                           "-Dcelestial_nail.expectSodiumExtras=" + str(present).lower(),
                           "-Dcelestial_nail.compatRunRoot=" + str(run_root)]}
            launch = command.get_minecraft_command(version, launcher, options)
            log_path = evidence / (target + "-" + mode + ".log")
            print("Running packaged", target, mode, flush=True)
            with log_path.open("w", encoding="utf-8") as log:
                process = subprocess.run(launch, cwd=game_dir, stdout=log, stderr=subprocess.STDOUT, timeout=660)
            report_path = game_dir / "sodium-extras-compat-results.json"
            if not report_path.exists(): raise RuntimeError("No runtime report; inspect " + str(log_path))
            report = json.loads(report_path.read_text())
            shutil.copyfile(report_path, evidence / (target + "-" + mode + ".json"))
            if (game_dir / "evidence").exists(): shutil.copytree(game_dir / "evidence", evidence / (target + "-" + mode), dirs_exist_ok=True)
            if process.returncode != 0: raise RuntimeError("Packaged client failed: " + str(log_path))
            validate_report(report, present)
            report["productionJarSha512"] = hashlib.sha512(nail.read_bytes()).hexdigest()
            report["target"] = target
            (evidence / (target + "-" + mode + ".json")).write_text(json.dumps(report, indent=2) + "\n")
            print("PASS:", target, mode, flush=True)

if __name__ == "__main__": main()
