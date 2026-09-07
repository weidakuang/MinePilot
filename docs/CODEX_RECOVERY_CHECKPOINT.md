# Codex Recovery Checkpoint

## Latest repair checkpoint: 2026-09-06

The five navigation review findings have been repaired. Read
`docs/reviews/2026-09-06-navigation-fixes.md` for exact changes, failed attempts,
commands and physical evidence. Final validation passed ten JUnit tests, two
Python client tests, the headless body gate, five controlled repair scenarios
in one GameTest, and a public Skill gate operated through the bundled CLI.
The final CLI driver was authored by the implementation assistant and was not
an independent source-blind model. This is still not real-client acceptance.

Current built JAR SHA-256:
`f7d74820cf616b3532b825f5d6929343a20aab7445470790f153385479bd5e40`.
The user's XMCL game was running the old installed JAR at the end of testing.
The new JAR was not installed live. A full Minecraft exit and replacement are
needed before the next real-client check. Existing uncommitted work and backup
artifacts remain preserved; no commit or GitHub work was performed.

Last updated: 2026-09-05

## Active direction

MinePilot is being rebuilt from the clean `467afa7` baseline. The current
scope is deliberately limited to the model-as-encoder system prompt and the
first physically executed navigation tool. Prompt/code audits use
`gpt-5.6-sol`; real gameplay tests use `gpt-5.6-luna`. Grok must not be used.

## Current root causes and fixes

- The old headless connection path called `Connection.tick()`. In Forge
  65.0.9, its packet listener ticks the player and then restores the position
  to the last client-reported coordinates. A headless player has no movement
  packets, so this silently undid every local movement frame. The session now
  ticks authoritative `ServerPlayer` physics directly and pumps outbound
  packets separately.
- Calling `ServerPlayer.snapTo` or `setGameMode` before `placeNewPlayer`
  created the packet listener caused null connection failures. Both operations
  now occur in a connection-safe order.
- The old weighted A* treated out-of-bounds `UNKNOWN` cells as solid floor and
  returned bridge routes that required more blocks than the inventory held.
  Bounds are now explicit and support use plus predicted damage are search
  state, so infeasible routes are pruned before they reach the model.
- Compass conversion was reversed. The runtime now maps MinePilot north=0 to
  Minecraft yaw=180 and converts other headings consistently.
- Speech-before-planning used to exist only in prose. The coordinator now
  enforces `NAVIGATION_ACCEPTED -> acknowledgement actually sent -> planning`
  as executable state transitions.
- Minecraft 26.2 treats an ordinary `Player` as client-authoritative. That made
  the server-side `LivingEntity.aiStep` skip `travel(input)` for the headless
  body even when the navigation controller continuously supplied forward input.
  `MinePilotServerPlayer` now explicitly declares server authority. The new
  body-physics GameTest proves continuous physical displacement without direct
  position changes in the follower.
- Model prose is no longer accepted as an executable result. Model turns must
  contain a phase-valid tool call, and the chat API is asked to require one.
  Acknowledgement chat validates the active navigation request before it is
  broadcast, stale route selections report that they did not start, completion
  waits for the requested arrival heading, and history trimming keeps tool-call
  groups intact.

## Files changed in the active rebuild

- `build.gradle`
- `src/main/resources/META-INF/accesstransformer.cfg`
- `src/main/resources/META-INF/mods.toml`
- `src/main/java/dev/mcai/companion/MinecraftAiCompanion.java`
- `src/main/java/dev/mcai/companion/agent/**`
- `src/main/java/dev/mcai/companion/gametest/**`
- `src/main/resources/data/mcai_companion/**`
- `src/test/java/dev/mcai/companion/agent/navigation/AnytimeNavigationPlannerTest.java`

## Verified gates

- Java 25 / Forge 65 compilation succeeds.
- JUnit regressions pass for out-of-bounds floor rejection and hard support
  block feasibility.
- Forge 65.0.0 GameTest server boots without a human player and reports the
  real `MinePilot[embedded]` player joining. Missing model configuration leaves
  the body online instead of preventing creation.
- Forge 65.0.9 now discovers the dedicated backend black-box navigation test
  through the data-driven GameTest registry. With the live-model flag disabled,
  the registration/server-lifecycle gate runs alongside the vanilla control
  test and both complete cleanly.
- Forge 65.0.9 passes `headless_control_frame_displacement`: the real embedded
  `ServerPlayer` advances at least three blocks under repeated control frames,
  retains health and inventory, and never exceeds the per-tick continuity
  bound. This is body-physics evidence only.
- The backend live gate injects normal player chat and restricts its acceptance
  observations to MinePilot chat, coordinates, health, inventory and hotbar.
  It requires visible acknowledgement, at least one block of displacement,
  arrival within 2.5 blocks, and a stable stop for twenty ticks.
