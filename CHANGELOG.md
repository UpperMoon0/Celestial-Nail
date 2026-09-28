# Changelog

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