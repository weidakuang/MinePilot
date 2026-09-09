# Movement, search, collection and body repairs — 2026-09-09

## Report and implementation

The user reported excessive downward gaze, follow ending after arrival, pauses
on small target movements, slow searches, near-start route failures in a forest,
failure to mine stone with an inventory pickaxe, one-log false whole-tree
completion, and a body that did not react to player knockback.

- Ordinary walking now aims at eye-height route points, with gradual grade
  adjustment limited to -25..15 degrees. Mining, doors and placement retain
  their precise interaction aim. Intermediate walking cells no longer cause
  final-arrival braking.
- Continuous follow remains active while its target stands still. A safe level
  corridor can be retargeted while preserving the active movement. Other target
  changes retain a valid path prefix before repair; player, entity and dropped
  item targets share this mechanism. Temporary no-route results keep follow
  waiting and retrying, without repeatedly announcing the same failure. Explicit
  cancellation stops it; missing targets and dimension changes report a reason.
- Repair selection now filters for the approved pace before comparing options.
  Previously a cheapest route incompatible with that pace could stall at a
  ready plan. Initial failed requests still require an actual approved option.
- The planner prefers a bounded ordinary-ground search before costly gap
  alternatives, caches occupancy only within each search, and has a 600 ms
  background budget instead of 150 ms. The main navigation snapshot is captured
  in approximately 2 ms read slices. Search runs on a bounded worker using
  immutable snapshots. This does not pin a physical core or allow asynchronous
  native world reads; final snapshot packaging and some internal placement /
  collection captures remain on the server thread.
- Block searches skip loaded sections whose native palettes cannot match the
  requested block. Each page bounds work; the host skips empty pages locally
  instead of spending another model turn per empty page. A village search is
  still a bell/structure clue, not proof of a complete village.
- Normal travel now prefers the planner's AUTO pace, allowing evaluated sprint
  routes. This is not new unconditional sprint-jumping across outdoor terrain.
- Mining and collection preview compatible tools across the real inventory,
  show their slot and durability, and equip the approved tool through native
  hand swaps. Stone collection defaults to cobblestone as its ordinary output.
- Whole-tree collection no longer truncates a recognized connected component
  to a quantity of one. It requires every approved log plus observed pickup
  receipts before `wholeTreeVerified`. Logs beyond the fixed survey radius are
  reported as incomplete. Normal item ejection and falling high-log drops get a
  bounded pickup allowance. Tree/farm/construction heuristics are retained;
  arbitrary placed logs are not proof of a natural tree.
- The persistent controller retains the player's original goal across an
  approach subtask. Navigation completion permits grounded follow-on tools for
  that goal; failure does not grant a blind retry. New direct movement and stop
  instructions cannot replay an old collection goal.
- The embedded connection now consumes the body's native velocity packet once.
  Vanilla player attacks can send that impulse and restore the old server
  velocity while expecting a client to apply it; the headless body previously
  missed that client-side step.
- A controlled restart exposed an older independent bug: headless login called
  `placeNewPlayer` without the 26.2 `PrepareSpawnTask` loading stage. Login now
  reads native player data, prepares saved-dimension chunks, restores the body,
  invokes the Forge loading event and restores parent vehicles / pearls through
  the native APIs. Only a new player defaults to survival. No inventory is
  reconstructed through commands.
- Final listener monitoring found `RemoteDisconnected` escaping the client and
  stopping its host. The local macOS HTTP proxy was intercepting loopback:
  initialization failed through that proxy and succeeded with a direct opener.
  The loopback client now bypasses proxies and wraps interrupted/partial HTTP
  responses as recoverable connection failures. It does not replay a POST with
  an uncertain result; the host reconnects and reads current state.

## Validation and limits

All physical gates ran actual Forge 65.0.9 Minecraft servers. Deterministic
fixtures are source-informed implementation regressions, distinct from the
independent Luna public-chat trial below.

- 29 JUnit tests and 69 Python client/controller tests passed. Build and diff
  checks passed. Native mining, placement, sound and knowledge gates passed.
- Fourteen navigation scenarios passed, including the new forest fixture:
  approximately 29.94 blocks displacement, 33.94 blocks traveled, 155 execution
  ticks. Existing door, gap, ladder, slab and moving-target checks were retained.
  The test now uses a wall-clock planning deadline because GameTest ticks run
  faster than real time; its 700-tick execution bound remains unchanged.
