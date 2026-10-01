# Compatibility and installation

This matrix describes the current source targets. It does not imply that every target has already been published.

| Minecraft | Loader | Runtime Java | Required dependencies |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | 17 | Boom 1.1.3+ (1.x), Fabric API, Architectury API 9.2.14+ |
| 1.20.1 | Forge | 17 | Boom 1.1.3+ (1.x), Architectury API 9.2.14+ |
| 1.21.1 | Fabric | 21 | Boom 1.1.3+ (1.x), Fabric API, Architectury API 13.0.8+ |
| 1.21.1 | NeoForge | 21 | Boom 1.1.3+ (1.x) |
| 26.1.2 | NeoForge | 25 | Boom 1.1.3+ (1.x) |

Fabric Loader must be at least 0.18.4. The NeoForge 1.21.1 artifact requires NeoForge `[21.1.228,21.2)` and Minecraft `[1.21.1,1.21.2)`. Build pins are in [gradle.properties](../gradle.properties); loader metadata is the install-time authority. Nail declares Boom `>=1.1.3 <2.0.0` and currently develops against 1.1.3. That declared range is not evidence that an arbitrary future Boom release has been tested.

Install matching game/loader JARs on the server and clients. Use the runnable artifacts, excluding sources, dev, and dev-shadow JARs. Remove obsolete copies from `mods` when updating. Nail does not embed Boom. Fabric 1.21.1 still needs Architectury for Nail even though Boom's Fabric 1.21.1 implementation does not.

## World behavior and other mods

Nail's direct terrain mutations intentionally bypass `mobGriefing` and normal explosion/player-break filters, including those FTB Chunks claim protections. Restrict the administrator commands; do not treat a claim as protection against a Nail strike.

The no-drop path handles vanilla containers, fluid purge, and the immediate six-neighbor boundary. It does not guarantee arbitrary modded callback, redstone, storage-network, or recursive physics compatibility. External fluids can flow back after cleanup. `remove` cancels future work without undoing committed block changes.

Nails save their stage and terrain cursors. Existing entity NBT field names are retained by the Boom migration. Preserve a world backup before upgrading or removing mods that own persistent entities. Chunk ownership remains Nail's responsibility, including preserving pre-existing forced chunks.

## Troubleshooting

- Missing dependency: check the loader/game combination and the table above, including Architectury on Fabric 1.21.1.
- `NoClassDefFoundError` for a Boom terrain class in development: rebuild and publish Boom, then refresh Nail's dependencies. A cached remapped JAR may still advertise 1.1.3 while containing older classes. See [CONTRIBUTING.md](../CONTRIBUTING.md).
- Command refused: check permission, unique ID, and that emergence has completed before launch.
- Water returns: sources outside the cleared sphere are intentionally preserved.

Report the exact command, game/loader/mod versions, `latest.log`, and any crash report through [GitHub Issues](https://github.com/UpperMoon0/Celestial-Nail/issues). Report mod conflicts with a minimal reproduction and the additional mod versions.

## Verification scope

All five targets have build coverage. The isolated complete-impact regressions run on NeoForge 1.21.1. That suite is not a five-target visual, multiplayer, or performance certification. See [TESTING.md](../TESTING.md) and [the runtime audit](runtime-audit.md).

For full command operation see [USAGE.md](../USAGE.md); for automation and Java integration boundaries see [INTEGRATION.md](../INTEGRATION.md).
