# Movement and latency continuation

This is an incremental development checkpoint, not full parkour or companion acceptance.
The original world and the earlier user server/OP state were preserved. No commit
or GitHub action was performed.

## Changes

- Continuous player/entity following retains one request in FOLLOWING, resumes
  on target movement with hysteresis, and fails explicitly on target loss. Safe,
  unambiguous local repairs need no extra model turn; risky/ambiguous choices do.
- Arrival verification no longer overwrites the route's moving-target baseline.
  That bug prevented drift detection and could keep steering at an old endpoint.
- Collision snapshots now retain vanilla shape boxes and actual standing height.
  Planner and live executor accept a supported partial block surface instead of
  requiring an entire solid block. Unknown/unloaded space remains unavailable.
- `replace_request_id` changes an observed active goal in one model decision.
  Validation happens before stopping the old goal; stale IDs and missing named
  targets preserve it. Replacement still requires its own visible acknowledgement.
- Stop decisions no longer replay the same player request and issue another reply.
- The persistent listener polls chat/inventory/navigation atomically at 0.2 seconds,
  cancels old model work asynchronously, rejects stale completions and exposes
  content-free timing metadata. Actual delivered AI chat is now publicly readable.
- The compact decision schema retains typed pace and target-kind enums. A first
  four-field attempt omitted these constraints and FAILED live with invalid speed
  and zero displacement. That failure is preserved, not counted as improvement.
- Shared KnowledgeTools schemas/execution and NavigationTargetCodec serve the
  Codex bridge and internal model. Internal tools now include perception, inventory
  queries/notes, waypoints and turning; routes expose exact supportMaterials.
  New chat cancels an obsolete internal model request, with generation checks and
  cancellation propagated to HTTP. This is implemented; live provider behavior is
  still unaccepted without a working service configuration.
- Vision uses each ray cell's collision context. Bars/barrels/shulker boxes are
  covered; entity reports separate body heading and head heading, and zombie
  villagers expose profession/variant data. This does not establish exhaustive
  variant/facing or natural-world performance acceptance.

## Evidence and failures

Source-informed real Forge 65.0.9 gates:

- Thirteen navigation scenarios passed, including the existing twelve regressions
  and walking from a full surface onto bottom slabs at their actual half height.
  Unit coverage additionally rejects a low ceiling over a slab route.
- Continuous follow/replacement gate passed: same-target reacquisition, chat during
  physical motion, cancelled follow staying stopped, lost target failure, stale and
  invalid replacement protection, and the new acknowledgement barrier.
- The knowledge gate passed after testing stone/redstone-block occlusion and
  glass, leaves, doors, bars, chest/barrel/shulker box, enchanting table, slabs,
  water, lamp, copper bulb, sculk sensor, crafter and piston pass-through.
  A first expanded fixture produced incidental drops that filled an unfiltered
  page; fixture replacements now avoid neighbor updates and the villager query
  explicitly filters the entity being asserted. Its failure log is preserved.
- Twenty-five JUnit tests and forty-six Python tests passed. The new loopback HTTP
  model-interface test uses synthetic replies: it checks real request serialization
  and parsing/shared schemas, not a live provider or intelligent gameplay.

Independent Luna tests on fresh copies of the supplied parkour world:

1. `run-parkour-independent-20260907/physical-results.json`: FAILED. Initial
   route options 0, four reported navigation requests, final body approximately
   (92.40145,-59,-36.46683), empty inventory. No sampled upward displacement.
2. `run-parkour-shapes-independent-20260907/physical-results.json`: FAILED after
   the bounded trial. Initial options 0, four reported requests, final body
   (92.44973,-59,-36.46914), empty inventory. Highest sampled Y -58.8 does not
   prove a successful parkour jump. Required goal was (93.5,-48,-36.5), radius .5.

