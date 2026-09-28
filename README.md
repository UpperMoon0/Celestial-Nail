# Celestial Nail

Celestial Nail is a multi-version Minecraft mod workspace.

## Supported targets

| Minecraft | Loader | Java |
| --- | --- | ---: |
| 1.20.1 | Fabric | 17 |
| 1.20.1 | Forge | 17 |
| 1.21.1 | Fabric | 21 |
| 1.21.1 | NeoForge | 21 |
| 26.1.2 | NeoForge | 25 |

The workspace follows the same split used by the maintained NsTut multi-version mods:

- `common` — loader- and Minecraft-independent code.
- `common-1.20.1` / `common-1.21.1` — Architectury/Loom common source sets for their Minecraft line.
- `fabric-1.20.1`, `forge-1.20.1`, `fabric-1.21.1` — loader-specific Loom modules.
- `neoforge-1.21.1` and `neoforge-26.1.2` — standalone ModDevGradle modules. The 26.1.2 lane intentionally uses Java 25.

## Build

```bash
./gradlew buildAll
```

Individual lanes:

```bash
./gradlew :fabric-1.20.1:build
./gradlew :forge-1.20.1:build
./gradlew :fabric-1.21.1:build
./gradlew :neoforge-1.21.1:build
./gradlew :neoforge-26.1.2:build
```

Convenience run tasks are available at the root, for example `runFabric1201Client`, `runForge1201Client`, `runFabric1211Client`, `runNeoForge1211Client`, and `runNeoForge2612Client`.
