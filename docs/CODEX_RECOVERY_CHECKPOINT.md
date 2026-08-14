# Codex Recovery Checkpoint

Last updated: 2026-08-14T06:04:00Z

This checkpoint is intentionally concise and English-only. Runtime chat and
multilingual test fixtures may contain other languages; public repository
documentation must not.

## Current objective

Continue the MinePilot M1–M4 objective on Minecraft Java 26.2 / Forge 65.x with
real, fair `ServerPlayer` behavior. Do not claim professional-companion,
random-seed, two-hour, or M0–M4 completion until the artifact-bound protocols
pass.

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
passed in 2.183 minutes. The full stronghold-to-End run still has not passed:
recent attempts reached the End and then either lost the fair dragon target or
were killed by dragon magic. The latest attempt failed with
`fight_ender_dragon.no_visible_combat_target` after 2.701 minutes; it did not
record `DRAGON_KILLED` or `RETURNED_FROM_END`.

The latest damage-focused patch also makes a shielded body perform one bounded
side-step after a directionless recent damage cue (such as indirect magic),
instead of remaining shield-only. This is covered by a regression test, but a
new full stronghold real-model run is still required before calling it an
effective dragon survival fix.

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

## Changes in the current worktree

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
Live MiMo stronghold-to-End dragon victory       FAIL (full route still lacks a stronghold-to-return PASS; isolated End chain now passes)
Live MiMo End return after anchor/portal fixes   PASS (model fight, vanilla return portal, stable UUID; bounded fixture)
Live MiMo movement causal chain                  PASS (12.26-second real-time slice; START_SKILL travel_to and vanilla move)
Live MiMo follow causal chain                    PASS (18.17-second real-time slice; START_SKILL follow_entity and vanilla move)
Live MiMo surprise-zombie defense (pre-fix)     FAIL (12.69-second slice; rear hostile killed body while scanning)
Live MiMo surprise-zombie defense (patched)     PASS (15.27-second real-time slice; bounded damage separation and vanilla counterattack)
Live MiMo golden-apple consumption               PASS (15.26-second real-time slice; vanilla item-use consumption)
Live MiMo Chinese combat task                    PASS (18.80-second real-time slice; engage_observed_entity, vanilla attack, Monster Hunter)
Delayed first-human anchor lifecycle             PASS (production zero-human startup GameTest; no rendered-client claim)
Delayed first-human anchor during emergency      PASS (Forge 65.1.1 production GameTest; deferred relogin after survival clears)
Initial-anchor no-safe-placement guard           PASS (focused source contract and patched live movement slice)
Live MiMo directionless-damage shield regression PASS (offline JUnit; real stronghold effectiveness NOT_PROVEN)
Live MiMo directional-damage separation         PASS (real 15.27-second slice; model request plus emergency recovery)
Roof-jump physical recovery contract             PASS (no-model GameTest)
Focused shelter-material/building JUnit tests    PASS
Exact Forge 65.1.1 dedicated lifecycle smoke      PASS (real dedicated server; no functional claim)
Exact Forge 65.1.1 two-boot persistence smoke     PASS (real restart; no functional claim)
Delayed first-human anchor client smoke           NOT_RUN (macOS lacks Linux/Xvfb; no client claim)
Full offline Gradle/JUnit, release jar, compatibility, and Python audit     PASS (61 Python tests)
Formal Actor/Observer client gate                 NOT_RUN
Hidden random Hardcore M1/M2/M4 gates             NOT_RUN
M0/M1/M2/M3/M4 product milestones                 NOT_RUN
```

The offline baseline proves only a controlled vanilla physical lower bound. It
does not substitute for a live model, rendered client, natural world, or hidden
seed statistic.

## Immediate next steps

1. Preserve all live progression results as controlled evidence; do not
   promote them to a random-seed or speedrun claim.
2. Continue the targeted stronghold-to-End real-model investigation. The
   remaining production gate is fair dragon reacquisition plus survival under
   vanilla magic damage; the isolated End slice passes, but the full route
   still lacks a stronghold-to-return PASS.
3. Add a bounded live multi-hostile combat slice after the directional-damage
   fix, then run formal Actor/Observer and hidden-seed gates only on an
   authorized Linux/Xvfb worker with the exact frozen jar. Keep missing
   infrastructure as `NOT_RUN`.

## Repository and release state

- Public repository: `https://github.com/weidakuang/MinePilot`.
- Main backup commit before this work: `2158b9a722b593d5d6d66087402384c934d86d80`.
- Forge 65.x backup branch: `mc26.2-forge65`.
- Local branch: `main`.
- The previous validated combat-fixture commit is backed up to public `main`;
  the directional-damage fix and its fresh evidence are the next backup.
  Generated run directories remain disposable and must not be staged.
- API keys are process-only during live tests and are never written here.
