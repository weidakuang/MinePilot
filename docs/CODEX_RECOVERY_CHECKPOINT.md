# Codex Recovery Checkpoint

Last updated: 2026-08-22T03:28:00+09:00

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

- Current published patch: `FightEnderDragonSkill` now routes a natural
  centerward obsidian frontier through bounded fair island re-entry before a
  lateral detour, and `EndIslandIngressSkill` rejects breaking the observed
  support voxel under the body (including the corrected block/grid coordinate
  conversion). The test-only terrain diagnostic records the natural platform
  shape without being used by production decisions.
- Focused ingress and combat JUnit suites pass, and the fresh Forge
  65.0.0 `natural_end_island_ingress` selector passed in 5.835 seconds.
- The latest completed zero-human Forge 65.1.1
  `natural_end_dynamic_dragon_combat` gate remains `FAIL`: after 8,002 fight
  ticks the body reached approximately `(46.636,51.0,-1.503)` but the vanilla
  AI-enabled manager dragon remained unharmed. The nested fair ingress mined 29
  observed blocks and placed 32 bridge blocks, reached 24 frontier probes, and
  then exhausted its scan budget with `tower_up.overhead_clearance_unverified`;
  it fired zero arrows and dealt zero dragon damage. The non-repeating visited
  feet-cell guard improved physical displacement but did not close the natural
  pillar-edge route. No teleport, command, hidden terrain read, dragon freeze,
  or fixture terrain mutation was used. This is the active release blocker; it
  is not M2/M4 or random-seed evidence.
- `EndIslandIngressSkill` now retains a bounded set of fairly observed
  landfall supports whose vanilla `TravelTo` child became stuck, excludes
  those cells from the next candidate search, and clears the set only after a
  real bridge, tower, mining, or completed-travel topology change. This is a
  local liveness guard, not a route oracle or world read. Focused ingress and
  dragon tests plus the full Gradle build pass with the change.
- A fresh natural dynamic run with that guard still fails honestly after
  8,002 fight ticks: body `(46.636,51.0,-1.503)`, ingress 29 observed mined
  blocks and 32 placed bridge blocks, 41 frontier probes, one failed landfall
  attempt, and `tower_up.overhead_clearance_unverified`; zero arrows, dragon
  damage, kill, or return. The guard prevents stale landfall re-selection but
  does not solve the natural pillar-edge route.
- A short exploratory tower-recovery change was tested against the same fresh
  natural selector and was immediately reverted because it regressed the
  measured route: the body stopped near `(42.603,50.0,-4.500)`, the nested
  ingress ended with `reach_end_island.scan_budget_exhausted` after
  `travel_to.stuck`, and the dragon received zero damage. The run is retained
  as negative experiment evidence only; the reverted change is not part of the
  published source or any pass claim.
- A fresh authorized Forge 65.1.1 live-model movement slice passed in
  `run/gametestserver`: SQLite recorded the real chat/task chain, HTTP-200
  Responses output, 7,741 input and 194 output tokens, `START_SKILL
  travel_to`, `skill_started`, and ordinary `move` actions. The body reached
  the bounded target without teleport or command. This remains controlled
  GameTest evidence, not rendered Actor/Observer or formal M1--M4 evidence.
- A fresh authorized Forge 65.1.1 live-model parkour slice passed in 26.61
  seconds. The real model issued `START_SKILL parkour_to` three times: the
  first attempt failed `missed_landing`, the second failed
  `unexpected_fall`, and the third crossed the three one-block gaps. SQLite
  recorded three HTTP-200 Responses chains, schema/revision acceptance, skill
  starts, and ordinary movement actions; no teleport or command was used. This
  is controlled parkour retry evidence, not a rendered-client, PVP, Hardcore,
  random-seed, or M1--M4 gate.
- A fresh authorized Forge 65.1.1 live-model water-clutch slice passed in
  15.26 seconds. MiMo returned HTTP 200 with a schema/revision-accepted
  `REPLAN` for the Chinese fall-rescue request; the local emergency lane then
  equipped the owned water bucket, placed observed vanilla water in the End,
  and completed the twelve-block fall with exactly one vanilla bucket use,
  full health, and no death. This proves the fair emergency path plus a real
  model request, not a model-selected water skill or a rendered/random-world
  gate. The run recorded 11,588 model tokens and remains controlled evidence.
