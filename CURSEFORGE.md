# Celestial Nail

**Your world is corrupted, Heavenly One. Summon a colossal celestial Nail above a mortal settlement, send it crashing down, and replace its transgressions with a massive crater.** Through administrator commands, decree where the Nail appears, how large it stands, and how much terrain your judgment erases.

Your subjects have unearthed forbidden knowledge. They call it “the duper.”

Their corruption spreads across your world. They call it “a starter base.”

**You have heard their explanations. Send down the Nail.**

## Restore order from above

Open a luminous sky rift and summon a towering Nail of stone and crystal. Leave it floating above the offending settlement while its inhabitants reconsider their relationship with the heavens.

Then launch it.

The Nail descends, unleashes an expanding destruction wave, and remains embedded in the crater as a lasting monument to your moderation policy.

- **A divine presence:** 72 blocks tall at default scale, with an animated emergence and floating sky portal.
- **Judgment of your choosing:** adjust the Nail’s size independently of its crater radius.
- **An appropriately dramatic arrival:** flashes, a light column, shockwaves, dust, fragments, sound and camera effects.
- **Consequences that persist:** Nails and their impact progress save with the world.
- **Mercy, technically:** cancel an active strike with a harmless crumble animation. Previously destroyed terrain remains destroyed. You are a god, not a backup service.

## Issue your decree

With cheats enabled or game-master/operator permission:

```text
/celestialnail summon judgment ~ ~80 ~ 32 1
```

Wait for the Nail to finish emerging. Give your subjects a moment to appreciate the architecture.

```text
/celestialnail launch judgment
```

The final two summon arguments are **crater radius** and **visual scale**:

- **Power:** 4–128, default 32.
- **Scale:** 0.1–4, default 1.
- Larger appearance does not increase crater radius. Even divine administration has separate controls.

Manage your instruments:

```text
/celestialnail list
/celestialnail remove judgment
```

Use a unique ID for each Nail. Your bureaucracy should be more organized than theirs.

## Before you purify the server

**This is an administrator-controlled world-edit tool.** Impacts intentionally bypass `mobGriefing` and protection that only intercepts ordinary explosions or manual block breaking, including those FTB Chunks claim filters.

Cleared blocks and vanilla container contents **do not drop items**. Living entities can take damage. Removing a Nail stops further damage and clearing, but does not restore what has already been destroyed. Outside fluids may flow back into the crater.

Back up worlds you want to keep. If you share your realm, give your subjects an apocalypse they signed up for.

Terrain work is spread across server ticks through **Perfomant Boom**. Large strikes still take substantial work. Heavenly authority does not upgrade your CPU.

## Installation

Install **Celestial Nail** and **Perfomant Boom 1.1.3 or newer within the 1.x series** for the same Minecraft version and loader on both the server and every connecting client. For a local world, install both in your client instance.

| Minecraft | Loader | Other required mods | Java |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Fabric API, Architectury API 9.2.14+ | 17 |
| 1.20.1 | Forge | Architectury API 9.2.14+ | 17 |
| 1.21.1 | Fabric | Fabric API, Architectury API 13.0.8+ | 21 |
| 1.21.1 | NeoForge | None beyond Perfomant Boom | 21 |
| 26.1.2 | NeoForge | None beyond Perfomant Boom | 25 |

Fabric requires Loader **0.18.4+**. Choose the file for your exact game version and loader.

## Inspiration

A fan-made Minecraft interpretation of Genshin Impact’s Celestial Nails. The divine decrees above are roleplay; “purification” here means destructive terrain editing, not an implemented corruption or forbidden-knowledge system.

[Source and documentation](https://github.com/UpperMoon0/Celestial-Nail) · [Report a problem](https://github.com/UpperMoon0/Celestial-Nail/issues)

Created by **NsTut**. Licensed under the **MIT License**.

### Rendering compatibility

The nail uses vanilla entity rendering with lightmap and normal lighting. Crystals remain luminous; portal and impact effects use vanilla emissive rendering. Shader packs control bloom and fog. This replaces the custom core shader that could disappear under shader replacement. The mod does not add dynamic block lighting.
