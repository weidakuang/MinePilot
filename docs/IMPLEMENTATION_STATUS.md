# Implementation Status

Last reviewed: 2026-08-20

## Current release line

- Repository: `MinePilot`; mod id: `mcai_companion`.
- Minecraft Java 26.2; Forge 65.0.0 inclusive through 66.0.0 exclusive;
  Java 25.
- Development version: `0.1.10-dev-mc26.2`.
- Public repository: `https://github.com/weidakuang/MinePilot`; snapshot commit
  `11975c380c243aa331ac19edbb0e494d99663313` carries source tree
  `32c864eff3c26c0119ee23f62537b58ff9acac49` and passes a fresh-clone build.
- Local development branch: `main`.
- Current development includes the focused End fall-recovery and bounded
  dragon melee-cycle corrections described below. Git remains the authority
  for the exact local and published snapshot identifiers.
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
- The End fall reflex now treats only the Nether as water-forbidden. A focused
  regression proves that an owned water bucket is equipped and used only after
  a fresh visible and reachable End-stone landing surface satisfies the
  existing fair-action gates. The Nether hay-bale regression remains intact.
- Dragon combat now resets its finite melee burst after four completed normal
  arrows, allowing a newly perched and safely reachable dragon to be attacked
  again. A focused regression proves the cycle reset and the next vanilla
  attack request; no dynamic-manager-dragon pass is claimed.
- Offline End-entry/dragon/return baseline passes on a controlled arena. The
  baseline uses a real `ServerPlayer`, real arrows/melee, static visible dragon
  parts, a 24-swing melee burst, bounded eight-tick ranged retreat, and a
  128-arrow budget. It is a no-model physical lower bound, not a live-model or
  random-seed result.
- Forge 65.x isolated lifecycle, no-human startup, delayed first-human anchor,
  emergency golem, zombie/skeleton horde, parkour, furnace/menu, water clutch,
  and compatibility smoke tests have passed in prior fresh servers. Their logs
  remain component evidence only.
- The zero-human cross-dimension regression passes on Forge 65.0.0 and 65.1.1
  with identical Overworld/Nether section coordinates. It verifies that the
  headless player's ordinary destination simulation window ticks an outlying
  entity and scheduled block update, then retires the old dimension window.
- Stronghold portal-room search and activation now have fair first-person
  diagnostics, bounded interior confidence, dead-end backtracking, and a
  regression guard against walking without observed support. The live entry
  harness keeps the chat sender connected until the production remove/relogin
  anchor transaction settles.
- The current working tree passed the focused End survival/dragon tests, the
  complete offline JUnit suite, the release build and jar verification, and
  the compatibility checker. The latest protocol-only change separately
  passed all 65 Python audit tests and ten mutation variants. The artifact is
  `build/libs/mcai_companion-0.1.10-dev-mc26.2.jar` with SHA-256
  `79859cc6ce7d4ff1c7ec0bf16b13a7ef333c696ff9ff4f9ca863f7e4485cdac1`.

### Live MiMo result (latest)

The latest authorized `mimo-v2.5` focused test used the supplied provider URL
and a real-time Forge 65.1.1 server. From one Chinese player task, the model
selected the stronghold search, twelve-eye portal activation, End entry,
dragon fight, and return-portal skills. Vanilla attributed the dragon death to
the companion; the route verifier recorded `DRAGON_KILLED` at game tick 4802
and `RETURNED_FROM_END` at tick 7425. SQLite contains the ordered model and
vanilla-action audit chain, and Forge reported `All 1 required tests passed`
in 6.188 minutes. The authored maze and controlled End arena disable ambient
spawning, use a static dragon target and bounded loaded chunks, and activate a
test return portal after the credited kill. This is a controlled causal chain,
not a dynamic vanilla dragon fight, random-seed, Hardcore, or speedrun claim.

The earlier standalone controlled End victory/return run passed in 2.183
minutes and remains narrower historical component evidence.

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
movement (`travel_to`, 12.26 seconds), follow (`follow_entity`, 12.66 seconds),
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

