# Releasing Celestial Nail

## Current automation

[validate.yml](.github/workflows/validate.yml) runs shared tests, all five target builds and the NeoForge 1.21.1 runtime suite. It does **not** publish GitHub or CurseForge releases. Changing `mod_version`, creating a tag or editing the description file does not install an automatic publisher.

## Prepare a release

1. Make the required Boom API/artifacts available first. Match Nail's `boom_version` and ensure CI's `PERFOMANT_BOOM_REF` resolves that implementation.
2. Set `mod_version` in [gradle.properties](gradle.properties) for the intended release, consolidate the Unreleased notes in [CHANGELOG.md](CHANGELOG.md), and update [compatibility](docs/COMPATIBILITY.md) and [CURSEFORGE.md](CURSEFORGE.md).
3. Publish the matching Boom artifacts to Maven local, then run `./gradlew :common:test buildAll` and `./gradlew :neoforge-1.21.1:runGameTestServer` from Nail with Gradle on Java 21.
4. Retain runtime evidence and explicitly record untested loaders/features. Exercise summon, emergence, launch, impact, removal and world reload in disposable worlds for the affected target. Check that the actual loaded Boom JAR contains the current API, especially after same-version local iteration.
5. Collect only the runnable JARs below, check embedded version/dependency metadata and record checksums against the release commit. Exclude `sources`, `dev` and `dev-shadow` artifacts.

| Target | Artifact directory |
| --- | --- |
| Fabric 1.20.1 | `fabric-1.20.1/build/libs/` |
| Forge 1.20.1 | `forge-1.20.1/build/libs/` |
| Fabric 1.21.1 | `fabric-1.21.1/build/libs/` |
| NeoForge 1.21.1 | `neoforge-1.21.1/build/libs/` |
| NeoForge 26.1.2 | `neoforge-26.1.2/build/libs/` |

## Manual publication

Publish the reviewed artifacts and release notes to the actual project destinations. Set the exact Minecraft and loader tags per file. Declare Perfomant Boom as a required dependency for every file, Fabric API for Fabric files, and Architectury API for both 1.20.1 loaders and Fabric 1.21.1. Keep Boom external; never upload a combined shaded Nail/Boom JAR.

Paste [CURSEFORGE.md](CURSEFORGE.md) into the project description editor and upload [icon.png](icon.png) as the project icon. These files are local page assets; committing them does not update the website or automatically package the icon into the mod. Verify the published files and dependency declarations after upload. No project ID, credentials or existing published version are assumed by this document.

Nail's metadata declares All Rights Reserved. Keep public license settings consistent with the maintainer's licensing decision.
