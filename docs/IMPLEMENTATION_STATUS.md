/Users/weida/.zprofile:7: no such file or directory: /opt/homebrew/bin/brew
# Implementation Status

Last reviewed: 2026-08-13

## Current release line

- Repository: `MinePilot`; mod id: `mcai_companion`.
- Minecraft Java 26.2; Forge 65.0.0 inclusive through 66.0.0 exclusive;
  Java 25.
- Development version: `0.1.10-dev-mc26.2`.
- Public backup: `https://github.com/weidakuang/MinePilot`.
- Local development branch: `agent/minecraft-companion-0.1.9`.
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
- The current working tree passed the focused combat test, the complete offline
  JUnit suite, the release build and jar verification, the compatibility
  checker, and the 61-case Python audit after this change. The artifact is
  `build/libs/mcai_companion-0.1.10-dev-mc26.2.jar` with SHA-256
  `7ecf2e1d2a8a5d6a7192a7603cdb5fd47b76681703cf1bfec1dd0d98f2eb9e8c`.

### Live MiMo result (latest)

The authorized `mimo-v2.5` focused test used the supplied provider URL and a
real-time Forge 65.1.1 server. The model produced a valid
`START_SKILL(fight_ender_dragon)` response; the body performed real melee and
bow actions, destroyed the observed crystal and dragon, then selected
`find_and_enter_observed_portal`. The route verifier recorded `DRAGON_KILLED`
at game tick 2164 and `RETURNED_FROM_END` at tick 2283. The same body UUID
returned through the activated portal and the Forge test reported
`All 1 required tests passed` in 1.903 minutes. The arena disables only
ambient mob spawning after installing the dragon and crystal, so this is a
controlled live-model chain, not a random-seed, Hardcore, or speedrun claim.

Earlier controlled MiMo slices reached End entry and demonstrated follow and
other task chains, but they are historical evidence and do not upgrade the
current formal gates.

### Fresh controlled live slices

Using the same real `mimo-v2.5` gateway, the current line also passed bounded
model-to-action slices for ordinary movement, follow, surprise-zombie defense,
and owned golden-apple consumption. The model returned an actionable skill in
each case, and the verifier observed vanilla movement, follow, combat, or food
use. See `docs/progress/CONTROLLED_LIVE_RUNS.md`; these remain controlled
evidence and do not upgrade any formal gate.

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

1. Preserve the live End victory/return evidence and extend the same causal
   audit to natural-world progression.
2. Add or refine regression coverage for observed target points, projectile
   lead, dragon-part reacquisition, and bounded retry behavior without direct
   world mutation.
3. Extend the controlled live causal chain to natural-world progression and
   add fresh combat regression coverage as observations expose new cases.
4. Run the formal Actor/Observer and hidden-seed protocols only on an authorized
   Linux/Xvfb worker with the exact frozen jar; retain `NOT_RUN` when unavailable.

No status above should be interpreted as a guarantee that an arbitrary random
world can be completed within two hours.
