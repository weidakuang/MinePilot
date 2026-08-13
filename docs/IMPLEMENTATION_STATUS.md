# Implementation status

## 2026-08-11 continuation: current Forge 65.1.1 emergency golem recheck

After the first-login anchor regressions, the current source was exercised
again in a fresh Forge 65.1.1/JDK25 GameTest server with
`mcai_companion:real_emergency_iron_golem_duel`. The real headless
`ServerPlayer` and vanilla iron golem completed the controlled duel 1/1 in
6.159 seconds (`run-debug315-emergency-golem-current`). The persisted player
stats recorded 36 damage taken and 280 damage dealt, so this was not an idle
or visual-only assertion. It validates local damage reacquisition, vanilla
melee timing and survival response; it is deliberately no-model and
server-side, and therefore does not promote rendered-client, live-model,
PVP, Hardcore, or any M0--M4 gate. The expected closed embedded test-channel
warning was the only shutdown noise.

The product artifact remains the dirty development JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`498c5e44fa782f1683840711c5b6b63dd2b6aa874625e1afea80c8c76251b960`,
17,151,193 bytes. Formal real Actor/Observer/model and M0--M4 gates remain
`NOT_RUN` because this Darwin host still has no Linux/Xvfb worker or
authorized `MCAI_*` model configuration.

The same source was then rerun with the requested twenty-mob pressure fixture
(`real_emergency_zombie_skeleton_horde`, ten zombies plus ten skeletons) in
`run-debug316-emergency-horde-current`. It passed 1/1 in 6.157 seconds; the
real test player stats recorded 47 damage taken and 221 damage dealt. The
fixture requires multiple damaged targets, body displacement and vanilla
sword durability, so a stationary “look-only” response cannot satisfy it.
This remains a local no-model emergency/PVE check, not a live-model PVP or
random-seed survival result.

## 2026-08-11 continuation: current Forge 65.1.1/65.0.0 lifecycle-performance recheck

The current source was compiled and exercised by the real Forge GameTest
server on both the latest declared patch and the lowest supported Forge 65
patch. `run-debug309-lifecycle-current` (Forge 65.1.1, JDK 25) passed
`mcai_companion:headless_player_lifecycle_state_and_fair_action`, 1/1, with
6,126 samples, average 300,549 ns, rolling p95 1,432,750 ns, and a 22,004,041
ns window maximum. `run-debug310-lifecycle-floor` (Forge 65.0.0, JDK 25)
passed the same test, 1/1, with 5,755 samples, average 316,992 ns, rolling
p95 1,357,500 ns, and a 32,314,625 ns window maximum. Both runs exercised the
real headless `ServerPlayer` lifecycle, vanilla actions, persistence and
cross-dimension cleanup; both were deliberately provider-disabled inner-loop
checks. They do not promote rendered-client, real-model, PVP, Hardcore or
M0--M4 gates. The current development artifact remains `DIRTY_NO_COMMIT` and
`NON_RELEASE`.

## 2026-08-11 continuation: model action-path guard for bound follow

The planner now recovers one additional provider mistake that matched the
reported “收到但不走” symptom: when an authorized player-follow goal is
already installed and the verified model returns `SAFE_IDLE`, the server may
start only the bounded `follow_entity`/`survey_surroundings` recovery if the
fair first-person sample proves the target or permits reacquisition. The
recovered decision still goes through `SkillSupervisor`, vanilla movement and
the model audit trace; ordinary goals, Hardcore locks and unbound entities do
not receive this shortcut. `BrainOrchestratorTest` (51 tests) and the full
JDK25 `test` task pass. The rebuilt development JAR is
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`498c5e44fa782f1683840711c5b6b63dd2b6aa874625e1afea80c8c76251b960`,
17,151,193 bytes. Python E2E remains 59/59 and
`check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker` passes 16
actionable tasks. This remains inner-loop evidence only; the real
Actor/Observer/model and M0--M4 gates stay `NOT_RUN`.

The follow-only Forge 65.1.1 server recheck after this change also passed
`mcai_companion:real_player_chat_to_immediate_bound_follow` 1/1 in
`run-debug311-follow-after-safeidle` (about 1.39 s). It verifies the real
PlayerList chat boundary, stable-body replacement and vanilla bound-follow
physics; it is intentionally no-model and no-rendered-client evidence.

The production `ServerGoalCompletionVerifier` now also rejects a model
`COMPLETE_GOAL` for a PLAYER_CHAT or MCP gameplay goal until at least one local
skill lease has been accepted. This prevents a one-line “任务完成” from
silently ending a motionless task while preserving verified route completion
and ordinary recovery/social goals. Brain and verifier regressions pass.

The fresh Forge 65.1.1 follow recheck after this completion guard is recorded
as `run-debug312-follow-after-completion-guard`: the real PlayerList chat to
bound-follow slice passed 1/1 in about 1.431 s. It remains a no-model,
server-side inner-loop check and does not promote the formal Actor/Observer,
Hardcore, or M0--M4 gates.

The player-visible brain sink now emits one honest status for
`model_completion_without_action`: if a provider says “completed” before
the server accepts any skill, the earlier acknowledgement is corrected in
chat and the goal remains under bounded replanning. Locked evaluation worlds
remain silent, and the status cannot mutate gameplay state.

The delayed first-human anchor regressions were rerun on Forge 65.1.1:
`run-debug313-delayed-anchor-normal` passed 1/1 in 2.448 s and
`run-debug314-delayed-anchor-emergency` passed 1/1 in 4.308 s. They cover the
normal remove/relogin anchor and the emergency-owned deferred retry with a
stable UUID, inventory, and idle goal. These remain no-model server-side
checks.

The Forge discovery command was rerun against the official promotions/index
sources at 2026-08-11T14:06:49Z. It returned `PASS`: Forge 65/Minecraft 26.2,
latest 65.1.1, recommended 65.1.0, no missing adapters, no missing patches,
no stale patches, and no promoted Forge 66 line. This updates facts only; it
does not upgrade the unexecuted runtime compatibility matrix.

## 2026-08-11 continuation: Forge 65.1.1 exact-JAR dedicated-server smoke

The real `e2e/orchestrator.py server-smoke --forge-version 65.1.1` path was
run against the current production JAR. It built and installed the exact same
SHA-256 product copy in the server, actor and observer instance directories,
started the dedicated server with no human clients, observed the headless AI
`ServerPlayer` joining, loaded SQLite from the product JAR, and stopped
gracefully. The immutable run is
`e2e/results/no-commit-dirty/20260811T135504Z-e2e4d81b1652159`; its verdict is
`PASS`, with `functionalAiClaim=false`, server exit code 0, five Oracle
lifecycle events and all installed product hashes equal to
`63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`.
This is an exact-JAR lifecycle/no-human smoke only; it is not a real-model,
rendered-client, movement, Hardcore or M0--M4 completion result.

The matching Forge 65.1.1 `restart-smoke` then booted the same exact-JAR world
twice. Its immutable run is
`e2e/results/no-commit-dirty/20260811T135715Z-e2e2df1f5f42ce7`; the restart
verdict is `PASS` with two clean exits, two lifecycle starts/stops, one stable
companion UUID, one SQLite database and `functionalAiClaim=false`. This is
restart/persistence evidence only and does not promote the real-client/model
or M0--M4 gates.

The Gradle `e2eFunctional` entry point was also executed at Forge 65.1.1. It
failed closed with a machine-readable `NOT_RUN` result (run
`e2e/results/no-commit-dirty/20260811T140533Z-formal20260811140533`), listing
the exact missing prerequisites `linux_host`, `Xvfb`, `MCAI_BASE_URL`,
`MCAI_MODEL` and `MCAI_API_KEY_or_MCAI_API_KEY_FILE`. No client was launched
and no provider request was made.

## 2026-08-11 continuation: Forge 65.1.1 combat/parkour recheck and anchor contract

The current source was rerun in two fresh Forge 65.1.1/JDK25 GameTest
servers. `mcai_companion:real_emergency_iron_golem_duel` passed 1/1 in
1.083 seconds, exercising the real headless `ServerPlayer`'s damage
reacquisition, vanilla melee and shield/survival lane. The separate
`mcai_companion:real_parkour_course` passed 1/1 in 2.851 seconds, exercising
ordinary jump, collision and landing physics across the multi-step course.
These are no-model physical checks; they are not PVP, rendered-client,
live-model or Hardcore completion evidence.

Added `AiPlayerManagerInitialAnchorSourceContractTest` to keep the one-time
first-human reanchor tied to both the real emergency survival state and the
`EMERGENCY_SURVIVAL` behavior claim. The Forge delayed-login emergency fixture
already covers the runtime path; this contract prevents a future lifecycle
refactor from removing/relogging an actively defending body. The full JDK25
`test` suite passes, Python E2E remains 59/59, and the package/compatibility
command (`check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`)
passes all 16 actionable tasks. The development JAR is unchanged at
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`,
17,150,533 bytes.

The latest preflight at 2026-08-11T13:51:51Z still has no Linux/Xvfb worker or
authorized `MCAI_BASE_URL`/`MCAI_MODEL`/credential, so real Actor/Observer/model,
rendered-client, hidden-seed Hardcore, soak, and all formal M0--M4 gates
remain `NOT_RUN`.

The production zero-human Forge 65.1.1 selector was also rerun with
`-Pzero_human_autospawn_test=true`: `zero_human_dedicated_server_chunk_and_respawn`
passed 1/1 in 1.636 seconds. It covers server startup with no human client,
remote-player chunk/entity activity, death and respawn; it does not establish
rendered-client visibility or model autonomy.

## 2026-08-11 continuation: emergency first-login recheck and soft-deadline wiring

The current Forge 65.1.1/JDK25 source was rerun in a fresh GameTest world with
`mcai_companion:delayed_human_login_while_emergency_active` (1/1). The
dedicated-server fixture starts with no human, lets the real headless
`ServerPlayer` become active and receive survival damage, then logs the first
human in through `PlayerList`. The initial anchor is deferred while the
emergency lane owns the body and is retried only after the threat clears via
the normal remove/relogin path. This is a no-model server-side lifecycle and
survival result, not a client, live-model, PVP or Hardcore pass.

The full JDK25 JVM suite passes after adding a source contract that keeps the
configured `MODEL_SOFT_TIMEOUT_SECONDS` value (with the existing hard-timeout
clamp) wired into both the Brain planner and conversation coordinator. Python
E2E remains 59/59. No provider request was made. The current development JAR
is unchanged at `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`,
17,150,533 bytes; source is `DIRTY_NO_COMMIT` and artifact status is
`NON_RELEASE`.

Real configured-model Actor/Observer, rendered-client, Linux/Xvfb, Hardcore
hidden-seed, and all formal M0--M4 gates remain `NOT_RUN` on this Darwin host.

The latest formal preflight at 2026-08-11T13:38:46Z still reports the exact
missing resources `linux_host`, `Xvfb`, `MCAI_BASE_URL`, `MCAI_MODEL`, and
`MCAI_API_KEY_or_MCAI_API_KEY_FILE`; it exited before any provider request.

## 2026-08-11 continuation: Forge 65.1.1 matrix coverage

The checked-in Forge 65 lock already listed 65.1.1 as the current Latest, but
both reusable real-client workflows stopped at 65.1.0. The matrix and manual
choice now include all 12 published Forge 65.x patches through 65.1.1. This
only prevents a patch from being skipped; the full real-client/model matrix
has not run on this host and remains `NOT_RUN`.

The same workflow now scopes `MCAI_API_KEY` to only the validation, preflight,
and functional-run steps; checkout, build, evidence summary, and artifact
upload steps do not receive the secret. A static workflow regression covers
this boundary.

## 2026-08-11 continuation: provider-neutral worker fail-closed contract

The worker protocol now refuses to execute a job under a scenario label it
does not implement. The current worker accepts only the one-case
`real_client_chat_follow_inventory` slice; M1/M2/M4 statistical labels are
archived as `NOT_RUN` until their dedicated real runners exist. A worker
`PASS` also requires the exact source/Forge/Minecraft/JAR/model binding, a
passing `e2e-verdict.json` with both ordered model-to-action traces, an
independent Oracle PASS, and all three exact product-copy hashes. This closes
the risk that a self-consistent artifact hash or a ready preflight could be
mistaken for real gameplay evidence. Python E2E is 59/59 and the change does
not promote any formal gate; Linux/Xvfb and authorized model execution remain
`NOT_RUN` on the current Darwin host.

The subsequent formal `e2eChat` preflight at Forge 65.1.1 (nonce
`worker-contract-20260811`) also returned `NOT_RUN` with the five missing
resource labels archived in its gate record; no client or provider request was
made.

## 2026-08-11 continuation: formal preflight evidence detail

The formal `e2eChat` entry point was executed again at Forge 65.1.1. It
correctly produced `NOT_RUN` on this Darwin host, and now archives each
missing prerequisite (`linux_host`, `Xvfb`, `MCAI_BASE_URL`, `MCAI_MODEL`, and
the API-key source) in the gate record instead of only a generic reason.
Python E2E is 53/53. This is an auditability fix, not a gameplay or model
pass; all real-client/model and M0--M4 gates remain `NOT_RUN`.

## 2026-08-11 continuation: delayed first-human real-client anchor harness

Added the separate `anchor-smoke` scenario to the test-only E2E harness. It
starts an exact-JAR dedicated server with zero human clients, waits for the
production headless body to be observed `ACTIVE`, holds the server human-free
for at least 40 server ticks, and then launches real offscreen Actor and
Observer clients. The Oracle only changes the later vanilla respawn point and
records read-only samples; it does not teleport a production player after the
scenario begins. The verifier requires the ordered lifecycle events, exact
product copies, safe same-dimension placement within 12 blocks, and both
clients' own rendered anchor observation. Chat is explicitly prohibited in
this scenario.

Python E2E tests now pass 52/52, and JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar` passes (16 actionable
tasks). This is lifecycle evidence only. The host is Darwin without Linux/Xvfb
or an authorized model environment, so the real model causal chain, rendered
client gate, Hardcore trials, and all M0--M4 formal gates remain `NOT_RUN`.

With the repository's correct `-Pzero_human_autospawn_test=true` property, the
Forge 65.1.1/JDK25 production GameTests
`delayed_human_login_after_zero_human_active` (1/1, 2.370 s) and
`delayed_human_login_while_emergency_active` (1/1, 4.305 s) also pass. The
emergency run is archived at `/tmp/mcai-delayed-emergency-current-20260811.log`.

An actual macOS `anchor-smoke` invocation then reached the real AI login and
`delayed_anchor_zero_human_active` event, but correctly stopped before client
launch because Xvfb is unavailable. The fixed exception path recorded that run
as `NOT_RUN` (server exit 0) in
`e2e/results/no-commit-dirty/20260811T123924Z-e2e014c4ed981e2`; it is not a
partial gameplay pass.

The final revalidation in this continuation passes JDK25 Gradle
`check verifyReleaseJar e2eClientJar e2eOracleJar` (16 actionable tasks),
Python E2E 52/52, `py_compile`, JSON parsing, and `git diff --check`. The
development JAR remains
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` with SHA-256
`63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3` and
17,150,533 bytes.

The official Forge 26.2 index was rechecked on 2026-08-11: 65.1.1 is the
latest release and 65.1.0 is recommended. Discovery reports no missing or
stale published 65.x patch, but the real-client compatibility matrix is still
not complete and remains `NOT_RUN`.

## 2026-08-11 continuation: post-fix package and offline-suite revalidation

After the initial-anchor/follow fixture race fix, the Forge 65.1.1 fresh
`real_player_chat_to_immediate_bound_follow` run passed 1/1 in 13.00 s
(`/tmp/mcai-follow-final3-ZGbHHg/logs/latest.log`). The subsequent JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar` passed (16 actionable tasks);
Python E2E passed 49/49, compatibility validation passed, and JSON/diff checks
passed. The current development artifact is
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, 17,150,533 bytes,
SHA-256 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`.
This revalidation does not promote the no-model physical slices to live-model,
rendered-client, Hardcore, or M0--M4 evidence; those gates remain `NOT_RUN`.
The latest functional preflight for Forge 65.1.1 is still `ready=false` on
Darwin: isolated Xvfb/Linux and `MCAI_BASE_URL`, `MCAI_MODEL`, plus an
authorized credential are absent. No provider request was made.

## 2026-08-11 continuation: first-login follow race

The fresh Forge 65.1.1 immediate-follow slice first caught a legitimate
initial-anchor lifecycle gap: the body is briefly absent from `PlayerList`
while the normal remove/relogin completes, and the test treated that as a
missing AI. After relogin, the safe spawn offset is intentionally only about
1--2 blocks from the human, so the follow controller correctly stayed still;
the old fixture's requirement that the body move two blocks before the human
started walking deadlocked the test.

The release-excluded fixture now waits for the replacement `ServerPlayer` and
places the test human at a legal six-block lead before submitting chat. No
production teleport or movement authority was added. Forge 65.1.1 then passed
`real_player_chat_to_immediate_bound_follow` 1/1 in 13.00 s
(`/tmp/mcai-follow-final3-ZGbHHg/logs/latest.log`). This remains a no-model
ServerPlayer physical slice; real Actor/Observer/model causality is still
`NOT_RUN`.

## 2026-08-11 continuation: emergency-at-login anchor regression

Added the release-excluded GameTest
`delayed_human_login_while_emergency_active`. It starts the production
zero-human body, applies real `ServerPlayer.hurtServer` damage, performs the
first human login through `PlayerList.placeNewPlayer`, and asserts that the
emergency body/UUID is retained while the initial anchor is deferred. After
the controlled threat is removed, the server-tick retry performs the bounded
normal remove/relogin beside the human. Forge 65.1.1 passed 1/1 in 4.302 s
(`/tmp/mcai-delayed-emergency3-5wAJUR/logs/latest.log`). The ordinary delayed
login regression was rerun in the same source and passed 1/1 in 2.395 s
(`/tmp/mcai-delayed-normal4-nEFXtP/logs/latest.log`).

The first attempt of the new fixture failed because the test body still had
an invulnerable ability; the fixture now explicitly restores Survival and
clears that ability before applying damage. This is recorded as a harness
correction, not hidden as product evidence.

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` (16 actionable tasks),
Python E2E (49/49), compatibility and diff/JSON checks pass. The release
artifact is unchanged because these are release-excluded tests:
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, 17,150,533 bytes,
SHA-256 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`.
Live-model Actor/Observer, rendered client, Hardcore and M0--M4 formal gates
remain `NOT_RUN`.

## 2026-08-11 continuation: combat safety slices

On the current source with JDK 25 and Forge 65.1.1, fresh GameTest server runs
passed `real_emergency_iron_golem_duel` (1/1, 6.374 s; log
`/tmp/mcai-combat-final-CyZYeF/logs/latest.log`) and
`real_emergency_zombie_skeleton_horde` (1/1, 6.229 s; log
`/tmp/mcai-horde-final-rKcHzE/logs/latest.log`). They exercise the real
headless `ServerPlayer` damage/target reacquisition/vanilla attack and shield
path, but deliberately do not call a model or render a client. They therefore
do not establish PVP, live-model combat, Hardcore completion, or any M1--M4
formal gate.

The focused JDK25 `BrainOrchestratorTest` and
`MinecraftBrainEventSinkSourceContractTest` also pass. An active PLAYER_CHAT,
MCP, or Hardcore goal cannot be silently completed by a provider SAFE_IDLE
decision, and no low-level action is claimed before a skill is accepted.
Configured-model Actor/Observer evidence remains `NOT_RUN` on this Darwin host
because the required Linux/Xvfb and authorized provider environment are absent.

## 2026-08-11 continuation: first-human lifecycle and active-goal safety

The no-human server lifecycle now records whether the first body placement
had a real-player anchor. If a dedicated server has already reached `ACTIVE`
after the 40-tick unanchored admission window, the first later human login
can trigger one bounded normal remove/relogin near that player, but only while
the goal lane, skill supervisor, emergency survival controller and emergency
arbiter lane are clear. A busy body is left alone and the login anchor is
retried from the server tick; this is not a gameplay teleport. The provenance
is persisted in `goal_progress.body_spawn_anchored`, with an anchored default
for older worlds.

The new GameTest
`mcai_companion:delayed_human_login_after_zero_human_active` passed on a fresh
Forge 65.1.1 server (1/1, about 2.444 seconds). It starts with no human,
waits for the AI to become active, then performs a real `PlayerList` login and
checks stable UUID, unchanged idle goal revision and inventory, same dimension,
safe distance, two-player list and disclosed `[AI]` TAB name. This remains a
no-model server-side lifecycle gate, not a real-client/model gate.

The planner also now refuses to treat a provider `SAFE_IDLE` response as a
successful stop for PLAYER_CHAT, MCP, or locked Hardcore goals. It records a
safe-idle rejection, suppresses speech that no skill accepted, and asks for a
bounded actionable correction (ordinary chat eventually waits for a new player
message). Recovery-only goals retain their explicit safe-idle behavior. JDK25
Brain/world/embodiment tests pass.

The current development artifact is
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, 17,150,533 bytes,
SHA-256 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`.
Full Gradle checks (16 actionable tasks), Python E2E (49/49), compatibility
and diff checks pass. Real configured-model Actor/Observer, rendered client,
Hardcore hidden-seed and M0--M4 formal gates remain `NOT_RUN`; source is
`DIRTY_NO_COMMIT` and release status is `NON_RELEASE`.

## 2026-08-11 continuation: model-owned follow and dedicated Actor authorization

The production brain previously started a player-bound follow skill directly
even after a verified model delegate was installed. That shortcut was useful
for no-model physical GameTests, but it could never produce the required live
causal chain from a real chat through a model `START_SKILL` decision. The
`ModelGateway` contract now exposes an explicit `configured()` readiness bit;
the switchable runtime gateway and JDK provider report it, and
`BrainOrchestrator` keeps the narrow direct follow path only as an unconfigured
offline fallback. A configured provider must receive and authorize the follow
decision before the ServerPlayer skill starts. The focused BrainOrchestrator
tests pass.

The dedicated real-client harness also now writes the deterministic vanilla
offline UUID for its non-OP `MCAIActor` into the isolated server's
`config/mcai-companion.toml` `chat.allowedSenders` list. It does not grant OP or
alter production defaults. Python orchestrator tests pass (14/14), including
the UUID/configuration assertion.

This closes two deterministic preconditions for the real vertical slice; it is
not live-model evidence. The focused Brain test, full JDK25 `check
verifyReleaseJar e2eClientJar e2eOracleJar` (16 tasks), and Python E2E suite
(49/49) pass. The current development JAR is 17,148,068 bytes with SHA-256
`cd439586671c56f0cf6b03f59ccc5fe429060d7a76d034cf198970e125576835`.
The current machine still lacks Linux/Xvfb and an authorized model
environment, so real Actor/Observer, rendered screenshots, Hardcore and M0--M4
gates remain `NOT_RUN` and the dirty development artifact remains
`NON_RELEASE`.

## 2026-08-11 continuation: Forge 65.1.1 compatibility evidence

The official Forge 26.2 index now reports 65.1.1 as the latest published
Forge 65 patch (65.1.0 remains the recommended MDK). The compatibility
declaration was updated without widening the product line: the runtime range
is still `[65.0.0,66.0.0)`, while the formal patch matrix remains explicitly
unverified.

Fresh Forge 65.1.1/JDK25 runs now pass the following real server-side
selectors, each in a clean working directory:

- `zero_human_dedicated_server_chunk_and_respawn`: 1/1 in 1.708 s;
- `real_player_chat_to_immediate_bound_follow`: 1/1 in 12.93 s;
- `real_emergency_zombie_skeleton_horde`: 1/1 in 6.273 s;
- `auto_presence_on_human_login`: 1/1 in 2.754 s;
- `real_parkour_course`: 1/1 in 26.75 s.
- `real_furnace_batch`: 1/1 in 10.27 s.

