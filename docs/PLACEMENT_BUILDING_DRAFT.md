# Placement, text blueprints and hand control design

Date: 2026-09-08. Status: the user endorsed the overall design direction and added
the renovation scenarios in section 11. Detailed defaults remain design proposals;
none of this placement capability is implemented or physically accepted.
This document refines the user's placement request. Proposed tool names
below do not describe tools currently available to a model. No production source,
installed Skill or JAR was changed by this design review.

## 1. Result the player should experience

A normal chat request can place one selected inventory item, build a path between
fixed coordinates, or construct a model-designed building. The model chooses the
materials, final shape and evaluated execution policy. A persistent executor
handles ordinary inventory swaps, movement, aiming, jumping, native item use and
verification. Each block does not require another model turn. Conversation and
explicit stop remain available while the job is running.

The same placement action must serve direct model calls, construction, navigation
supports and mining supports. Parent jobs may delegate bounded actions; they must
not run independent followers that compete for the same body.

## 2. Important corrections to the initial draft

- Five blocks is a tool target envelope, not permission to extend vanilla reach.
  For a direct request, resolve the target and check its center against a sphere
  of radius 5 from the captured body feet. Actual item use additionally requires
  an unobstructed hit within the live native interaction range from the eyes
  (4.5 by default in the installed 26.2 sources). A locally known target behind a
  wall is not physically clickable. The custom perception transparency list must
  never be reused for placement interaction rays.
- A large batch may extend beyond that initial sphere. Its coordinates remain
  fixed; the executor must physically approach successive work positions, and
  each item-use action must satisfy the five-block envelope and native reach at
  execution time. A direct `place_one` does not silently walk elsewhere.
- The Agent still has a real vanilla inventory and hotbar. A uniform model API
  can hide hotbar management by performing ordinary inventory/menu swaps. It
  cannot create a second inventory, duplicate items or equip nonexistent stacks.
- Block facing is not a generic 0..360 property. The body can face any heading;
  blocks expose their own legal cardinal facing, axis, half, attachment, hinge
  and other properties. The controller computes a legal player pose and clicked
  surface to obtain the requested state; it cannot set that state directly.
- `Air` in a hand means an empty hand. Air in a blueprint means reserved empty
  space. An omitted blueprint coordinate means preserve existing content. These
  three meanings must be distinct; none authorizes deleting a world block.
- Interacting successfully with a block is not evidence of placement: it may
  open a chest or toggle a door. Verify the requested world state, inventory
  consumption and relevant companion cells after the native action.

## 3. Model-facing representation of a building

Use a declarative blueprint, rather than a huge natural-language paragraph or
model-generated executable code. Its canonical form contains:

- An ID, revision, dimension, fixed origin, integer dimensions, and local axes.
  Local x means right, y up, z forward. Freeze the origin and orientation when
  submitting; do not rotate the building whenever the Agent turns. World-axis
  offsets and body-relative offsets are separate input modes. Grid rotation is
  0/90/180/270 degrees, with a declared pivot and resolved world bounds; arbitrary
  player headings must not silently become a distorted integer-grid building.
- A palette: symbolic material names mapped to registry items, desired legal
  block-state constraints and a material/substitution policy. Full glass blocks
  and glass panes are different entries. Unknown/modded state rules are reported,
  not guessed from a name substring.
- A bounded set of primitives: individual cells, axis-aligned lines, inclusive
  cuboids, hollow shells and explicit reserved regions. A diagonal road needs a
  declared rasterized path/width, not an ambiguous pair of endpoints. Overlapping
  conflicting writes are validation errors, not last-write-wins surprises.
- Semantic features: a two-cell doorway, a two-cell bed, windows, access passages,
  floor, roof, fixtures and any protected existing structures. Doors and beds are
  each one inventory item with multiple resulting block cells. Their second cell
  is a dependency of the same placement, never a second item-use operation.
