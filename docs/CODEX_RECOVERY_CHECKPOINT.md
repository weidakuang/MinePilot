# Codex Recovery Checkpoint

Last updated: 2026-08-20T23:19:00+09:00

This checkpoint is intentionally concise and English-only. Runtime chat and
multilingual test fixtures may contain other languages; public repository
documentation must not.

## Current objective

Continue the MinePilot M1–M4 objective on Minecraft Java 26.2 / Forge 65.x with
real, fair `ServerPlayer` behavior. Do not claim professional-companion,
random-seed, two-hour, or M0–M4 completion until the artifact-bound protocols
pass.

## Current recovery state

This section supersedes older chronological notes below when they conflict.

- The latest authorized real-model `mimo-v2.5` Ender-pearl reserve slice now
  passes on Forge 65.1.1 after the epoch fix below. The model returned
  `START_SKILL secure_ender_pearl_reserve`; the real ServerPlayer built the
  observed roof, killed and picked up Endermen, reached 14 pearls with full
  health, and Forge reported `All 1 required tests passed` in 3.094 minutes.
  SQLite recorded `conversation_task_accepted`, HTTP-200 model response,
  schema/revision acceptance, `skill_started`, and
  `low_level_actions_issued`. This is a bounded live-model acquisition slice,
  not random-world, Hardcore, speedrun, or M1-M4 evidence.
- The preceding run reached the same 14-pearl state but failed its final tick
  with `stale_world_revision`. The cause was an active skill's route milestone
  changing while the skill was still authorized; the observation provider
  released the frozen decision epoch on that ordinary progress change. Active
  skills now retain their bound epoch until completion, so a long skill cannot
  be rejected at the exact completion boundary. The regression is covered by
  the observation-provider source contract and the 25-field route-snapshot
  test.
- The source/test fix is published on public `main` as the validated
  production snapshot commit `50fb70bbbb0441b16e1456e5b96e6d2c08defdb5`
  with tree `66cf4c51c975d8c0f82e7cb8659c2b8e4fdf5736`; later docs-only
  commits carry the evidence updates. Fresh clones of the published line
  matched the production blobs and Forge 65.1.1 `compileJava` passed.

- The fair natural-End ingress gate now passes on both Forge 65.1.1 and
  Forge 65.0.0 in fresh isolated GameTest server directories. The gate uses a
  real vanilla End portal, zero human players, the production `ServerPlayer`
  skill path, fresh first-person semantic frames, ordinary observed mining,
  natural terrain, and no post-entry teleport or hidden terrain query in the
  production controller. The body reached the strict natural End-stone
  support/radius handoff in both runs. One seed had a continuous route and did
  not need a bridge, so the fixture accepts zero block placement only when no
  gap is encountered; a gap still requires ordinary owned block use.
- The ingress budget was raised from 16/32 to a bounded 128 observed-block
  mining allowance after real natural terrain showed a continuous End-stone
  wall from the entry platform toward the central island. This remains an
  observed BreakBlock path with an ordinary iron pickaxe, not a world scan or
  teleport. The focused ingress, bridge, break, combat, route, and planner
  tests pass after this change.
- The immediate ingress production failure before the pass was
  `reach_end_island.mining_budget_exhausted` at radii 90.8, 83.96, and 67.71;
  those runs are retained as negative evidence. The final 65.1.1 and 65.0.0
  runs both report `All 1 required tests passed`.
- The release line is now `0.1.11-dev-mc26.2`. The clean Gradle `build`
  passed after the ingress, rally-evidence, loadout, and planner-guide
  changes. The release JAR is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `44d9a8013c16a96c227046cef233e51c4ce6affa54d5d360ce2775f3c914512b`.
- The validated production source tree is published on public `main` through
  the snapshot above. A fresh public clone verified the same production blobs,
  the previously truncated emergency-survival files, the new epoch/projection
  and observed-rally fixes, and `compileJava` on Forge 65.1.1. The repository
  remains `NON_RELEASE` because dynamic-dragon and formal M1--M4 gates are
  not complete.
- The natural End selector was extended with a strict dynamic-presence slice.
  A fresh Forge 65.1.1 zero-human run observed the vanilla manager dragon with
  `isNoAi=false`, a real displacement of 104.5 blocks in that run, and a
  recorded vanilla phase; the selector passed. This verifies only manager
  presence/motion. It does not claim dynamic dragon combat, crystal clearing,
  survival, return, random Hardcore, or two-hour completion.
