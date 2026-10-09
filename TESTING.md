# Verification

## Normal development clients with rendering mods

Run `python tools/install_dev_renderers.py` once from the repository root. It verifies
SHA-512 pinned downloads and installs separate local profiles, excluded from release
JARs. The ordinary client launch tasks then load these combinations:

| Client | Installed combination |
| --- | --- |
| Forge 1.20.1 | Embeddium 0.3.31 + Sodium Extras 1.0.7 + Options API 1.0.10; existing Oculus remains available |
| Fabric 1.20.1 | Sodium 0.5.13 + Sodium Extras 1.0.7 + Options API 1.0.10 + Reese's Options 1.7.2 |
| Fabric 1.21.1 | Sodium 0.6.13 + Sodium Extras 1.0.8 + Options API 1.0.10 + Reese's Options 1.8.3 |
| NeoForge 1.21.1 | Sodium 0.6.13 + Sodium Extras 1.0.8 + Options API 1.0.10 + Reese's Options 1.8.3 |

Use the usual `runForge1201Client`, `runFabric1201Client`, `runFabric1211Client`,
and `runNeoForge1211Client` root tasks. NeoForge additionally has an Embeddium
1.0.15 profile: `./gradlew runNeoForge1211Client -PdevRenderer=embeddium`.
That profile excludes Sodium Extras because its 1.21.1 builds require Sodium's API.
Neither requested mod currently publishes a 26.1.2 build, so that dev target keeps
its existing renderer. Existing worlds and mod preferences are preserved.

The Fabric and Forge local dependencies are remapped by Loom. NeoForge's local
profiles use separate `neoforge-1.21.1/run/client` and `run/client-embeddium`
directories containing loose mod JARs, not server/test runs. Older saves under
`neoforge-1.21.1/run/saves` remain intact in their original location.
The installer also exposes embedded config/runtime libraries that Loom omits from
its loose dev classpath. Fabric 1.20.1 pins LWJGL Java libraries and natives to
3.3.1, as required by Sodium 0.5.13; its normal dev upgrade to 3.3.2 is rejected
by Sodium's startup check.

On 2026-10-03, all five profiles above completed loader initialization, sound
startup, and texture/shader resource loading on the local NVIDIA GPU. Logs are
saved under `build/reports/dev-renderers`. These are client startup checks;
in-world cinematic behavior with each combination still needs manual testing.

## Automated final-image Oculus regression

Run `python tools/run_render_regression.py` with Java 21 selected. This downloads
and SHA-512 verifies pinned Oculus 1.8.0, Embeddium 0.3.31, Complementary Reimagined
r5.9.3 and Oculus's jcpp library before launching the actual Forge 1.20.1 client.
It requires a functioning OpenGL display; it fails rather than skips if the client,
shader pack, world setup, entity tracking, or image capture cannot run.

The `:forge-1.20.1:runRenderRegression` task runs the isolated `renderTest` source set
in `forge-1.20.1/run/render-regression`, creates a uniquely named disposable flat
world, sets up a spectator camera and spawns server-side Nails using real tracking
packets. It never opens the ordinary development world's saves. The test helper mod
and its resources are packaged only into a separate render-test JAR, not release JARs.
Each run keeps its world and evidence; no existing worlds are deleted.

Seventy-six live cases cover both shader-disabled and confirmed Complementary-enabled
pipelines: near/far idle, close/inside/underside hover views, dark pinned state, cloud overlap, minimum/maximum scale, portal opening, emergence, descent,
impact, a large embedded body with a buried anchor, and crumble. Six controls in
each mode cover offscreen/out-of-range geometry, deliberately suppressed draws,
lost client tracking, a real opaque terrain wall, and a deliberate final-composite loss after successful real
draw calls. The latter control overwrites the captured final color texture with
the hidden-Nail reference; its assertion must reject visible pixels despite real
renderer/vertex/shader activity. Draw counters alone cannot pass this test.

