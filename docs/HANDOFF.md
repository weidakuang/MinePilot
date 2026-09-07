# MinePilot Project Handoff

Last updated: 2026-09-07

## Latest movement/latency checkpoint

See `docs/reviews/2026-09-07-movement-continuation.md` and the installation JSON.
Continuous follow/replacement and thirteen controlled navigation scenarios passed,
including fractional slab surfaces. New chat cancels obsolete model decisions;
stop no longer replies twice. Seven knowledge tools share schemas/execution across
internal and external controllers. 25 JUnit/46 Python tests passed, with real
follow/knowledge gates and a synthetic loopback provider-interface test.

Two new independent Luna runs FAILED the supplied parkour, ending near
(92.45,-59,-36.47), with empty inventories. Do not report it passed. Ordinary
language remains slow: the final replacement reply took 6.41 seconds and stop
4.66 seconds, with about 5.6 blocks of movement during that stop delay.
Full provenance, exploration/global navigation, complete structure recognition,
natural-world performance, real internal provider and client/world acceptance
remain outstanding. The internal provider/model configuration question is pending;
no credential was placed in source or evidence.

The new development JAR is now installed in XMCL with original/intermediate backups.
Final SHA-256: `910e6a84cbdc82e66433d89a3f180727e962661d76a9b2d3709d5c66bd05ca17`.
Backend Forge was 65.0.9; the launcher version discrepancy remains unverified.
All temporary test servers/controllers were saved/stopped. Preserve the original
world and the stopped user-server/OP configuration; no GitHub work was performed.

## Earlier sound-tool checkpoint

The new read-only `listen` tool exposes native Chinese sound subtitles, eight
relative directions, ear-distance in blocks, native player/entity/block labels
and explicit source confidence. It reads sounds actually delivered to the body;
range follows each selected vanilla 26.2 sound resource and volume. Default
caption lifetime is three seconds, with bounded storage and paginated results.
MCP, direct Skill CLI, persistent model host and internal model schemas expose it.
It does not automatically speak, move or interrupt navigation.

See `docs/reviews/2026-09-07-sound-perception.md` for the full contract, source
references and evidence. Client-only/level-event-generated sounds, resource packs
and full client subtitle parity remain unsupported. Positional source matches
are candidates, not proven emitters. There was no Luna/internal-provider sound
conversation run in this update.

Final verification: 23 JUnit and 40 Python tests, Skill validation, one real Forge
sound gate and the existing physical knowledge regression gate all passed. Sound
snapshots and the two preceding failed fixtures are retained under
`run-sound-gate-20260907/`; knowledge regression evidence is under
`run-sound-knowledge-regression-20260907/`. Test servers stopped after saving.

The new development JAR supersedes older artifact hashes below:
`26d45ab995a09617517d70e8ac1238bcce544c4d14225f56b1baf807f1f6a790`.
The installed XMCL JAR, original worlds and user's stopped server remain unchanged.

## Latest independent-test and inventory-chat update

A true spawned `gpt-5.6-luna` subagent (Socrates) separately passed a two-stage
static-coordinate then dropped-item test using direct public tools and its own
route choices. See `docs/reviews/2026-09-07-spawned-luna.md`: first arrival was
0.2162 blocks from the requested point with an empty inventory, followed by
three physically acquired apples. Cold launch-to-first-motion was 41.035 seconds;
total pickup time was 66.367 seconds. This does not accept the supplied parkour.

The direct CLI now exposes the new gameplay tools and named dropped/waypoint
targets. The user's clarified acquisition policy is silence by default, optional
brief thanks/reports for direct player relevance or explicit requests, and no
repeated pickup/inventory/organization announcements. Host logic retains recent
acquisition evidence for later target-loss events, tracks already-reported gains,
and keeps idle reviews silent unless reporting was explicitly requested.
Python tests currently total 39; Java remains 19. Skill validation passes.
The final real-server speech trial verified silent ordinary pickup/organization
and one requested apple-count report without a repeated follow-up over 35 seconds.
See `docs/reviews/2026-09-07-inventory-chat-policy.md`; preserve the preceding
empty-message failure record.
The current scope and missing work are listed in
`docs/reviews/2026-09-07-request-coverage.md`. The JAR hash below is unchanged;
this update changed the locally linked Skill, not Java production code.

## Latest inventory/perception checkpoint

`docs/reviews/2026-09-07-inventory-perception.md` supersedes the artifact and
capability status below. The external Codex Skill now supports turning, bounded
perception, actual acquisition events, item grades/notes, exact per-route support
materials, dropped-stack targets and world-scoped named coordinates. See
`docs/INVENTORY_PERCEPTION_SPEC.md` for the refined contract and the review for
implemented limits. New tools have not been verified with the internal provider.