- The dynamic dragon controller now has an observed rally search: after an
  empty sky sweep it may choose a fresh, low-danger standing cell from the
  current first-person navigation frame, bounded to the verified End radius,
  and must pass the ordinary `TravelTo` precondition before moving. A completed
  rally leg becomes the next bounded search anchor. Focused combat tests and
  the fair-source contract pass; a natural manager-dragon combat/kill/return
  gate remains `NOT_RUN`.
- The dynamic-presence documentation and release-excluded fixture are also
  published on `main` as commit `8edf819ba87c7a5624011a6a5b96ac11ae21af87`
  with tree `1e537f0ca53b4283ce2687014fb6b99b5342f528`; a fresh clone matched
  the local tree and compiled on Forge 65.1.1.
- The current combat-safety patch also classifies an active, visible
  ender-dragon breath cloud as a fair proximity threat; waiting or expired
  clouds remain neutral. The focused perception source-contract test passes;
  no hidden effect data or world mutation is used.
- A fresh authorized MiMo movement slice on Forge 65.1.1 passed in an
  isolated GameTest server. The SQLite chain was
  `conversation_task_accepted → model_request_started →
  model_response_received(HTTP 200) → decision_revision_accepted(START_SKILL
  travel_to) → skill_started → low_level_actions_issued(move)`, and the body
  reached its requested point with ordinary `ServerPlayer` movement. Forge
  reported `All 1 required tests passed` in 22.86 seconds; the model request
  itself completed in 7.136 seconds. This is controlled live evidence, not a
  rendered Actor/Observer or formal M1-M4 gate.
- The same provider's explicit multi-phase smoke test is retained as negative
  evidence: capability negotiation succeeded, but its real structured answer
  for `secure_ender_pearl_reserve` was `REPLAN` instead of the required
  `START_SKILL`. The assertion failed after 67.66 seconds, and no fallback
  action was invented.

- Root causes closed in the current worktree: the vanilla End fight bootstrap
  could retire a test dragon created before its legacy scan settled; a
  headless player could retain the same section coordinates across dimensions
  without refreshing the new level's ordinary player-tracking window; unsafe
  optional cage traversal terminated the whole dragon fight instead of
  reacquiring a visible dragon; and the continuous-route harness incorrectly
  required destruction of one optional crystal and one artificial bar even
  after vanilla had credited the dragon kill to the companion.
- Two additional production defects are closed in the current worktree. The
  emergency fall controller incorrectly treated the End as water-forbidden;
  it now rejects water only in the Nether and retains all observed-surface and
  vanilla-use gates. Dragon combat also used to enter ranged mode permanently
  after its first finite melee burst; four completed arrows now reopen one
  bounded melee window so a later perch can be used safely.
- Current implementation files include `AiPlayerSession`,
  `FightEnderDragonSkill`, and the controlled live/zero-human Forge GameTests.
  Focused JUnit coverage includes risky-cage recovery, dimension-aware session
  tracking, and Gradle selector wiring.
- The last failed live gate reached `Free the End` but the harness rejected the
  credited kill because of its over-constrained optional-obstacle assertion.
  That failure is retained as negative harness evidence.
- The corrected authorized `mimo-v2.5` run passed the complete controlled
  stronghold-room-to-return chain on Forge 65.1.1 in 6.188 minutes. The model
  selected normal skills for maze search, twelve-eye activation, End entry,
  dragon combat, and return. Vanilla recorded `The End?`, `Free the End`, and
  companion kill credit; route milestones recorded `DRAGON_KILLED` at game
  tick 4802 and `RETURNED_FROM_END` at game tick 7425. SQLite records the
  perception/request/HTTP-200/schema/revision/skill/action causal chain.
- A zero-human, same-section Overworld-to-Nether regression passes on Forge
  65.0.0 and 65.1.1. It verifies destination simulation, an outlying entity
  and scheduled block tick, and retirement of the old dimension's window
  without a test force-load at the destination.