Logs are retained under `/tmp/mcai-forge6511-{lifecycle,follow,horde,
presence-20260811b,parkour-20260811,furnace-20260811}/logs/latest.log`. These are no-model
Forge `ServerPlayer` lifecycle, chat/physical-follow, contact-defense and
movement slices. They do not establish a live model decision, rendered
Actor/Observer client, PVP policy, Hardcore completion rate, or any M0--M4
formal gate. The lock now records 65.1.1 as compile- and lifecycle-smoke
verified, while `formalMatrixUnverifiedPatches` still includes every patch.

After synchronizing the offline discovery fixture for the newly observed
patch, the JDK25 Gradle checks (`check verifyReleaseJar e2eClientJar
e2eOracleJar`) pass, Python E2E is 48/48, and the deterministic mutation gate
catches 10/10 variants. The current development JAR remains
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, 17,147,953 bytes,
SHA-256 `5b793d0bdf6c486ddb5d3a894bbb25f152a0623c054c136f6a105a47f1273e68`.
These are source/build and no-model physical regressions only; release status
is still `NON_RELEASE`.

## 2026-08-11 continuation: ordinary chat task permission boundary

The ordinary player-chat path was passing `mayAdmin(source)` into the
conversation coordinator. On a dedicated server that is operator-only, so a
non-operator could receive an acknowledgement while the task was rejected
before `GoalCoordinator` and the planner ever saw it. This is a production
cause of the reported “说了但不动” symptom, independent of model quality.

`CompanionCommandAccess.mayControlCompanion(source)` now separates gameplay
task control from administrative `/mcai` commands. Singleplayer owners and
dedicated-server gamemasters retain control; a non-operator teammate must be
explicitly opted in by UUID through `chat.allowedSenders` in the Forge config.
The allow-list is UUID-only and fails closed. Administrative/evaluation
commands still use `mayAdmin` and were not broadened. The source contract and
UUID allow-list tests pass on JDK25. This patch does not claim a live model
response or movement: the dedicated non-operator chat-to-action chain and all
formal M0--M4 gates remain `NOT_RUN`.

An addressed, obvious imperative from a player who is not allowed to control
the companion is now rejected before the model lane. The player receives a
clear localized permission message and the audit records
`conversation_task_permission_denied`; no model-generated “任务已接受” can be
emitted for a goal that the server will not install.

Fresh Forge 65.0.0 selector
`mcai_companion:real_player_chat_to_immediate_bound_follow` also passed 1/1
after the change (GameTest summary 12.88 s; log
`/private/tmp/mcai-permission-follow-650-20260811/logs/latest.log`). It is the
existing no-model ServerPlayer/chat/physical-follow slice, so it only proves
that the permission refactor did not break the owner/admin path; it is not
evidence for a live model or a non-operator allow-list run. The rebuilt
development JAR is 17,147,953 bytes with SHA-256
`5b793d0bdf6c486ddb5d3a894bbb25f152a0623c054c136f6a105a47f1273e68`.

After the model-preflight permission-denial branch was added, the same
selector was rerun again and passed 1/1 in 12.86 s
(`/private/tmp/mcai-permission-denial-follow-650-20260811/logs/latest.log`).
The test's owner/admin task path still installs the bound-follow goal and
emits physical follow movement; no provider request was made.

The same Forge 65.0.0 build also passed the hostile-contact regression
`mcai_companion:real_emergency_zombie_skeleton_horde` 1/1 in 6.272 s
(`/private/tmp/mcai-permission-horde-650-20260811/logs/latest.log`). This is a
no-model emergency controller/perception slice: it confirms the bounded
damage/contact path can physically react, but does not prove a model-selected
PVP policy, ten-target combat, or a survival completion rate.

## 2026-08-11 continuation: physical-contact hostile reacquisition

The previous perception path discarded a hostile that was already intersecting the
AI body when the head was facing away, because it still required the normal view
cone and visual clip. `FairPerceptionSampler` now admits only that bounded,
currently contacting threat as a separate `PHYSICAL_CONTACT` entity observation.
It does not claim visual line-of-sight; `FairPlayerActuator` re-checks the vanilla
crosshair, reach, and obstruction immediately before an attack. No hidden entity
scan, teleport, or direct world mutation was added.

Fresh Forge 65.0.0 selector
`mcai_companion:real_emergency_contact_reacquisition` passed 1/1 in about
758.1 ms (`/tmp/mcai-contact-recheck-20260811-d/logs/latest.log`). The new
source-contract test plus the focused semantic-observation and sampler tests all
pass on JDK25. The same selector passed 1/1 on Forge 65.1.0 in about 794.0 ms
(`/tmp/mcai-contact-recheck-20260811-651/logs/latest.log`). The
current development JAR is 17,146,697 bytes with SHA-256
`97bf0d11a27b21e8748c905232a8cf72708574dd1dcf8f8914f40082d35ec32a`.
`check verifyReleaseJar e2eClientJar e2eOracleJar`, Python E2E 48/48, mutation
10/10, and JSON/diff checks pass. This is a no-model physical/fair-perception slice; real model,
Actor/Observer, rendered client, Hardcore hidden-seed, soak, and M0--M4 gates
remain `NOT_RUN`, with source `DIRTY_NO_COMMIT` and artifact `NON_RELEASE`.

## 2026-08-11 continuation: Forge 65.0.0 chat-bound follow physical recheck

- Fresh Forge 65.0.0 GameTest selector
  `mcai_companion:real_player_chat_to_immediate_bound_follow` passed 1/1 in about 1.276 s.
  The log is `/tmp/mcai-follow-recheck-20260811/logs/latest.log`; it records a test-player chat,
  the AI acknowledgement, the follow-status message, and the real `ServerPlayer` bound-follow slice.
- This is a no-model physical regression slice for the Forge chat/task/body chain. The deliberate
  GameTest disconnect emits a closed-channel packet warning after assertions; it does not change the
  pass result and is not rendered-client or companion-quality evidence.
- Product JAR SHA-256 remains
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`. Real model, Actor/Observer,
  Hardcore hidden-seed, M0--M4, and soak gates remain `NOT_RUN`; source is `DIRTY_NO_COMMIT` and the
  artifact is `NON_RELEASE`.

## 2026-08-11 continuation: real-client system-chat evidence fix

- Fixed a null-sender crash in the test-only real client: system chat can have no
  sender UUID, so evidence writing now records an empty sender and continues.
- Added `E2eClientSourceContractTest` to keep that boundary explicit.
- JDK25 `test e2eClientJar` passed. Product JAR remains
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`; e2e
  client JAR is `03bdb9ddb59f89341dfb33da86bfe2616e8be115e0ad492e8f5664f447c60613`.
- This is not a real-model or rendered-client run. Formal M0--M4 gates remain
  `NOT_RUN` on this Darwin host without Linux/Xvfb and authorized model
  credentials.

## 当前继续状态（2026-08-11，当前生产 JAR 专服 smoke 复核）

精确生产 JAR 在 Forge 65.1.0/JDK25 专服 smoke 中通过启动、精确哈希、三份安装副本一致、
AI ServerPlayer 加入、SQLite Jar-in-Jar、运行时数据库与正常退出检查，服务端退出码为 0。
对应运行目录为 `e2e/results/no-commit-dirty/20260811T093116Z-e2e385185ed6ad6`，产品与加载
产品 SHA-256 均为
`2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`。Oracle 因服务在生命周期
检查后停止而记录 `server_stopped_before_result`，因此这不是模型聊天、移动或动作门禁；
`functionalAiClaim=false`。真实模型、Linux/Xvfb Actor/Observer、Hardcore 随机种子、soak 与
M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`，工件 `NON_RELEASE`。

## 当前继续状态（2026-08-11，20 TPS 观察热路径性能修复）

修复长生命周期性能回归：20 Hz 身体失效检查不再复制完整背包/菜单/危险列表，只比较原本参与
失效判断的标量字段，完整语义列表仍按 4 Hz 采样生成。Forge 65.1.0 长跑 5,870 samples，平均
0.290 ms、p95 1.293 ms；Forge 65.0.0 下限 6,087 samples，平均 0.344 ms、p95 1.260 ms，
两项均 1/1 通过。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks、Python E2E 48/48、mutation
10/10、JSON/diff 校验通过。当前 JAR SHA-256：
`2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`（17,146,468 bytes）。正式
真实模型、Linux/Xvfb Actor/Observer、Hardcore 随机种子和 M0--M4 门禁仍全部 `NOT_RUN`；源码
`DIRTY_NO_COMMIT`，工件 `NON_RELEASE`。

## 当前继续状态（2026-08-11，普通世界身体失败自愈）

修复普通无目标世界的身体生命周期缺口：初次 `ServerPlayer` 出生/区块放置瞬态失败时，运行时
现在只对 `SessionState.FAILED` 做独立限频重试（接受后 20 tick、拒绝后 200 tick），不触碰
用户主动移除的 `ABSENT`，Hardcore 死亡也不会重生。新增源码契约测试；新鲜 Forge 65.1.0
无人服务器区块/死亡/重生 1/1、真人聊天绑定跟随 1/1 通过；Forge 65.0.0 下限同两项也各 1/1，
均无模型请求。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks、Python E2E 48/48、mutation
10/10、JSON/diff 校验通过。当前 JAR SHA-256：
`31fcda6960400f8fbf2e89c7fdc77cf051e1fa5e2b496b48500d79295397e882`（17,146,462 bytes）。正式真实
模型、Linux/Xvfb Actor/Observer、Hardcore 随机种子和 M0--M4 门禁仍全部 `NOT_RUN`；源码
`DIRTY_NO_COMMIT`，工件 `NON_RELEASE`。

## 当前继续状态（2026-08-11，敌对声音来源过滤后的最终回归）

按 Forge 65.x 实际 API 修正声音监听：不调用不存在的事件取消方法，只接受存活的 `Enemy`，再
执行服务端实际距离过滤。JDK25 全量构建 16 tasks、Python E2E 48/48、mutation 10/10 和
JSON/diff 校验通过。最新 JAR SHA-256：
`9fc3f2be6773b0d5280a4ebf8b6efc2928657239853c09c8669d87232bbe9636`；正式模型和 M0--M4 仍
`NOT_RUN`。

## 当前继续状态（2026-08-11，听觉线索距离边界修复）

服务端声音事件现在先检查敌对实体与 AI 身体的真实距离，再写入有界听觉线索；超过音量推导的
4--16 格范围会被丢弃，避免服务端事件被误当成客户端听觉或实体雷达。定向测试、JDK25 全量
构建（16 tasks）、Python E2E 48/48 和 mutation 10/10 均通过。当前生产 JAR SHA-256：
`7f86ca25d78b1db6bd257d990fdb8c4a392aba94beedd2267d20e7f1e60a19ff`。正式真实模型与 M0--M4
门禁仍为 `NOT_RUN`。

## 当前继续状态（2026-08-11，公平听觉威胁线索）

为避免敌对声音发生在两次语义采样之间时身体只停留观察，服务端现在通过 Forge
`PlayLevelSoundEvent.AtEntity` 接收近身 `Enemy` 声音，并向公平感知层写入短时、无身份的威胁
线索（20 tick、距离上界 4--16、方向和威胁等级）。它只触发语义刷新；本地生存安全层仍拥有
第一响应，模型不能取得实体 ID 或精确坐标。新增音频来源契约和方向测试。

JDK25 定向测试、完整 `check verifyReleaseJar e2eClientJar e2eOracleJar`（16 tasks）、Python
E2E 48/48、mutation 10/10、JSON/diff 校验均通过。产品 JAR SHA-256 为
`08edbc2073f8307739af84d9ee1315668d4a9513cd3deb596b7f7a2d897b89d4`。这些仍不是真实模型或
陪玩门禁证据；第一人称截图保持 fail-closed，正式 M0--M4、真实 Actor/Observer 与 Hardcore
统计门禁继续 `NOT_RUN`，工件仍 `NON_RELEASE`。

## 当前继续状态（2026-08-11，MiMo 请求契约与截图能力边界）

MCP `get_screenshot` 现在明确报告 headless ServerPlayer 尚无经过认证的第一人称捕获：
`modelInput=false`、`observerCameraAllowed=false`、`requiresAuthenticatedClientCapture=true`。
这避免把外部 Observer 渲染证据误当成模型视觉输入。新增离线 MiMo Token Plan 请求契约测试，
锁定 `mimo-v2.5` 的 Chat `max_completion_tokens`/`thinking.type=disabled`/严格 JSON Schema
以及 Responses `store=false`/`max_output_tokens`/`reasoning.effort=none`；不会读取或发送密钥。
JDK25 定向测试通过。真实客户端捕获、真实模型、Actor/Observer、M0--M4 与 soak 仍为
`NOT_RUN`，源码 `DIRTY_NO_COMMIT`、工件 `NON_RELEASE`。

随后固定 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过（16 actionable tasks），
Python E2E 48/48、mutation gate 10/10、JSON 与 `git diff --check` 通过。当前产品 JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` 的 SHA-256 为
`1d9c9ea68f456795b9d34d6dc07201bde427706402927ab3823220f8c0e872a6`（17,143,136 bytes）。
该构建仍没有供应商请求、真实客户端或正式 M0--M4 结果。

## 当前继续状态（2026-08-11，Forge 65.x 真实客户端矩阵入口）

`.github/workflows/real-client-functional-e2e.yml` 现在同时支持手动运行和可复用调用；新增的
`.github/workflows/real-client-functional-e2e-matrix.yml` 会在明确触发后对 65.0.0--65.0.9、
65.1.0 各启动一个隔离 Linux/Xvfb + 真实模型 Actor/Observer 任务。它只提供可审计的运行入口，
没有自动触发或本机密钥使用，完整 Forge 矩阵和 `forge65PatchMatrix` 仍为 `NOT_RUN`。

## 当前继续状态（2026-08-11，Observer 渲染证据防假通过）

发现验证器此前只检查 Observer 的登录、TAB 和坐标样本，仍可能把没有可审阅渲染产物的
客户端运行误认为“rendered Observer”。现在测试客户端在自身样本首次看到 AI 后，只捕获一帧
第一人称 PNG 到隔离运行目录；`e2e/orchestrator.py` 同时校验 PNG 签名、大小、尺寸、保存事件、
运行 nonce 和有序客户端生命周期。截图只用于证据审阅，不上传模型，也不替代皮肤、HUD、动画
和 UI 的人工视觉验收。JDK25 `e2eClientJar` 编译成功，Python E2E 48/48 通过；真实 Linux/Xvfb
客户端和模型尚未运行，正式 Render/UI、M0--M4 继续 `NOT_RUN`。

本轮 JDK25 全量复核 `check verifyReleaseJar e2eClientJar e2eOracleJar` 为 16 tasks 成功，Python
E2E 48/48、mutation gate 10/10、JSON 与 `git diff --check` 通过。生产 JAR 未因测试客户端改动
变化：`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`，17,143,018 bytes；当前
preflight 仍因 Darwin/无 Xvfb/无模型配置退出 2，未启动真实客户端或供应商请求。

## 当前继续状态（2026-08-11，真实客户端生命周期证据门禁）

发现并修复了 E2E 验证器的一个假通过路径：Observer 只有位置/TAB样本而没有真实登录事件时，
旧验证器仍可能通过。现在 `e2e/orchestrator.py` 要求 Actor/Observer 的加载、连接、登录、准备
标记和有序事件链；Actor 还必须按真实顺序发出两条聊天并观察到跟随到达。合成夹具已同步更新，
并新增删除 Observer 登录事件的失败测试。

Python 定向测试 11/11、完整 E2E 测试 46/46、突变门禁 10/10 全部通过。这里仍是验证器和
测试基础设施证据，不是真实模型或渲染客户端运行；源码 `DIRTY_NO_COMMIT`、工件 `NON_RELEASE`，
正式 M0--M4 与实时模型/Actor/Observer 门禁继续 `NOT_RUN`。

同一门禁随后补上了运行 nonce 绑定：Oracle、Actor、Observer 和最终结果必须属于同一运行，
混合不同运行事件会失败。Orchestrator 12/12、完整 Python E2E 47/47、突变门禁 10/10 通过；
仍未运行真实模型或 Linux/Xvfb 客户端。

真实客户端工作流的 Forge 运行时选择也扩展为当前已发布的全部 65.x 补丁（65.0.0--65.0.9、
65.1.0），但每次手动触发只执行一个补丁；完整 65.x 真实模型矩阵仍需独立逐补丁证据，不能由
单次运行推断。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks 随后通过，生产 JAR 保持
SHA-256 `71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`（17,143,018 bytes）。
这不是模型或 M0--M4 证据；工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

## 当前继续状态（2026-08-11，聊天模型失败可见性与 Forge 65 边界）

会话通道不再把供应商/网络问题报告成“没听清”。传输失败、超时、限流、服务端暂时失败、无效响应和缺失结果现在分别给出可行动的重试或设置检查提示，并明确“这条消息尚未执行”；重试只在同一条消息上有界进行，不能产生目标或动作承诺。新增 `ConversationFailureMessageTest` 覆盖中英文和失败类别。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks、Python E2E 45/45、JSON/兼容/diff 校验通过。产品 JAR SHA-256 为 `71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`（17,143,018 bytes），源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。Forge 65.0.0 与 65.1.0 新鲜专服均通过 `mcai_companion:real_player_chat_to_immediate_bound_follow` 1/1（无模型、真实 ServerPlayer/chat/目标绑定/跟随切片）。这仍不是实时模型、真人 Actor/Observer、PVP、Hardcore 随机种子或 M0--M4 验收；正式门禁继续 `NOT_RUN`。

## 当前继续状态（2026-08-11，模型失败状态可见性）

为修复“模型没有动作但玩家只看到 AI 停住”的可观测性缺口，
`MinecraftBrainEventSink` 现在会在普通世界对模型超时、传输/暂时失败或供应商退避发出一次
明确状态，说明“当前还没有执行动作”；连续失败进入安全暂停时再发一次终止状态。状态按目标
revision 去重，不使用模型文本、不声称技能已运行，极限评测保持静默。JDK25 定向测试（包括
`MinecraftBrainEventSinkSourceContractTest`、`BrainOrchestratorTest`、`ModelBootstrapCoordinatorTest`）
通过；Forge 65.1.0 `real_dispenser_button_activation` 新鲜服务器 1/1（595.7 ms）通过。

本次完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 个任务、Python E2E 45/45、JSON、
兼容性和 diff 校验均通过。当前产品 JAR SHA-256 为
`7de533f4ca1e9db425e97a2938e710f6c27906052e4277eb62b3e6453761ccde`（17,141,866 字节），仍是
`DIRTY_NO_COMMIT`/`NON_RELEASE`。真实模型、真人 Actor/Observer、Hardcore 随机种子和正式
M0--M4 仍为 `NOT_RUN`。

## 当前继续状态（2026-08-11）

本轮未把无模型物理切片冒充正式 AI 验收。当前源码在 Forge 65.1.0 新鲜服务器中通过
`real_end_victory_and_return` 1/1（约 6.947 秒），实际记录末地进入、末影龙击杀、`Free the End`
和返回；跨平台凭据/启动恢复定向 JVM 测试 29/29 通过。JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar`、Python E2E 45/45、兼容性/JSON/diff 校验
均通过。当前产品 JAR SHA-256 为
`f38cd15e63482a7d235a98cee82f1d83342983336153aca1c749f8fa35912b04`（17,140,455 字节）。

同一源码还在三个独立 Forge 65.1.0 服务器中通过 `real_brewing_stand_batch`、
`real_ender_chest_transaction`、`real_shulker_box_transaction` 各 1/1，补齐了酿造台、
末影箱和潜影盒的原版菜单/槽位事务组件证据；它们没有模型请求或直接物品写入，不能替代
真实模型工作站决策和 M3 场景矩阵。

随后 `real_cartography_table_transaction`、`real_stonecutter_transaction`、
`real_barrel_transaction`、`real_hopper_transaction` 也在独立 Forge 65.1.0 服务器各通过
1/1，扩展了制图台、切石机、木桶与漏斗的可达面和菜单转移组件证据；正式工作站矩阵仍未完成。

当前源码还通过 `real_dispenser_transaction` 与 `real_dispenser_button_activation` 各 1/1，
核验发射器菜单、按钮供能、箭矢消耗和原版投射物；这仍是红石组件回归，不是模型选择或 M3
完整自动化场景。

正式 M0--M4、真实模型/Actor/Observer、双客户端渲染、Hardcore 随机种子统计和 24/100 小时
soak 仍为 `NOT_RUN`；源码为 `DIRTY_NO_COMMIT`，工件为 `NON_RELEASE`。当前未读取或发送任何
真实 API Key。

## 最新规划器承诺防线（2026-08-11）

规划器不再把“目标已接受、任务已创建、开始执行、正在前往”等状态话术误当成真实提问；这类
`ASK_PLAYER` 承诺会清除等待玩家状态并触发有界重规划，技能仍必须由模型返回有效
`START_SKILL`，服务端不会自行发明技能或修改世界。聊天层对 `REPLAN` 承诺也会明确报告“未创建
任务”。`BrainOrchestratorTest`、`PlayerTaskIntentTest` 定向测试及 JDK25 完整打包均通过。
当前产品 JAR SHA-256：`c2b51def6ac70c003e0166e1e829ca7070ef70f475ffb569a2585edc946bdf4d`
（17,138,918 bytes）。这不是实时模型/客户端或 M0--M4 验收。

## 最新启动验证聊天队列修复（2026-08-11）

模型能力探测进行时，普通聊天不再被静默丢弃：runtime 将真实
`ModelRuntime.snapshot().probeInFlight()` 传入会话协调器；消息最多排队 32 条、最多 600 ticks，
并显示“模型正在验证，消息已排队”。认证失败、缺少端点和配置替换不会缓存命令；探测在排队
期间失败会在下一 tick 清空启动队列并逐条明确报告未执行，超时消息也明确报告未执行。重试消息
不会被误判为启动等待消息。

定向 JVM 测试、JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`、Python E2E 45/45、
JSON/兼容校验和 Forge 65.1.0 `auto_presence_on_human_login` 1/1 均通过。当前产品 JAR
SHA-256：`c2b51def6ac70c003e0166e1e829ca7070ef70f475ffb569a2585edc946bdf4d`（17,138,918 bytes）。
这是启动可靠性修复，不是实时模型/客户端或 M0--M4 验收；正式门禁继续 `NOT_RUN`。

## 最新 Forge 65.x 物理子集兼容矩阵（2026-08-11）

当前源代码以已注册的完整 selector 在每个官方已发布 patch 的独立服务端工作目录中运行了四项
物理回归：`real_parkour_course`、`real_furnace_batch`、`auto_presence_on_human_login`、
`real_emergency_zombie_skeleton_horde`。Forge 65.0.0--65.0.9 与 65.1.0 共 11 个 patch，
共 44 次运行，结果 44/44（每次 1/1；日志均含 `All 1 required tests passed`）。完整逐行摘要为
`/tmp/mcai-compat-65x-python-correct-summary.txt`，日志保留在
`/tmp/mcai-compat-correct-65.*.log`，隔离目录也记录在摘要中。

这不是完整 patch matrix，也不是实时模型、客户端渲染、PVP、Hardcore 随机种子或 M0--M4 证据；
上述正式门禁继续为 `NOT_RUN`。此前裸 selector/`timeout` 失败只属于测试编排错误，已与本次结果
分开保留。源码仍 `DIRTY_NO_COMMIT`，产物仍 `NON_RELEASE`。

JSON 状态验证、`scripts/validate-compat.py`、JDK25 `./gradlew build --no-daemon`（16 tasks）
和 Python E2E `45/45` 均通过。唯一产品 JAR 为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为
`1c8ea9740091e3385f53c61adf9e7b3ffcd4e44d15edb5389e65320d313cba7d`（17,136,675 bytes）；
它仍是 dirty-tree、`NON_RELEASE` 开发产物。

