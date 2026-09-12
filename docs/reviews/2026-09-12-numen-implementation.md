# MinePilot / Numen integration — 2026-09-12

Deployed to the original server on 2026-09-12 at 05:13 UTC. The server and
Luna Fast listener are running. The original saved position, health and all 12
carried item types were checked against the pre-update NBT and match exactly.
The delivery manifest records binary hashes, backup and process IDs. Some
experience checks remain qualified below; this is not a claim of flawless play.

## Product and migration scope

Minecraft Java 26.2, Forge 65.0.9, Java 25. Server-side mod and persistent external
Codex listener; vanilla protocol clients do not need a companion mod. Luna stays
`gpt-5.6-luna`, low reasoning, requested Fast (accepted backend tier `priority`).
No model/API replacement, client UI or streamed answer was introduced.

The 15 adapted Numen classes and exact upstream paths are listed in
[THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md), pinned to
[`34ef004dac3095fbbd928a897927e277c69d02fa`](https://github.com/Dwinovo/minecraft-numen/tree/34ef004dac3095fbbd928a897927e277c69d02fa).
They retain section scanning, ring searches/budgets, failure ladders, native menu
operations, build order, breathing recovery, event batching and memory compaction.
The native body, route planner/follower, break/place logic, inventory policies and
causal drop records remain MinePilot's. The LGPL code and exact modified sources
are embedded in the distributable JAR; original adapters remain Apache-2.0.

New public tools are wired to MCP, the internal model schema and the external
listener: `find_resources`, `gather`, `craft`, `interact_block`,
`inspect_container`, `transfer_items`, `close_container`, `smelt`, `eat`,
`build_camp`, `remember_context`, `companion_mode`, and corresponding status,
cancel and camp-resume tools.

## Implemented behavior

- Shared maximum/default perception is 150 blocks. Resources, entities and native
  structure records read loaded space; visible, obscured and unknown are distinct.
  Explicit sweeps turn through four headings. Actual interaction reach is vanilla.
  `/minepilot_mark` and chat `标记这里` share the human crosshair and bounded nearby
  context. A custom N binding is impossible with an unmodified client and is not
  claimed. Marker rays stop before unloaded chunks.
- One resource job scans, checks approaches, tries alternatives, mines and verifies
  pickups. Cobblestone acquisition targets natural stone. Up to eight small
  access steps / 24 observed natural blocks can clear bank or headroom barriers;
  native tool use, range, falling-block and entity checks remain enforced. Rejected
  tree components are excluded until their observed block/neighbour fingerprint
  changes, so a rootless remnant cannot trap the same task in an approach loop.
  A candidate also has a progress budget; genuine approach/mining progress renews
  it. A gathering leg that fails without moving returns to the recovery ladder
  rather than recapturing the same entrance. Other native tasks retain their
  existing precise-position retries.
- Native recipe/menu operations support inventory and workbench crafting, real
  container transfers and furnace input/fuel/output. Smelting includes a checked
  approach to the furnace and collects the output; cooking consumes normal ticks.
  A nearby workbench placement can prepare one bench from carried eligible logs
  through native recipes in the same call. Food uses normal timed consumption. No items are manufactured by these tools.
- Native swimming maintains surface flotation while waiting, seeks connected air
  openings when trapped and preserves breath recovery after player stop.
- The camp is a persistent 83-cell blueprint: 25 floor, 30 wall, 25 roof, crafting
  table, furnace and chest, with a two-block-high entrance. It gathers/crafts
  missing supplies, orders support/layers, verifies actual blocks and resumes only
  unfinished cells. Replacement wood may change the species of unbuilt cells.
  Actual nearby standing cells are checked for remaining wall/roof faces; the
  builder is no longer limited to a sparse list of viewpoints.
- Per-world/per-body memory stores recent dialogue, bounded older excerpts/model
  summary, the current goal, stations, events and the camp blueprint. Removed
  loaded stations are invalidated. Explicit stop pauses autonomy and cancels work.
  Normal chat can interrupt model thought while a native task continues.
- The listener keeps its Codex process warm, supplies bounded current state and
  recent actions, and uses a fresh compact decision thread per turn. Child results
  preserve the original goal, including a blocked fast command followed by travel; native child events do not trigger duplicate model
  work. Recoverable failures have a bounded repair path. Intermediate tool steps
  are quiet. Decisions/results and public timing are journalled without credentials.

## Performance defect found in the supplied natural terrain

A Java Flight Recorder profile identified `NavigationWorldSnapshot` construction,
not just model inference, as a dominant delay. `Map.copyOf` built a dense immutable
map with severe collisions from the old coordinate hash. It now owns a detached
unmodifiable HashMap and uses a spatial coordinate hash. This preserves the same
route algorithm and immutable snapshot contract.

Before: 2,179 of 3,861 sampled server-thread stacks included snapshot construction
(2,175 included `MapN` probing). After: 11 of 952 included snapshot construction.
The profiles cover different 70/60-second intervals and are diagnostic evidence,
not a calibrated speedup ratio. The corresponding natural-world resource test no
longer produced the earlier 2.4–4.8-second server backlog warnings.

## Verification evidence

All normal-world checks use the isolated `run-takeover-natural-20260910` copy at
25591/MCP25792. AI position, inventory, health and world tick speed are not granted
or rewritten during these checks. Native GameTests use explicitly constructed
fixtures and are reported separately from real-time play.

| Check | Result / evidence |
| --- | --- |
| Java unit tests | 42 passed; snapshot immutability, search boundaries, menus/memory and existing route/schema tests. |
| Python listener tests | 103 passed; fast placement/reclaim, stop ownership, goal retention, casual interruptions, bounded failure repair and quiet intermediate actions. |
| Natural stone acquisition | Four native stone breaks and four attributed cobblestone pickups after leaving water for a dry approach; health 20. `natural-gather-shore-repair.json`. |
| Natural wood acquisition, earlier access test | Earlier job: 22.375 seconds, 16.782 blocks of actual movement, three native access breaks, four log breaks and four verified carried logs; health 20. `natural-wood-final-access.json`. This continued from the earlier recorded access attempts, not an untouched original gully. |
| Natural wood after remnant exclusion | Four logs actually collected in 16.326 seconds, selecting another tree after rejecting unchanged remnants. `natural-wood-rejected-tree-repair.json`. |
| Natural wood after progress budget | From the actual reached camp approach, four new logs collected in 42.798 real seconds with health 20. No AI teleport, grant or tick change; world includes earlier access edits. `natural-wood-progress-watchdog.json`. |
| Full survival chain fixture | Seven supplied fixture logs → planks/workbench/pick → eight mined cobblestone → furnace/real charcoal → chest deposit/withdraw → timed bread. Furnace approach plus cooking: 251 native ticks, including at least 200 cooking ticks. `/tmp/numen-delivery-survival.log`. |
| Natural camp | Auto-gathered 19 further logs, native recipes and all 83 actual cells completed. The resumed construction took 51.094 real seconds after correcting own-body tall-grass occlusion. No AI material grants, teleport or tick acceleration. `natural-camp-acceptance.json` and `natural-camp-after-grass-repair.json`. |
| Process restart | Same completed camp ID, identical recent dialogue, workstation memory and inventory after a full server stop/start. `camp-process-restart-before.json` / `camp-process-restart-after.json`. |
| Bank/ore fixture | Four-high natural bank, six native access breaks, four verified coal pickups; partial local tranches continue without excluding neighboring ore. `/tmp/numen-progress-water-gate.log`. The accelerated GameTest yields briefly during asynchronous planning; this is fixture evidence, not a wall-clock latency benchmark. |
| Camp fixture | Public entry, interruption, persisted blueprint reload and resume with replacement materials; all 83 cells verified in 921 and 934 ticks in two consecutive final runs. `/tmp/numen-camp-work-area-gate.log` and `/tmp/numen-camp-work-area-repeat-gate.log`. |
| Water fixture | 149-block result included, 151 excluded; native reach unchanged; deep flotation, 120-tick surface hold, ceiling escape after stop and deep-water traversal/bank exit passed. `/tmp/numen-water-final.log`. |
| Action fixture | One supplied log crafted into a bench, own-body tall grass cleared through native breaks before placement in seven ticks; target grass workbench, reasonable break tool, overhead mining, actual pickup, immediate nearby placement, straight travel, water, emerald attention, view turns and yielding passed. `/tmp/numen-final-prerequisite-play.log`. |
| Structures/collection fixtures | Existing final structure and collection gates passed; old and failed logs retained. |
| Ten-minute protocol scenario | Second 610-second vanilla-wire session completed real cobblestone/furnace/charcoal and tool preparation, identified three gifted emeralds, remembered riverside preference. Stop measured 6.07 and 6.11 ms from wire chat send to cancelled native work. It also exposed repeated progress chatter, furnace approach loops and blocked wood search; subsequent fixes are not retroactively counted as passing that session. |

The protocol probe is not a rendered Minecraft desktop client. It uses creative
mode only for its artificial test player because it has no client physics; the
AI remains survival. It moves away from the work area before reclaim tests so it
cannot accidentally collect the AI's drops. Earlier failed sessions and repair
attempts remain available; their failures are not hidden by later successes.

## Final experience results and limits

The third 610-second session verified native charcoal cooking/collection, stops,
and riverside dialogue memory. It also exposed a wrong item reference, a lost
blocked-reclaim request, repeated residual-tree approaches and standby chatter.
Those observations drove the subsequent repairs; that session is not marked as
an all-pass result.

The final 300-second targeted session correctly named the current three emeralds,
recovered a distant workbench after a blocked local search and actual navigation,
placed a nearby bench, reclaimed it, and recalled the riverside preference. Its
stops were verified in 8.47 and 11.21 ms. One-second polling observed placement
completion within 1.357 seconds and reclaim completion within 2.802 seconds.
The later wood request still made no pickup before its 105-second interruption;
this prompted the final gathering progress limit. A subsequent native trip then
collected four logs in 42.798 seconds. These are separate trials, not one clean
combined ten-minute session on the final binary.

Thirteen completed Luna Fast decisions in the targeted run took 7.569–13.639
seconds, median 9.265 seconds (including small setup overhead). The held-item
question's complete visible response was about 13.9 seconds after the player
sent it. `firstTextMs` measures the beginning of internal structured JSON, not a
visible first sentence. Fast is accepted as `priority`; no streaming or empty
acknowledgement is used to present inference as finished.

Remaining qualifications:

- A complete, uninterrupted ten-minute mixed scenario on the final binary has
  **not** passed as a single acceptance run. The preserved sessions and targeted
  repairs show which behaviors were actually observed.
- Narrow one-block camp entrances can still require navigation replanning.
  A manual return from the outer bank stopped twice at entrance geometry; it was
  recorded as incomplete, not arrival. The final camp builder passes two native
  fixtures with its expanded work-position search, but arbitrary player-requested
  routes are not guaranteed to be shortest or always reachable.
- A rendered vanilla desktop client and a full natural village expedition were
  not exercised in this run. Vanilla protocol login/chat/markers and loaded-world
  structure fixtures passed. An unmodified client cannot expose a new N binding;
  use `/minepilot_mark` or chat `标记这里`.
- Whole-camp process restart was checked after completion; mid-construction
  persistence/resume was checked in native fixtures, and natural preparation
  resumed across a server restart. These are distinct proofs.

## Deployment and rollback

Delivery: `deliveries/2026-09-12-numen/` contains the tested JAR, exact source
archive, compressed public evidence, latency/deployment manifests and checksums.
The installed binary SHA-256 is:

`b652843400db331f06fff12fde2410faf46265a3f3b04b9bdabe92c60033ade6`

Production is `run-peaceful-play-20260908-234254`, game `127.0.0.1:25565`, MCP
`127.0.0.1:25766`. Java server PID 85405 and listener PID 85533 were verified;
process IDs are transient. The test copy/listener are stopped. Production terrain,
player data, OP/configuration and initial carried stock were preserved; no test
world or test-agent state was copied into it.

The complete pre-update server archive is in
`.minepilot-backup/production-numen-20260912-135821/`, with a SHA-256 manifest and
matching previous listener source. Follow the delivery's `ROLLBACK.md`; restore
to an empty sibling directory and preserve the post-update server first. The
backup includes private local connection configuration and should stay private.

The original 180-minute wall-clock estimate expired across the overnight pause.
This report does not claim that deadline was met.