- The exact current-tree Gradle clean/test/build, release-JAR inspection, and
  compatibility checker pass after the End safety corrections. The preceding
  evidence-integrity snapshot separately passed all 65 Python protocol tests
  and all ten mutation variants. The preceding 0.1.10 production JAR SHA-256
  was
  `79859cc6ce7d4ff1c7ec0bf16b13a7ef333c696ff9ff4f9ca863f7e4485cdac1`.
  The last published snapshot before these two focused corrections is public
  commit `11975c380c243aa331ac19edbb0e494d99663313`, exact tree
  `32c864eff3c26c0119ee23f62537b58ff9acac49`; its fresh clone compiled
  successfully. Formal
  Actor/Observer, random Hardcore, and M0--M4 statistical gates remain
  `NOT_RUN`.

## Latest root cause and evidence

The latest authorized `mimo-v2.5` live progression slices now include a
complete controlled M1 foundation route, a complete controlled Nether portal
handoff, and a complete End combat/return chain. Final evidence:

- Forge 65.1.1, Java 25, real-time GameTest server;
- model response was HTTP 200 and schema-valid `START_SKILL`;
- `skill_started` and real melee/bow low-level actions were observed;
- the body used a bounded first-person retreat of approximately eight ticks,
  kept the crystal inside the observed firing corridor, and remained on the
  constructed obsidian course;
- the model-selected fight skill destroyed the visible crystal and dragon with
  normal melee and bow actions; the route verifier recorded `DRAGON_KILLED` at
  game tick 2164;
- the same UUID then selected `find_and_enter_observed_portal`, physically
  entered the activated End return portal, and the verifier recorded
  `RETURNED_FROM_END` at game tick 2283;
- test `real_player_task_to_live_model_end_victory_and_return` passed with
  `All 1 required tests passed` in a 2.183-minute real-time GameTest run after
  the cross-dimension initial-anchor guard and the 16-block observed-portal
  approach policy were applied.

The run also reproduced and closed two lifecycle/skill defects: a first human
joining the overworld must not remove a body that is already in the End, and a
visible return portal at roughly ten blocks must be eligible for ordinary
approach. The model selected `fight_ender_dragon`, vanilla recorded `Monster
Hunter` and `Free the End`, the route verifier recorded `DRAGON_KILLED` and
`RETURNED_FROM_END`, and the stable body UUID returned to the overworld. The
earlier run that ended in `embodiment_failed` and the run that ended in
`portal_not_observed` remain failure evidence, not hidden successes.

The End scenario is controlled and bounded: the test-only End arena disables
ambient mob spawning after installing the dragon and crystal. The foundation
and portal scenarios are also bounded fixtures. They are evidence for live
model-to-vanilla causal chains, not random-seed or speedrun results.

The latest stronghold entry slice also passed after two real-model failures
exposed interaction bugs. The first failure came from an overly local final-eye
fallback that kept the body rotating at one station; the second came from
accepting a semantic hit in the 4.5-to-4.7 block range that vanilla correctly
rejected. The fallback was removed and the candidate distance was tightened to
4.45 blocks. Run `debug15` then passed with the real model: physical maze
search, dead-end backtracking, a second turn, four-station portal activation,
the full twelve-eye transaction, model-selected End entry, and the vanilla
`The End?` advancement. The SQLite causal order included four model
responses, four accepted skill starts, and low-level movement/use actions.
This remains a controlled fixture, not an M2 or speedrun result.

The next live End failure exposed a separate stale multipart index: after End
entry, `ServerLevel.dragonParts()` retained old collider objects with the same
UUIDs but positions 90--101 blocks from the live dragon root. The root-owned
`getSubEntities()` objects were 2--8 blocks away and fully loaded, yet the
old candidates consumed the fair FOV/LOS budget. Candidate collection now
replaces same-UUID indexed parts with the current root-owned parts, uses the
tracked dragon-fight UUID as a bounded fallback, and treats the canonical
dragon root as a hostile boss for finite candidate prioritization. A separate
standalone authorized MiMo End test passed dragon combat and the return portal
in 1.958 minutes. A fresh repeat after the anchor and portal policy fixes also
passed in 2.183 minutes. Those older full-route failures remain useful negative
evidence, but the current controlled stronghold-room-to-return chain now passes
as recorded in the current recovery state above.

