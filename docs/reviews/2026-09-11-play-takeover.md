# Play takeover and acceptance — 2026-09-11

The previous task, user screenshots and native server logs were reviewed before
repairing the existing dirty branch. The original world was stopped on takeover;
normal-speed acceptance uses a separate copy at 25591/MCP25792. No test fixtures
or acceptance actions are applied to the user world.

## Source-informed methods

- [Mineflayer collectblock](https://github.com/PrismarineJS/mineflayer-collectblock/blob/master/docs/api.md): one operation owns approach, tool selection, breaking and pickup. Applied through the existing native CollectionCoordinator; inventory gain is checked before reporting recovery.
- [Mineflayer pathfinder](https://github.com/PrismarineJS/mineflayer-pathfinder): physical reach goals, composite goals, changing-path recovery and swimming are useful references. This project keeps native Forge physics rather than introducing a second protocol bot.
- [Baritone MineProcess](https://github.com/cabaletta/baritone/blob/master/src/main/java/baritone/process/MineProcess.java): periodic rescan, excluded inaccessible candidates, inventory quantity completion and direct overhead mining. Used as a behavioral reference, not copied source.
- [Mindcraft skills](https://github.com/mindcraft-bots/mindcraft/blob/develop/src/agent/library/skills.js) and [world helpers](https://github.com/mindcraft-bots/mindcraft/blob/develop/src/agent/library/world.js): bounded local nearest-resource queries, resource/output aliases, integrated pickup and explicit interruptions. Its move-away goal can choose any available direction; this project's helper supplies native collision/support-checked alternatives. These sources also contain limitations; an imported project is not proof of success here.
- [Voyager](https://github.com/MineDojo/Voyager): reusable executable skills and environment/error feedback support reducing model calls between low-level actions. Its independent survival results do not establish this project's survival competence.
- [Verity JE](https://www.curseforge.com/minecraft/mc-mods/verity-je): checked the Java project and its LLM-based companion description. The unrelated public decompilation repository is not treated as official source or copied into this project. Its presentation alone cannot establish its resource-search algorithm.

## Implemented and retained repairs

The earlier uninstalled repairs (grass replacement, native overhead reach,
bare-hand tool preference, item drop handling, swim controls and straight
movement projection) were preserved and tested. New takeover changes add:

- Physical four-heading sensing in one bounded native job. It returns ordinary
  visible/proximity results with explicit incomplete coverage; caps do not mean
  absence. New chat interrupts the scan.
- N-marker item rays include dropped stacks. Server mouse rotation, not delayed
  head animation, defines the human ray. Explicit marks survive implicit chat
  gaze for 60 seconds. Nearby blocks are grouped and items retain identity/count.
- Exact nearby workbench placement and short yielding bypass model inference
  before the action. Negations, questions and specified locations still use
  normal interpretation. Completed short movements cannot replay their chat.
- Nearby auto-placement includes the upper integer cell when standing on paths,
  farmland or slabs. Mining/pickup approaches use native collision-shape heights.
- Yield candidates use supported route-grid centers, avoiding a real path-edge
  case where a physically balanced endpoint had an unsupported grid center.
- Resource names are normalized before collection. Chat context keeps recent
  acquisitions but compacts repeated provenance trees without changing inventory.

## Verification

Java build and 33 JUnit tests pass. 90 Python tests pass. Native Forge gate groups
passed: knowledge, collection, placement, navigation repair and play experience.
The final play gate covers 14 stages including path-edge yield, path-height
pickup, real water traversal, overhead wood, bare-hand workbench recovery,
Chinese bed/golem names, pointed emeralds and a physical four-heading scan.

Native evidence:

- Four-heading scan: 26 game ticks, all four cardinal-direction golems observed.
- Open-ground diagonal walk: about 10.07 blocks travelled with lateral error
  below 0.00002 blocks in that fixture; this is not global optimal-path proof.
- Pond crossing: the body spent 69 game ticks in water and reached the far bank.
- Path-height workbench pickup: 10 game ticks, actual inventory gain.
- Path-edge yield: final gate passed with 33 movement ticks.

Normal 20 TPS copied-world evidence (separate from accelerated GameTests):

- Two exact workbench placements took 10 and 7 server ticks from chat to the
  placement receipt (0.50 and 0.35 seconds). Actual world block and inventory
  debit were inspected. See placement-acceptance.json in the copied server.
- Luna Fast completed workbench recovery, leaving air in the old cell and one
  workbench in inventory. Wooden-pickaxe damage stayed at the original 1/59.
  The second recovery was observed completed after 10.31 seconds of polling;
  that measurement excludes the preceding one-second console-tool wait.
- Final yield arrived within the observation upper bound of 48 server ticks
  (2.4 seconds), walked rather than sprinted, and did not issue another move
  after completion. See yield-acceptance.json and its later-position check.

Failed tests were used to fix defects, not counted as passes: the human ray
initially used stale head rotation; the first sweep fixture lacked clear lines
of sight in two directions; an old coal fixture overlapped the body's boundary;
the fast-yield radius was below the public minimum; completed movement replayed
its relative request; and a real path edge did not align with the route grid.

## Acceptance boundaries

Client N-key registration and native marker processing are present, and native
ray/item tests pass. A real keyboard press in a freshly restarted XMCL client
has not been independently accepted. Client restart is required after replacing
the jar. General natural-language requests still pay model inference latency;
the sub-second workbench numbers apply to exact fast controls. Broad autonomous
survival and universally optimal routes are not claimed by these regressions.
