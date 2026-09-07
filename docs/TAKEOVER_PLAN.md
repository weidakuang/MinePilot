# MinePilot takeover assessment and development plan

Date: 2026-09-06

This is a source-grounded assessment and proposed implementation sequence, not
a release claim. It supplements `HANDOFF.md`; it does not replace the rebuild
rules or mark any new gameplay capability accepted. No production behavior was
changed during this assessment.

## 1. Product direction

Build a capable Minecraft teammate whose ordinary chat connects to reliable,
interruptible gameplay. The user wants to build, gather and produce resources,
complete survival progression, and eventually construct large productive
redstone machines together with the Agent.

Treat human-like experience as observable behavior: timely acknowledgement,
continuous purposeful motion, conversational turn-taking, useful initiative,
remembered commitments, coordinated work, and truthful recovery from mistakes.
Do not promise that an unrestricted Agent is indistinguishable from a human.
Keep the existing visible AI label.

Preserve the Forge 65.x / Minecraft 26.2 / Java 25 platform and the current
server-authoritative player body. The existing authorized world-information
advantage may remain; actions must still respect the agreed survival rules.
There is no reason established by this assessment to restart the implementation
or change the game platform.

## 2. State verified during this assessment

- Branch: `codex/agent-rebuild-v2`; HEAD: `467afa7`.
- Existing modified and untracked rebuild files were preserved. No staging,
  commits, branch changes, stash operations, or GitHub operations were performed.
- Read `AGENTS.md`, `CODEX_GOAL_REBUILD.md`, `docs/HANDOFF.md`,
  `docs/CODEX_RECOVERY_CHECKPOINT.md`, and the explicitly supplied companion
  Skill. Reviewed the runtime, body, model loop, navigation, and MCP observation
  paths.
- Re-ran Java compilation and JUnit using the repository JDK 25 and
  `./gradlew test --rerun-tasks --no-build-cache -Pforge_compile_version=65.0.9`.
  Gradle succeeded. XML reports show four tests, zero failures/errors/skips.
- Re-ran the two Python Skill client tests. Both passed.
- The existing repository JAR and XMCL installed JAR both hash to
  `c1d41e5ae18322cb50d648e2fbcf436fcea2771eabf3c39f87bbcfaf6c0808af`.
  This verifies agreement of those two existing artifacts, not that either
  incorporates every current source file. No replacement JAR was built.
- The public Skill `observe` attempt failed because its configured token file
  was unavailable/unreadable. No new live-body observation was obtained.
- No real-client world, physical GameTest, or remote-model acceptance gate was
  run during this assessment. Previous physical evidence remains the historical
  evidence described in the handoff.
- The XMCL directory/version discrepancy in `HANDOFF.md` remains unresolved.

## 3. Concrete gaps in the present source

### Conversation, action ownership, and interruption

`AgentBrain.startQueuedInputIfPossible()` defers input while `modelBusy` or
`pendingPlanToolCallId` is set. New chat can be handled during movement after
the preceding model call returns; it is not blocked for the entire walk.
However, urgent new input still shares the model request queue. The HTTP
request timeout is 90 seconds. This is a potential long interruption delay,
not a measured latency result.

The first navigation has four sequential model decisions: request, say, plan,
and choose. Local physical control already runs independently afterward.
Measure acknowledgement and movement-start latency before optimizing. Preserve
the visible acknowledgement barrier while investigating safe orchestration
improvements; do not silently weaken the accepted protocol.

Add prioritized cancellation and stale-result rejection as a small, tested
extension when addressing this gap. Eventually conversation and task execution
need separate scheduling, with one owner of physical actions. Multiple models
or controllers must not compete for the body. A fast stop path must remain
available while a provider request is slow or fails.

### Model and MCP observation parity

`AgentBrain.worldState()` includes position, health, food, players and nearby
entities, but no inventory/hotbar contents. The MCP `CodexToolService` supplies
inventory/hotbar observations. Giving Codex a capable observation path does not
automatically give the in-game model that same information.

Before item tools, share the necessary observation schema and capability
definitions between both entry points. Keep timestamps/revisions and preserve
structured data. Test both controllers separately, with exclusive ownership.
The Codex Skill is a useful operator and test surface; normal player chat must
eventually work without Codex polling alongside the game.

### Terrain changes and long-distance navigation

`NavigationToolCoordinator.markWorldChanged()` is defined but has no call sites
in the current `src` tree. The follower does inspect the next step for new
collisions, and moving destinations are checked every ten body ticks. Those
mechanisms do not establish general corridor invalidation or predictive
avoidance of moving entities.

`NavigationSnapshotBuilder` captures cells on the server thread. Its default
limits include a 96-block horizontal span, a 48-block vertical span, and up to
350,000 cells; margins consume part of the span. Large destinations explicitly
fail with a request for a global corridor. A worker-thread path search does not
make this snapshot capture asynchronous. Profile capture and planning
separately before changing limits.