- A fresh authorized Forge 65.1.1 live-model farm-work slice passed in
  45.65 seconds. MiMo selected `harvest_and_replant_step` twice and then
  `maintain_observed_crop_field`; the body used ordinary observed crop mining
  and planting actions, and the server recorded `A Seedy Place`. SQLite
  recorded five gameplay planning requests and 40,964 total model tokens.
  This is bounded wheat-plot evidence only, not a long-term farm, random-world,
  rendered-client, or formal M1--M4 result.
- The late End-completion rerun now reaches the real model after fixing the
  initial-anchor chat race, but fails honestly at the portal handoff. MiMo
  returned `START_SKILL activate_observed_end_portal` three times; each start
  was rejected by the current safety/observation preconditions while ambient
  hostile pressure displaced the body. The supervisor entered
  `SAFE_IDLE/repeated_skill_rejection_without_world_change` at tick 2465 with
  zero active portal blocks, no End entry, and 25,927 recorded model tokens.
  This is a genuine negative model-to-safety result, not a fabricated action
  or a formal M2 claim.
- A fresh authorized Forge 65.1.1 run of
  `real_player_task_to_live_model_nether_materials_to_victory` exercised the
  real MiMo model through Nether preparation, Eye crafting, verified portal
  return, and stronghold triangulation. On the first portal-room request, the
  server fairly rejected `search_stronghold_portal_room.stronghold_evidence_required`.
  The next model request received a schema with that rejected action removed
  and selected `reach_observed_stronghold`, which issued ordinary movement and
  exposed a real stronghold structure. The route then encountered genuine
  `excavate_safe_tunnel.torch_place_target_occluded` and
  `search_stronghold_portal_room.search_exhausted` failures while continuing
  bounded visible-node exploration (120 visited, 61 exhausted at the last
  checkpoint). The run was stopped before a terminal result after 11 HTTP-200
  model requests (99,361 input, 1,419 output, 100,780 total tokens); no End
  entry, dragon kill, return, teleport, command, or hidden structure read was
  claimed. This validates the rejection-recovery guard as controlled live-model
  evidence, but remains incomplete and is not M2/M4 or random-seed evidence.
- `ExcavateSafeTunnelSkill` now treats a semantic torch-support face as turn
  guidance only and waits for the live centre crosshair to hit the same block
  and face before issuing vanilla `useOnBlock`. This closes the observed
  `torch_place_target_occluded` timing mismatch without widening interaction
  permissions. The focused excavation suite (including a stale-crosshair
  regression), planner suite, and brain suite pass; no live victory claim is
  attached to this fix yet.
- A fresh authorized Forge 65.1.1 live-model container slice also passed. The
  model first returned an invalid `use_block` envelope; local validation
  rejected it without an action, then the model corrected the decision,
  completed vanilla chest opening, and completed `transfer_menu_item` for
  three oak planks. SQLite recorded the failure/recovery and accepted causal
  chains; this remains controlled chest evidence, not the formal Inventory
  black-box gate.
- A fresh authorized Forge 65.1.1 live-model Zombie-defense slice passed. The
  emergency lane guarded the body while the model request crossed its soft
  deadline; the model then returned `START_SKILL engage_observed_entity`, the
  body moved and attacked through vanilla combat, survived, and triggered
  Monster Hunter. SQLite recorded the task/perception/HTTP-200/skill/action
  chain. This remains one controlled hostile, not PVP, Horde, Hardcore, or
  formal M2 evidence.
- A fresh authorized Forge 65.1.1 live-model ten-Zombie plus ten-Skeleton
  horde slice passed in 39.98 seconds. The real model accepted the Chinese
  protection request, selected `engage_observed_entity`, and SQLite recorded
  HTTP-200 Responses, schema/revision acceptance, `skill_started`, and
  `low_level_actions_issued`. The fixture observed 10 damaged targets out of
  20, body movement, and full body health. This is controlled multi-hostile
  evidence only; it is not human PVP, Hardcore, random-seed, or M4 evidence.
