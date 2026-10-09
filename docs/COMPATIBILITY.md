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

## Sodium Extras distance culling

Sodium Extras rejects entities using the position of their anchor before Nail's
renderer can test its full visual bounds. The default horizontal limit is 64
blocks; the vertical test is `abs(anchorY - cameraY - 4) < 32`. This can hide an
in-view Nail when walking away, beneath it, or looking at an angle. The optional
client hook exempts only `celestial_nail:celestial_nail`; ordinary entities and user
whitelists retain Extras' behavior, and Nail's own distance/frustum limits still apply.
No pack configuration is rewritten.

| Nail target | Extras artifact checked | Hook included | Source checked | Runtime tested |
| --- | --- | --- | --- | --- |
| Forge 1.20.1 | Forge 1.0.7 | Yes | Yes | Yes: packaged absent/present startup and final RGB |
| Fabric 1.20.1 | Fabric 1.0.7 | Yes | Yes | Yes: packaged absent/present startup and final RGB |
| Fabric 1.21.1 | Fabric 1.0.8 | Yes | Yes | Yes: packaged absent/present startup and final RGB |
| NeoForge 1.21.1 | NeoForge 1.0.8 | Yes | Yes | Yes: packaged absent/present startup and final RGB |
| NeoForge 26.1.2 | None listed on 2026-10-02 | No applicable hook | Not applicable | Not applicable |

All four applicable targets passed fresh packaged absent/present runs locally on
2026-10-02: six real-camera image cases per launch (48 total), with tracked entities
and invisible controls. The active [Validate workflow](../.github/workflows/validate.yml)
runs the same matrix and uploads startup logs, reports and reference/visible PNGs.
CI status is recorded on the PR; a local pass is separate from a CI pass.

“Included” means present in the built mod. “Source checked” means the published
Extras source retains the targeted exemption method and dispatcher rejection.
“Runtime tested” requires successful startup of that packaged jar and a passing
report, not compilation or source inspection alone. Exact filenames and hashes are
pinned in [the artifact manifest](../tools/compat-artifacts.json); some Extras jars'
internal version fields lag their published release labels. Check the current
[Extras release list](https://modrinth.com/mod/sodium-extras/versions) before trying
a different version. The hook uses an optional injection and may not cover a future
renamed method or a different mod's culling path.

For an older Nail build or an unsupported Extras combination, add
`"celestial_nail:celestial_nail"` to the existing `whitelist` in
`config/sodiumextras-client.toml`, under
`[embeddiumextras.performance.distanceCulling.entities]`. Preserve the other entries.
Restart the client: Extras caches exemption results per entity type. Turning off
entity distance culling is a broader fallback. If the Nail still vanishes, enable
Nail render diagnostics and capture the exact loader, Extras/renderer versions,
camera position and `latest.log`; the exemption cannot restore an untracked entity
or override Nail's own fade/frustum tests.

## Troubleshooting

- Missing dependency: check the loader/game combination and the table above, including Architectury on Fabric 1.21.1.
- `NoClassDefFoundError` for a Boom terrain class in development: rebuild and publish Boom, then refresh Nail's dependencies. A cached remapped JAR may still advertise 1.1.3 while containing older classes. See [CONTRIBUTING.md](../CONTRIBUTING.md).
- Command refused: check permission, unique ID, and that emergence has completed before launch.
- Water returns: sources outside the cleared sphere are intentionally preserved.

Report the exact command, game/loader/mod versions, `latest.log`, and any crash report through [GitHub Issues](https://github.com/UpperMoon0/Celestial-Nail/issues). Report mod conflicts with a minimal reproduction and the additional mod versions.

## Verification scope

All five targets have build coverage. The isolated complete-impact regressions run on NeoForge 1.21.1. That suite is not a five-target visual, multiplayer, or performance certification. See [TESTING.md](../TESTING.md) and [the runtime audit](runtime-audit.md).

For full command operation see [USAGE.md](../USAGE.md); for automation and Java integration boundaries see [INTEGRATION.md](../INTEGRATION.md).

## Grounded Nails and culling bounds (0.1.6)

The floating and grounded Nail are lifecycle phases of the same
`celestial_nail:celestial_nail` entity type. The existing optional Sodium Extras
exemption covers both, including restored saves; no second whitelist ID is needed.
The portal, fragments, shockwave and dust are rendered effects, not separately
registered entities. There are no additional entity types to exempt.

Impact bounds now include the ground-surface effect origin independently of the
buried anchor. Previously, a small Nail with high power could have its surface
effects above the bounds used for frustum culling. All three game-version adapters
also expose `visualBounds()` through native `getBoundingBoxForCulling()`. This
allows compatible culling consumers to inspect the full visual extent while
preserving Nail distance/frustum checks and terrain depth testing. It does not
certify every third-party culling mod or change server tracking distance.

The packaged Extras matrix now requires twelve camera cases per absent/present
launch: six floating and six embedded. Grounded cases verify the synchronized
embedded phase, a solid ground plane hiding the buried body, vertical and horizontal
anchor cutoffs, both viewing angles and offscreen/missing-draw controls. New runtime
results are recorded on the PR; the dated 2026-10-02 results above cover floating
cases only.
