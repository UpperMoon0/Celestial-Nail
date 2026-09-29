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

`/celestialnail remove <id>` starts a 2.7-second harmless removal: stone sections subtly separate along real fragment boundaries before releasing into thick tumbling fragments and dust, then the entity disappears. This works while emerging, floating, descending, impacting or embedded. Removal cancels all further damage and terrain clearing immediately; the visual debris never places or breaks blocks. Repeating the command does not restart the animation.

A summoned Nail is a real persistent, networked entity. It floats in place, displays its ID, and force-loads only its own chunk so it remains addressable after players leave or the server restarts. `/celestialnail launch <id>` accelerates it straight down until its tip intersects terrain.

## Impact behavior

On impact, the Nail starts an inside-out spherical destruction wave. During the active blast, nearby living entities take damage every ten ticks; the traveling shock front also damages living entities it reaches outside the crater. Normal damage immunity frames are respected. Damage stops when the blast and wave finish, or immediately when an administrator removes the nail. The nail pierces downward over 18 ticks, with most penetration in the initial impact, until its tip is embedded below the crater floor, then remains permanently. Its embedded phase, exact impact height and blast-completion state are saved, so loading the world does not restart the explosion. Terrain is removed in bounded per-tick batches instead of executing one giant vanilla explosion. All impacts in a dimension share a limit of 3,000 block changes, 45,000 scan steps and one new impact-chunk request per tick, plus an eight-millisecond cooperative work window. The window stops before the next operation; it cannot preempt an individual vanilla or mod callback and is not a guaranteed server tick time. Cold impact chunks are requested without synchronously loading them. Blocks and vanilla container inventories do not drop items; custom block callbacks may define additional behavior.

The wave clears fluid-bearing blocks as well as solids, suppresses neighbor-shape update cascades during removal, and finishes with two bounded top-down fluid sweeps separated by a settling period. Source water, flowing water, lava and waterlogged states are cleared inside the sphere, including both valid build-height boundaries. A final bounded pass reconciles the shapes and survival of the one-block six-neighbor boundary. Unsupported attachments in that layer can disappear without drops; fluid ticks scheduled by boundary states are allowed to run naturally. This is not a general recursive redstone/physics settlement pass. Sources outside the blast remain intact and can subsequently flow naturally; the mod does not install permanent invisible fluid barriers.

The destruction path intentionally uses server-authorized direct block mutation rather than a vanilla explosion or player break action. Therefore it intentionally ignores `mobGriefing` and protection filters that only cancel normal explosion/player-grief events, including FTB Chunks claim explosion/break protection. Treat the command as an administrative world-edit operation.

Deep embedding is exempt from vanilla below-world entity disposal, and removed entities cannot reacquire tickets. Visual-sized bounds do not participate in fluid-volume scans. Boundary progress is saved; completed impacts cannot accept temporary impact-ticket ownership.

If a Nail occupies a chunk that was already force-loaded by an administrator or another system, removing the Nail will not un-force that pre-existing chunk. When multiple Nails share one chunk, force-load ownership is transferred until the last owning Nail is removed.

## Cataclysm atmosphere and visibility

Arrival adds a cold screen tint, rising dust, a distant sky crack and a low drone. The floating nail carries slow suspended fragments, a fine shader pulse along its crystal inlays and occasional crystal creaks. Launch keeps the fragments orbiting, sends the pulse down toward the tip, swells the charge sound and leaves a short silence before descent.

Impact produces a brief blue-white flash, a vertical light column and a soft procedural shock front with dust. The pulse and shockwave use procedural fragment shading rather than textured ring meshes. The local boom and restrained camera shake arrive with the wave (12 blocks per tick after an eight-tick delay). Smoke, falling fragments, drifting ash, blue particle seams on the crater bowl and intermittent aftershocks fade over one minute. These are client effects; they do not create falling-block entities or add chunk tickets. Particle spawning is capped at 48 per client tick across all nails. Effects honor horizontal visibility and audio category volume; other game sounds are attenuated during the sequence. The original portal excerpt is retained, with six compact original procedural sound layers generated by `tools/generate_cataclysm_audio.py`.

Nail and portal visibility uses horizontal distance, so altitude does not consume the render-distance budget. Their shader bypasses vanilla vertical fog and applies a smooth fade over the final two horizontal chunks. The camera far plane expands only for a tracked, horizontally visible nail or impact column; terrain fog and chunk loading remain unchanged. Both projection and frustum clipping use the expanded distance. The impact timestamp is saved and synchronized; existing impacts without a timestamp do not replay a cinematic on upgrade.

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

## Verification

See [TESTING.md](TESTING.md) for shared tests, isolated GameTests, their coverage and the remaining cross-version/live-client validation limits. The baseline source audit is recorded in [docs/runtime-audit.md](docs/runtime-audit.md).

## Model review renders

Run `tools/render_nail_job.ps1` to compile and export the actual shared entity mesh, then render the game atlas from front, back, left, right, top and bottom with close-ups. The output is `docs/celestial-nail-model-preview.png`. This is an offline mesh/material review, not an in-game capture; the portal, shader effects and world lighting are excluded. `docs/celestial-nail-model-before.png` preserves the previous design for comparison.

The refined model uses a wider crown, lower asymmetric rear petals, beveled stone-and-gold tracery, elongated glass lancets, irregular stone cap fragments, staggered casing fractures and illuminated cube debris. Its flat facets and nearest-neighbor pixel atlas preserve the Minecraft style. Hidden internal molding caps are omitted to keep the body and crystal debris below the existing 10,000-quad budget.


Crystal highlights are procedural: blue internal light, a camera-dependent sheen and sparse four-point stars animate on the crystal faces. The former short fixed glint strips are removed; thin facet edges remain part of the model. The shader preserves the pixel atlas, distance fade and crumble fade. Animation time travels with each nail's vertices, so several nails with different ages can share a render batch safely. This is stylized emissive shading, not screen-space bloom, refraction or illumination of nearby blocks.

For an animated GPU review, install Python packages `numpy Pillow moderngl glcontext`, export the mesh with `tools/render_nail_job.ps1`, then run `python tools/preview_crystal_shader.py`. It compiles both shader versions and renders the actual mesh/fragment shader into `build/crystal-glint-preview/`, checking animation, camera response and the clock loop. The preview substitutes Minecraft's matrix imports with equivalent uniforms; it is not an in-game capture.


The pulse now travels as staggered streams through individual crystal facets with a soft blue afterglow, rather than a continuous bright band. Run `tools/render_nail_shader_job.ps1 -Open` for an interactive offline preview with playback, scrubbing, turntable rotation and close-up views. The generated HTML is self-contained and works without starting Minecraft.

The embedded nail bypasses the terrain-section visibility gate for its buried tip, while retaining depth testing, model frustum culling and horizontal fading. Its camera depth range covers the entire body throughout impact and aftermath.

The portal opening audio has its own horizontal falloff: full strength through half its reach, then a smooth fade to a minimum 512-block radius (or twice the configured render distance, if larger). Portal altitude does not attenuate it. This applies to newly summoned nails received by the client; the server's entity-tracking/view-distance limit still controls which clients receive a nail. Ambient volume and the launch/removal fades still apply.
