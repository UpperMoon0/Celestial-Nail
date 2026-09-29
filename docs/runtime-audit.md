# Vanilla runtime audit

Baseline: `563c1be0a244253555cd4c827c813f618e65bfb5` (2026-09-29).
Reference: `MC-Modding-Src` at `2e782f8ea29b04094efc76b5a59f51a7174013ee`, versions 1.20.1, 1.21.1 and 26.1.2. The original review was source-based; successful builds were not treated as live reproductions.

This document records the baseline findings and the required regression coverage. Implementation status and verification results belong in the pull request; the descriptions below are not claims that every item is already fixed.

## P1: below-world removal interrupts impact and can re-acquire a chunk

The nail calls `super.tick()` before `forceOwnChunk()`, with no removed-state guard. Its impact animation moves the real position down by `power + height * 0.18`. Vanilla `Entity.baseTick()` checks below-world position and discards entities below the dimension minimum minus 64. A sufficiently deep impact can therefore discard the nail before the blast finishes, release its chunks, then acquire its own chunk again in the remainder of the same tick.

Acceptance: a deep, high-power impact finishes and remains embedded; intentional removal is terminal for ticking; no force-load is acquired after removal; removal and save/reload leave no orphan temporary tickets.

## P2: container removal still drops contents

`setBlock(AIR, 2 | 16 | 32)` skips ordinary block loot but does not suppress container removal effects. In 1.20.1/1.21.1, `LevelChunk.setBlockState` invokes the old state's removal callback. In 26.1.2, block entities receive `preRemoveSideEffects` unless flag `0x100` is supplied. These paths can spawn container contents and work outside the block-change budget.

Acceptance: populated chests, hoppers and furnaces are removed without inventory item drops, including waterlogged containers, and block entities are cleaned up normally.

## P2: chunk forcing blocks and budgets are per entity

`ensureImpactChunkReady` calls `ServerLevel.setChunkForced(true)`. In all three inspected versions that API also synchronously calls `getChunk` when newly forcing a chunk. Returning from the nail's method afterward does not make the loading asynchronous. Concurrent nails also multiply the per-entity destruction allowance.

Acceptance: impact readiness uses nonblocking ticket acquisition and loaded-chunk checks; outstanding requests and per-level work are bounded; multiple nails share the work limit; save/reload and cancellation clean up ownership.

## P2: visual-sized entity bounds trigger expensive fluid scans

The physics bounds grow to approximately 37.44 x 288 x 37.44 at scale 4. Older `Entity.baseTick` fluid checks traverse the loaded bounding volume even with `noPhysics`; disabling fluid push alone does not disable that traversal. The 26.1.2 implementation has an additional section-level fluid-presence precheck, so the same unconditional dry-volume cost should not be attributed to it.

Acceptance: the admin-controlled nail does not perform full-model fluid scans or receive fluid displacement; the visual bounds and appearance remain unchanged.

## P2: completed overlapping impacts can exchange temporary ownership

Fluid cleanup releases temporary chunk ownership before setting `blastCleared`. The recipient eligibility check accepts any impact-phase nail, including one that has finished its blast but has not yet transitioned to embedded. Two impacts finishing in the same tick can transfer a ticket back to a completed nail which never releases it again.

Acceptance: completed or cancelling impacts cannot accept temporary ownership; overlapping impacts finishing together retain only required nail anchor tickets.

## P2: 26.1.2 top build layer is excluded

The port uses `< level.getMaxY()` in terrain and fluid traversal. In 26.1.2 `getMaxY()` is inclusive, unlike the older exclusive maximum build-height API.

Acceptance: both the minimum and maximum valid layers are included, and out-of-height positions are excluded, on every target.

## P2: boundary updates are never reconciled

Bulk edits suppress ordinary neighbor notification and shape propagation, then finish without reconciling surviving boundary states. For example, a torch outside the sphere can retain its state after its supporting block inside the sphere is removed.

Acceptance: a bounded deduplicated boundary pass reconciles neighboring survival/shape state without introducing an unbounded cascade or resuming destructive work after cancellation. Document the policy for secondary effects outside the crater.

## Validation baseline

The audit ran the shared unit suite (20 tests, zero failures/errors), incremental builds for all five targets, and a NeoForge 1.21.1 disposable-server startup. The loader-specific test tasks had no test sources. The existing manual smoke script does not cover the lifecycle and ownership cases above.

Required evidence for the fix includes shared policy tests, version-adapter compilation, and real-server regressions for removal, container contents, height boundaries, overlapping impact ownership, and bounded boundary cleanup. Do not label source assertions or compilation as a substitute for live behavior tests.
