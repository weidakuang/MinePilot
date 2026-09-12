# Player arrival, follow termination, and construction navigation — 2026-09-12

Player rendezvous now uses a three-block sphere and ends on the next server tick,
before retargeting or path capture. It releases movement and looks at the player.
Ordinary vanilla braking/inertia remains; the native gate bounds settling motion
below 0.35 blocks and verifies no subsequent pursuit when the player walks away.
Coordinate/entity navigation keeps its original acceptance radius and settling rules.

Player follow remains live during movement and short rests. It completes when the
followed player says `到了`, `到地方了`, or `我们到了`, or when that player stays
within a 0.25-block position tolerance for 600 server ticks and the companion is
within three blocks. Another player's arrival chat cannot terminate it. At normal
20 TPS this stationary timeout is 30 seconds. Dimension changes and lost targets
retain failure handling; no-route retries now terminate after three retries.

Numen reference: pinned 34ef004dac3095fbbd928a897927e277c69d02fa,
FollowCompanionTask, MovementTraverse, MovementPillar. The adaptation retains
MinePilot physics and path planning. Planned support dependencies now allow a
continuous bridge. The executor sneaks beyond the anchor's edge before using its
side face. Support waypoints cannot finish merely because the body overhangs the
edge: the planned floor must actually exist. Pillars jump clear of the destination
block before native underfoot placement. No teleports or generated inventory.

Automatic route selection prefers zero-material paths and allows at most 16
manifested expendable support blocks, importance 3–5, with zero predicted damage.
Larger or hazardous routes remain model decisions. Native placement protection,
reach, collision, material availability, and bounded progress checks remain active.
This is not a general terrain-demolition or arbitrary parkour port.

Validation:
- 44 Java unit tests and 128 Python tests passed; build succeeded.
- Native follow gate: continuous walking/turning/chat, cancellation, lost target,
  one-shot three-block arrival, no orbit/restart, scoped arrival chat, 600-tick end.
- Survival bridge: six actual cobblestone supports, inventory 12 -> 6, physically
  crossed a six-block gap and reached the seventh block.
- Survival pillar: three actual supports, inventory 12 -> 9, physically rose three blocks.
- All 14 prior native navigation repair scenarios passed, including newly exposed
  lava stopping, changed corridor detours, sprint gap jumps, and forest traversal.
- These are accelerated isolated native tests, not a new normal-speed player session.

Deployment:
- Production JAR SHA-256: `3f81fa164d26733f80e03b8002660d80eae10f6c6a3826199d71fd9df958c1e8`.
- Recoverable stopped-world backup: `/Users/weida/Documents/minecraft-ai-companion-forge/.minepilot-backup/navigation-arrival-1789222097/production`.