- Final follow regression after the persistence fix resumed within 24 ticks
  after the target moved again, under the same request. Earlier repaired runs
  resumed in 15–20 ticks. It also verified chat during movement, persistent
  idle follow, explicit cancellation and lost-target reporting. Native player
  attack displaced the body 1.932713 blocks.
- Collection fixtures verified a three-log whole tree despite requested count
  one, a four-log branched tree, a six-log spruce, storage-slot pickaxe use on
  two stone blocks with two actual durability points, pickup, interruption,
  disappearing drops and farm safeguards.
- Sparse 96-block loaded-region marker search took two pages / 4096 examined
  cells and approximately 6.08 ms in its fixture. This is not a natural-world
  latency bound or a guarantee that every structure can be identified.
- Native persistence regression saved, closed and rejoined a real body with
  deliberately wrong fallback coordinates/dimension. Saved position, rotation,
  mode, health, hunger, inventory, offhand and durability survived. Ten physical
  ticks checked position/inventory stability afterward.
- A real local HTTP regression configured an unusable proxy, closed the first
  response and accepted the next request. The direct client surfaced the first
  as a connection failure without replaying it, then connected successfully.
- After the proxy fix, 27 authenticated observations over 130.25 seconds kept
  the same idle Luna listener alive and ready. The live server advanced at
  approximately 20 ticks/second; the slowest observed MCP call was 9.8 ms. This
  measures idle connection health, not model response or active-search latency.
  See `2026-09-09-listener-soak.json`.

Logs are retained in `/tmp/minepilot-{forest-repair2,collection-final,
follow-persistence,knowledge-final,sound-final,mining-repair,placement-repair,
persistence-final}.log`. Earlier failed runs remain available; later success
does not erase their diagnosis. Public evidence is saved alongside this review.

## Independent normal-rate Luna trial

`run-repair-luna-20260909` ran a normal-rate production Forge 65.0.9 server with a
six-log spruce fixture. Setup completed before the request; the operator did not
edit the world/body while Luna worked and inspected public observations, chat
and terminal gameplay evidence, not private model reasoning.

The first trial failed: Luna walked to an invented approach point and stopped
because the completion schema had lost the original goal. That failure is
retained. After the controller correction, the same request completed:

- First visible Chinese response: 7.117 seconds.
- Observed physical collection completion: 37.398 seconds.
- Final inventory: six spruce logs and a wooden axe with six damage points.
- Six approved blocks broken, six matching inventory receipts,
  `remainingApprovedBlocks=0`, `wholeTreeVerified=true`.

See `2026-09-09-luna-collection.json`. This trial used JAR
`28e348623112d5f18d4f794236ccac9c278a27ff27f95fd430e43d3345971b1e`;
the later native-login correction is verified separately. It is one observed
latency sample, not a 30-second or universal tree-completion guarantee.

## Installation and continuing play

Final JAR SHA-256:
`3aaa4d2d53ffbb0b35653cbcf4798b3564fe5bb2cda09e64ca1d76242418b228`.
Repository, XMCL client and production user-server copies match. Previous JARs
and the complete pre-restart world archive are preserved.

The same `peaceful-world` is running at `127.0.0.1:25565`, survival/peaceful,
offline authentication, with the user's OP preserved. The incorrect empty
login was stopped; only the Agent's two player-data files were restored from
the pre-restart archive. Public observation confirms exactly the original
position and inventory (two spruce logs, one wooden pickaxe, three leaf litter).
See `2026-09-09-repair-installation.json` for exact paths and coordinates.

Luna remains LISTENING with its model connection ready. The existing Minecraft
client must restart to load the new JAR. Server Forge is 65.0.9; the last-known
XMCL configured runtime is 65.0.8 despite its folder name. This remains visible
and was not silently changed. Fresh real-client entry is not claimed.

Arbitrary three-dimensional natural terrain, the original user parkour map,
giant/mixed trees and universal ownership/farm recognition remain unaccepted.
The dirty working tree and earlier placement work were preserved; no commit,
push, old-world reset or retired implementation restoration was performed.