- Constraints such as keeping an exit usable, leaving headroom, retaining a
  working route to each unfinished section, and mounting a torch on a supporting
  face. Blueprint lighting is explicit; mining still does not add torches unless
  requested.

The runtime can render any small portion as text layers or a cross-section:
`blueprint_view(plan_id, layer_y, bounds, offset, limit)`. Show desired/existing
state differences, conflicts, current body position and unknown cells separately.
The palette and full blueprint stay in runtime storage; ordinary model events
carry counts, current region, changes and blocking reasons with a revision and
coverage indicator. Exact cells are paginated on demand. This avoids replaying
thousands of block records on every conversation turn.

`examples/placement-house.blueprint.json` demonstrates a 5 x 7 footprint, three
interior levels, roof, reserved air, two-high door, windows, bed, wall torch and
chest. It is an illustrative target layout, not a built-in `build_house` macro
or an accepted building capability. A model must be able to create and revise
other sizes and layouts through the same general representation.

## 4. Proposed public interfaces

| Interface | Purpose |
| --- | --- |
| `inventory` / `inventory_capacity` | Current identities, components, counts, reservations, hands, empty storage slots and item-specific remaining capacity |
| `equip_item` | Select an observed identity/slot for main hand, offhand, or empty hand using ordinary swaps |
| `place_one` | Start one bounded, already-local native placement, optionally a jump placement beneath the body; always returns a request ID |
| `plan_placement` | Validate a fixed line/region/blueprint and return evaluated material, work-position and obstacle policies |
| `choose_placement` | Approve an exact plan revision and option ID |
| `placement_status` | Physical results, actual consumption, progress and current work step |
| `pause_placement`, `resume_placement`, `cancel_placement` | Interrupt the parent and all active children; resume revalidates reality |
| `resolve_placement` | Choose an exact, current obstacle response after a blocked event |
| `blueprint_view`, `revise_blueprint` | Inspect or deliberately change a bounded design without immediate world mutation |

These are design names. The implementation should share schemas between the
internal model, public MCP and persistent Skill, including phase-valid actions.

`place_one` takes a real item identity (or unambiguous observed item/slot), exact
integer target, desired state constraints, chosen hand, optional auto-equip, and
whether the target is beneath the current feet. Its direct request authorizes
that single placement only. If satisfying it requires movement, excavation or
additional materials, return evaluated alternatives for model approval instead of
silently broadening the operation. A batch requires plan/choose before execution.

A placement plan needs at least:

- Requested final cells and state constraints; already-correct cells and conflicts.
- Exact permanent-item counts and temporary support counts by inventory identity;
  available, reserved and missing quantities. Material estimates count actions
  and item costs as well as resulting cells (beds, doors and merged slabs differ).
- Legal candidate work positions, movement distance, hand changes, jump steps,
  clicked anchors/faces and placement dependencies. Feasibility/coverage must be
  explicit; an unsearched alternative is unknown, not a proven route.
- Excavation targets, selected tool and actual durability, estimated wear and
  time, temporary-block cleanup and estimated recoverable items. Estimates carry
  assumptions and confidence; the runtime never invents exact time or guaranteed
  drops for unknown rules.
- Fluid, falling-block, player/mob collision, unstable attachments, server denial,
  protected builds, fall risk and other currently observed hazards. A route that
  avoids an obstacle while omitting required blocks is partial, not completion.
- Plan revision/expiry and resource limits. A stale plan, changed material policy,
  disappeared tool or materially changed geometry requires revalidation.

## 5. Ordinary placement and underfoot jump placement

For each action: recheck ownership, loaded geometry, inventory, server permissions
and goal; equip from the actual inventory; choose a reachable anchor/replaceable
surface and a legal hit point; turn through normal control frames; validate the
actual aim ray and target state; invoke native player item use; then verify the
result and actual cost. Do not synthesize a hit behind an obstructing wall.