The latest damage-focused patch also makes a shielded body perform one bounded
side-step after a directionless recent damage cue (such as indirect magic),
instead of remaining shield-only. This is covered by a regression test and was
present in the passing controlled full-route run; that single bounded pass is
still not a random-world survival statistic.

The latest live debugging found one concrete first-human lifecycle failure:
the body could be removed before a malformed or void-like test anchor had a
vanilla-safe placement, leaving no visible AI. `AiPlayerManager` now validates
the bounded `SafeCompanionSpawnLocator` result before removing the current
body; if no safe placement exists it keeps the authoritative body and claims
the one-time startup provenance. The production zero-human delayed-login
GameTest now passes with the body active before human login and safely present
after the login. Real `mimo-v2.5` movement, follow, surprise-zombie defense,
and golden-apple slices also pass on the patched line. The first movement run
before this guard remains recorded as a genuine failure (`body did not return
after initial-anchor relogin`), not silently upgraded.

The first fresh live combat rerun exposed a test-fixture defect rather than a
model or production combat result: the embedded test player was logged in at
the vanilla world origin. That invalid login anchor caused the legitimate
first-human lifecycle to remove the body before the chat packet reached the
model, so the run was stopped and recorded as a failure. The combat fixture
now logs the player in two blocks beside the live body through the normal
`ServerPlayer` login path. The rerun passed with the supplied `mimo-v2.5`
gateway in 18.80 seconds: Chinese chat acceptance, model response, schema and
revision audit, `engage_observed_entity`, `skill_started`, low-level movement
and attacks, and the vanilla `Monster Hunter` advancement were all observed.
This remains a controlled one-zombie live slice, not a PVP, horde, Hardcore,
random-seed, or M4 result.

The next real-model surprise-defense rerun then exposed a production safety
gap: a rear Zombie could deliver a fair, directional damage cue while the
emergency lane only swept its view and waited for the planner. The first run
(`run-live-surprise-20260814e`) killed the body at tick 251 and is retained as
a genuine failure. `EmergencySurvivalController` now takes one bounded,
sneak-protected step away from a recent damage direction after the first scan
tick, using only the fair source vector and freshly observed adjacent-cell
evidence. The same real `mimo-v2.5` scenario then passed in
`run-live-surprise-20260814f`: the body reacquired the Zombie, dispatched
ordinary vanilla attacks, survived, and triggered `Monster Hunter` before the
test ended. The model lane returned a replan during the pressure window; the
survival result is therefore evidence for emergency recovery plus a real
model request, not a claim that the model alone cleared the encounter.

The first bounded multi-hostile fixture exposed a harness anchoring defect:
`run-live-horde-20260814j` left the six targets at their pre-login positions
while the normal first-human anchor transaction moved the body, so the model
correctly reported that no hostile was visible and selected a survey replan.
The fixture now repositions those same entities around the authoritative body
after the anchor transaction. The latest rerun recreates the authored targets
after the replacement body is stable, discards ambient entities, requires the
model skill to be genuinely `RUNNING`, and starts target-health attribution at
that causal edge. The Chinese team request first produced a bounded model
replan, then HTTP-200 `START_SKILL(engage_observed_entity)`,
schema/revision acceptance, `skill_started`, and
`low_level_actions_issued` in SQLite; the server log recorded four of six
authored targets damaged, body movement, and full body health. The latest
clean run passed in 40.14 seconds. This is not a human PVP, Hardcore,
random-seed, or M4 protocol.

The ten-plus-ten extension initially exposed two test-only attribution defects:
target death flags and local/ambient damage were counted before a model skill
started, and the first-human relogin could unload the authored mobs. The
fixture now recreates ten Zombies and ten Skeletons after the body relogin,
requires `engage_observed_entity` to enter `RUNNING`, and compares health only
against a post-start baseline. The latest real `mimo-v2.5` run passed in 39.70
seconds: SQLite recorded the full model-to-skill-to-action chain, sixteen of
twenty authored targets were damaged (the fixture threshold is ten), the body
moved, and body health stayed full. A previous 38.28-second run damaged all
twenty and remains retained controlled evidence.
This remains a controlled multi-hostile slice, not human PVP, Hardcore,
random-seed, or M4 evidence.

