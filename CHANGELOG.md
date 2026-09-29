# Changelog

## Unreleased

- Adopt MIT licensing across source, loader metadata and packaged JARs.
- Add validated five-target CurseForge/GitHub release automation with required project IDs and a pinned Boom dependency.

- Require matching Perfomant Boom 1.1.0 on all five targets.
- Delegate sphere clearing, fluid purge, boundary repair, budgets and world mutation to Boom.
- Preserve Nail entity/NBT progress, visual stages, special damage and chunk ownership.
- Remove duplicate server mutation mixins and transfer reusable cursor/budget tests to Boom.
- Add CurseForge page copy, compatibility, contributor, architecture and release documentation.

- Kept deep impact/embedded nails alive below vanilla's entity-removal threshold, and made removal terminal for ticking and chunk acquisition.
- Suppressed vanilla container inventory drops during staged terrain removal, with version-specific block-entity cleanup.
- Requested persistent forced chunks without the synchronous `ServerLevel.getChunk` path; shared mutation, scan, chunk-request and cooperative time budgets across impacts in each dimension.
- Disabled full-model fluid interaction scans without changing the nail's rendering or scale.
- Prevented completed overlapping impacts from accepting temporary forced-chunk ownership back after cleanup.
- Corrected 26.1.2's inclusive maximum build-height handling for terrain and fluid passes.
- Added saved, bounded, duplicate-free boundary shape/survival reconciliation without recursive physics cascades.
- Added eight shared budget/boundary tests and seven isolated NeoForge 1.21.1 server regressions, with runtime execution and log artifacts in CI.

- Replaced the block placeholder with a shared detailed stone, gold and crystal entity mesh.
- Added a default 72-block height and a proportional summon scale argument from 0.1 to 4.
- Added a synchronized animated rift, clipped nail emergence, hovering crystals and launch closure.
- Raised the floating portal half a nail-height above the crown while preserving the command tip coordinates, including migration of saved floating nails.
- Added the supplied opening audio as a ten-second streaming mono Ogg with a smooth fade, preserving its source and conversion tool.
- Fixed fluid cleanup with bounded top-down water/lava sweeps and safe impact chunk loading.
- Added geometry, clipping, timing, scale and purge traversal checks, a live fluid smoke test, and reproducible model previews.

## 0.1.0

- Added the persistent `celestial_nail:celestial_nail` networked entity on every supported loader/version.
- Added administrator commands to summon, launch, list, and remove Nails by unique ID.
- Added configurable strike power/radius from 4 to 128 blocks, defaulting to 32.
- Added straight-down accelerated launch, terrain collision, impact particles/sound, and radial entity damage.
- Added staged inside-out terrain destruction with per-tick block-change and scan budgets to avoid a single giant explosion tick.
- Added intentional direct-world-mutation behavior that bypasses `mobGriefing` and normal FTB Chunks claim explosion/break filtering.
- Added persistent impact-wave progress and safe force-loaded-chunk ownership across saves/restarts.
- Added Genshin-inspired floating Nail rendering for 1.20.1, 1.21.1, and the 26.1.2 render-state pipeline.
- Added shared impact-shell math tests.
