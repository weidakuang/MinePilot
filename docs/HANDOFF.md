## Acquisition notifications — 2026-09-12 22:51 JST

Deployed JAR `fc1dfa69a62be4a3b881c02bfa0a4e1f1d4f7826cd1aa6e08f031c1d473db125`; backup `.minepilot-backup/pickup-notification-1789220842/production/`. Pickup/other acquired inventory units are summarized from authoritative receipts in `pickup_notifications.py`. Each item has an acquired quantity and counted sources: latest emitter/dropper, native category, production origin, block/entity type and known cause. Mixed/unknown parts remain separate, without proximity-based giver guesses. LivingDropsEvent now records the deceased emitter separately from killer and damage cause; native last-transfer lineage is retained and self-loot correctly identified.

The listener's next available model turn decides only speech or silence, including while gathering/crafting jobs own the body. Receipts are batched on the 0.2-second poll; a running inference or direct chat can add delay. Chat interruption requeues unspoken receipts; deduplication prevents repeats. The speech-only decision cannot drop/organize items or cancel movement. Idle inventory review remains separate. Native SSE stays private; chat is complete messages only.

127 Python tests pass, including mixed counted provenance, unknown remainder, latest dropper, killer vs deceased, notification during active work, silence, interruption and restricted actions. Native respawn/death-drop gate and build pass (43 Java tests). A synthetic DeepSeek test chose Chinese thanks for Alice's three emeralds in 1476 ms, and silence for routine own-mined cobblestone in 820 ms. This model probe did not inject test items into production. Source event timing is actual receipt timing, not a guarantee of instant remote inference.

## Automatic native respawn — 2026-09-12 22:43 JST

Deployed JAR SHA-256 `528d3428cc2db505514ee04a3a73bf10db21c55701db72832d904d17b5b427cd`. Backup `.minepilot-backup/respawn-1789220560/` contains the stopped world, old JAR/config/profile. Production server and DeepSeek listener are running.

HeadlessPlayerSession now sends vanilla PERFORM_RESPAWN after 20 server ticks dead. A scoped constructor mixin preserves the companion player subclass while PlayerList performs normal spawn/restoration and Forge hooks. Hardcore remains native spectator behavior. Cached body controllers are rebuilt, old goals paused, chat/memory cursors retained. Native last-death dimension/position persists in player data and is exposed through observe/poll_events.lifecycle. The listener drops old work, receives one respawn event and does not invent restored loot.

Physical regression: two consecutive native deaths, same UUID, connection rebound, forced native spawn honored, normal movement after respawn, three emerald death drops with empty respawn inventory, old goal paused and death coordinates verified. 43 Java and 120 Python tests pass. Production previously dead companion respawned at (-7.5,71,7.5), health 20. Death point is minecraft:overworld (-424,68,182); the DeepSeek respawn event produced a complete in-game acknowledgement containing this location. No claim that old death drops have been recovered. Tests do not cover every bed/anchor/hardcore configuration; those use vanilla semantics.

## Event and drop repair — 2026-09-12 22:35 JST

Python listener repaired and restarted without restarting the world. Drop adapters generate/reuse idempotency keys, convert flat item/count into items, and attach the originating player instruction. Native drop tools are resident. Cancelled/replaced tasks are fenced; post-action scheduling re-polls instead of overwriting markers with the old snapshot. Stale job decisions are rejected and completion speech is deduplicated by originating request. Extra provider calls are serialized by executing only the first and re-deciding from its real result. 119 Python tests and native item-drop regression pass. Backup: `.minepilot-backup/event-repair-1789219893/`.

Production MinePilot was killed by a zombie at 22:30:52, before listener restart. The listener now reconnects to the dead body and suppresses background body-action retries; this repair does not implement or claim respawning. The world remains running. Do not claim physical delivery of the player's sword: it was dropped on death, not handed over by this repair.

## Latest runtime checkpoint — 2026-09-12 17:43 JST

Production is backed up and running DeepSeek Flash with private model streaming and complete vanilla chat. See [runtime repair and acceptance](DEEPSEEK_RUNTIME_REPAIR_20260912.md). Server 25565/MCP 25766 and one persistent listener are running; isolated test servers/listeners are stopped. This provider supersedes earlier Luna/o-c1 entries. Production JAR SHA-256 is `8248033976d3f3b775b964f8d36f69dec9aeb1d3422101456351c57fd0680982`. Recovery backup: `.minepilot-backup/deepseek-deploy-20260912-1740/`. No commit or push was performed during this repair.

# Continuous following and completeness audit — 2026-09-12

Production was backed up and updated with the continuous-follow correction.
Game/MCP remain 25565/25766 in `run-peaceful-play-20260908-234254`. Verified server
PID93138 and restarted Luna Fast listener PID93259; recheck transient PIDs.
Installed JAR SHA-256:
`bf469f5910a69b5cbe4d38ac13c78e1297eeb50a745711a3900557dca2f4309e`.
Delivery/source/evidence: `deliveries/2026-09-12-follow/`.

The exact final JAR was subsequently verified on installed Forge 65.0.9 using
only vanilla Java 26.2 protocol packets: login, enter-play and `/minepilot_mark`
passed without a Forge handshake or client mod. Evidence:
`docs/reviews/2026-09-12-final-vanilla-client.json`. This is protocol verification,
not a rendered desktop-client playthrough. Server-only installation/join steps
are now explicit in README. No compatibility code or production JAR needed changing.

Follow alone uses a resident live corridor, actual target displacement, analog
spacing control and walking/sprinting hysteresis. Our planner and ordinary
routes remain in place. A pre-existing reverse-brake input leak found in both
old/new baselines was fixed by consuming that frame once in the native body.
42 Java tests, 103 Python tests, all 14 ordinary navigation scenarios and native
follow gates at WALK/AUTO passed. Old slow follow stopped 15/72 measured ticks;
the corrected WALK/AUTO runs stopped 0/72 and the turning leg stopped 0/75.
Resume took 5–8 game ticks; these accelerated fixtures are not wall-clock/model
latency promises. Installed Forge 65.0.9 vanilla-wire login/mark also passed.

The full Numen plan is **not complete**: persistent failed-location memory and
automatic semantic conversation compression are still missing/partial; full
exploration/village and a clean final ten-minute all-feature scenario are not
verified. See `docs/reviews/2026-09-12-numen-completeness-audit.md`. All 15 copied
classes + three adapters and 19 new tool routes were checked, not just counted.