Verification: 19 JUnit and 31 Python tests, Skill validation, a real Forge physical
knowledge gate and the navigation regression gate. A persistent Luna model
session passed one-chat apple collection from empty inventory in adventure mode:
3.752 blocks of actual movement, three apples acquired after 15.530 seconds.
First movement took 14.682 seconds, so model latency remains a material issue.
This Luna session was started by the Skill through local Codex app-server, not
by the parent agent's subagent-spawn tool. The host may choose a unique safe
zero-material route. Do not label this a spawned-subagent or model route-choice
test. Failed parameter and dynamic-gravity trials remain recorded separately.

Final JAR SHA-256:
`e1701b591a0831a993043183cb6ff26f9d6c08d7a0fde30365d861402c41b4f5`.
The final storage guard and arrival-margin change have physical-gate evidence,
not a repeat Luna run.
The installed XMCL JAR and original saves remain unchanged. Bounded test servers
were saved/stopped; preserve the user's stopped offline server and OP state.
The supplied parkour, general terrain, complete structure recognition, exhaustive
item provenance and humanlike companion claims remain unaccepted.

## Latest movement/usability checkpoint

`docs/reviews/2026-09-06-usability-repairs.md` supersedes the artifact and
capability counts in earlier checkpoints below. Current verification is 14
JUnit tests, 22 Python tests, and one Forge gate containing 12 physical scenarios.
A separate Luna model-only controller also passed public-chat jump, relative
step, ten-block flat movement and conversation during movement on a real
dedicated server. The supplied parkour remains unaccepted. The latest measured
model delay is still noticeable; full-block stairs and ladders do not establish
general three-dimensional terrain traversal.

The public MCP includes `jump_once` and `jumpPhase`, signed `forward_blocks`,
and opt-in `allow_partial` with a distinct APPROACHED result. Normal player
navigation permits a partial approach; exact coordinate tests do not by default.
See the Skill for persistent session ownership and the safe unique-route policy.
Current JAR SHA-256:
`5098dbb330407d81a3dd25889c959228ee19853ab64dfcd61c31e93794684583`.
The installed XMCL JAR was not replaced. Preserve the saved offline-mode user
server `run-parkour-diagnostic-06` and its OP configuration; it was stopped when
work resumed. New bounded usability test servers do not replace that user world.

The first resumed Luna jump failed because the public tool registration was
missing despite a passing body test. Its failure record is retained. The fixed
public-interface results are in `run-luna-usability-final-20260906`; subsequent
local stop changes were physically checked separately. Do not combine their
attribution or claim the supplied map was completed.

## 2026-09-06 update (supersedes artifact and repair status below)

Five navigation findings are now repaired: armor-independent fall estimates,
exact-start/final-approach handling, live footing/fluid validation with physical
braking, stable-stop completion, and human-player discovery/UUID targets.
See `docs/reviews/2026-09-06-navigation-fixes.md` for full validation and limits.
The default body gate, five controlled repair scenarios, ten JUnit tests, two
Python client tests and the final public CLI/Skill gate passed. That final
operator was scripted by the implementation assistant, not an independent
source-blind model. Real-client acceptance remains pending.

The rebuilt `0.2.0-dev-mc26.2` JAR now has SHA-256
`f7d74820cf616b3532b825f5d6929343a20aab7445470790f153385479bd5e40`.
The old build is backed up under `run-navigation-artifact-backups/`.
The XMCL game was running during final tests, so its installed JAR remains the
old artifact described below. Exit Minecraft completely before installing the
replacement. The user Skill profile was preserved; isolated tests used their
own profiles, including port 25767 when the real game occupied 25766.

The remainder of this document describes the earlier checkpoint. In
particular, its old checksum and stale-prompt note are historical; use the
repair report above for current build identity and fixed behavior.

## 1. Executive status

MinePilot is an experimental Minecraft Java 26.2 Forge Agent. The repository
was reset to a clean baseline and is being rebuilt one observable capability at
a time. The current development build is `0.2.0-dev-mc26.2`.

The only end-to-end capabilities currently accepted are:

- creating one visible headless `ServerPlayer` body without a second Minecraft
  account;
- observing that body's position, health, hunger, hotbar, and inventory;
- receiving normal player chat and sending clearly labelled `[AI] MinePilot`
  chat;
- resolving a coordinate, player, entity, world spawn, respawn point, or death
  point as a navigation target;
- enforcing the staged navigation protocol `request -> visible acknowledgement
  -> plan -> choose -> execute -> verify`;
