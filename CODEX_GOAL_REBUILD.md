# MinePilot Clean Rebuild Objective

## State

MinePilot has been reset to a minimal Forge mod baseline on the
`codex/agent-rebuild-v2` branch. The baseline registers only the mod entry
point and deliberately implements no Agent behavior.

The retired implementation remains recoverable from Git history and the local
stash named `pre-agent-rebuild-wip-2026-09-04`. It is not part of this branch's
runtime.

## Working rule

Rebuild the Agent incrementally in the order explicitly directed by the user.
Do not anticipate later features by adding dormant frameworks, placeholder
skills, scripted demonstrations, or copied legacy subsystems.

For every new capability:

1. Define the externally observable behavior.
2. Implement the smallest production path that can perform it.
3. Exercise it through the same public input path a player uses.
4. Verify actual coordinates, health, inventory, equipment, world blocks,
   entities, menus, and vanilla statistics as applicable.
5. Record failures honestly. Speech, planner output, internal state, code
   presence, and controlled fixture setup are not success evidence.

## Fixed platform

- Minecraft Java 26.2
- Forge 65.x, loader metadata `[65,66)`
- Java 25
- Mod ID `mcai_companion`
- Apache License 2.0

No body, model provider, credential store, chat controller, movement, gameplay,
memory, UI, MCP, Xaero integration, skin system, or compatibility adapter is
approved for the rebuild until it is introduced and verified in a later,
explicit step.