Backup `.minepilot-backup/production-follow-20260912-145150/` contains the stopped
server and matching listener. Production position, dimension, health and all
inventory entries matched before/after restart. Test server/listener are stopped.
Do not run GameTests on production MCP25766; the isolated audit run uses25793.
Do not issue AI mutations while the listener owns its controls. No commit/push
was requested. The previous entries below describe historical versions.

---

# Numen server-side delivery — 2026-09-12 (previous version)

Current production: `run-peaceful-play-20260908-234254`, 127.0.0.1:25565,
MCP25766. Server PID85405 and Luna Fast listener PID85533 were verified running.
Model: gpt-5.6-luna, low, requested fast, accepted priority, warm connection ready.
Recheck transient PIDs before process actions. Test copy/listener are stopped.

Installed JAR SHA-256: `b652843400db331f06fff12fde2410faf46265a3f3b04b9bdabe92c60033ade6`.
150-block loaded-world sensing, native gathering/crafting/menus/smelting/eating,
water recovery, persistent conversation/workstations and small-camp jobs are
wired to both model controllers and MCP. Source, license and public evidence are
in `deliveries/2026-09-12-numen/`; full review is
`docs/reviews/2026-09-12-numen-implementation.md`.

42 Java and 103 Python tests pass. Final camp fixtures pass twice (921/934 ticks).
Natural copied-world proofs include four stone/log pickups, real 83-cell camp,
restart, actual charcoal and model conversation. Remaining limits: no single
clean final ten-minute all-feature session, narrow entrance navigation can still
need replanning, and no rendered-client/natural-village expedition proof. Do not
turn those limitations into claimed successes. All failed sessions are preserved.

Original position, health20 and all12 carried item types matched saved NBT after
production startup. Backup `.minepilot-backup/production-numen-20260912-135821/`
contains full server and previous listener sources; follow delivery ROLLBACK.md.
Do not copy test agent/world state to production. Do not run GameTests on MCP25766
while production uses it. Do not issue manual AI mutations while the listener owns
control. User authorized implementation, backup and original-server deployment;
no commit, push or additional Codex task was requested.

The historical notes below describe older binaries/configurations and are
superseded by this checkpoint.

---

# Luna Fast configuration — 2026-09-10

The user explicitly requested minimum reasoning and Fast. The active companion
uses gpt-5.6-luna with low (its supported minimum) reasoning and requested Fast.
The local Codex transport confirms serviceTier=priority; a real structured JSON
probe completed successfully in 5.733 seconds including transport setup. This is
not an end-to-end gameplay latency guarantee. 82 Python tests pass.

The user server remains at 127.0.0.1:25565; only its companion listener restarted.
Listener PID13316 is LISTENING and modelConnectionReady=true. Recheck transient
IDs. The non-secret per-world profile serviceTier=fast persists this preference;
the global Codex configuration was not changed. The CLI also accepts
--service-tier fast/default and rejects reusing a listener with mismatched
settings. Status exposes requested/accepted tier and reasoning effort.
See docs/reviews/2026-09-10-luna-fast.json. JAR and Minecraft server were unchanged.

---

# Tree-search repair and physical drops — 2026-09-10

The screenshot showed real trees while the model reported no eligible trees.
Verified interface defects were generic/Chinese tree filters matched literally
against block IDs, and block search returned half-block centers whereas tree
inspection required integer block coordinates. Search now accepts generic tree
aliases and supported species names; sensed block x/y/z are exact integers with
a separate movement `center`. A wrong leaf/ground seed reports its actual block
and nearby observed trunk candidates. Rejected tree plans retain bounded concrete
reasons instead of implying no trees exist. This does not establish the exact
private model arguments that caused the screenshot.

A source-informed read-only test of the original world's stopped-backup copy at
(-71.35, 70, 10.3) searched Chinese `trees/树` across eight pages in 0.42 seconds,
returning 39 trunk cells. Five connected candidates included two harvestable oak
candidates (7 and 6 logs) and three explicitly incomplete observations. No tree
was changed by that read-only check. An independent gpt-5.6-luna subagent then chose the six-log oak using only the
public Skill, felled all six logs, collected six oak logs and dropped two through
`drop_items`. Parent verification found all six original cells air, four carried
oak logs and a real two-log item entity with owner pickup avoidance. The operator
reported 1734 native game ticks (86.7 game seconds), not a latency SLA. It received
no target coordinates or route from the parent. This accepts one natural oak,
not every species or large tree farm. The temporary copy server was stopped;
the user server and persistent Luna remain available.

`drop_items` / `reclaim_drop` are implemented and exposed through both controllers,
including inventory events and active jobs. Native drops conserve real items,
restore cancelled tosses, reject stale/ambiguous/excess selections, protect
importance 0..2 from autonomous cleanup, and respect exact held tools and remaining
job materials. Explicit player requests can hand over protected items without
another approval. Bounded persistent request receipts prevent uncertain retries
from tossing twice. Owner avoidance follows native merges; other players can
pick up normally; reclaim releases the whole sensed mixed stack. Throws use the
current facing. Read `docs/ITEM_DROP_DESIGN.md` for the implemented boundary.

Seven selected real Forge server gates passed (drop, knowledge, collection,
mining, placement, excavation, provenance), plus 33 JUnit and 80 Python tests;
Skill validation and build passed. Sensor fixtures now consume asynchronous
pages across ticks and use integer coordinates. The storage-failure fixture
reloads its recovered ledger before the independent tree-recognition case.
See `docs/reviews/2026-09-10-tree-drop-repair.json` for evidence and limitations.

Production user world remains at **127.0.0.1:25565**, MCP25766, peaceful/offline
with OP retained. Detached server PID12420; persistent Luna PID12446 LISTENING,
modelConnectionReady=true. Use `run-peaceful-play-20260908-234254/profile.json`.
Recheck transient IDs. Coordinates, dimension, inventory, hotbar and selected
slot exactly matched the pre-update public observation. Full world/JAR backups
are in that server's `.minepilot-backup/tree-drop-20260910/` and client mods backup.

Repository, user server and XMCL JAR SHA-256 all match:
`58d172e565666910f182d7aa1cc84e7b4a1ca5085e9ae5b68d79bd05742be56e`.
Client restart is required; fresh client entry is not claimed. Server Forge65.0.9
versus last-known XMCL configured65.0.8 remains visible. Existing expansion work
and these repairs remain uncommitted on `codex/mining-expansion-20260909`; do not
reset, restore or overwrite them. The prior GitHub backup remains70b24f6.

---

# Active recovery — 2026-09-09 23:23 local

