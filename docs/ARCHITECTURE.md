# Architecture

Celestial Nail owns a persistent strike entity and its presentation. Perfomant Boom owns reusable terrain execution.

| Location | Responsibility |
| --- | --- |
| `common/` | Loader-independent logic, geometry, shared assets and unit tests |
| `common-1.20.1/`, `common-1.21.1/`, `common-26.1.2/` | Version-specific entity, commands, rendering and game API adapters |
| `fabric-1.20.1/`, `forge-1.20.1/`, `fabric-1.21.1/` | Loom loader bootstraps, metadata and packaging |
| `neoforge-1.21.1/`, `neoforge-26.1.2/` | ModDevGradle loader builds incorporating shared/version sources |
| `neoforge-1.21.1/src/gameTest/` | Isolated server regression fixtures, excluded from production JARs |
| `tools/` | Model/audio preparation and manual verification tools |

## Strike lifecycle

The server validates the command and creates a Nail with a unique ID. Emergence, floating, launch/descent, impact and embedded phases belong to the entity. Synced state drives client animation, while saved fields retain progress across world saves. Removal cancels damage and terrain work before its visual crumble completes.

During impact, the entity supplies Boom with its current sphere cursor, readiness checks and cancellation condition. It then coordinates fluid sweeps and repeated boundary passes. The entity persists the next positions and changed-pass state; Boom does not serialize Nail entities. Existing cursor NBT field names remain stable through the migration.

## Ownership boundary

Nail owns commands, permissions, special damage, timing, effects, saved stages and chunk tickets. Boom owns sphere/fluid/boundary traversal, per-level work allowances, no-drop replacement, vanilla block lifecycle adaptation and the mutation-scope mixins.

Never package a private copy of Boom into Nail. The external dependency keeps mutation scopes, budgets and mixins under one runtime owner. New reusable terrain logic belongs in Boom; Nail-specific appearance and strike choreography stay here.

## Version changes

Check the exact target's vanilla sources and loader behavior before changing block lifecycle, build-height, tickets or rendering APIs. A matching method name in another version is not sufficient evidence. Keep the source audit and [verification notes](../TESTING.md) current, and distinguish compilation from actual client/server execution.
