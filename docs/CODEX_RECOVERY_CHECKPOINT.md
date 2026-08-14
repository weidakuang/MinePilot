# Codex Recovery Checkpoint

Last updated: 2026-08-14T01:18:03Z

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
complete controlled M1 foundation route and a complete controlled Nether
portal handoff. Final evidence:

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
  `All 1 required tests passed` in a 1.903-minute real-time GameTest run.

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

## Changes in the current worktree

- `FairPerceptionSampler`: multipart Ender Dragon samples begin at a collider
  center and bounded horizontal points, with ordinary first-person LOS checks.
- `FightEnderDragonSkill`: bounded 24-swing melee burst, eight-tick ranged
  retreat, projectile/damage evasion, crystal firing-lane recovery, and
  transient child-shot failure recovery.
- `SkillSupervisor`: exposes the last checkpoint for timeout diagnostics.
- `EmbodimentGameTests`: offline dragon window/arena diagnostics and 128-arrow
  baseline.
- `LiveModelChatGameTests`: live dragon/return evidence for visible entities,
  dragon parts, danger signals, revisions, arrows, checkpoint state, and the
  isolated no-ambient-mob End arena.
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
Live MiMo End victory and return                  PASS (controlled real-model chain; Forge 65.1.1)
Live MiMo movement, follow, surprise defense, food PASS (four controlled live-model slices)
Live MiMo container withdrawal and item collection PASS (two controlled live-model slices)
Live MiMo foundation bootstrap and shelter       PASS (8.912-minute controlled slice; isolated SQLite)
Live MiMo Nether portal build and entry          PASS (52.17-second controlled slice; isolated SQLite)
Live MiMo stronghold search, portal activation, End entry PASS (controlled real-model slice; debug15)
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
2. Extend the controlled live causal chain from End entry into the release-
   excluded combat arena and return path, while keeping portal target and
   movement regressions in the offline suite.
3. Run formal Actor/Observer and hidden-seed gates only on an authorized
   Linux/Xvfb worker with the exact frozen jar. Keep missing infrastructure as
   `NOT_RUN`.

## Repository and release state

- Public repository: `https://github.com/weidakuang/MinePilot`.
- Main backup commit before this work: `8ba8a554aa461f0ce2c09c3924d996cbe0854d82`.
- Forge 65.x backup branch: `mc26.2-forge65`.
- Local branch: `main`.
- Local commit: `885d7c898461c829cc52683780c579e6817bd140`.
- Public `main` was updated through the authorized GitHub connector at commit
  `0a35efeec519cd0fa1a7bda1c25980d6973b0b16`; generated run directories remain
  ignored or disposable.
- API keys are process-only during live tests and are never written here.
