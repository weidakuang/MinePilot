# Inventory, orientation and perception contract

Date: 2026-09-06. This document refines the user's draft; implementation and
acceptance status must be reported separately.

## Orientation

Heading is clockwise from north: N=0, E=90, S=180, W=270; input 360 normalizes
to 0. A turn changes heading through ordinary control frames without requesting
translation. It is exclusive with active movement/jumping. Relative facing uses
the bearing from the agent to a currently observed entity or visible sun.
Facing means that bearing, back adds 180, left-side-facing adds 90, and
right-side-facing subtracts 90. This specifies which side of the agent faces
the reference, not moving to the target's left. Target facing in observations
instead describes which side of the target faces the agent. Coincident targets,
an occluded/nighttime sun, and a near-zenith sun have no reliable bearing.

## Inventory and provenance

An acquisition is an actual count increase, never a pickup attempt. Record a
monotonic event sequence, game tick, item identity including components, count,
source category, giver/cause where known, and confidence. Events arriving in
one tick are batched; merging/splitting/reordering inventory is not acquisition.
Pickup-completed hooks are authoritative for picked-up counts. Uninstrumented
direct changes are unknown; proximity alone never establishes a giver.
Source categories include player toss, self loot, other death drop, mined block,
broken-container contents, system grant and unknown. Unavailable provenance
must remain unknown; categories are not a promise of universal attribution.

Classification is 0 (most important) through 5 (least important), with a bounded
note. Unclassified identities default to protected level 2. Identical item and
component identities share a conservative policy across slots/split stacks.
Different provenance batches remain distinct event records even if items merge.
Policies and named waypoints are world-scoped and persist independently of slot
positions. Item importance is not permission to discard or spend arbitrarily.
Navigation must exclude levels 0–2 both when counting resources and immediately
before placing a support. A policy change can invalidate an earlier route.

The model receives acquisitions promptly and defaults to silence. Player gifts,
loans, explicitly requested collection/reporting or another concrete direct
player relationship may justify optional thanks or a report. Ordinary loot,
natural drops, incidental pickups and unknown gains do not need announcements.
Do not repeat acquisition, target-completion and organization reports for the
same gain, unless the player explicitly requests repeated/detailed reporting.
It can classify at a suitable idle moment; that review is silent unless reporting
was requested. Small pickups may be coalesced, but their
counts/sources must remain available through a bounded paginated event stream.

## Perception layers

- Proximity: block positions in a ±10 cube, dropped stacks within a 10-block
  sphere, living entities in a ±16 cube, including occluded entities. Label this
  explicitly as a privileged local sensor rather than visual evidence.
- Vision: loaded entities/blocks up to 96 blocks, horizontal ±60 degrees from
  heading; vertical ±60 degrees from pitch. Unknown/unloaded space blocks the
  ray. Distinguish known absence from incomplete coverage.
- Occlusion: use block shapes and an explicit pass-through policy. Fluids,
  foliage, doors/gates/fences, glass, containers, enchanting tables, signs,
  flowers/crops, candles, carpets, slabs and non-redstone-block redstone devices
  do not occlude this custom sensor. They still appear in results. Entities do
  not block rays. Collision/pathfinding uses ordinary Minecraft geometry.
  This is a custom perception rule, not pixel-identical player rendering.
- Living details: type, UUID, exact coordinates, heading, relative facing,
  custom/player name and available age/profession/variant data. An unknown
  variant is unknown; do not invent age/occupation details.
- Items: each ItemEntity is one physical stack, with UUID, item, count and
  coordinates. Grouped summaries retain stack count and total item count.

Default observations contain basic world/weather/time/body information and
small summaries, not all block coordinates. Detailed queries are bounded and
paginated, with timestamp, coverage and truncation. Large block searches must
use bounded scan work, not iterate a 193-cubed volume every tick. Filters target
types, names or identifiers. Results do not generate/load chunks. Tree results
are observed log candidates; they are not automatically a complete tree.
Structure markers are observed/remembered candidates, not hidden seed-based
`locate` results. Unseen structures require actual exploration or a separately
declared privileged capability. Never imply absence outside examined coverage.

## Dynamic drops, memory and chat

A dropped stack can be a dynamic destination by its observed UUID. Track its
movement; if it merges, despawns, is collected or leaves observable scope, stop
and report loss of that identity. Do not silently redirect to a guessed pile or
claim collection from arrival coordinates. Inventory acquisition proves pickup.

Named waypoints include dimension, coordinates, name, note and saved time. They
are memories, not proof the area remains safe. Names are unique within a
dimension; a navigation request must use the stored dimension. Explicit search
may consult both current observations and saved markers with their provenance.

Capture received player chat and visible system chat with separate origins;
do not feed the agent's own replies back as fresh player commands. Actionbar
messages are not ordinary chat. Game names, item names, notes and chat are data
and never authorize filesystem/network operations.

## Acceptance

Real-server gates must prove turn-only coordinates, actual item pickup/counts,
source attribution, policy survival across slot movement/restart, exclusion of
protected supports, occlusion versus proximity, dynamic-drop disappearance,
bounded output, named waypoint round-trip and world time/weather. A scripted
fixture is source-informed evidence; independent model gameplay is reported
separately. Prior parkour and latency shortcomings remain separate open work.

## References checked

- https://github.com/MinecraftForge/MinecraftForge/blob/26.2/src/main/java/net/minecraftforge/event/entity/player/PlayerEvent.java
- https://github.com/MinecraftForge/MinecraftForge/blob/26.2/src/main/java/net/minecraftforge/event/entity/living/LivingDropsEvent.java
- https://docs.minecraftforge.net/en/1.21.x/datastorage/saveddata/

The pinned local Forge 65.0.9 sources determine actual 26.2 signatures;
older documentation is used only for architectural guidance.