Both used actual Minecraft dedicated servers, adventure mode, empty inventories
and only an initial administrator teleport. Fresh spawned gpt-5.6-luna operators
used the public Skill, not source, map files or server logs. The parent observed
only allowed body coordinates/dimension/inventory and public chat. Operator prose
and approximate operator time estimates are not physical evidence. The server
observer's timestamps are the timing source. Both test servers were saved/stopped.

Ordinary-language persistent Luna trials are separate from spawned-agent tests.
`run-latency-follow-live-20260907/latency-results.json` retains six cases. An initial
10-block request replied in 11.35 seconds and physically stopped about 1.06 blocks
from the goal, within its radius 2. A later sneak request replied in 4.95 seconds;
chat during movement replied in 6.06 seconds without stopping motion. Stop took
3.34 seconds and initially repeated a reply; that host branch was then fixed.
These tiny cold/warm samples are not p95 measurements or proof of low latency.
The observed model first-text delay dominates host setup in the measured turns.
A later 80-block request honestly failed the bounded local planner without moving.
Final replacement/stop trial results are retained in `run-latency-final-20260907`.

## Still open

The supplied parkour, broader three-dimensional traversal and natural-terrain
performance are NOT accepted. Sub-cell horizontal routes, takeoff/landing choices,
low ceilings and map-specific obstructions need further general mechanism work.
Do not special-case the supplied coordinates or replace failed evidence.

Mining/broken-container/system-grant provenance and complete merged lineage remain
unimplemented. The pinned Forge sources have no general merge event; neighboring
spawn timing cannot establish causal ownership. Research checked the actual Forge
26.2 source and [Mixin callback semantics](https://github.com/SpongePowered/Mixin/wiki/Advanced-Mixin-Usage---Callback-Injectors),
but no bytecode hook was introduced in this checkpoint. Existing uncertain lineage
remains explicitly uncertain.

Search is still bounded to loaded space up to 96 blocks, and structures are observed
markers. There is no complete village/stronghold recognition, frontier exploration,
global corridor or hidden locate. Broader senses must respect visibility/coverage;
increasing a radius alone would not implement exploration.

No valid MiMo/internal-provider test has been run. The pending user question asks
only which provider/model to configure, never for a credential in chat. Newer real
XMCL client/world entry and long-session acceptance also remain outstanding.

## Final installation and stop check

The final ordinary-language replacement case acknowledged once in 6.412 seconds,
then physically stopped at (15.78634,-60,6.50001), about 1.28634 blocks from its
new target (14.5,-60,6.5), within radius 2. The old destination was still active
when the new player chat was received. No intermediate cancellation reply occurred.
The final stop request replied once in 4.659 seconds and stayed within .01 blocks
for the last ten seconds of observation. Inventory stayed empty. The body travelled
about 5.6 additional blocks during the model delay: conversational stop latency
is still unacceptable for an immediate stop requirement, despite duplicate-speech
and stale-turn fixes. Exact short local commands have a separate path and are not
used to claim ordinary-language latency improvement.

The final cleanup also uses Java 25 HttpClient.shutdownNow() when closing an
internal controller, avoiding a lingering provider connection and blocking close.
This last lifecycle-only change passed the JUnit/local HTTP build check after the
physical runs; it was not separately exercised with a real provider.

The newly built JAR was installed in the XMCL instance with matching SHA-256:
`910e6a84cbdc82e66433d89a3f180727e962661d76a9b2d3709d5c66bd05ca17`.
The original installed artifact and the intermediate build were preserved in
mods/.minepilot-backup. Exact paths/hashes are in
[the installation record](2026-09-07-movement-installation.json).
The launcher previously reported Forge 65.0.8 despite the directory name 65.0.9;
these backend tests used 65.0.9. Client/world entry has not been reverified.
All temporary controllers, independent operators, observers and test servers from
this checkpoint were stopped/saved. Original worlds and the old user-server/OP
configuration remain preserved. No Git staging, commit or GitHub operation occurred.