The driver samples the **final main render target after GameRenderer and shader-pack
composition**, not an intermediate draw framebuffer. It compares a body-centered RGB
region against a hidden-Nail image and measures temporal noise with a second hidden
image. Fixed animation time, camera, partial tick and cleared random particles keep
particles/name labels from standing in for Nail geometry. Server-side visual fixture
states are frozen to prevent terrain work while the production renderer and shader
code run normally. These are rendering lifecycle-state tests; normal ticking,
damage/terrain lifecycle and multiplayer timing remain covered by their separate
server suites, not by this frozen visual fixture.

The nighttime buried-anchor case also requires mean brightness of changed Nail pixels
at least 100/255; a merely visible dark silhouette cannot pass. The portal-opening
fixture sits below the cloud layer to avoid testing legitimate cloud occlusion.

Visible cases require tracking, submitted vertices, Nail shader execution, the
expected GL program, enabled RGB writes, enabled translucent blending and a final-image RGB change
above both an absolute threshold and measured background noise. Shader mode and
synchronized scale/phase/animation state are checked. Controls require absence of
visible pixels; the terrain-occlusion and composite-loss controls must still have real draw activity.
Missing cases, an incomplete run, or any failed assertion produce a non-zero Gradle
and wrapper exit. Reports distinguish `FINAL_IMAGE_MISSING` from `BLENDING_DISABLED`
and other upstream failures.

Thirty-four additional cases isolate the procedural compositor: for each shader mode the same world and Nail are rendered with the pass disabled for both reference images, then enabled for the visible image. Cases cover impact contact, a weaker looking-away pulse, dust, persistent presence, a real wall hiding the entire dust volume, an expired impact clock, and disabled preferences. Active effects must change the final RGB image above temporal noise and perform real compositor draws; the wall control must draw the pass without producing visible dust. Expired and disabled controls must perform no compositor draws. The subtle lingering-grade case uses a 2/255 changed-pixel threshold; stronger effects use 8/255. Both retain the absolute and noise-relative signal requirements. Body-only cases keep cinematics disabled so a screen effect cannot substitute for Nail geometry.

Results are in `forge-1.20.1/run/render-regression/render-test-results.json` and paired
`render-test-evidence/*-hidden.png` / `*-visible.png` images. `tools/test_live_render_report.py`
checks that the result validator cannot accept incomplete matrices, invisible output,
wrong shader mode, or controls that never exercised their failure. The proposed CI change in `tools/ci/live-render-workflow.patch` adds a
`live-render-1201` job with Xvfb/Mesa and uploads the report,
images and logs even on failure. Mesa CI is a separate renderer from a local NVIDIA
run; this matrix is not a claim of coverage of every GPU, shader pack or MC version.

Do not weaken the final-image, color-mask, blend or terrain-occlusion assertions to
make a draw-counter-only result pass.

The original custom-shader baseline failed all ten Complementary final-image and
color-write checks. The current matrix verifies the normal body pipeline and final
images; a minimum-brightness or draw-counter-only result is not sufficient.
The Mesa CI job is supplied as a patch because the current GitHub token cannot
update workflow files; it has not run. Apply it with `git apply tools/ci/live-render-workflow.patch`
using credentials with workflow permission. Local NVIDIA validation is independent.

Fixture CLOCK is 100000, greater than every age. Idle states use launch/impact/
crumble sentinels only when those events have not happened. All post-launch states
have valid LaunchTime values; impact and embedded states have valid ImpactTime.
Every tracked case records and checks actual summon, launch, impact and crumble
ages against the scene before accepting an image. Older reports lacking these
fields cannot pass the report validator.

The idle-near, dark pinned and cloud-overlap cases reject more than 15% clipped-white
changed pixels. The previously all-emissive Complementary idle image clipped about
40%, which this check rejects. Main body draws must write depth. The cloud-overlap
fixture enables vanilla clouds, keeps Complementary clouds active, and requires
at least 500 changed Nail pixels with newly nearer depth where the hidden reference
contains white or blue-tinted clouds. World depth is sampled after world composition before
the hand/HUD depth clear; color is still sampled from the final GameRenderer image.
Depth readback checks GL errors and restores read-framebuffer and pixel-pack state.