- Focused JUnit, full build, repository preflight, Git Data API publication,
  fresh-clone byte comparison, and fresh-clone Forge `compileJava` all passed
  for this snapshot. The dynamic natural-End gate remains recorded as failed;
  real client/model, Hardcore random-seed, and M1--M4 gates remain `NOT_RUN`
  or externally blocked.
- The current source snapshot is public `main` commit
  `6d802a7d1ca687830f0b77c719938dc53d42f762`; the four changed blobs match a
  fresh clone byte-for-byte and that clone passed Forge `compileJava`. The
  locally built development JAR SHA-256 is
  `9cf823383fcc36bb3ad98697cdb89db1498f48587c8109a5b638c05eeb8c07b5`.

- The current uncommitted combat correction contains three narrowly bounded
  fair-perception changes. `FightEnderDragonSkill` now treats a changed face
  on the same observed block as a valid reacquisition candidate, prioritizes
  a current-column overhead block only while the head cell is actually
  blocked, and permits a fresh observed lateral wall to be mined after head
  clearance. It also permits a fresh, supported, two-block-clear neighboring
  detour around an observed pillar when a strictly centerward cell is not
  available; the detour remains inside the 56-block arena radius and never
  reads hidden level state. The ingress target geometry and the 0.50 TravelTo
  arrival bound were corrected in the same patch.
- Focused `FightEnderDragonSkillTest`, dynamic-rally source-contract tests,
  and `EndIslandIngressSkillTest` pass. The Forge 65.1.1 physical selector
  `mcai_companion:natural_end_island_ingress` also passes in the current
  worktree.
- The latest zero-human natural dynamic-dragon selector was run on Forge
  65.1.1 at 2026-08-22 00:55 JST and remains an honest failure. The body
  reached approximately `(49.752,49.0,0.498)`, mined 66 currently observed
  End-stone blocks, and performed seven observed rally starts; the live
  manager dragon remained loaded, but there were zero arrows, damage events,
  kill, or return. The terminal failure was
  `fight_ender_dragon.no_visible_combat_target` after 2,649 skill ticks. This
  is not dynamic-dragon, random-seed, Hardcore, speedrun, or M1--M4 evidence.
- No model credential was used by this physical gate, and no production
  teleport, command, hidden terrain read, dragon freeze, or fixture terrain
  mutation was introduced. The next release decision is therefore gated on
  full regression and a fresh review of this still-failing dynamic combat
  path; do not promote this patch to a release claim.

- Commit `cc877a4` records a `TowerUp` precondition rejection
  in `lastChildFailureCode`, allowing the already bounded frontier-probe path
  to run instead of silently retrying the same tower. Focused ingress/combat
  tests, the full JUnit suite, and the Forge 65.1.1 build passed after this
  one-line state fix.
- The completed natural dynamic-dragon rerun
  `/tmp/mcai-dynamic-dragon-towerfrontier-NnqCfv` still failed honestly at
  8,002 executed ticks with `reach_end_island.scan_budget_exhausted`; body
  `(47.433,51.0,-0.187)`, zero arrows, zero dragon damage, zero kill, and zero
  return. The latest frame showed only observed End-stone and obsidian faces,
  so no unsupported route was inferred. The state fix is published on public
  `main` as `fd20b5c52dc518b61e9fe2ba02f062764c225ae4` through Git Data API
  objects. Fresh clone `/tmp/minepilot-tower-state-OO07E9` matched the three
  changed file SHA-256 values byte-for-byte, its goal JSON parsed successfully,
  and Forge 65.1.1 `compileJava` passed.
- The current development JAR is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `ece574e8ca4b345d4ee2127bb72ad68d3163f10fbedd48a627c8341e964a1ac5`.

- Commit `882d3e5` makes observed side-step targets land near
  the far edge of the destination cell instead of allowing `BridgeToSkill` to
  complete inside its minimum arrival radius, and lets a fresh frontier probe
  try a visible wall attachment before ordinary side travel. The new edge
  target regression, focused ingress/combat suites, full JUnit suite, and Forge
  65.1.1 build passed. The natural ingress selector passed in
  `/tmp/mcai-natural-ingress-sidestep-w37BT4`.
