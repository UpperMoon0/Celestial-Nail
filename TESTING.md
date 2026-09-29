# Verification

Run Gradle with Java 21. The 26.1.2 module resolves Java 25 for compilation.

## Shared deterministic tests

```sh
./gradlew :common:test
```

The 28 tests include existing animation/mesh/shell/fluid checks and eight new work-budget/boundary checks. New coverage verifies shared per-level ownership of an allowance, change and scan caps, chunk-request caps, elapsed-time cutoffs, per-tick reset, a six-neighbor brute-force boundary oracle, duplicate rejection, saved-cursor reconstruction, and quadratic traversal at maximum power.

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

This launches a disposable GameTest server in `neoforge-1.21.1/run/runtime-tests`. It never connects to the player's server or existing smoke world. Tests live in an isolated `gameTest` source set and are not shipped in the production jar. The dedicated runtime task exits nonzero when required tests fail. Successful evidence must include **all seven required tests**, not merely a server startup message.

The suite exercises:

1. A removed entity's tick and forced-chunk acquisition paths remain inert.
2. Impact and NBT-restored embedded phases survive a position below the normal vanilla discard threshold.
3. Populated waterlogged chests, hoppers and furnaces lose their block entities without creating inventory item entities.
4. A cold-chunk request enters the persistent vanilla forced set but does not synchronously load the chunk.
5. Two active overlapping impacts transfer a temporary chunk to the active recipient, then release it instead of handing it back to the completed first impact.
6. A real complete radius-four impact removes its support block and reconciles an unsupported torch just outside the sphere.
7. Boundary and completed-purge cursor fields round-trip through entity NBT without resetting progress.

Some fixtures use reflection to enter the entity's private impact/completion stages deterministically; no test-only accessors are added to production code. These exercise actual ServerLevel/entity/block behavior, but they are not command/network end-to-end tests or process-restart tests.

CI runs this suite after the 1.21.1 builds and uploads the runtime logs. The existing `tools/smoke_nail.py` remains a separate manual test with its own disposable-world guard.

## Remaining validation limits

The newly added runtime suite currently covers NeoForge 1.21.1, not all five loaders. In particular, 26.1.2's inclusive top layer and dedicated block-entity side-effect flag have source/compile verification but still need a version-native runtime lane. Whole-process save/restart, client rendering, multi-client networking, and end-to-end performance benchmarking are not covered by this suite.

Boundary reconciliation intentionally covers only the immediate six-neighbor layer and uses suppressed-propagation updates. It can remove unsupported boundary attachments and schedule normal fluid ticks, but does not promise arbitrary recursive redstone or mod-specific network settlement outside the crater. Fully settled worlds should not be inferred from a passed torch regression.