- physically moving the body across a simple prepared surface and proving the
  result from coordinates rather than text;
- controlling the same chat and navigation surface from a locally installed
  Codex Skill over an authenticated loopback MCP endpoint.

This is **not** a professional companion release. Mining, item handling,
crafting, combat, shelter construction, farms, redstone, trading, Nether/End
progression, speedrunning, long-term memory, skins, multi-Agent UI, and the M1
through M4 product claims are not implemented and verified in this rebuild.

## 2. Repository state

- Repository: `/Users/weida/Documents/minecraft-ai-companion-forge`
- Current branch: `codex/agent-rebuild-v2`
- Current HEAD: `467afa7 chore: reset MinePilot to clean Forge baseline`
- License: Apache-2.0
- Minecraft: 26.2
- Forge metadata range: `[65.0.0,66)`
- Java: 25
- Mod ID: `mcai_companion`
- Build version: `0.2.0-dev-mc26.2`

All rebuild work after `467afa7` is currently uncommitted. The working tree
contains modified and untracked production code, tests, resources, the Skill,
and this documentation. Do not reset, clean, switch branches destructively, or
restore the retired implementation over these files.

The retired implementation remains recoverable from Git history and the local
stash `pre-agent-rebuild-wip-2026-09-04`. It must not be copied back wholesale.

Before any commit, inspect the full diff, stage only intentional files, run
`scripts/preflight-before-commit.sh`, and avoid duplicate, placeholder, or
evidence-free commits. The user has currently asked not to perform GitHub work.

## 3. Architecture

### Agent body and runtime

- `MinecraftAiCompanion` owns server lifecycle hooks.
- `AgentRuntime` creates one stable MinePilot runtime and captures bounded
  visible player chat.
- `HeadlessPlayerSession` gives the Agent an embedded connection, pumps outbound
  packets, and ticks server-authoritative player physics.
- `MinePilotServerPlayer` is the actual body and owns vanilla position,
  inventory, hands, armor, health, food, effects, and statistics.
- Missing model configuration does not suppress the body. It leaves the body
  online and available to Codex external control.

The key movement fix is that the embedded connection must not call the normal
client-authoritative connection tick. A headless player sends no movement
packets, so that path restored the previous client position and silently undid
local movement. The current session ticks authoritative `ServerPlayer` physics
and pumps outbound packets separately.

### Model-as-encoder path

- `AgentSystemPrompt` tells the model to translate arbitrary natural language
  into typed tool calls and never treat prose as an action.
- `OpenAiCompatibleChatClient` is the single high-level model client.
- Configuration is runtime-only through `MINEPILOT_BASE_URL`,
  `MINEPILOT_API_KEY`, `MINEPILOT_MODEL`, and optional
  `MINEPILOT_TEMPERATURE` in `[0.0,1.0]`.
- Remote endpoints must use HTTPS. Plain HTTP is accepted only for loopback
  development.
- When an internal model controller is active, Codex external control is
  rejected. There is exactly one controller owner at a time.

The currently supplied external MiMo credential/channel did not complete a
live provider test: the configured relay returned HTTP 503 and the former MiMo
endpoint returned HTTP 401. No credential is stored in source, Git, world data,
the Skill profile, or this document. Treat every credential previously pasted
into chat as exposed and rotate it before future use.

### Navigation

- `NavigationToolCoordinator` owns request phases and enforces the visible
  acknowledgement barrier.
- `NavigationTargetResolver` resolves static and dynamic targets.
- `NavigationSnapshotBuilder` captures a bounded server-side world snapshot.
- `AnytimeNavigationPlanner` builds one to eight evaluated route options.
- `NavigationFollower` applies continuous control frames instead of changing
  coordinates directly.
- Planning runs off the server thread. World mutations and authoritative reads
  are marshalled back to the server thread.
- Completion re-resolves dynamic targets and checks the requested acceptance
  radius before emitting `COMPLETED`.

Planner fields include distance, time, predicted health/food loss, support
block cost, hazards, feasible state, supported paces, and path steps. The model
chooses among evaluated options; it does not generate raw movement per tick.

Only simple-surface physical navigation has black-box evidence. Door handling,
support placement, difficult terrain, moving-target pursuit, obstruction
recovery, water, cliffs, parkour, bridging, chunk-distance travel, and hostile
interference remain unaccepted until individually proven in real worlds.

### Codex bridge and Skill

`CodexMcpServer` binds only to `127.0.0.1:25766/mcp`. It uses a rotating random
Bearer token, Host and Origin validation, bounded request bodies, and owner-only
token-file permissions. The token is deleted when the server stops.

