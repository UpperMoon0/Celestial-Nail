# Celestial Nail

Celestial Nail is a Minecraft adaptation of the celestial pillars seen in Genshin Impact. It brings their imposing stone-and-crystal appearance into administrator-controlled world events.

## Inspiration

In Genshin's lore, these Nails were cast down from the heavens and can profoundly alter the surrounding land. Their role includes purifying and stabilizing powers foreign to Teyvat; their arrival can also have devastating consequences. See the [Celestial Nail lore overview and in-game references](https://www.hoyodex.com/wiki/genshin-impact/celestial-nail) and [HoYoWiki's Dragonspine entry](https://wiki.hoyolab.com/pc/genshin/entry/5112?lang=en-us).

This mod interprets that imagery as a summonable, persistent structure with a staged descent and terrain-clearing impact. The sky portal, adjustable scale, crater radius and removal animation are Minecraft gameplay and visual choices. It does not simulate Genshin's Ley Lines or purification of forbidden knowledge.

## Features

- **A persistent monument:** a 72-block-tall Nail at default scale, with a tip-first emergence animation and a floating sky portal.
- **Independent size and power:** choose the visual scale separately from the crater radius.
- **A staged strike:** downward acceleration, an expanding destruction wave, entity damage and deep embedding.
- **Cinematic effects:** flashes, a light column, procedural shockwave, dust, fragments, sound and camera effects.
- **Saved progress:** Nails and their strike stages persist across world saves; completed impacts do not restart on reload.
- **Controlled removal:** an administrator can cancel further damage and clearing, followed by a harmless crumble animation.

Terrain work is spread across server ticks through the required Perfomant Boom library. Large strikes still perform substantial work; completion time depends on terrain, chunk readiness and server load.

## Installation

Install Nail and **Perfomant Boom 1.1.0 or newer within the 1.x series** for the same Minecraft version and loader on the server and every participating client. In single-player, install both in your client instance. Boom is a separate dependency, not bundled inside Nail.

| Minecraft | Loader | Additional required mods | Java |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Fabric API, Architectury API 9.2.14+ | 17 |
| 1.20.1 | Forge | Architectury API 9.2.14+ | 17 |
| 1.21.1 | Fabric | Fabric API, Architectury API 13.0.8+ | 21 |
| 1.21.1 | NeoForge | None beyond Boom | 21 |
| 26.1.2 | NeoForge | None beyond Boom | 25 |

Fabric Loader must be **0.18.4+**. The NeoForge 1.21.1 artifact requires NeoForge **21.1.228 or newer within 21.1.x** and Minecraft **1.21.1**. Choose runnable JARs, excluding sources/dev JARs, and remove obsolete duplicates when updating. The supported-target matrix does not imply that every artifact is already published.

Nail currently develops and tests against Boom 1.1.0; metadata accepts `>=1.1.0 <2.0.0`. That range does not certify untested future versions. See [compatibility](docs/COMPATIBILITY.md) for full setup and conflict notes.

## Quick start

Use a disposable world with cheats enabled, or an account with game-master/operator permission.

```mcfunction
/celestialnail summon sky_nail ~ ~80 ~ 32 1
```

This places the floating tip 80 blocks above the command source, with crater radius 32 and visual scale 1. Wait for emergence to finish (about 8.5 seconds), then launch:

```mcfunction
/celestialnail launch sky_nail
```

List existing IDs or remove a Nail:

```mcfunction
/celestialnail list
/celestialnail remove sky_nail
```

`power` accepts **4–128**, default **32**. `scale` accepts **0.1–4**, default **1**. Scale changes appearance, not crater radius. Use a unique ID for each Nail. Read [USAGE.md](USAGE.md) for full syntax, timing, cancellation and troubleshooting.

## World changes and limits

This is an administrative world-edit tool. Impacts intentionally bypass `mobGriefing` and protection that only intercepts ordinary explosions or player block breaking, including those FTB Chunks claim filters. Back up worlds and restrict command access accordingly.

Cleared blocks and vanilla container contents do not drop items. Living entities can take damage during the strike. Removal stops further damage and terrain work immediately but **does not restore existing damage or terrain**. External fluid sources remain intact and can flow back after cleanup.

Nails retain their own chunk access; temporary impact chunks are managed separately. Existing forced chunks are preserved. Work limits are cooperative, not a promise of lag-free operation. Arbitrary modded callbacks and recursive physics behavior are not guaranteed.

## Automation and development

Use [INTEGRATION.md](INTEGRATION.md) for command blocks, datapack functions and the boundary between command automation and Java internals. Nail does not currently document a stable public Java integration API.

For source builds, run Gradle on Java 21 with Java 17/21/25 target toolchains. First publish the Boom revision matching Nail's `boom_version` to Maven local, then run `./gradlew buildAll` in Nail (`gradlew.bat` on Windows). [CONTRIBUTING.md](CONTRIBUTING.md) explains dependency setup and development runs.

## Documentation and support

- [Usage](USAGE.md) and [integration](INTEGRATION.md)
- [Compatibility and troubleshooting](docs/COMPATIBILITY.md)
- [Detailed behavior and visual reference](docs/REFERENCE.md)
- [Architecture](docs/ARCHITECTURE.md), [contributing](CONTRIBUTING.md) and [testing](TESTING.md)
- [Releasing](RELEASING.md), [changelog](CHANGELOG.md) and [CurseForge description source](CURSEFORGE.md)

Report issues with game/loader versions, Nail and Boom versions, the command used, reproduction steps and logs through [GitHub Issues](https://github.com/UpperMoon0/Celestial-Nail/issues). Build coverage spans all five targets; complete-impact runtime tests focus on NeoForge 1.21.1 and do not certify every target's visuals or performance.

Created by **NsTut**. Licensed under the [MIT License](LICENSE).
