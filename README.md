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
/celestialnail summon <id> <x> <y> <z> [power] [scale]
/celestialnail launch <id>
/celestialnail remove <id>
/celestialnail list
```

`id` must be unique across every loaded dimension. `power` is the crater radius in blocks, accepts `4` through `128`, and defaults to `32`. The summon position is the Nail's final floating tip and the vertical line it will strike. `scale` defaults to `1` (72 blocks tall), accepts `0.1` through `4`, and scales the entire nail and portal uniformly. It does not change explosion power.

For example, `/celestialnail summon sky_nail ~ ~80 ~ 32 1` creates a 72-block nail with a 32-block impact radius. Use `32 0.5` for 36 blocks tall or `32 2` for 144 blocks tall.

On summon, a luminous four-point rift opens over 1.5 seconds. The nail emerges tip-first through it over the next 7 seconds, with geometry above the aperture hidden. The nail continues down until its tip reaches the command coordinates, leaving a gap of half its height between crown and portal (36 blocks at scale 1). The portal stays open at that higher anchor while the nail floats. Previously saved floating nails receive the larger gap on reload. Launch is available once emergence finishes; it collapses the portal and fades its sound over 1.5 seconds, then begins descent. The animation clock, scale and portal anchor are synchronized and saved, so reconnecting clients and reloaded worlds retain the sequence. The opening sound plays once to nearby clients rather than restarting when an old nail is encountered.

The supplied audio source is preserved at `assets/audio/portal-source.mp3`; the game streams a 10-second mono Ogg Vorbis opening excerpt with a two-second fade-out from `common/src/main/resources/assets/celestial_nail/sounds/portal_open.ogg`. Regenerate it with `tools/prepare_portal_audio.ps1` (FFmpeg required).

A summoned Nail is a real persistent, networked entity. It floats in place, displays its ID, and force-loads only its own chunk so it remains addressable after players leave or the server restarts. `/celestialnail launch <id>` accelerates it straight down until its tip intersects terrain.

## Impact behavior

On impact, the Nail damages nearby living entities and starts an inside-out spherical destruction wave. Terrain is removed in bounded per-tick batches instead of executing one giant vanilla explosion, reducing the single-tick cost of large strikes. Blocks do not drop items.

The wave clears fluid-bearing blocks as well as solids, suppresses neighbor-shape update cascades during removal, and finishes with two bounded top-down fluid sweeps separated by a settling period. Source water, flowing water, lava and waterlogged states are cleared inside the sphere. Sources outside the blast remain intact and can subsequently flow naturally; the mod does not install permanent invisible fluid barriers.

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
