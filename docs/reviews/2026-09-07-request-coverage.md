# User request coverage

Date: 2026-09-07. This is a capability ledger, not a completion claim.
Latest evidence: `2026-09-07-movement-continuation.md`; final installation hashes: `2026-09-07-movement-installation.json`.

| Requested outcome | Implemented | Acceptance / remaining work |
| --- | --- | --- |
| Refine the gameplay draft and resolve ambiguity | Written contract and Chinese user-facing draft | Explicitly distinguishes proximity knowledge from vision, visible markers from confirmed structures, and event delivery from model response latency |
| Static and dynamic navigation | Coordinates, players/entities, observed item UUIDs, remembered points | Thirteen controlled movement cases and sustained-follow/replacement gate passed; natural terrain and both new independent supplied-parkour runs remain unaccepted |
| Fluid conversational companionship | Persistent listener, chat interrupts model work without automatically stopping travel, bounded failure recovery and outcome reporting | Conversation during motion and single-reply stable stop passed; final ordinary replacement/stop replies still took 6.41/4.66 seconds, so low latency and long-duration acceptance remain open |
| Jump once / short step | Public actions; exact short phrases have a local fast path | Physically verified; fast-path latency is not independent language-model latency |
| Turn 0–360, relative to entity or sun | Public turn tool and reference/side calculations | Absolute turn verified; exhaustive relative/sun and unavailable-reference behavior not physically accepted |
| Receive actual item/count/current inventory | Post-pickup events, inventory deltas, cursor, silent-by-default speech policy and idle review | Actual pickup and idle annotations verified; history is bounded, not unlimited storage |
| Know source and giver | Item entity origin marks for toss/death and actor metadata, conservative unknown fallback | Real-human toss attribution not independently accepted; mining, broken-container, system grant and complete merged-stack lineage missing |
| Grade 0–5 and notes | Item/component identity policy, world persistence | Slot move/reload and requested note verified; no comprehensive policy for every future gameplay tool |
| Never spend grade 0–2 as support | Planner and executor checks; unreadable/failed storage protects stock | Physically verified, including policy change after planning and storage failure |
| Declare exact material X/count for route X | Every route returns exact supportMaterials, no undeclared substitution | A declared one-cobblestone route physically consumed one; protection stopped it with zero spent |
| Seek dropped stacks / notify disappearance | Observed UUID target and target-loss reason, acquisition correlation | Real normal-gravity pickup and disappearance verified; merge reasons remain ambiguous rather than guessed |
| Near blocks/items/entities and far vision | Requested proximity layers, 120-degree horizontal cone, custom passthrough policy, entity metadata | Expanded passthrough table passed, including bars, containers, water and redstone exceptions; exhaustive variants/relative facing and natural-world performance remain open |
| Control context size | Small default summary, grouping, paging, filters and bounded scan loops | Bounded-result tests passed; no worst-case TPS or token-cost benchmark |
| Broader entity/item/tree/structure search | Queries in loaded space up to 96 blocks; log/structure markers | Complete tree/structure recognition and exploration beyond that range missing; no hidden locate |
| Name and annotate coordinates | Dimension-scoped waypoints and navigation target | Save/reload verified; long-term map updating and cross-dimension portal traversal not implemented |
| Time/weather/basic state | Public world/body observations and received chat | Basic output verified; full environmental edge-case matrix not accepted |
| Real Luna subagent independently chooses routes | A true spawned gpt-5.6-luna direct operator was dispatched separately | Passed the short static-then-dynamic scenario with physical coordinates/inventory; original parkour still unaccepted; earlier Skill-owned Luna sessions are separate |
| Install and play current build in XMCL | New development JAR built and installed, original and intermediate JARs backed up | Hashes verified; real-client/world entry and Forge65.0.8 vs65.0.9 discrepancy remain unverified |
| Internal model tool integration | Shared seven knowledge-tool schemas/execution, shared target parser, route material details, new-chat cancellation | Local synthetic HTTP wire test passed; actual provider, in-world internal acquisition scheduling and end-to-end cancellation remain unaccepted |

## Next work order

1. Continue from the passed short spawned-subagent static/dynamic check to the
   supplied parkour acceptance with explicit failure records. Do not replace the original
   save or manipulate an active test to produce success.
2. Repair measured movement and responsiveness failures, then test interruptions,
   blocked/partial approaches, doors, height changes and moving destinations.
3. Complete acquisition provenance where actual hooks establish it; add explicit
   inventory-full/item-loss behavior, then normal item drop as a new capability.
4. Expand relative-facing and perception coverage, including natural-world cost.
5. Implement actual exploration/structure recognition, and verify new tool parity
   with the internal model controller before claiming that integration.

The long-term building, survival progression, production and redstone companion
vision remains a roadmap, not present release functionality. Test counts do not
establish a percentage completion or a 100 percent gameplay success rate.
