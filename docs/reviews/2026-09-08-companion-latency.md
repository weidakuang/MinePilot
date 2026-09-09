# Persistent companion latency — 2026-09-08

## Result and scope

The final controller completed two nearby, bare-hand, three-birch-log requests
within 30 seconds on a real, normal-tick Minecraft 26.2 / Forge 65.0.9 dedicated
server. The model was `gpt-5.6-luna` through the installed persistent Skill's
Codex app-server transport. This is a model-controlled normal-play test, not a
newly spawned general-purpose Codex operator task or an internal-provider test.

| Trial | First visible model reply | First inventory item | Three inventory logs | Outcome |
| --- | ---: | ---: | ---: | --- |
| Initial latency patch | 10.462 s | 22.464 s | 30.897 s | Missed 30-second target |
| Compact approval, before historical-state fix | 7.419 s | None | None | False completion in chat; failed |
| Final patch, first game request on prepared connection | 7.695 s | 18.109 s | 25.847 s | Passed |
| Final patch, subsequent request on same controller | 5.193 s | 14.627 s | 22.326 s | Passed |

The original 53.331-second independent trial included starting a separate Codex
task and reading the Skill. It used a different control path; it is not a
like-for-like latency baseline for the persistent listener. The two final
samples are not p95 statistics or a universal guarantee for model/network delays,
distant trees, difficult terrain, large quantities or other gameplay objectives.

## Implementation

- Send a model-authored message attached to a successful general tool call.
  Previously `action: tool` silently discarded that message. A new actionable
  request asks for one brief acknowledgement with its planning call; subsequent
  tool calls remain quiet unless there is useful new information.
- Prepare the app-server connection and ephemeral model context when connecting
  the listener. Preparation runs in the background and performs no inference or
  game action. The first real request still requires model inference.
- Give evaluated collection/mining plans a compact approval schema. The model
  compares complete returned options, then approves an exact option ID or rejects
  the plan. The host binds it to the immutable event's request ID; invented
  options and approval outside a plan event are rejected. Server validation still
  governs the public choose call. This never auto-selects a collection plan.
- Do not ask the model for a redundant waiting decision after an asynchronous
  choose/resume has successfully entered EXECUTING. Player chat and acquisition
  events remain available while the game performs normal physics and mining.
- Remove duplicate plan payloads from the observation accompanying their result.
  Keep idle inventory organization out of active mining/collection.
- Skip historical terminal events when connecting a listener. For fresh chat,
  remove old terminal job payloads from the current observation and identify their
  status tools for explicit historical queries. Keep active jobs and current
  physical state. Instructions require a new plan for a new action request,
  including verbatim repeated requests. Existing terminal-event reporting remains.
- Retain 32 timing-only records and a transport-readiness flag in listener status.
  No model decision text or reasoning is exposed through those fields.
- On listener shutdown, cancel the collection parent before mining/navigation so
  it cannot restart a child after the controller disconnects.

## Evidence protocol

Local evidence is retained in `run-collection-latency-20260908/`:
`final-evidence.json`, case snapshots, controller timing snapshots and
`allowed-observations.jsonl`. The original failures are retained.

The observer filtered public MCP data to inventory/hotbar, dimension, coordinates
and visible chat. The parent did not inspect model decisions or private reasoning,
select options, provide mining steps or inject a completion message. The Luna
controller could use the normal public game observation and tool surface.

The server used survival mode, an initially empty inventory and bare hands. A
vanilla birch feature was placed four blocks east of the initial body position
before each new fixture. Setup teleports/clears/features were outside gameplay;
no setup mutations were made during a measured request. Trial 2 did not change
the world and trial 3 reused its intact tree. Trial 4 used another tree and a
fresh empty-inventory start, with the same listener/model as trial 3.

Final verified bodies in the overworld were approximately
`(24.12635, -60, 1.63671)` and `(43.12792, -60, 0.5)`, each with three birch logs.
Elapsed time starts at the observer's first receipt of the visible player command
and ends at its first receipt of the required inventory count. Sampling is every
150 ms plus MCP latency. Completion speech is optional and is not the completion
criterion. First movement may occur after a stationary break and is not a valid
proxy for the first gameplay action.

The listener stayed LISTENING between final trials. It and the isolated test
server were stopped after evidence capture. This test does not establish an
in-game human client joining XMCL or a live internal OpenAI-compatible provider.

## Validation and installation

All 60 Python tests and Skill validation passed. Regressions cover successful vs
rejected tool speech, mandatory model approval, request/option binding, background
execution without an extra decision, fresh chat during work, cancellation order,
transport preparation without inference, and historical-success isolation.

Only the Python Skill/controller and documentation changed for this optimization.
The installed Skill is a symlink to this repository, so a newly started listener
uses the update. Java physics, break speed and the installed JAR were unchanged;
no JAR rebuild or replacement is claimed. The installed JAR checksum remains
`8fa48ec45bac6f7febfe2985a314a8123ab744f75bf2b7fa9db1d8d21f6cb04b`.
