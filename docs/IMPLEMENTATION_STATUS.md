# Implementation Status

Last reviewed: 2026-08-14

## Current release line

- Repository: `MinePilot`; mod id: `mcai_companion`.
- Minecraft Java 26.2; Forge 65.0.0 inclusive through 66.0.0 exclusive;
  Java 25.
- Development version: `0.1.10-dev-mc26.2`.
- Public backup: `https://github.com/weidakuang/MinePilot`.
- Local development branch: `main`.
- Latest local commit: see the public `main` history for the current
  validation commit.
- Forge 65.x major-line backup branch: `mc26.2-forge65`.
- Artifact status: `NON_RELEASE` until all formal gates pass.

## Evidence levels

The project reports implementation, controlled no-model physical checks,
authorized live-model/client slices, and formal release gates separately. The
first two levels cannot be promoted to a professional-companion, random-seed,
or speedrun claim.

## Implemented foundations

- Vanilla `ServerPlayer` body, stable UUID, embedded connection lifecycle,
  player-list login/removal, keepalive/teleport confirmation, save/restart,
  no-human dedicated-server presence, delayed first-human anchoring, and TAB
  identity disclosure.
- Server-authoritative config screen with agent name validation, color,
  `0.0–1.0` temperature, local 64×64 skin import, arm model, provider URL/key/
  model fields, system preference, first-run tutorial, scrollable layout, and
  responsive Save/Validate/Back controls.
- Cross-platform credential status paths for macOS Keychain, Windows DPAPI,
  Linux Secret Service, process-only injection, and deterministic restart
  precedence without writing secrets to world/config/database/logs.
- Single-flight Java 25 model gateway with Responses/Chat Completions probing,
  schema validation, revision checks, soft/hard deadlines, cancellation, and
  redacted audit events.
- First-person fair perception, loaded-chunk bounds, FOV/occlusion/LOS,
  multipart dragon collider evidence, semantic memory, SQLite WAL/FTS5/R*Tree,
  waypoints, portal graph, MCP, and Xaero structured waypoint intake.
- Local 20-TPS survival and movement lane: follow, travel, mining, crafting,
  menus, food, shielding, fall/water clutch, conservative parkour, bridge and
  tower skills, emergency PVE, visible item pickup, shelters, crops, portal
  casting, Nether/End controlled progression, and combat recovery.

## Recent focused evidence

### Passed controlled physical checks

- JDK25 focused combat, perception, and dragon unit tests pass.
- Offline End-entry/dragon/return baseline passes on a controlled arena. The
  baseline uses a real `ServerPlayer`, real arrows/melee, static visible dragon
  parts, a 24-swing melee burst, bounded eight-tick ranged retreat, and a
  128-arrow budget. It is a no-model physical lower bound, not a live-model or
  random-seed result.
- Forge 65.x isolated lifecycle, no-human startup, delayed first-human anchor,
  emergency golem, zombie/skeleton horde, parkour, furnace/menu, water clutch,
  and compatibility smoke tests have passed in prior fresh servers. Their logs
  remain component evidence only.
- Stronghold portal-room search and activation now have fair first-person
  diagnostics, bounded interior confidence, dead-end backtracking, and a
  regression guard against walking without observed support. The live entry
  harness keeps the chat sender connected until the production remove/relogin
  anchor transaction settles.
- The current working tree passed the focused combat test, the complete offline
  JUnit suite, the release build and jar verification, the compatibility
  checker, and the 61-case Python audit after this change. The artifact is
  `build/libs/mcai_companion-0.1.10-dev-mc26.2.jar` with SHA-256
  `5f656e8c0131a0491e0e200030390b41e120f0e1fb8662232673459c4da7e94c`.

### Live MiMo result (latest)

The authorized `mimo-v2.5` focused test used the supplied provider URL and a
real-time Forge 65.1.1 server. The model produced a valid
`START_SKILL(fight_ender_dragon)` response; the body performed real melee and
bow actions, destroyed the observed crystal and dragon, then selected
`find_and_enter_observed_portal`. The route verifier recorded `DRAGON_KILLED`
and `RETURNED_FROM_END`; the same body UUID returned through the activated
portal and Forge reported `All 1 required tests passed` in 2.183 minutes. The
run also covered the first-human cross-dimension anchor guard and the 16-block
fair portal approach bound. The arena disables only ambient mob spawning after
installing the dragon and crystal, so this is a controlled live-model chain,
not a random-seed, Hardcore, or speedrun claim.