The exposed tools are:

- `observe`
- `read_chat`
- `say`
- `request_navigation`
- `plan_navigation`
- `navigation_status`
- `choose_navigation`
- `cancel_navigation`

There is no teleport, slash-command, filesystem, shell, credential, direct
inventory mutation, or direct world-edit tool.

The Skill is stored at `skills/minepilot-companion/` and locally installed as:

```text
/Users/weida/.codex/skills/minepilot-companion
  -> /Users/weida/Documents/minecraft-ai-companion-forge/skills/minepilot-companion
```

Its non-secret local profile is
`/Users/weida/.codex/minepilot-companion.json`, mode `0600`. It contains only
the loopback URL and the Minecraft instance's token-file path. The Python
client refuses HTTP redirects so the Bearer token cannot be forwarded to a
redirect destination.

One prompt sentence is now stale: `AgentSystemPrompt` still mentions
post-completion actions such as staying silent or sneaking that are not exposed
as MCP tools. Runtime `validActions` is correct and Sol accepted it, but the
prompt should be aligned before the next internal-model test.

## 4. Physical evidence already obtained

### Headless body physics

Forge GameTest `headless_control_frame_displacement` proves that the embedded
`ServerPlayer` moves at least three blocks under continuous control frames,
keeps health and inventory unchanged, and respects a per-tick continuity bound.
This proves the body boundary only, not general navigation.

### Codex Skill black-box navigation

A `gpt-5.6-luna` subagent acted as the high-level model substitute. It was
forbidden from reading source, GameTest internals, or server/mod logs. It could
use only the installed Skill, visible chat, coordinates, health, hunger,
hotbar, inventory, and navigation results.

In the accepted Forge 65.0.9 backend run:

- initial body position: `(5731171.5, -48.0, 958957.5)`;
- resolved TestHuman destination: `(5731181.5, -48.0, 958957.5)`;
- final body position: `(5731180.731496816, -48.0, 958957.5136019184)`;
- physical displacement: approximately 9.23 blocks;
- terminal distance: approximately 0.7686 blocks inside a 2.0-block radius;
- health and food remained 20;
- hotbar and inventory remained empty and unchanged;
- the request-correlated sequence was `observe -> request_navigation -> say ->
  plan_navigation -> choose_navigation -> navigation_status(COMPLETED)`;
- Forge selected exactly one required test and reported it passed;
- Gradle exited zero.

The terminal evidence record atomically binds request ID, outcome, target
identity, resolved destination, and body coordinates. The gate does not accept
model prose, planner output, or a `STARTED` phase as success.

A read-only `gpt-5.6-sol` audit closed the last four findings concerning target
attribution, phase-valid action names, interruption races, and trace retention.

## 5. Build and test commands

The repository-local JDK is used because no system JDK is registered:

```bash
export MINEPILOT_JAVA="$PWD/.toolchains/jdk-25/Contents/Home"
export PATH="$MINEPILOT_JAVA/bin:/usr/bin:/bin:/usr/sbin:/sbin"
```

Compile and run JUnit:

```bash
./gradlew test --rerun-tasks --no-build-cache
```

Run the default physical body gate on Forge 65.0.9:

```bash
./gradlew runGameTestServer --no-build-cache \
  -Pforge_compile_version=65.0.9
```

Run the independently operated Codex Skill gate:

```bash
./gradlew runGameTestServer --no-build-cache \
  -Pforge_compile_version=65.0.9 \
  -Dminepilot.codexSkillTest=true
```

That gate intentionally waits for a separate Codex/Luna operator. It must not
be reported as passing unless the operator uses the public Skill flow and a
target-bound physical terminal status is observed.

Run Skill client security tests:

```bash
python3 -m unittest discover \
  -s skills/minepilot-companion/tests -v
```

Build the development JAR:

```bash
./gradlew build --no-build-cache
```

The last built artifact was:

```text
build/libs/mcai_companion-0.2.0-dev-mc26.2.jar
SHA-256 c1d41e5ae18322cb50d648e2fbcf436fcea2771eabf3c39f87bbcfaf6c0808af
```

After source changes, regenerate the artifact and checksum. Never reuse the
recorded checksum for a different build.

## 6. Local XMCL installation state

The verified JAR copy is installed at:

```text
/Users/weida/Library/Application Support/xmcl/instances/
  26.2-forge65.0.9/mods/mcai_companion-0.2.0-dev-mc26.2.jar
```

Its checksum matched the repository build at the time of installation. The
previous `0.1.16` JAR was moved, not deleted, to:

```text
mods/.minepilot-backup/mcai_companion-0.1.16-dev-mc26.2.jar
```