The user actually joined the original `127.0.0.1:25565` server and then reported
a disconnect. Both terminal-managed Java processes were absent, without native
crash/shutdown records. Exact exit cause remains unproven. The original
`run-peaceful-play-20260908-234254/peaceful-world` was archived while stopped;
its JAR was updated to `2bdf5f3158068fcc40dfc96d9572db60c28a6ef796d6eed8e52512009156d9bc`
matching XMCL. It is now launched detached via `scripts/local-server.py`.
Server PID 10294 (parent 1, no terminal), MCP 25766; persistent Luna PID 10328,
LISTENING with model connection ready. Recheck transient IDs. Offline auth,
peaceful survival and OP are preserved. Native status and public observation
verify server recovery and the original two logs, wooden pickaxe and three leaf
litter. Use this server/profile for the user, not the stopped 25581 test world.
See `docs/reviews/2026-09-09-server-recovery.json` and `docs/ITEM_DROP_DESIGN.md`.
Dropping is a proposal only: implement after the requested design step. Startup
no longer falsely reports failure merely because a live listener is warming.
77 Python tests pass. Expansion changes remain uncommitted; preserve them.

---

# Active player test server — 2026-09-09 23:07 local

The user interrupted expansion verification to test personally. Use
`run-expansion-packaged-20260909/profile.json` for the new peaceful survival
server at `127.0.0.1:25581` (MCP 25782), **not** the preserved old 25565 server.
Luna is persistently LISTENING with a warmed model connection. XMCL received the
same expansion JAR as this server; restart the client. Client entry is unverified.
See `docs/reviews/2026-09-09-expansion-play-server.json` for exact hashes, process
hints, OP and the physically observed inventory. Recheck transient process IDs.

GitHub pre-expansion backup is `codex/backup-rebuild-20260909` at `70b24f6`.
Current branch `codex/mining-expansion-20260909` contains uncommitted expansion:
local access/resource/region/tunnel/optional-fishbone/tree jobs, native collection
and replant, cave survey, counted carried/container/merge/pickup provenance,
native level-event sounds and sensor pagination. Preserve all changes. The
expanded JAR build and ten Forge physical gate groups passed; 77 Python tests
passed. An independent Luna tree operator was interrupted before its final report.
Interim observation showed six logs; handoff showed eight. It is NOT accepted
as independently verified complete felling/replant. Source-informed eight-log tree
and cave fixtures did pass. Finish review/documentation and another backup after
user testing. Unknown custom transfers, large trees, automated farm operation,
complete natural-world performance and general survival/parkour remain bounded
or unaccepted. The development artifact is not a professional release.

---

# Latest addition: native structure records within 96 blocks — 2026-09-09

`sense kind=structures` now queries native server structure starts/references,
not bell/marker candidates. Default/max radius 96, measured as a fixed 3D sphere
to the nearest individual recorded piece volume. Results identify their server
source and do not prove current blocks, an entrance or a safe standing point.
Registered IDs, #tags, village/村庄 and other Chinese aliases are supported.
Queries are bounded, continued with a cursor, cached briefly and do not expose
out-of-radius centers/bounds. Native metadata dependencies may be loaded outside
the sphere. Other visual/block/entity queries remain unchanged. Player-built
houses, farms and portals are not generation records.

Read `docs/reviews/2026-09-09-structure-records.md`. Seven native physical fixture
queries passed, including 95/96/97-block limits, an external referenced start,
vertical exclusion and no bell false positive. A naturally generated seed-0
village matched vanilla /locate; its bounded query took 49 ms processing /
114.767 ms including polling, with unchanged position/inventory. A village 991
blocks from spawn was excluded there. Luna's public Chinese report matched the
independent native result and did not move the body. 29 JUnit, 70 Python and the
knowledge/support physical gate passed. These samples do not guarantee all
queries or model responses within a fixed time.

Latest installed JAR SHA-256:
`ca0bac76b99f52b6acc749e3e05fbd2cc8e7efdd5182070c0b645b9a11ce1b73`.
The repository, original user server and XMCL copies match. The same peaceful
survival world remains at `127.0.0.1:25565`, offline auth and OP retained. Server
PID 94800, console session 20475; Luna listener PID 94828. Verify transient IDs
before reuse. Use `run-peaceful-play-20260908-234254/profile.json`. Coordinates
and inventory survived the controlled restart exactly. Complete world and JAR
backups were kept; the test server/listener were stopped. Client restart is
required, and client entry remains unverified. Server Forge 65.0.9 versus the
last-known XMCL configured 65.0.8 discrepancy remains documented. No commit/push.

See `docs/reviews/2026-09-09-structure-installation.json` for live verification.
Older structure-marker-only statements below are superseded by this bounded,
explicitly privileged capability; other previous acceptance limits still apply.

---

# Historical repair and running user server — 2026-09-09 01:27 local

Read `docs/reviews/2026-09-09-movement-collection-repair.md` first. Walking gaze,
intermediate-cell braking, persistent dynamic follow, compatible automatic
repair selection, forest planning budget, palette-filtered search, inventory
mining tool choice, whole-tree completion and native player knockback were
repaired. The persistent Luna controller now keeps the original collection goal
through approach navigation instead of stopping at that subtask's destination.
Normal travel prefers AUTO pace. Chunk/world reads remain on the server thread
with bounded capture work; path search runs on a worker, without CPU affinity.

29 JUnit and 69 Python tests passed. Fourteen physical navigation scenarios and
mining/collection/placement/knowledge/sound gates passed. Final follow resumed in
24 ticks after a stationary target moved, with the same request; native attack
displaced the body 1.932713 blocks. Independent Luna whole-spruce trial: first
reply 7.117 s, physical completion 37.398 s, six logs received and six axe damage.
The earlier failed Luna approach is retained. Do not claim general 3D/parkour,
universal tree recognition or a fixed latency guarantee.

Restart revealed and fixed a pre-existing missing native player-data load during
embedded login. The new persistence gate proves actual save/logout/rejoin plus
inventory, tool damage, offhand, mode, position, dimension and rotation. The
user server's original Agent data was restored from the pre-restart world
archive; only Agent data files were restored, not the whole world. Public
observation verified zero position error and unchanged original inventory.

Current production server: `run-peaceful-play-20260908-234254/`, same peaceful
survival world, `127.0.0.1:25565`, offline auth and OP weidakuang retained.
Server PID 93475, console session 81882; Luna listener PID 93640, LISTENING and
model connection ready. Verify these transient IDs before reuse. Use this run's
explicit `profile.json` with MCP `127.0.0.1:25766/mcp`, not the default XMCL
integrated-world profile. Leave server and listener available for user play.

