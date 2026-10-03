"""Install checksum-pinned, loader-specific render mods for normal dev clients."""
import shutil
import zipfile
from io import BytesIO
from pathlib import PurePosixPath

from runtime_artifacts import Artifact, COMPAT, EMBEDDIUM, ROOT, prepare


def install_embeddium_menu_hook(destination):
    """Adapt only the synthetic menu method in a local, non-release dev copy."""
    source = prepare(EMBEDDIUM)
    member = "me/jellysquid/mods/sodium/mixin/features/gui/hooks/settings/OptionsScreenMixin.class"
    old, new = b"lambda$init$2", b"method_19828"
    old_entry = b"\x01" + len(old).to_bytes(2, "big") + old
    new_entry = b"\x01" + len(new).to_bytes(2, "big") + new
    with zipfile.ZipFile(source) as original:
        hook = original.read(member)
        if hook.count(old_entry) != 1:
            raise ValueError("Pinned Embeddium menu hook has unexpected class constants")
        with zipfile.ZipFile(destination / "embeddium-0.3.31+mc1.20.1-dev.jar", "w") as adapted:
            for info in original.infolist():
                data = original.read(info.filename)
                if info.filename == member:
                    data = hook.replace(old_entry, new_entry)
                adapted.writestr(info, data)
    config = ROOT / "forge-1.20.1/run/client/config/embeddium-mixins.properties"
    if config.exists():
        data = config.read_bytes().replace(b"mixin.features.gui.hooks.settings=false",
                                          b"mixin.features.gui.hooks.settings=true")
        config.write_bytes(data)
    else:
        config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text("# Local Loom development workarounds; settings hook remains enabled.\n"
                          "mixin.features.render.gui.debug=false\n"
                          "mixin.features.render.immediate.buffer_builder.fast_delegate=false\n",
                          encoding="utf-8")


def install():
    for target in ("forge-1.20.1", "fabric-1.20.1", "fabric-1.21.1", "neoforge-1.21.1"):
        names = [f"extras-{target}", f"sodium-options-api-{target}"]
        if target == "forge-1.20.1":
            # Already on Forge's remapped shader runtime; do not install it twice.
            prepare(EMBEDDIUM)
        else:
            names += [f"sodium-{target}", f"reeses-sodium-options-{target}"]
        destination = ROOT / ".dependencies/dev-renderers" / target
        destination.mkdir(parents=True, exist_ok=True)
        for name in names:
            source = prepare(COMPAT[name])
            shutil.copyfile(source, destination / source.name)
        if target.startswith("fabric-"):
            # Loom strips embedded jars while remapping; expose the config port and
            # its bundled libraries from the already verified Extras archive.
            def expose_nested(data):
                with zipfile.ZipFile(BytesIO(data)) as jar:
                    for entry in jar.namelist():
                        if entry.startswith("META-INF/jars/") and entry.endswith(".jar"):
                            child = jar.read(entry)
                            (destination / PurePosixPath(entry).name).write_bytes(child)
                            expose_nested(child)
            expose_nested(prepare(COMPAT[f"extras-{target}"]).read_bytes())
        if target == "neoforge-1.21.1":
            mods = ROOT / target / "run/client/mods"
            mods.mkdir(parents=True, exist_ok=True)
            for name in names:
                source = prepare(COMPAT[name])
                shutil.copyfile(source, mods / source.name)
        if target == "forge-1.20.1":
            install_embeddium_menu_hook(destination)
            # Loom's loose dev classpath does not load the jar-in-jar runtime.
            with zipfile.ZipFile(prepare(COMPAT[f"sodium-options-api-{target}"])) as jar:
                (destination / "mixinextras-forge-0.4.1.jar").write_bytes(
                    jar.read("META-INF/jars/mixinextras-forge-0.4.1.jar"))
                libraries = ROOT / ".dependencies/dev-renderer-libraries/forge-1.20.1"
                libraries.mkdir(parents=True, exist_ok=True)
                (libraries / "fabric-api-base-0.4.31+ef105b4977.jar").write_bytes(
                    jar.read("META-INF/jars/fabric-api-base-0.4.31+ef105b4977.jar"))
            with zipfile.ZipFile(destination / "mixinextras-forge-0.4.1.jar") as jar:
                libraries = ROOT / ".dependencies/dev-renderer-libraries/forge-1.20.1"
                libraries.mkdir(parents=True, exist_ok=True)
                (libraries / "MixinExtras-0.4.1.jar").write_bytes(
                    jar.read("META-INF/jars/MixinExtras-0.4.1.jar"))
        print(f"{target}: installed {len(names)} pinned mods and their bundled runtime libraries")

    # Extras 1.21.1 targets Sodium's API and cannot accompany Embeddium 1.x.
    embeddium = Artifact(
        ".dependencies/dev-renderers/neoforge-1.21.1-embeddium/embeddium-1.0.15+mc1.21.1.jar",
        "https://cdn.modrinth.com/data/sk9rgfiA/versions/J7b96IEd/embeddium-1.0.15%2Bmc1.21.1.jar",
        "d073cf52dcf2aeec3ac9f746d4571c7b4b1ad746fa11d4bcc5a21a264ae3119ac6d341d08cc9a70042cf0ddffc3b3ef3329ed30217035d4551c9d6788b6dc1e6")
    source = prepare(embeddium)
    mods = ROOT / "neoforge-1.21.1/run/client-embeddium/mods"
    mods.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, mods / source.name)
    print("neoforge-1.21.1: optional Embeddium profile installed (without Extras)")
    print("26.1.2: neither Embeddium nor Sodium Extras has a matching published build")


if __name__ == "__main__":
    install()
