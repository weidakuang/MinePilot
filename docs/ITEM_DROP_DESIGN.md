# Item dropping proposal

Status: implemented and native-server tested on 2026-09-10. The server recovery
was completed first. `drop_items` and `reclaim_drop` now use the shared public
knowledge-tool surface in both controllers, including inventory event/review
schemas and active gameplay phases. See the repair evidence in
`docs/reviews/2026-09-10-tree-drop-repair.json`.

## Implemented boundary

The tool tosses along the current facing without turning or changing hands.
Recipient-specific aiming is not part of this version; use normal approach/turn
when appropriate, and never mistake a toss for confirmed receipt. Select 1..8
item/count entries. Slot selections also require the observed `entry_id`; tools
can additionally require `expected_damage`. Item-only selection rejects
ambiguous components/durability. Exact native drops can span several stacks.

Reservations retain current held mining/collection stacks, remaining placement
materials, excavation supports/tools/replanting stock, and the approved
navigation support manifest. Navigation retains its conservative original
manifest count until the route ends. Pausing does not release reservations;
cancel the superseded job or drop only its unreserved excess.

A cancelled native toss restores the still-missing inventory count. Unexpected
hook changes stop the batch and report partial/indeterminate receipts without
automatic replay. Retry keys persist with the Agent's native player data, limited
to the last 32 receipts / 250 KB; keys are unique action identities, never reused
for new actions. A crash before native world save is not a transactional
cross-file guarantee. Mixed merged stacks are entirely avoided by the original
Agent until explicitly reclaimed; other players are unaffected.

The source-informed real-server gate verifies partial storage/offhand drops,
full inventory, protected rejection/explicit handover, stale and excessive
requests, cancellation restoration, identical retry, reload of receipts,
separate worn tool variants, 66 items across two native stacks, retained known
native-give origins, ongoing physical walking/mining, exact active-tool and
remaining building-material reservations, actual placement of the reserved
blocks, native merge avoidance, another player's pickup and explicit reclaim.
It is not an independent model gameplay result.

## Original approved proposal

## Public behavior

`drop_items` should select exact carried identities or slots and positive counts,
with a small bounded batch. Distinct components, names, enchantments and damage
must not be conflated by item ID. Reject stale slots and excessive counts before
mutating the inventory. Use the native inventory drop path and physical item
entities, including Forge cancellation, pickup delay and normal throw physics.
Never delete inventory as a substitute for dropping, or give items directly to
another player's inventory.

The default throw follows the current facing. A nearby observed player can be
an optional aim target; being thrown toward someone does not prove receipt.
Out-of-range or occluded recipients require normal movement or a clear outcome.

Return attempted and actual counts, inventory before/after, new entity identities
and positions, retained origin segments, remaining capacity, and per-item failure
reasons. For partial native success, retain receipts and never replay the whole
request automatically. Use a bounded idempotency key for an uncertain response.

## Availability during other work

Expose the tool during movement, following, mining, excavation, placement and
ordinary chat, including their event-specific model schemas and both controllers.
Dropping an unrelated carried stack should not cancel, replan or turn a body job.
Resolve slot identity and commit native operations on the server thread.

Revalidate active-job reservations. An active mining tool, currently used hand
item, or material needed for remaining approved supports/replant/placement must
not silently vanish from a still-valid plan. Report the exact reservation and
free quantity. The model can drop unreserved excess, choose a different item, or
pause/cancel the affected job and explicitly release its resources. A player's
clear replacement instruction is already authority to cancel that job; do not
add a ritual approval prompt.

Importance 0..2 remains protected for autonomous cleanup. An explicit player
instruction can authorize handing over the named items without asking again;
record the corresponding current request. Grades 3..5 are eligible for the
model's context-dependent capacity decision, not an automatic deletion trigger.
Borrowed, gifted and reserved items retain their provenance and notes.

Avoid immediately recovering deliberately abandoned items: the physical pickup
path and automatic collection selectors both need a bounded per-origin avoidance
record, following native merges rather than only the original entity UUID.
Mixed abandoned/wanted origins must be reported honestly rather than inventing
unit identity. An explicit request to reclaim can release avoidance. Other
players remain able to pick up the actual drops normally.

Chat stays available. One concise response when requested or directly useful;
no repeated announcements for each drop, inventory delta and capacity update.

## Acceptance

Use a normally running Minecraft server and public tools. Verify actual inventory
loss together with matching physical item entities and conserved provenance.
Cover exact partial-stack quantity, storage/offhand selection, distinct variants,
full inventory, absent/stale inputs, cancelled native tosses and uncertain retries.
Exercise dropping unrelated items during real walking and native mining without
cancelling those jobs; verify reservation conflict handling separately. Cover
protected items versus explicit handover, owner re-pickup prevention, merges,
other-player pickup and explicit reclaim. Inventory loss alone, tool output alone
or chat alone cannot establish a successful handover.