The XMCL instance directory is named `26.2-forge65.0.9`, but the launcher UI
reported its configured Forge version as `26.2-forge-65.0.8`. The Mod metadata
allows this version because its range is `[65.0.0,66)`, but the discrepancy
must be kept visible in future test reports.

The last launch attempt opened XMCL and selected this instance, but the turn was
interrupted immediately after pressing Launch. A later process check found no
Minecraft/Forge Java process. Therefore Minecraft launch and real-client world
entry are **not verified**. XMCL itself remained running.

Once a world's integrated server has started, the Mod creates:

```text
config/mcai-companion/codex-mcp.token
```

Codex can then use `$minepilot-companion`. The token file does not exist while
the world/server is stopped, which is expected.

## 7. Known limitations and risks

1. No real-client acceptance run has been completed for the current JAR.
2. No valid external `mimo-v2.5` provider run has completed.
3. The current Skill can talk and move only. It cannot mine, pick up, drop,
   equip, eat, craft, fight, build, use containers, or interact with workstations.
4. Navigation evidence covers one short, flat, unobstructed route. It does not
   justify general terrain, follow, parkour, water-bucket, bridge, or survival
   claims.
5. The route snapshot and planner require performance and correctness gates on
   naturally generated worlds, distant targets, unloaded chunks, and no-human
   dedicated servers.
6. The current controller and runtime support one active Agent only.
7. There is no settings UI, onboarding flow, custom skin path, persistent
   memory, SQLite store, Xaero adapter, or general Mod adapter SPI in this rebuild.
8. Secret storage is environment-only for the internal model path. A production
   cross-platform credential store for macOS, Windows, and Linux is not present.
9. The current development JAR must not be advertised as M1, M2, M3, M4,
   professional companion, hardcore completion, or two-hour random-seed ready.

## 8. Required next steps

Proceed in this order and keep each gate evidence-based:

1. Finish launching the installed XMCL instance and enter a throwaway world.
   Confirm the current JAR loads, MinePilot appears in the player list and
   world, and the loopback token is created.
2. From a separate Codex task, use `$minepilot-companion` to observe, send one
   visible Chinese message, walk to the real player on flat ground, and verify
   the final coordinates. Record failure honestly if any step is missing.
3. Repeat navigation in natural terrain: slopes, doors, trees, one-block
   obstructions, safe descents, short gaps, moving targets, and temporary player
   obstruction. Fix general mechanisms, not fixture coordinates.
4. Align the internal system prompt with actual phase-valid tools, then rerun
   the public-chat model-as-encoder gate with a valid provider credential.
5. Add the next single gameplay tool: normal item acquisition and normal item
   drop. Test `give the Agent an item -> inventory observes it -> ask through
   chat -> Agent physically drops it -> inventory and world entity prove it`.
6. Only after movement and item handling are stable should the rebuild add
   mining, equipment/food reflexes, combat, crafting, containers, building, and
   longer survival objectives.

For every future capability, success means an externally observed state change.
Chat text, a model tool call, an internal log, a scripted assertion with no
physical observation, or code presence alone is not acceptance evidence.

## 9. File map

- `AGENTS.md`: repository execution and commit rules.
- `CODEX_GOAL_REBUILD.md`: clean-rebuild objective and acceptance philosophy.
- `docs/CODEX_RECOVERY_CHECKPOINT.md`: chronological root-cause and test notes.
- `src/main/java/dev/mcai/companion/MinecraftAiCompanion.java`: Forge entry point.
- `src/main/java/dev/mcai/companion/agent/AgentRuntime.java`: runtime and chat.
- `src/main/java/dev/mcai/companion/agent/body/`: headless player authority.
- `src/main/java/dev/mcai/companion/agent/model/`: prompt, schemas, and model client.
- `src/main/java/dev/mcai/companion/agent/navigation/`: target, planner, follower,
  route protocol, and physical movement.
- `src/main/java/dev/mcai/companion/codex/`: loopback MCP server and tool surface.
- `src/main/java/dev/mcai/companion/gametest/`: physical backend gates.
- `src/test/java/dev/mcai/companion/agent/navigation/`: planner unit tests.
- `skills/minepilot-companion/`: Codex Skill, client, metadata, and security tests.

## 10. Handoff rule

Start by reading `AGENTS.md`, `CODEX_GOAL_REBUILD.md`, this file, and
`docs/CODEX_RECOVERY_CHECKPOINT.md`. Preserve the dirty working tree and local
backup artifacts. Continue from the first unverified physical gate; do not
restart from the retired implementation and do not claim the long-term product
plan from the current narrow evidence.
