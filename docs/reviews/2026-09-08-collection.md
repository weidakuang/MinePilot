# Bounded collection and tree sources — 2026-09-08

## Implemented behavior

The new shared `plan_collection` / `choose_collection` interface executes a
bounded multi-block gathering job using the existing normal-player breaker and
physical navigation follower. One source/cost approval covers repeated aiming,
normal breaks, local movement and pickup; child block results do not trigger
one model turn per block. Chat, hearing, perception and item annotations remain
available. Pause/cancel stop both the current break and approach; resume checks
current resources. Standalone movement/tool changes cannot steal the body.

`resource: wood` distinguishes `source: any`, `tree` and `drops`. Any offers
loose-log and mature-tree alternatives; an explicit tree seed cannot be silently
substituted. Exact non-wood block IDs require the expected output item ID. The
center is fixed when planned, radius is 1..10 blocks, and only matching sensed
blocks/drop identities are eligible. Quantity is 1..64; exhausting eligible
resources returns BLOCKED with actual partial progress. Air is rejected.
Fishbone mining is optional, never required or implicitly started by gathering.
Its tunnel executor remains unimplemented.

Plans expose exact target positions, held tool/current durability, nominal wear,
break-only time estimate, movement budget, source evidence and empty support
manifest. Current collection routes require zero support consumption and zero
predicted health loss. Their actual evaluated distance/time are exposed during
execution. Total travel distance and completion time are not precomputed, and
access excavation, scaffolding and automatic tool alternatives remain open.
Newly collected/protected blocks are never consumed by this job.

Completion requires matching pickup receipts and a corresponding live inventory
increase. Tree/ore credits are capped by the physical drops emitted by the job's
breaks. Old inventory, speech, route completion and block destruction alone do
not satisfy the item goal. Full attribution of every unit after stack merging
is not established; receipts retain that uncertainty. Missing selected drops
return a decision rather than a fake collection or endless pursuit.

## Trees and farms

`inspect_tree` recognizes unstripped vanilla oak, spruce, birch, jungle, acacia,
dark oak, mangrove, cherry, pale oak, crimson and warped trunk IDs. It examines
connected same-species logs/stems, compatible ground/roots and matching
non-persistent leaves or fungal cap. Different axes and branches are allowed.
At most eight candidate tree components are inspected per collection plan, with up to four source alternatives; this is not proof of absence in uninspected components.
The connected component is capped at 128 logs and must be observed. Bamboo,
roots, stripped/decorative wood and unknown mod species need separate handling.
This is not acceptance of every naturally generated shape of those species.

Machine components, planks/stripped wood/beds, bee fixtures and creaking hearts
exclude a tree from automatic harvesting. Saplings, regularly spaced trunk rows
and explicit farm declarations distinguish candidate managed groves. These are
classification clues, not proof of player ownership or a perfect universal farm
detector. `tree_farm` saves world/dimension-specific inclusive areas as manual or
automated. Explicit manual harvesting requires `allow_managed_grove`; automated
areas and machinery stay protected. Execution rechecks declarations and nearby
fixtures. Tree-farm machine operation, output containers and replanting are not
implemented; accessible loose output logs may be collected.