Native rules govern waterlogging, attachment survival, collision, item cooldowns,
world bounds, Adventure restrictions and Forge cancellations. Sneaking to place
against an interactable block is an explicit control action, not a bypass of its
interaction handler. Items that place entities need their own footprint/result
adapters and are not silently treated as ordinary blocks. Unsupported item rules
must yield a specific reason.

Underfoot placement targets the empty cell occupied by the feet, rather than the
already-solid support below it. Inspect stable takeoff, headroom, a clickable
support and a safe landing before jumping. Execute an ordinary jump, wait until
the body's collision box actually clears the requested cell, aim down, place and
verify landing on the new block. A normal jump does not guarantee that arbitrary
stairs, slabs or a low ceiling permit the maneuver. Limit attempts and time;
never repeat jumping indefinitely or assign position/velocity to simulate it.

A stop prevents new item uses and cancels child mining/navigation immediately at
the next server processing point. An airborne body still follows gravity and
should finish a safe landing without initiating another placement. No rollback
of already placed blocks or consumed materials is implied by cancel.

## 6. Construction planning and temporary supports

The desired blueprint is separate from its execution schedule. Compile a
support/dependency graph, choose work positions, and reserve the inventory budget
before starting. Optimize unnecessary travel and hand changes while maintaining
access. Walls must not seal the Agent into a room or eliminate the only legal
click position for an unfinished roof/fixture.

For a simple room: prepare the disclosed foundation, retain door/window openings,
place reachable wall sections, obtain safe roof access, build the roof, place
windows and fixtures with their native dependencies, finish the doorway, clean up
approved temporary supports, and verify the final room plus reachable exit.
The exact ordering is derived from geometry; it is not universally "bottom up".

A temporary support is allowed only if the selected plan lists its item identity,
quantity, coordinates, purpose and later cleanup. The chain must start from real
clickable geometry, remain bounded and preserve support/exit dependencies. It is
not permission to fly or to build a hidden bridge outside the approved region.

Navigation/mining/temporary building supports must never use importance 0..2.
An explicit permanent material assignment is a different purpose and must respect
the player's intended use and saved item notes; never downgrade a protected grade
just to make an automatic plan feasible. Reserved quantities are unavailable to
other jobs. If the policy changes during execution, revalidate before consuming.

Removing a temporary block uses normal mining and its real tool/time/drop rules.
Do not remove it if a torch, door, gravity block, redstone mechanism or current
body still depends on it. Do not promise recovery of glass or every drop. If a
recorded temporary block changed, treat ownership as uncertain and do not blindly
break the current block at that coordinate. Cleanup outside the initially known
budget requires another model-approved response.

On cancellation, report remaining temporary cells rather than deleting them
without a new instruction. If gameplay later supports resumable jobs across a
restart, reconcile live blocks and inventory first; never replay cached placement
steps blindly.

## 7. Obstacle handoff

Use `BLOCKED` with a structured reason, affected target and actual obstacle
identity/state/position. Distinguish occupied cell, absent anchor, out of reach,
blocked ray, entity collision, lost material, unsupported requested orientation,
unsafe jump, liquid/falling-block risk, protected structure and server refusal.

Return only evaluated alternatives, with a revision and their actual prerequisites:

- Stop or cancel the remaining work; list partial results and remaining supports.
- Wait briefly for a moving entity, with a bounded timeout and no guarantee it moves.
- Approach from another reachable work position, retaining the same final design.
- Mine specifically listed obstructions, naming the tool, current durability,
  nominal wear, estimated time, dropped-item uncertainty and destruction scope.
- Add a disclosed temporary support chain and its cleanup operation/cost.
- Skip a target or revise the route/design, clearly identifying unmet requirements.

The list need not contain three entries. Infeasible candidates either carry an
explicit reason or are omitted; an unknown route has null cost and unknown status,
not invented numbers. Empty-handed digging has no durability, rather than 0/0.
For a tool, show remaining/max durability and expected wear separately; precision
is limited by tool enchantments, effects and server rules.

