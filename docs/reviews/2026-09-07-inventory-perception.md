# Inventory, orientation, perception and declared route materials

Date: 2026-09-07. Development checkpoint, not a companion release or parkour acceptance.

## Resulting behavior

The external Codex controller now receives world time/weather, bounded nearby
entity and item summaries, real acquisition events and the current inventory.
It can turn without requesting translation; inspect filtered/paginated sensors;
classify item identities from 0 through 5 with notes; save dimension-scoped named
coordinates; and navigate to an observed dropped-stack UUID or remembered point.
The design contract is `docs/INVENTORY_PERCEPTION_SPEC.md`.

Every route includes `supportMaterials`: exact item/component identity, item ID,
count and importance. Counts sum to `supportBlocksRequired`. Zero-cost routes
return an empty list. Unclassified items default to protected 2. Grades 0–2 are
excluded both during planning and immediately before execution. Adventure and
spectator inventories do not advertise unrestricted support placement. The
executor checks remaining quantities in usable hotbar slots, pauses on missing
or newly protected stock and never substitutes an undeclared material.

Acquisition records use the actual post-pickup stack count and source entity;
uninstrumented direct inventory increases remain unknown. Entity-origin marks
cover player tosses and living death drops, with actor information where known.
Vanilla item merges do not establish complete per-item lineage. Responses expose
that uncertainty rather than attributing an entire merged stack to one giver.
Policies survive slot moves and memory reload. A failed disk save disables all
support consumption until memory is recovered, including when the failed save
was an attempt to protect previously expendable stock.

Sensors separate privileged proximity from custom line-of-sight observations.
Default summaries are small; detailed queries return at most 64 records per
page. Block scanning proceeds in expanding shells, at most 4096 cells and an
approximately 5 ms loop budget per call. Results disclose loaded coverage,
truncation, timestamps and non-atomic multi-page scans. The custom transparency
policy does not alter vanilla collision physics. World memory is bounded,
world-scoped JSON written with atomic replacement; invalid files are preserved.

The persistent host remains available after model turns and gameplay outcomes.
Acquisitions are queued, with optional speech followed by idle classification.
Exact short jump/step/heading commands use a local public-tool fast path; other
language uses Luna. A running old JAR missing required tools reports a version
mismatch rather than pretending that a running world is unavailable.

## Failures found through independent testing

The first dropped-item trial overlapped a failed Java compilation with a running
development server and returned an operation error. It is not accepted evidence.
Never compile into classes used by a running development server. The failure
record is retained in `run-luna-drop-20260907`.

A fresh trial in `run-luna-drop-final-20260907/results.json` failed: Luna omitted
the target-name parameter, remained at its original coordinates for 100 seconds,
and had an empty inventory. The host now checks named navigation arguments
before any mutating tool, retains the original request, and permits one bounded
correction. Schema/prompt text explicitly assigns the observed dropped entity
UUID to `target_name`; `request_id` identifies an existing navigation request.
Physical failures and disappeared entities do not enter this argument retry.

The subsequent `results-retry.json` also failed: acknowledgement but no movement
or pickup within 100 seconds. A separate diagnostic chat reported exhausted
route searches on the five-block flat approach. Code review found that the
planner extrapolated entity velocity ten ticks into the future, including
residual gravity/bounce velocity. This can put a grounded target underground.
Planning now uses observed coordinates; live target re-resolution still tracks
movement. A regression test proves negative vertical velocity cannot shift the
goal underground. FAILED/APPROACHED model events now require a visible outcome,
so the host cannot silently accept a wait after an unfulfilled acknowledgement.
These are general changes, not hardcoded apple positions.

The final navigation regression then exposed a boundary-stop problem: a route
ended exactly on a player target's acceptance radius, but normal braking stopped
the body slightly outside. Planned cell endpoints now leave 0.05 blocks of
interior margin; actual arrival still uses the original strict radius. The
failing run is retained in `run-navigation-knowledge-final-20260907`. All 12
physical scenarios passed after the change. An initially malformed new unit-test
fixture used an exact start outside its declared start cell; correcting that
fixture restored the full 19-test Java suite. No production check was weakened.

## Verification and attribution

Java: 19 JUnit tests, zero failures/errors. Python: 31 tests, all passed, including
listener lifetime, chat interruption, query/acquisition behavior, bounded argument
repair, public failure reporting and client security. Skill validation and
`git diff --check` passed.

The final real Forge 65.0.9 knowledge gate is source-informed fixture testing,
with gameplay actions dispatched through the public MCP tool service. It proves:

- Turn to 90 degrees with unchanged physical coordinates.
- Actual pickup of five cobblestone, post-pickup count/source-entity correlation,
  protection across a slot move and memory reload, and waypoint persistence.
- Protected/adventure support exclusion and exact route material accounting.
- Opaque stone versus custom-transparent glass; occluded local villager sensing
  and age/profession metadata; bounded query output and basic world clock.
