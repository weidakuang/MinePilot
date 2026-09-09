# Placement foundation acceptance — 2026-09-08

Implemented a shared internal/external placement interface, native storage/main/
offhand swaps, component-aware capacity, bounded region/text-layer jobs, physical
underfoot jumping, per-obstruction choices, normal obstacle mining with inventory
tool selection, and declared temporary support cleanup. Navigation support now
uses the same face geometry, checks actual block state plus item debit, and can
select real storage slots. Protected importance 0..2 remains unavailable for
navigation and temporary supports.

No production placement path uses setBlock, setPos, teleport, generated stacks,
or direct inventory writes. The native BlockItem placement-state preview is
exposed read-only through the access transformer. Actual placement is through
ServerPlayerGameMode.useItemOn; inventory changes use the native menu swap and
selected-slot packet paths. Mining is the existing normal-player break engine.

## Scope and protocol

See `skills/minepilot-companion/references/placement.md` for exact supported tools
and JSON examples. Direct use targets must be within five blocks and also meet
the live vanilla eye interaction range, clear ray, legal face/state, collision,
server permissions and survival restrictions. Doors/beds are one action with two
verified cells. Desired state properties constrain native context; they are not
world-state setters.

A batch has at most 256 item-use anchors. Its fixed-origin text grid has at most
4096 cells; periods preserve contents and underscores require sensed empty air.
Optional local approaches stay inside 24 blocks of the origin with a declared
travel budget. Exact unsearched route/time costs remain unknown. A blocked job
returns current target/ray observations, exact decision ID and available options;
a normal break is offered with the selected real tool's wear/time/remaining
capacity only if the mining engine can evaluate it. Retry cannot authorize an
unlisted target mutation. Skip produces PARTIAL. Cancellation never rolls back
already placed or mined cells.

Pause/cancel reaches child routes and breaks. Completed placement is verified in
the same server-thread action so a pause cannot leave an unrecorded item debit.
Underfoot landing also survives pause/resume. Chat remains available. Both model
paths expose the same tool schemas; the persistent Skill skips redundant model
turns after accepted async starts and suppresses per-block child result events.

## Physical validation

The source-informed Forge 65.0.9 `placement_tool_regressions` gate passed all 18
recorded case groups, including real inventory conservation/capacity; missing
item and reach rejection; horizontal log, upper slab, door/bed and wall torch
states; native underfoot jump/landing continuity; pause/chat/hand change/resume;
exact obstruction choices and stale-decision rejection; normal stone excavation
using an automatically selected storage pickaxe and one observed durability loss;
bounded physical approach; cancellation without delayed placement; explicit skip
as PARTIAL; protected temporary materials; normal support cleanup; physical wall
occlusion; text layout with reserved opening; and offhand placement.

Evidence: `2026-09-08-placement-physical-evidence.json`. Fixture setup is explicitly
source-informed test administration, not model gameplay or arbitrary-world proof.

The knowledge/support gate passed after its support inventory was moved out of
the hotbar: protected grade change consumed zero blocks; the approved route
consumed exactly one cobblestone, placed a real support and physically arrived.
Mining and collection physical regressions passed. Navigation repair's first
regression run reached SEARCH_LIMIT_REACHED in its half-slab scenario; an unchanged
repeat and the final geometry-build regression passed all 13 scenarios. Preserve
that intermittent search-budget failure; do not claim universal navigation or
100% reliability from these passes.

Two placement defects were found and fixed during physical testing: crouching
changes eye height so aiming must refresh its context; horizontal face sampling
must vary both surface coordinates rather than sample a diagonal. A small angular
margin now avoids grazing corners that can hit the neighboring block after float
rounding. When the obstacle test changed from dirt to stone, fixture item entities
also had to be cleared between independent cases so the next case could not pick
up an earlier cobblestone drop and invalidate its inventory baseline.

`./gradlew test build --offline --no-build-cache --console plain
-Pforge_compile_version=65.0.9` passed 29 JUnit tests, including bounded layout
validation and synthetic internal-provider wire schema parity. The Skill's 65
Python tests and skill-creator metadata validator passed. No valid live internal
provider claim follows from the synthetic protocol test.

## Normal Luna play

A normal-rate dedicated Forge server ran with a persistent `gpt-5.6-luna` Skill
controller, without parent placement commands after fixture preparation. Only
public chat, inventory, dimension/coordinates and bounded actual block states
were inspected; no model reasoning or decision output was inspected. One Chinese
chat requested a three-block cobblestone row. The final build physically created
all three cells at (2,-60,0)..(2,-60,2), with inventory 8 -> 5 and the body at
(0.5,-60,0.5). A later question received the correct remaining count while the
listener was still active.

| Measured public event | Final build | Earlier build trial |
| --- | ---: | ---: |
| First visible model reply | 10.76 s | 10.65 s |
| All three actual cells and inventory debit observed | 15.45 s | 14.90 s |
| Completion chat | 18.79 s | 20.20 s |
| Later chat reply | 3.60 s | 4.15 s |

Sampling was about 0.15 seconds. These two simple trials are not latency guarantees.
`2026-09-08-placement-luna.json` binds each trial to its build hash and public
observations. Parent fixture administration is recorded separately from gameplay.

## Artifact and limits

Built and installed SHA-256:
`401aea6801e98937413483c248099951f2902fceb922df1fb1b132dff5025a77`.
The prior XMCL JAR was backed up before atomic replacement; see
`2026-09-08-placement-installation.json`. The test listener/server stopped and the
loopback token was removed. Original user worlds were not modified by these tests.
The last-known XMCL UI configuration was Forge 65.0.8 while the folder is named
65.0.9; backend tests used 65.0.9. Actual client world entry is still unverified.

This does not accept automatic house/frame recognition, whole-house design,
second-storey renovation, native log stripping, arbitrary scaffolding recovery,
long-distance rail construction or functioning railway/redstone networks.
The larger design-only example remains a separate, unbuilt format. Natural-world
performance and uncommon compound/modded block placement need further gates.
Existing uncommitted mining/latency work was preserved; no Git commit or push was
made for this implementation request.
