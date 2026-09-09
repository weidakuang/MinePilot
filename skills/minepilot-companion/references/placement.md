# Placement and hands

These are public game tools, called with `minepilot.py tool --name NAME --arguments
'JSON'` or by the persistent model controller. All block changes use the real
survival player's inventory and native item-use/break actions. No world edits,
item creation or physical clicks through walls are exposed.

- `set_hand {item|entry_id|slot, hand:"main"|"offhand"}` selects a real storage
  slot (0..35) or offhand (40). Distinct components require an exact identity.
  `item:"minecraft:air"` needs an empty slot. Items are swapped, never discarded.
- `inventory_capacity {item|entry_id|slot}` counts exact compatible stack capacity
  and empty storage slots; armor and offhand are excluded. Capacities for
  different items share the same empty slots and must not be summed.
- `place_block {item|entry_id|slot,x,y,z,state?,hand?,jump?}` starts one placement
  within five blocks of the body, also subject to the vanilla eye interaction
  range and real unobstructed face. The body turns before using the item.
- `plan_placement` previews a batch. Supply **one** of `targets`, `region` or
  `blueprint`. At most 256 item-use anchors; material count is per action, so a
  door or bed takes one item and produces two native cells. Specify door lower
  or bed foot only; leave its companion coordinate unspecified.
- `choose_placement {request_id,option_id}` approves the returned material policy
  once. Local execution continues without a model decision for each block.
- `placement_status {}` reports actual position, recent material receipts and
  phase. `COMPLETED` verifies desired required states; `PARTIAL` and `BLOCKED`
  are not success. `inspect_placement {targets:[{x,y,z},...]}` reads 1..64 exact
  sensed current block states, including air, for external verification.
- `pause_placement`, `resume_placement`, `cancel_placement` take `request_id`.
  They stop the job's child movement/mining too. Chat remains available.
  Paused placement permits changing hands; resuming re-equips reserved items.
- On blockage, inspect `reason`, `decisions` and `decisionId`. Use
  `resolve_placement {request_id,decision_id,option_id}` with returned values.
  A suitable real inventory tool is selected before previewing native excavation;
  its actual wear/remaining durability is reported. Excavation is offered only when evaluated. Unknown travel
  or time is reported as unknown. Retry does not silently authorize clearing a
  changed target. Skip produces `PARTIAL`. Cancellation preserves actual changes.

A target's `state` is a native property subset with string values, for example
`{"axis":"x"}`, `{"type":"top"}` or `{"facing":"east"}`. These constrain an
ordinary item use, not arbitrary property mutation. Impossible angles or states
are reported as blocked. Wall torches use the torch item and native wall state.
Underfoot placement requires `jump:true`, stable takeoff, headroom, an actual
placement window and observed landing. No position is set directly.

For batches, `allow_movement:true` permits evaluated local approaches within 24
blocks of the fixed origin; `movement_budget` defaults to 64 and is capped at
256. Only currently sensed target cells can be planned. Current approach routes
require no support blocks or predicted health loss. Failure returns a decision.

A simple region is:

```json
{"region":{"from":{"x":1,"y":64,"z":1},"to":{"x":3,"y":64,"z":1},"item":"minecraft:cobblestone"},"allow_movement":true}
```

A compact text blueprint is:

```json
{"blueprint":{"origin":{"x":1,"y":64,"z":1},"palette":{"P":{"item":"minecraft:oak_planks"},"G":{"item":"minecraft:glass"}},"layers":[["PPP"],["P_G"],["..."]]},"allow_movement":true}
```

Replace illustrative coordinates with a sensed work area. Layers increase y,
rows increase z, characters increase x. `.` preserves existing contents and `_`
requires air at planning and completion; it does not authorize excavation. The
layout is rectangular, at most 4096 cells and 256 placement actions. A useful
order is foundation, walls/openings, roof, then fittings, with escape/access
preserved. This compact format is distinct from the larger design-only house
example in the repository; do not submit its unimplemented metadata as arguments.

For explicitly planned auxiliary blocks, set `temporary:true`; only stable full
cubes graded 3..5 are accepted. Grades 0..2 remain protected for support use.
`cleanup_temporary` defaults to true for batches. Cleanup uses normal mining and
may stop for unsafe reach, attachments, falling blocks or changed world state.
A skipped cleanup is partial, not restored terrain. Support items are not refunded
synthetically. Navigation shares placement geometry and verifies its exact
approved support material debit; it can now select storage slots too.

This foundation does not yet identify a house/frame automatically, generate a
complete house from a vague instruction, strip placed logs, orchestrate a
multi-storey renovation, restore arbitrary scaffolds or verify functioning rail
networks. Do not claim those scenarios from a small placement test. Return a
concise limitation or use only an explicitly bounded supported plan.