## 最新 Forge 主版本发现门禁（2026-08-11）

官方 Forge 26.2 页面与 `promotions_slim.json` 于 2026-08-10 19:08:36 UTC
复核仍为 65.1.0 Latest/Recommended，已发布 65.0.0--65.0.9 与 65.1.0，未发布 Forge 66。
新增 `scripts/discover-forge-lines.py`、Gradle `forge-major-discovery` 和 5 个夹具测试；
发现官方新主版本或新 patch 而仓库没有对应适配/锁定时会 fail-closed。发现任务和 Python
E2E 44/44 通过，
但这不等于 patch 矩阵、模型客户端或 M0--M4 通过；源码仍 `DIRTY_NO_COMMIT`、产物仍
`NON_RELEASE`，正式 AI 门禁仍 `NOT_RUN`。

## 最新状态证据审计（2026-08-11）

`docs/progress/GOAL_STATE.json` 已完成重复键审计：当前 520 个 developmentEvidence 键、无重复；旧版同名记录改为
带时间戳的历史键。正式 `M0--M4`、模型/客户端、soak 与隐藏种子门禁仍保持 `NOT_RUN`，
源码仍 `DIRTY_NO_COMMIT`，产物仍 `NON_RELEASE`。

## 最新跑酷物理回归（2026-08-11）

`real_parkour_course` 在 Forge 65.1.0/65.0.0 新鲜工作目录各通过 1/1（2.426 s、2.478 s）。
这是 headless `ServerPlayer` 的真实跳跃/平台物理和离场回归，未使用模型、传送或隐藏世界
数据；因此只是本地运动执行器下限，不能替代模型自主跑酷、客户端观感或正式 movement/M1--M4
门禁。源码仍 `DIRTY_NO_COMMIT`，产物仍 `NON_RELEASE`。

## 最新 live-model 选择器审计（2026-08-11）

`real_zero_human_dedicated_server_foundation` 实际是 live-model 场景却曾可在无开关时走
`helper.succeed()`；`build.gradle` 已将其加入 fail-closed 选择器。Forge 65.1.0 无开关定向
运行在配置阶段 exit 1、未启动游戏/请求供应商；该旧旁路结果不计证据，正式 M0--M4 仍
`NOT_RUN`。

## 最新选择器防线后完整构建（2026-08-11）

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 在防线改动后重新通过 16 个任务。
产品 JAR SHA-256 仍为
`7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`（17,135,452 bytes）；
这是非发布 dirty-tree 工件，不能替代真实模型/客户端或 M0--M4 门禁。

当前精确 JAR 的 Forge 65.1.0 dedicated-server smoke 为 `PASS`（ServerPlayer 生命周期、
Jar-in-Jar SQLite、精确 SHA、正常退出）；其 oracle 为主动停止前未完成结果，且
`functionalAiClaim=false`，不计为聊天、移动或模型玩法通过。

## 最新无真人专服模拟回归（2026-08-11）

`zero_human_dedicated_server_chunk_and_respawn` 在 Forge 65.1.0/65.0.0 各通过 1/1
（1.692 s、1.775 s）：无真人玩家时 AI 自己持有 `ServerPlayer` 模拟票据，远端区块更新与
原版非 Hardcore 重生均通过。该无模型身体证据不等于实时模型或正式 M0--M4 通过。

同一切片再以生产 `ServerStartedEvent` 自动 spawn 开关运行，在 Forge 65.1.0/65.0.0 各
通过 1/1（1.474 s、1.700 s）；这证明无真人服务器不依赖测试手动创建 AI 身体。

最新应急战斗切片：铁傀儡单挑 Forge 65.1.0/65.0.0 各 1/1（1.010 s、981.6 ms），
僵尸/骷髅群体各 1/1（1.154 s、1.190 s）。两者均为无模型原版身体/安全控制下限，不能
替代实时模型 PVP 或 M0--M4。

API Key/启动恢复定向 JVM 测试 29/29 通过（credential manager、ModelRuntime、
ModelBootstrapCoordinator）；未发送用户密钥，真实供应商配置仍需外部 preflight。

最新 preflight（2026-08-10T18:50:03Z）仍 `ready=false`/exit 2：Darwin 无 Linux/Xvfb，且没有
注入 `MCAI_BASE_URL`、`MCAI_MODEL`、`MCAI_API_KEY(_FILE)`。真实模型、客户端与 M0--M4 保持
`NOT_RUN`，不是功能通过。

## 最新切片后构建（2026-08-11）

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks；产品 JAR
`mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`，仍为 dirty-tree
非发布工件。Python E2E 39/39 通过，但不替代真实模型/客户端门禁。

## 最新遮挡工作台恢复回归（2026-08-11）

`occluded_iron_toolkit_table` 在 Forge 65.1.0/65.0.0 各通过 1/1（1.788 s、1.846 s）：
真实 headless 身体在工作台被方块遮挡时按第一人称可达面侧移、重新瞄准并完成铁工具菜单
链。这是无模型 M1 物理回归，不是模型自主判断或正式 M1--M4。

## 最新基础合成事务回归（2026-08-11）

`reachable_basic_crafting` 在 Forge 65.1.0/65.0.0 各通过 1/1（1.628 s、1.685 s）：
真实 headless 身体通过可达工作台、原版菜单和配方结果点击完成基础合成。这是无模型 M1
物理下限，不是自主生存或正式 M1--M4 统计。

## 最新已验证传送门往返回归（2026-08-11）

`real_verified_portal_return` 在 Forge 65.1.0/65.0.0 各通过 1/1（3.139 s、3.069 s）：
真实 headless 身体进入下界传送门并执行原版返回路线。碰撞诊断保留用于排查卡门；这是无
模型跨维度物理证据，不是实时模型寻路、随机 Hardcore 或 M0--M4 通过。

## 最新末影人即时防御回归（2026-08-11）

`real_emergency_enderman_defense` 在 Forge 65.1.0/65.0.0 各通过 1/1（1.462 s、1.637 s）：
真实 headless 身体使用原版装备/盾牌并完成本地应急位移，未站桩死亡。这是无模型生存物理
下限，不是实时模型、客户端或 PVP/M0--M4 通过。

## 最新真实功能预检（2026-08-11，外部阻断）

`python3 e2e/orchestrator.py preflight --forge-version 65.1.0` 返回 `ready=false`、exit 2：
当前是 Darwin，缺少 Linux host、Xvfb、`MCAI_BASE_URL`、`MCAI_MODEL` 和
`MCAI_API_KEY(_FILE)`。预检没有启动客户端、触碰物理显示器或发送模型请求；真实客户端、
实时模型聊天到动作及 M0--M4 仍为 `NOT_RUN`，不是功能通过。

## 最新真实 live-model 探测（2026-08-11，外部阻断）

显式 `-Plive_model_test=true` 的 Forge 65.1.0
`real_player_chat_to_surprise_zombie_defense` 确实启动专服并创建身体，但探测返回
`INVALID_CONFIGURATION: Base URL or model name is not configured`、`requestsMade=0`，进程
exit 1。它没有走 no-model skip，也没有任何聊天/战斗成功证据；当前正式模型/客户端/M0--M4
门禁保持 `NOT_RUN`，等待可验证配置与真实客户端环境。

## 最新落水自救与史莱姆防御回归（2026-08-11）

`real_water_clutch` 在 Forge 65.1.0/65.0.0 各通过 1/1（784.8 ms、693.1 ms），日志显示
`PREPARING_WATER`→`DEPLOYING_WATER`→`BRACING_FALL` 的真实水桶自救；
`real_emergency_slime_defense` 在两条版本各通过 1/1（815.3 ms、882.4 ms），并获得原版
`Monster Hunter`。这是无模型本地应急和物理下限，不是实时模型 PVP/客户端或 M0--M4 验收；
正式模型/客户端门禁仍 `NOT_RUN`。

## 最新 live-model 选择器伪通过防线（2026-08-11）

审计发现 `real_player_chat_to_surprise_zombie_defense` 在未开启
`-Plive_model_test=true` 时会走 no-model `succeed()` 旁路；该次 `All 1 required tests passed`
不计入任何战斗证据，已作废。`build.gradle` 现在对 `live_model`、`surprise_zombie_defense`
和 `critical_golden_apple` 选择器做配置期 fail-closed，缺少显式真实模型开关时直接 exit 1。
定向验证 `/tmp/mcai-zombie-chat-defense-gate-20260811-a2` 按预期拒绝且没有启动游戏或请求
供应商。这样不会把 no-model skip 误报成模型动作；正式模型/客户端/M0--M4 仍 `NOT_RUN`。
修复后的 JDK25 `./gradlew --no-daemon check` 通过；这只是构建/测试契约，不是实时模型验收。
完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 也通过 16 tasks；当前产品 JAR SHA-256
为 `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`，仍是
`NON_RELEASE`，正式模型/客户端门禁没有被升级。
精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T182412Z-20260811-selector-guard-water-slime/`
也为基础生命周期 `PASS`（退出码 0、SHA/SQLite/ServerPlayer 均匹配，`functionalAiClaim=false`）；
Oracle 的预期停服状态不构成模型聊天或移动通过。
离线 Python E2E 当前 39/39 通过，范围仍限于证据/协议/安全契约，不是实时模型或客户端门禁。

## 最新自动在场与 TAB 身份回归（2026-08-11）

`auto_presence_on_human_login` 在 Forge 65.1.0
`/tmp/mcai-auto-presence-gate-20260811-a` 与 Forge 65.0.0
`/tmp/mcai-auto-presence-gate-20260811-floor` 均通过 1/1（602.9 ms、598.7 ms）。真实
`PlayerList` 在真人登录后创建 AI `ServerPlayer`，断言同维度、12 格内、TAB 名称 `[AI] `，
并在无模型时保持 40 tick 无漂移。它是无模型服务端在场/安全待机证据，不等于客户端视觉
渲染、模型聊天到动作或专业陪玩验收；源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`，正式
M0--M4/模型客户端门禁仍 `NOT_RUN`。

## 最新真人聊天即时跟随与 AI 在场回归（2026-08-11）

`real_player_chat_to_immediate_bound_follow` 在 Forge 65.1.0
`/tmp/mcai-follow-presence-gate-20260811-a` 与 Forge 65.0.0
`/tmp/mcai-follow-presence-gate-20260811-floor` 均通过 1/1（1.300 s、1.308 s）。真实
`TestHuman`/`MCAI` 登录、服务端聊天绑定、`[AI]` 回执和实际跟随物理均有断言；这是受控
无模型 gateway 的服务端回归，不等于实时供应商模型、客户端渲染、视觉理解或专业陪玩已验收。
源码仍 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`，正式 M0--M4/模型客户端门禁仍 `NOT_RUN`。

## 最新临界金苹果双 Forge 回归（2026-08-11）

`real_offline_critical_golden_apple` 在 Forge 65.1.0
`/tmp/mcai-golden-apple-gate-20260811-a` 通过 1/1（605.1 ms），Forge 65.0.0
`/tmp/mcai-golden-apple-gate-20260811-floor` 通过 1/1（611.1 ms）。真实 headless
`ServerPlayer` 在临界生命值下由本地应急层使用自有金苹果，并以普通库存/生命/吸收结果
核验；没有模型调用或直接世界写入。这是生存下限证据，不等于实时模型对赠礼语境、PVP、
Hardcore 或 M0--M4 已验收；源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新掉落物拾取双 Forge 回归（2026-08-11）

`headless_player_lifecycle_state_and_fair_action` 在 Forge 65.1.0
`/tmp/mcai-lifecycle-item-gate-20260811-a` 通过 1/1（24.61 s），Forge 65.0.0
`/tmp/mcai-lifecycle-item-gate-20260811-floor` 通过 1/1（30.44 s）。真实 headless
`ServerPlayer` 在第一人称观察到掉落物后有限移动、通过 vanilla `ItemEntity` 拾取并以
库存增长核验，且同一链覆盖登录/重登、菜单、维度和战斗事务。它是受控 gateway 的无模型
物理/技能证据，不是实时模型或客户端陪玩验收；M0--M4/正式模型客户端门禁仍
`NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实装备回归（2026-08-11）

针对“给它东西也不穿戴”，`offline_idle_equipment` 在 Forge 65.1.0
`/tmp/mcai-equipment-gate-20260811-a` 通过 1/1（573.4 ms），Forge 65.0.0
`/tmp/mcai-equipment-gate-20260811-floor-a` 通过 1/1（630.3 ms）。真实 headless
`ServerPlayer` 经原版库存菜单装备自有铁头盔和盾牌，并获得 `Suit Up`。这是无模型、无
客户端的库存装备证据，不等于丢物拾取、模型理解或完整陪玩已验收；源码仍
`DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`，M0--M4/正式模型客户端门禁仍 `NOT_RUN`。

## 最新金苹果库存事实校正（2026-08-11）

对话层新增服务端背包事实校正：模型不能在背包数量为 0 时声称拥有金苹果，也不能在数量大于
0 时声称没有；中英文均覆盖，普通知识性描述不拦截。`ConversationGroundingTest` 4 项通过。
该逻辑只改聊天事实，不会拾取/生成/装备/食用物品或选择技能。随后 Gradle 16 tasks、Python
E2E 39/39 和精确产品 JAR 专服 smoke 均通过；SHA-256 为
`7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`，smoke 目录为
`e2e/results/no-commit-dirty/20260810T180414Z-20260811-golden-apple-grounding/`，
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新动作防线与跨平台凭据定向验证（2026-08-11）

通过 `PlayerTaskIntentTest`、`BrainOrchestratorTest`、`EmergencySurvivalControllerTest` 和
`ConversationCommitmentSourceContractTest`；未启动技能的承诺性回复不会被当成已执行动作，
服务端绑定跟随仍可绕过无效闲聊直接进入正常技能监督器。通过
`ApiKeyManagerRestartPersistenceTest`、`CrossPlatformCredentialSourceTest`、并发、设置状态、
模型档案和 `ModelRuntimeTest`；凭据来源契约覆盖 Keychain、DPAPI、Secret Service/进程凭据。
这些是 JVM/源契约证据，不是用户机器的真实密钥或供应商响应；正式模型/客户端/M0--M4 仍
`NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。随后当前源 Gradle 16 tasks、Python
E2E 39/39 以及精确产品 JAR 专服 smoke 均通过；JAR SHA-256 为
`9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`，smoke 目录为
`e2e/results/no-commit-dirty/20260810T180105Z-20260811-followup-parkour-zero-human/`，
`functionalAiClaim=false`。

## 最新真实跑酷与无真人专服回归（2026-08-11）

`real_parkour_course` 在 Forge 65.1.0 `/tmp/mcai-parkour-gate-20260811-a`（1/1，2.460 s）和
Forge 65.0.0 `/tmp/mcai-parkour-gate-20260811-floor-a`（1/1，2.574 s）通过；这是 headless
`ServerPlayer` 的原版跳跃/落地/障碍物物理证据。`zero_human_dedicated_server_chunk_and_respawn`
在 Forge 65.1.0 `/tmp/mcai-zero-human-gate-20260811-a`（1/1，1.682 s）和 Forge 65.0.0
`/tmp/mcai-zero-human-gate-20260811-floor-a`（1/1，1.744 s）通过，验证无真人在线时自动出现、
远端实体/区块 tick、死亡和重生。两者均无模型、无客户端观测，不能升级为模型陪玩或随机种子
跑酷声明；正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实应急 PVE 回归（2026-08-11，十僵尸十骷髅/铁傀儡）

针对被攻击后不还手、不撤退的反馈，重新运行了无模型真实 headless `ServerPlayer` 应急门禁。
10 个僵尸 + 10 个骷髅在 Forge 65.1.0 `/tmp/mcai-horde-gate-20260811-a`（1/1，1.105 s）和
Forge 65.0.0 `/tmp/mcai-horde-gate-20260811-floor-a`（1/1，1.282 s）通过；铁傀儡单挑在
Forge 65.1.0 `/tmp/mcai-golem-gate-20260811-a`（1/1，1.031 s）与 Forge 65.0.0
`/tmp/mcai-golem-gate-20260811-floor-a`（1/1，993.1 ms）通过。测试使用真实玩家生命周期、
原版实体伤害和应急控制器，不是模型决策或站桩脚本；收尾的 headless 断开警告不影响明确的
`All 1 required tests passed`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实木门开关回归（2026-08-11）

新增 `real_door_open_close`：真实 `ServerPlayer` 通过第一人称射线和公平空主手方块使用
路径开门、重新瞄准已打开门扇后关门。Forge 65.1.0 `/tmp/mcai-door-gate-20260811-d`
与 Forge 65.0.0 `/tmp/mcai-door-gate-20260811-floor-b` 各通过 1/1；首轮仅为测试断言类型
错误，已修正并重新运行。该证据覆盖门的原版副作用，不等于模型导航或完整陪玩验收。

门测试后 JDK25 Gradle 16 tasks、Python E2E 39/39、JSON 校验和精确产品 JAR 专服 smoke
均通过；产品 SHA-256 为 `9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`，
smoke 目录为 `e2e/results/no-commit-dirty/20260810T175120Z-20260811-door-open-close/`，
且 `functionalAiClaim=false`。

## 最新真实红石按钮/发射器交互回归（2026-08-11）

新增真实第一人称按钮门禁：通过合法 `PlayerList` 登录的 `ServerPlayer` 和观察到的射线，
由 `FairPlayerActuator` 空主手调用原版 `ServerPlayerGameMode.useItemOn`，验证按钮通电、
发射器箭消耗/箭实体生成和原版复位。Forge 65.1.0 与 65.0.0 各通过 1/1（分别为
`/tmp/mcai-dispenser-button-gate-20260811-a16`、`/tmp/mcai-dispenser-button-gate-20260811-floor-a1`）。
此前“只 dispatch 不生效”的 headless 空手副作用缺陷已修复，物品在手时仍走原版数据包路径。

JDK25 Gradle 16 tasks、Python E2E 39/39、JSON 校验和精确产品 JAR 专服 smoke 均通过；
产品 SHA-256 为 `9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`，
smoke 目录为 `e2e/results/no-commit-dirty/20260810T174258Z-20260811-redstone-button/`，
且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实红石库存菜单事务回归（2026-08-11，漏斗/发射器）

漏斗和发射器已加入独立公平菜单验证：真实方块容器由原版菜单提供，执行器从观察到的槽位
完成存入、取出和快速移动。发射器的按钮/红石触发与发射时序不是这条库存门禁的结论。
一次合并选择器运行只匹配一个测试，随后已逐项重跑；新增资源缺少 `type` 的启动错误也已按
仓库标准 `minecraft:function` 修正。

Forge 65.1.0 和 Forge 65.0.0 下限的漏斗、发射器 GameTest 均各通过 1/1；随后 JDK25
Gradle 16 tasks、Python E2E 39/39、JSON 校验通过。产品 SHA-256 为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确产品 JAR 专服
smoke `e2e/results/no-commit-dirty/20260810T172049Z-e2efe261a0229e6/` 为 `PASS` 且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实仓储菜单事务回归（2026-08-11，木桶/潜影盒/末影箱）

木桶和潜影盒已接入独立公平容器事务验证：真实容器先由方块实体提供菜单，测试从观察到的
玩家/容器槽位转移物品，验证存入、取出和快速移动；没有把容器初始化当成生产动作。
末影箱使用玩家专属的原版末影箱库存。首轮错误地尝试 `getMenuProvider` 后，已修正为真实
`BlockState.useWithoutItem` 右键入口，再由原版设置活动末影箱并创建菜单。

Forge 65.1.0 与 Forge 65.0.0 下限的木桶、潜影盒、末影箱 GameTest 均各通过 1/1；末影箱
专服目录为 `/tmp/mcai-ender-chest-menu-gate-20260811-a2` 和
`/tmp/mcai-ender-chest-menu-gate-20260811-floor`。随后 JDK25 Gradle 16 tasks、Python
E2E 39/39、JSON 校验通过；产品 SHA-256 为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确产品 JAR 专服
smoke `e2e/results/no-commit-dirty/20260810T171504Z-e2e22b70b16efa6/` 为 `PASS` 且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，石切机）

石切机已接入独立公平菜单验证：石料先经观察槽位绑定后转移到 `StonecutterMenu`，再从观察到的
`stonecutter_recipe` 选项中选择；输入消耗和结果由原版菜单决定，结果通过观察结果槽快速移动取回。
首次专服运行修正了“一次石切只消耗一个输入”的断言语义，没有修改生产执行器。

Forge 65.1.0 全新 `/tmp/mcai-stonecutter-menu-gate-20260811-a2` 与 Forge 65.0.0 下限
`/tmp/mcai-stonecutter-menu-gate-20260811-floor` 均通过 1/1。JDK25 Gradle 16 tasks、
Python E2E 39/39、JSON 校验通过；产品 SHA-256 为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确产品 JAR 专服
smoke `e2e/results/no-commit-dirty/20260810T170557Z-e2ed81e16c94b30/` 为 `PASS` 且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，制图台）

制图台已接入公平菜单事务验证：真实填充地图和纸张先经观察槽位绑定，再通过
`CartographyTableMenu` 输入槽转移；缩放结果由原版菜单产生，最后从观察到的结果槽快速移动取回。
没有直接修改地图组件、库存、方块或 NBT。

Forge 65.1.0 全新 `/tmp/mcai-cartography-menu-gate-20260811-a` 与 Forge 65.0.0 下限
`/tmp/mcai-cartography-menu-gate-20260811-floor` 均通过 1/1。完整 JDK25 Gradle 16 tasks、
Python E2E 39/39、JSON 校验通过；产品 SHA-256 为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确产品 JAR 专服
smoke `e2e/results/no-commit-dirty/20260810T170127Z-e2e9d68ee200776/` 为 `PASS`，且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，高炉与烟熏炉）

高炉和烟熏炉现已从生产 `SmeltMenuBatchSkill` 的代码支持推进到真实菜单门禁：输入与燃料先经
观察槽位绑定和普通菜单转移，等待各自原版熔炼/熏制计时，再从纯输出槽快速移动取回。没有直接
改炉子、库存或 NBT。

Forge 65.1.0 全新 `/tmp/mcai-blast-smoker-gate-20260811-a` 中两项各通过 1/1；Forge
65.0.0 下限 `/tmp/mcai-blast-furnace-gate-20260811-floor` 与
`/tmp/mcai-smoker-gate-20260811-floor` 也各通过 1/1。JDK25 Gradle 16 tasks、Python E2E
39/39、JSON/兼容校验通过；产品 SHA-256 仍为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke
`e2e/results/no-commit-dirty/20260810T165525Z-e2ee1a0065091bd/` 为 `PASS` 且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，酿造台）

酿造台已接入公平菜单事务验证：三瓶水瓶、下界疣和烈焰粉先通过观察帧绑定后转移到
`BrewingStandMenu`，等待原版酿造计时完成，再观察三个 Awkward 药水并用普通槽位
`quick_move` 取回；原版负责药水组件写入、计时、燃料和原料消耗。首次专服运行发现并
修正了缺少 test-instance 资源和瓶槽不可用 `outputOnly` 的真实语义问题，没有直接改库存
或 NBT。

Forge 65.1.0 全新 `/tmp/mcai-brewing-menu-gate-20260811-a3` 与 Forge 65.0.0
下限 `/tmp/mcai-brewing-menu-gate-20260811-floor` 均通过 1/1；这是无模型真实
ServerPlayer/vanilla menu 证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成。JDK25
Gradle 16 tasks、Python E2E 39/39、JSON/兼容校验通过；产品 SHA-256 为
`3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke
`e2e/results/no-commit-dirty/20260810T164850Z-e2e3711f4096cb2/` 为 `PASS` 且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、
产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，铁砧）

