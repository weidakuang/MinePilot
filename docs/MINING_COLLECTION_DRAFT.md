# Mining and collection design draft

Date: 2026-09-08. Status: user-confirmed design; implementation and gameplay
acceptance remain pending. The user confirmed the design with the near-sphere,
air-omission and fixed-radius material-filter clarifications below.
The user requested understanding and refinement before implementation. This file
does not authorize reporting mining, caves, tunnelling or excavation as working.
The existing implementation was backed up separately at GitHub commit `81dfb6b`
on `codex/backup-rebuild-20260908` before this draft was written.

## First implementation checkpoint

The single-block foundation is now implemented and physically tested; see
[the September 8 report](reviews/2026-09-08-mining-foundation.md). All region,
access-excavation and exploration behavior below remains design. This draft's
user approval is separate from gameplay acceptance.

## Intended experience

The player describes a goal in normal chat. The model chooses among evaluated
plans; a persistent executor performs legal movement, aiming, mining and pickup
without requiring a model decision per block. Chat remains available throughout.
The model can interrupt, change goals, query progress, classify items, and decide
whether a completion message is useful. Silence is valid. No automatic torches
or substitute lighting unless the player requests lighting.

Two layers share one physical action executor: navigation can propose excavation
needed to reach an interaction position, while collection can propose movement
needed to harvest a bounded target set. They must not recursively run competing
navigation and mining controllers on the same body.

## Knowledge and geometry

- Replace the earlier block-proximity cube specification with a sphere: a block
  center is included when its squared distance from the body's feet position is
  at most 100. Within this sphere perception covers every direction, including
  above, below and behind the body, independent of facing and occlusion: the
  user's omnidirectional, see-through fisheye analogy. Skip air in model-facing
  block results; do not emit individual air records. Collision/navigation may
  still check empty space internally, without turning it into observation rows.
  Fluids and other non-air states remain observable. Dropped entities use their
  actual positions within the same radius. This is an explicit
  occlusion-bypassing local sensor, not ordinary player eyesight. Entity proximity
  at 16 blocks is a separate pre-existing policy, not silently changed here.
- Beyond the 10-block proximity sphere, blocks and dropped items require vision:
  horizontal heading +/-60 degrees, at most 96 loaded blocks from the eyes, not
  an additional 96 blocks beyond the sphere. This outer view is directional and
  occlusion-tested; it has no general through-wall or rear-facing sensing. The
  existing vertical pitch +/-60 degrees is the proposed default, to be documented
  explicitly. A visible part of a block face can qualify; a center-only ray is
  insufficient for partly exposed blocks. Broad scans must report coverage and
  remain bounded rather than claiming exhaustive visibility from sparse rays.
- Preserve the user's pass-through categories: fluids, foliage, doors, gates,
  fences, containers, enchanting tables, signs, crops, carpets, slabs and redstone
  components other than redstone blocks. Entities also do not occlude this custom
  view. These objects still appear in observations. Use versioned data tags and
  shape checks, not unrestricted name substring rules as the final policy.
- Perception transparency never changes collisions or the physical interaction
  ray. Knowing ore behind glass or a door does not permit mining through it.
- Unloaded cells are unknown, not air or absent ore. No save-file, seed, hidden
  locate command or unlimited remote world scan substitutes for exploration.
- Store detailed local geometry in the runtime; provide the model grouped
  materials, vein candidates, air-space connectivity and nearby hazards by
  default. Supply exact coordinates/states through filtered, paginated queries.
  Report dimension, origin, capture time, revision, coverage and truncation.
  Update with bounded work and changes rather than sending thousands of cells
  on every chat turn. Benchmark server-tick time and model payload size separately.

## Goals and spatial boundaries

Support a single block, material(s) within a sphere, an inclusive block cuboid,
a tunnel cross-section/path, a levelling height, and a bounded vein expansion.
Exact block IDs/tags distinguish coal ore, deepslate coal ore, coal items and
decorative coal blocks; natural-language grouping must be resolved in the plan.
"Mine all coal ore within five blocks" means matching coal-ore targets inside
that fixed sphere, not all materials in the sphere. Resolve the requested family
(for example ordinary and deepslate coal ore) into an explicit target filter in
the plan. Access excavation is a separately disclosed operation; it does not
silently convert selective mining into clearing the entire sphere.