A model chooses the returned response. It can approve an evaluated repair within
the authorized construction goal without routinely asking the player to confirm.
Player speech is reserved for useful progress, an important failure, or an
intent-level ambiguity such as whether an existing player structure may be removed.
No unconditional "I'm coming" when a plan is already known to be blocked.
Repeated unchanged failures should stop and report once, not chatter or spin.

## 8. Hand selection and inventory capacity

Expose item identity, source slot(s), item/component match, quantity, current
main/offhand, durability and note/policy. The model can address any ordinary
storage stack; the executor swaps it into a real hand through native inventory
operations and verifies count/component conservation. Equipment slots and an
open external container are not silently treated as extra storage. For full
inventories, ordinary swapping is still possible, but emptying a hand requires a
legal destination or merge space for the held stack. Never drop an item merely
to make room without authorization.

`equip_item(hand="main"|"offhand", item="air")` requests an empty hand. Emptying
the offhand can merge into compatible partial stacks when possible. A failed
move must preserve both stacks. Auto-equip uses the same path as explicit equip
and must not erase the other hand. Return which stacks actually moved.

Body-changing work owns a single execution lease. During an active break/place,
a conflicting equip request must pause/abort that physical step or queue at a
safe step boundary; it cannot silently invalidate the approved tool. Report the
choice to the model. Read-only inventory queries, private note edits and chat do
not acquire the body lease. A new destination/goal safely replaces the parent,
not only one child follower.

Capacity has three distinct quantities:

1. Empty ordinary storage slots out of 36 (hotbar included), with offhand/armor
   reported separately.
2. Space in existing compatible stacks, using actual stack component equality
   and each slot/item's maximum rather than assuming every item stacks to 64.
3. Additional capacity for one requested item: compatible-stack space plus empty
   slot space. This is conditional on dedicating those free slots to that item;
   capacities for different items cannot all be added together. A batch packing
   query must allocate the shared slots once.

Example only: one cobblestone stack at 40/64 and two empty ordinary slots permits
24 more cobblestone without occupying a new slot, or 152 total if both free slots
are devoted to it. A non-stackable item could instead use two slots. Named,
component-bearing, damaged or modded stacks must use real stack compatibility.
Raw storage capacity and unreserved capacity for new plans should both be shown.

## 9. Review of the current rebuild

The existing `NavigationFollower.tryPlaceSupport` selects only a hotbar stack,
constructs adjacent-face hit results without a complete native visibility/reach
check, and increments consumption on `consumesAction()`. `ServerPlayerGameMode`
can return that result for container/door interaction; its direct `useItemOn`
entry also does not supply all the connection's packet-level reach guards.
Replace this path with the shared verified placement primitive during
implementation, retaining support-material identity/count and importance checks.
Do not advertise the current support path as physically accepted scaffolding.

`MiningCoordinator.equip` supports slots 0..8 and checks body ownership.
`InventoryLedger.inventory` reports entries/equipment but not the requested
item-specific free capacity. Both need the hand/capacity extension above.
The native body already provides bounded turning and genuine jumping, and the
persistent listener already supports chat while jobs run. Reuse those mechanisms.

## 10. Implementation and physical acceptance order

1. General inventory-to-main/offhand swaps, empty-hand behavior, exact capacity;
   test full inventories, incompatible components and conservation.
2. Native single placement and state verification, then use the same primitive
   for existing navigation supports. Test missing inventory, blocked ray, reach,
   denied interaction and interactable anchors with no false success.
3. Underfoot jumping: actual continuous coordinates, placement, inventory debit
   and stable landing; low ceilings and cancellation must not produce jump loops.
4. State families: log axes, stairs/halves, slab merging, wall/floor torches,
   doors/hinges/two cells and beds/two cells. Test the specific current version,
   not assumed generic orientation logic.
5. Fixed path/region execution, legal approach, pause/chat/cancel, obstacle
   alternatives, temporary supports and physically verified cleanup.
6. Model-generated text blueprint construction on a real server: independently
   choose materials/layout, retain door/window voids, install real fixtures,
   reconcile all required states and costs, and leave a reachable exit.
