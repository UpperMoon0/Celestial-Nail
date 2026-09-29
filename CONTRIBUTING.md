# Contributing

For player setup and command examples, start with [README.md](README.md). Report bugs through [GitHub Issues](https://github.com/UpperMoon0/Celestial-Nail/issues) with game/loader versions, both Nail and Boom versions, the triggering command, logs and a minimal reproduction.

## Local development

Run Gradle on Java 21. Target toolchains use Java 17, 21 or 25. On Windows, use `gradlew.bat` in place of `./gradlew`.

First build the companion [Perfomant Boom](https://github.com/UpperMoon0/Perfomant-Boom) revision that supplies `boom_version` from [gradle.properties](gradle.properties):

```sh
# In the Boom checkout
./gradlew buildAll publishToMavenLocal
# In the Nail checkout
./gradlew :common:test buildAll
./gradlew :neoforge-1.21.1:runGameTestServer
```

For example, `./gradlew runForge1201Client` starts the Forge 1.20.1 dev client; `runNeoForge1211Client` starts NeoForge 1.21.1. All client/server aliases are in [build.gradle](build.gradle). Use disposable worlds for destructive tests.

If you republish Boom under the same version, stop the dev client and run Nail's affected build with `--refresh-dependencies` before relaunching. Loom caches remapped dependencies. Check the actual runtime JAR when diagnosing missing classes: a 1.1.0 label alone cannot distinguish two local revisions. The `SphereShellCursor` impact crash was caused by such a stale JAR.

## Changes and review

- Keep reusable terrain operations in Boom and strike-specific entity behavior here. Follow [ARCHITECTURE.md](docs/ARCHITECTURE.md).
- Preserve saved field meanings or provide an explicit migration. Check cancellation and chunk ownership when changing stages.
- Inspect the exact version's vanilla/loader code for lifecycle changes; record evidence and limitations rather than assuming cross-version equivalence.
- Use [TESTING.md](TESTING.md) to select meaningful regression and runtime checks. A build does not prove rendering or a complete impact.
- Update [CHANGELOG.md](CHANGELOG.md), compatibility guidance and [CURSEFORGE.md](CURSEFORGE.md) when users' setup or behavior changes.

CI resolves Boom from repository variable `PERFOMANT_BOOM_REF`, defaulting to `main`. Coordinate API and consumer revisions and land the Boom API first. See [RELEASING.md](RELEASING.md) for release preparation; the publisher requires configured CurseForge IDs, a pinned Boom commit and passing validation.

Contributions are distributed under the repository's [MIT License](LICENSE). Keep loader metadata consistent with that license.