Connect relevant world changes to bounded corridor invalidation. Establish
local terrain reliability first. Later use a coarse route/corridor and bounded
local segments for distance travel, with explicit loaded/unloaded knowledge and
chunk budgets. Do not solve distance by copying a larger whole-world volume.

The cell representation reduces collision shapes to booleans. Partial blocks
and special terrain require dedicated physical tests before claiming that
ordinary natural terrain is covered.

### One arrival is not a persistent follow task

Moving-player targets are re-resolved while navigation executes. The follower
emits `COMPLETED` and ends its route on arrival. That is a one-shot arrival at a
dynamic target, not a durable follow behavior.

A later follow task needs a maintained distance band, hysteresis, yielding to
the player, appropriate waiting, target-loss handling and cancellation. Local
route maintenance should not require a fresh language-model round trip every
time the player takes a step. Material changes in risk or resource use should
still return to the decision layer.

### Prompt and advertised capabilities

`AgentSystemPrompt` mentions optional silence/sneaking after completion, but
the executable model surface has no such idle action. Its text about automatic
path repair and threats also exceeds accepted physical evidence. Align the
prompt with phase-valid actions and label predictions as predictions before
the next live provider test.

Do not extend the existing single-request brain into an enormous switch for
every future gameplay task. Introduce shared lifecycle mechanisms only when
the next verified capability actually needs them.

## 4. Architecture to grow toward incrementally

```text
Player chat + observed world events
                |
Conversation and task interpretation <----> bounded, grounded memory
                |
Task executive: dependencies, commitments, resources, interruptions
                |
Verified gameplay skills: navigate, acquire, drop, mine, use, craft, build
                |
One action owner + local reactions + continuous control
                |
ServerPlayer and ordinary game interactions
                |
Observed outcomes, failures and changed world state ----> next decision
```

The language model should interpret goals, negotiate task scope, compose
verified skills and choose tradeoffs. Local controllers should own movement,
aiming, interaction timing and urgent reactions. The task executive should
retain progress, resume after interruptions, and track prerequisites. Observed
world state should determine completion.

A reusable gameplay skill should specify its input, preconditions, resource
needs, allowed effects, completion observation, cancellation behavior and
recoverable failure reasons. Add these properties to actual implemented skills
rather than registering empty future capabilities.

Resource goals require an executable dependency plan: recipes, quantities,
fuel, tools, durability, workstations, storage and transport. Use the running
game's recipe/state data and observed inventory to constrain that plan.
Language-model recipe recall alone is insufficient evidence of feasibility.

Start memory with current commitments, base location, player preferences,
material reservations and recoverable task progress. Partition durable facts
by world and dimension; distinguish observations, player statements and
inferences. Revalidate facts such as chest contents. A vector database is not a
prerequisite for the first useful memory.

## 5. Delivery sequence and acceptance

Each row is a sequence of small capability gates, not a single large feature
branch. Earlier evidence must remain reproducible as later skills are added.

| Stage | Player-visible result | Required evidence |
| --- | --- | --- |
| A. Reproduce the current body | The installed build appears and walks to the real player after a Chinese acknowledgement. | Actual loader/JAR identity, visible body/chat, before/after coordinates, requested radius, stable stop, health and inventory. |
| B. Reliable nearby teammate | Traverses individually tested terrain, yields or recovers from obstruction, follows persistently when that skill is added, and stops on interruption. | New terrain layouts, multiple player movements, blocked paths, cancellation latency, no old action restarting after cancellation. |
| C. Material exchange | Acquires a player's dropped items and physically returns a requested quantity. | Agent inventory plus item entities/recipient inventory; quantities reconcile, including partial stacks and full inventories. |
| D. Useful survival labor | Gathers requested materials, uses tools/food, crafts and handles storage; later combines these into building a small shared shelter. | Block/item changes, actual recipe inputs/outputs, durability, workstation use, survivability, and recovery from missing resources. |
| E. Construction and production | Builds a small design, repairs incorrect placements, and operates a simple farm or production line. | Material consumption, placed block states/orientations, navigation access, inventory output over game time and recovery after interruption. |
| F1. Cooperative progression | Shares preparation and execution of increasingly long survival and dimension objectives. | Supplies, equipment, portal travel, combat outcomes, death/respawn recovery, and agreed progression milestones. |
| F2. Redstone engineering | Reproduces validated modules, assembles a larger machine, diagnoses faults and eventually adapts a design. | Version-bound functional tests, measured output, sustained operation, material accounting and reproducible fault recovery. |