The first three live iron-golem attempts remain negative evidence: the model
returned `START_SKILL(engage_observed_entity)` and SQLite recorded a vanilla
`attack_entity` dispatch that damaged the golem, but the body did not receive
a verified incoming attack. The root cause was stale creative/invulnerability
state inherited by the test body, not a model decision. The latest authorized
run clears those abilities and asserts genuine survival before chat. It
passed in 12.21 seconds: HTTP-200 schema/revision acceptance, skill start,
vanilla attack dispatch, body movement, golem damage, a real golem attack, and
body survival. This is a controlled neutral-mob slice, not a human PVP or
Hardcore result.

## Changes in the current worktree

- `AiPlayerSession`: refreshes the ordinary vanilla player chunk-tracking
  window when the `ServerLevel` changes even if the section coordinates do
  not.
- `FightEnderDragonSkill`: an unsafe optional cage tower now clears tentative
  traversal authority and resumes fair target scanning instead of terminating
  the complete fight.
- `LiveModelChatGameTests`: waits for vanilla End-fight settling, uses bounded
  loaded geometry for the controlled arena, allows the historically measured
  150-second combat window, and distinguishes credited physical dragon damage
  from optional crystal/bar tactics.
- `EmbodimentGameTests`: zero-human same-section cross-dimension simulation
  regression for the headless player's vanilla tracking window.
- `build.gradle`: supports the documented generic `test_selector`, preserves
  `live_model_selector` as an alias, and rejects conflicting values.
- `build.gradle`: disables caching only for ForgeGradle's merged main
  `compileJava` output. A clean build had reported `FROM-CACHE` without
  materializing `build/sourceSets/main`, causing `compileTestJava` to lose
  production classes; the same clean command now compiles main locally and
  passes.
- Public governance: current security version, an auditable 0.1.9 chronology,
  and an English Contributor Covenant-based code of conduct.

- `FairPerceptionSampler`: multipart Ender Dragon samples begin at a collider
  center and bounded horizontal points, with ordinary first-person LOS checks;
  stale same-UUID index entries are replaced by current root-owned parts,
  dragon roots and parts are prioritized as hostile candidates within the
  finite budget, and sibling colliders publish once as one semantic parent.
- `EmergencySurvivalController`: a recent directionless damage cue keeps a
  shield lease but adds a bounded alternating side-step, preserving ordinary
  collision and fall rules instead of allowing repeated magic damage to become
  a stationary guard loop.
- `FightEnderDragonSkill`: bounded 24-swing melee burst, eight-tick ranged
  retreat, projectile/damage evasion, crystal firing-lane recovery, and
  transient child-shot failure recovery.
- `SkillSupervisor`: exposes the last checkpoint for timeout diagnostics.
- `EmbodimentGameTests`: offline dragon window/arena diagnostics and 128-arrow
  baseline.
- `LiveModelChatGameTests`: live dragon/return evidence for visible entities,
  dragon parts, danger signals, revisions, arrows, checkpoint state, and the
  isolated no-ambient-mob End arena; diagnostics tolerate the intentional
  one-tick initial-anchor relogin gap without masking a real failure.
- `AiPlayerManager`: first-human initial anchoring preserves an active body
  across dimensions and retains a validated same-tick retry anchor; it never
  turns a gameplay portal transition into a hidden teleport.
- `LiveModelChatGameTests`: the live combat fixture now creates its embedded
  human at a safe position beside the companion, matching a real client login
  and preventing the test from fabricating an invalid origin anchor.
- `EmergencySurvivalController`: a recent directional damage cue now permits
  one bounded observed-cell separation before the next model round trip,
  closing the rear-hostile "look but do not move" death path without granting
  hidden target coordinates or direct world mutation.
- `EmergencySurvivalControllerTest`: regression coverage verifies the
  directional damage separation is issued on the second 20-TPS tick and uses
  a cautious vanilla movement input.
- `LiveModelChatGameTests`: the bounded horde fixture now reanchors its six
  existing hostile entities after the normal first-human login transaction, so
  fair semantic visibility and damage assertions refer to the same body.
- `LiveModelChatGameTests`: the live golem and horde fixtures now force
  survival mode, clear invulnerability and stale creative abilities, and fail
  closed unless the body is genuinely damageable before the model request.
- `LiveModelChatGameTests`: the multi-hostile fixtures recreate authored
  targets after the normal first-human body relogin, require the combat skill
  to be `RUNNING`, and attribute damage only against a post-start health
  baseline. This prevents speech-only, ambient, and target-death flags from
  becoming false combat passes.
