# First mining implementation checkpoint

This implements the first small physical capability from the confirmed mining
design. It does not implement region mining, excavation routes, cave exploration
or branch mining. The previous GitHub backup remains `81dfb6b`; these changes are
local development work, not a new release or a claim of full design completion.

## Implemented behavior

- Block proximity is a 10-block sphere around the body's feet, omnidirectional
  and independent of occlusion. Air is omitted. Outside the sphere, the existing
  directional vision and custom transparency rules apply. A smaller requested
  query radius also bounds proximity; the old cube corners no longer leak in.
- Inventory policy/count identity excludes ordinary durability damage. Wear does
  not create a fake acquisition or discard the tool's note/grade. Per-slot
  equipment observations report actual damage, maximum and remaining durability.
  Existing damage-specific policies migrate conservatively; identical tools share
  policy identity, not a unique persistent physical-tool UUID. Cache sizes remain
  bounded. Full arbitrary component-change identity is still outside this scope.
- `equip_tool` selects an existing hotbar slot through the normal player handler;
  an empty slot selects bare hands. It does not create or move inventory items.
- `plan_mining` previews one sensed block already within physical reach using the
  held tool. It reports current wear, nominal tool-component wear, estimated break
  time, target, harvest eligibility, zero movement and an empty support manifest.
  It neither moves nor breaks. It returns one actual option, not invented A/B/C.
- `choose_mining` approves the exact current plan and starts asynchronous aiming
  and the normal START/STOP block-break lifecycle. Live geometry, tool identity,
  block state, physical support and hazards are checked before actions. Read-only
  access to native break acknowledgement/progress prevents a rejected START or
  an early STOP from becoming a later unintended destruction. The executor never
  writes those native fields or directly sets/removes blocks.
- `pause_mining`, `resume_mining`, `cancel_mining` retain exact request identity.
  Resume revalidates and starts unfinished progress again. An unapproved plan
  cannot be paused/resumed to bypass choice. Chat and item notes remain available;
  turn, jump, held-tool changes and navigation cannot compete for the mining body.
- Exact local stop chat also cancels mining and invalidates obsolete internal
  model decisions. This does not resolve general natural-language model latency.
- Item spawn events inside the actual break call carry a causal mining-request,
  actor, block and dimension origin. Actual pickup events report acquired counts.
  Full merged-stack lineage is still explicitly unknown. A mining completion
  does not claim collection; normal movement can approach the emitted drop UUID.
- MCP, direct Skill commands, the persistent Skill host and internal model schemas
  expose the same mining tools. The Skill host emits one terminal mining event and
  preserves chat preemption. Internal completion events wait behind player input;
  completion speech is optional. No automatic torches are introduced.

## Physical evidence

The real Forge 65.0.9 source-informed gate is `mining_tool_regressions`, selected
with `-Dminepilot.miningTest=true`. It operates through the public MCP dispatcher.
Fixture setup and reset between distinct scenarios are not gameplay actions.

Passed scenarios include wrong-tool rejection without mutation; plan without
mutation; wooden-pickaxe coal mining with exactly one actual damage point; chat
during breaking; pause holding an intact block for 70 ticks; resume; physical coal
pickup through normal public navigation; acquisition linked to the mining request;
no fake pickaxe acquisition; policy persistence after wear/reload; stop near
completion with no delayed break for 80 ticks; denied adventure actions; falling
gravel rejection; stale block approval rejection; bare-hand dirt breaking;
body-owner exclusion; and a Forge-cancelled START remaining intact for 80 ticks.
The expanded sphere fixture also sees a nearby block behind the body while
excluding a behind-body cube corner outside the sphere.

Evidence is retained under these ignored run directories, including
`gametestserver/gametestworld/mining-physical-evidence.json`:

- `run-mining-gate-20260908`
- `run-mining-sphere-gate-20260908`
- `run-mining-native-gate-20260908`
- `run-mining-denial-gate-20260908`

Earlier failures are retained: a transient onGround flag incorrectly blocked a
physically supported body (now uses actual contact); a drop did not enter pickup
range while standing still (test now uses public navigation); and the test chose
a slot already occupied by acquired coal for its bare-hands fixture (corrected to
an empty slot). These failures are not counted as passes. Compilation corrections
included the navigation status accessor and Forge 7's predicate cancellation API.

## Independent Luna trial

A true spawned `gpt-5.6-luna` subagent, Singer, independently used the installed
Skill's direct interface on a disposable real server. It received no source,
world-file access, server logs, expected tool sequence or parent control during
the run. Setup provided survival mode, a wooden pickaxe and one nearby coal ore.
The parent observed only public coordinates/dimension/inventory/hotbar/chat and
did not inspect private model outputs. It requested mining and collecting one
nearby coal through normal server chat.

The inventory changed from one undamaged wooden pickaxe to that pickaxe with
damage 1 plus coal x1. The body remained at (0.5,-60,0.5); the drop happened to be
within pickup range, so this independent run does not prove navigation to a drop.
Actual pickup appeared 37.387 seconds after the chat request; the public result
reply appeared after 48.601 seconds. These are single cold independent-task
measurements, not warm conversation latency or percentile estimates.

Evidence: `run-mining-independent-20260908/physical-results.json` and
`allowed-observations.jsonl`. The operator, observer and server were closed/stopped;
the server saved its world and removed the loopback token. Later native-progress,
approval and denial guards passed source-informed gates; the independent trial
was not repeated after those final guard changes.

## Verification and remaining work

26 JUnit and 50 Python tests passed. The synthetic local HTTP test checks the
shared mining schemas alongside other tool definitions; it is not a real-provider
mining test. Skill validation and diff whitespace checks passed. The real knowledge gate and the existing thirteen-scenario navigation gate also
passed on the final source. Build succeeded; the final artifact was installed
only after confirming no Minecraft Java process was running.

Current limits are material: only a currently reachable single block and the
currently held hotbar tool are planned; movement must be requested separately.
Survival mode is required; supported adventure permissions are not implemented.
Container/block-entity excavation, fluid-adjacent work, falling overhead material
and breaking the body's support are rejected. Durability is a nominal component
estimate, not a guarantee for arbitrary mod overrides. No support placement,
automatic tool alternatives, fixed-radius/cuboid job, large-destruction model
confirmation, return strategy, cave scoring or fishbone mining is implemented.
Broad vision coverage/performance, full provenance, internal-provider acceptance,
original parkour and natural-language responsiveness remain open.

The next increment is interaction-position navigation and bounded access
excavation, followed by fixed-origin material-filtered collection. Do not promote
this foundation into a complete autonomous mining claim.

## Installed development artifact

Final JAR SHA-256: `e11bc0a043aadd98e443613274dba24ef85ebd8f9989fb3cfa7c9725608a8a6d`. The matching XMCL copy
replaces the previous development JAR, preserved under mods/.minepilot-backup.
Exact paths and hashes are in `2026-09-08-mining-installation.json`. Real client
world entry was not repeated; the launcher previously reported Forge 65.0.8 while
all backend gates use 65.0.9. All temporary servers/controllers/observers are
stopped. No user world was modified and no additional commit/push was made.