The next real-model surprise-defense run deliberately placed a Zombie behind
the body and exposed a genuine safety regression: the body could receive a
fair directional damage cue yet spend the whole bounded scan window looking
instead of separating. The first run (`run-live-surprise-20260814e`) killed
the body and is retained as a failure. The emergency controller now uses one
bounded sneak-protected step away from that recent damage direction, based
only on the first-person damage cue and observed adjacent-cell evidence. The
patched rerun (`run-live-surprise-20260814f`) passed in 15.27 seconds: the
body survived, reacquired and attacked the Zombie through vanilla, and earned
`Monster Hunter`. The live model issued a replan during the pressure window;
this is evidence for model request plus local emergency recovery, not a claim
that the model alone cleared a hostile encounter.

The clean bounded multi-hostile rerun passed in 40.14 seconds with real
`mimo-v2.5` on Forge 65.1.1. After the normal first-human anchor transaction,
the fixture recreated the same three Zombies and three Skeletons around the
authoritative body and began damage attribution only when the model-selected
skill was genuinely `RUNNING`. The Chinese team request first caused a model
replan, then produced HTTP-200 `START_SKILL(engage_observed_entity)` and the
full SQLite causal chain; four of six authored targets were damaged, the body
moved, and body health remained full. Earlier stale-target, ambient-mob, and
pre-start attribution runs remain superseded evidence. This is bounded
evidence, not the formal Hardcore, random-seed, or M4 result.

The requested ten-Zombie/ten-Skeleton extension now also passes as a real
MiMo causal slice in 39.70 seconds in the latest authorized rerun. The fixture
recreates twenty authored targets after the body relogin, requires
`engage_observed_entity` to enter
`RUNNING`, and counts only target health loss against a post-start baseline.
SQLite recorded `skill_started` and `low_level_actions_issued`; sixteen of
twenty targets were damaged after the skill entered `RUNNING` (the fixture
threshold is ten), the body moved, and body health remained full. This is
controlled multi-hostile evidence, not human PVP, Hardcore, random-seed, or
M4 evidence. The previous 39.19-second run remains retained as controlled
evidence.

The first three live iron-golem duel attempts remain negative evidence: the
real model selected `START_SKILL(engage_observed_entity)` and damaged the golem,
but the body did not receive a verified incoming attack. The root cause was in
the fixture, which inherited stale creative/invulnerability abilities. The
latest authorized MiMo run (12.21 seconds) now clears those abilities and
asserts genuine survival before chat; it passed model response validation,
vanilla attack dispatch, body movement, golem damage, a real golem attack, and
body survival. This is a controlled neutral-mob slice, not a human PVP or
Hardcore claim.

The external evidence verifier now receives its expected run nonce from the
immutable manifest instead of deriving it from the submitted Oracle log.
Every Actor, Observer, Oracle, and Oracle-result event in both the functional
and delayed-first-human slices must carry that exact nonce. Focused regressions
reject a required Oracle event with no nonce, an internally consistent bundle
from a foreign run, and mixed delayed-anchor client evidence. All 65 Python
protocol tests and all ten mutation variants pass; an actual `prepare` run
wrote `BINDINGCHECK` into the manifest without persisting model credentials.
This hardens evidence provenance only and does not turn the Linux/Xvfb client
gate into a gameplay pass.

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

1. Preserve the controlled stronghold-room-to-return pass while adding a
   separate manager-owned, AI-enabled vanilla dragon slice; never replace the
   stricter static physical regression or promote either to random-seed proof.
2. Close natural End ingress and terrain-safety gaps before claiming dynamic
   combat: the ordinary End spawn is outside the fair entity range and current
   no-target recovery only returns to its original local rally point.
3. Keep regression coverage for observed target points, portal reach limits,
   projectile lead, dragon-part reacquisition, and bounded retry behavior
   without direct world mutation.
4. Run the formal Actor/Observer and hidden-seed protocols only on an authorized
   Linux/Xvfb worker with the exact frozen jar; retain `NOT_RUN` when unavailable.

No status above should be interpreted as a guarantee that an arbitrary random
world can be completed within two hours.