- A fresh zero-human natural dynamic-dragon run after this correction remains
  a physical failure in `/tmp/mcai-dynamic-dragon-attachstep-YtWEcZ`: the
  bounded fight reached 8,002 executed ticks, body approximately
  `(47.433,51.0,-0.187)`, ingress ended with
  `reach_end_island.scan_budget_exhausted`, and there were zero arrows, dragon
  damage, kill, or return. The final fair frame exposed only observed
  End-stone/obsidian faces at the pillar edge; no safe destination was inferred.
  Dynamic combat and every formal M1--M4/random-Hardcore/speedrun claim remain
  unreleased.
- The current development JAR after this correction is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `d171284cb19242398cb9e2bfc408f277188a6d6eb0e7f9639d0af065a320a71f`.
  The correction is published on public `main` as
  `e2e1d009797d59404e0fa59fe4e7b69745295da6` through Git Data API objects.
  Fresh clone `/tmp/minepilot-side-step-J0hDs2` matched all four changed file
  SHA-256 values byte-for-byte, its goal JSON parsed successfully, and Forge
  65.1.1 `compileJava` passed.

- Commit `0f3ab2a` adds fresh observed rally targets,
  bounded sky-clearance target rebinding, observed lateral End-stone mining,
  safe side-step recovery before opaque pillar ascent, and a frontier probe
  after a failed tower child. It also records the nested ingress checkpoint so
  a failure remains diagnosable after the parent fight resumes. All movement,
  mining, looking, and block use remain ordinary `ServerPlayer`/vanilla skill
  actions authorized by fresh first-person semantic frames; no teleport,
  hidden terrain read, command, or fixture world mutation is used by the
  production controller.
- Focused ingress/combat tests, the full JUnit suite, and the Forge 65.1.1
  build passed after this patch. The fresh natural ingress selector
  `mcai_companion:natural_end_island_ingress` passed in isolated directory
  `/tmp/mcai-natural-ingress-posttower-HjoyRe` (`All 1 required tests passed`).
- The latest zero-human natural dynamic-dragon selector remains an honest
  failure in `/tmp/mcai-dynamic-dragon-sidestep-kY5x7c`: the body reached
  approximately `(47.629,51.0,-0.121)`, but the ingress child ended with
  `reach_end_island.scan_budget_exhausted` and the fight with
  `fight_ender_dragon.no_visible_combat_target` after 7,045 executed ticks.
  It recorded 17 rally starts and 96 observed sky blocks mined, but zero
  arrows, dragon damage, kill, or return. Dynamic dragon combat therefore
  remains a release blocker; no random-seed, Hardcore, two-hour, or M1--M4
  claim is made.
- The current release JAR is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `ca2a6076694b393c36413ae222a9abf93ed59172089949b04f76117cce784863`.
  The snapshot is published on public `main` as
  `b2294c825c942626a676d64b276f105667c836d5` through Git Data API objects.
  Fresh clone `/tmp/minepilot-public-7UJzUD` matched all four changed file
  SHA-256 values byte-for-byte, and its Forge 65.1.1 `compileJava` passed.
  `git diff --check` is clean after publication.

- Commit `f0301fc4a4f95ccc42d2eb68dde7ed8f41964e51` adds a narrowly scoped
  observed-wall attachment path to `BridgeToSkill`: only the End ingress
  caller enables it,
  only a first-person visible solid face may authorize the adjacent placement,
  and a newer semantic frame must prove the placed block is weight-bearing
  before crossing. It also adds bounded low/side frontier observations and a
  stable child-failure diagnostic. The commit is published as
  `05b0697cc1e9bbfca7f17ba4c4ab84b00a2f3a7a` through Git Data API objects;
  a fresh clone verified all five changed blob hashes and Forge 65.1.1
  `compileJava` passed. The release JAR built from this commit is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `cd7727d7527e8deb9fa582a954a98d175b5a1b6a33b5c071b3cc24afdde6553f`.
- Commit `828c7ea` adds a server-tick-safe mining handoff: End ingress now
  waits for a newer first-person frame before starting `BreakBlockSkill`, uses
  the tick-local crosshair hit point, and equips material before an observed
  wall attachment. Dragon sky-clearance retries remember one occluded target
  and exclude it from the next bounded scan. The focused bridge, ingress, and
  combat suites pass; the full JUnit suite and Forge 65.1.1 build also pass.