Freeze the origin, dimension and heading when resolving relative coordinates.
Return both the original relative instruction and resolved absolute bounds.
World-axis offsets and body-relative forward/right/up offsets must be explicitly
different modes. Do not let a five-block mining radius move with the body and
turn a finite request into endless excavation. New discoveries inside the fixed
bounds may join only within the selected plan's target/resource limits; expansion
beyond them requires a new model-approved plan. Specify whether a region includes
its boundary and what floor/ceiling is preserved. Coordinates alone do not imply
that every block between the player and destination may be destroyed.

For an ore six blocks below the body, plan a legal standing position with a clear
interaction ray to a target face, within the body's real reach. Arrival requires
actual position, stable support, reach, facing and current target-state checks.
Do not navigate into the ore block itself or mine straight down under the body.
An ordinary movement request defaults to no excavation; mining/clearing goals can
explicitly allow a bounded access corridor whose destruction is separately listed.

## Plans and selection

Proposed interface names: `request_mining`, `plan_mining`, `choose_mining`,
`mining_status`, `pause_mining`, `resume_mining`, `cancel_mining`. Navigation gains
an interaction-position goal and an explicit excavation policy. Names and schemas
will be settled during the first small implementation, not added as dormant tools.

Generate up to three useful alternatives where available: less destruction/risk,
less time, and less resource consumption. Return fewer when plans are equivalent
or dominated; never invent A/B/C just to fill the UI. No feasible plan returns
specific reasons and any actually verified partial approach, not a success claim.

Each option exposes:

| Field | Contract |
| --- | --- |
| Identity | Request ID, option ID, plan revision, dimension and expiration |
| Geometry | Current coordinates, fixed work bounds, approach/return route |
| Work | Target blocks, access blocks, protected/excluded blocks and counts |
| Movement | Walking/climbing distance, support placements and ordered phases |
| Equipment | Exact tool instance/slot, current/max durability, enchantments, harvest eligibility |
| Durability | Expected use, conservative range, predicted remaining durability, reserve and substitutions |
| Materials | Exact support item/component identities, grades, notes, counts and placement positions |
| Time | Estimated range for travel, breaking and pickup; assumptions and uncertainty |
| Yield | Expected items/range or unknown; never fabricated exact random loot |
| Risk | Fall, lava, flooding, falling blocks, mobs, tool breakage, trapping and uncertainty |
| Limits | Maximum destruction, duration, distance, support use and permitted region |
| Completion | What counts as mined, collected, skipped, partial and unreachable |

Bare hands are a real candidate with durability not applicable, not an infinite
durability tool. A breakable block that yields no requested loot under that tool
is unsuitable for collection, even if it is usable for an explicitly approved
clearance operation. Tool speed/harvest eligibility comes from actual game rules,
current state, enchantments and effects. Do not hard-code all tools as consuming
one durability point per block. Estimates must handle random durability effects,
repair and modded behavior; compare actual damage after every completed operation.

Support blocks graded 0, 1 or 2 remain forbidden, including during emergency
replanning. Unclassified blocks stay protected. Never lower grades automatically
to make a route feasible. Revalidate the exact support manifest before placement;
newly mined cobblestone is not automatically expendable. Keep tool-use permission
separate from permission to consume a block as scaffolding. Named/borrowed tools
may be usable with a durability reserve without becoming disposable material.

## Continuous execution, chat and interruption

Suggested lifecycle: REQUESTED -> PLANNING -> AWAITING_MODEL_CHOICE -> EXECUTING
-> VERIFYING -> COMPLETED/PARTIAL/BLOCKED/CANCELLED. PAUSED and REPLAN_REQUIRED
retain a resumable goal but never imply completion. Waiting for a decision holds
at a safe location. Speech policy is separate: progress and completion events can
be silent; a long chat/tool exchange is not an execution prerequisite.

The executor owns movement, head/body orientation, selected hand and block-use
actions under one job generation. It sequences approach, aim, break, step, support
placement and pickup. Movement while mining is permitted only while real reach,
targeting and vanilla progress remain valid; otherwise stop walking to mine.
Do not choose a faster-than-vanilla break speed to meet a latency target.

