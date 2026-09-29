# Integration guide

Celestial Nail can be orchestrated through Minecraft commands for maps, events and server scenarios. This guide describes that interface and the development boundary; it does not promise a stable Java API for entity internals.

## Command blocks and datapack functions

Use the command syntax from [USAGE.md](USAGE.md). Datapack functions omit the leading slash. For example, a one-shot summon function can contain:

```mcfunction
execute positioned 100.0 80.0 100.0 run celestialnail summon event_01 ~ ~80 ~ 32 1
```

This example puts the final floating tip at 100, 160, 100 in the execution dimension. A separate trigger, after emergence has completed, can launch it:

```mcfunction
celestialnail launch event_01
```

A cleanup function can request removal:

```mcfunction
celestialnail remove event_01
```

Use separate triggers or your scenario's scheduling logic. Emergence normally takes about 170 server ticks; allow a margin before launch and test your sequence under server load. A fixed delay is not a completion signal for terrain work. Nail has no documented command that waits for an impact to finish.

Command blocks require the server's command-block setting and a permitted command source. Datapack functions also run under the server's execution permissions. Trigger summon once per event, rather than continuously from a repeating command block or tick function. Allocate unique IDs and retain them for later launch/removal.

## Deployment and lifecycle

Distribute the matching game/loader versions of Nail and its required Boom dependency on server and clients. Include the additional dependencies in the [installation matrix](README.md#installation). A dependency relationship belongs in your pack or distribution metadata; copying library classes into another mod is not a substitute.

Nail owns persistent entities, strike stages, damage, presentation and chunk ownership. Its terrain calls use Boom's shared engine. Removal cancels future work without restoring terrain. Account for world saves, reloads and failed/duplicate command triggers when designing your event state.

Permissions are part of your integration: direct terrain editing bypasses filters intended for ordinary explosions or player block breaking. Gate player-triggered events through your own authorization rules before issuing administrative commands.

## Java integrations and source development

The documented external control surface is the command interface. Internal entity classes, NBT fields and renderer implementation are not a versioned public API. There is no dedicated scripting integration promised by this repository. If an integration directly calls internals, pin the source/version and test the exact loader rather than assuming binary stability.

For reusable scheduled explosions or terrain passes in your own Java mod, use the [Perfomant Boom library](https://github.com/UpperMoon0/Perfomant-Boom) directly. Depend on Nail when you need its actual strike entity and presentation, not merely its terrain engine.

For changes to Nail itself, follow [CONTRIBUTING.md](CONTRIBUTING.md). Build the Boom version selected by `boom_version` into Maven local before building Nail; the CI dependency revision is configured by `PERFOMANT_BOOM_REF`. The dependency must exist at the selected version/revision. Nail's source and runtime tests remain in this repository; its dependencies own their own releases.

Validate command permissions, duplicate IDs, delayed launch, cancellation, save/reload and client presentation for your scenario. See [TESTING.md](TESTING.md) for the repository's test scope and [architecture](docs/ARCHITECTURE.md) for responsibilities.
