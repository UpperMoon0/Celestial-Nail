# Usage guide

Install matching Nail and Boom JARs on server and clients using the [installation matrix](README.md#installation). Commands require game-master/operator permission; enable cheats for single-player. Test destructive operations in a disposable world first.

## Commands

| Command | Purpose |
| --- | --- |
| `/celestialnail summon <id> <x> <y> <z> [power] [scale]` | Create a Nail and begin emergence |
| `/celestialnail launch <id>` | Launch a Nail after emergence completes |
| `/celestialnail remove <id>` | Stop future damage/clearing and begin harmless removal |
| `/celestialnail list` | List active Nail IDs across loaded dimensions |

The ID must be unique across loaded dimensions. Coordinates specify the final floating **tip** and the vertical strike line, not the model center or portal. Relative coordinates are evaluated at the command source.

| Parameter | Default | Range | Meaning |
| --- | --- | --- | --- |
| `power` | 32 | 4–128 | Crater radius in blocks |
| `scale` | 1 | 0.1–4 | Visual size multiplier; default height is 72 blocks |

Both numeric arguments accept decimals. To specify scale, also supply power: `32 0.5` gives a 36-block-tall Nail with the same radius as `32 1`. Scale does not change terrain power.

## A complete strike

1. Summon: `/celestialnail summon event_01 ~ ~80 ~ 32 1`.
2. Wait for the opening and emergence sequence to finish, about 8.5 seconds at normal tick rate.
3. Launch: `/celestialnail launch event_01`.
4. The portal closes over about 1.5 seconds, then the Nail accelerates downward until its tip intersects terrain.
5. Impact starts staged terrain clearing and damage. The Nail embeds and remains as a landmark when the strike completes.
6. Remove the landmark when desired: `/celestialnail remove event_01`.

Do not run summon and launch immediately back-to-back. A command success confirms the requested transition, not completion of the whole animation or terrain job. At low server tick rates, sequences take longer in wall-clock time.

## Removal, saving and chunks

Removal works during emergence, floating, descent, impact and the embedded phase. It immediately cancels further damage/clearing, then plays a roughly 2.7-second crumble animation. Repeating removal does not restart the animation. Visual fragments never place or break blocks.

Removal is not undo: already changed terrain, emptied containers and dealt damage remain. Restore a backup if you need the original world.

Nails are persistent networked entities. Saved state includes their stage, appearance and impact progress. Completed strikes do not replay after reload. Each Nail retains its own chunk access so it remains addressable after players leave; impact work requests additional chunks incrementally. Removing one Nail preserves pre-existing forced chunks and shared ownership needed by other Nails.

## Impact behavior

The blast clears an inside-out sphere without item drops, including vanilla container contents. Fluid cleanup and repeated immediate-boundary repair follow the main clearing pass. Sources outside the crater remain intact and can flow back. This is not a permanent fluid barrier or a general recursive physics settlement system.

Nearby living entities take damage during the active blast and traveling wave, respecting normal damage immunity frames. Commands intentionally bypass normal explosion/player-break protection filters and `mobGriefing`. Restrict access; a land claim is not protection against an authorized Nail strike.

Terrain operations share per-level work limits through Boom. Large or simultaneous strikes can take multiple ticks to finish. Native/modded callbacks, lighting and cold chunks can still cause delays. See [the detailed reference](docs/REFERENCE.md) for exact stages, budgets and visual behavior.

## Troubleshooting

- **Missing dependency:** match the game and loader for both Nail and Boom, and check Fabric API/Architectury requirements.
- **Duplicate ID:** choose another ID or remove the existing Nail and wait for its removal to finish.
- **Launch refused:** wait for emergence to finish; a Nail that has already launched cannot launch again.
- **Unknown ID:** use `list` and check the spelling and active world.
- **Water returns:** outside sources are deliberately retained.
- **Slow strike:** test a smaller power and inspect server tick load; increasing visual scale does not reduce the crater work.
- **Missing distant effects:** entity tracking/view distance limits which clients receive a Nail; horizontal effect reach does not force the server to track it everywhere.

For mod conflicts or crashes, include versions, exact command, reproduction steps and logs. See [compatibility](docs/COMPATIBILITY.md) and [integration](INTEGRATION.md).
