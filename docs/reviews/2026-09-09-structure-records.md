# Native structure queries within 96 blocks — 2026-09-09

The user authorized server-data structure lookup, restricted to 96 blocks. The
existing `sense` tool now routes `kind=structures` to native structure records
instead of identifying a bell or another block as a structure candidate.
Other perception modes retain their existing visibility/loaded-space rules.

## Contract

```json
{"kind":"structures","radius":96,"filter":"village","limit":32}
```

The default and maximum radius is 96. The query fixes the body's position and
dimension when it begins and uses a three-dimensional sphere. Filters accept
registered structure IDs, native `#tags`, path substrings, common Chinese aliases
and an empty filter for all types. Unknown filters and radius values above 96
are rejected, not reported as a completed empty search.

Each result identifies its structure type and the closest point in an individual
recorded structure-piece volume. That point must be inside the sphere. A large
structure can be reported when a nearby piece lies in the sphere even if its
start chunk is outside it. The tool does not disclose the outside center,
complete bounding box or distant piece coordinates.

Results are explicitly sourced from `server_structure_record`, not vision.
`safeToStandVerified=false` and `currentBlocksVerified=false` distinguish a
recorded volume point from an entrance, safe navigation target or an intact
present-day building. Records can survive demolition. Player houses, tree farms
and constructed portals are not registered world-generation structures.

While `status=SEARCHING`, continue the same cursor, radius and filter. Completed
results paginate with `nextOffset` and the same cursor. `coverageComplete=true`
and `totalMatched=0` establish only the absence of matching records within the
examined sphere. Missing data, caps and timeouts yield partial coverage.

## Implementation and performance

The 26.2 `/locate` implementation calls `findNearestMapStructure`; its search
radius is based on placement regions, not a block-distance bound. Passing 96
would therefore not implement this contract. `StructurePerception` instead reads
the starts/references of chunks intersecting the bounded sphere, resolves
referenced starts and applies the final 3D piece-distance filter.

Loaded records are read only on the server thread with a soft 2 ms work slice.
At most one new native metadata request is dispatched per server tick, with at
most two pending requests per scan and four retained scans. Native
`ServerChunkCache.getChunkFuture` explicitly marshals off-thread requests to its
main-thread processor; the worker performs dispatch only. No extra access
transformer or unsynchronized world read was added. The requested generation
stage is STRUCTURE_REFERENCES or STRUCTURE_STARTS, not FULL terrain. Native
dependencies may read neighboring metadata outside the sphere; no outside
structure position is returned. A 30-second processing timeout and bounded
reference/piece counts prevent an unbounded query from becoming an absence claim.

Recently completed identical queries are reused for five seconds. The fixed
completion duration (`elapsedMillis`) is separate from snapshot age; late cursor
polls must not inflate reported processing time. The persistent controller can
advance empty pages locally while preserving the native normalized filter and
radius. MCP and the internal model share the same schema and execution path.
Their prompts and Skill documentation now explain the privileged result source.

## Evidence

- 29 JUnit tests passed, including shared internal/MCP schema bounds. The
  internal-model protocol test uses a synthetic endpoint, not a live provider.
- 70 Python tests passed, including normalized structure cursor continuation.
- The real Forge structure gate physically generated two native desert pyramids
  and registered their native fixture starts/references. Public dispatch verified
  near detection, 95/96/97-block boundaries, a referenced start outside the
  sphere, vertical exclusion, a smaller radius, unknown/range rejection and no
  village false positive from a placed bell. Body and inventory remained
  unchanged during queries. The final fixture's maximum tool-call time was
  3.686 ms. GameTest runs accelerated ticks, so its total query durations are
  not normal-play latency measurements.
- The existing knowledge/support/visibility/item gate passed after integration.
- An isolated, normally ticking Forge 65.0.9 world with seed 0 provided a real
  generated village. Native `/locate structure #minecraft:village` found
  `minecraft:village_plains` at X272/Z944, 991 blocks from spawn. The bounded
  spawn query returned no village. After operator-only safe fixture positioning
  before the next query, the new tool returned the actual nearby village with
  complete coverage; native processing was 49 ms and the two-call/poll observation
  took 114.767 ms. Coordinates and inventory were unchanged.
- Persistent Luna received a Chinese request to report a village within 96
  blocks without moving. Its visible game-chat answer matched the independently
  queried structure type, position and distance and described the server-record
  limitation. The body remained at (272.5,68,944.5) with an empty inventory.
  Private model reasoning/actions were not inspected. These are public result
  checks, not a universal model-latency guarantee.

See `2026-09-09-structure-physical-evidence.json`,
`2026-09-09-structure-natural-evidence.json` and
`2026-09-09-structure-luna-evidence.json`. Source-informed generated/indexed
fixtures are distinct from the naturally generated world and Luna chat trial.
The isolated listener/server were stopped and their world/logs preserved.

## Installation

JAR SHA-256:
`ca0bac76b99f52b6acc749e3e05fbd2cc8e7efdd5182070c0b645b9a11ce1b73`.
The repository build, original user server and XMCL client were synchronized
with previous JAR backups. The same peaceful survival world and
`127.0.0.1:25565` address are retained; a complete pre-update world archive was
saved. The client must restart to load the new file. Server Forge is 65.0.9;
the last-known XMCL configuration is 65.0.8. No client-entry claim, Git commit
or push is made. Final liveness/restoration is recorded separately in the
structure installation JSON.

This adds bounded record lookup. It does not prove entrance detection, reaching
every found structure, current structure integrity, player-building recognition
or unrestricted long-distance exploration.
