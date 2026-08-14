# Controlled Live-Model Runs

This page records bounded development evidence for the `0.1.10-dev-mc26.2`
line. It is intentionally separate from the formal M0-M4 gates. The tests use
the real Java model gateway, a real Forge server tick loop, a real
`ServerPlayer`, and ordinary vanilla actions, but they do not use a random
Hardcore world or the rendered Actor/Observer protocol.

## Provider and privacy

- Model: `mimo-v2.5`.
- Base URL host: `api.slomerex.xyz` (the runtime normalized the API prefix to
  `/v1`).
- Credentials were injected only through the process environment.
- No key, authorization header, prompt secret, or model response body is
  stored in this repository, world data, SQLite, or public evidence.
- Runtime chat and GameTest fixtures may contain Chinese; this public record
  remains English-only.

## Results

All runs used Forge `65.1.1`, Minecraft `26.2`, and Java `25`. Each passing
test ran one case and exited cleanly with `All 1 required tests passed`; the
newest movement, follow, defense, food, and delayed-login results are frozen
in the current validation commit and are being backed up to public `main`.
They remain controlled evidence, not release artifacts.

| Scenario | Model decision evidence | Vanilla-world evidence | Result |
| --- | --- | --- | --- |
| `real_player_task_to_live_model_movement` | `START_SKILL(travel_to)` | The body walked from the fixture start to the requested point; no teleport or command was used. | PASS (controlled live slice) |
| `real_player_task_to_live_model_follow` | `START_SKILL(follow_entity)` with an observed player target | The body survived the initial-anchor relogin and followed the moving course through ordinary player movement. | PASS (controlled live slice) |
| `real_player_chat_to_surprise_zombie_defense` | `START_SKILL(engage_observed_entity)` after local damage/hostile observations | The emergency lane reacted before model completion and the body killed the visible zombie with normal combat actions. | PASS (controlled live slice) |
| `real_player_chat_to_critical_golden_apple` | `START_SKILL(consume_owned_food)` for `golden_apple` | The body consumed the owned item through the ordinary use path; the scenario also verified the follow-up food-decision integrity. | PASS (controlled live slice) |
| `delayed_human_login_after_zero_human_active` | No model request; production zero-human startup and login lifecycle | The body became active before any human joined, then rejoined through the bounded initial-anchor lifecycle beside the later human without a gameplay teleport. | PASS (controlled physical GameTest) |
| `real_player_task_to_live_model_end_victory_and_return` | `START_SKILL(fight_ender_dragon)` followed by `find_and_enter_observed_portal` | The body destroyed the observed End crystal/dragon, then entered the activated return portal with the same UUID after a first-human cross-dimension anchor guard. | PASS (2.183-minute controlled live slice) |
| `real_player_task_to_live_model_container_withdrawal` | An initial malformed `use_block` was rejected by local argument validation; the model then selected `use_block` and `transfer_menu_item`. | The body opened the real chest menu and transferred three oak planks through the vanilla menu path. | PASS (14.41-second controlled live slice) |
| `real_player_task_to_live_model_item_collection` | `START_SKILL(collect_observed_item)` for an observed item entity. | The body used the normal pickup path and the scenario verified the resulting inventory/stat delta. | PASS (16.86-second controlled live slice) |
| `real_player_task_to_live_model_foundation_bootstrap` | Multiple accepted foundation skills, ending with `build_shelter_step`; the model re-planned roof placement after observed occlusion. | The body mined/crafted/organized the M1 materials, physically built the dynamic shelter, survived the configured night, and the SQLite audit recorded `skill_completed.build_shelter_step`, `low_level_actions_issued`, `server_verified_auto_complete`, and `goalStatus=COMPLETED`. | PASS (8.912-minute controlled live slice) |
| `real_player_task_to_live_model_nether_portal_build_and_entry` | `START_SKILL(build_and_light_nether_portal)` followed by `START_SKILL(find_and_enter_observed_portal)`. | The body built and lit the portal using the ordinary placement/use path, entered it, and the portal-entry skill completed. | PASS (52.17-second controlled live slice) |
| `real_player_task_to_live_model_stronghold_portal_room_and_entry` | Four accepted real-model decisions: `survey_surroundings`, `search_stronghold_portal_room`, `activate_observed_end_portal`, and `find_and_enter_observed_portal`. | The body traversed the authored maze, visited a dead end and a second turn, inserted all twelve eyes through ordinary crosshair use and inventory verification, created nine portal blocks, entered the End, and triggered the vanilla `The End?` advancement with the same body UUID. | PASS (`debug15`, 1.627-minute controlled live slice) |

## Superseded live failures that drove fixes

These runs remain recorded because a green build alone is not evidence of a
working progression route:

- `run-live-foundation-20260814`: remembered workstation aim stayed in an
  unbounded `AIM_TABLE` loop with enough planks but no door/light inventory.
- `run-live-foundation-20260814c`: the table aim fix worked, but a closed
  shelter door repeatedly occluded the roof ray and the route stopped in a
  roof retry loop.
- `run-live-foundation-20260814d`: the door case was no longer dominant; the
  last outer roof corner remained marked as interior-deferred and repeatedly
  re-entered the same fallback cycle. This run used a process compiled before
  the final exterior-recovery patch.
- `run-live-stronghold-victory-debug7-20260814` and
  `run-live-stronghold-victory-diag-20260814`: strict floor-support admission
  correctly refused to walk from a first-person frame that saw stronghold
  faces but no observed floor. The diagnostic showed the fixture floor was one
  block below the visible sample; bounded inherited interior confidence now
  permits only a finite one-block corridor retrace.
- `run-live-stronghold-entry-debug8-20260814` through
  `run-live-stronghold-entry-debug10-20260814`: the production initial-anchor
  relogin placed the body outside the authored maze and the search selected a
  stale scan direction. The harness now places the human one block forward in
  the safe locator ring, and the search derives station yaw from actual travel.
- `run-live-stronghold-entry-debug11-20260814`: search passed, but a final-eye
  fallback stalled after eleven eyes. `debug12` then exposed stale
  `TARGET_OUT_OF_REACH` replay after nine eyes. The final-eye fallback was
  removed, stale targets are discarded, and semantic interaction candidates
  now fail closed at 4.45 blocks. `debug15` is the first passing run after all
  three fixes.

The isolated `20260814e` run is the first foundation run after both fixes and
is the only one counted as a passing controlled foundation result.

## What this proves

These runs close concrete regressions such as speech-only movement, passive
follow promises, staring at a nearby hostile, refusing to use an owned food
item, and losing the body during first-human anchoring. They prove that the
current implementation can carry a real model decision through the local
safety/skill lanes into observable vanilla actions in bounded fixtures. The
delayed-login result is a production lifecycle check, not a model claim.

The stronghold run specifically proves that portal activation is not treated
as a hidden state mutation: every eye is selected from a current first-person
ray, dispatched through the vanilla server-player actuator, and confirmed by
the inventory/world revision before the next eye. A semantic ray outside the
authoritative survival reach is rejected and causes a bounded station change
rather than an infinite speech-only retry.

They do not prove:

- arbitrary random-seed completion or a two-hour speedrun;
- Hardcore survival statistics;
- a rendered real-client Actor/Observer causal gate;
- the M0 24-hour soak or M3 100-hour world-memory gate;
- the M1, M2, or M4 hidden-seed sample sizes.

The formal statuses therefore remain `NOT_RUN` until an authorized Linux/Xvfb
worker executes the frozen production JAR with the required client and oracle
protocol.