Earlier controlled MiMo slices reached End entry and demonstrated follow and
other task chains, but they are historical evidence and do not upgrade the
current formal gates.

### Fresh controlled live slices

Using the same real `mimo-v2.5` gateway, the current line passed bounded
model-to-action slices for ordinary movement, follow, surprise-zombie defense,
owned golden-apple consumption, chest withdrawal, item collection, a complete
foundation shelter route, and Nether portal construction/entry. The model
returned actionable skills and the verifier observed vanilla movement,
inventory/menu transactions, combat, food use, physical building, and portal
entry. See `docs/progress/CONTROLLED_LIVE_RUNS.md`; these remain controlled
evidence and do not upgrade any formal gate.

The newest controlled slice, `real_player_task_to_live_model_stronghold_portal_room_and_entry`,
also passed with the supplied `mimo-v2.5` gateway on Forge 65.1.1. The model
selected `search_stronghold_portal_room`, `activate_observed_end_portal`, and
`find_and_enter_observed_portal` in sequence. The verifier observed physical
maze movement, a dead-end and second-turn route, twelve ordinary Eye of Ender
transactions, nine active portal blocks, End entry, and the vanilla `The End?`
advancement. SQLite recorded the complete model request/response, decision
acceptance, skill-start, and low-level action chain. This is a bounded fixture
and does not upgrade M2, M4, or the formal Actor/Observer gate.

The latest live regression set also passed on the same real gateway: ordinary
movement (`travel_to`, 12.26 seconds), follow (`follow_entity`, 18.17 seconds),
surprise-zombie defense (14.27 seconds), and owned golden-apple consumption
(15.26 seconds). The movement and follow runs recorded the complete causal
sequence through `skill_started` and `low_level_actions_issued`; the defense
and food runs additionally prove that the local 20-TPS survival lane acts while
the model is waiting. A separate production zero-human startup GameTest passed
the delayed first-human anchor lifecycle. These are controlled slices, not
random-seed or rendered-client gates.

A fresh real-model combat rerun also passed after the test fixture was corrected
to log its embedded human beside the companion through the safe login path. The
first attempt had placed that player at the world origin; the legitimate
initial-anchor lifecycle removed the body before chat delivery, so that attempt
was stopped and is retained as a failure. The corrected 18.80-second run used a
Chinese chat command and recorded `conversation_task_accepted`, model HTTP 200,
schema/revision acceptance, `skill_started` for
`engage_observed_entity`, `low_level_actions_issued`, vanilla movement/attacks,
and the `Monster Hunter` advancement. It remains a one-zombie controlled slice,
not evidence for PVP, a hostile horde, Hardcore survival, or a random seed.

## Formal gate status

```text
M0 technical gate                         NOT_RUN
M1 basic survival                         NOT_RUN
M2 vanilla completion                     NOT_RUN
M3 expert companion                      NOT_RUN
M4 random Hardcore optimization           NOT_RUN
Live Actor + Observer causal slice        NOT_RUN
Rendered dual-client skin/animation      NOT_RUN
24-hour stability soak                   NOT_RUN
100-hour companion soak                  NOT_RUN
M1 unseen 100-seed statistic             NOT_RUN
M2 unseen 200-seed statistic              NOT_RUN
M4 hidden 1,000-seed statistic            NOT_RUN
```

The current macOS development host does not provide the isolated Linux/Xvfb
Actor/Observer worker required by the formal client gate. Provider credentials
and capability status must be supplied at run time. A model HTTP response alone
does not satisfy the causal audit.

## Next engineering steps

1. Preserve the live End-entry evidence and extend the same causal audit into
   the release-excluded End combat arena and return path.
2. Keep regression coverage for observed target points, portal reach limits,
   projectile lead, dragon-part reacquisition, and bounded retry behavior
   without direct world mutation.
3. Extend the controlled live causal chain to natural-world progression and
   add fresh combat regression coverage as observations expose new cases.
4. Run the formal Actor/Observer and hidden-seed protocols only on an authorized
   Linux/Xvfb worker with the exact frozen jar; retain `NOT_RUN` when unavailable.

No status above should be interpreted as a guarantee that an arbitrary random
world can be completed within two hours.