An idle listener originally exited on an uncaught RemoteDisconnected from the
macOS local proxy. The loopback client now bypasses system/environment proxies
and treats interrupted responses as connection failures; it never replays an
uncertain mutation automatically. The proxy/closed-response HTTP regression
passes. System proxy settings were not modified.
The restarted listener also passed 27 live observations across 130.25 seconds
without exiting; the server continued at approximately 20 ticks/second.

Installed repository/server/XMCL JAR SHA-256:
`3aaa4d2d53ffbb0b35653cbcf4798b3564fe5bb2cda09e64ca1d76242418b228`.
Client restart is required; fresh client entry has not been verified. Production
server Forge is 65.0.9; the last-known client configured Forge is 65.0.8. Preserve
the world/JAR backups, dirty working tree and previous work. No commit or push.
See `docs/reviews/2026-09-09-repair-installation.json` for restoration evidence.

---

# Historical user play server launch — 2026-09-08 23:43 local

The user requested a fresh default peaceful world for hands-on testing. The real
production Forge 65.0.9 server is running at `127.0.0.1:25565`, with natural world
generation, survival mode, peaceful difficulty, offline authentication (retained
from the user's earlier request), and OP retained for `weidakuang`.

Server directory: `run-peaceful-play-20260908-234254/`. Native server PID was 89774
and the console tool session was 42905 at launch. The persistent Luna Skill
listener PID was 89800, using this directory's `profile.json` and MCP at
`127.0.0.1:25766/mcp`. Verify liveness before reusing IDs. This is an ongoing user
play session; leave server and listener running until the user asks to stop.

The server loads the production JAR, and its SHA-256 matches the repository and
XMCL client copies: `401aea6801e98937413483c248099951f2902fceb922df1fb1b132dff5025a77`.
The client was already synchronized. No code changed; a redundant rebuild was
cancelled during dependency refresh and the previously accepted artifact was used.
The user's normal Skill profile was preserved; use the explicit active-server
profile above. Current Minecraft client entry is not claimed from server readiness.
Earlier test-server shutdown records below are historical and refer to other runs.

---

# Latest checkpoint: placement foundation — 2026-09-08

Placement is now implemented and physically tested; earlier design-only notes
below are historical. Read `docs/reviews/2026-09-08-placement.md` and
`skills/minepilot-companion/references/placement.md` for the current contract.

- Native five-block selection with vanilla reach/occlusion, oriented placement,
  door/bed footprints, underfoot jumping and real inventory/main/offhand swaps.
- Interruptible 256-action batches via explicit cells, inclusive regions or
  fixed-origin text layers; reserved air is not automatic excavation permission.
- Bounded safe working-position movement, concrete obstacle decisions, selected
  inventory mining-tool cost, normal temporary cleanup and importance protection.
- Shared MCP/internal schemas and persistent Skill events; chat remains usable.
- 18 source-informed physical case groups, 29 JUnit tests, 65 Python tests and
  Skill validation passed. Knowledge/support, mining, collection and final
  navigation repair gates passed. One earlier half-slab search-budget timeout is
  retained in the review; do not turn later passes into a 100% reliability claim.
- Final-build Luna normal-chat trial: first visible reply 10.76 s, three real
  cobblestone cells plus inventory 8 -> 5 at 15.45 s, completion chat 18.79 s;
  subsequent chat answered in 3.60 s. No private model output was inspected.
- XMCL JAR atomically updated with backup, SHA-256
  `401aea6801e98937413483c248099951f2902fceb922df1fb1b132dff5025a77`.
  Test listener/server stopped and token removed. Real-client world entry remains
  unverified; backend Forge was 65.0.9, last-known XMCL UI selection was 65.0.8.

Whole-house recognition/design, renovations, stripping and rail networks remain
unaccepted. Preserve the dirty working tree and all backups. No new commit/push.
Continue with actual client acceptance and bounded construction scenarios; do
not infer the full placement/renovation draft from this foundation.

---

# Placement design checkpoint — 2026-09-08

The next proposed capability is specified in `docs/PLACEMENT_BUILDING_DRAFT.md`:
native placement, general hand/offhand selection and capacity, interruptible
batches, obstacle decisions and declarative text blueprints. The sample layout
and static-only checks are in `docs/examples/placement-house.*`. No placement
production implementation, Skill tool exposure, physical gate or JAR change was
made for this design review. Existing latency/collection acceptance below stands.

The code review found that the existing navigation support path needs a shared
placement primitive with real aim/reach checks and actual state/count verification;
`consumesAction()` alone can mean block interaction rather than placement. Preserve
the support importance 0..2 prohibition. Five-block target selection must not
extend native interaction reach. Doors/beds use one item for multiple cells;
reserved blueprint air must not silently authorize excavation. Read the draft
before implementation; the user endorsed the broad direction, while detailed
defaults and physical acceptance remain pending.

The user subsequently added renovation scenarios: second-storey additions,
cherry-wood frame infill, height-anchored cut/fill and cobblestone floor replacement,
in-place log stripping, model-designed huts and home-to-village railways. Section
11 of the placement draft adds bounded reference grounding, observed-to-desired
state differences, native tool transformations, preservation constraints and
rail surveying/functional acceptance. These are design requirements, not newly
implemented tools. No production/Skill/JAR or world change was made for this update.

---

# Latest checkpoint: persistent companion latency — 2026-09-08

The installed Skill/controller now delivers model-authored tool messages, prepares
its model connection without inference, uses compact model-approved collection
choices, avoids redundant post-start model turns, and isolates historical job
success from new requests. Listener shutdown cancels collection/mining/navigation.

Real normal-tick Forge 65.0.9, persistent Luna, survival, empty inventory and bare
hands: final three-birch-log trials finished in 25.847 s and 22.326 s, with first
visible replies at 7.695 s and 5.193 s. The second reused the same controller.
A preceding 30.897 s miss and a false-completion/empty-inventory failure are kept
in the evidence. These samples do not guarantee every request within 30 seconds.
60 Python tests and Skill validation passed. No Java or JAR change in this step;
the installed Skill symlink receives the update on listener startup. Test server
and listener stopped after the run. Details: `docs/reviews/2026-09-08-companion-latency.md`.

---

# MinePilot Project Handoff

## 2026-09-08 continuous collection checkpoint

The local working tree now includes bounded wood/ore collection on top of the
single-block foundation. See [the collection review](reviews/2026-09-08-collection.md)
for exact acceptance and limitations. `plan_collection`/`choose_collection`
automate normal approach, repeated breaks and pickup while chat remains available.
Sources distinguish loose logs and mature trees; explicit tree/farm policies are
preserved. `inspect_tree` and world-scoped `tree_farm` declarations expose
classification evidence and uncertainty. Fishbone mining is optional and its
executor is still unimplemented.

The public Forge gate covers actual bare-hand/axe gathering, branches, loose
logs, pause/chat/resume/cancel, fixed-radius partial ore results and disappearing
drops. The first independent Luna trial FAILED after one log because pickup
populated the selected empty hand slot; that path has been fixed and is covered
by a new empty-inventory real-server regression. A fresh independent Luna retest acquired three birch logs in 53.33 seconds from
the public request; cold-task latency remains slow. Evidence is recorded in the review. Do not erase the failed first trial.

The installed JAR now has SHA-256 `8fa48ec45bac6f7febfe2985a314a8123ab744f75bf2b7fa9db1d8d21f6cb04b`; see `docs/reviews/2026-09-08-collection-installation.json`. Both independent servers/observers and Luna operators are stopped. Real client entry remains unverified.

These changes remain local and uncommitted on `codex/backup-rebuild-20260908`.
The previously uploaded backup commit is unchanged. Original parkour, broad
natural-world navigation, access excavation, container sources, replanting,
automated farm operation, fishbone/cave execution and live internal-provider
latency remain open. Do not infer complete mining or every tree shape from these
bounded results.


Last updated: 2026-09-08

## First mining implementation checkpoint

See `docs/reviews/2026-09-08-mining-foundation.md`. Implemented the 10-block
non-air sphere, wear-stable policy/count identity, per-slot durability,
hotbar-tool selection and one reachable-block mining plan/choice/status with
pause/resume/cancel. Actual player actions respect native progress and server
denials. Shared public/internal schemas and the Skill expose this limited scope.
Mining-origin pickup events do not claim full merged provenance.

Real Forge gates proved coal mining/wear/pickup, pause/cancel persistence, sphere
boundaries and server-denial/approval guards. One independent spawned Luna trial
acquired coal x1 with wooden-pickaxe damage 1; request-to-pickup was 37.387 seconds,
and its result chat took 48.601 seconds. This does not solve latency. Later final
guards have source-informed evidence only. Tests: 26 JUnit and 50 Python.

The broader design is in `docs/MINING_COLLECTION_DRAFT.md`. Interaction-position
navigation, access excavation, fixed-radius/cuboid automation, tool alternatives,
mining supports, cave exploration and fishbone mining remain unimplemented.
The original parkour and broader responsiveness problems remain open.
The final JAR is installed in XMCL with a preserved previous-JAR backup; see
`docs/reviews/2026-09-08-mining-installation.json`. Client world entry is unverified.
All temporary test servers/operators/observers were stopped.
The prior GitHub backup is `81dfb6b` on `codex/backup-rebuild-20260908`; current
mining work is local and uncommitted. Preserve all earlier reports/failures.

## Latest movement/latency checkpoint

See `docs/reviews/2026-09-07-movement-continuation.md` and the installation JSON.
Continuous follow/replacement and thirteen controlled navigation scenarios passed,
including fractional slab surfaces. New chat cancels obsolete model decisions;
stop no longer replies twice. Seven knowledge tools share schemas/execution across
internal and external controllers. 25 JUnit/46 Python tests passed, with real
follow/knowledge gates and a synthetic loopback provider-interface test.

Two new independent Luna runs FAILED the supplied parkour, ending near
(92.45,-59,-36.47), with empty inventories. Do not report it passed. Ordinary
language remains slow: the final replacement reply took 6.41 seconds and stop
4.66 seconds, with about 5.6 blocks of movement during that stop delay.
Full provenance, exploration/global navigation, complete structure recognition,
natural-world performance, real internal provider and client/world acceptance
remain outstanding. The internal provider/model configuration question is pending;
no credential was placed in source or evidence.

The new development JAR is now installed in XMCL with original/intermediate backups.
Final SHA-256: `910e6a84cbdc82e66433d89a3f180727e962661d76a9b2d3709d5c66bd05ca17`.
Backend Forge was 65.0.9; the launcher version discrepancy remains unverified.
All temporary test servers/controllers were saved/stopped. Preserve the original
world and the stopped user-server/OP configuration; no GitHub work was performed.

## Earlier sound-tool checkpoint

The new read-only `listen` tool exposes native Chinese sound subtitles, eight
relative directions, ear-distance in blocks, native player/entity/block labels
and explicit source confidence. It reads sounds actually delivered to the body;
range follows each selected vanilla 26.2 sound resource and volume. Default
caption lifetime is three seconds, with bounded storage and paginated results.
MCP, direct Skill CLI, persistent model host and internal model schemas expose it.
It does not automatically speak, move or interrupt navigation.

See `docs/reviews/2026-09-07-sound-perception.md` for the full contract, source
references and evidence. Client-only/level-event-generated sounds, resource packs
and full client subtitle parity remain unsupported. Positional source matches
are candidates, not proven emitters. There was no Luna/internal-provider sound
conversation run in this update.

Final verification: 23 JUnit and 40 Python tests, Skill validation, one real Forge
sound gate and the existing physical knowledge regression gate all passed. Sound
snapshots and the two preceding failed fixtures are retained under
`run-sound-gate-20260907/`; knowledge regression evidence is under
`run-sound-knowledge-regression-20260907/`. Test servers stopped after saving.

The new development JAR supersedes older artifact hashes below:
`26d45ab995a09617517d70e8ac1238bcce544c4d14225f56b1baf807f1f6a790`.
The installed XMCL JAR, original worlds and user's stopped server remain unchanged.

## Latest independent-test and inventory-chat update

A true spawned `gpt-5.6-luna` subagent (Socrates) separately passed a two-stage
static-coordinate then dropped-item test using direct public tools and its own
route choices. See `docs/reviews/2026-09-07-spawned-luna.md`: first arrival was
0.2162 blocks from the requested point with an empty inventory, followed by
three physically acquired apples. Cold launch-to-first-motion was 41.035 seconds;
total pickup time was 66.367 seconds. This does not accept the supplied parkour.

The direct CLI now exposes the new gameplay tools and named dropped/waypoint
targets. The user's clarified acquisition policy is silence by default, optional
brief thanks/reports for direct player relevance or explicit requests, and no
repeated pickup/inventory/organization announcements. Host logic retains recent
acquisition evidence for later target-loss events, tracks already-reported gains,
and keeps idle reviews silent unless reporting was explicitly requested.
Python tests currently total 39; Java remains 19. Skill validation passes.
The final real-server speech trial verified silent ordinary pickup/organization
and one requested apple-count report without a repeated follow-up over 35 seconds.
See `docs/reviews/2026-09-07-inventory-chat-policy.md`; preserve the preceding
empty-message failure record.
The current scope and missing work are listed in
`docs/reviews/2026-09-07-request-coverage.md`. The JAR hash below is unchanged;
this update changed the locally linked Skill, not Java production code.

## Latest inventory/perception checkpoint

`docs/reviews/2026-09-07-inventory-perception.md` supersedes the artifact and
capability status below. The external Codex Skill now supports turning, bounded
perception, actual acquisition events, item grades/notes, exact per-route support
materials, dropped-stack targets and world-scoped named coordinates. See
`docs/INVENTORY_PERCEPTION_SPEC.md` for the refined contract and the review for
implemented limits. New tools have not been verified with the internal provider.

Verification: 19 JUnit and 31 Python tests, Skill validation, a real Forge physical
knowledge gate and the navigation regression gate. A persistent Luna model
session passed one-chat apple collection from empty inventory in adventure mode:
3.752 blocks of actual movement, three apples acquired after 15.530 seconds.
First movement took 14.682 seconds, so model latency remains a material issue.
This Luna session was started by the Skill through local Codex app-server, not
by the parent agent's subagent-spawn tool. The host may choose a unique safe
zero-material route. Do not label this a spawned-subagent or model route-choice
test. Failed parameter and dynamic-gravity trials remain recorded separately.

Final JAR SHA-256:
`e1701b591a0831a993043183cb6ff26f9d6c08d7a0fde30365d861402c41b4f5`.
The final storage guard and arrival-margin change have physical-gate evidence,
not a repeat Luna run.
The installed XMCL JAR and original saves remain unchanged. Bounded test servers
were saved/stopped; preserve the user's stopped offline server and OP state.
The supplied parkour, general terrain, complete structure recognition, exhaustive
item provenance and humanlike companion claims remain unaccepted.

## Latest movement/usability checkpoint

`docs/reviews/2026-09-06-usability-repairs.md` supersedes the artifact and
capability counts in earlier checkpoints below. Current verification is 14
JUnit tests, 22 Python tests, and one Forge gate containing 12 physical scenarios.
A separate Luna model-only controller also passed public-chat jump, relative
step, ten-block flat movement and conversation during movement on a real
dedicated server. The supplied parkour remains unaccepted. The latest measured
model delay is still noticeable; full-block stairs and ladders do not establish
general three-dimensional terrain traversal.

The public MCP includes `jump_once` and `jumpPhase`, signed `forward_blocks`,
and opt-in `allow_partial` with a distinct APPROACHED result. Normal player
navigation permits a partial approach; exact coordinate tests do not by default.
See the Skill for persistent session ownership and the safe unique-route policy.
Current JAR SHA-256:
`5098dbb330407d81a3dd25889c959228ee19853ab64dfcd61c31e93794684583`.
The installed XMCL JAR was not replaced. Preserve the saved offline-mode user
server `run-parkour-diagnostic-06` and its OP configuration; it was stopped when
work resumed. New bounded usability test servers do not replace that user world.

The first resumed Luna jump failed because the public tool registration was
missing despite a passing body test. Its failure record is retained. The fixed
public-interface results are in `run-luna-usability-final-20260906`; subsequent
local stop changes were physically checked separately. Do not combine their
attribution or claim the supplied map was completed.

## 2026-09-06 update (supersedes artifact and repair status below)

Five navigation findings are now repaired: armor-independent fall estimates,
exact-start/final-approach handling, live footing/fluid validation with physical
braking, stable-stop completion, and human-player discovery/UUID targets.
See `docs/reviews/2026-09-06-navigation-fixes.md` for full validation and limits.
The default body gate, five controlled repair scenarios, ten JUnit tests, two
Python client tests and the final public CLI/Skill gate passed. That final
operator was scripted by the implementation assistant, not an independent
source-blind model. Real-client acceptance remains pending.

The rebuilt `0.2.0-dev-mc26.2` JAR now has SHA-256
`f7d74820cf616b3532b825f5d6929343a20aab7445470790f153385479bd5e40`.
The old build is backed up under `run-navigation-artifact-backups/`.
The XMCL game was running during final tests, so its installed JAR remains the
old artifact described below. Exit Minecraft completely before installing the
replacement. The user Skill profile was preserved; isolated tests used their
own profiles, including port 25767 when the real game occupied 25766.

The remainder of this document describes the earlier checkpoint. In
particular, its old checksum and stale-prompt note are historical; use the
repair report above for current build identity and fixed behavior.

## 1. Executive status

MinePilot is an experimental Minecraft Java 26.2 Forge Agent. The repository
was reset to a clean baseline and is being rebuilt one observable capability at
a time. The current development build is `0.2.0-dev-mc26.2`.

The only end-to-end capabilities currently accepted are:

- creating one visible headless `ServerPlayer` body without a second Minecraft
  account;
- observing that body's position, health, hunger, hotbar, and inventory;
- receiving normal player chat and sending clearly labelled `[AI] MinePilot`
  chat;
- resolving a coordinate, player, entity, world spawn, respawn point, or death
  point as a navigation target;
- enforcing the staged navigation protocol `request -> visible acknowledgement
  -> plan -> choose -> execute -> verify`;
- physically moving the body across a simple prepared surface and proving the
  result from coordinates rather than text;
- controlling the same chat and navigation surface from a locally installed
  Codex Skill over an authenticated loopback MCP endpoint.

This is **not** a professional companion release. Mining, item handling,
crafting, combat, shelter construction, farms, redstone, trading, Nether/End
progression, speedrunning, long-term memory, skins, multi-Agent UI, and the M1
through M4 product claims are not implemented and verified in this rebuild.

## 2. Repository state

- Repository: `/Users/weida/Documents/minecraft-ai-companion-forge`
- Current branch: `codex/agent-rebuild-v2`
- Current HEAD: `467afa7 chore: reset MinePilot to clean Forge baseline`
- License: Apache-2.0
- Minecraft: 26.2
- Forge metadata range: `[65.0.0,66)`
- Java: 25
- Mod ID: `mcai_companion`
- Build version: `0.2.0-dev-mc26.2`

All rebuild work after `467afa7` is currently uncommitted. The working tree
contains modified and untracked production code, tests, resources, the Skill,
and this documentation. Do not reset, clean, switch branches destructively, or
restore the retired implementation over these files.

The retired implementation remains recoverable from Git history and the local
stash `pre-agent-rebuild-wip-2026-09-04`. It must not be copied back wholesale.

Before any commit, inspect the full diff, stage only intentional files, run
`scripts/preflight-before-commit.sh`, and avoid duplicate, placeholder, or
evidence-free commits. The user has currently asked not to perform GitHub work.

## 3. Architecture

### Agent body and runtime

- `MinecraftAiCompanion` owns server lifecycle hooks.
- `AgentRuntime` creates one stable MinePilot runtime and captures bounded
  visible player chat.
- `HeadlessPlayerSession` gives the Agent an embedded connection, pumps outbound
  packets, and ticks server-authoritative player physics.
- `MinePilotServerPlayer` is the actual body and owns vanilla position,
  inventory, hands, armor, health, food, effects, and statistics.
- Missing model configuration does not suppress the body. It leaves the body
  online and available to Codex external control.

The key movement fix is that the embedded connection must not call the normal
client-authoritative connection tick. A headless player sends no movement
packets, so that path restored the previous client position and silently undid
local movement. The current session ticks authoritative `ServerPlayer` physics
and pumps outbound packets separately.

### Model-as-encoder path

- `AgentSystemPrompt` tells the model to translate arbitrary natural language
  into typed tool calls and never treat prose as an action.
- `OpenAiCompatibleChatClient` is the single high-level model client.
- Configuration is runtime-only through `MINEPILOT_BASE_URL`,
  `MINEPILOT_API_KEY`, `MINEPILOT_MODEL`, and optional
  `MINEPILOT_TEMPERATURE` in `[0.0,1.0]`.
- Remote endpoints must use HTTPS. Plain HTTP is accepted only for loopback
  development.
- When an internal model controller is active, Codex external control is
  rejected. There is exactly one controller owner at a time.

The currently supplied external MiMo credential/channel did not complete a
live provider test: the configured relay returned HTTP 503 and the former MiMo
endpoint returned HTTP 401. No credential is stored in source, Git, world data,
the Skill profile, or this document. Treat every credential previously pasted
into chat as exposed and rotate it before future use.

### Navigation

- `NavigationToolCoordinator` owns request phases and enforces the visible
  acknowledgement barrier.
- `NavigationTargetResolver` resolves static and dynamic targets.
- `NavigationSnapshotBuilder` captures a bounded server-side world snapshot.
- `AnytimeNavigationPlanner` builds one to eight evaluated route options.
- `NavigationFollower` applies continuous control frames instead of changing
  coordinates directly.
- Planning runs off the server thread. World mutations and authoritative reads
  are marshalled back to the server thread.
- Completion re-resolves dynamic targets and checks the requested acceptance
  radius before emitting `COMPLETED`.

Planner fields include distance, time, predicted health/food loss, support
block cost, hazards, feasible state, supported paces, and path steps. The model
chooses among evaluated options; it does not generate raw movement per tick.

Only simple-surface physical navigation has black-box evidence. Door handling,
support placement, difficult terrain, moving-target pursuit, obstruction
recovery, water, cliffs, parkour, bridging, chunk-distance travel, and hostile
interference remain unaccepted until individually proven in real worlds.

### Codex bridge and Skill

`CodexMcpServer` binds only to `127.0.0.1:25766/mcp`. It uses a rotating random
Bearer token, Host and Origin validation, bounded request bodies, and owner-only
token-file permissions. The token is deleted when the server stops.

The exposed tools are:

- `observe`
- `read_chat`
- `say`
- `request_navigation`
- `plan_navigation`
- `navigation_status`
- `choose_navigation`
- `cancel_navigation`

There is no teleport, slash-command, filesystem, shell, credential, direct
inventory mutation, or direct world-edit tool.

The Skill is stored at `skills/minepilot-companion/` and locally installed as:

```text
/Users/weida/.codex/skills/minepilot-companion
  -> /Users/weida/Documents/minecraft-ai-companion-forge/skills/minepilot-companion
```

Its non-secret local profile is
`/Users/weida/.codex/minepilot-companion.json`, mode `0600`. It contains only
the loopback URL and the Minecraft instance's token-file path. The Python
client refuses HTTP redirects so the Bearer token cannot be forwarded to a
redirect destination.

One prompt sentence is now stale: `AgentSystemPrompt` still mentions
post-completion actions such as staying silent or sneaking that are not exposed
as MCP tools. Runtime `validActions` is correct and Sol accepted it, but the
prompt should be aligned before the next internal-model test.

## 4. Physical evidence already obtained

### Headless body physics

Forge GameTest `headless_control_frame_displacement` proves that the embedded
`ServerPlayer` moves at least three blocks under continuous control frames,
keeps health and inventory unchanged, and respects a per-tick continuity bound.
This proves the body boundary only, not general navigation.

### Codex Skill black-box navigation

A `gpt-5.6-luna` subagent acted as the high-level model substitute. It was
forbidden from reading source, GameTest internals, or server/mod logs. It could
use only the installed Skill, visible chat, coordinates, health, hunger,
hotbar, inventory, and navigation results.

In the accepted Forge 65.0.9 backend run:

- initial body position: `(5731171.5, -48.0, 958957.5)`;
- resolved TestHuman destination: `(5731181.5, -48.0, 958957.5)`;
- final body position: `(5731180.731496816, -48.0, 958957.5136019184)`;
- physical displacement: approximately 9.23 blocks;
- terminal distance: approximately 0.7686 blocks inside a 2.0-block radius;
- health and food remained 20;
- hotbar and inventory remained empty and unchanged;
- the request-correlated sequence was `observe -> request_navigation -> say ->
  plan_navigation -> choose_navigation -> navigation_status(COMPLETED)`;
- Forge selected exactly one required test and reported it passed;
- Gradle exited zero.

The terminal evidence record atomically binds request ID, outcome, target
identity, resolved destination, and body coordinates. The gate does not accept
model prose, planner output, or a `STARTED` phase as success.

A read-only `gpt-5.6-sol` audit closed the last four findings concerning target
attribution, phase-valid action names, interruption races, and trace retention.

## 5. Build and test commands

The repository-local JDK is used because no system JDK is registered:

```bash
export MINEPILOT_JAVA="$PWD/.toolchains/jdk-25/Contents/Home"
export PATH="$MINEPILOT_JAVA/bin:/usr/bin:/bin:/usr/sbin:/sbin"
```

Compile and run JUnit:

```bash
./gradlew test --rerun-tasks --no-build-cache
```

Run the default physical body gate on Forge 65.0.9:

```bash
./gradlew runGameTestServer --no-build-cache \
  -Pforge_compile_version=65.0.9
```

Run the independently operated Codex Skill gate:

```bash
./gradlew runGameTestServer --no-build-cache \
  -Pforge_compile_version=65.0.9 \
  -Dminepilot.codexSkillTest=true
```

That gate intentionally waits for a separate Codex/Luna operator. It must not
be reported as passing unless the operator uses the public Skill flow and a
target-bound physical terminal status is observed.

Run Skill client security tests:

```bash
python3 -m unittest discover \
  -s skills/minepilot-companion/tests -v
```

Build the development JAR:

```bash
./gradlew build --no-build-cache
```

The last built artifact was:

```text
build/libs/mcai_companion-0.2.0-dev-mc26.2.jar
SHA-256 c1d41e5ae18322cb50d648e2fbcf436fcea2771eabf3c39f87bbcfaf6c0808af
```

After source changes, regenerate the artifact and checksum. Never reuse the
recorded checksum for a different build.

## 6. Local XMCL installation state

The verified JAR copy is installed at:

```text
/Users/weida/Library/Application Support/xmcl/instances/
  26.2-forge65.0.9/mods/mcai_companion-0.2.0-dev-mc26.2.jar
```

Its checksum matched the repository build at the time of installation. The
previous `0.1.16` JAR was moved, not deleted, to:

```text
mods/.minepilot-backup/mcai_companion-0.1.16-dev-mc26.2.jar
```

The XMCL instance directory is named `26.2-forge65.0.9`, but the launcher UI
reported its configured Forge version as `26.2-forge-65.0.8`. The Mod metadata
allows this version because its range is `[65.0.0,66)`, but the discrepancy
must be kept visible in future test reports.

The last launch attempt opened XMCL and selected this instance, but the turn was
interrupted immediately after pressing Launch. A later process check found no
Minecraft/Forge Java process. Therefore Minecraft launch and real-client world
entry are **not verified**. XMCL itself remained running.

Once a world's integrated server has started, the Mod creates:

```text
config/mcai-companion/codex-mcp.token
```

Codex can then use `$minepilot-companion`. The token file does not exist while
the world/server is stopped, which is expected.

## 7. Known limitations and risks

1. No real-client acceptance run has been completed for the current JAR.
2. No valid external `mimo-v2.5` provider run has completed.
3. The current Skill can talk and move only. It cannot mine, pick up, drop,
   equip, eat, craft, fight, build, use containers, or interact with workstations.
4. Navigation evidence covers one short, flat, unobstructed route. It does not
   justify general terrain, follow, parkour, water-bucket, bridge, or survival
   claims.
5. The route snapshot and planner require performance and correctness gates on
   naturally generated worlds, distant targets, unloaded chunks, and no-human
   dedicated servers.
6. The current controller and runtime support one active Agent only.
7. There is no settings UI, onboarding flow, custom skin path, persistent
   memory, SQLite store, Xaero adapter, or general Mod adapter SPI in this rebuild.
8. Secret storage is environment-only for the internal model path. A production
   cross-platform credential store for macOS, Windows, and Linux is not present.
9. The current development JAR must not be advertised as M1, M2, M3, M4,
   professional companion, hardcore completion, or two-hour random-seed ready.

## 8. Required next steps

Proceed in this order and keep each gate evidence-based:

1. Finish launching the installed XMCL instance and enter a throwaway world.
   Confirm the current JAR loads, MinePilot appears in the player list and
   world, and the loopback token is created.
2. From a separate Codex task, use `$minepilot-companion` to observe, send one
   visible Chinese message, walk to the real player on flat ground, and verify
   the final coordinates. Record failure honestly if any step is missing.
3. Repeat navigation in natural terrain: slopes, doors, trees, one-block
   obstructions, safe descents, short gaps, moving targets, and temporary player
   obstruction. Fix general mechanisms, not fixture coordinates.
4. Align the internal system prompt with actual phase-valid tools, then rerun
   the public-chat model-as-encoder gate with a valid provider credential.
5. Add the next single gameplay tool: normal item acquisition and normal item
   drop. Test `give the Agent an item -> inventory observes it -> ask through
   chat -> Agent physically drops it -> inventory and world entity prove it`.
6. Only after movement and item handling are stable should the rebuild add
   mining, equipment/food reflexes, combat, crafting, containers, building, and
   longer survival objectives.

For every future capability, success means an externally observed state change.
Chat text, a model tool call, an internal log, a scripted assertion with no
physical observation, or code presence alone is not acceptance evidence.

## 9. File map

- `AGENTS.md`: repository execution and commit rules.
- `CODEX_GOAL_REBUILD.md`: clean-rebuild objective and acceptance philosophy.
- `docs/CODEX_RECOVERY_CHECKPOINT.md`: chronological root-cause and test notes.
- `src/main/java/dev/mcai/companion/MinecraftAiCompanion.java`: Forge entry point.
- `src/main/java/dev/mcai/companion/agent/AgentRuntime.java`: runtime and chat.
- `src/main/java/dev/mcai/companion/agent/body/`: headless player authority.
- `src/main/java/dev/mcai/companion/agent/model/`: prompt, schemas, and model client.
- `src/main/java/dev/mcai/companion/agent/navigation/`: target, planner, follower,
  route protocol, and physical movement.
- `src/main/java/dev/mcai/companion/codex/`: loopback MCP server and tool surface.
- `src/main/java/dev/mcai/companion/gametest/`: physical backend gates.
- `src/test/java/dev/mcai/companion/agent/navigation/`: planner unit tests.
- `skills/minepilot-companion/`: Codex Skill, client, metadata, and security tests.

## 10. Handoff rule

Start by reading `AGENTS.md`, `CODEX_GOAL_REBUILD.md`, this file, and
`docs/CODEX_RECOVERY_CHECKPOINT.md`. Preserve the dirty working tree and local
backup artifacts. Continue from the first unverified physical gate; do not
restart from the retired implementation and do not claim the long-term product
plan from the current narrow evidence.


## 2026-09-12 23:08 — player arrival and construction navigation

See `NAVIGATION_ARRIVAL_REPAIR_20260912.md`. Player one-shot arrival finishes within
three blocks; follow ends on scoped arrival chat or 600 stationary nearby ticks.
Six-block native bridge and three-block native pillar now physically pass with
exact inventory debit. Automatic routes accept up to 16 manifested expendable
supports. 44 Java / 128 Python tests and both native navigation gates passed.
Production backup: `/Users/weida/Documents/minecraft-ai-companion-forge/.minepilot-backup/navigation-arrival-1789222097`.
Installed JAR: `3f81fa164d26733f80e03b8002660d80eae10f6c6a3826199d71fd9df958c1e8`.