The corrected matrix passed all 42 cases on the local NVIDIA GTX 1050 Ti with
Oculus 1.8.0 and Complementary r5.9.3. Cloud-backed nearer-depth pixels numbered
1570 in vanilla and 927 in Complementary. Embedded fixtures confirmed summon age
2300, launch age 2045, impact age 2000 and no crumble. Ten Python tests passed;
all five production targets built and the 30 shared tests remained passing.
The 26 server runtime tests passed in the preceding validation; server behavior
was unchanged by this rendering and fixture correction. Mesa CI remains unrun.

### Upstream source reference

Local reference clones live under ignored `.dependencies/shader-reference`:
Oculus uses branch `1.20.1-new`, commit
`b3b278134f719afe32ba8b6b5d3a93f052175afc` (declares MC 1.20.1 / Oculus 1.8.0).
Complementary's repository main is commit
`c09950df0650b27dac260c1b0f69afc11387aa6f` (r5.9.2); the installed r5.9.3 ZIP is
separately extracted as `ComplementaryReimagined-r5.9.3`. Do not treat that older
repository head as the deployed pack. The runner pins the deployed ZIP's SHA-512.

Oculus `MixinShaderInstance.iris$lockDepthColorState` calls
`DepthColorStorage.disableDepthColor()` for ordinary custom ShaderInstances while
the world shader pipeline is active. That sets depth writes false and all four
color-write channels false. The installed Oculus JAR bytecode confirms this hook;
the former Nail custom ShaderInstance was neither ExtendedShader nor FallbackShader.
Oculus's ExtendedShader instead binds the pipeline's before/after-translucent
framebuffers, and FinalPassRenderer replaces the main color target with its final
composition. Complementary r5.9.3 `shaders/program/final.glsl` reads `colortex3`.
Simply drawing to the main target or restoring its color mask is therefore not
established shader compatibility.

Production retains the 0.1.2 vanilla entity/emissive pipeline integration and complete entity
vertex format, allowing Oculus to replace its shaders and bind its world pipeline
framebuffers. The body uses the normal translucent entity pass with depth writes and full-bright
light coordinates. Animated effects use the emissive pass; the whole body must not
be classified as glowing eyes. This retains stable anchor-independent lighting
without flooding Complementary bloom or losing cloud depth. The custom-shader post-composition workaround is retired. Diagnostics
identify Nail draws by its unique texture, rather than a custom shader name, so
Oculus replacement shaders are included. Shadow draws are excluded from the main
color-write assertions because their depth-only output is intentional.

Diagnostics now run immediately before VertexBuffer.draw, after all shader-apply
mixins finish. This avoids reporting state before Oculus's apply-tail hook has
locked the masks. The live report records `colorWritesDisabled` and requires RGB
writes enabled in visible cases (`COLOR_WRITES_DISABLED` on failure). Final-image
checks remain independently required.

## Sodium Extras compatibility regression (four applicable targets)