- `MinecraftPlannerInputFactory`: explicit combat goals now receive a compact
  hostile-only target playbook that forbids selecting dropped items or passive
  entities and requires immediate `START_SKILL` when a legal hostile is in the
  current fair frame.
- `EngageObservedEntitySkill`: the fair target gate accepts canonical vanilla
  hostile types even when a derived hostile bit is absent, while continuing to
  reject passive mobs, projectiles, and dropped items.
- `LiveModelHordeFixtureSourceContractTest` and planner/combat unit tests:
  regression coverage for the ten-plus-ten registration, hostile-only prompt,
  canonical hostile validation, and post-start damage attribution.
- `EmbodimentGameTests`: the physical neutral-golem regression now exercises
  the same delayed `NoAI` to vanilla-AI target activation lifecycle.
- `PortalSkillPolicy`: the default observed-portal approach distance now uses
  the full fair 16-block perception budget, while entry still requires a
  current first-person visible face and vanilla reach.
- `EndCrystalStandOffPlanner`: a bounded 20-block observed lateral firing-lane
  search; the live fixture now places the test crystal off the dragon's direct
  centerline so the scenario can verify crystal priority without an impossible
  static occlusion.
- `PrepareFoundationShelterMaterialsSkill`: remembered-table aiming now has a
  short bounded alignment window and reacquires the table through ordinary
  movement instead of remaining in a stale crosshair loop.
- `BuildShelterStepSkill`: roof recovery opens a closed shelter door only via
  the normal first-person use action when it occludes a pending roof face, and
  resets one exhausted exterior fallback marker so a final roof corner can be
  reached from the observed apron without a permanent interior retry loop.
- `SearchObservedStrongholdPortalRoomSkill`: bounded inherited interior
  confidence, fair frontier diagnostics, station yaw derived from actual
  travel, and no walking from a first-person view without observed floor
  support. The live harness keeps the chat sender connected until the initial
  anchor transaction settles and places it in the safe locator's authored
  corridor.
- `ActivateObservedEndPortalSkill`: stale target outcomes are discarded rather
  than replayed, final-eye movement uses the normal station route, and the
  semantic candidate range fails closed below the survival block interaction
  range. This prevents the observed "staring but doing nothing" loop while
  preserving vanilla crosshair, reach, and inventory validation.
- `FairPerceptionSupportSourceContractTest`, `FightEnderDragonSkillTest`, and
  `EmergencySurvivalControllerTest`: regression coverage for collider-center,
  transient shot failures, bounded retreat, and End fall recovery.
- English-only public project documentation and a compact evidence status.

## Last completed checks