- A completed Forge 65.1.1 natural dynamic-dragon rerun after this change is
  `/tmp/mcai-dynamic-dragon-occlusion-skip-lRtQE2`. It still fails honestly at
  `fight_ender_dragon.no_visible_combat_target` after 48 bounded scans with
  `lastSkyFailure=break_block.action_target_occluded`, 30 sky blocks mined,
  zero arrows/damage/kill/return, and body `(47.84,49.0,0.59)`. This is not a
  release or speedrun result; the remaining gap is target reacquisition around
  the natural pillar/ceiling edge.
- The current development JAR after the full build is
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `c84c63e3d5259d21cde2c5874aba3321e96aed4cdc8815a45f0c421b9f257fcb`.
- The commit is published on public `main` as `f530504557714f9b9eecb2c2f55b8daec26bfcb5`
  through Git Data API objects. A fresh clone verified seven changed file
  blob hashes byte-for-byte and passed Forge 65.1.1 `compileJava`.
- A fresh real Forge 65.1.1 natural dynamic-dragon run after the eight-pattern
  cardinal frontier probe was `/tmp/mcai-dynamic-dragon-cardinalprobe-njnr9C`.
  It failed at 2026-08-21 03:50:28 with the same honest result:
  `fight_ender_dragon.no_visible_combat_target`, ingress child
  `reach_end_island.timed_out`, 30 observed End-stone blocks mined, zero
  arrows/damage/kill/return, body approximately `(47.84,49.0,0.59)`, and
  `scanTurns=48`. The natural terrain slice showed an observed/persisting
  obsidian pillar frontier from x46 through x38 and End-stone beginning at
  x37. This is evidence that the remaining gap is a real pillar-edge route,
  not a missing model call.
- The subsequent attached-step run directory
  `/tmp/mcai-dynamic-dragon-attachedstep-OJNTER` was interrupted before a
  server result directory was created. It is `NOT_RUN` evidence and must not
  be counted as a pass. The attached-step implementation is included in the
  published commit, but the physical behavior still requires a later
  completed fresh gate.

- The latest End-fight commit adds fair sky-clearance recovery,
  bounded observed one-cell rally movement, and one bounded reuse of
  `EndIslandIngressSkill` when the dragon scan reaches an observed frontier.
  It uses only the existing `ServerPlayer` movement, mining, crouch/place, and
  fresh semantic-frame contracts; it does not teleport, inspect a level, or
  invent a bridge destination.
- Focused combat and rally-evidence tests pass after that patch. The latest
  real Forge 65.1.1 zero-human selector run was
  `/tmp/mcai-dynamic-dragon-reentry-result-xQ0M6S`; it failed honestly after
  2,311 fight ticks with `fight_ender_dragon.no_visible_combat_target`. The
  body advanced to approximately `(47.8,49.0,0.6)`, mined 30 observed
  End-stone blocks, consumed no arrows, and never produced a dragon damage
  event or kill/return. The fight-specific ingress completion radius is now
  32 blocks and the re-entry child ran once, ending with the bounded
  `lastIngressResult=failed:skill_failure`; natural dynamic dragon combat
  remains a release blocker and is not claimed as passed.
- The earlier two real runs remain negative evidence: the one-cell movement
  run ended at `(54.9,49.0,0.5)` with the sky budget exhausted; the prior
  unpatched run ended near `(55.8,49.0,0.5)` after four rally scans. These
  results show real physical progress but not a complete End fight.
- Commit `bf70b73f891ab816d21ca8110d198654cd3ea369` is published on the
  public `main` branch through Git Data API objects. A fresh clone verified
  all five changed blob hashes byte-for-byte and `compileJava` passed for
  Forge 65.1.1. The local release JAR remains
  `build/libs/mcai_companion-0.1.11-dev-mc26.2.jar` with SHA-256
  `5e703f7945df7bdea2283033d9b36688a292609af20a017962c136935df69f67`.

- The latest Forge 65.1.1 natural End ingress rerun passed in isolated
  directory `/tmp/mcai-end-ingress-piTmLV`: `All 1 required tests passed` in
  2.519 minutes. It reached natural End-stone inside the ready radius using
  the production headless `ServerPlayer`, observed mining/bridge actions, and
  no teleport or hidden terrain access. The preceding run that stalled in
  `VERIFYING_CURRENT_SUPPORT` was stopped and the over-strict extra support
  wait was removed; the focused JUnit suite is green again.