铁砧已接入公平菜单事务验证：观察并转移两个受损钻石剑输入，刷新后由原版 `AnvilMenu` 计算结果与 XP 成本，再通过观察到的结果槽普通快速移动取回；确认输入消耗与耐久合并均由原版完成。没有直接写库存、方块或 NBT。

Forge 65.1.0 全新 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，Forge 65.0.0 下限同一选择器门禁也通过 1/1；都是无模型真实 ServerPlayer 菜单/物理证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成。JDK25 Gradle 16 tasks、Python E2E 39/39、JSON/兼容校验通过；产品 SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T164010Z-e2e27edac751582/` 为 `PASS` 且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，砂轮）

砂轮已接入公平菜单事务验证：观察并转移两个受损钻石剑输入，刷新后由原版 `GrindstoneMenu` 计算修复结果，再通过观察到的输出槽普通快速移动取回；确认两个输入按原版消耗。没有直接写库存、方块或 NBT。

Forge 65.1.0 全新 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，Forge 65.0.0 下限同一选择器门禁也通过 1/1；都是无模型真实 ServerPlayer 菜单/物理证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成。JDK25 Gradle 16 tasks、Python E2E 39/39、JSON/兼容校验通过；产品 SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T163605Z-e2e7a8b9c862456/` 为 `PASS` 且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，锻造台）

锻造台已接入公平菜单事务验证：观察模板/基底/添加物三个输入槽后，用普通菜单转移放入自有下界合金升级模板、钻石剑和下界合金锭；刷新并观察原版结果槽，再用普通 `quick_move` 取回下界合金剑，确认输入物按原版消耗。没有直接写装备、容器或 NBT。

Forge 65.1.0 全新 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，Forge 65.0.0 下限在全新选择器目录 `/tmp/mcai-smithing-menu-gate-20260811-floor-focused` 同一复合门禁通过 1/1；都是无模型真实 ServerPlayer 菜单/物理证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成。JDK25 Gradle 16 tasks、Python E2E 39/39、JSON/兼容校验通过；产品 SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T163103Z-e2eb028d8f611f6/` 为 `PASS` 且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，织布机）

织布机已接入公平菜单事务验证：先观察并转移自有白色旗帜/蓝色染料，刷新后从模型可见且可用的 `loom_pattern` 选项中选择，再通过原版输出槽快速移动取回旗帜成品。没有写入组件或绕过 `LoomMenu`。

Forge 65.1.0 全新 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，Forge 65.0.0 下限同一复合门禁也通过 1/1；两者都是无模型真实 ServerPlayer 菜单/物理证据，不是实时模型、PVP 或 M3 完成。JDK25 Gradle 16 tasks、Python E2E 39/39、JSON/兼容校验通过；产品 SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`，精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T162122Z-e2ee6f18fd49fa4/` 为 `PASS` 且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新真实菜单事务回归（2026-08-11，附魔台）

已把附魔台纳入现有公平菜单链的真实事务验收：观察空菜单后，使用自有剑和青金石完成原版输入转移；刷新观察帧，仅选择已观察且可负担的附魔选项；验证原版按钮原位附魔，再通过普通观察槽位快速移动取回。先后从专服运行中修正了附魔台不自动吸入背包、无熔炉式输出槽以及 `outputOnly` 不适用于可放置输入槽三个语义问题。

全新 Forge 65.1.0 `headless_player_lifecycle_state_and_fair_action` 通过 1/1（出现 `Enchanter`，最终 `All 1 required tests passed`）；Forge 65.0.0 最低线同一复合门禁也通过 1/1。该证据是无模型的真实 ServerPlayer/vanilla menu 事务回归，不是实时模型、PVP、随机 Hardcore 或 M3 完成。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks、Python E2E 39/39、Forge 兼容校验通过；产品 JAR SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`（GameTest 类不进入发布 JAR）。精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T161619Z-e2e2fa869e7371c/` 为 `PASS`，且 `functionalAiClaim=false`。正式模型/客户端/M0--M4 门禁保持 `NOT_RUN`；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 最新跟随再捕获等待窗口与提示启动回归（2026-08-11）

已把已绑定跟随的再捕获从普通地形调查中分离：4 个水平第一人称扇区、每扇区最多 12 tick 新鲜观察；普通模型选择的 `survey_surroundings` 仍默认 40 tick。`survey_surroundings` 现在兼容三参数旧决策，并支持严格校验的可选 `observationWaitTicks=4..40`。定向 JVM 测试通过。

真实 Forge 65.1.0 `real_player_chat_to_immediate_bound_follow` 在全新工作目录 `/tmp/mcai-follow-reacquire-wait12-retry2-20260811` 通过 1/1。过程中第一次和第二次启动分别诚实捕获 planner guide 超限 `13176 > 13000` 与 `13033 > 13000`，压缩提示后才进入行为门禁；没有提高上限来掩盖回归。同一门禁在 Forge 65.0.0 最低线 `/tmp/mcai-follow-reacquire-wait12-floor-20260811` 也通过 1/1；这只是跟随切片双版本回归，不是完整 65.x 矩阵。

最终源码封装：JDK25 Gradle 16 tasks、Python E2E 39/39、Forge 65 兼容校验通过；产品 JAR SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。同一 JAR 的 Forge 65.1.0 专服 smoke `e2e/results/no-commit-dirty/20260810T155824Z-e2eafa39b81c9dd/` 为 `PASS`，`functionalAiClaim=false`。当前仍为开发源码 `DIRTY_NO_COMMIT`/产物 `NON_RELEASE`；模型、客户端 Actor/Observer、随机 Hardcore 和 M0--M4 正式门禁保持 `NOT_RUN`。

## 最新跟随再捕获定向修复（2026-08-11）

对“跟随时只转头、响应很慢”的局部停滞路径做了最小修复：已由服务端绑定的跟随目标暂时离开
当前第一人称语义帧时，专用 `survey_surroundings` 再捕获从 8 个水平采样减为 4 个扇区；普通
模型请求的地形调查仍保留较大视角预算。JVM 定向测试通过，精确 Forge 65.1.0
`real_player_chat_to_immediate_bound_follow` 在全新目录 `/tmp/mcai-follow-reacquire-20260811`
通过 1/1。该证据没有模型调用或隐藏坐标，只证明真实 ServerPlayer 的绑定跟随链。随后 JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，产品 JAR
SHA-256 为 `f00441e3f82f44da8a74c67f9c0d22ec4cf3207470a7b6107547f0a4cd24c30e`；同一精确产物的
Forge 65.1.0 专服 smoke `e2e/results/no-commit-dirty/20260810T154508Z-e2e747d9d1f6a98/`
verdict 为 `PASS` 且 `functionalAiClaim=false`。正式模型/客户端、随机 Hardcore 与 M0--M4
继续 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 最新打包与专服验证（2026-08-11，金苹果切片）

当前产品 JAR `mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`6ce64e88b35e47a9a39e428b9ead6a6643580190a96cfd0ef47bc58d7f2b39cc`。
JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39；
精确 Forge 65.1.0 专服 smoke 目录为
`e2e/results/no-commit-dirty/20260810T152951Z-e2e6135dcca3a8f/`，verdict `PASS`。
其 oracle 的停止前无结果仅表示 smoke 主动结束服务端；没有把它报告为 AI 功能通过。
正式模型/客户端/M0--M4 仍 `NOT_RUN`，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。
同一 GameTest 在 Forge 65.0.0 最低线全新目录 `/tmp/mcai-golden-offline-6500-20260811`
也通过 1/1；这只证明该应急动作切片在 65.0.0 与 65.1.0 均能运行，不是完整 patch 矩阵。

## 最新定向生产切片（2026-08-11，危急生命金苹果真实原版交互）

新增无模型但真实服务端的 `real_offline_critical_golden_apple` GameTest：真实
`ServerPlayer` 在 4 点生命、满饥饿且只拥有一枚金苹果时，生产的危急生存控制器必须先通过
原版库存菜单装备，再通过普通持物使用消耗，并获得吸收效果。精确 Forge 65.1.0 全新世界
`/tmp/mcai-golden-offline-20260811` 通过 1/1。该测试只验证本地应急动作链，不能升级为
模型聊天、PVP、随机 Hardcore 或 M0--M4 通过；正式门禁仍为 `NOT_RUN`，源码为
`DIRTY_NO_COMMIT`、产物为 `NON_RELEASE`。

## 最新定向生产切片（2026-08-11，观察式甘蔗真实物理门禁）

`plant_observed_sugarcane` 已从参数/状态机测试推进到真实 Forge 65.1.0 GameTest。首次门禁失败的
根因不是生产技能“不会做”，而是夹具把沙支撑放在空气上；原版重力在观察后的下一 tick 让沙块下落，
因此普通准星合法命中不可能发生。已补真实泥土底座，保留相邻水、沙顶面、第一人称 OUTLINE 准星、
普通 `equipMainHand`/`useOnBlock`、库存消耗和新观察确认。

清理所有临时调试输出后，`/tmp/mcai-sugarcane-clean` 的
`mcai_companion:real_plant_observed_sugarcane` 通过 1/1；定向 JDK25 技能/参数/注册/提示测试通过。
完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 为 16 tasks 成功，Python E2E 为 39/39，
JSON/兼容校验通过。唯一产品 JAR 为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256：
`6ce64e88b35e47a9a39e428b9ead6a6643580190a96cfd0ef47bc58d7f2b39cc`。

精确 65.1.0 专服 smoke：`e2e/results/no-commit-dirty/20260810T151942Z-e2e2ec651a76d7f/`，
verdict `PASS`、退出码 0、精确 JAR/SQLite Jar-in-Jar/ServerPlayer 生命周期通过；
`functionalAiClaim=false`。源码仍 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实模型/客户端、
随机 Hardcore 及 M0--M4 正式门禁继续是 `NOT_RUN`。

## 最新定向生产切片（2026-08-10，观察式甘蔗单株种植）

新增 `plant_observed_sugarcane` 技能：模型必须从同一第一人称语义观察提供支撑顶面采样，当前
帧还必须看到相邻水和自有甘蔗；技能使用普通装备与方块交互路径，只放置一株并等待新鲜观察或
库存变化确认。参数解析、注册、提示边界和状态机测试均通过。它不是甘蔗机、长线农场或 M3
验收的替代品。

JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks、Python E2E 39/39、JSON/兼容
校验均通过；产品 JAR SHA-256 为 `d6d7ae585022f63351aa65d860a0f3aeb08fde24295472e293891d084b3fd1d9`。
Forge 65.1.0 精确专服 smoke `20260810T144225Z-sugarcane-skill-contract` 为 PASS，且
`functionalAiClaim=false`。正式模型/客户端/M0--M4 门禁仍是 `NOT_RUN`，工作树
`DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。

## 最新模型行为约束（2026-08-10，危险状态禁止口头拖延）

为回应“模型说正在格挡/跟随/逃跑但身体没有动作”的直接风险，
`MinecraftPlannerInputFactory` 已把 `currentSafetyDeficits` 的处理写成明确的可信规则：
受击、着火、坠落、溺水或危急生命存在时，只要白名单有适用技能就必须选择带完整观察参数的
`START_SKILL` 并省略语音；没有适用技能才允许裸 `REPLAN` 或按评测规则 `SAFE_IDLE`，不得
在技能启动前声称已经格挡、后退、吃东西、战斗或逃跑。20 TPS 应急反射已经持有输入时，
规划器也不能把它叙述成自己的动作。对应提示契约和 `BrainOrchestratorTest` 定向测试通过。

同一源码的 JDK25 package `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，
Python E2E 39/39、JSON/兼容校验通过；产品 JAR SHA-256 为
`f5b05e1eb1a446fc92632e276c2f21e7528cc4d9b490e3d4d4ef1557afa50cf1`。这不是实时模型或正式
M0--M4 通过：真实供应商/客户端门禁仍受凭据与 Darwin 无 Linux/Xvfb 阻断，状态保持
`NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

同一精确 JAR 的 Forge 65.1.0 专服 smoke 结果为 `PASS`：
`e2e/results/no-commit-dirty/20260810T142046Z-safety-prompt-contract/`。服务器退出码 0，
精确 SHA、SQLite Jar-in-Jar、ServerPlayer 加入和优雅生命周期均通过；没有客户端或模型请求，
所以 `functionalAiClaim=false`。

## 最新定向生产修复（2026-08-10，连续受击空中脱离）

此前完整生命周期回归在末地水晶造成无方向连续伤害后，身体短暂被击退到空中，
`EmergencySurvivalController` 因 `onGround=false` 只扫描不脱离。现在有限的近期伤害
分支允许普通水平输入完成短暂空中脱离；未知投射物仍不会进入该分支，水中、着火和真正
下落仍由更高优先级车道处理。新增的定向 JUnit 通过；全新 Forge 65.1.0
`headless_player_lifecycle_state_and_fair_action` 1/1 通过，5967 个运行时样本平均
269540 ns、滚动 p95 1272625 ns，低于 1 ms/2 ms 目标。日志为
`/tmp/mcai-lifecycle-after-air-knockback-fix.log`。这仍是无模型、受控 headless
回归，不提升真实模型、随机 Hardcore 或 M0--M4 状态。

同一修订在全新 Forge 65.1.0 工作目录重跑 `real_emergency_zombie_skeleton_horde` 与
`real_emergency_iron_golem_duel`，各 1/1 通过（1.185 秒、1.089 秒）；这保持了十僵尸/十小白
压力场和铁傀儡近战的原版应急回归，不提升模型 PVP、真人客户端或正式 Hardcore 结论。

新产品 JAR `63f1b80e…f3dc5d00` 的 Forge 65.1.0 精确专服 smoke 也通过：服务器退出码 0，
精确 JAR、SQLite Jar-in-Jar、ServerPlayer 加入和优雅生命周期均通过，结果目录为
`e2e/results/no-commit-dirty/20260810T141222Z-air-knockback-fix/`。这是 `functionalAiClaim=false`
的专服/封装证据；没有启动客户端或模型，oracle 随附的停止前无结果状态不改变 smoke verdict。

## 最新定向生产修复（2026-08-10，农田水边逃逸与连续移动）

本轮 fresh Forge 65.1.0 复现并修复了两个真实边界：水边逃逸把脚下 farmland
漏判为普通落点，跳跃会把支撑踩成 dirt；农田移动每 Tick 先清除输入加速度，导致
潜行拾取在边缘只转向不前进。`HarvestAndReplantStepSkill` 现在对当前及上方候选
坐标核验 farmland/作物块，新鲜非农田落点最多发出一次跳跃；行走、拾取和逃水的
转向使用不清空连续输入的移动准星，精确交互仍走原版 stop + 中心准星门禁。

全新 Forge 65.1.0 世界的 wheat、carrot、potato、beetroot、expanded wheat
维护测试各 1/1 通过，source compileJava/compileTestJava 通过。随后完整 JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E
39/39 通过；当前唯一安装产物 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`
SHA-256 为 `63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`，
client/oracle 测试 JAR 哈希保持 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` /
`97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。这些仍是无模型、
受控 headless ServerPlayer 内循环证据，不能升级为真实 AI 聊天到动作、随机 Hardcore 或
M1--M4 通过。

正式模型/Actor/Observer 门禁仍因上游 401 与本机 Darwin 无 Linux/Xvfb 保持
`NOT_RUN`；源码仍为 `DIRTY_NO_COMMIT`、产物仍为 `NON_RELEASE`。

同一最终源码还通过了 fresh Forge 65.1.0 的 `real_parkour_course`、
`real_emergency_iron_golem_duel`、`real_emergency_zombie_skeleton_horde` 各 1/1。
这些是受控无模型 headless 物理/应急回归，不提升为模型控制、真人客户端、PVP 或
正式 M0--M4 通过。

同一最终源码随后通过 fresh Forge 65.1.0 的 `real_end_portal_activation`、
`real_end_victory_and_return`、`zero_human_dedicated_server_chunk_and_respawn`
（生产零人自动启动）和 `auto_presence_on_human_login` 各 1/1。终局场景日志包含
`The End?`、`Free the End` 和返回完成；无人服务器场景实际出现 MCAI 玩家，真人登录
场景实际出现 TestHuman 与 MCAI。仍然是无模型、受控 headless GameTest 证据，不能升级
为真实 AI 通关、客户端视觉或 M0--M4 通过。

`real_furnace_batch`、`real_charcoal_furnace_batch` 也在 fresh Forge 65.1.0 各 1/1
通过；JDK25 定向凭据重启/并发/运行时测试（`ApiKeyManagerRestartPersistenceTest`、
`ApiKeyManagerConcurrencyTest`、`ModelRuntimeTest`）通过，证明跨实例恢复/失效隔离的
代码路径没有被本轮改动破坏。真实供应商仍未通过 401 能力门禁。

`real_player_chat_to_immediate_bound_follow` 当前源码 fresh Forge 65.1.0 也为 1/1；日志
显示 TestHuman 的跟随任务已安装且 MCAI 真实加入玩家列表并执行跟随。该场景是服务端
绑定目标的无模型回归，不能外推到模型已验证或所有自然语言任务。

基础生存/技术路径 `real_zero_human_dedicated_server_foundation`、
`real_prepare_and_plant_plot`、`real_build_hydrated_crop_field` 和
`real_verified_portal_return` 在 fresh Forge 65.1.0 各 1/1。它们加强了身体、农田、
水利与已验证传送边的内循环证据，但仍不是随机 Hardcore、真实模型或客户端黑盒门禁。

当前源码的最终 JDK25 package 复核通过 `check verifyReleaseJar e2eClientJar e2eOracleJar`
16 tasks，Python E2E 39/39，GOAL_STATE JSON/compat 校验均通过；产品 SHA-256 仍为
`63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`。Forge 兼容检查
明确报告 65.x 已发布 11 个 patch、正式矩阵未完成。

2026-08-10 UTC 重新核对 Forge 官方 26.2 下载页后，Latest/Recommended 仍是
65.1.0，已列发布版本为 65.0.0--65.0.9 与 65.1.0；Forge 66 仍无官方发布条目，
所以兼容范围继续保持 `[65.0.0,66.0.0)` 的 65 线声明而不是伪造 66 兼容。

同一当前源码在 Forge 65.0.0 最低线 fresh `real_zero_human_dedicated_server_foundation`
通过 1/1，并实际加载 71 个 GameTest 函数与 SQLite Jar-in-Jar。完整 65.x 补丁矩阵
仍未完成，不能把这个最低线 smoke 写成全版本兼容。

`offline_idle_equipment` 在 Forge 65.1.0 fresh world 通过 1/1，原版日志出现 `Suit Up`；
该证据覆盖离线身体已有装备的穿戴路径，不能外推为模型读取聊天后自动装备的黑盒门禁。

`real_emergency_enderman_defense` 在 Forge 65.1.0 fresh world 通过 1/1，并出现原版
`Cover Me with Diamonds`；这是受控末影人战斗/装备回归，不提升模型 PVP 或 Hardcore 结论。

本次继续在全新 Forge 65.1.0 工作目录运行 `real_nether_blaze_rod_acquisition`，1/1 通过；日志确认
MCAI 通过原版近战击杀烈焰人、拾取烈焰棒并获得 `Into Fire`。这是下界战斗/掉落拾取的受控无模型
组件证据，不是自然要塞发现、模型控制或 M2 通关证据。Forge/Gradle 日志为 `BUILD SUCCESSFUL`；
命令末尾的 zsh 保留变量名退出只属于测试壳层，不改变 GameTest 结果。

随后在全新 Forge 65.1.0 工作目录运行 `real_stronghold_reach`，1/1 通过（GameTest 14.16 秒）。
日志确认 MCAI 从已观察入口外沿普通行走、下降挖掘并消耗铁镐/火把完成测试；这是已验证目标的
接近/挖掘内循环，不是自然随机种子定位、模型控制或正式 M2 证据。

再在全新 Forge 65.1.0 工作目录运行 `real_ender_pearl_reserve`，1/1 通过（GameTest 7.798 秒）；
日志显示真实 ServerPlayer 完成受控战斗/拾取并达到 13 颗末影珍珠储备。这是预置资源条件下的
末影珍珠收集组件，不是自然探索、模型控制或随机 Hardcore 证据。

当前源码还逐个在全新工作目录运行 Forge 65.x 全部 11 个已发布 patch 的
`real_zero_human_dedicated_server_foundation`：65.0.0、65.0.1、65.0.2、65.0.3、65.0.4、
65.0.5、65.0.6、65.0.7、65.0.8、65.0.9、65.1.0 均 `rc=0` 且 1/1 通过；日志位于
`/tmp/mcai-forge65-<version>-foundation.log`。这只是兼容启动/加载/无人 ServerPlayer
基础 smoke，不是每个 patch 的聊天、移动、菜单、保存、重启完整矩阵。

正确 CLI 的正式客户端 preflight 已再次运行：`ready=false`，缺少 Linux host、Xvfb、
`MCAI_BASE_URL`、`MCAI_MODEL` 和 `MCAI_API_KEY(_FILE)`；物理显示器未触碰，客户端未启动，
密钥未读取或发送。该输出位于 `/tmp/mcai-preflight-current.log`，所以真实 Actor/Observer/
模型门禁保持 `NOT_RUN`，不是游戏失败。

随后在同一 11 个 Forge 65 patch 上逐个运行 `zero_human_dedicated_server_chunk_and_respawn` 与
`auto_presence_on_human_login`，共 22/22 通过（每个 `rc=0`、1/1）。这补齐了 patch 级无人自动
出生/重生与真人登录并存的生命周期内循环；仍不等于每 patch 的聊天、移动、菜单、保存/重启
完整门禁。

在全新 Forge 65.1.0 工作目录运行 `real_nether_blaze_material_reserve`，1/1 通过（GameTest
5.145 秒）；真实 ServerPlayer 以可见目标完成原版烈焰人近战、掉落拾取并达到 7 根烈焰棒储备，
技能状态为 `COMPLETED`。日志为 `/tmp/mcai-next-blaze-material-65.1.0.log`；这是受控无模型
下界材料组件，不是自然要塞、模型或 M2 证据。

矩阵后再次执行固定 Temurin JDK25 的 `check verifyReleaseJar e2eClientJar e2eOracleJar`，16 tasks
通过；Python E2E 39/39、GOAL_STATE JSON 与 compat 校验也通过。修复空中脱离后产品 JAR
SHA-256 更新为 `63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`，
client/oracle 哈希未变。

## 最新定向生产修复（2026-08-10，农田路线与中心准星门禁）

在严格的“语义视野只授权、当前第一人称中心准星重新命中农田顶面后才使用”规则下，扩大
农田曾暴露单步拾取器无法绕过未被父事务授权的相邻作物格。`HarvestAndReplantStepSkill`
现在在同一最新公平导航快照内做有界 cardinal BFS；每个节点仍必须通过事务授权、碰撞、支撑、
液体和危险谓词，不读取世界、不扫描隐藏方块/实体、不传送。全新 Forge 65.1.0 GameTest
结果：`real_maintain_observed_expanded_field` 1/1，以及 wheat/carrot/potato/beetroot
四个紧凑矩阵各 1/1 通过。

严格中心准星夹具已补齐，farming/core 定向 JVM 测试通过；完整 JDK25
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过（16 tasks），Python E2E 39/39 通过。
当前唯一安装产物 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`8efb1f160fb533e7e679555213d79ca00ce0c3a3032f19369ab6839ef125e93b`；客户端/Oracle 测试
JAR 哈希仍分别为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` 与
`97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。这是无模型、受控
headless ServerPlayer 内循环证据，不能升级为真实 AI 聊天到动作、随机 Hardcore 或 M1--M4。

正式模型/Actor/Observer 门禁仍因此前上游 401 与本机 Darwin 无 Linux/Xvfb 保持 `NOT_RUN`；
源码仍为 `DIRTY_NO_COMMIT`、产物仍为 `NON_RELEASE`。

## 最新定向生产修复（2026-08-10）

