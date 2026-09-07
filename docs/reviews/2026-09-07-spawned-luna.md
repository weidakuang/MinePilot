# Spawned Luna static and dynamic navigation acceptance

Date: 2026-09-07. This is a separate test from the earlier Skill-owned persistent
Luna model sessions. Parent tool `multi_agent_v1.spawn_agent` created an actual
`gpt-5.6-luna` subagent, nickname Socrates, ID
`01a07bd6-9163-7ca2-9d1d-ab93ee539e46`, with low reasoning and no inherited context.
It read the Skill and used direct public CLI calls. No persistent companion host,
second model or automatic route selection participated.

## Setup and observation boundary

Real Minecraft Java 26.2 / Forge 65.0.9 dedicated server in
`run-luna-subagent-20260907`, loopback game 25570 / MCP 25770. Adventure mode,
empty initial inventory, flat terrain. Before the trial, the parent supplied
one physical dropped stack of three apples near `(15.5, -60, 0.5)` and sent one
Chinese game-chat request: first reach `(10.5, -60, 0.5)` within radius 0.5,
then find nearby apples and collect them. No later teleport, world edit,
inventory mutation, corrective coordinate or route suggestion was supplied.

Luna was instructed to use only public observations/tools and never read source,
world saves, logs, test internals or private data. It was responsible for goals
and route choices. The parent did not inspect its context, reasoning or tool
trace. Parent acceptance used only actual dimension/coordinates/inventory and
visible chat, sampled independently every approximately 0.2 seconds.
The bounded test ends after its objectives; it is not a persistent-play test.

## Results

Passed both objectives, in order:

- Start: overworld `(0.5, -60.0, 0.5)`, empty inventory and hotbar.
- First physical movement: 41.035 seconds after the request was sent, including
  subagent launch, Skill loading and direct tool round trips.
- First target reached: 43.141 seconds, at
  `(10.28379616994901, -60.0, 0.5000442752826388)`, approximately 0.2162 blocks
  from the target, still with an empty inventory.
- Apples acquired: 66.367 seconds, at
  `(14.48015447688037, -60.0, 0.5000356640419955)`, inventory changed to three apples.
- Luna's final compact report independently stated success, three apples and
  a later stopped coordinate approximately `(14.6997, -60, 0.5)`.
- Luna reported two navigation requests, one per stage, with one returned route
  in each stage. These route counts are operator-reported; the parent did not
  inspect private decisions to verify the counts. Physical results were
  independently verified and do not depend on that report.

Evidence: `physical-result.json`, `allowed-observations.jsonl`, `settled-final.json`
and visible `[AI]`/`[Server]` chat in that isolated run directory. The parent
inspected only those visible chat lines, not backend diagnostic logs.

## What this establishes and what it does not

A real spawned Luna subagent can use static then observed dynamic targets and
physically collect a normal-gravity item through the public interface, choosing
its own routes. This is a short flat-world success, not the supplied parkour,
3D navigation, door interaction, or an unconditional success-rate claim.
The 41-second launch-to-movement delay is poor interactive latency and includes
the cold subagent orchestration workflow; it should not be compared as a like-for-
like warm-model latency measurement. The earlier persistent-model pickup had
14.682-second first movement, which is also not an acceptable humanlike target.

The direct CLI lacked new gameplay-tool commands despite their availability to
the persistent host. This turn added a bounded `tool --name` entry point and
`dropped_item`/`waypoint` preparation, preserving the acknowledgement barrier,
argument validation and game-tool allowlist. Independent operators no longer
need to inspect client source or launch another model to use those features.
Python: 33 tests passed. Skill validation and whitespace checks passed.
The Java artifact was unchanged during the trial:
`e1701b591a0831a993043183cb6ff26f9d6c08d7a0fde30365d861402c41b4f5`.
The dedicated server was saved/stopped after final observations; original saves
and the installed XMCL JAR were not modified.