7. Apply the bounded editing workflow in section 11 to existing structures:
   frame infill, selective replacement, native stripping, elevation-based
   earthworks and a second storey with access. Verify untouched context as well
   as required changes. Validate rail connections in short sections before
   attempting surveyed long-distance routes and actual minecart journeys.

For physical verification, inventory reduction plus a tool return alone is
insufficient: observe actual placed blocks/states and companion cells. Existing
inventory/chat/coordinates-only observers can disprove fake collection, but
cannot prove a finished house's geometry. A placement acceptance observer must
also use bounded public observed-block data (for example `sense`) or a human's
real-client inspection. A source-informed GameTest can be an additional explicit
white-box gate, never mislabeled independent model gameplay.

Static blueprint dimensions/counts do not establish legal reach, collision,
placement, pathfinding or model reliability. Each new family needs actual game
acceptance, and model/server/network latency is measured separately. Do not
promise a house in 30 seconds from the previous three-log latency samples.

## 11. Existing-world editing and the user's scenario extensions

The user added these examples after endorsing the broad design: add a second
storey to this house; fill this frame with cherry wood; level this hill to our
height and replace the floor with cobblestone; strip the wood in our home; build
your own hut; and lay a railway from home to the village. These are product
scenarios for the shared engine, not six hard-coded command strings or claims of
current functionality.

### Ground references before planning mutations

Resolve a phrase such as "this", "our height", "the frame" or "home" into a
bounded selection with evidence before changing anything. A selection carries
the chat request/speaker, dimension, immutable coordinates or cell mask, origin,
semantic role, relevant actual states, captured time/revision, scan coverage,
unknown cells and excluded/protected features. Plans refer to this selection's
ID and revision; turning, walking and later unrelated chat do not retarget it.

Possible evidence includes an explicit coordinate/marker, a previously designated
building or blueprint, the player's indicated block/face, and an unambiguous
locally sensed structure. The current waypoint is a point, not proof of a house's
entire footprint or ownership. A material-connected component can include an
attached neighbour, bridge or tree, so connectivity alone cannot define the scope.
Likewise, a few village markers do not prove a complete village boundary.

The current public player observation does not expose a grounded player focus
hit. If pointing is added, expose a bounded, timestamped result through the public
game interface, tied to the speaker, rather than pretending the Agent can already
see the player's crosshair. A player's cue can identify a destination, but does
not disclose every unseen block of its structure. Existing perception limits,
unknown space and ordinary interaction reach remain in force.

When there is one clear candidate, state the concrete interpretation briefly and
proceed within the player's request; no ritual confirmation for every frame or
floor. For multiple plausible frames, different floor heights, or a surface vs
solid-volume ambiguity with materially different results, ask one targeted
in-game question about the actual candidates. Do not ask the user now to locate
objects from these hypothetical examples. A preview may show a text slice or
optional non-mutating selection overlay, but must not require leaving Minecraft.

### Compile an edit of observed state into the desired result

Extend a blueprint with explicit operation kinds: preserve, add/fill, remove,
replace matching cells, and apply a named native tool transformation. Region
queries and editing plans remain bounded. A "replace" result is implemented as
ordinary break/collect/place steps; it is never a world setter. A "strip" result
is a normal axe interaction and does not become break-and-replace.

Show the delta: unchanged cells, additions, removals, replacements, tool-use
transformations, reserved voids and unknown/uninspected cells. Already-correct
cells are satisfied without consuming another item. Each mutating step checks its
expected prior state. A player edit invalidates affected steps and dependencies;
it must not be overwritten by a stale cached job. Repeated or resumed requests
compare against the current world instead of blindly repeating physical actions.

Excavation, reuse of drops, tool selection, placement and cleanup belong to one
parent job with a shared material budget and interruptible child actions. Useful
recovered materials can reduce future purchases/collection only after actual
pickup; planned drops are not items already in inventory. Include storage space
for debris and tool/durability reserves. Expected future pickups must not be
double-counted as currently available materials.