Use the normal player break-action lifecycle, including start/abort/finish and
server progress, game-mode restrictions, Forge cancellation, tool wear, block
updates, loot and experience. Do not call a world-edit operation or direct block
destruction as a shortcut. The pinned local Forge 65.0.9 source determines exact
APIs. Adventure mode from the old parkour test does not automatically authorize
mining; use survival for ordinary mining tests or deliberately permitted adventure
tools, and test denied adventure actions without bypassing them.

Chat and read-only queries continue independently while a job runs. Item notes
and grades can be edited concurrently; a change invalidating reserved supports
pauses before spending them. Actual slot movement, tool switching, eating or
container use must coordinate with active hand/slot reservations and briefly pause
breaking as needed. Model output is never a second physical controller.

Pause/cancel preempts on the next server tick after the command is accepted,
aborts unfinished breaking, clears movement/jump input and prevents stale plans
from continuing. Already broken blocks are not restored; falling bodies retain
normal momentum. Before breaking another block, revalidate the job generation.
Resume re-observes changed terrain and resources. Restart preserves a paused job
record, never silently resumes destructive actions.

Natural-language recognition latency and command-to-physical-stop latency are
different measures. The current multi-second model path is an unresolved blocker;
next-tick executor cancellation alone does not fix it. Design a priority
interruption path and test varied natural stop requests, negation, quoted speech,
chat during work and target replacement. Mere casual conversation must not stop
the job; old model replies must not restart it.

## Unexpected conditions and bounded destruction

Revalidate the next block, support, escape space and tool at action boundaries.
Pause/replan on lava/flooding, falling material, unstable ledges, a player entering
the work area, changed targets, insufficient reach, denied breaking, full inventory,
nearly broken tools or loss of a return route. Include the concrete reason and
remaining feasible work in status. Do not retry an unchanged obstruction forever.
Keep a conservative escape margin; recovery does not justify teleportation,
invulnerability, protected-material spending or a second body.

For large excavations, show the MODEL the resolved region, volume, non-air count,
access damage, containers/redstone/portals and other sensitive structures, with
estimated versus known coverage. Require its explicit confirmation of a particular
plan revision and limits. This is not another player confirmation dialog. A model
confirmation cannot expand beyond the player's requested region/purpose or bypass
server permissions. Unknown block ownership remains unknown; do not assume every
natural-looking block is disposable. Explicitly permitted clearing can include
selected sensitive blocks, but that scope must be in the preview.

No single hard-coded volume threshold is a complete damage policy. Use configurable
volume budgets plus semantic warnings; even one container or machine component
can matter. Split large work into bounded segments and re-evaluate changing cost
or hazard outside the approved envelope. Completed segments remain recorded.

## Collection and provenance

Track block-break operation -> emitted item entities -> split/merge transfers ->
actual pickup counts. Preserve the originating block/dimension, actor, tool,
request and event time when observed causally. Use unknown when instrumentation
cannot prove lineage. Proximity is not proof of who supplied a drop. Broken
container contents and the container block itself need distinct origins.

Keep target blocks broken, byproduct blocks broken, expected drops, actual emitted
drops and actual acquired items as separate totals. Walking to a drop does not
prove acquisition. Loot taken by another player, destroyed in lava, despawned or
unreachable produces a truthful partial result rather than infinite pursuit.
Full inventory triggers a bounded pause/return strategy; no implicit discarding.

Stable equipment policy identity must survive durability changes, slot moves and
stack operations. The current full-component inventory key can change when damage
changes; mining must not create fake acquisitions or lose an item's grade/note
after each use. Instance identity, material identity and provenance lots are
related but distinct records.

Queue acquisition information promptly for the model, grouping bursts without
losing counts. Routine ore and byproduct gains are silent by default. Player
gifts/loans, requested pickups/reports or direct player relevance may justify
optional thanks/reporting. Completion can be spoken or silent; deduplicate it
against pickup and inventory messages. Do not stream a narration for every block.

## Cave exploration and branch mining

