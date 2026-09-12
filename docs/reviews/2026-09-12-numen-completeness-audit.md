# Numen completeness audit and follow correction — 2026-09-12

## Verdict

The agreed port is **not complete**. The deployed integration contains working
components and connected tools, but several requirements remain partial or lack
end-to-end evidence. This audit supplements, rather than replaces, the preserved
[implementation results](2026-09-12-numen-implementation.md).

Inspected upstream: Dwinovo/minecraft-numen commit
`34ef004dac3095fbbd928a897927e277c69d02fa`, local checkout
`/tmp/minecraft-numen-review-20260912`. The initial live binary matched the
previous delivery SHA-256
`b652843400db331f06fff12fde2410faf46265a3f3b04b9bdabe92c60033ade6`.

## Verified wiring and remaining scope

| Plan area | Code and evidence | Status |
| --- | --- | --- |
| Adapted upstream code | All 15 listed Numen classes plus three native adapters are present in the JAR. All 18 embedded source files match the corresponding working-tree sources. Every class has a runtime call site, including transitive event constants. | Connected, not dead copied files. |
| Public/model capabilities | All 19 names in `SurvivalTools.NAMES` appear in live MCP tools and the Python model schema. Both `AgentBrain`/`AgentToolSchemas` and `CodexToolService` dispatch them. | Wiring verified; this alone is not physical acceptance. |
| 150-block search | Live `find_resources` defaults to 150, reports loaded-only scope and rejects 151. Scanner, ring order, budget, candidate cache and native approaches are connected. | Range verified. Only loaded chunks can be searched; not every resource is reachable. |
| Mining, crafting, menus, facilities, building | Previous native fixtures and copied-world evidence verify real item deltas, native menu output, smelting, camp cells and pickup. Existing MinePilot executors are retained. | Implemented with the limitations recorded in the previous review. |
| Exploration | `GatherCoordinator` scans candidates, excludes failures and tries at most four checked outward moves. | Partial. The full upstream exploration/task runtime was not ported. A natural village expedition is unverified. |
| Persistent failure locations | `GatherCoordinator` keeps `failures` and `visited` in memory and clears them at each task start. Resource cache is short-lived RAM. `CompanionMemory` has no persistent failure-place ledger. | Missing the planned restart-persistent failure memory. |
| Conversation compression | `CompanionMemory.chat` uses `CompactSplit`, retains recent messages and caps old excerpts at 1,600 characters. The `summary` changes only through an explicit memory update. | Partial. There is no automatic semantic summarization pipeline equivalent to the full upstream memory loop. |
| Workstation/dialogue persistence | World/body-scoped dialogue, goal, camp and dimension-aware workstation records are saved; previous restart evidence verified them. | Implemented. Remembered statements remain distinct from current observations. |
| Responsiveness | The resident Python Codex listener, urgent events and immediate native actions are connected. Upstream AI transport is not imported. | Partial experience target: previous Luna decisions took median 9.265 seconds; this is not seconds-level universal dialogue. |
| Vanilla client | Server-only mod and listener; vanilla protocol login, chat and markers were exercised. | Protocol verified, rendered client unverified. New arbitrary N binding is unavailable to an unmodified client; use `/minepilot_mark` or `标记这里`. |
| Whole experience | Several real survival chains and targeted regressions passed. | No single clean ten-minute final all-feature acceptance run. Narrow entrances can still require repair. |

Read-only live audit output is saved with the follow delivery. These reads did
not grant items, change terrain or take control away from the production listener.

## Follow change requested during the audit

The user requested smoother following based on Numen while preserving normal
MinePilot movement. Inspected Numen's `FollowCompanionTask` resident-task policy
and `PlayerNav.trackGoal` live-goal behavior. The integration uses that behavioral
design with our existing corridor validation, route options and native inputs;
it does not import the upstream movement engine.

The old follower treated close approach as a fixed destination: brake, settle,
finish the executor, then plan another route after the player moved away. An
isolated real-physics slow-walk regression reproduced 15 stopped ticks out of 72
measured moving ticks. An initial run with hostile entity interference is also
retained; the controlled baseline used peaceful difficulty.

The correction retains a verified flat follow corridor, samples the target's
actual displacement and scales ordinary movement input to maintain spacing.
Waiting retains the same request and executor. A target moving again can resume
without a fresh model choice or path capture. Obstructed/non-level terrain uses
the existing planner and execution rules. Position, reach, inventory and normal
fixed-destination movement are not replaced.

The ordinary navigation regression also exposed an existing defect: an intended
one-tick reverse braking frame persisted after the route released control. The
same test failed with the original two navigation classes extracted from the
previous delivery. The body now consumes that counter-input exactly once; a new
owner's frame replaces it normally. No pathfinding algorithm changed. The
original terrain-disturbance assertion is retained; all 14 ordinary navigation
scenarios then passed, including gap jumping, ladders, stairs, slabs, forest
detours, a moving non-follow destination and stopping before lava/missing floor.

The physical follow gate includes full-speed walking, 40% walking input, 60%
walking input with an uninterrupted right-angle turn, chat while walking, two
80-tick rests followed by departure, cancellation, invalid/valid goal replacement,
offline target failure, a nearby player physically jumping during rest, and native player knockback. It drives both bodies with
native player physics, not teleports. Fixture setup alone sets their position and
adventure mode; inventory stays empty throughout navigation. Evidence is per tick,
not a claim about model inference latency or a natural village expedition.

## Final results and deployment

| Check | Result |
| --- | --- |
| Native slow following | Old: 15 stopped ticks / 72 measured moving ticks. Corrected WALK and AUTO: 0 / 72. |
| Turn without stopping the target | WALK and AUTO: 0 stopped ticks / 75 measured moving ticks. |
| Departure after waiting | 5–8 game ticks to more than 0.1 block of real movement. At normal 20 TPS this is 0.25–0.4 game seconds; the fixture itself is accelerated. |
| Ordinary navigation | All 14 scenarios passed with the original physical safety assertions. |
| Unit tests | 42 Java and 103 Python, no failures. |
| Installed Forge 65.0.9 | Isolated vanilla 26.2 protocol login and marker confirmed; no companion client handshake required. |
| Production restoration | Position, dimension, health and every inventory entry matched the snapshot before the normal save/restart. |

Production now runs JAR SHA-256
`bf469f5910a69b5cbe4d38ac13c78e1297eeb50a745711a3900557dca2f4309e`.
Server PID93138 and Luna Fast listener PID93259 were started; process identifiers
are transient. Game/MCP ports remain 25565/25766. The test server/listener are
stopped. All 18 embedded vendor/adaptor sources still match exactly.

Full stopped-server backup:
`.minepilot-backup/production-follow-20260912-145150/`. Delivery, public evidence,
source archive, hashes and rollback instructions: `deliveries/2026-09-12-follow/`.
The earlier Numen delivery and both passing/failing logs are preserved.
