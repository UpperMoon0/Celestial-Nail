"""Shared, named, SHA-512 pinned artifacts for isolated client regressions."""
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]

@dataclass(frozen=True)
class Artifact:
    path: str
    url: str
    sha512: str

def verify(data, expected):
    if hashlib.sha512(data).hexdigest() != expected:
        raise ValueError("Pinned artifact SHA-512 mismatch")

def prepare(artifact):
    path = ROOT / artifact.path
    if path.exists():
        verify(path.read_bytes(), artifact.sha512)
    else:
        request = urllib.request.Request(artifact.url, headers={"User-Agent": "Celestial-Nail-regression/1.0"})
        with urllib.request.urlopen(request, timeout=60) as response:
            data = response.read()
        verify(data, artifact.sha512)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return path

COMPAT = {name: Artifact(**definition) for name, definition in
          json.loads((ROOT / "tools/compat-artifacts.json").read_text()).items()}
EMBEDDIUM = Artifact(
    ".dependencies/shaders/embeddium-0.3.31+mc1.20.1.jar",
    "https://cdn.modrinth.com/data/sk9rgfiA/versions/UTbfe5d1/embeddium-0.3.31%2Bmc1.20.1.jar",
    "ffbf2da4685260a4d5c14c621708bd20722563f084f042d3dfb0a7b87f048e39299648c854a93939129da0d23a15a91ec628560d601e76074b08e275f6e132e9")