- A `gpt-5.6-luna` subagent has now acted as the high-level model through an
  ephemeral loopback OpenAI-compatible relay. It received the unchanged
  production system prompt, tool schemas, world observations and tool results,
  then supplied `request_navigation`, `say`, `plan_navigation` and
  `choose_navigation` calls through the production `ModelGateway`. In the
  backend black-box gate, MinePilot physically moved about 8.79 blocks from a
  point ten blocks from TestHuman, stopped about 1.21 blocks from the target,
  retained 20 health, and kept its inventory/hotbar unchanged. Forge reported
  the required test pass. No direct coordinator call or position mutation was
  used by the model substitute.

These gates do not prove that the complete navigation loop works in a real
client. A `gpt-5.6-luna` real-game test against Forge 65.0.9 and `mimo-v2.5`
is in progress and must judge success from body displacement, not chat text.

## Last failed gate

Two server boots failed before the successful headless-player boot:

1. `snapTo` ran before `player.connection` existed.
2. `setGameMode` emitted player-list data before `player.connection` existed.

Both failures were reproduced in Forge 65 server startup and fixed. A custom
physical GameTest registration experiment did not register under Forge 65's
data-driven test registry and was removed rather than being reported as a
passing movement test.

The live backend navigation runs were correctly treated as failed:
the Keychain credential authenticated to the configured relay, but that
credential's channel returned HTTP 503 for `mimo-v2.5`; the former MiMo
endpoint returned HTTP 401 with the same credential. Grok was not substituted.
The latest black-box run showed only the visible model-connection failure; the
body did not begin navigation. No physical model-driven navigation claim was
made. Java compilation, JUnit, and the isolated body-physics GameTest pass.

The external MiMo credential/channel remains blocked, but it is now distinct
from the movement implementation: the same production model protocol and
runtime completed the black-box navigation gate when Luna supplied the model
turns through a loopback relay.

## Next actions

1. Supply or recover a valid `mimo-v2.5` channel credential without writing it
   to source/logs, then rerun the backend black-box gate. Its permitted evidence
   is limited to real player chat packets plus MinePilot coordinates, health,
   inventory and hotbar. It fails if the body never moves, moves before visible
   acknowledgement, misses the destination, or passes through without stopping.
2. Fix every observed body/protocol failure and repeat the same physical test.
3. Wire material terrain changes into route invalidation and add near-field
   dynamic obstacle prediction.
4. Add a cross-platform secret/profile loader after the navigation loop is
   physically proven; no credential may enter source, Git, or logs.
5. Only then package a test JAR. Do not claim M1-M4 or survival capability from
   this first-tool rebuild.

## 2026-09-05 Codex Skill checkpoint

- Added a loopback-only, bearer-authenticated Streamable HTTP MCP endpoint and
  the local `minepilot-companion` Codex Skill. The exposed surface is limited
  to observation, normal player chat, labelled AI chat, and the staged
  navigation protocol; it exposes no teleport, command, filesystem, or direct
  world-mutation tool.
- Installed the repository Skill locally as a symlink and created a non-secret
  local client profile. The rotating bearer token remains outside Git and is
  removed when the Minecraft server stops.
- A new independent Codex task successfully initialized the MCP connection,
  observed the live body, recorded its coordinates/health/empty inventory,
  sent visible Chinese chat, planned a route, selected it, and caused physical
  movement toward `TestHuman`.
- The last gate failed after the body moved from ten blocks away to about 0.77
  blocks from the target with unchanged health and inventory. The former test
  incorrectly measured three-dimensional drift from the instant the body first
  entered the 2.5-block arrival radius; that instant occurred while the body
  was still settling vertically. It also closed the server after only twenty
  ticks, before the independent Codex task could read the final state.
- The next run must detect an on-ground low-horizontal-speed stop for twenty
  continuous ticks, then preserve a read-only result-observation window before
  shutting down. Success remains based on body coordinates, health, inventory,
  visible chat, and actual movement rather than model or planner text.

## 2026-09-05 Codex Skill acceptance result

- An independent Codex task used the installed `minepilot-companion` Skill in
  a fresh Forge 65.0.9 backend world. It observed the body, sent visible Chinese
  chat, requested movement to `TestHuman`, inspected the route set, selected a
  route, and polled the terminal status.
- The final atomic status reported `COMPLETED`; MinePilot moved from
  `(-13771646.5, -48.0, -6632777.5)` to
  `(-13771637.268503191, -48.0, -6632777.486398082)`. The resolved destination
  was `(-13771636.5, -48.0, -6632777.5)`, leaving a measured distance of
  `0.768623553286504` blocks inside the requested `2.0`-block radius. Health
  remained 20, food remained 20, and the inventory and all nine hotbar slots
  remained empty.
