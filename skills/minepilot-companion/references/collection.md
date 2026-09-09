# Bounded collection

`plan_collection` accepts `resource: "wood"` or an exact block registry ID such
as `minecraft:coal_ore` with `output_item: "minecraft:coal"`. Set `count` (1..64),
`radius` (1..10, default 5), and optionally all of `x/y/z` for a fixed center.
The default center is the current body position and never follows the Agent.
Only matching sensed targets are included. Air is ignored. No fishbone tunnel
or torch placement is implied.

For wood, `source: "any"` offers loose logs and mature-tree alternatives;
`source: "tree"` defaults to `whole_tree: true`: every observed trunk block must be broken and acquired, even when `count` is smaller. `wholeTreeVerified` must be true before reporting the entire tree felled. `whole_tree: false` explicitly requests a quantity only, while `source: "drops"` only picks up.
Use `species` for the requested kind (default `any`). A particular tree requires
all of `tree_x/tree_y/tree_z`; the job must not substitute another tree.
`inspect_tree {x,y,z}` returns the observed connected component, species,
farm/building/inhabited-tree clues, and classification uncertainty.

A collection plan inspects at most eight tree components and offers up to four
source alternatives; no eligible option is not proof that an entire forest lacks
wood. Change the sensed seed or location when coverage is insufficient.

Review the returned target list, previewed inventory tool and remaining durability,
nominal wear, break-time estimate, source and movement budget. Then call
`choose_collection {request_id,option_id}` once. There may be fewer items than
requested; an exhausted plan returns BLOCKED with actual partial progress.
The current executor evaluates zero-support/no-predicted-health-loss local
routes internally. It cannot excavate access routes or build platforms.
Do not claim total travel time from the break-only estimate.

`collection_status {}` reports physical position, current route, blocks broken,
pickup receipts and matching inventory increase. The persistent listener emits
one collection result instead of a model call for every child block.
`pause_collection`, `resume_collection`, and `cancel_collection` take the exact
request_id. Chat, hearing and inventory annotations stay available throughout.
Cancel the collection before issuing unrelated movement or changing tools.
Do not interpret BLOCKED, speech or a broken block as a collected item.
Complete lineage of merged stacks is still unknown.

Known farms can be saved with `tree_farm` using `operation: "save"`, `name`,
`kind: "manual"|"automated"`, and all six inclusive `min_x/min_y/min_z` and
`max_x/max_y/max_z` coordinates (maximum span 129 per axis). `list` and `remove`
are also available. Save declarations supported by the player or actual
observations; appearance alone does not establish ownership.
Manual groves require `allow_managed_grove: true` in the collection plan.
Automated farm mechanisms, saplings, constructed wood, bee fixtures and
creaking hearts remain protected. Do not dismantle an automated farm to get
logs: collect accessible output drops or explain the missing machine/container
operation. Bamboo requires its own harvest/crafting workflow and is not a log.
Tall, giant, obstructed or partially sensed trees can stop for a new access
plan; current support does not imply every tree species/layout is accepted.

Tool previews search real storage/offhand slots and show the chosen slot and remaining durability. Approval equips through native inventory swaps. Ordinary stone and deepslate default to cobblestone and cobbled deepslate; unknown outputs still require an explicit item ID. The executor credits actual matching pickups, never a predicted loot count.

The target-block sphere remains fixed. Previously approved or job-emitted drops may be pursued within a two-block margin around that sphere because normal item ejection can cross its boundary. This margin never approves additional blocks for mining.
