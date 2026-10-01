"""Validate a stationary-camera Oculus fixture with one fully emerged Nail.
Verifies custom draw-target pixels, not the shader pack's final composite.
"""
import argparse
from pathlib import Path
import re


def check_log(text):
    samples = []
    for line in text.splitlines():
        if "[NailRenderDebug]" not in line:
            continue
        fields = dict(re.findall(r"\b(\w+)=([^\s]+)", line))
        pack = re.search(r"\bpackInUse=(true|false)\b", line)
        fields["packInUse"] = pack.group(1) if pack else "unknown"
        if fields.get("check") == "NOT_ARMED":
            continue
        samples.append(fields)
    if not samples:
        raise ValueError("No armed samples; enable diagnostics, pixelProbe and requireVisibleDraw.")
    for sample in samples:
        if sample.get("forceVisible") != "false" or sample.get("disableDistanceFade") != "false":
            raise ValueError("Visibility overrides invalidate the normal rendering regression.")
        if sample.get("packInUse") != "true":
            raise ValueError("Oculus shader pack is not confirmed active in every fixture sample.")
        if sample.get("check") != "PASS" or sample.get("result") != "PIXELS_CHANGED":
            raise ValueError("Render fixture failed: " + sample.get("result", "unknown"))
        if int(sample.get("changedPixels", "0")) <= 0:
            raise ValueError("No verified RGB pixel changes.")
    if len(samples) < 2:
        raise ValueError("Capture at least two successful intervals to exclude a transient draw.")
    return len(samples)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    args = parser.parse_args()
    try:
        count = check_log(args.log.read_text(encoding="utf-8", errors="replace"))
    except (ValueError, OSError) as error:
        parser.exit(1, f"FAIL: {error}\n")
    print(f"PASS: {count} live Oculus intervals changed Nail draw-target pixels.")


if __name__ == "__main__":
    main()
