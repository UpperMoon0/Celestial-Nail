# Celestial Nail

Celestial Nail is a multi-version Minecraft mod that adds an administrator-controlled divine strike inspired by Genshin Impact's Celestial Nails.

## Supported targets

| Minecraft | Loader | Java |
| --- | --- | ---: |
| 1.20.1 | Fabric | 17 |
| 1.20.1 | Forge | 17 |
| 1.21.1 | Fabric | 21 |
| 1.21.1 | NeoForge | 21 |
| 26.1.2 | NeoForge | 25 |

The workspace follows the maintained NsTut multi-version split: loader-neutral code in `common`, version-specific entity/command/render code in `common-1.20.1`, `common-1.21.1`, and `common-26.1.2`, plus loader entrypoints for each target.

## Commands

All commands require game-master/operator permission. In single-player, enabling cheats grants the required permission.

```text
/celestialnail summon <id> <x> <y> <z> [power]
/celestialnail launch <id>
/celestialnail remove <id>
/celestialnail list
```

`id` must be unique across every loaded dimension. `power` is the crater radius in blocks, accepts `4` through `128`, and defaults to `32`. The summon position is the Nail's lower tip and the vertical line it will strike.

A summoned Nail is a real persistent, networked entity. It floats in place, displays its ID, and force-loads only its own chunk so it remains addressable after players leave or the server restarts. `/celestialnail launch <id>` accelerates it straight down until its tip intersects terrain.

## Impact behavior

On impact, the Nail damages nearby living entities and starts an inside-out spherical destruction wave. Terrain is removed in bounded per-tick batches instead of executing one giant vanilla explosion, reducing the single-tick cost of large strikes. Blocks do not drop items.

The destruction path intentionally uses server-authorized direct block mutation rather than a vanilla explosion or player break action. Therefore it intentionally ignores `mobGriefing` and protection filters that only cancel normal explosion/player-grief events, including FTB Chunks claim explosion/break protection. Treat the command as an administrative world-edit operation.

If a Nail occupies a chunk that was already force-loaded by an administrator or another system, removing the Nail will not un-force that pre-existing chunk. When multiple Nails share one chunk, force-load ownership is transferred until the last owning Nail is removed.

## Build

Run Gradle itself on Java 21; the 26.1.2 module selects a Java 25 toolchain for compilation.

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

Root convenience tasks also provide client/server runs for every supported target.