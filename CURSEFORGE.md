# Celestial Nail

Summon a towering celestial monument, open a luminous sky rift, and unleash a cinematic strike that carves a vast crater into your world.

Inspired by the Celestial Nails in Genshin Impact, **Celestial Nail** is an administrator-controlled Minecraft mod for dramatic events, creative worlds, and custom server scenarios.

## A strike with a lasting presence

- Watch an ivory-and-crystal Nail emerge through a glowing four-point rift and hover above your chosen location.
- Choose its size independently of its destructive power: the default Nail stands 72 blocks tall.
- Launch it straight down to create an expanding destruction wave, with flashes, sound, dust, fragments, and camera effects.
- Leave the embedded Nail as a permanent landmark, or remove it with a harmless crumble animation.
- Nails and their impact progress persist with the world.

Terrain clearing is spread across server ticks using **Perfomant Boom**. Large strikes still do substantial work; completion time depends on the world and server.

## Getting started

Enable cheats in single-player or use an account with game-master/operator permission on a server.

```text
/celestialnail summon sky_nail ~ ~80 ~ 32 1
/celestialnail launch sky_nail
/celestialnail list
/celestialnail remove sky_nail
```

Wait for the emergence animation to finish before launching. The summon coordinates mark the floating tip and the vertical strike line. In this example, `32` is the crater radius in blocks and `1` is the visual scale.

Power accepts **4–128**, default **32**. Scale accepts **0.1–4**, default **1**. Scaling the model does not change the crater radius. Use a different ID for each Nail.

## Installation

Install Celestial Nail and **Perfomant Boom 1.1.0** for the same Minecraft version and loader on both the server and participating clients. In single-player, install them in your client instance.

| Minecraft | Loader | Other required mods | Java |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Fabric API, Architectury API 9.2.14+ | 17 |
| 1.20.1 | Forge | Architectury API 9.2.14+ | 17 |
| 1.21.1 | Fabric | Fabric API, Architectury API 13.0.8+ | 21 |
| 1.21.1 | NeoForge | None beyond Perfomant Boom | 21 |
| 26.1.2 | NeoForge | None beyond Perfomant Boom | 25 |

Fabric targets require Fabric Loader **0.18.4 or newer**. Choose the download for your exact game version and loader; the JARs are not interchangeable.

## Before launching a strike

This is an administrative world-edit tool. **Impacts intentionally bypass `mobGriefing` and protection that only intercepts ordinary explosions or player block breaking, including those FTB Chunks claim filters.** Back up worlds you want to keep and restrict command access accordingly.

Cleared blocks and vanilla container contents do not drop items. Living entities can take damage during the blast. Removing a Nail stops further damage and clearing, but does not restore terrain already removed. Fluids outside the crater can flow back naturally.

## Help and feedback

[Source and documentation](https://github.com/UpperMoon0/Celestial-Nail) · [Report a problem](https://github.com/UpperMoon0/Celestial-Nail/issues)

For a crash or unexpected impact, include the Minecraft version, loader, Nail and Boom versions, command used, and the crash report or latest log.

Created by **NsTut**. All Rights Reserved.