新鲜 Forge 65.1.0 农田维护内循环曾真实失败于“掉落物已存在但身体未走到拾取范围”。
`HarvestAndReplantStepSkill` 现在只在当前第一人称语义感知确认匹配掉落物时，沿其已观察位置
执行受限普通移动，并继续要求重植地块、站立、非水和事务局部边界；不读取隐藏实体、不直接
修改物品栏。`HydratedCropFieldPlanService` 的维护规划方法也改为显式契约，移除了永久生产
`UnsupportedOperationException` 默认桩。定向 farming JVM 测试通过，修复后同一测试在全新
Forge 65.1.0 GameTest 世界 1/1 通过。该证据无模型、无真人客户端，只是当前生产物理内循环，
不能提升 M1--M4 或专业陪玩声明；完整 package 门禁将在本轮随后重跑。

随后完整 JDK25 package 门禁通过 16 tasks，Python E2E 39/39；当前产品 JAR SHA-256 为
`9114adea1edb3a4dfef943fa57b172640ac710dc9062d62d54fb1975f3f92bcd`，e2e-client/oracle
测试 JAR SHA-256 分别为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf`
和 `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。Forge 兼容检查仍显示
11 个已发布 65.x 条目、正式矩阵未完成；真实模型/客户端和 M0--M4 仍未运行。

## Latest continuation: provider-neutral worker contract (2026-08-10)

已加入 `e2e/worker_protocol.py`、`scripts/create-worker-job.py`、`scripts/run-e2e-worker.py` 和
`scripts/verify-worker-result.py`，让真实 Linux/Xvfb Actor/Observer 分片可以由任意隔离
worker 执行并由中央按源码/JAR/工件哈希复核。协调器脚本默认拒绝脏工作树，并只接受公开的
64-hex 种子承诺；协议拒绝原始种子、凭据和世界路径；缺少环境只产生
`BLOCKED_INFRA`/`BLOCKED_CREDENTIAL`。39/39 Python E2E 通过；当前 macOS 没有
Linux/Xvfb，所以没有功能客户端或模型调用，M0--M4 仍未运行，不能宣称专业陪玩或通关。
worker 还会在启动前和子运行结束后绑定实际模型名、Base URL 主机、凭据存在性和来源；本轮
Gradle `check verifyReleaseJar e2eClientJar e2eOracleJar --offline` 仍通过，生产 JAR SHA-256
保持 `731b7a4d572ffdb8494c98026c1da0fd008c156a78e54ceeba388185278eae21`。

更新日期：2026-08-10。

本轮修复模型连接丢失后的 active-skill 停滞：运行时现在允许已授权技能进入
`SkillSupervisor` 的 `model_disconnected` 安全收尾；紧急生存先取得所有权时会安全摘除旧技能，
不启动新动作、不发起规划请求。定向 JVM、完整 `check`（1076 tests、0 failures、0 errors、2
skips）、`verifyReleaseJar`、客户端/Oracle 测试 JAR、Forge 兼容检查和 Python E2E 30/30 通过。
最新开发 JAR SHA-256 为
`731b7a4d572ffdb8494c98026c1da0fd008c156a78e54ceeba388185278eae21`；释放旧输入后的精确
Forge 65.1.0 专服 lifecycle smoke `20260810T104022Z-e2e071cf86ffb19` 为 PASS，但
`functionalAiClaim=false`，因此不升级为真实模型、聊天到动作或 M0--M4 证据。

本机正式 `e2eChat` 仍因 Darwin 无 Linux/Xvfb 且没有模型环境而 `NOT_RUN`；真实模型、Actor/Observer、
随机 Hardcore 和 M0--M4 继续保持 `NOT_RUN`，源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

本轮补齐 Tab 列表在线状态：真实服务端 AI 身体显示 `[AI] Name  ● online`，离线/会话重建显示
`○ offline`；该标记只来自 `AiPlayerManager.Status.online()`，不会伪装模型已验证。源码编译与
完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，Python E2E 30/30 通过。当前开发
JAR 为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`ceefa3e120c13d1bbc71cb1fa8787af1fb4a185c2426b907f6856062684065cb`。由于本机 Darwin 没有
Xvfb，Tab 的真实客户端视觉验收仍为 `NOT_RUN`；真实模型、随机 Hardcore 与 M0--M4 继续
`NOT_RUN`，源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

正式 `e2eChat` 前置检查也已执行但未启动：`ready=false`，当前 Darwin 缺 Linux/Xvfb，测试环境
没有模型端点、模型名或 API 凭据，因此没有产生模型请求。证据在
`e2e/results/no-commit-dirty/20260810T102441Z-formal20260810102441/`；这只更新阻断记录，不改变
正式门禁结论。

最新产品 JAR 的 Forge 65.1.0 独立专服精确工件 smoke 通过：
`e2e/results/no-commit-dirty/20260810T102609Z-e2e42b26f3709cf/server-smoke-verdict.json` 报告
Mod/SQLite Jar-in-Jar/headless ServerPlayer 登录与干净关闭均通过，JAR SHA-256 为
`ceefa3e120c13d1bbc71cb1fa8787af1fb4a185c2426b907f6856062684065cb`。该归档的 Oracle 任务没有
启动（`functionalAiClaim=false`），因此不能作为模型、聊天、移动、物品或 M1--M4 证据。

同一当前源码在最低支持 Forge 65.0.0 上运行 `zero_human_dedicated_server_chunk_and_respawn`
独立专服门禁，结果 1/1（1.539 秒）：Forge 65.0.0 能加载 Mod，无真人时创建 headless
`ServerPlayer`，远端玩家区块模拟与普通重生链通过。Forge 的“65.0.0 非最新补丁”提示是预期
版本检查，不是测试失败；这只证明最低补丁的启动/身体内循环，不等于 65.x 全矩阵或正式 M0--M4。
产品 JAR SHA-256 仍为 `2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，
源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

离线 Python E2E 回归 `python3 -m unittest discover -s e2e -p 'test_*.py'` 通过 30/30，覆盖
工件隔离、密钥不泄露、兼容声明和失败闭合协议；它不启动客户端或模型，不能升格为真实 AI 验收。
产品 JAR SHA-256 仍为 `2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，
正式真实模型、真人客户端、随机 Hardcore 与 M0--M4 仍 `NOT_RUN`，源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

随后运行 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar --offline`，Forge 兼容检查、
完整 JVM `check`、发布 JAR 验证、客户端/Oracle 测试 JAR 构建均通过。当前产品 JAR SHA-256
仍为 `2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，GOAL_STATE JSON
验证通过。该门禁仍是源码/封装/受控内循环证据；真实模型、真人客户端、随机 Hardcore 和 M0--M4
继续 `NOT_RUN`，源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Forge 65.1.0 独立 GameTest `headless_player_lifecycle_state_and_fair_action` 已通过 1/1（28.79
秒）。真实 PlayerList-backed headless `ServerPlayer` 完成登录/移除/重登录与状态保持，并通过原版
箱子存取、熔炉装载/取出、切石机选项、交易、船乘坐/航行、动力铁轨矿车乘坐/航行和公平动作检查。
结果仍是受控无模型物理证据，不是
真实模型、真人客户端、自然随机世界或 M0--M4 通过；产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Forge 65.1.0 独立专服的 `zero_human_dedicated_server_chunk_and_respawn` 已通过 1/1（约
1.736 秒）：无真人玩家时生产启动会创建唯一在线 AI `ServerPlayer`，远端区块/相邻模拟区块由
原版玩家 ticket 正常维持，实体与方块 tick 均推进，随后普通死亡/重生完成。该结果是无模型的
生命周期与区块模拟证据，不是自主游玩、真实客户端或 M0--M4 证据。产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

本轮 Forge 65.1.0 定向物理回归新增三项通过：`real_prepare_water_source` 1/1（631.2 ms）、
`real_charcoal_furnace_batch` 1/1（651.2 ms）和 `real_portal_cast_and_light` 1/1（1.246 秒）。
它们是当前源码的真实 headless `ServerPlayer` 原版水源、熔炉和下界门操作链，均为受控无模型内循环
证据；正式真实模型、真人客户端、自然随机世界与 M0--M4 仍为 `NOT_RUN`。产品 JAR SHA-256
仍为 `2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

末地关键生命周期回归已在当前源码/Forge 65.1.0 完成：`real_end_victory_and_return` 1/1，
真实 headless `ServerPlayer` 进入末地、击杀末影龙、触发胜利进度并返回主世界，GameTest 日志
为 `All 1 required tests passed :)`（8.464 秒）。这是无模型、受控夹具的物理链证据，不是
Hardcore 随机种子或真实模型通关证据；正式 M0--M4 仍为 `NOT_RUN`。当前产品 JAR SHA-256
仍为 `2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

同一当前源码的 Forge 65.1.0 原版交互回归随后通过：`real_furnace_batch` 1/1、
`workstation_wood_prerequisite_composition` 1/1、`real_prepare_and_plant_plot` 1/1。这些是
真实 headless `ServerPlayer` 的熔炉、木材前置工作站和耕地播种链，仍是受控无模型内循环证据，
不升级正式 M1/M3、自然世界或模型门禁。

战斗/食物回归随后也通过：`real_emergency_slime_defense` 1/1 与 `real_food_animal_hunt` 1/1。
这些结果证明本地公平感知与原版身体控制会在受控受击/食物场景产生真实动作，但不代表真实模型
决策、PVP、随机种子或正式 M0--M4。

本次继续复核了“光说不做、受击站桩、收到装备不穿”的反馈。Forge 65.1.0 当前源码的真实
dedicated GameTest 定向结果：即时玩家跟随 1/1（HoldingModelGateway、零模型调用，仅验证
聊天到身体的内循环）、十僵尸十骷髅 1/1、铁傀儡单挑 1/1、无模型自动穿戴已拥有铁帽/盾牌
1/1。它们证明 headless `ServerPlayer` 的本地移动/紧急生存/原版背包交易链在当前源可用，
但不代表真实 MiMo、真人客户端、PVP 或 M0--M4 正式门禁；这些仍保持 `NOT_RUN`。最新精确
产品 JAR 仍为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`；源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

坐标无关的整田维护现已接入生产技能 `maintain_observed_crop_field`。它不接受方块坐标数组：先用
自身第一人称滚动语义观察建立有界现场图，在后台生成工作顺序，再逐格普通步行、原版破坏、等待
掉落实体、碰撞拾取、装备种子并原版补种；每次修改前与最终完成时都要求新 observation revision。
真实门禁曾在 `run-debug183`/`184` 失败，最终根因是感知导航把无碰撞小麦视觉表面误判为实心墙。
语义格式 7 现在只对已经被射线命中的方块发布 `EMPTY`/`OBSTRUCTED_OR_PARTIAL` 粗粒度碰撞
affordance，不暴露碰撞箱或隐藏邻块，并支持同类模组方块。修复后 Forge 65.0.0 的
`run-debug185`/`187` 连续 1/1 通过（32.05/18.52 秒），Forge 65.1.0 的当前源码
`run-debug188` 1/1 通过（24.06 秒），均完成 8 次真实收割、拾取、8 次补种与复验。

当前完整 JVM 为 1056 tests、0 failures、0 errors、2 个外部 live 测试 skipped；`check
verifyReleaseJar e2eClientJar e2eOracleJar` 通过。唯一安装 JAR 为
`mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`58b450437f38b4f8146475b3da9a54b86a1b895067ac9e7aba4052a1a44cbac0`；测试夹具、GameTest
world resources 和 GameTest mixin 均未进入该 JAR。以上仍是无模型 inner-loop 证据，真实
Actor/Observer、合规模型、自然世界长期农场、Hardcore 随机种子及正式 M0--M4 全部保持
`NOT_RUN`，源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

即时玩家绑定跟随现在有一条 release-excluded 的严格物理 Forge GameTest：真实
PlayerList-backed `ServerPlayer` 通过 Forge 普通聊天提交“跟我来”，目标必须在不调用占位模型
网关的情况下启动 `follow_entity`，连续持有技能、完成两段移动、AI 身体累计步行不少于 7 格，且
单 tick 位移不超过 0.9 格。该门禁先真实暴露出规划重试连续超出 2 ms 导致技能重启：
`MoveToSkill` 每 tick 重建同一 2090-voxel 记忆快照，arrival goal 又扫描整张图。现在同一不可变
导航快照/脚下格的安全融合会复用，目标候选只枚举 arrival cube 与观测图中较小者；新快照或脚下
格变化立即失效，2 ms 监督门槛未放宽。修复后 `run-debug147`、`148`、`149` 三个独立进程严格
1/1 通过（2.335、2.728、3.340 秒），且审计均无技能失败、重启或模型请求。

当前完整 JVM 回归为 1016 cases、0 failures、0 errors、2 skipped；`check verifyReleaseJar
e2eClientJar e2eOracleJar`、Forge 65.1.0 兼容检查与 mutation 10/10 通过。新 fat/slim SHA-256
分别为 `e760f4387c3079f49f216402c22c5d9f2ad78990f76f843210eff6fec51d1696` /
`a04217873ddd8d2078c1a397f969a7b24d9182fcf3f6f56be6776d699c50326e`；精确专服 smoke
`20260809T110358Z-e2eaf8f76907f1a` 为 PASS。它仍是无真实客户端/模型的内环与生命周期证据，
`functionalAiClaim=false`，正式 M0--M4 保持 `NOT_RUN`。

明确的玩家聊天“跟我/跟我走”不再先等待模型往返：服务端已绑定发话者身份且该玩家出现在 AI
自身当前公平语义样本中时，`BrainOrchestrator` 立即通过正常 `SkillSupervisor` 启动
`follow_entity`；目标不在视野时最多直接执行一次第一人称 `survey_surroundings`，随后恢复普通
模型规划。它不使用坐标、隐藏实体位置或传送，且以直接玩家指令审计，不伪造模型 trace。定向
JVM 58/58、完整构建、兼容检查与 mutation 10/10 通过；精确 Forge 65.1.0 smoke
`20260809T103752Z-e2e340cf381816e` 为 PASS。当前 fat/slim SHA-256 分别为
`dc8f9b33af8e20f7fefb03bb27dc1bf5092417beae6c6c306a06c140db9c1beb` /
`9e9807fb927bc5a90fdb24bcbdf2a1c8571b70ca53c9e902c7c9eebfdedba182`；该 smoke 仍为
`functionalAiClaim=false`，真实客户端/模型和 M0--M4 仍 `NOT_RUN`。

本轮关闭了另一个“只说不做”的无限循环：普通任务若连续四次收到没有技能的
`CONTINUE`/`REPLAN` 或动作承诺，不再无限消耗模型请求。玩家聊天目标会进入可由下一条真实玩家
消息唤醒的明确等待状态并只广播一次诚实澄清提示；MCP/恢复目标与锁定 Hardcore 评测安全终止并
留下 `planner_no_action_exhausted` 审计，绝不等待评测人员干预、虚构进展或代替模型选择动作。
`BrainOrchestratorTest` 定向验证了有界等待、玩家唤醒和锁定评测终止。当前完整
`check verifyReleaseJar e2eClientJar e2eOracleJar`、兼容检查和 mutation 10/10 均通过；精确
Forge 65.1.0 专服 smoke `20260809T102438Z-e2e519d82e232c1` 为 PASS，fat JAR SHA-256 为
`9c8892bcd5cba1dca7b5bfefbe5373e33a4ca2c6bb05a1a852dca52a291ad1c0`，但
`functionalAiClaim=false`。真实客户端/模型、M0--M4 仍为 `NOT_RUN`。

按目标重新执行一次真实 `e2eChat` 入口，最新归档
`20260809T092655Z-recoverychat20260809` 在 preflight 阶段 `NOT_RUN`：当前 Darwin 无
Linux/Xvfb，也没有模型端点、模型名或凭据；未启动客户端、未触碰物理显示、未发送模型请求。
正式聊天到动作和 M0--M4 继续保持 `NOT_RUN`。

针对“只回复好但身体不动、玩家不知道是否仍在规划”的路径，`MinecraftBrainEventSink` 现在
对同一目标 revision 的 `planner_no_action_backoff` 最多广播一次诚实状态：`[AI] 名称：我还没有选定可执行动作，正在重新规划。`
该状态不启动技能、不移动、不宣称完成；评测锁定时不广播。定向脑部/通信 JVM 契约与完整
`check`/打包均通过。
最新 fat JAR SHA-256 为
`9ab54de35550d5606c77626487ebf6dec0807a9881b9795672edbb4274c12c13`，slim 审计 JAR 为
`1b52c936abec92701f992612133e1304c299d7f2b0fa24ab285b1a4ea48f1b73`；mutation `20260809T092022Z` 为
10/10 捕获，Forge 65.1.0 专服 smoke `20260809T092026Z-e2e6c00a8c279d5` 为 PASS，
但仅是精确 JAR 生命周期证据且 `functionalAiClaim=false`。真实模型/客户端和 M0--M4 仍
`NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

本轮补齐了模型等待期限的实际 wiring：`model.softTimeoutSeconds` 现在传入规划器和聊天
lane。规划请求首次超过软期限只写一次 `model_request_soft_deadline`，不取消、不重试、
不产生动作；聊天 lane 向原发消息玩家只发送一次仍在处理的进度提示。软期限与硬期限
不一致时使用 `hard - 1` 秒并记录安全警告，避免非法跨字段配置让服务启动失败。
`CompanionConfigTest` 和 `BrainOrchestratorTest` 定向通过；随后完整 `check
verifyReleaseJar e2eClientJar e2eOracleJar`、兼容检查、JVM、Python E2E `20/20`、JSON 和
秘密扫描均通过。当前 fat JAR SHA-256 为
`dfe417b7dfb4dc90055891b92e15208fcf61d657844f5cbcc40bb12317905d94`，slim 审计 JAR 为
`307c0ff901aa8e0d338cc252e3a6a2fca0f2fe66d8a7b51ab18aaa760843f9c8`；精确 Forge 65.1.0
smoke `20260809T085339Z-e2e6ce26d860d9f` 为 PASS，但只证明生命周期且
`functionalAiClaim=false`。真实模型/客户端、PVP、正式 M0--M4 和随机 Hardcore 统计仍
`NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

随后修复了规划器把短确认误当成玩家澄清、导致身体停在
`WAITING_FOR_PLAYER` 的路径。对玩家任务中的 `ASK_PLAYER` + `好的`、`收到`、`OK`、
`没问题` 等无问句短语，现在只触发有界重规划并抑制未被技能接受的语音；新增
`BrainOrchestratorTest` 与 `PlayerTaskIntentTest` 通过。完整
`check verifyReleaseJar e2eClientJar e2eOracleJar` 也通过，当前 fat JAR SHA-256 为
`e2f3e04522cc39f8d562629186c36fc5b2fcdbecca7d3ac63dfbf1a5dbc10b99`，审计 slim JAR 为
`1e4f36268306de897b9fb6bd46bbba0eb6502f66fd7d597e756e7e8ece412de1`。当前源码 mutation
门禁 `20260809T090612Z` 为 10/10 捕获；Forge 65.1.0 精确专服 smoke
`20260809T090616Z-e2e52778ef17299` 为 PASS、`functionalAiClaim=false`。这些仍不替代真实
模型/客户端聊天到动作或 M0--M4 正式门禁。

最新定向复测又通过两项 Forge 65.1.0 当前源码的真实物理门禁：`run-debug140` 的
`real_water_clutch` 1/1（844 ms），日志确认生产紧急车道实际进入
`DEPLOYING_WATER`；`run-debug141` 的 `real_parkour_course` 1/1（2.849 s），确认
headless `ServerPlayer` 走普通碰撞/跳跃物理完成跑酷。这些没有模型请求，只能证明本地
自救与移动基础，不提升实时模型、PVP、真人客户端或 M0--M4 正式门禁。

随后 `run-debug142` 在同一 Forge 65.1.0 当前源码通过 `real_emergency_zombie_skeleton_horde`
1/1（1.450 s），十僵尸＋十骷髅压力场景的真实 headless 身体没有冻结。它仍是无模型
本地生存证据，不是模型控制的 PVE/PVP 或正式随机种子门禁。

持久化 API Key 的设置事务现在按平台能力分层：有 Keychain/DPAPI/Secret Service 时跨重启
保存；无桌面 Secret Service 的 Debian/容器会接受本次进程内验证，并返回
`saved_verified_process_restart_required`，同时要求下次启动前使用
`MCAI_API_KEY_FILE`/systemd Secret/`MCAI_API_KEY` 注入，绝不伪装成重启安全保存。最新构建
门禁后的可安装 fat JAR SHA-256 为
`f040fdc8c4e8a95292db74e1be6d25520260a56f29d4e5e3521f76acbcf857f4`，审计 slim JAR（位于
`build/audit-libs`）SHA-256 为 `b1a3a3984084ecded7c8912215cf9df11d1088490517b7b3eb9df02ebf4aa96a`；
JVM、工件、兼容和 Python E2E 门禁均通过。真实 MiMo 之前唯一探测仍为 HTTP 401，当前
macOS 预检没有 Linux/Xvfb 或模型环境，真实客户端/模型聊天到动作和 M0--M4 继续
`NOT_RUN`。

随后使用当前唯一可安装 fat JAR 做了 Forge 65.1.0 精确专服 smoke：归档
`20260809T083522Z-e2e17481252e31d` 为 `PASS`，server/actor/observer 安装副本哈希全部
一致，Jar-in-Jar SQLite 与 headless 生命周期正常，`functionalAiClaim=false`。

最新一次定向审计保留了真实失败边界：集中隔离入口后的 `run-debug131` 曾触发运行时 p95
约 2.030 ms 超限，`run-debug132`/`run-debug133` 曾在末地水晶后的
`RECENT_DAMAGE_EVENT` 安全等待上失败。当前只在测试夹具加入了世界/帧 tick 与伤害状态
诊断，并将夹具等待上限从 80 调到 100 tick；生产伤害信号与安全前置未被清除或放宽。
Forge 65.1.0 的 `run-debug134`、`run-debug135` 生命周期定向复测均 1/1 通过（约
27.51/27.90 秒，滚动 p95 约 1.397/1.414 ms），但失败运行仍保留，不能写成批量或正式
门禁通过。

这之后的 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过：JVM 1005（0 failures、
0 errors、2 skipped），兼容检查通过，Python E2E `20/20`；当前 fat/slim 工件 SHA-256 为
`ef06e0ec5f6dc48a062920cc7b604eb1286f1bfd54a28f9c6a3a9a17214d95c9` /
`a05d9cff2f448fa14d127fd3b6136b69ef0c11b37fc3d4b1be5d3336152227d5`。这些仍是无模型的
服务端/物理与构建证据，M0--M4、真实客户端/模型、PVP 和随机 Hardcore 统计继续 `NOT_RUN`；
源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

本轮真实客户端预检再次明确为 `ready=false`（Darwin、无 Linux/Xvfb、无运行时模型环境）；
预检未启动客户端、未触碰物理显示、未读取或发送密钥，故 Actor/Observer 与模型聊天到动作
仍保持 `NOT_RUN`，不能用无模型 GameTest 替代。

测试专用 live-model 场景的入口也已接入同一隔离清理边界，避免共享 GameTestServer 在失败
后遗留旧 body、动作租约或紧急车道；这部分不进入发布 JAR，不改变生产启动或重生路径。

Forge 65.1.0 离线战斗复核：`run-debug136` 十僵尸＋十骷髅 1/1（约 1.874 秒），
`run-debug137` 铁傀儡单挑 1/1（约 1.618 秒）。这是生产紧急车道在真实 headless
`ServerPlayer` 上的移动/装备/近战 PVE 证据，不是 PVP、模型或 M0--M4 正式门禁。