- A new release-excluded natural dynamic-dragon combat selector was exercised
  three times on Forge 65.1.1. Vanilla manager-dragon presence/motion remains
  real and AI-enabled, but the production fight still fails honestly with
  `fight_ender_dragon.no_visible_combat_target` after four bounded rally scans;
  the latest failure ended near `(69.99,49.0,0.49)` with zero shots and a
  first-person frame showing a low natural End-stone ceiling. This is now the
  active P0 combat gap: the controller must mine/escape an observed overhead
  obstruction and reacquire the live dragon before any dragon-kill claim.
- Fight admission now requires the natural standing-cell evidence, then lets
  the production skill clear a bounded, freshly observed low roof before
  scanning for the dragon. Observed rally candidates are filtered to
  centerward progress; the latest run still found no damageable dragon after
  the bounded clearance and re-entry attempt. The focused
  `FightEnderDragonSkillTest`, ingress tests, and
  `EndIslandRallyEvidenceSourceContractTest` pass. The natural dynamic fight
  gate remains `FAILED`, not a release or speedrun result.

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
- A fresh Forge 65.1.1 rerun of
  `mcai_companion:natural_end_island_ingress` also passed after the observed
  wall/ceiling recovery changes. It completed in 2.518 minutes from the
  isolated directory `/private/tmp/mcai-end-ingress-current.CKtNcl`, with
  `All 1 required tests passed`. This remains physical ingress and
  dynamic-presence evidence only; it is not a dragon-combat, random-seed,
  Hardcore, speedrun, or rendered-client result.
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
against a post-start baseline. The latest authorized `mimo-v2.5` run passed in
39.98 seconds: SQLite recorded the full model-to-skill-to-action chain, ten of
twenty authored targets were damaged (the fixture threshold is ten), the body
moved, and body health stayed full. The prior 39.70-second run with sixteen
damaged targets and the earlier 38.28-second run with all twenty damaged remain
retained controlled evidence.
This remains a controlled multi-hostile slice, not human PVP, Hardcore,
random-seed, or M4 evidence.

The first three live iron-golem attempts remain negative evidence: the model
returned `START_SKILL(engage_observed_entity)` and SQLite recorded a vanilla
`attack_entity` dispatch that damaged the golem, but the body did not receive
a verified incoming attack. The root cause was stale creative/invulnerability
state inherited by the test body, not a model decision. The latest authorized
run clears those abilities and asserts genuine survival before chat. It
passed in 12.15 seconds: SQLite recorded 7,736 input and 148 output tokens,
HTTP-200 Responses output, schema/revision acceptance, skill start, vanilla
attack dispatch, body movement, golem damage, a real golem attack, and body
survival. This is a controlled neutral-mob slice, not a human PVP or Hardcore
result. A preceding invocation passed a literal placeholder instead of a
credential and made zero requests; it is harness/configuration negative
evidence, not a combat result.

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
Live MiMo iron-golem duel                         PASS (12.15-second bounded slice; 7,736 input/148 output tokens; model engage, vanilla attack, golem damage, verified incoming golem attack, body survival)
Live MiMo water clutch                             PASS (15.26-second bounded slice; HTTP-200 REPLAN plus local emergency water placement, exactly one vanilla bucket use, no fall damage; 11,588 model tokens)
Live MiMo farm work                                PASS (45.65-second bounded slice; two harvest/replant skills plus observed crop-field maintenance, A Seedy Place, 40,964 model tokens)
Live MiMo late End completion handoff              FAIL (137-second bounded slice; three real portal-activation decisions rejected under hostile pressure, SAFE_IDLE, 25,927 model tokens; no End entry)
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
- The latest evidence snapshot is public `main` commit
  `a838d6c857f4dcb376c89ce688062518ec9ba4d3`; a fresh clone matched both
  evidence files byte-for-byte and passed `compileJava`. The immediately
  preceding publication was repaired after a shell-prefix upload corruption;
  the repaired blobs now match local Git object hashes exactly.