A cave candidate is connected observed navigable air space, not one air block or
a promise of natural origin. Describe entrance, clearance, passage connectivity,
depth/span, known ore exposure, unexplored frontiers, hazards, mobs, and verified
return cost. Rank candidates with explainable components, coverage and uncertainty.
Unknown routes cannot receive a perfect safety score. Large flooded shafts may
rank below modest accessible passages. Player tunnels and caves may be ambiguous.

Cave mining explores physically, extends only from known frontiers, harvests
approved exposed/locally sensed resources, and preserves a route back. Set search,
time, inventory and durability limits; no discovery is a valid bounded outcome.
Remember explored branches to avoid oscillation. Structure recognition is not a
prerequisite for labelling an observed traversable cave candidate.

Branch/fishbone mining is optional and must be selected for the current objective; it is never a prerequisite or implicit default for ordinary collection.
It uses an explicit level, main corridor, branch spacing,
cross-section, branch length and finite work envelope. Resolve a useful depth from
version/dimension/world-generation data or a stated assumption, never a universal
magic Y level. Report expected strategy benefits rather than guaranteed rare ore.
Use newly sensed veins within the approved expansion policy, then return to the
planned corridor. Account for travel back, remaining tools and full inventory.
No automatic torches; darkness-related hazards still affect plan estimates.

## Implementation and acceptance order

1. Fix the block sphere/coverage contract and durable tool observation/identity.
   Establish legal single-block break, abort and physical drop/pickup evidence.
2. Add interaction-position navigation and a short explicitly approved access
   corridor, with movement and excavation sharing the same executor.
3. Add bounded material-radius collection with plan choice, resource manifests,
   ordinary chat during work, replacement, low-latency pause and truthful results.
4. Add relative/cuboid clearing, tunnelling, supports and destruction previews.
5. Add bounded cave-frontier exploration and branch-mining strategies.

Do not jump to whole-cave autonomy while the first steps fail. Existing 3D
navigation and latency failures remain prerequisites, not solved by this document.

Physical gates must cover: correct/incorrect/hand tools; cancelled and prohibited
breaks; coal ore below the body; two veins inside/outside a fixed radius; a changed
block; real wear and drops; pickup/source attribution; grade 0..2 exclusion;
slabs/doors/ledges; lava/gravel; full inventory; tool near breakage; stale approval;
relative-origin drift; denied large-region expansion; chat/stop/replacement during
breaking; and no unsolicited torches or repeated reports.

Measure request-to-first-response, request-to-first-action, accepted-cancel-to-stop,
natural-stop-to-stop, extra blocks broken after stop, and missed/duplicate actions.
Report cold/warm samples and p50/p95 only with enough runs, including failures.

Mining acceptance needs before/after block and durability observations in addition
to coordinates, chat and inventory. Keep the previous parkour's narrower observer
rules intact for parkour. For future independent mining tests, define the allowed
world-observation surface explicitly; source-informed block-state GameTests are
separate evidence and never claimed as source-blind Luna gameplay. Never inspect
private model reasoning to guide or rescue an independent run.

## Sources checked

- [Forge 26.2 player game-mode patch](https://github.com/MinecraftForge/MinecraftForge/blob/26.2/patches/minecraft/net/minecraft/server/level/ServerPlayerGameMode.java.patch)
  shows the break-progress, event cancellation, harvest, wear and drop boundaries.
  Consult the pinned local 65.0.9 sources before implementing exact signatures.
- [Forge 26.2 block events](https://github.com/MinecraftForge/MinecraftForge/blob/26.2/src/main/java/net/minecraftforge/event/level/BlockEvent.java)
  is a reference for event contracts, not proof that every provenance hook exists.
- [Mojang's Caves and Cliffs Part II features](https://www.minecraft.net/ja-jp/article/caves---cliffs-part-ii-the-features)
  documents generation/distribution changes; it is historical background, not a
  verified 26.2 optimum mining-depth table.

## Wood collection implementation

The first bounded continuous collection increment is implemented and documented
in [the collection review](reviews/2026-09-08-collection.md). Resource goals may
choose loose logs or mature-tree sources; explicit felling constrains the source
and a named trunk constrains the component. Farm cues/declarations preserve
saplings and production mechanisms. This does not complete access excavation,
all tree shapes, replanting, container withdrawal, cave or fishbone execution.