本轮针对上一批次的共享 GameTest 状态补了测试专用隔离清理：普通夹具开始前终止遗留目标、
停止动作车道、释放技能监督器并移除旧 headless body；生产 JAR 不包含该 helper，且生产
`zeroHumanAutoSpawnTest` 路径仍验证启动时自动出生。Forge 65.1.0 定向验证为：普通零人
`run-debug128` 1/1（约 6.184 秒）、生产自动出生 `run-debug129` 1/1（约 6.230 秒）、
headless 生命周期 `run-debug130` 1/1（约 36.04 秒）。此前批量 `run-debug127` 在约 90
分钟后因停滞以退出码 130 中断，不能写成全批次通过；上述定向结果也不等于模型聊天到动作、
真人客户端、PVP 或 M0--M4 正式门禁。源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，M0--M4
继续 `NOT_RUN`。

需要保留一个失败边界：在加入 `localGeometry` 的当前源码上，Forge 65.1.0 默认完整批次
`run-debug121` 实际完成 63 个 GameTest，但以退出码 4 结束，4 个必需测试失败（返回已验证
地狱门超时、Nether 烈焰人材料窗口超时，以及两个 tick 0 的 isolated-body 断言）。这不是
63/63 通过，也没有升级任何 M0--M4 状态。四个失败测试在独立新目录中分别重跑：
`run-debug122` 生命周期 1/1（约 39.06 秒）、`run-debug123` 烈焰人 1/1（约 4.151 秒）、
`run-debug124` 庇护所材料 1/1（约 21.67 秒）、`run-debug125` 屋顶跳跃 1/1（约 31.35 秒）。
当前将其作为 GameTest 批次内清理/顺序/预算问题继续诊断，而不是隐藏失败或把隔离结果冒充
全批次通过；真实模型、真实客户端和 M0--M4 仍未运行。

Forge 65.1.0 当前源码在新增 `localGeometry` 后完成三项定向物理复测：
`real_parkour_course`（`run-debug117`，1/1，约 4.247 秒）、
`real_emergency_iron_golem_duel`（`run-debug118`，1/1，约 1.817 秒）和
`real_emergency_zombie_skeleton_horde`（`run-debug119`，1/1，约 1.970 秒）。
这些测试证明真实 headless `ServerPlayer` 的原版移动、碰撞、装备和本地紧急战斗车道
仍可运行，但不包含模型请求、真人客户端或正式 M0--M4 统计门禁；不能据此宣称已完成
AI 陪玩、PVP 或两小时极限通关。源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

随后 `auto_presence_on_human_login` 在 Forge 65.1.0 `run-debug120` 通过 1/1（约
1.018 秒）：没有模型凭据时，测试真人与 `MCAI` 仍在同一服务器相邻出生并完成清理，
验证了身体生命周期和公开身份路径；这不是渲染客户端 TAB 或真实模型门禁。

本轮在语义感知边界加入了有界 `localGeometry` 摘要：由当前有限第一人称射线的已观察
表面派生面方向、相对高度、清晰片段和保守形状提示，帮助模型区分可能的墙面、上/下方
表面和近距离表面簇。它不扫描邻居/区块、不读取隐藏方块，也不把缺失证据当作开放空间；
实现是独立代码，只参考 Numen 公开的自我中心语义表征原则。

定向 `SemanticObservationJsonCodecTest`、完整 JVM `1005`（0 failures、0 errors、2 skips）、
`check`/`verifyReleaseJar`、客户端/Oracle 工件、兼容检查和 Python E2E `20/20` 均通过。
Forge 65.1.0 无真人专服 `run-debug116` 通过 1/1（约 2.100 秒），新 fat/slim SHA-256 为
`ef06e0ec5f6dc48a062920cc7b604eb1286f1bfd54a28f9c6a3a9a17214d95c9` /
`a05d9cff2f448fa14d127fd3b6136b69ef0c11b37fc3d4b1be5d3336152227d5`。这不提升真实模型、
视觉客户端、聊天到动作或 M0--M4 状态；源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

