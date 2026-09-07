# Movement and conversation usability repairs

Date: 2026-09-06. This is a development report, not full companion or parkour acceptance.

## Reproduced problems

The player's real server chat showed repeated acknowledgements for one destination,
10–17 second model replies, a spoken promise to jump without a jump tool, and
unhelpful no-route replies. Source review found additional concrete mechanisms:

- Each event launched a separate Codex CLI decision. Old chat was included in
  route events, allowing an event to repeat the original navigation request.
- Chat interrupting route selection could consume the PLAN_READY notification
  without ever choosing the route.
- After applying a decision, the listener could dispatch the previous navigation
  status, adding a spurious model turn before starting a new route.
- Flat traversal used sprint-jumping and chased overshot grid waypoints. Stuck
  detection counted physical bouncing as progress rather than progress toward
  the next waypoint.
- Gap expansion could consume the planning budget even for a direct flat walk.
- A failed route had no explicit partial-approach outcome.

## Implemented behavior

The public request/visible-acknowledgement/plan/choose/execute/verify sequence is
retained. Normal persistent play can choose a unique route locally after the
model authorizes its goal, only if the route predicts zero damage, needs zero
support blocks, lists no hazards, and supports the requested pace. Multiple or
partial routes still require a model choice. Direct independent model-choice
tests do not use this optimization.

The listener now reuses a local Codex app-server connection over private stdio,
with ephemeral model threads, bounded rollover, a read-only workspace, disabled
shell/apps/browser/computer/skill-search features, and no approval or external
execution delegated to the model. Only typed decisions reach the public game MCP.
Game chat remains data. No provider key was introduced or copied. World loss
closes the model session; the listener still waits for the world to return.

Route-event output schemas prevent replaying a navigation request. Stale route
choices are discarded; interrupted ready events resume after chat. Conversation
history is bounded. Repeated obstruction replans are limited. Ordinary target
movement can trigger a local replan without repeated acknowledgement; only the
same unique-safe-route conditions permit local execution afterward. Locally
handled stop chat is marked so the model does not repeat or undo it.

Flat walking has a safe direct-path fast path, no automatic bunny-hopping,
overshoot handling, and waypoint-progress timeouts. A normal step-up jump is
requested once per step. Mandatory gap takeoff remains a separate controller.

`jump_once` performs a vanilla jump and exposes `jumpPhase`; actual ascent and
landing determine its result. It respects the existing vanilla jump cooldown.
`forward_blocks` resolves a short relative navigation target on the server from
the current body heading. Neither action assigns position or velocity.
An idle body may turn toward a nearby visible player who spoke, without changing
movement steering while a task executes. That attention behavior has not yet had
real-client visual acceptance.

Player-target requests in normal companionship opt into a safe partial approach.
The planner retains the original target and a separate `partialDestination`.
`APPROACHED` means only arrival at that closer evaluated reachable point; it is
never `COMPLETED` for the original target. Exact-goal tests remain opt-out by
default. This is the closest useful candidate in the bounded search, not a proof
of the globally closest position. No complete route can still mean missing
traversal support or exhausted search; the model must not invent an obstacle.

## Verification and attribution

Final Java build/JUnit: 14 tests, zero failures. Python: 22 tests, zero failures.
Skill validation passed. `navigation_repair_regressions` passed one Forge test
containing 12 physical scenarios: exact start, airborne completion, lost support,
new hazard, moved human target, four-block jump, ladder exit, single jump, direct
flat sprint, ascending full-block steps, and a physically observed partial
approach to an unreachable elevated target, plus stopping a pending jump without
retaining or repeating its input. These are source-informed GameTests,
not Luna independently completing the supplied parkour world.

A separate real dedicated Minecraft 26.2 / Forge 65.0.9 flat-world server ran at
25570 with its MCP on 25770. The host sent ordinary backend console chat through
the same runtime chat ingress. Luna interpreted those messages into public tools;
the host's unique-safe-route policy could perform route selection. The observer
recorded only dimension, coordinates, inventory/hotbar, and visible chat. No
teleport or world edit was used during these movement trials. Inventory stayed
empty and the body was configured in adventure mode. This was a backend chat
test, not a real-client chat/UI acceptance test.

The first fresh-CLI run recorded:

| Task | Chat to motion/ascent | Observed outcome |
| --- | ---: | --- |
| Jump once | 8.11 s | 1.252-block rise and return to start |
| One block forward | 9.37 s | 1.004-block displacement |
| Ten-block walk | 16.73 s | 10.013-block path, 0.006-block final target error |

After model connection reuse and stale-status repair, a warmed session recorded:

