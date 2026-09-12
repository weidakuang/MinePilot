# Bounded excavation jobs

`plan_excavation` snapshots observed local geometry, then computes alternatives
on a worker. It returns `CAPTURING` or `PLANNING`; read `excavation_status` until
`PLAN_READY`, or let the persistent listener deliver its event. Planning does not
break blocks. Compare the returned scope, distances, time assumptions, tool
wear/remaining durability, support identities/counts and any limitation.

Modes:
- `access`, `target:{x,y,z}`: reach a native working position without breaking the
  final target. `allow_access:true` permits disclosed natural-terrain clearing.
- `resource_radius`, exact `block`, `radius:1..10`: matching sensed blocks in a
  sphere whose center is fixed at planning. It does not expand with the body.
- `region`, `from:{x,y,z}`, `to:{x,y,z}`: inclusive bounded cells, omitting air.
  Optional exact `block` filters the region. `relative:true` freezes offsets
  against the current integer body coordinate.
- `tunnel`, cardinal `direction`, `length:1..12`, `slope:-1|0|1`: a two-block-high
  local corridor. `fishbone` additionally uses `branch_length:1..8` and
  `branch_spacing:2..6`. Fishbone is optional, not an implicit gathering strategy.

`allow_supports:true` permits only returned ordinary inventory supports of grades
3..5. They remain as return-route infrastructure. No automatic torches. Local
segments are limited to targets within 14 blocks on each axis, 4096 breaks and
512 travel blocks; unknown cells can make a plan partial, not fictitiously clear.

Choose the exact `request_id` and `option_id` with `choose_excavation`. For a large
listed change, `confirm_destructive:true` is the model's scope confirmation; it
does not require a ritual player prompt. Never approve a different scope from the
player's task. Live block/tool/obstruction changes stop for a new decision.

Use parent `pause_excavation`, `resume_excavation`, `cancel_excavation` while it
owns the body. Chat, sensing and private inventory annotations remain available.
Empty cells prove clearance; `collectionVerified` and actual acquisitions separately
prove collection. Drops can be lost, unreachable or limited by inventory capacity.

Use `survey_mining` to compare local cave space, exposed ore, hazards, known return connectivity and current-biome generation hints; completion arrives as `mining_survey_event`. Survey output is a local graph, not a verified navigation route. Move/survey/collect in bounded segments. `fishbone` remains optional, never the default for every mining request. No automatic torches.

Excavation collects its own native drops by default (except access-only mode). Check both excavation completion and `collectionVerified`; PARTIAL may mean the requested blocks are empty but drops could not be collected. Full inventory and lost/restricted drops require a new model decision.

## Whole trees and declared groves

Use `inspect_tree` before `plan_excavation` mode `tree`, with its observed
`target:{x,y,z}`. This job evaluates all connected approved logs and a return
route, using native break reach and (when explicitly enabled) ordinary scaffold
stock. `allow_manual_grove:true` permits only a declared manual grove; it does
not authorize automated machinery or unknown ownership. `replant:true` reserves
the correct species seedlings and preserves a one-root or valid two-by-two root
layout. Manual groves default to replanting. Missing seedlings block approval.
Require `wholeTreeVerified`, actual collected quantities, and requested
`replantVerified` before reporting completion. Local capture limits and incomplete
large tree components remain explicit limitations. Existing scaffolds are kept.