Registry/API behavior was checked against the installed Forge 65.0.9 Minecraft
26.2 sources, including `TreeFeatures`, block states, normal break actions and
`Player`'s actual pickup AABB. Additional primary references:
[Forge tag documentation](https://docs.minecraftforge.net/en/1.20.1/datagen/server/tags/)
and [Mojang's introduction of the creaking heart in pale oak trees](https://www.minecraft.net/en-us/article/minecraft-snapshot-24w40a).
The exact current registry and runtime take precedence over historical articles.

## Physical and protocol verification

Real Forge 65.0.9 `collection_tool_regressions`, through the public MCP dispatcher:

- Loose oak logs: inventory +4, no tree blocks broken.
- One approval, bare hands: three trunk blocks actually broken, oak logs +3;
  pause for 60 game ticks, chat, resume and body ownership checks passed.
- Branched birch fixture: four logs acquired and wooden axe damage +4.
- Exact stop chat cancels a partially progressed job; no delayed destruction
  or additional tool wear after 80 game ticks.
- Coal radius boundary: only one eligible ore included; coal +1, BLOCKED for
  requested quantity two, no automatic tunnel or scope expansion.
- Removed dropped entity: zero acquired, BLOCKED.
- All eleven trunk families passed bounded classification fixtures. Machine,
  construction, bee/heart, planting-row and saved-farm checks passed; manual
  farm declaration survived ledger reload.

Final empty-hand regression gate:
`run-collection-empty-hand-gate-20260908/gametestserver/gametestworld/collection-physical-evidence.json`.
The controlled plan samples took approximately 0.07..5.16 ms on the server thread;
this is not a natural-world performance percentile or model response latency.

The original single-block physical gate and existing thirteen-case navigation
gate passed on the collection source. Build, 26 JUnit tests, 54 Python tests,
Skill validation and whitespace checks passed. Synthetic HTTP tests verify that
both model and MCP schemas include collection tools; they are not a live-provider
acceptance test.

Fixture creation, resource replenishment and body resets between separate cases
are source-informed test setup, never Agent gameplay. GameTest runs faster than
real time, so its wall-clock duration is not a user-facing latency measurement.
Independent normal-tick gameplay is recorded separately below.

## Failures retained and fixes

- Missing GameTest namespace annotation initially prevented mod loading in the
  new gate; the registration was corrected.
- A drop under a hanging trunk was treated as a body destination. The collector
  now finds collision-free neighboring pickup positions.
- A distance-only pickup check accepted a position outside the effective pickup
  box. Planning/arrival now use the native pickup geometry with approach margin,
  and wait for drop settling. A bounded retry handles a changed pickup position.
- One fixture regrew a tree over the Agent after harvesting; separate-case setup
  now resets the body before placing that next fixture. The production occupied
  space guard correctly rejected this setup.

The failed logs remain in their original run directories. They are not passing
evidence and were not erased or replaced with hand-authored success records.

## Independent gameplay: first trial failed

A fresh `gpt-5.6-luna` operator used only the installed Skill and public gameplay
interface against a normal-tick Forge 65.0.9 dedicated server. A vanilla
`minecraft:birch` configured feature generated the test tree before the trial.
The body began at `(0.5, -60, 0.5)`, survival mode, empty inventory. The Chinese
chat request required felling nearby birch and collecting three birch logs.
The parent observed only chat, inventory, dimension and coordinates, with no
operator deliberation or server/world data during play.

This trial FAILED: only one birch log entered inventory, after 42.94 seconds
from the visible request. Final coordinates were approximately
`(3.2766, -60, 1.5409)`. The acquired log filled the selected empty hand slot,
which was incorrectly rejected as a tool change before the second break.
The empty-hand plan now selects another existing empty hotbar slot through the
normal selection packet. It never moves item counts or discards collected wood.
No free slot returns a clear inventory-management failure. A real Forge fixture
now begins with empty inventory/selected slot zero and reproduces this case;
it completes three breaks and pickups. The earlier fixture reserved slot eight
and therefore did not cover this behavior.

First-trial results and allowed observations are retained under
`run-collection-independent-20260908`. This failed independent trial is not
reported as successful because a source-informed regression subsequently passed.

## Independent gameplay: fresh retest passed

A second fresh Luna operator received the same public Chinese request, Skill
and observation restrictions, without the earlier failure or implementation
hints. A new world and another vanilla-generated birch were used. It acquired
three birch logs from an empty survival inventory, with no parent gameplay
intervention. Final stable position was approximately `(4.5930, -60, 0.2133)`
in `minecraft:overworld`.

Allowed observations measured 43.71 seconds to first displacement, 44.18 seconds
to the first log, and 53.33 seconds to three logs. Stationary mining may precede
first displacement; these are observation timings, not a model/tool trace.
No completion chat was emitted, which is allowed for this request. This is one
cold direct-task trial, not persistent-session latency or a p95 measurement.
It demonstrates item acquisition and exposes that the overall response remains
slow; it does not close the natural-language latency requirement.

Both Luna agents and both dedicated servers/observers have been stopped. The
successful result and inventory/chat/coordinate observations are retained under
`run-collection-independent-retest-20260908`; `physical-results.json` separates
the first quantity-satisfying observation from final stable coordinates.
The parent did not inspect operator tools/deliberation, so the independent
record does not claim a specific plan count or tool sequence. The source-informed
public-dispatcher gate independently proves the one-approval multi-block path.

## Remaining acceptance

Natural-language latency, live internal provider calls, arbitrary natural terrain,
tall/giant/partly hidden trees, mixed-origin stack lineage, container sources,
replanting and automated tree-farm operation remain open. The original parkour,
access excavation, cuboids, cave scoring, return strategy and fishbone execution
are not completed by this increment.

## Installed artifact and repository state

The final development JAR was built and installed into the existing XMCL
instance. SHA-256: `8fa48ec45bac6f7febfe2985a314a8123ab744f75bf2b7fa9db1d8d21f6cb04b`. The previous JAR is preserved in
`mods/.minepilot-backup`; exact paths and hashes are in
`2026-09-08-collection-installation.json`. No Minecraft Java process was running
at replacement. No user save was modified.

All backend tests used Forge 65.0.9. The instance directory is named 65.0.9, but
the launcher previously reported Forge 65.0.8; actual client world entry was not
repeated. Source changes remain uncommitted on `codex/backup-rebuild-20260908`;
the earlier GitHub backup was not overwritten or force-pushed.