| Task | Chat to motion/ascent | Observed outcome |
| --- | ---: | --- |
| Jump once | 4.00 s | 1.252-block rise and return to start |
| One block forward | 5.29 s | 1.004-block displacement |
| Ten-block walk | 6.26 s | 10.013-block path, 0.006-block final target error |

The ten-block acceptance sample completed at 10.64 seconds after the command,
including two seconds of observation inside the target radius. During the longer
sneaking task, game chat was sent at 17:44:22, Luna replied at 17:44:27 while
movement continued, and the observed arrival gate completed at about 17:44:39.
These are a few measured runs, not a percentile latency guarantee. The last
transport race guards and stop-message de-duplication were unit/build checked
after that warmed run; they were not included in its latency measurement.

Those historical raw records remain in `run-luna-usability-20260906/`: `results.json`,
`results-warm.json`, `allowed-observations*.jsonl`, and visible server chat logs.
The current development JAR SHA-256 after the final build is
`5098dbb330407d81a3dd25889c959228ee19853ab64dfcd61c31e93794684583`.
No installed XMCL JAR, original user save, Git history, or GitHub state was changed.

### Resumed source reconciliation and public-interface retest

At resumption the working tree did not match all earlier documented fixes. The
user confirmed that no other task was editing it. A local archive was preserved
before reconciling the source; no reset or retired implementation was used.
The newer persistent Codex transport was retained, with turn-id attribution for
completed message events. Missing partial-plan, relative-target, safe-route
selection and stale-status handling were restored and regression checked.

The first resumed Luna trial failed: the body could jump in a GameTest, but
`jump_once` was absent from the public MCP dispatcher/catalog. The failed record
is retained in `run-luna-usability-resume-20260906/results.json`. The public tool
and `jumpPhase` were then connected, with exclusive navigation/jump ownership.
Gameplay tool errors now retain their public reason for a bounded explanatory
model event; that event cannot retry the action. Target resolution and snapshot
failures enter FAILED with their reason instead of stranding an acknowledged
request. Local stop also marks idle/pending-model requests as handled and clears
the actual control frame.

A fresh independent Luna controller then interpreted ordinary backend chat on
`run-luna-usability-final-20260906`. Observation remained restricted to body
coordinates/dimension, empty inventory/hotbar and visible chat; no private model
output was inspected. Its results were:

| Task | Chat to motion/ascent | Observed outcome |
| --- | ---: | --- |
| Jump once | 4.87 s | 1.249-block rise and return to `(0.5,-60,0.5)` |
| One block forward | 5.28 s | 1.004-block displacement |
| Ten-block walk | 6.74 s | 10.013-block path; final `(10.50634,-60,1.50394)`, target error 0.00635 |

Ten-block arrival sampling ended 11.10 seconds after chat, including two seconds
inside the target radius. In the subsequent sneaking trial, the human chat was
sent at 20:14:09 JST, Luna replied at 20:14:14, and arrival sampling ended at
20:14:26. During the reply second the body's X advanced from 17.728 to 18.744;
at 20:14:24 it was near X=30.221. Both the reply and continued motion were
observed, rather than treating a sent question as proof of conversational success.

An exact coordinate target eight blocks across and six blocks above was requested
at 20:15:43. At 20:15:53 visible chat accurately reported an exhausted search,
the current coordinates, ten-block separation and six-block elevation, without
inventing a physical obstacle. The observer retained the subsequent coordinate
window in `unreachable-observations.jsonl`.
The full 35-second window had zero displacement and zero jump rise.
Partial approach was intentionally
disabled for this exact-coordinate test; the partial behavior is covered by the
separate physical scenario, not an independent Luna player-follow claim.

The final local-stop change was made after the Luna server started and was
validated by the additional physical scenario, not by that Luna latency run.
A final wording-only prompt change discourages repetitive apologies and requests
for a new instruction; the preceding timings do not validate its conversational
effect. Build/physical logs are retained under `run-usability-stop-20260906/`.

## Remaining limitations

- Roughly five to nine seconds in the latest commands is still noticeable;
  first model connection and provider latency can be slower.
- General half-block, stair-shape, low-ceiling, swimming and natural-terrain
  traversal are not established by the full-block step/ladder tests.
- The supplied parkour world has not been completed by an independent Luna.
  Earlier source-informed partial traversal must not be advertised as that pass.
- Earlier run `run-luna-parkour-20260906` copied the original world but the server
  defaulted to normal difficulty, overriding its peaceful setting. Later parkour
  diagnostic runs preserved peaceful. That earlier setup mismatch is retained
  as a limitation, not silently corrected in historical evidence.
- A dynamic arrival task still is not an indefinitely maintained follow task.
