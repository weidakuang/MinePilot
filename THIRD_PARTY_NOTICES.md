# Third-party notices

## Numen components

The following components are adapted from [Dwinovo/minecraft-numen](https://github.com/Dwinovo/minecraft-numen/tree/34ef004dac3095fbbd928a897927e277c69d02fa), pinned commit `34ef004dac3095fbbd928a897927e277c69d02fa`.
Copyright (c) 2026 Dwinovo. Adaptations dated 2026-09-12 remain under **LGPL-3.0-only**. The upstream public API MIT exception is not used for these components. No upstream assets, UI or client runtime are included.

All destination paths below are relative to `src/main/java/dev/mcai/companion/vendor/numen/`. In upstream paths, `core/` means `core/common/src/main/java/com/dwinovo/numen/core/` and `api/` means `api/common/src/main/java/com/dwinovo/numen/`.

| Upstream source | Destination | Changes and retained behavior |
| --- | --- | --- |
| `core/scan/BlockScanner.java` | `scan/BlockScanner.java` | Palette/section scanning retained; package, English comments, public loaded-chunk accessor. |
| `core/scan/RingSpiral.java` | `scan/RingSpiral.java` | Ring enumeration retained; package relocation. |
| `core/scan/SearchGeometry.java` | `scan/SearchGeometry.java` | Nearest-first section order and distance stop bound retained; include unequal X/Z chunk offsets. |
| `core/scan/SearchBudget.java` | `scan/SearchBudget.java` | Shared time/section budget retained; Forge 26.2 server ticking adapter. |
| `core/scan/BlockSearch.java` | `scan/BlockSearch.java` | Incremental loaded-column search, result sorting, caps/deadline and coverage ledger retained; per-server ownership, exposed-resource predicate and cancellation. |
| `core/act/Interaction.java` (`fireUseBlock`) | `tools/BlockInteraction.java` | Native hand-order use and consumed-action swing retained; resolved-hit execution shared by placement and workstation use, 26.2 server player adapter. Placement keeps its exact authorized hand and receipt checks. |
| `core/tools/CraftOps.java` | `tools/CraftOps.java` | Recipe batches, ingredient selection, native menu crafting and leftover cleanup retained; 26.2 recipe display/placement APIs, MinePilot body/inventory adapters, actual output delta verification, and a bounded native log-to-planks prerequisite for one workbench. |
| `core/tools/MenuOps.java` | `tools/MenuOps.java` | Native pickup/quick-move/drip/sweep sequences retained; 26.2 ContainerInput and body adapter. |
| `core/tools/ContainerOps.java` | `tools/ContainerOps.java` | Slot checks and exact/native automatic transfers retained; server-side adapter, bounded public moves and result snapshots. |
| `core/task/base/RecoveryLadder.java` | `task/RecoveryLadder.java` | Ordered retries and failure-specific fallbacks retained; generic adapter used for checked resource approaches. |
| `core/task/chain/BreathChain.java` | `movement/BreathChain.java` | Native swim-up, ceiling detection and bounded air-opening search retained; existing control frame, connected-water first-step search, idle surface hold. No position/oxygen fabrication. |
| `core/task/build/BuildOrder.java` | `build/BuildOrder.java` | Support passes, vertical/stage ordering and alternating rows retained; small target DTO, 26.2 block classes. Existing MinePilot placement controls pacing. |
| `api/event/EventQueue.java` | `event/EventQueue.java` | Bounded urgent/event batching and journal interface retained; world/body-scoped MinePilot journal. |
| `api/event/EventTypes.java` | `event/EventTypes.java` | Event constants retained; package relocation. |
| `ai/src/main/java/com/dwinovo/numen/agent/llm/CompactSplit.java` | `memory/CompactSplit.java` | CJK-aware recent token budget and role boundaries retained; Gson dialogue adapter. |
| `api/common/src/client/java/com/dwinovo/numen/client/agent/WorkBlockMemory.java` | `memory/WorkBlockMemory.java` | Station classification, recency eviction, serialization and rendering retained; server-only world/body/dimension files, observed validation, atomic saves, bounded loads and corrupt-file preservation. |

`tools/NativeInventory.java`, `tools/RecipeProbe.java` and `tools/ActionResult.java` are new MinePilot adapters under the repository's Apache-2.0 license. MinePilot orchestration in `agent/` composes these components with its existing native player, navigation, mining, placement, inventory and provenance systems. The external Python listener uses the event/current-state design with independently implemented adapters; it does not import Numen's AI transport.

The LGPL and incorporated GPL texts are distributed in every binary under `META-INF/licenses/numen/`. The **exact adapted Java sources**, including all listed components, are embedded under `META-INF/sources/numen/`; this notice is embedded as well. The source tree and Gradle build allow rebuilding and replacing the components. Modified JARs are not restricted by signatures. Distributions of this combined mod must preserve these notices and the LGPL rights; the repository's Apache license does not replace the copied components' LGPL license.

Other MinePilot source remains Apache-2.0 unless its own notice states otherwise. Other projects mentioned in research/review notes are behavioral comparisons, not copied source.

The 2026-09-12 follow correction also compares Numen's
`core/task/move/FollowCompanionTask.java` and `core/pathing/execute/PlayerNav.java`
at the same pinned commit. Their resident/live-target navigation behavior informed
an independent MinePilot implementation in `NavigationFollower` and
`NavigationToolCoordinator`. No Numen path executor or body-control source was
copied into those classes; normal MinePilot navigation is retained.

The external listener also adapts `ai/src/main/java/com/dwinovo/numen/agent/prompt/NumenPrompts.java` (ENTITY_PROMPT) into `skills/minepilot-companion/scripts/numen_prompt.py`, under LGPL-3.0-only at the same pinned revision. The prompt retains direct tool execution, resident/deferred tools and companion style; tool names, current-state attachment and server-only restrictions are adapted. The Python source and license texts are distributed with the skill.

Player rendezvous completion additionally follows the live proximity check in
`FollowCompanionTask`. Bridge edge approach and jump-then-place pillar execution
adapt the action ordering in `MovementTraverse` and `MovementPillar` at the same
pinned Numen revision. MinePilot retains its snapshot planner, collision checks,
real material manifest, native item use, and server-player physics. The local
adaptations are in `AnytimeNavigationPlanner`, `NavigationFollower`, and
`NavigationToolCoordinator`; the upstream movement classes are not wholesale ports.

On 2026-09-13, `BlockScanner` gained copied palette-section scans, and
`BlockSearch` gained bounded asynchronous batches and live main-thread candidate
validation. Their runtime world-read budget is now MinePilot's shared
`agent/concurrent/MainThreadBudget`; the ported `SearchBudget` remains a reference
and test primitive, not a second production budget. The new shared worker and
budget adapters remain Apache-2.0; modifications inside the Numen source files
retain LGPL-3.0-only.