本轮继续审计 Forge 65.1.0 无真人专服路径：隔离 `zero_human_dedicated_server_chunk_and_respawn`
在 `run-debug115` 通过 1/1（约 1.890 秒），真实 headless `ServerPlayer` 在没有真人客户端
时自动出生、经历模拟区块窗口、死亡/原版重生并清理连接。随后对
`runE2eObserverClient`、`runE2eInstalledObserverClient`、`e2eClientJar`、`e2eOracleJar`
做 Gradle 配置级 `--dry-run`，并通过 `check verifyReleaseJar e2eClientJar e2eOracleJar`。
这些是服务端/构建基础设施证据，不是模型、视觉客户端、聊天到动作或 M0--M4 正式通过。
当前源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`，MiMo Token Plan 未重试。

离线装备夹具隔离后的当前源码已再次通过完整包门禁：JVM `1005`（0 failures、0 errors、
2 skipped）、`check`/`verifyReleaseJar`、客户端/Oracle 工件和 Python E2E `20/20`。
该轮只改 GameTestServer 的网关清理，不改变生产 fat JAR；生产 SHA 与 Forge 65.0.0
smoke 仍以本文和恢复检查点 v90.66 的记录为准。真实模型请求仍受供应商 HTTP 401
阻断，所有正式 M0--M4 与客户端聊天到动作门禁继续 `NOT_RUN`。

随后在同一当前源码上补做 Forge 65.1.0 隔离 `real_parkour_course`：`run-debug112` 通过
1/1（约 3.095 秒），真实 headless `ServerPlayer` 完成原版碰撞、跳跃、落差移动和清理。
这是无模型物理回归，不是视觉、聊天到动作、PVP、专业陪玩或 M1--M4 正式门禁；源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

用户此前询问的落地水也在 Forge 65.1.0 隔离 `real_water_clutch` 通过：`run-debug113`
1/1（约 797 ms）。真实 headless `ServerPlayer` 触发 `PREPARING_WATER`、
`DEPLOYING_WATER` 和 `BRACING_FALL` 紧急车道并完成场景；这是无模型 20 TPS 物理证据，
不代表自然随机世界的模型视觉判断或 M1--M4 正式门禁。

随后 Forge 65.1.0 隔离 `real_travel_diagonal_detour` 也通过：`run-debug114` 1/1（约
3.839 秒），真实 headless `ServerPlayer` 完成对角障碍绕行和路线恢复；这是无模型物理/
导航回归，不是聊天到动作或随机种子统计门禁。

同一当前源码在 Forge 65.1.0 隔离 `run-debug111` 通过
`headless_player_lifecycle_state_and_fair_action` 1/1（约 26.59 秒），覆盖 headless
加入/重登、菜单、移动、落水自救、下界/末地/强hold和返回门；20 TPS 运行时样本平均约
0.283 ms、滚动 p95 约 1.420 ms。这是跨补丁的身体/物理回归，不是模型聊天到动作、客户端
视觉或 M0--M4 通过；夹具传送/掉落诊断 WARN 已随测试退出，不是生产错误。

屋顶修复后的完整包已重新生成并通过工件门禁：JVM 1005（0 failures、0 errors、2 skipped）、
Python E2E 20/20、`check`/`verifyReleaseJar`、客户端/Oracle 工件均通过。当前 0.1.5
fat/slim SHA-256 为
`a69fe0438e1764fad6a3551e00cc079a1e0b2dce01ed39cf475bf9744138a766` /
`85373c5cfd6dc130b2fb093057e8cb895f0a15f9b2bc4034bfcb19cbde9ffac6`。精确 Forge 65.0.0
专服 smoke `20260809T050723Z-e2ead06345411b8` 通过精确哈希、Jar-in-Jar SQLite、
headless ServerPlayer 生命周期和优雅退出；无模型 Oracle 按设计失败，`functionalAiClaim=false`。

同一修复在 Forge 65.1.0 的 `run-debug104` 屋顶隔离门禁中再次通过 1/1（约 24.05 秒）。
这只是跨两个 Forge 65.x 版本的定向复核，不提升完整兼容矩阵或 Forge 66 状态。

同一当前源码的 Forge 65.1.0 隔离战斗复核也通过：`run-debug105` 铁傀儡单挑 1/1
（约 1.147 秒），`run-debug106` 十僵尸＋十骷髅压力场景 1/1（约 1.312 秒）。
这是无模型的原版 PVE/紧急生存物理证据，不是玩家 PVP、自然随机种子或正式 M0--M4。

历史整合批次中的 `headless_player_lifecycle_state_and_fair_action` 随后单独复现为
`run-debug107` 的强hold搜索安全拒绝：前一阶段 End 水晶的真实 recent-damage cue 尚未
自然过期。夹具已补充有界等待而不触碰生产安全判定；`run-debug108` Forge 65.0.0
通过 1/1（约 27.42 秒），整合链运行时样本 5835、平均约 0.271 ms、滚动 p95 约 1.427 ms。
`run-debug107` 仍保留为修复前失败，且本链无模型，不等于正式 M0--M4。

该夹具修复后的完整 `test`/`check`/`verifyReleaseJar`/兼容检查仍通过，JVM 1005（0 failures、
0 errors、2 skipped），Python E2E 20/20；因为只改 GameTest 等待逻辑，v0.1.5 生产 JAR
哈希和上方精确 smoke 仍有效。

随后单独重跑 `offline_idle_equipment`：`run-debug109` Forge 65.0.0 通过 1/1（约 709 ms）。
夹具在断言前清理上一场景残留的 HoldingGameTestGateway，恢复无模型边界；这是菜单/装备
的无模型物理证据，不是实时模型行为。

`real_nether_blaze_rod_acquisition` 也在隔离 `run-debug110` Forge 65.0.0 通过 1/1
（约 2.044 秒）；历史批次的 `already_active` 属于共享 GameTestServer 的单例/并发夹具
冲突，不能作为该技能的独立失败或通过声明。

屋顶修复已在移除临时诊断日志后再次通过 Forge 65.0.0 隔离门禁：
`run-debug103` `roof_jump_placement` 1/1（约 24.22 秒）。这是当前源码的干净重跑，
证明实际身体仍能沿已观察 apron、跳跃瞄准并完成屋顶放置；修复前的
`run-debug96`--`101` 失败记录继续保留。该修复后的完整包与精确产品 smoke 已在上方
记录；无模型证据不提升为真实陪玩或 M0--M4 通过。

## 当前继续点：屋顶跳跃站位修复（v0.1.5-dev-mc26.2）

当前源码在 Forge 65.0.0 隔离真实 GameTest 中通过 `run-debug102`
`roof_jump_placement` 1/1（约 23.68 秒）。屋顶顶面瞄准候选现在从半径 1 的
公平观察站位开始，覆盖紧凑庇护所外墙一格 apron；观察站位历史与实际失败瞄准历史
分开，避免已验证的外侧站位被错误跳过。候选仍经过安全站立、跳跃净空、观察射线、
计划通行与原版触及距离约束。`run-debug96`--`run-debug101` 的失败记录保留为
修复前边界，不能改写成通过；临时诊断日志已删除。

该证据无模型，只证明真实 Forge 身体/移动/跳跃/放置路径，不是聊天到动作、PVP、
专业陪玩或 M0--M4 门禁。当前源码/工件仍是 `DIRTY_NO_COMMIT`/`NON_RELEASE`；后续继续
隔离真实动作审计和模型恢复条件，不把无模型证据升级成正式产品声明。

## 当前继续点：烈焰人拾取交接修复（v0.1.5-dev-mc26.2）

现场“上一只烈焰人死亡后模型继续说话但身体不再行动”的根因已在当前源码中复现：资源
子技能处于观察掉落物的交接阶段时，20 TPS 生存监督器把新一帧旧敌对信息升级为
`GUARDING`，导致活动技能永远拿不到下一只目标。`SecureNetherBlazeMaterialSkill`
现在只在无投射物、无未授权敌对实体且满足有界授权条件时保留交接；拾取阶段允许远离
近战距离的已授权 Blaze 继续处理，投射物、近身接触和其他敌对实体仍优先交给紧急车道。
`AcquireNetherBlazeRodSkill` 仅暴露拾取交接阶段，不暴露隐藏世界状态。临时诊断字段和日志
已移除。

当前源码真实 Forge GameTest 定向证据：`run-debug81` Forge 65.0.0 1/1（约 2.599 秒）、
移除诊断后的 `run-debug82` Forge 65.0.0 1/1（约 3.624 秒）、`run-debug83` Forge
65.1.0 1/1（约 7.656 秒）。完整并发批次 `run-debug74` 仍是 63 项中 10 项失败，含
测试单例夹具冲突、离线模型配置污染、屋顶跳跃和其他战斗/传送场景，不能提升为整批通过。
（该段记录当时尚未重打包的中间状态；随后 v90.61 已完成重打包、Python E2E 与精确产品 JAR
专服 smoke，并在本文件上方记录了当前工件哈希。）
真实 MiMo 仍只有一次 HTTP 401；M0--M4、真实客户端聊天到动作和隐藏 Hardcore 统计门禁
继续 `NOT_RUN`。Forge 65.1.0 定向通过不代表 Forge 66 已适配。

修复后的当前源码包已重建：固定 Temurin JDK 25 下 JVM 1005（0 failures、0 errors、2 skipped），
Python E2E 20/20，fat/slim SHA-256 为
`32900a438282ae4ca343229736c3fb9d8e183a42a7c4ec818e7a290330d14c56` /
`11623bbfec5b6f3527eeb224dbc13288460e5e599656fee9787b059be581c4e8`。精确产品专服 smoke
`20260809T043610Z-e2e5dac10058c64` 在 Forge 65.0.0 通过所有生命周期检查，加载副本与
产品哈希一致，`functionalAiClaim=false`；本次没有模型凭据。

随后用当前源码做了 Forge 65.0.0 隔离物理回归：铁傀儡单挑、末影人防御、10 僵尸＋10 骷髅、
跑酷、离线装备栏、对角绕行、木材探索/庇护所、传送门浇筑点火和落水自救分别在
`run-debug85`--`run-debug93` 通过 1/1。它们证明了真实 headless ServerPlayer 的原版移动、
受击响应、普通攻击/装备、跳跃、传送门和水桶动作；无模型，不等同于真实陪玩或速通统计。

随后在同一当前源码上补做了专服生命周期：`run-debug94` 的无真人自动出生、模拟区块窗口、
死亡与原版重生通过 1/1（Forge 65.0.0，约 1.738 秒）；`run-debug95` 的真人登录与 AI 同时
出现、邻近出生和 TAB 可见身份通过 1/1（Forge 65.0.0，约 647.8 ms）。两项仍是无模型
生命周期证据，不提升真实模型聊天到动作或 M0--M4。完整无过滤批次 `run-debug84` 的原版
结构生成 OOM 边界仍保留，不能写成整批通过。

同一时间重新执行的无过滤 63 项批次 `run-debug84` 在 Forge GameTest 生成大量结构时以
原版 `StructureTemplate.loadPalette` 的 4 GiB Java heap OOM 崩溃，并且崩溃前仍出现并发单例
夹具、离线配置污染和屋顶步骤失败。完整批次不可记为通过，后续改用隔离/串行小批次。

本轮导航定向回归已在全新 Forge 65.0.0 隔离世界完成：对角绕行
`run-debug68` 1/1（3.281 秒）、木材探索/庇护所 `run-debug69` 1/1（17.38 秒）、
木材前置工作站合成 `run-debug70` 1/1（5.788 秒）、无真人专服出生/死亡/重生
`run-debug71` 1/1（1.739 秒）。修复保留为 `RollingTravelPlanner` 的目标方向投影
约束和探索专用短预算；临时诊断日志已移除。中间尝试的物理回程门导致对角回归
`run-debug65`/`run-debug66` 的 `no_progress`，已撤回，不能计为通过。此前 63 项批次
仍为 57/63，六项失败与模型 HTTP 401 边界仍保留；M0--M4 和真实模型聊天到动作
继续 `NOT_RUN`，当前源码仍 `DIRTY_NO_COMMIT`。

导航修复后的完整包已重新生成：JVM 1005（1005 total、0 failures、0 errors、2 skipped）、
Python E2E 20/20，产品 fat/slim SHA-256 为
`a3ef87d39f8c204b9b04ee3b414b4a057124af8fef0b31fe48dfa4303591fd8d` /
`3aaef97bb59d5b0463c24f70be3702d3a6d01774c9d2567f71a202a0b6580066`。精确产品
专服 smoke `20260809T041117Z-e2ee0a41b042bcf` 在 Forge 65.0.0 启动、Jar-in-Jar
SQLite、headless ServerPlayer、精确哈希和优雅退出均通过；oracle 因没有模型凭据
按设计失败，`functionalAiClaim=false`。

同一导航修复随后在 Forge 65.1.0 复核：`run-debug72` 庇护所探索 1/1（17.39 秒），
`run-debug73` 对角绕行 1/1（3.439 秒）。这只是当前源码的关键场景定向证据；完整
65.x patch matrix、Forge 66、真实模型和 M0--M4 仍未通过或未运行。

当前开发工件已推进到 `0.1.5-dev-mc26.2`。固定 Temurin JDK 25 下最新完整包为
1004 个 JVM 测试用例（1004 total、0 failures、0 errors、2 skipped），Python E2E
20/20，兼容性校验与 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过。产品
JAR SHA-256 为
`6b7c7d28bb87209d7d50443ea661f2e1f0f861fafba8bca0e05f62f078481a56`，slim 工件为
`3ea8c8c94d1282259a1d9d90875cdf125c78d5cc99860d394a4da121b72dd2ca`。精确产品
专服 smoke `20260809T033729Z-e2eb8f364fe1c3d` 在 Forge 65.0.0 通过生命周期，
但 `functionalAiClaim=false`；工作树仍为 dirty，真实模型/客户端及 M0--M4 仍未运行。

最新交互修复增加低风险本地即时社交回复：普通单人聊天中的 Hello/Hi、询问是否说中文或
英文不再等待一次高层规划；这不是模型回复，也不绕过模型可用性检查，任务和动作仍必须
走真实模型或公平本地应急车道。跨实例凭据回归同时通过，验证进世界后可从持久化凭据存储
恢复 API Key。Forge `run-debug58` 的 10 僵尸＋10 骷髅真实物理应急回归 1/1 通过。
E2E 编排器按当前 `build.gradle` 版本选择工件，旧开发 JAR 留在构建目录也不会被误用。

按用户要求实际启动了一个模型链路 GameTest
`real_player_task_to_live_model_movement`：隔离目录缺端点的首次运行被正确拒绝为
`INVALID_CONFIGURATION`（0 请求）；注入非秘密端点/模型后，Keychain 恢复的凭据走到
真实 MiMo capability probe，但服务端返回 HTTP 401 `Please provide valid API Key`。
这不是脚本模型结果，因而真实聊天到动作与 M1--M4 仍保持 `NOT_RUN`，没有继续重试或
伪造通过。替换有效 Key 后可复用同一选择器继续黑盒测试。

新增跨实例凭据回归验证首个服务端管理器保存后，第二个服务端管理器可以从同一持久化
存储解锁 API Key；该测试不读取或写入用户秘密。真实 `restart-smoke` 已完成双启动归档
`20260809T022742Z-two-boot-audit`：同一精确
fat JAR、同一世界、同一 companion UUID，生产 SQLite 的四条 lifecycle 行和单调
修订校验通过。formal `e2eRestart` 在 dirty 源上仍为 NOT_RUN（底层 verifier PASS，
不可提升为发布门禁）。

重启证据边界已实现：启动/停止时的 `runtime_lifecycle_audit` 只保存稳定 UUID、修订号、
SavedData 状态和 schema 版本；`e2e/restart_gate.py` 要求同一归档有两次启动、停止、
UUID稳定和单调修订号，单次 smoke 会失败，缺归档的 formal `e2eRestart` 保持 NOT_RUN。
新包的 Forge 65.0.0 精确 smoke `20260809T022349Z-lifecycle-audit-smoke` 已实际落盘
一次启动和一次停止行，重启 verifier 正确拒绝它作为双启动证据。

Xaero 共享点目标的规划边界已收紧：服务端持久化且获授权的同维度坐标会被提示为
立即 `START_SKILL move_to`，跨维度只允许基于已验证传送门边的 `travel_to`；没有
传送、命令或隐藏小地图读取。新增规划回归通过；这改善坐标任务的动作选择，但不
冒充真实模型或第三方 Xaero UI 的端到端门禁。

包传输审计已接入生产运行时：真实 `HeadlessConnectionPump` 的出站队列高水位、未释放
包数、keepalive/传送/区块批确认和最终 disconnect 回调会进入 SQLite
`connection_transport_audit`；外部 verifier 与 `PACKET_LEAK` mutation 直接消费该
事件。当前 `compileJava test`、`check verifyReleaseJar e2eClientJar e2eOracleJar`
通过，Python E2E 16/16，mutation 10/10；fat/slim SHA-256 为
`1f6516eed3f707f6b3090f522bcdff29c96aec2f2393feedb3d3a1c21364e20c` /
`219bf5c6eb17f54d6158a533e8f0bea1713150cdb5b4a476f50a9bd70de2d976`。这仍是
`DIRTY_NO_COMMIT` 的源码与确定性故障证据，不是实时模型、双客户端、Hardcore 或
M0--M4 的正式通过。

精确 fat JAR 专服 smoke `20260809T021444Z-transport-audit-smoke`（Forge 65.0.0）
又验证了该事件落盘：运行中和最终断开各有一条生产 transport audit，最终
`disconnectHandled=true`、`unreleasedOutboundPackets=0`，产品哈希一致；这是
生命周期审计证据，`functionalAiClaim=false`。

库存门禁已补上真实 Forge `PlayerEvent.ItemPickupEvent` 来源证明；直接写背包或只删除
掉落实体不能再通过。`e2e/mutation_gate.py` 已接入 formal `mutationGate`，10/10
故障变体被捕获；Python E2E 16/16，`check verifyReleaseJar e2eClientJar e2eOracleJar`
在固定 JDK 25 下通过。该证据仍是确定性故障注入与测试工件验证，不提升实时模型、
真实客户端、Hardcore 或 M0--M4 门禁。

本次变更后的账本/兼容声明/Python E2E、固定 JDK 25 `verifyReleaseJar` 和通信/凭据定向
JVM 测试均再次通过；这些仍是源码与工件证据，实时模型、真实客户端、Hardcore 及
M0--M4 门禁保持未运行。

最新通信边界修复：模型即使误把闲聊/语言问题返回为 `ASK_PLAYER`，也不能在服务端
凭空创建游戏目标。单人聊天仍不要求 `@`；多人未寻址消息不能写目标，模型只会在
非疑问且已明确寻址时获得任务候选权。本轮定向测试、完整 JVM 998/0/0/2、Python
E2E 15/15 均通过。最新 fat/slim SHA-256 为
`824b56125e734958e03aaed090167e667a6fe6037432ad4f379243302d7fed61` /
`303f6ff1f8dda125de76358bef086c301cd2f908a0fccf09cf533da56326cd79`；精确专服
smoke `20260809T013915Z-e2e9503d501c9cc` 生命周期通过，但无有效模型的 Oracle
仍失败，`functionalAiClaim=false`。

通信边界改动后的物理回归同样通过：`offline_idle_equipment` 在当前源码的
`run-debug49`、Forge 65.0.0 以 1/1、约610.3 ms 完成铁帽/盾牌原版菜单穿戴；
`real_emergency_zombie_skeleton_horde` 在 `run-debug50` 以 1/1、约1.203 s 完成
十僵尸＋十骷髅压力场景，身体移动、存活并造成伤害。两项均为无模型受控本地证据，
不等于模型聊天、真人 PVP 或 M0--M4 通过。

上一轮进展：无模型时的本地装备维护已通过真实 Forge 65.0.0 GameTest，而不再只有
源码断言。隔离 `run-debug49` 的 `offline_idle_equipment` 在约610.3 ms 内让真实
headless `ServerPlayer` 使用原版背包菜单穿戴已拥有的铁帽和盾牌，1/1 required
tests passed；高层移动、技能和聊天仍在没有模型时停用。新增夹具与数据资源见
`EmbodimentGameTests.java`、`test_instance/offline_idle_equipment.json` 和
`test_environment/exclusive_offline_idle_equipment.json`。

上一轮包门禁为 JVM 998/0/0/2、Python E2E 15/15，fat/slim SHA-256 为
`9535d4d4d8cd21a5bf2f91fac501d63176d45998f37bdbfe1ffcc0fe82c495fc` /
`e36984abe405aa08ca75eac7f78b23dbbf22a82e891f8ebd76f810aebf887522`。精确 fat JAR
专服 smoke `20260809T013113Z-e2efe9f19e6c9d4` 生命周期通过，但没有有效模型的
独立 Oracle 仍诚实失败，`functionalAiClaim=false`；正式实时模型、真人客户端、
随机 Hardcore、M0--M4 仍未运行。

历史修复：模型未就绪不会授权高层移动/技能/聊天，但低风险的
已有物品装备维护仍走原版背包菜单，能穿护甲升级、装备盾牌和更好的普通武器。
这直接覆盖“收到物品却不穿”的本地动作缺口；源码契约测试、完整包和当前 Forge
65.0.0 十僵尸十骷髅压力场景 `run-debug45` 通过。最新 fat/slim SHA-256 为
`9535d4d4d8cd21a5bf2f91fac501d63176d45998f37bdbfe1ffcc0fe82c495fc` 与
`e36984abe405aa08ca75eac7f78b23dbbf22a82e891f8ebd76f810aebf887522`，精确 JAR
smoke `20260809T012442Z-e2ee1bb30e5c8da` 生命周期通过，但无模型 Oracle 仍失败；
正式实时模型、真人客户端、随机 Hardcore 与 M0--M4 不变。

本轮继续修复单人聊天体验：集成服中唯一真人的普通聊天现在由服务端明确视为已
寻址消息，不要求 `@` 或 Agent 名；多人专服仍只把显式 Agent 地址当作回复授权，
避免抢答他人聊天。`ChatAddressingTest` 与通信定向测试通过。

该源码随后重新通过 `check verifyReleaseJar e2eClientJar e2eOracleJar`：JVM
998/0/0/2，Python E2E 15/15。当前 fat/slim JAR SHA-256 为
`c2782fed27256521f5381306236cde2517b1a829d3fe41e3ffe5e43b7a7e654e` 与
`10b263f1452ba1ee82c2c93a637a0d0aa5cda3dffe7fd3ccec9b39d36c6a83a7`。
Forge 65.0.0 当前源码 `run-debug44` 的十僵尸十骷髅压力场景 1/1 通过；精确
JAR 专服 smoke `20260809T011912Z-e2eae301135c205` 生命周期通过，但无有效
模型的独立 Oracle 明确失败且 `functionalAiClaim=false`。正式模型、真人客户端、
随机 Hardcore 与 M0--M4 仍未运行。

又修复了规划层的一个“光说不做”分支：玩家聊天目标已存在时，模型若返回无问号的
行动承诺型 `ASK_PLAYER`（例如“我这就来”“I'm on my way”），运行时不再把身体
锁进 `WAITING_FOR_PLAYER`。它会保留目标、抑制空头话术、携带 `planner_no_action`
纠正并短退避重试真正的 `START_SKILL`；带明确问题的澄清、Hardcore 锁定目标和
普通不确定语句仍保持安全等待。该恢复仅改变规划状态，不选择技能、不写世界。新增
BrainOrchestrator 回归通过，仍不是实时模型或 M0--M4 证据。

本轮针对现场“跟随时只转头、不走路”的实际停滞根因继续修复：
`FollowEntitySkill` 不再把局部扫描/转身当作身体进度。若当前跟随目标仍公平可见，
但身体连续80 tick（4秒）没有至少0.08格的真实位移，技能安全返回
`follow_entity.no_physical_progress`，交由监督器结束原子技能并重新取样/规划；
目标从短暂视野丢失后重新出现会重置该计时。该修复不写位置、不传送、不读隐藏实体
坐标。新增 `FollowEntitySkillTest` 已通过；这只是当前源的动作停滞回归，尚未提升
正式模型、真人客户端、随机 Hardcore 或 M0--M4 状态。

本轮修复设置页窄窗口布局：侧栏、表单、配色/温度控件、保存/返回按钮和教程
控件均按可用宽度自适应，表单整个内容面板都可用滚轮滚动，滚动偏移会被夹紧。
这解决了窄屏字段越界和“滚轮没有反应”的 UI 断点；仍不把单Agent首发后端
伪装成未实现的多Agent列表。定向设置/皮肤网络测试通过；源改动后的完整
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，Python E2E 为15/15。
当前 fat JAR SHA-256 为
`e1b37b4458c8c5c933d715bc1fd33db01cec18c682067db889e61dc60a4e97bf`，slim 为
`be2ac128963361f51f82745c7d6a24360e9fada5635ab4e8b6399f3577b35087`；精确
Forge 65.0.0 smoke `20260809T005023Z-e2edac5b8bfdf42` 的生命周期检查通过，
独立 oracle 因无有效模型凭据为 FAIL，`functionalAiClaim=false`。正式模型、
Linux/Xvfb 真人客户端与 M0--M4 继续 `NOT_RUN`。

随后修复暂停菜单按钮锚点：本地化后的“回到游戏”如果以 literal 组件出现，
现在按最终显示文本匹配；仍无法识别时从最上方全宽主按钮推断同列位置，不再用
会覆盖“游戏菜单”标题的固定 y=54。当前源改动后的完整包与 Python E2E 仍通过，
fat SHA-256 为
`e84b41a5fc26389069470937c6b5169f8cde441ca41049976b644998e69c9070`，slim 为
`fddb88a5f944b23492753c25ae4f7ecec535ba0ed78b01e2ac8264e2ee260672`；精确 Forge
65.0.0 smoke `20260809T005504Z-e2ef47399ffee9f` 生命周期通过，oracle 因无有效
模型凭据失败且 `functionalAiClaim=false`。

当前源码随后在隔离 `run-debug40`、Forge 65.0.0 重跑真实
`real_emergency_zombie_skeleton_horde`：十只僵尸和十只骷髅压力场景 1/1
通过（1.243 秒），确认暂停菜单与响应式设置页改动没有影响 20 TPS 本地应急
战斗车道；这仍是无模型受控 PVE 压力证据，不是 PVP、随机 Hardcore 或 M0--M4
通过。

按用户指定的另一项场景，当前源码 Forge 65.0.0 隔离 `run-debug41` 的
`real_emergency_iron_golem_duel` 也通过 1/1（934.5 ms）。它只证明受控铁傀儡
压力下的原版身体战斗响应，不是模型驱动的真人 PVP 胜率。

随后在当前源码/Forge 65.0.0 隔离 `run-debug42` 重跑
`auto_presence_on_human_login`，1/1 通过（597.6 ms）：真人测试玩家先登录，
AI 身体正常加入安全邻近位置，TAB 身份和无模型静止约束通过；这仍是身体
生命周期证据，不是高层聊天/移动或 M0--M4 通过。

最新正式预检（`2026-08-09T01:00:41Z`）仍为 `ready=false`：当前主机 Darwin，
没有隔离 Linux/Xvfb，也没有 `MCAI_BASE_URL`、`MCAI_MODEL` 或有效凭据环境。
预检不触碰物理显示器、不启动真人客户端、不写入密钥，正式 live/随机种子门禁
继续 `NOT_RUN`；恢复条件是配置隔离 Linux/Xvfb 与有效模型后重新运行
`e2e/formal_gates.py`。

本次继续补做真实实体压力场景：`real_emergency_iron_golem_duel` 与
`real_emergency_zombie_skeleton_horde` 分别通过 1/1。后者在普通 Forge
GameTest 场景中实际生成十只僵尸和十只骷髅，身体存活、移动并对多个目标造成
伤害；前者修复了中立铁傀儡受击后的有限追击租约丢失。首个铁傀儡运行曾真实
失败并已保留在检查点，后续只把门禁定义为有界压力响应，不把受控场景冒充
完整 PVP 胜率。完整 JVM 套件现为 998 项（0 失败、0 错误、2 跳过），Python
E2E 审计 15/15，新的 fat JAR SHA-256 为
`9535d4d4d8cd21a5bf2f91fac501d63176d45998f37bdbfe1ffcc0fe82c495fc`，slim 为
`e36984abe405aa08ca75eac7f78b23dbbf22a82e891f8ebd76f810aebf887522`。
精确 JAR 专用服务器生命周期 smoke 通过（run
`20260809T003432Z-e2e811774ddd5e0`）；其中无有效模型凭据的 oracle 仍明确为
`FAIL`，总 smoke verdict 的 `functionalAiClaim=false`，因此没有把“JAR 能加载”
写成“AI 已经会聊天和行动”。

这些新增证据仍是无模型、无渲染客户端、受控场地测试，不提升 M0--M4、随机
Hardcore 两小时通关、真人级陪玩或正式 PVP 声明；正式模型/客户端/隐藏种子
门禁继续保持 `NOT_RUN`。

针对现场的“光说不做”症状又补了一层严格受限的动作恢复：对玩家聊天安装的
`serverBoundPlayerName` 跟随目标，如果当前公平语义样本确实看到了该同名、非
敌对玩家，即使模型返回一次合法的 `CONTINUE/REPLAN/ASK_PLAYER`，运行时也会把已知目标
恢复为 `follow_entity` 高层决策，再走原有技能前置、观察绑定、局部 A* 和原版
玩家移动；看不到目标、名称不匹配或样本损坏时不会猜测。`BrainOrchestratorTest`
目前37项通过，普通目标的 speech 抑制和退避测试仍保持通过。这不是模型实战
通过，不能替代有效凭据和真实客户端门禁。

该窄恢复路径已重新打包并用精确 fat JAR 在 Forge 65.0.0 专用服务器上做了
生命周期 smoke：同一 run 的加载、零真人身体、Jar-in-Jar SQLite、清理和哈希
一致性均通过；独立 Oracle 因没有有效模型凭据而为
`FAIL/server_stopped_before_result`，所以 `functionalAiClaim=false`。

发布前又在精确源码上重跑了真实 Forge 65.0.0 的
`real_emergency_zombie_skeleton_horde`（`run-debug39`）：十只僵尸和十只骷髅
压力场景 1/1 通过（1.249 秒），确认本次 ASK_PLAYER 跟随恢复没有关闭应急战斗车道。关闭
时的嵌入连接 fallback 仍是已知清理警告，不是断言失败。

随后在隔离的 `run-debug37`、Forge 65.0.0、无真人服务器中重跑
`zero_human_dedicated_server_chunk_and_respawn`：1/1 通过（1.816 秒）。身体
从远端测试区加载、正常加入、接受原版伤害死亡并完成受控重生/重新加入；关闭
时的嵌入连接 fallback 警告属于预期拆除，不是测试断言失败。该结果仍只是
生命周期证据，不是模型、渲染客户端、随机 Hardcore 或 M0--M4 通过。

本轮继续复验了末地交互和无真人可用性：末地传送门激活技能现在复用同一
个公平第一人称命中点，避免语义射线在转身时切换方块面造成“看到了但放不
进去”；清理诊断后的 `real_end_portal_activation`、`real_end_victory_and_return`
分别通过 1/1，后者实际进入末地、击杀受控龙并通过中心传送门返回。最新
`zero_human_dedicated_server_chunk_and_respawn`、`auto_presence_on_human_login`
和两项应急敌对生物防御在 Forge 65.0.0/Minecraft 26.2 均通过。完整 JVM
套件与 Python 审计通过，fat JAR 已重新生成并由精确专用服务器 smoke
验证；其 `functionalAiClaim=false` 仍然是有意的，因为当前 MiMo 凭据独立
探测为 HTTP 401。

这些是受控服务端和生命周期证据，不提升 M0--M4、自然随机种子、真实客户
端或当前模型可用性状态；正式矩阵继续保持 `NOT_RUN`，直到有有效凭据和
专用 Actor/Observer/Hardcore 评测环境。

设置界面本轮还修复了一个重进世界时的本地凭据竞态：服务端现在先在工作线程
完成一次 Keychain/Secret-Service/注入凭据恢复，再向设置界面返回无密钥状态，
因此不会在恢复完成前误报“缺少 API Key”。该路径不发模型请求、不把密钥返回
客户端，也不写入世界；`ModelRuntimeTest` 已定向通过。真实 MiMo 凭据的单请求
探测仍为 HTTP 401，真实客户端与 M1–M4 门禁仍保持 `NOT_RUN`。

设置页状态还不再把运行时的 `api_key_rejected` 覆盖成 `ready`；真实认证失败会
明确显示原因，身体仍保持安全静止。该显示修复由 `ModelRuntimeTest` 覆盖，不会
改变凭据值或自动重试供应商请求。

认证失败还会进入当前运行时的凭据隔离状态：401/403 后清除进程内旧值，阻止
Keychain、DPAPI、Secret Service 或环境注入在同一运行时静默恢复旧凭据，设置快照会
把凭据标为不可用。系统不会自动删除持久密钥；用户必须在设置页显式保存替换值，空
凭据更新会返回 `api_key_rejected_requires_replacement`。替换保存成功后才解除隔离并
允许再次能力探测。这避免了“设置页看似保存、重进世界又继续使用坏 Key”的循环。
首次“保存并验证”本身收到 401 时也走同一隔离路径，不会只显示一次失败后把旧 Key
留在下一次探测中。
该路径由 `ModelRuntimeTest` 覆盖；真实模型凭据探测目前仍被 provider 拒绝，不能据此
宣称模型聊天或游戏动作已通过。

凭据管理器还串行化持久存储的保存、恢复、清除和关闭，避免世界启动读取旧值与
设置页保存新值并发时旧值回写进程缓存；`ApiKeyManagerConcurrencyTest` 已覆盖该
顺序。`acquire()` 仍不持锁，不会阻塞游戏 tick。

兼容性声明本轮已迁移为可审计的 `compat/forge-lines.toml` schema v2：使用
`[[line]]`、`forgeMajor`、`minimumForge`、`recommendedForge`、`module` 和
`status`，并把编译证据、生命周期烟测证据与正式矩阵未验证集合分开。新增
`scripts/validate-compat.py` 在当前 Python 3.9 fallback parser 下通过。真实
Forge 运行时的零真人出生/死亡重生与真人登录自动出现两项 GameTest 已在
65.0.0、65.0.8、65.0.9、65.1.0 各通过1/1（共8/8）；这只是身体生命周期
烟测，不是11个已发布65.x补丁的聊天/移动/菜单/保存正式矩阵，正式兼容门禁仍
为 `NOT_RUN`。

2026-08-07 又将当前源代码分别编译到全部11个已发布 Forge 65 补丁
（65.0.0–65.0.9、65.1.0），并写入 `compileVerifiedPatches`。这只扩大了
编译证据；每个补丁的精确 JAR 加载、聊天、移动、菜单、保存/重启和真实客户端
回归仍未完成，不能把编译通过写成完整兼容。

本轮先在真实 Forge 65.1.0 零真人服务器门禁中发现并停止了一个真实阻断：
未锚定身体通过原版 `PrepareSpawnTask.Ready.spawn` 进入
`ServerLevel.waitForEntities` 后，服务器线程超过四分钟没有继续 tick。原因不是
模型响应，也不是 GameTest 断言超时，而是无网络登录配置任务时原版同步实体等待
不适合 headless 身体。现在 `PendingPlayerSpawn` 与 `AnchoredPlayerSpawn` 统一走
异步安全锚点加载，再由原版 `ServerPlayer`、`PlayerList.placeNewPlayer` 和玩家
数据恢复完成交接，仍保留首次真人登录的40 tick重锚窗口；未提供真人时优先恢复
保存的位置/维度，否则使用世界出生点。修复后的真实 Forge 65.1.0
`zero_human_dedicated_server_chunk_and_respawn` 通过1/1（1.670秒），
`auto_presence_on_human_login` 通过1/1。此次源改动后的完整包现已通过989项
JVM测试（0失败、0错误、2跳过），`jarJar`、`verifyReleaseJar`、E2E Client/Oracle
工件均通过；产品 JAR SHA-256 为
`d903b44c943de7d17adcf130044fdbf5466d71224a0d3bedc0e96a6a0f764d58`，slim 为
`18083297d1ae2b243d28e8fd1a10c73040470ebe2d941e0d55b158aed1692526`。精确 JAR
服务器烟测 `20260806T152134Z-e2e26cc46bb701f` 在 Forge 65.1.0 通过，服务端、
Actor、Observer 三份产品哈希一致，SQLite Jar-in-Jar 和生命周期通过，且
`functionalAiClaim=false`；Python E2E 审计15/15通过（含兼容声明与旧
schema拒绝测试）。

当前源码随后在真实 Forge 65.1.0 无模型凭据下再次通过
`zero_human_dedicated_server_chunk_and_respawn` 1/1（1.724秒）和
`auto_presence_on_human_login` 1/1（649.9毫秒）；这仍仅是 ServerPlayer
生命周期烟测，不是模型、聊天、移动或 M1–M4 门禁。

本轮继续修复了一个单人登录时的身体出生竞态：服务器启动可以先为零真人
场景创建未锚定的 `PendingPlayerSpawn`，但此前首个真人登录时会直接保留这
个准备任务，导致 AI 可能在世界出生点完成而不在玩家旁边。现在只有在仍处于
`PREPARING` 且尚未锚定时才取消并重新使用 `SafeCompanionSpawnLocator` 以玩家
为锚点准备；已 `ACTIVE` 的身体不会因登录被搬动。`auto_presence_on_human_login`
在 Forge 65.1.0 的真实 GameTest 通过 1/1，TAB `[AI]` 身份和无模型静止约束
均保持通过。该阶段重新打包为985项 JVM 测试（0失败、0错误、2跳过），产品 JAR
SHA-256 为 `85c227c3d3161f465499cbb2171b94658ee9dfdba5917f4fc71251705fdeea60`；
精确 JAR 烟测 `20260806T134602Z-20260806-login-anchor-final-smoke` 通过，仍
仅是生命周期证据，`functionalAiClaim=false`。

本轮又补齐了正式门禁的可审计入口：`build.gradle` 现在注册计划要求的17个
门禁任务，`e2e/formal_gates.py` 会把每次调用写入 `e2e/results/formal-gates`
并严格区分 `PASS`、`FAIL`、`NOT_RUN`。其中功能/渲染/聊天/移动/物品五个
入口调用真实 Actor+Observer runner，其余重启、Xaero、变异、长时间和随机种子
入口在专用场景尚未完成前明确返回 `NOT_RUN`。本机实际执行的 `e2eChat` 因
Darwin 无 Linux/Xvfb 且无有效模型环境而被归档为 `NOT_RUN`，没有改写任何
里程碑状态。

本轮还修复了跟随技能的一个真实延迟竞态：`follow_entity` 现在可以依据
最近一次同维度、非敌对的公平观测绑定目标；如果模型响应期间目标暂时离开
最新视野，则进入已有的有限 `SEARCHING` 阶段，而不是在启动前返回
`target_not_currently_visible`。搜索仍受 `lostGraceTicks`、第一人称扫描和
可见性过滤约束，不读取隐藏坐标、不传送。定向跟随回归通过，完整包为984项
测试（0失败、0错误、2跳过），产品 JAR SHA-256 为
`e82c63f4b303236621b202adb9d77e22fea43e1d963c926503a30e8b32c352d1`；精确
JAR 烟测 `20260806T133127Z-20260806-follow-search-final-smoke` 通过，仍
仅是生命周期证据，`functionalAiClaim=false`。

同时修复了功能 runner 的审计缺口：前置条件不足时也会先生成隔离 run，保存
无密钥的 preflight 与基础设施错误，而不是只抛异常导致这次尝试不可追踪。

另外修正了一个会造成“光说不做”观感的真实话术边界：本地高置信聊天分类
现在只说“任务已经创建；我正在规划第一个动作”，只有技能成功启动后的事件
才允许动作性播报。它不替代真实模型执行，但避免在路径失败、等待或网关失效
时提前声称已经开始移动；新增源契约已通过。

本轮继续修复了一个会放大 API 失败影响的运行时边界：模型网关未验证时，
服务端现在只停用高层技能、闲置装备和聊天控制，但仍运行公平的本地应急
生存车道。它可以依据当前第一人称感知执行举盾、进食、撤退、落水救援和
必要的应急近战；不会调用模型、创建目标、说话或读取隐藏世界数据。菜单、
船和矿车控制在离线状态会被释放，恢复验证网关后才重新开放普通技能。
定向 JVM 门禁、完整 984 项 Gradle 测试和最新精确 JAR 服务端生命周期烟测
均通过。这是断网安全行为证据，不是模型玩法或 M0–M4 正式通过；当前真实
MiMo 单请求探测仍为 HTTP 401，因此不能把身体在线误报成模型已就绪。

本轮继续完成真实客户端纵向切片的构建边界：生产 JAR 与测试专用
`mcai-e2e-client`、`mcai-e2e-oracle` JAR 均可按 Forge 65.1.0 编译；新增
`python3 e2e/orchestrator.py preflight` 只读预检，用于在不启动 Minecraft、
不占用物理显示器的前提下区分 Linux/Xvfb/Java/模型凭据缺失。当前这台
macOS 主机预检报告缺少 Xvfb、模型 Base URL/名称和凭据，因此真实 Actor+
Observer 功能门禁仍是 `NOT_RUN`，预检不构成任何游戏能力通过。

另外修复了一个启动恢复边界：如果桌面 Keychain/Secret Service 在世界已
加载时暂时不可用，身体会保持在线但模型控制暂停；普通世界现在每100个
服务端 tick 只重试本地凭据解锁，不会对已经返回401、计费或限流的供应商
请求自动重试。这样不会把“在线身体”误报成“AI已就绪”，也避免因为暂时
锁定的系统密钥环要求玩家重新输入 API Key。

## 发布判断

当前代码是 `0.1.5-dev-mc26.2` 工程验证版，不是最终版，也没有达到“专业游戏陪玩”或“两小时随机种子极限通关”的产品声明门槛。

本轮继续修复了一个实际的停滞根因：玩家在模型曾返回
`ASK_PLAYER` 后发送新的普通聊天或“走啊”时，脑体不会再永久停在
`WAITING_FOR_PLAYER`；新消息会唤醒规划器并清除旧的退避状态。该修复已有
定向 JVM 回归，但还没有被计入 M0–M4 退出门禁。

本轮还真实复现并修复了一个 Enderman 应急战斗门禁失败：首次最终源码
重跑在第236 tick 于 `BRACING_FALL` 中死亡；原因是瞬移/击退后的最近受击
方向没有直接用于盾牌朝向。现在有公平受击方向时先朝攻击来源举盾，只有
没有方向时才使用有限扫描。控制器 JVM 测试通过，Forge 65.1.0 的
Enderman 物理门禁随后暴露了第二个稳定问题：攻击冷却分支的 `stop()`
清掉了脚步输入，且瞬移后没有目标样本。现在以不含实体身份的短时方向
记忆保留战斗脚步租约；Forge 65.1.0 的 Enderman 物理门禁连续 3/3
通过，Slime 对照门禁也通过。这仍是局部生存证据，不是模型或随机
Hardcore 统计证据。

实现进度已经明显超过早期移动演示：Headless `ServerPlayer`、Agent
设置中心、密钥边界、皮肤分发、公平感知、SQLite 记忆、菜单驱动和
生存/交通/战斗/维度技能都已进入生产注册表。本轮最新完整 package 已完成
998 项 JVM
测试，并注册62个MCAI测试函数。此前19项required test的整批结果仍是
发布基线；新增门禁和本轮物理修复之后，62项尚未作为一个批次全部重跑，
因此不能把旧的19/19写成当前整版通过。此前六个线上模型游戏门禁曾在
同一无窗口 Forge 服务端运行过，真实覆盖自然对话、坐标步行、僵尸战斗、
三段跑酷、生存模式落地水，以及小麦收割、掉落拾取和补种；这些是历史
样本，不是当前凭据的可复验结论。本轮另有突袭战斗、危急金苹果和零真人
服务端等定向门禁证据。当前配置的 MiMo 单请求探测为 HTTP 401，因此真实
模型聊天、移动和生存正式门禁仍记为 `NOT_RUN`。原计划 M0 的24小时
门禁仍未执行，M1/M2 也没有自然随机种子统计；因此不能用受控夹具或
“技能已注册”替代里程碑退出标准。

## 里程碑对照

| 里程碑 | 当前判断 | 证据与缺口 |
|---|---|---|
| M0：技术门禁 | 实现广泛，退出门禁未完成 | 单 JAR、Java 25、Headless 玩家、SQLite、保存/重登和受控跨维度已验证；缺 24 小时连续运行、两个真实客户端、完整重启/跨维度/菜单矩阵 |
| M1：基础生存 | 部分实现，统计未开始 | 已有独立隐藏种子 foundation 评测路线、服务端阶段证据与完成防伪；工作台/熔炉/箱子和入箱事务、动态庇护所与跨日均需服务端复验，安全食物、石镐和铁制生存套装还会按当前背包动态撤销。路线与紧急控制器共用安全食物目录，且庇护所复验完成后不再重复索取结构块、门和光源；若证据失效则恢复需求。路线只为 foundation 目标注入紧凑条件式流程。采集、合成、农业、睡眠、运输和基础战斗技能已注册。未执行 100 个未见 Hardcore 种子的零干预验收 |
| M2：完整通关 | 受控链通过，自然验收未完成 | 大型夹具链已重新通过下界往返、受控烈焰棒、末影之眼、末地门、末地路线、受控龙战与返回；仍缺自然堡垒、自然资源经济、自然要塞/门房和动态龙的端到端路线 |
| M3：专家陪玩 | 未通过 | 只有部分原子能力；全部工作站、长期基地、完整建筑/红石/农场/PVE/PVP 与用户场景矩阵未完成 |
| M4：两小时 RSG | 未开始统计 | 没有隐藏随机种子完成样本，不能报告两小时或六小时成功率 |
| M5：模组适配 | 仅有 SPI/契约骨架 | 没有通过精确版本门禁的 Create、MTR、农夫乐事专用适配器 |

## 子系统状态

| 领域 | 状态 | 当前已验证范围 |
|---|---|---|
| Minecraft 26.2 / Forge 65.x / Java 25 / 单 JAR | 已实现 | 以65.0.0为最低编译基线，运行范围为`[65.0.0,66.0.0)`；65.0.0、65.0.8与65.0.9曾通过当时的完整发布基线，当前新增22项整批尚未重跑；Forge Jar-in-Jar内嵌SQLite；当前仍是dev版本 |
| Headless `ServerPlayer` | 受控门禁通过 | 持久 UUID、登录、移除、同 UUID 重登、`PlayerList`、连接确认、状态恢复、动作租约和清理；真人登录会自动在原版校验的附近安全点创建身体，无模型时身体仍在线但保持静止 |
| 原版玩家语义 | 部分通过 | 背包/副手/护甲/末影箱/生命/饥饿/经验、普通移动、挖掘、工具耐久、物品使用和菜单点击；未完成全部工作站与多人观察矩阵 |
| 极限永久死亡/评测锁 | 已实现约束 | `SavedData` 死亡锁、初始目标冻结、外部写入封锁；缺随机 Hardcore 统计和大规模死亡原因矩阵 |
| 单模型 HTTP 网关 | 已实现并测试 | Responses/Chat、结构化输出降级、single-flight、正文硬超时、取消、revision、防重复扣费切换和脱敏；0.0–1.0采样temperature随真实请求发送；本地技能启动拒绝原因会以安全代码反馈给下一次规划，并在成功启动或换目标时清除；429无`Retry-After`时采用10/20/40/60秒退避，保留目标和本地安全循环，并向在线玩家说明正在重试 |
| Agent 设置 UI | 单 Agent 设置中心已实现，多 Agent 未完成 | Esc/Mods 原版风格入口，名称/配色/温度/皮肤/模型/系统偏好、四步引导与首次交互；小窗口/高GUI缩放可滚动，底部操作不与表单重叠，暂停菜单入口与“回到游戏”同列；服务端名称冲突、权限、加密连接、一次性令牌、保存后 probe 和极限冻结有测试。仍只有一个活动 Agent，缺独立多身体目录 |
| API Key 存储 | 已实现跨平台边界 | macOS Keychain；Windows/Windows Server当前用户DPAPI密文；Linux桌面Secret Service；Debian/容器支持`MCAI_API_KEY_FILE`与systemd`CREDENTIALS_DIRECTORY/mcai-api-key`；另有进程内可擦除存储和环境变量回退。显式外部注入优先于旧安全存储，便于无交互轮换；密钥不进入TOML/世界/SQLite/日志，世界启动会异步解锁或注入凭据 |
| 公平语义感知 | 已实现核心 | 第一人称视线/遮挡、视觉与碰撞语义分离、实体交互视线、菜单/掉落/危险属性和样本失效 |
| 截图视觉 | 未完成 | `get_screenshot` 明确返回不可用；尚无经过认证、脱敏验证的客户端捕获通道 |
| SQLite 记忆 | 已实现核心 | WAL、事件、checkpoint、FTS5、RTree、revision、TTL、软删除、跨世界/维度隔离和传送门边 |
| Xaero | 已实现结构化入口 | 26.4.2/26.4.3 共享标点解析、来源限制和正常寻路目标；未做第三方 UI 侧端到端人工回归 |
| MCP | 已实现核心 | 回环、Bearer、Host/Origin、请求上限及八个工具；截图工具如实不可用 |
| 自定义皮肤 | 已实现代码/协议 | 64×64 alpha PNG、1 MiB 上限、SHA-256 缓存、经典/纤细手臂、分块同步和 UUID 稳定史蒂夫/艾利克斯回退；缺两个真实客户端全动画人工门禁 |
| 核心移动/探索 | 已实现局部与滚动路线 | `move_to`、`travel_to`、`follow_entity`、已观察目标探索、环境勘察；没有自然世界全局路线统计 |
| 船/矿车 | 受控 GameTest 通过 | 正常进入与驾驶受控水道/铁路；复杂自然交通网和长期路线恢复未验证 |
| 搭桥/垫高 | 受控真实链通过 | 搭桥的真实放置、材料消耗和跨越，以及垫高的原版跳跃/放置均由大型链通过；自然悬崖、敌对干扰与长期路线统计未完成 |
| 落地水/落地救援 | 独立受控 Forge 通过；当前线上模型门禁未运行 | 除3/3原版物理门禁外，历史曾有真实玩家聊天→MiMo→生产紧急控制器样本完成12格下坠、背包换桶、真实水源、空桶、使用统计与无伤；当前凭据 HTTP 401，不能把历史样本写成当前模型证明。下界水被拒绝 |
| 跑酷 | 独立受控 Forge 通过；当前线上模型门禁未运行 | 本轮先复现两格缺口失败，再以原版空中反向输入提前制动修复；修复版复杂课程连续2/2通过。历史曾有聊天→MiMo→`parkour_to`样本完成连续缺口；当前凭据未复验，不宣称 Neo、盲跳或自然世界成功率 |
| 采集/农业/庇护所 | 原子能力及首个现场农田施工链已实现 | 可见资源簇、成熟作物单格收获补种、现场庇护所与坐标无关水合农田规划/施工已进入生产注册；单场地真实 Headless GameTest 完成一水源、八耕地、八小麦，并在测试加速的原版随机刻下验证 8/8 水合、8/8 光照及正生长年龄。尚缺整田维护、默认产率、重启/区块卸载、故障维修和三场地泛化，完整“空背包生存到第二天”也未通过 |
| 物品/合成 | Forge GameTest 通过基础事务 | 原版 2×2 木板、3×3 木镐、装备、主副手交换和丢物；未覆盖全部配方与复杂生产链 |
| 菜单/工作站 | 通用驱动已实现，范围部分通过 | 真实箱子、熔炉、切石机和村民交易已验证；`smelt_menu_batch`通过普通菜单投入原料/燃料、等待原版烹饪并取出精确产物；M1 会记录 AI 亲自打开的工作台/熔炉/箱子精确位置与成功入箱事务，且动态撤销被拆除或清空的证据。菜单原语可操作已观察槽位/选项，但不等于所有工作站高层知识已完成 |
| 本地战斗 | 部分通过 | 可见目标近战、攻击冷却、盾牌/撤退、远程武器和由连续可见位置推断的移动目标提前量；完整自然 PVE/PVP 场景矩阵未完成 |
| 下界路线 | 受控大型链通过 | 正常传送门进入/返回、受控走廊、受控 `NoAI` 烈焰人击杀/掉落与回门已重验；独立浇筑门门禁另验证真实桶/流体/点火。没有自然堡垒发现与成功率 |
| 要塞/末地门 | 受控大型链通过 | 末影之眼实体投掷/轨迹、受控门房发现与插眼已重验；没有自然要塞迷宫发现 |
| 末地/末影龙 | 受控大型链通过 | 大型链取得 `The End?`、击杀夹具提供的满血 `NoAI` 龙、取得 `Free the End` 并完成返回；动态自然龙从未通过 |
| 建筑/红石/农场/PVP | 未达到 M3 | 有少量组成能力，没有完整专家场景矩阵 |
| 模组适配 | 骨架 | `ModAdapter`/注册表契约存在，没有生产专用适配器 |

## 当前自动化证据

### JVM 测试

当前完整 `./gradlew test jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar -Pforge_compile_version=65.0.0` 汇总为：

```text
tests=1002 failures=0 errors=0 skipped=2
```

本轮最终源码重建后的安装 JAR SHA-256 为
`37d3c6ee0976710b40f204b1fbe0d9b9618eca2f367461fb91f0ad5f0318c82f`，
slim 审计 JAR 为
`0fd56fdd31bceed8997e710b571f1a6ceaec2b3754d0d9e4fecb7ce6cc1b5226`。
精确 JAR 专用服务器烟测 `20260809T031954Z-e2effac8762a49f`
在 Forge 65.0.0 通过；该结果只证明加载、SQLite、零真人身体生命周期，不代表
模型或完整生存能力（`functionalAiClaim=false`）。Python E2E 审计为20/20通过。

默认会跳过的真实供应商测试为：

- `LiveCapabilityVerificationTest`
- `LiveProviderSmokeTest`

它们必须显式启用并从系统钥匙串或环境注入真实供应商凭据。本轮
`LiveProviderSmokeTest` 的历史记录曾通过一次能力协商和一个通过结构验证的
`SAFE_IDLE` 决策；这不构成游戏能力或长期供应商稳定性证明。本轮配置的
MiMo 凭据探测为 HTTP 401，当前模型相关正式门禁仍是 `NOT_RUN`。

### Forge GameTest

当前MCAI注册62个测试函数；下面列出已有零供应商凭据证据的稳定子集：

```text
real_water_clutch
real_parkour_course
real_portal_cast_and_light
real_furnace_batch
natural_recipe_unlock_after_log_pickup
headless_player_lifecycle_state_and_fair_action
verified_shelter_evidence
verified_foundation_evidence
real_food_animal_hunt
zero_human_dedicated_server_chunk_and_respawn
realtime_clock_contract
auto_presence_on_human_login
real_player_chat_to_live_model
real_player_chat_to_critical_golden_apple
real_player_chat_to_surprise_zombie_defense
real_player_task_to_live_model_movement
real_player_task_to_live_model_zombie_defense
real_player_task_to_live_model_parkour
real_player_task_to_live_model_water_clutch
real_player_task_to_live_model_farm_work
real_player_task_to_live_model_foundation_bootstrap
real_zero_human_dedicated_server_foundation
```

测试夹具可以搭建场地、放置结构/实体并在互不相干的阶段重置起点；
被断言的动作由生产注册技能通过原版玩家路径完成。当前可信状态是：

- 最终产品 Jar 的 exact-Jar dedicated-server smoke 在 Forge 65.1.0 通过：
  真实服务器只安装一个完整 Jar，SQLite 从 Jar-in-Jar 打开，Headless
  `ServerPlayer` 在零真人时加入并在关服时清理；该门禁明确不构成模型或
  生存能力通过（`functionalAiClaim=false`）。

- `real_water_clutch` 清理后连续 3/3 通过约 5 格主动下降和约 12 格
  紧急下坠，验证真实重力、碰撞、水源、空桶、使用统计和无伤。
- `real_parkour_course` 本轮先在两格缺口稳定复现失败，修复着陆前的
  原版输入制动后连续2/2通过连续三次一格缺口、两格缺口、90°转向、
  一格上升与第二次转向跳跃；没有传送、直接改位置或放宽成功窗口。
- `real_portal_cast_and_light` 连续三次通过，并在清理诊断后复验：
  原版桶事务、流体更新、动态模具、黑曜石框架、模具清理和打火石点门
  均成立。
- `real_furnace_batch` 在真正的原版熔炉菜单中移动原料和燃料，等待
  实际烹饪时间，再只取出观察到的指定产物；不直接填充输出槽。
- `natural_recipe_unlock_after_log_pickup` 通过自然木头拾取后的配方解锁。
- `verified_shelter_evidence` 会重新检查指定墙体、屋顶、上下门体、
  关闭状态、指定光源、地板、照明与可行走空间，并确认破墙/开门失效。
- `verified_foundation_evidence` 会检查已观察工作台、熔炉、箱子和入箱
  事务，并确认清空箱子或拆除熔炉后里程碑失效。
- 大型链在此前发布基线通过，从 Headless 生命周期、菜单与
  基础动作一路覆盖交通、搭桥/垫高、跑酷、下界往返、受控资源、眼睛
  轨迹、末地门、末地路线、受控龙战、`Free the End` 与返回。本次
  Forge65.0.0完整GameTest服务端用40.85秒通过当时全部19个required test；
  Forge65.0.9在本版发布回归中用40.79秒通过相同19项，65.0.8此前也已
 通过兼容门禁。源码当前注册62个 GameTest 函数；本轮最新完整 JVM
 package 通过998项，但这不等于62个真实服务端函数已作为一个批次重跑。
- `auto_presence_on_human_login` 通过真实 `PlayerList.placeNewPlayer`
  登录事件验证自动身体、附近安全落点、`[AI]` TAB身份，以及没有已验证
  模型时连续40 Tick不自行移动。
- 历史线上MiMo门禁由真实登录`ServerPlayer`的Forge聊天事件发起，而不是
  测试代码直接调用技能：同一测试服连续运行的自然对话、坐标步行、
  Zombie近战、连续三段一格缺口跑酷、显式生存模式12格落地水，以及
  三块成熟小麦的收割/拾取/补种共6项全部通过，用时约1.04分钟。这些是
  既有受控内循环证据；当前配置的 MiMo 单请求探测为 HTTP 401，因此本轮
  真实模型聊天、移动和生存正式门禁仍记为 `NOT_RUN`，不能把历史样本当作
  当前可用性证明。
  落地水同时要求原版水桶变为空桶、使用统计
  增加、真实下坠和零伤害；坐标移动要求至少产生2格真实位移并抵达目标
  半径；农业要求至少三次生产技能启动、三次原版挖掘统计、三处精确
  补种和至少三份小麦进入背包。429会退避而不丢目标。
- 独立基础生存启动门禁同样只通过普通玩家聊天发起。MiMo保留完整
  foundation目标并选择生产`gather_visible_block_cluster`，随后身体
  砍下四块相连橡木、通过公平可见掉落技能逐一拾取，并由原版挖掘统计、
  拾取统计、原坐标方块状态、背包和服务端`WOOD_OBTAINED`里程碑共同
  验证。该门禁只证明基础生存的采木启动段，不证明完整M1。
- 单独的真实20 TPS启动恢复门禁不调用测试侧探测函数：它从macOS
  Keychain自动恢复MiMo，让玩家登录触发AI身体和TAB条目，并把登录后
  立即发出的普通聊天保留到模型就绪；本次约3秒得到自然中文回复。
- `realtime_clock_contract` 在当前修订版通过，验证40 Tick实际耗时约
  2秒且不会把创建夹具前的等待补偿成突发快进。
- `zero_human_dedicated_server_chunk_and_respawn` 在当前修订版通过：
  零真人在线时，探针位于AI当前区块之外，实体和方块仍按模拟距离Tick；
  不使用强制加载，且覆盖死亡与原版重生。
- `real_emergency_enderman_defense` 在 Forge 65.1.0 通过 1/1（1.493 秒）：
  生产紧急控制器在真实末影人威胁下完成武器装备、撤退、格挡/守卫和反击，
  并以击杀或至少八格、四十 tick 无伤安全站位结束；这不是模型决策证据。
- `real_emergency_slime_defense` 在 Forge 65.1.0 通过 1/1（1.797 秒）：
  生产紧急控制器在真实史莱姆威胁下完成撤退、装备武器、反击和击杀；
  同样不提升任何随机种子或模型统计门槛。
- Forge 兼容性编译矩阵在当前源代码下通过 65.0.0、65.0.8、65.0.9；
  发布元数据仍声明 `[65.0.0,66.0.0)`，并未把未验证的其它构建写成通过。
- `real_player_chat_to_surprise_zombie_defense` 在最终修订版通过一次：
  真实玩家普通聊天进入模型链，身体在12 Tick内产生物理反应，随后实际
  击杀僵尸、移动至少0.35格并保持逐Tick平滑视角。一次通过不是稳定率。
- `real_player_chat_to_critical_golden_apple` 已通过危急生命下的真实物品
  使用，修复了饥饿条满时中断金苹果的问题。

### 无启动器测试边界

Forge官方的`gameTestServer`可用`./gradlew runGameTestServer`启动无窗口
服务端，并以required失败数作为退出码。本项目用它加载真实方块/实体
夹具、登录Headless身体和模拟真人`ServerPlayer`，然后通过Forge聊天
事件进入生产模型链。这能验证服务端玩家生命周期、聊天、模型决策、
移动/战斗/物品/菜单/世界物理以及存档证据，不需要启动器、画面或UI点击。
显式允许真实供应商调用时使用：

```text
./gradlew runGameTestServer \
  -Plive_model_test=true \
  -Prealtime_gametest=true