- The post-audit GameTest selected only the Codex acceptance test and passed
  one required test. In addition to visible chat and physical body state, it
  verified the authenticated, request-correlated MCP sequence:
  `observe -> request_navigation -> say -> plan_navigation ->`
  `choose_navigation -> navigation_status(COMPLETED)`.
- Default GameTest runs now select only the headless physics regression; the
  opt-in live-model and Codex gates can no longer be counted as passing when
  they did not run.
- The Skill client now refuses redirects before a bearer can be forwarded and
  provides a one-time `configure` command that writes a mode-0600 non-secret
  connection profile. A local redirect regression reached the original
  endpoint once and the redirect destination zero times.
- External chat now observes the same controller-ownership lock as movement;
  queued server-thread operations are cancelled before execution on timeout;
  route execution is correctly marked potentially destructive; coordinate
  radii are preserved; dynamic targets are re-resolved at completion; and
  status exposes the destination, measured distance, inventory, last event
  reason, and currently valid recovery actions.

## 2026-09-05 Luna-as-model black-box result

- A `gpt-5.6-luna` subagent acted as the `mimo-v2.5` stand-in and was forbidden
  from reading source, server/mod logs, or GameTest internals. It used only the
  installed Skill, visible chat, coordinates, health, hunger, hotbar, inventory,
  and MCP navigation results.
- The first run was correctly rejected as unproven: the former one-minute
  terminal-observation window closed before Luna's final status poll. The gate
  now retains a physically stopped body for up to five wall-clock minutes and
  succeeds immediately after receiving a target-bound terminal status.
- In the fresh run, Luna observed first, emitted visible Chinese acknowledgement
  chat, and used `request_navigation -> say -> plan_navigation ->
  choose_navigation -> navigation_status`. MinePilot moved from
  `(5731171.5, -48.0, 958957.5)` to
  `(5731180.731496816, -48.0, 958957.5136019184)`, approximately 9.23 blocks.
  The resolved `TestHuman` destination was
  `(5731181.5, -48.0, 958957.5)` and terminal distance was approximately
  0.7686 blocks inside the requested 2.0-block radius. Health and food stayed
  at 20; inventory and all nine hotbar slots remained empty.
- Forge 65.0.9 selected exactly one required Codex Skill GameTest and reported
  `All 1 required tests passed`; Gradle exited zero.
- Terminal evidence is now one server-thread record containing request ID,
  outcome, target identity, resolved destination, and body coordinates. The
  GameTest binds all of those fields to the same request and physical target.
  Repeated polling is coalesced and a valid sequence is retained once observed.
- Navigation events now advertise exact callable MCP tool names only. A queued
  server operation is cancelled on interruption; if it has already started,
  the caller waits for its truthful result and then restores interrupt status.
- A final read-only `gpt-5.6-sol` audit closed all four remaining findings:
  target attribution, callable action names, interrupted execution semantics,
  and trace retention.

## 2026-09-06: in-game usability repairs

See `docs/reviews/2026-09-06-usability-repairs.md` for the current behavior,
source-informed versus Luna test attribution, raw evidence paths, artifact hash,
and remaining limitations. The supplied parkour is still not accepted. The user
played on `run-parkour-diagnostic-06`, game port 25569, MCP 25769; its offline-mode
configuration and saved world must be preserved. The separate usability test
server is `run-luna-usability-20260906`, ports 25570/25770. Do not confuse their
profiles or reapply the old parkour start-position fixture to the user's world.

### Resumed reconciliation, 2026-09-06 evening

The user confirmed that no concurrent Codex task was modifying the repository.
An archive was made before reconciling source/documentation mismatches. The
newer persistent model transport was retained. A public Luna jump test caught
missing MCP registration that the direct body GameTest had not covered; the
failed trial remains in `run-luna-usability-resume-20260906`. After repair, a
fresh real server in `run-luna-usability-final-20260906` passed jump, one-block
step, ten-block walk and conversation while walking. The observer inspected
only allowed physical/chat results, never private model decisions. An elevated
exact target produced a truthful search-limit explanation and no movement or
repeated jumps over a 35-second observation window.

Final build: 14 JUnit, 22 Python, Skill validation, and one Forge test containing
12 physical scenarios passed. Logs are retained in
`run-usability-stop-20260906/`. The final stop guard clears pending jump input
and marks stop as locally handled even before an action starts; this last Java
change was covered by its physical scenario rather than the earlier Luna run.
Artifact identity and remaining limitations are in the usability report and
the newest HANDOFF section. No original save, installed XMCL JAR or GitHub state
was changed. The supplied parkour remains unaccepted.

## 2026-09-07: inventory, orientation, perception and route material costs

