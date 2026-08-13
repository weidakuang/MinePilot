/Users/weida/.zprofile:7: no such file or directory: /opt/homebrew/bin/brew
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

All runs used Forge `65.1.1`, Minecraft `26.2`, Java `25`, and the same source
revision before the `0.1.10` metadata-only version bump. Each test ran one
case and exited cleanly with `All 1 required tests passed`.

| Scenario | Model decision evidence | Vanilla-world evidence | Result |
| --- | --- | --- | --- |
| `real_player_task_to_live_model_movement` | `START_SKILL(travel_to)` | The body walked from the fixture start to the requested point; no teleport or command was used. | PASS (controlled live slice) |
| `real_player_task_to_live_model_follow` | `START_SKILL(follow_entity)` with an observed player target | The body survived the initial-anchor relogin and followed the moving course through ordinary player movement. | PASS (controlled live slice) |
| `real_player_chat_to_surprise_zombie_defense` | `START_SKILL(engage_observed_entity)` after local damage/hostile observations | The emergency lane reacted before model completion and the body killed the visible zombie with normal combat actions. | PASS (controlled live slice) |
| `real_player_chat_to_critical_golden_apple` | `START_SKILL(consume_owned_food)` for `golden_apple` | The body consumed the owned item through the ordinary use path; the scenario also verified the follow-up food-decision integrity. | PASS (controlled live slice) |
| `real_player_task_to_live_model_end_victory_and_return` | `START_SKILL(fight_ender_dragon)` followed by `find_and_enter_observed_portal` | The body destroyed the observed End crystal/dragon and entered the return portal with the same UUID. | PASS (controlled live slice) |

## What this proves

These runs close concrete regressions such as speech-only movement, passive
follow promises, staring at a nearby hostile, and refusing to use an owned
food item. They prove that the current implementation can carry a real model
decision through the local safety/skill lanes into observable vanilla actions
in bounded fixtures.

They do not prove:

- arbitrary random-seed completion or a two-hour speedrun;
- Hardcore survival statistics;
- a rendered real-client Actor/Observer causal gate;
- the M0 24-hour soak or M3 100-hour world-memory gate;
- the M1, M2, or M4 hidden-seed sample sizes.

The formal statuses therefore remain `NOT_RUN` until an authorized Linux/Xvfb
worker executes the frozen production JAR with the required client and oracle
protocol.