First publish the Perfomant Boom revision matching Nail's `boom_version` in
`gradle.properties` to Maven local for the targets you will test. In the Boom
checkout, run `./gradlew buildAll publishToMavenLocal` (`gradlew.bat` on Windows).
See [the development setup](CONTRIBUTING.md#local-development). The runner selects
the configured Boom version and the current Nail version's exact fixture filename;
older artifacts in `build/libs` do not affect selection.

NeoForge 1.21.1 uses the official 21.1.228 installer pinned by SHA-512 in
`tools/compat-artifacts.json`. The runner checks its embedded Minecraft and loader
versions before installation, avoiding the mutable NeoForge version catalogue
that intermittently rejected 1.21.1 in CI. Update the installer pin together with
the runner's loader version when changing the tested runtime.

Install `python -m pip install -r tools/compat-requirements.txt`, set `JAVA_HOME`
to Java 21, and run `python tools/run_sodium_extras_compat.py`. Use
`--target fabric-1.21.1` (or any target in the matrix) for one loader. On a headless
Linux machine run under `xvfb-run -a -s '-screen 0 1280x720x24'` with
`LIBGL_ALWAYS_SOFTWARE=1` and `ALSOFT_DRIVERS=null`.

The runner builds and launches the production Nail jar with a separate fixture jar,
matching Boom, pinned renderer/dependency artifacts, and Extras absent/present.
Each launch has a unique disposable game directory under `build/compat-runtime`.
It never opens a user's world. Optional dependency versions and SHA-512 hashes are
named in `tools/compat-artifacts.json`; download verification is shared with the
shader runner through `tools/runtime_artifacts.py`.

Shared assertions exercise the transformed EntityType exemption and dispatcher:
first/cached Nail lookup, both anchor cutoffs, ordinary entity rejection, configured
whitelist retention, and Nail's own distance/frustum rejection. Loader registration
and version-specific world startup live separately from those reusable assertions.
The image fixture then creates a real integrated-server Nail and requires client
tracking, the correct summon clock, actual camera visibility, and final RGB pixels
beyond both cutoffs and at two angles. Two hidden frames measure background noise.
Offscreen and suppressed-draw controls must produce no mesh pixels or draws.

Reports, production jar hashes, startup logs and hidden/visible PNGs are saved under
`build/reports/sodium-extras-compat`. The active Validate workflow runs all four
targets with both optional-mod modes. Fixture classes and optional mods must never
be embedded in production artifacts. The existing tooling unit tests validate the
runner/report contracts; their count is not a count of live compatibility cases.

This suite does not certify arbitrary Extras versions, shaders, modpacks,
multiplayer servers, or NeoForge 26.1.2 (no matching Extras release was listed when
checked). The separate Forge/Oculus renderer suite covers shader compositing.

## Oculus render diagnostics and live regression (1.20.1)

The client creates `config/celestial_nail-render-debug.properties` in its game directory.
Settings reload every two seconds. Defaults are disabled; this development instance
has logging enabled. A code change requires a client restart.

```properties
enabled=true
logIntervalSeconds=5
forceVisible=false
disableDistanceFade=false
pixelProbe=false
requireVisibleDraw=false
```

`[NailRenderDebug]` summaries are global, at most one per interval (clamped to 1–300
seconds), with bounded samples of four tracked Nails. They include camera and render
distance, entity coordinates and visual bounds, distance/frustum rejection counts,
render calls, submitted vertices and alpha range, configured render passes, actual GL
program and framebuffer, depth/blend/cull state, shader reloads, and Oculus pack state.
Config-read errors retain the previous settings and warn at most once per 30 seconds.
Only the first main Nail draw in each interval queries GL state. The snapshot
is immediately before the actual vertex draw, after shader-apply hooks finish;
`colorWriteMask` records the four channel masks (1 means enabled).

Use `forceVisible=true` to bypass the renderer's distance/frustum rejection, and
`disableDistanceFade=true` to bypass its horizontal fade. These controls require
`enabled=true`; they do not bypass entity tracking, depth testing or shader-pack
composition. Keep both false for regression validation.

For a live regression, enable Complementary in the Forge 1.20.1 client. Use an empty
test world, summon a single idle Nail, wait at least 170 game ticks for emergence,
and keep the stationary camera aimed at its unobstructed body, well inside the chunk
render distance. Do not launch or remove it during sampling. Set `pixelProbe=true`
and `requireVisibleDraw=true`, then record at least two summaries. Disable the latter
before moving the camera or changing scenes.

The probe reads RGB from the first Nail draw's active color target before/after the
draw. It samples at most once per log interval, in a centered region capped at
2048×2048 pixels, and restores framebuffer/read-buffer/pixel-pack state. It is opt-in
because synchronous GPU readback may stall a frame. Alpha-only changes and one-byte
rounding noise do not pass. Menu frames and pre-emergence scenes remain unarmed.

```powershell
python tools/check_render_debug.py forge-1.20.1/run/client/logs/latest.log
```

The checker fails for missing render calls, zero emitted vertices, transparent
geometry, absent Nail shader applications, a mismatched GL program, or unchanged
draw-target pixels. It also rejects shader-disabled sessions, visibility overrides,
missing evidence, and single-interval successes. `DRAW_OBSERVED` remains unverified.
Use a fresh log dedicated to the fixture; any armed failure fails the run.

`common:test` covers the diagnostic failure classifier, RGB pixel comparison, and
monotonic rate limiting (including timer wrap and no burst after a stall). CI also
runs `tools/test_render_debug.py` to prevent invisible-draw logs from passing.
These deterministic tests do not run Oculus. A successful pixel probe establishes
the Nail draw target changed; it does **not** establish that Complementary's final
composite displays those pixels. Compare the final in-game view as well, particularly
when the Nail draw framebuffer differs from the main framebuffer.

Run Gradle with Java 21. The 26.1.2 module resolves Java 25 for compilation.

## Shared deterministic tests

```sh
./gradlew :common:test
```

The current Nail suite contains 20 tests for Nail-owned animation, geometry and math. Reusable sphere/fluid/boundary cursor and work-budget tests moved to Perfomant Boom's `core` suite with the API extraction. Run Boom's checks as well when changing that dependency; the historical regression notes below describe the behavior introduced before extraction, not terrain helpers still owned by Nail.

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

This launches a disposable GameTest server in `neoforge-1.21.1/run/runtime-tests`. It never connects to the player's server or existing smoke world. Tests live in an isolated `gameTest` source set and are not shipped in the production jar. The dedicated runtime task exits nonzero when required tests fail. Successful evidence must include **all twenty-six required tests**, not merely a server startup message.

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
18-19. Complete impacts remove unsupported suspicious sand and gravel, unregister their block entities, and produce neither items nor falling entities.
20-21. Supported suspicious sand and gravel retain their original live block entities.
22-23. Complete impacts convert dry boundary coral and preserve coral with surviving external water, checking beyond the native delayed-death interval.
24. All twenty vanilla coral variants convert when dry, preserve external hydration/waterlogging, and retain wall-fan orientation.

Some fixtures use reflection to enter the entity's private impact/completion stages deterministically; no test-only accessors are added to production code. These exercise actual ServerLevel/entity/block behavior, but they are not command/network end-to-end tests or process-restart tests.

CI runs this suite after the 1.21.1 builds and uploads the runtime logs. The existing `tools/smoke_nail.py` remains a separate manual test with its own disposable-world guard.

## Remaining validation limits

The newly added runtime suite currently covers NeoForge 1.21.1, not all five loaders. In particular, 26.1.2's inclusive top layer and dedicated block-entity side-effect flag have source/compile verification but still need a version-native runtime lane. Whole-process save/restart, client rendering, multi-client networking, and end-to-end performance benchmarking are not covered by this suite.

Boundary reconciliation intentionally covers only the immediate six-neighbor layer and uses suppressed-propagation updates. It can remove unsupported boundary attachments and schedule normal fluid ticks, but does not promise arbitrary recursive redstone or mod-specific network settlement outside the crater. Fully settled worlds should not be inferred from a passed torch regression.

## Review follow-up evidence

The two review regressions were reproduced locally against the pre-fix implementation: tripwire removal loaded a cold neighbor and the complete scaffolding strike produced an item. After scoped callback suppression and bounded survival reconciliation, all ten required NeoForge 1.21.1 tests passed. Logs are generated under `build/review-regressions-before.log` and `build/review-regressions-after.log` (local build artifacts). Runtime coverage remains NeoForge 1.21.1; the other targets have build/refmap verification only.

Older engines suppress nail-scoped `onRemove`/`onPlace` dispatch and explicitly unregister obsolete block entities; the normal level mutation still updates lighting, height maps, POIs, persistence and client notifications. The newer engine uses its native side-effect/placement flags. All three adapters reconcile survival immediately; scaffolding distance/bottom properties are resolved directly, unsupported gravity blocks are removed without falling entities, and deferred block ticks from shape checks are suppressed. Fluid ticks remain permitted. This intentionally does not run recursive redstone or arbitrary mod callbacks outside the bounded pass.

## Historical custom-shader review without Minecraft

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

## Shared terrain dependency

Publish the Perfomant Boom version configured by `boom_version` to Maven local before these checks.
All terrain mutation tests now exercise Boom's external API and mixins. The three passes
and cursors no longer live in Nail; the entity keeps its existing persisted cursor fields.
When iterating on Boom at the same version, refresh Nail's dependency cache with
`--refresh-dependencies` before validating it again.

## Vanilla entity pipeline and shader-pack checks

The production renderer now uses vanilla `entityTranslucent` and `entityTranslucentEmissive` with complete entity vertices (UV, overlay, lightmap, normal). `NailSurfaceTest` checks real atlas coordinates through the lifecycle, pulse clipping inside long facets, rigid fragment normal rotation and the shockwave annulus. The old offline custom-shader preview remains historical evidence only.

Use a disposable client world. Compare the ivory/metal shell at noon, midnight and beside a torch; rotate the view and observe facet shading. Check emergence, portal, traveling pulse, impact ring and removal with shaders disabled and enabled. Reload resources and toggle the pack repeatedly. Vanilla and pack fog now apply to the nail, so repeat at a height inside the configured fog range before diagnosing visibility. Shader-pack bloom or illumination of nearby blocks is not guaranteed by full-bright entity materials.

## Void removal regression

The server runtime suite includes descending through empty space below minimum
build height and idle below-world cleanup. Both enter the normal timed crumble
phase, stop motion, avoid an impact, retain the saved crumble clock, and discard
only after CRUMBLE_TICKS. The descent test checks forced-chunk cleanup. Existing
deep-impact/embedded tests continue to protect deliberate below-world embedding.
The production change is applied to all three Minecraft version adapters.

### Cinematic implementation verification

The new compositor has adapters for 1.20.1, 1.21.1 and 26.1.2. Shared tests cover its 750 ms authored impact stages, Reduced/Off modes, first-render timing after delayed updates, old clocks, dust fade, lingering radius and bounded preference values. Building all five targets checks the adapters; live final-image tests specifically exercise Forge 1.20.1 with vanilla and Oculus/Complementary. Compilation alone does not certify visuals on 1.21.1 or 26.1.2, other shader packs, alternate GPU backends, multiplayer packet latency, resizing or resource reload. The implementation reads shared synchronized entity state rather than broadcasting a new effect packet; every client that tracks the Nail independently samples that clock.

The 1.20.1 adapter takes the real world view matrix from the `renderLevel` pose stack, including Forge camera roll. Its Camera quaternion uses a positive-Z forward basis, while 1.21.1 and 26.1.2 use negative-Z; treating them identically reverses the dust projection. This was checked against decompiled 1.20.1 Forge 47.4.0 `GameRenderer`/`Camera` and the cached 1.21.1 and 26.1.2 sources. Dust fixtures use the normal 32-block crater radius. A delayed-impact fixture delivers real server metadata after both reference images, observes the update, then advances the client game clock by eight ticks before capturing the first visible frame. Dust and body draws are disabled in that fixture, so only the impact pulse can pass its final-image assertion. Facing and looking-away impact fixtures share the contact timestamp so their relative strength is observable.

In the initial cinematic verification on 2026-10-03, all five production targets built, 34 shared tests and 11 Python render-validator tests passed, and all 56 Forge 1.20.1 final-image cases passed on the local GTX 1050 Ti in vanilla and Oculus 1.8.0/Complementary r5.9.3 modes. Packaged NeoForge 1.21.1 checks passed all six scenes with Sodium Extras present and all six absent. The isolated NeoForge 26.1.2 client reached its loaded title screen with the new mixins; this validates startup, not its world visuals. A final metadata guard excludes the default idle state of an unidentified freshly constructed client entity from arming receipt grace; it does not change the synchronized clocks or compositor exercised by the image matrix.

The Forge dev menu has a separate isolated probe: `./gradlew :forge-1.20.1:runClient -PdevMenuSmoke`. It presses the real Video Settings button, verifies Embeddium and the Extras option pages, and captures both menus under `forge-1.20.1/run/menu-smoke/menu-evidence`. The installer builds a checksum-derived local Embeddium copy with only its synthetic menu hook adapted from `lambda$init$2` to the current Loom `method_19828`; the original download and release JAR remain untouched. The Fabric event API used by Options API must load in the game classloader because it references Minecraft types, while the MixinExtras bootstrap stays on Forge's library runtime.

The revised presentation and audio passed all 58 Forge 1.20.1 final-image cases on 2026-10-03, including real delayed impact metadata followed by eight client catch-up ticks, in vanilla and Complementary modes with Embeddium and Sodium Extras installed. The menu probe confirmed the actual Embeddium screen and Extras pages. All five targets built with the supplied mono Ogg explosion and portal-closing recordings; 34 shared tests and 15 Python image/compatibility report tests passed. This checks integrated-server metadata delivery, not a multi-client latency simulation. Original MP3 inputs are preserved under `.dependencies/audio-sources`; the ambience generator does not overwrite the replacement recordings.

The ledge-dust fixture hides the Nail body and views from 80 blocks below the impact origin, beneath an opaque high ledge. Its final image must contain the falling dust volume independently of the impact pulse. The camera-shake fixture measures actual rendered camera yaw and pitch changes with fixed player inputs; each must exceed 0.1 degrees. All other fixtures suppress shake to keep their paired image comparisons deterministic.

On 2026-10-03, all 62 final-image cases passed on the local GTX 1050 Ti with Embeddium and Sodium Extras, with shaders disabled and with Oculus/Complementary enabled. Both new cases passed in each mode: lower-level dust with the body hidden, and measured camera rotation with fixed player inputs. All five targets built, 36 shared tests and 15 Python report-validator tests passed. GPU frame-time improvement is not measured; the optimization reduces the bounded shader workload (two noise octaves instead of three, arithmetic hashing, empty-segment rejection, and per-frame shape calculation). Runtime visual coverage remains Forge 1.20.1.

Anime impact presentation lasts 750 ms on the monotonic first-render clock. Independent phase images isolate white, inverted, gold-fracture, ink-fracture and recovery treatments, plus Reduced and Off modes, with body and dust hidden. The local `impactMode` property accepts `full`, `reduced` or `off`; Full contains strong flashing, Reduced is a smooth cold grade, and Off suppresses the impact screen sequence and its camera shake. Existing `impactIntensity`, `shakeIntensity` and Minecraft Screen Effects remain respected.

On 2026-10-03, the authored impact sequence passed 76 final-image cases across vanilla and Oculus/Complementary modes with Embeddium and Sodium Extras. The full run passed 74 cases; two Off controls correctly showed no effect but had an erroneous visible-image expectation. After fixing only that fixture expectation, `python tools/run_render_regression.py --case cinematic-sequence-off-control` passed both controls. `build/anime-impact-verification.json` validates the combined 76 cases and records both source reports. All five production targets built, 36 shared tests and 16 Python validator tests passed. Runtime shader coverage remains Forge 1.20.1.

The compositor now checks OpenGL 3.3 or ARB sampler-object support before sampler queries, overrides and restoration. OpenGL 3.2-only contexts skip those operations and use the scratch textures' filtering parameters. All five targets rebuilt, 36 Java and 56 Python tests passed for this capability-guard follow-up. No fresh visual clients or 3.2-only hardware tests were run; the saved 76-case visual evidence predates this guard.

## Grounded Nail culling regressions

The active packaged Sodium Extras matrix requires twelve final-image cases per
launch, with Extras absent and present on each of the four applicable targets.
Six cases retain floating coverage; six use a saved embedded Nail with a buried
anchor and a stone ground plane. Reports must confirm the actual embedded phase,
expected lifecycle clock, real tracking, renderer admission and final framebuffer
pixels. The same transformed dispatcher checks enforce both cutoff exemptions,
frustum rejection and Nail's own distance limit on the grounded state.

`buriedAnchorBoundsIncludeImpactOriginAcrossScales` covers small/default/large
scales and low/default/high powers in impact, embedded and crumbling phases on
NeoForge 1.21.1. It checks surface-origin containment and the native culling bounds
contract. The other game adapters receive the same correction and build coverage.

Embedded impact bounds conservatively include the wide effect region, so an
offscreen grounded body may still reach the renderer. That control requires zero
visible final pixels; it permits renderer admission. Missing-draw controls require
zero renderer calls, and every visible case requires both draws and visible pixels.