| Player scenario | Required interpretation and execution | Physical completion criterion |
| --- | --- | --- |
| Add a second storey to this house | Identify its footprint, roof, ceiling, existing doors/windows, fittings and protected machinery. Plan whether the roof becomes a floor or needs partial removal, the new room height, stairs/ladder/access opening and new roof. Preserve the first storey except explicitly listed changes. | Required new floor/walls/roof/openings exist; access connects both levels; preserved first-storey content remains; temporary supports are accounted for. |
| Fill this frame with cherry wood | Identify the selected frame and its plane/volume. Resolve cherry planks vs logs from context or an exact material choice. Fill only the intended bounded interior; retain the frame and designated openings unless their alteration was requested. | All required interior cells have the chosen states, intended openings remain, and the frame/outside cells were not changed. |
| Level the hill to our height, then use cobblestone flooring | Capture a reference standing surface or indicated top face and a finite horizontal footprint. Distinguish surface elevation from block Y, especially slabs/stairs. Disclose cut volume above the plane, bounded fill/foundation below it and the final cobblestone surface. | The requested footprint has the selected surface height/material and required clearance; permitted fill/cut limits are met, with remaining unsupported/unknown cells reported. |
| Strip the wood in our home | Select strippable placed blocks belonging to the bounded home structure. Use the actual axe/tool action, preserving position, species and axis. Exclude planks, already stripped blocks, nearby trees and container contents; only supported registered transformations qualify. | The selected blocks have the intended stripped states with positions/axes preserved; actual tool wear is recorded and unrelated blocks/items remain. |
| Build your own hut | The model selects a suitable observed site, bounded layout and available materials compatible with the player's goal, creates a blueprint and chooses an evaluated plan. Its design is not limited to the example house. | The chosen blueprint, fixtures, access and material accounting are physically verified. |
| Lay rails from home to the village | Bind actual endpoint locations and connection faces, survey a bounded corridor and compare route/elevation/material choices. Plan roadbed, rail shapes, bends, slopes, clearance and any disclosed bridge/tunnel work. Distinguish connected track from powered passenger service. | Track topology reaches the bound endpoints with required shapes/supports. If rideable service is the selected goal, separately verify propulsion and a real minecart journey, including direction requirements. |

For a levelled hill, never turn an unspecified hill boundary into an unlimited
connected-mountain excavation or fill all underground caves to bedrock. The plan
needs horizontal bounds, fill depth/foundation policy, permitted excavation and
a finite clearance envelope. If a uniform whole-block surface cannot match a
fractional reference height, show the concrete elevation choice rather than
silently rounding. Preserve or explicitly list changes to buildings, containers,
crops, liquids and mechanisms inside the selected footprint.

For replacing a floor, ordinary work should be staged so it retains a safe
standing surface and escape path. Native attachment, gravity, fluid and redstone
dependencies matter; vanilla planks do not need invented real-world structural
load calculations. Nearby furnishings must not be silently removed to simplify
replacement. A plan can recommend moving them, but any such capability and scope
must actually exist and be included in the evaluated operation.

Native stripping is a distinct action family, with a preview of exact old/new
state and nominal/actual tool wear. Validate the named transformation before use
so an axe action does not unintentionally scrape copper or remove wax. Respect
native offhand blocking intent, reach, aim and server-denial rules. The installed
26.2 `AxeItem` preserves the original axis for its built-in stripping conversion
and invokes normal tool damage; source inspection does not prove Agent execution.

### Rails require surveying and functional checks

A railway is a connected path whose shape is affected by adjacent rails; it is
not just a line of block IDs. Ordinary bends and ascending sections, powered-rail
shape restrictions, rigid support surfaces, neighbouring junctions, power state
and minecart clearance must be evaluated in the installed version. Recheck local
connections after neighbour updates and at section joins. Do not advertise a
fixed universal powered-rail interval without testing the intended slope, cart,
load, rules and travel direction.

