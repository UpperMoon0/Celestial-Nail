# Releasing Celestial Nail

## Current automation

[validate.yml](.github/workflows/validate.yml) runs shared tests, all five target builds, release-tooling regressions, the NeoForge 1.21.1 runtime suite, and packaged Sodium Extras compatibility on all four applicable targets in absent/present modes with tracked-Nail final-image checks. [release.yml](.github/workflows/release.yml) reuses these checks before packaging and publishing all five artifacts to CurseForge and GitHub. A main-branch version change, an untagged release repair, or a manual run on main can release; feature branches and tag-only pushes do not publish.

## Prepare a release

1. Make the required Boom API/artifacts available first. Match Nail's `boom_version` and ensure CI's `PERFOMANT_BOOM_REF` resolves that implementation.
2. Set `mod_version` in [gradle.properties](gradle.properties) for the intended release, add the matching `changelog/<version>.md`, and update [compatibility](docs/COMPATIBILITY.md) and [CURSEFORGE.md](CURSEFORGE.md).
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

## Publication configuration

Set repository variables `CURSEFORGE_PROJECT_ID` (Nail), `PERFOMANT_BOOM_PROJECT_ID` (Boom), and `PERFOMANT_BOOM_REF` (the full 40-character tested Boom commit SHA), plus the secret `CURSEFORGE_API_TOKEN`. The release preflight rejects missing IDs or an unpinned dependency. The CurseForge job rejects a missing token. No secret is sent to validation jobs.

Add nonempty release notes at `changelog/<mod_version>.md`. Checksums and a source-commit manifest verify the exact artifact set before upload. Existing tags cannot move; releases cannot downgrade the fetched version history. Same-commit retries can finish a draft, while already-public GitHub assets must match their recorded checksums. Inspect partial CurseForge uploads before retrying because the service may reject duplicates.

The workflow publishes the reviewed artifacts and release notes to the configured destinations. Set the exact Minecraft and loader tags per file. Declare Perfomant Boom as a required dependency for every file, Fabric API for Fabric files, and Architectury API for both 1.20.1 loaders and Fabric 1.21.1. Keep Boom external; never upload a combined shaded Nail/Boom JAR.

Paste [CURSEFORGE.md](CURSEFORGE.md) into the project description editor and upload [icon.png](icon.png) as the project icon. These files are local page assets; committing them does not update the website or automatically package the icon into the mod. Verify the published files and dependency declarations after upload. No project ID, credentials or existing published version are assumed by this document.

Set the public project license to MIT, matching [LICENSE](LICENSE) and the loader metadata.
