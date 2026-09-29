# Verification

Run Gradle with Java 21. The 26.1.2 module resolves Java 25 for compilation.

## Shared deterministic tests

```sh
./gradlew :common:test
```

The 32 tests include existing animation/mesh/shell/fluid checks and eight new work-budget/boundary checks. New coverage verifies shared per-level ownership of an allowance, change and scan caps, chunk-request caps, elapsed-time cutoffs, per-tick reset, a six-neighbor brute-force boundary oracle, duplicate rejection, saved-cursor reconstruction, and quadratic traversal at maximum power.

A budget is cooperative: it limits when the next operation starts. It does not claim to preempt a slow third-party callback or measure end-to-end server/client performance. Current allocation is shared, not a round-robin fairness scheduler.

## All supported builds

```sh
./gradlew buildAll
```

This compiles and packages Fabric/Forge 1.20.1, Fabric/NeoForge 1.21.1 and NeoForge 26.1.2. Builds alone are not runtime or visual verification.

## Isolated real-server regression suite

```sh
./gradlew :neoforge-1.21.1:runGameTestServer
```

This launches a disposable GameTest server in `neoforge-1.21.1/run/runtime-tests`. It never connects to the player's server or existing smoke world. Tests live in an isolated `gameTest` source set and are not shipped in the production jar. The dedicated runtime task exits nonzero when required tests fail. Successful evidence must include **all twenty-four required tests**, not merely a server startup message.

The suite exercises:

1. A removed entity's tick and forced-chunk acquisition paths remain inert.
2. Impact and NBT-restored embedded phases survive a position below the normal vanilla discard threshold.
3. Populated waterlogged chests, hoppers and furnaces lose their block entities without creating inventory item entities.
4. A cold-chunk request enters the persistent vanilla forced set but does not synchronously load the chunk.
5. Two active overlapping impacts transfer a temporary chunk to the active recipient, then release it instead of handing it back to the completed first impact.
6. A real complete radius-four impact removes its support block and reconciles an unsupported torch just outside the sphere.
7. Boundary and completed-purge cursor fields round-trip through entity NBT without resetting progress.
8. Removing existing tripwire beside two unloaded chunks through the production replacement helper does not load either neighbor.
9. A complete radius-four impact removes unsupported surviving-boundary scaffolding, then waits ten more ticks and asserts no item or falling-block entities.
10. Boundary reconciliation suppresses queued block survival work, preserves water ticks, and restores ordinary tick scheduling after leaving the scoped operation.
11. Received entity movement interpolates without snapping or overshoot.
12. Accelerated descent still collides with a single-block floor.
13. A complete radius-four impact removes both supports below adjacent boundary scaffolds and settles both unsupported scaffolds without items or falling blocks. The persistence fixture also saves the changed-pass flag and checks conservative loading of older partial passes.
14. A complete impact removes unsupported natural leaves in the immediate boundary without drops or queued support ticks.
15. Persistent boundary leaves survive while their distance is recalculated to 7 after losing support.
16. Natural leaves retain distance 1 when a surviving external log supports them.
17. Adjacent unsupported boundary leaves settle through repeated bounded passes instead of retaining stale mutual support.
18–19. Complete impacts remove unsupported suspicious sand and gravel, unregister their block entities, and produce neither items nor falling entities.
20–21. Supported suspicious sand and gravel retain their original live block entities.
22–23. Complete impacts convert dry boundary coral and preserve coral with surviving external water, checking beyond the native delayed-death interval.
24. All twenty vanilla coral variants convert when dry, preserve external hydration/waterlogging, and retain wall-fan orientation.

Some fixtures use reflection to enter the entity's private impact/completion stages deterministically; no test-only accessors are added to production code. These exercise actual ServerLevel/entity/block behavior, but they are not command/network end-to-end tests or process-restart tests.

CI runs this suite after the 1.21.1 builds and uploads the runtime logs. The existing `tools/smoke_nail.py` remains a separate manual test with its own disposable-world guard.

## Remaining validation limits

The newly added runtime suite currently covers NeoForge 1.21.1, not all five loaders. In particular, 26.1.2's inclusive top layer and dedicated block-entity side-effect flag have source/compile verification but still need a version-native runtime lane. Whole-process save/restart, client rendering, multi-client networking, and end-to-end performance benchmarking are not covered by this suite.

Boundary reconciliation intentionally covers only the immediate six-neighbor layer and uses suppressed-propagation updates. It can remove unsupported boundary attachments and schedule normal fluid ticks, but does not promise arbitrary recursive redstone or mod-specific network settlement outside the crater. Fully settled worlds should not be inferred from a passed torch regression.


## Review follow-up evidence

The two review regressions were reproduced locally against the pre-fix implementation: tripwire removal loaded a cold neighbor and the complete scaffolding strike produced an item. After scoped callback suppression and bounded survival reconciliation, all ten required NeoForge 1.21.1 tests passed. Logs are generated under `build/review-regressions-before.log` and `build/review-regressions-after.log` (local build artifacts). Runtime coverage remains NeoForge 1.21.1; the other targets have build/refmap verification only.

Older engines suppress nail-scoped `onRemove`/`onPlace` dispatch and explicitly unregister obsolete block entities; the normal level mutation still updates lighting, height maps, POIs, persistence and client notifications. The newer engine uses its native side-effect/placement flags. All three adapters reconcile survival immediately; scaffolding distance/bottom properties are resolved directly, unsupported gravity blocks are removed without falling entities, and deferred block ticks from shape checks are suppressed. Fluid ticks remain permitted. This intentionally does not run recursive redstone or arbitrary mod callbacks outside the bounded pass.