F1 and F2 branch after their actual prerequisites. Completing the End is not a
prerequisite for pursuing the user's construction and production interests.
Equipment/food, combat, crafting, containers and building within D should still
be introduced and accepted one at a time in dependency order.

The first compact capability loop after navigation is the handoff's item loop:
give the Agent an item, observe acquisition, request return through chat,
observe an actual drop, and reconcile inventory and world state. The first
broader enjoyable session should be shared gathering and a small shelter, with
the user able to interrupt, change the material target, and resume work.

Human-like behavior is a quality requirement in every stage: concise relevant
chat, continued action while talking, remembered instructions, spatial courtesy,
coordinated resource use and honest failure. Do not defer all of these until
after survival progression or attempt to substitute extra chat for competence.

## 6. Large redstone machines need their own engineering path

Separate three abilities: reproducing a verified design, diagnosing/repairing
its physical realization, and inventing a new working design. Success at the
first does not prove the other two.

Begin with compact version-verified modules: a small door mechanism, item
transport/sorting or a simple automated production unit. For each selected
design, record supported edition/version, inputs, outputs, activation method,
material bill and environmental assumptions. Verify actual behavior in the
target Forge world; do not assume an old tutorial applies unchanged.

Represent a build as more than coordinates: exact block states and direction,
settings, placements and interactions, support requirements, dependency order,
temporary access, liquids, required initial container contents, and testing
steps. The Agent must be able to reach the work face using real movement and
ordinary placement rules and remove its temporary access safely.

Compile a design into a staged material and construction plan. Detect missing
supplies before that stage starts. Verify component behavior, then subsystem
behavior, then the assembled machine. Record unexpected states and let the
Agent locate and repair mismatches instead of blindly rebuilding everything.

Functional acceptance must specify operating conditions and an observation
window. Measure output delta, consumed input/fuel, uptime and failures. Record
loaded chunks, game ticks, wall time and server performance so claimed
throughput is interpretable. Test restart/interruption and relevant unload and
reload behavior. A block-for-block visual match alone is insufficient.

Only after reliable construction and debugging should the Agent optimize or
invent designs. Test experimental designs in an explicitly selected disposable
world before a shared build. In normal play, material and world mutations must
still come through the established survival action boundary.

## 7. Immediate takeover work packages

1. Complete the real-client gate from the handoff using the identified XMCL
   instance and a disposable world. Record the actual Forge version, not only
   the directory name. Use the companion Skill to obtain independent physical
   observations. This gate is still pending.
2. Reproduce the first failing natural-terrain case and fix its general cause.
   Include dynamic obstruction and corridor invalidation coverage. Add a
   priority-stop case with a deliberately delayed model response. Do not start
   mining merely because one flat-ground route passed.
3. Align the prompt and model/MCP observations. Verify the normal player-chat
   path against an actually functioning provider, including cancellation and
   truthful provider-failure behavior. Retain controller exclusivity. The
   earlier provider failures do not establish current provider availability.
4. Implement normal item acquisition/drop as the next small gameplay change,
   then physically verify quantities through both the public chat path and the
   external operator surface as appropriate.
5. Expand only from demonstrated dependencies. Record each accepted result with
   source/build identity and actual observations; update documentation to match
   the resulting capability. Follow the repository preflight before any future
   authorized commit.

## 8. Evaluation practices and decision thresholds

Keep three evidence categories distinct: unit/protocol tests, controlled
physical world tests, and integrated real-player sessions. All matter, but
none substitutes for the others. Normal in-game model chat and external MCP
operation are separate end-to-end paths.

For each capability record world/version, scenario, actual request, starting
resources, allowed actions, success conditions, deadline, final observations
and failures. Hold out terrain layouts/seeds from implementation tuning.
Repeat independent cases instead of repeatedly demonstrating one fixture.

Measure success rate, false completion claims, human rescue interventions,
acknowledgement latency, movement-start latency, cancellation latency, planning
and snapshot cost, server tick time, model calls and tokens per completed goal.
Set release thresholds after measuring baselines. A successful test count or a
selected video does not establish a general reliability percentage.

Use blinded human ratings of usefulness, conversational timing, coordination
and recovery for the teammate experience, alongside physical metrics. Do not
use fluent prose as evidence that actions happened.

## 9. Research context

[Voyager](https://voyager.minedojo.org/) provides evidence for reusable,
composable skills and iterative improvement from environment feedback. Its
research implementation uses executable code as its action space. The proposed
MinePilot design borrows the composition and feedback idea, while retaining
typed, constrained production actions and independent physical acceptance.

[STEVE-1](https://sites.google.com/view/steve-1) studies short-horizon text and
visual instruction following with pixel inputs and low-level controls. It is a
different control approach, not evidence that this Forge body or long-term
redstone engineering already works. Neither project establishes compatibility
with this rebuild or a drop-in human-equivalent companion.