See `docs/reviews/2026-09-07-inventory-perception.md` and the newest HANDOFF
section for source-informed physical evidence versus independent model-session
attribution. Implemented world-scoped item grades/notes and waypoints, actual
acquisitions, bounded sensors, turning and exact protected support manifests.
An independent Luna pickup failure exposed missing target arguments; a second
failure exposed velocity extrapolation that could put grounded dynamic targets
underground. A bounded pre-action argument repair and observed-position planning
fixed the tested path. Final single-chat collection physically moved 3.752 blocks
and acquired three apples in 15.530 seconds from an empty adventure inventory.

The Luna controller is a Skill-owned persistent local Codex app-server session,
not a subagent spawned through the parent's delegation tool. Its safe single-route
host optimization is not independent model route-choice evidence. The parent
did not inspect private model decisions. The user explicitly asked about this
distinction, and it was clarified. Preserve both earlier failed trials.

Final code passed 19 JUnit, 31 Python, Skill validation, the physical knowledge
gate and navigation regressions. Artifact hash and exact run paths are in the
review. No original saves, XMCL installation, commits or GitHub state changed.
Never compile development classes while a test server uses those classes.

### Follow-up: true spawned Luna and quieter acquisition chat

Dispatched Socrates through the actual subagent tool, model gpt-5.6-luna, no
inherited context. Its direct public-interface test reached a static target then
collected three apples, with physical proof recorded independently. See
`docs/reviews/2026-09-07-spawned-luna.md`. The test server was saved/stopped.
The supplied parkour remains unaccepted; cold subagent first motion took 41 s.

The user then clarified acquisition speech: default silent, optional only for
explicit requests or direct player relevance, and avoid repeated acknowledgements
unless requested. Updated Skill/model instructions plus host reason gating,
idle-review suppression and retained/deduplicated acquisition evidence. Added
regressions; 36 Python tests pass. Java/JAR unchanged.

The first live speech-policy trial caught an empty-model-message error that
repeated a successful pickup. Empty replies now mean silence; quiet navigation
cancellation still has a valid internal reason, and failed movement still gets
an outcome. Final count: 39 Python tests passed. Fresh real-server testing proved
40 seconds of silent ordinary pickup/classification and an explicit requested
three-apple report without repetition for 35 seconds after pickup. See
`docs/reviews/2026-09-07-inventory-chat-policy.md`. Both bounded speech-policy
servers were stopped/saved, with failure evidence preserved.


## 2026-09-07: native subtitle hearing

Added `listen` from real outbound sound packets, native 26.2 metadata, eight
relative directions, ear-distance and honest source confidence. The decoder
preserves vanilla broadcast/resource ranges and uses a three-second bounded
caption window. All public/model entry points expose the query; it never speaks
or moves automatically. Pure client/level-event sound coverage remains incomplete.
See `docs/reviews/2026-09-07-sound-perception.md` for exact limits and references.

First real gate failure: fixture heading was overwritten by its old control
frame. Second: tightly spaced overflow fixtures collapsed under native float
sound-packet precision at large coordinates. Both logs are retained. Corrected
fixture setup and production candidate matching; the final real sound gate and
knowledge/physical movement-inventory regression gate passed. 23 JUnit, 40 Python
and Skill validation passed. No independent Luna sound trial was run.

Final JAR SHA-256:
`26d45ab995a09617517d70e8ac1238bcce544c4d14225f56b1baf807f1f6a790`.
Test servers saved/stopped; installed XMCL artifact, original saves and stopped
user server/OP state remain unchanged. No commits or GitHub operations.


## 2026-09-07: movement continuation and honest failed parkour gates

See `docs/reviews/2026-09-07-movement-continuation.md`. Fixed moving-target baseline
mutation, continuous follow/replacement, stale model cancellation, duplicate stop
speech, public AI chat readback, shared knowledge schemas/execution, and partial
collision surfaces. Kept typed pace/target enums after the compact-schema live
failure. Thirteen real controlled navigation cases plus follow/knowledge gates
passed; 25 JUnit and 46 Python tests passed. Synthetic HTTP interface tests are
not provider acceptance. Two fresh independent Luna parkour copies both FAILED;
final near (92.45,-59,-36.47), inventory empty. Full provenance, global exploration,
complete structures, broad perception/performance and provider/client gates remain.

Final ordinary chat still took seconds: replacement 6.41, stop 4.66, with 5.6 extra
blocks while deciding to stop. Stop replied only once and then stayed physically
stable. Do not claim low-latency companionship. The final JAR was installed with
backups; SHA-256 `910e6a84cbdc82e66433d89a3f180727e962661d76a9b2d3709d5c66bd05ca17`. Client entry remains
unverified, including the launcher's previously observed Forge65.0.8 discrepancy.
Temporary tests and operators were stopped; no original world edits or GitHub work.