## Animated shader review without Minecraft

Run `tools/render_nail_shader_job.ps1 -Open` after installing Python `numpy Pillow moderngl glcontext`. It exports the current game mesh, compiles both GLSL variants on the GPU, verifies independent animation/view response and pulse visibility, and produces two GIFs plus a self-contained `build/crystal-glint-preview/preview.html` viewer. The viewer offers play/pause, time scrubbing, speed, rotation, pulse toggle and full/crown/tip views. All mesh, atlas and shader data are embedded; opening the HTML does not require network access or a Minecraft process. Browser review confirmed rendering, camera preset changes, time scrubbing and resumed playback. Studio lighting replaces world lighting; portal/world effects and live-client integration remain outside this preview.


## Buried-nail visibility and launch orbit

Debris now advances on the summon clock throughout launch instead of subtracting the launch age. A client mixin skips only the buried origin's compiled/visible terrain-section gate for nail entities; ordinary entities retain vanilla behavior. Nail frustum checks, horizontal fading and depth testing remain active. Camera projection now covers both tip and crown during all phases, including the impact aftermath gap. The shared regression checks body endpoints across deep/raised positions, camera altitudes, viewing distances and nail scales.

All five targets build, the 29 shared tests pass, and the Forge 1.20.1 client boots with the new render mixin. This verifies client startup/injection, not a live visual reproduction of the reported crater-distance issue. Other loaders have build/refmap verification for this change.

Forge 1.20.1 supplies `pack.mcmeta` (resource format 15) in its own resources directory, so both the development directory and packaged mod have valid pack metadata. The package and processed development output were checked; a new startup is needed to clear an already displayed metadata warning.

## Smooth descent and removal fracture

Client position updates interpolate over two ticks instead of snapping. Descent starts at 2.5 blocks/tick, accelerates by 0.65 to a cap of 18, and swept collision still checks the entire traveled segment. Ground penetration uses an 18-tick ease-out, evaluated at partial ticks for rendering. Removal lasts 54 ticks: subtle separation along real fragment boundaries, staggered fragment release, rigid tumbling around local pivots, accelerating downward movement and dust. Removal still cancels damage and terrain work immediately.

All five builds and 32 shared tests pass. The NeoForge runtime suite now passes twenty-four required tests, including interpolation without snapping/overshoot and fast descent onto a single-block floor. These checks do not establish live multiplayer smoothness or visual performance on every GPU. The player's running client was left untouched.

For an offline removal preview, first run `tools/render_nail_shader_job.ps1`, then `python tools/preview_crumble.py` with Java 21, the same Python dependencies and ffmpeg available. It exports the production fracture geometry and rigid transforms to an 82-frame, 30 FPS video at `build/crystal-glint-preview/crumble.mp4`, with a local `crumble.html` player and contact sheet. This studio preview excludes dust, world collision and audio. The contact sheet was inspected and both production shader variants compiled on the GPU.

## Adjacent scaffolding settlement

The complete-impact adjacent-scaffolding regression failed before the correction (`build/adjacent-scaffold-before.log`). Boundary reconciliation now repeats its saved cursor after any successful state replacement and finishes only after an unchanged full pass. Every revisit still consumes the shared scan/change/time allowance, and chunk readiness checks remain in place. The changed-pass flag survives NBT reload; older active saves without it conservatively request another pass. Native deferred block ticks remain suppressed and fluid ticks remain allowed. This covers dependencies within the same immediate boundary layer, not recursive external physics networks. Each pass remains quadratic in radius; total work also depends on the number of passes required to settle.


## Boundary leaf support

Vanilla leaf shape updates schedule distance recalculation instead of changing the state immediately. The bounded adapter performs that six-neighbor calculation directly using the version's `LeavesBlock.getOptionalDistanceAt` (including version-specific support tags). Unsupported natural leaves are replaced through the no-drop path immediately; persistent leaves retain their properties and receive the corrected distance. Waterlogged leaf reconciliation preserves permitted fluid scheduling. The existing readiness guard covers all six reads, and changed states participate in the saved, budgeted revisits.

Before the correction, the natural-leaf, adjacent-leaf and persistent-distance full-impact regressions failed; the supported-leaf control and previous thirteen tests passed (`build/boundary-leaves-before.log`). Post-fix results are recorded in `build/boundary-leaves-after.log`. Runtime coverage is NeoForge 1.21.1; the other adapters are build-verified.


## Brushable gravity and coral hydration

Unsupported brushable blocks use the same bounded no-drop replacement as ordinary falling blocks, including the existing version-specific block-entity cleanup. Supported brushable blocks remain intact. The adapter resolves hydration for all twenty vanilla live coral blocks, plants, floor fans and wall fans; dry coral becomes its corresponding dead state with shared properties preserved. Waterlogged coral and coral with adjacent water stay alive. This explicit vanilla mapping does not claim support for custom modded coral death variants.

The full-impact suspicious-sand, suspicious-gravel and dry-coral regressions failed before the fix, while the prior seventeen tests and three preservation controls passed (`build/brushable-coral-before.log`). The corrected suite and all-target build output are in `build/brushable-coral-after.log`. Runtime evidence remains NeoForge 1.21.1; 1.20.1 and 26.1.2 have source/build verification. No unrestricted native gravity or coral death callbacks are enabled.