The selected product outcome is explicit: connected track, powered one-way
transport, or two-way service. Materials and acceptance differ. The model may
choose a suitable evaluated outcome from context and communicate it briefly;
unclear user intent is not permission to hide the cost of an elaborate station,
signal network or extra excavation. A real journey requires an actual available
minecart and supported use/ride tools, not a test-spawned vehicle attributed to
Agent gameplay.

Home-to-village distances may exceed the existing loaded 96-block perception and
navigation coverage. Survey by normal travel and persist verified corridor
segments. Treat an unseen continuation as provisional; do not claim a complete
route, exact total cost or confirmed distant village from a single local scan.
The chosen corridor/destination and total budget remain fixed across segments;
local repair inside that policy may continue without a model turn for every rail,
while scope/risk/resource changes return a fresh choice. Cross-dimension endpoints
need separate transport planning and are not one continuous ordinary rail line.

### Additional acceptance cases

- Two nearby or connected frames, an open frame and a hollow 3D frame: select the
  intended face/volume and retain unrelated cells. Lost scan coverage is explicit.
- A player moves or turns after referring to a height/frame: the accepted
  selection stays fixed. An unresolved ambiguous reference starts no mutation.
- A second-floor addition retains a furnished first floor and a working exit.
- Surface replacement on an irregular footprint and a bounded cut/fill site
  retain supports and handle unknown caves/fluids without unlimited expansion.
- Stripping across axis orientations preserves identity/orientation and counts;
  already-stripped logs, boards and nearby trees remain unchanged. Test offhand
  interference and cancellation between tool-use actions.
- Short tracks with curves, rises, powered segments and pre-existing junctions
  retain verified connections after updates. Track placement and actual rides
  have separate, explicitly named outcomes.
- Player edits during construction, repeated commands, interruptions and eventual
  restart/resume reconcile the live world and do not duplicate completed work.

## Sources and applicability

The installed Forge 65.0.9 Minecraft 26.2 sources are the authority for exact APIs:
`Player.DEFAULT_BLOCK_INTERACTION_RANGE`, `Player.blockInteractionRange`,
`Inventory`, `InventoryMenu`, `BlockPlaceContext`, `BlockItem.place` and
`ServerPlayerGameMode.useItemOn`. They were read from the local injected source
archive; source inspection is not physical acceptance.
For section 11, the same source archive's `AxeItem`, `BaseRailBlock`, `RailBlock`
and `PoweredRailBlock` were inspected for native transformations, supports and
rail shape/update rules.

- [Forge block states](https://docs.minecraftforge.net/en/latest/blocks/states/):
  state properties and placement-context-dependent state selection. The installed
  version's implementation governs exact behavior.
- [Mojang's door overview](https://www.minecraft.net/en-us/article/taking-inventory--door):
  door support and placement direction/hinge considerations.
- [Mojang's minecart overview](https://www.minecraft.net/en-us/article/taking-inventory--minecart):
  propulsion and slope considerations motivate distinguishing a placed track
  from a verified transport service; installed-version rules govern exact tests.
- [Mineflayer public API](https://github.com/PrismarineJS/mineflayer/blob/master/docs/api.md):
  reference-block/face placement and hand/offhand abstraction are useful interface
  precedents. MinePilot is not being migrated to Mineflayer.
- [Sponge schematic v3 specification](https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-3.md):
  palettes, relative offsets and explicit block state data inform the blueprint
  representation. Importing a final layout would not authorize world-edit pasting.
- [Collaborative Dialogue in Minecraft, ACL 2019](https://aclanthology.org/P19-1537/)
  and [Microsoft IGLU datasets](https://github.com/microsoft/iglu-datasets):
  relevant research settings for language-grounded building. They do not prove
  this mod can perform survival construction or provide a reliability guarantee.

The blueprint/executor separation, compact model events and obstacle protocol
above are this project's proposed engineering design, not claims that these
sources already implement the requested Forge companion.