- Disappearing dropped target fails without pretending arrival or retargeting.
- A one-block support route declares one cobblestone. Protection after planning
  pauses execution with zero spent; reclassification/replanning physically places
  one cobblestone, reduces inventory from five to four and reaches the endpoint.
- Normal-gravity dropped apples are approached physically and three apples enter
  inventory, while the remaining four cobblestone are preserved in adventure mode.
- A forced storage-write failure leaves carried materials protected.

Final knowledge evidence: `run-knowledge-release-final-20260907`, build log
`/tmp/mp-knowledge-release-final.log`. The twelve navigation scenarios are in
`run-navigation-release-final-20260907` with log `/tmp/mp-navigation-release-final.log`. The earlier gravity fixture and log are
`run-knowledge-gravity-20260907` and `/tmp/mp-knowledge-gravity.log`.
The source-informed provenance fixture supplies an origin mark before actual
pickup; it does not establish an independent real-human toss lineage test.

The independent Luna listener used only supplied public observations and chat,
with no tools for source, logs, shell, filesystem or world mutation. The parent
observed public physical/chat results and did not read private model decisions.
Fixture setup used an isolated real dedicated server, adventure mode and an empty
initial inventory. Items were supplied as test fixtures before their respective
acquisition objectives; there was no teleport or inventory/world mutation to
make the Agent complete those objectives.

Controller identity: this was a Skill-owned persistent Luna session through the
local Codex app-server, not a subagent created by the parent's delegation tool.
The user asked explicitly about that distinction, and it was clarified. These
results must not be relabelled as a spawned-subagent test.

Measured in `run-luna-knowledge-20260907/results.json`:

| Scenario | Observed result | Time |
| --- | --- | --- |
| Exact short jump | Actual rise 1.252 blocks | First motion 0.419 s; local fast path |
| Free-language turn east | Heading 90, unchanged coordinates | 7.921 s; Luna |
| Acquisition and idle review | Five cobblestone, grade 2 and a note | 17.192 s; Luna |
| Protect and annotate | Grade 0, requested note, five retained | 7.330 s; Luna |
| Remember home | Actual dimension/coordinates/name/note saved | 5.586 s; Luna |

The weather/sense answer was visible, but its exact natural-world animal census
was not independently audited; it is not counted as sensor correctness proof.
These measurements preceded the last metadata/pagination refinements.

The final independent single-request pickup passed in
`run-luna-drop-gravity-20260907/results-retry.json`. The player request was simply
to find nearby apples and walk over to collect them. Luna chose the target;
the persistent host executed its public protocol and could locally select a
unique safe, zero-material route. This is model-controlled normal-play evidence,
not proof that Luna itself compared multiple routes.

- Start: overworld `(0.5, -60.0, 0.5)`, empty inventory/hotbar.
- Fixture: three apples near `(5.5, -60.0, 0.5)`.
- First physical movement: 14.682 seconds after chat.
- Physical pickup: 15.530 seconds after chat.
- Observed pickup position: `(4.252062922471642, -60.0, 0.4996838011515553)`.
- Displacement: 3.752 blocks; inventory changed from empty to three apples.
- Subsequent visible chat confirmed three apples and ended the disappeared-target
  request. Pickup before the exact target coordinate is normal collision pickup;
  this is inventory-verified collection, not a claim of reaching a 0.5-block radius.

The Luna run used JAR-equivalent sources with hash
`54f42564c5185a9d576bafbc5807a0f3ba3cc64376c096ffb19c6cccb21ca267`.
The subsequent storage-failure guard and interior arrival margin were verified
by physical gates, not by another Luna run. Do not conflate these attributions.

## Limits and artifact

The 14.7-second model response remains too slow for a humanlike companion.
The local short-command path does not establish equivalent general-language
latency. These bounded successes do not establish a 100% success rate.

Sensors cover loaded space up to 96 blocks. Structure/tree queries identify
observable markers/logs, not complete villages, fortresses, strongholds, temples
or whole trees. Larger exploration, comprehensive decoration/entity sensing,
all mod variants, exhaustive natural-world occlusion/performance and sustained
moving-target pursuit still need separate work. Entity scans cap candidates at
2048; the block loop budget is not a benchmark for worst-case server tick cost.

Mining/container-destruction/system-grant origin instrumentation and complete
merged-stack lineage are not implemented; unknown remains unknown. Item loss
and full-inventory notifications, equipment, eating, dropping, mining, crafting,
containers, building and redstone production are not accepted in this scope.
The new gameplay tools are integrated with the external Codex listener; parity
with the internal provider controller has not been verified. The supplied
parkour, general 3D terrain and real XMCL client acceptance remain unaccepted.

Final artifact: `build/libs/mcai_companion-0.2.0-dev-mc26.2.jar`.
SHA-256: `e1701b591a0831a993043183cb6ff26f9d6c08d7a0fde30365d861402c41b4f5`.
The installed XMCL JAR and original saves were not changed. Bounded Luna test
servers/listeners were stopped and saved. Preserve the user's stopped offline
server `run-parkour-diagnostic-06` on game 25569/MCP 25769 and its OP configuration.
No GitHub operation or commit was performed.