```

Forge构建测试服默认会尽快推进Tick，外部模型等待期间可能经过数万
游戏Tick，使掉落物消失或作物生长。`realtime_gametest`仅在开发测试
进程启用50毫秒Tick节流；`realtime_clock_contract`同时断言40 Tick
耗时1.75–5秒。该Mixin和测试资源均被发布JAR审计明确排除。

客户端ESC界面视觉布局、真实鼠标点击手感、自定义皮肤和两个客户端同步
动画仍需渲染客户端或人工验收；服务端测试不会把这些项目标记为通过。

这些 GameTest 是动作语义和组合链路门禁，不是随机种子通关基准。
自然结构发现、随机资源经济、动态怪物行为、真正 Hardcore 世界与
长期风险都没有由夹具覆盖。

## 明确尚未宣称通过

- 24 小时连续 Headless 压测和 100 小时世界记忆稳定性。
- 两个真实客户端同时观察自定义皮肤、头部、装备、主副手与全部动作。
- 自然随机种子中从空背包完成 M1 基础生存。
- 自然堡垒、自然烈焰人/末影人资源路线、自然要塞/门房和动态末影龙。
- 一个初始命令、此后零人工干预的一条命 Hardcore 通关。
- 100/200/1,000 个隐藏随机种子及两小时/六小时统计。
- 平均/p95 Tick、长期内存、10,000 标点/100,000 资产和 10,000 格路线性能目标。
- 全部原版工作站、红石装置、农场、建筑、PVE/PVP、死亡恢复和长期陪玩场景矩阵。
- 多 Agent 独立 UUID、身体、凭据槽、目标、记忆、皮肤和并发预算；
  当前设置中心只管理一个活动身份。
- Create、MTR、农夫乐事等精确版本适配器。
- 经许可的第三人称真人/AI盲测。

只有产生相应的真实测试数据后，才能把这些项目改为“通过”。设计、已注册技能、单元测试或受控夹具都不能替代自然端到端验收。

返回 [README](../README.md)。

## 2026-08-11 farming regression follow-up

当前源码已针对紧凑灌溉田的真实失败完成定向修复：长距离掉落物直达只有在
第一人称观察已覆盖全部身体/支撑体素且确认非液体时才允许；已授权且当前可见的
作物在旧 Forge 补丁中短暂报告 AIR 支撑时保留有限兼容分支，但液体支撑始终拒绝。
`PerceptionNavMapper` 也不会用边缘站立的 BODY_CONTACT 把已观察水体升级为 SOLID。

新鲜隔离 Forge GameTest 结果：65.1.0 的小麦、胡萝卜、马铃薯、甜菜根和偏置水田
均 1/1；65.0.0 的小麦、胡萝卜和甜菜根均 1/1。此前的 65.0.0 支撑观察超时被保留
为失败证据，修复后通过；一次五个测试共享服务器的通配符运行因生命周期竞争失败，
不作为功能结果。

本轮 `./gradlew test`、`./gradlew build` 和 Python `unittest` 44/44 通过。新的
开发工件为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为
`1c8ea9740091e3385f53c61adf9e7b3ffcd4e44d15edb5389e65320d313cba7d`。
源码仍是 `DIRTY_NO_COMMIT`，工件仍为 `NON_RELEASE`；真实供应商模型、客户端和
Hardcore M0–M4 门禁仍 `NOT_RUN`。

后续隔离 Forge 65.1.0 定向回归中的即时聊天跟随、十僵尸/十骷髅应急生存和跑酷
三项均 1/1 通过（分别见 `/tmp/mcai-followup-real_player_chat_to_immediate_bound_follow.log`、
`/tmp/mcai-followup-real_emergency_zombie_skeleton_horde.log`、
`/tmp/mcai-followup-real_parkour_course.log`）。这些仍是无模型的真实服务端物理
组件证据，不升级真实模型、客户端、PVP 或 M0–M4 状态。

无玩家服务端回归也已用正确注册选择器复跑：
`zero_human_dedicated_server_chunk_and_respawn` 1/1（1.759 s），
`auto_presence_on_human_login` 1/1（605.0 ms），均为 Forge 65.1.0 新鲜目录。
先前带 `real_` 前缀的尝试只是未匹配测试的命令错误，未进入产品执行。

另修复 GameTest 选择器 guard 将 `real_offline_critical_golden_apple` 误判为
live-model 的编排问题。修复后 Forge 65.1.0 离线金苹果、末影人应急防御、史莱姆
应急防御分别 1/1（689.9 ms、1.551 s、799.1 ms）；`./gradlew build` 与 Python
unittest 45/45 通过（包含选择器回归），产品 SHA-256 未变。无 provider
凭据在本轮被读取或发送。

M0 定向物理回归已跨 Forge 65.0.0/65.1.0 复跑：
`headless_player_lifecycle_state_and_fair_action` 分别 1/1（24.70 s、25.34 s），
`real_water_clutch` 分别 1/1（736.4 ms、788.2 ms）。这组结果只证明真实
Headless ServerPlayer 的局部生命周期、公平动作和落水自救组件，不升级正式
客户端、模型或 M0–M4 门禁。

当前源代码随后针对全部已发布 Forge 65.x patch 运行精确选择器
`mcai_companion:zero_human_dedicated_server_chunk_and_respawn`，65.0.0、65.0.1、
65.0.2、65.0.3、65.0.4、65.0.5、65.0.6、65.0.7、65.0.8、65.0.9 和 65.1.0
均在隔离目录中 1/1 通过。日志保存在 `/tmp/mcai-patch-65.0.0.log` 至
`/tmp/mcai-patch-65.1.0.log` 及对应的 `/tmp/mcai-patch-65.*` 运行目录。
这是完整的 patch 生命周期兼容性 smoke，不是完整聊天/移动/菜单/存档矩阵，
也不提升真实模型、客户端或正式 M0–M4 状态。