```text
FightEnderDragonSkillTest                         PASS
FairPerception focused tests                      PASS
Offline End/dragon/return physical baseline       PASS (no model)
Live MiMo standalone End victory and return      PASS (controlled real-model chain; Forge 65.1.1, 2.183 min)
Live MiMo movement, follow, surprise defense, food PASS (four controlled live-model slices)
Live MiMo container withdrawal and item collection PASS (two controlled live-model slices)
Live MiMo foundation bootstrap and shelter       PASS (8.912-minute controlled slice; isolated SQLite)
Live MiMo Nether portal build and entry          PASS (52.17-second controlled slice; isolated SQLite)
Live MiMo stronghold search, portal activation, End entry PASS (controlled real-model prefix)
Live MiMo stronghold-room-to-return chain        PASS (6.188-minute controlled real-model chain; not random Hardcore evidence)
Live MiMo End return after anchor/portal fixes   PASS (model fight, vanilla return portal, stable UUID; bounded fixture)
Live MiMo movement causal chain                  PASS (12.26-second real-time slice; START_SKILL travel_to and vanilla move)
Live MiMo follow causal chain                    PASS (12.66-second real-time slice; ordinary unaddressed Chinese chat, START_SKILL follow_entity, vanilla move)
Live MiMo surprise-zombie defense (pre-fix)     FAIL (12.69-second slice; rear hostile killed body while scanning)
Live MiMo surprise-zombie defense (patched)     PASS (15.27-second real-time slice; bounded damage separation and vanilla counterattack)
Live MiMo golden-apple consumption               PASS (15.26-second real-time slice; vanilla item-use consumption)
Live MiMo Chinese combat task                    PASS (18.80-second real-time slice; engage_observed_entity, vanilla attack, Monster Hunter)
Delayed first-human anchor lifecycle             PASS (production zero-human startup GameTest; no rendered-client claim)
Delayed first-human anchor during emergency      PASS (Forge 65.1.1 production GameTest; deferred relogin after survival clears)
Initial-anchor no-safe-placement guard           PASS (focused source contract and patched live movement slice)
Live MiMo directionless-damage shield regression PASS (offline JUnit; real stronghold effectiveness NOT_PROVEN)
Live MiMo directional-damage separation         PASS (real 15.27-second slice; model request plus emergency recovery)
Live MiMo bounded hostile group                  PASS (40.14-second slice; six visible mobs, model engage RUNNING, 4 damaged, body moved/alive)
Live MiMo ten-plus-ten hostile group             PASS (39.70-second slice; 20 visible mobs, model engage RUNNING, 16 damaged, body moved/alive; threshold 10)
Live MiMo iron-golem duel                         PASS (12.21-second bounded slice; model engage, vanilla attack, golem damage, verified incoming golem attack, body survival)
Roof-jump physical recovery contract             PASS (no-model GameTest)
Focused shelter-material/building JUnit tests    PASS
Exact Forge 65.1.1 dedicated lifecycle smoke      PASS (real dedicated server; no functional claim)
Exact Forge 65.1.1 two-boot persistence smoke     PASS (real restart; no functional claim)
Delayed first-human anchor client smoke           NOT_RUN (macOS lacks Linux/Xvfb; no client claim)
Zero-human same-section cross-dimension simulation PASS (Forge 65.0.0 and 65.1.1; no fixture force-load at destination)
Full current Gradle/JUnit, release jar, compatibility, and Python audit     PASS (63 Python tests; artifact SHA recorded above)
Formal evidence nonce-binding regressions          PASS (65 Python tests; foreign/missing/mixed-run evidence rejected)
Prepared manifest request-nonce binding             PASS (`BINDINGCHECK`; no model credential persisted)
Clean cached Gradle release gate                    PASS after merged-main cache guard
Formal Actor/Observer client gate                 NOT_RUN
Hidden random Hardcore M1/M2/M4 gates             NOT_RUN
M0/M1/M2/M3/M4 product milestones                 NOT_RUN
```

The offline baseline proves only a controlled vanilla physical lower bound. It
does not substitute for a live model, rendered client, natural world, or hidden
seed statistic.

## Immediate next steps

1. Preserve the closed formal-evidence nonce boundary: the manifest now owns
   the externally expected nonce and both functional and delayed-anchor
   verifiers bind every Actor, Observer, Oracle, and Oracle-result event to it.
   Missing, mixed, and internally consistent foreign bundles fail closed.
2. Preserve all live progression results as controlled evidence; do not
   promote them to a random-seed or speedrun claim. The last formal-client
   preflight remains `NOT_RUN` because this macOS host lacks Linux/Xvfb; the
   next local verification command is the focused Python orchestrator suite.
3. Run the formal Actor/Observer client gate only on an authorized Linux/Xvfb
   worker with the exact frozen JAR, then extend the controlled chain into a
   natural dynamic-dragon world and execute hidden-seed protocols only after
   their exact artifact and worker prerequisites are satisfied.

## Repository and release state

- Public repository: `https://github.com/weidakuang/MinePilot`.
- Public `main` before repair: `66bb2cff9362451fb08daf6fbf330a5d1058d589`.
- Verified repair commit: `5edcdc7f7d37e91b1466fff460b571333c8312db`;
  exact source tree: `e9ec6c1fab68dcbd04b9fa14063e9072bb6e44b1`.
- Forge 65.x backup branch: `mc26.2-forge65`.
- Local branch: `main`.
- Public `main` is again a valid source backup. The two large
  emergency-survival files were restored with full Git blob/tree objects;
  byte counts and blob hashes match the local tree, and a fresh public clone
  passed Gradle test/build/JAR/compatibility verification. Future connector
  publications must keep using blob/tree objects for large files. Generated
  run directories remain disposable and must not be staged.
- API keys are process-only during live tests and are never written here.
