# Navigation tool review

Date: 2026-09-06

Update: these findings were subsequently addressed. See
`2026-09-06-navigation-fixes.md` for the changes, validation and remaining
acceptance boundaries. The probe now asserts the repaired behavior; the
historical reproduction outputs below are retained as review evidence.

Scope: the existing movement capability and the user's public Codex Skill
workflow: observe, greet in Chinese, approach the player, verify final position.
No production changes, Git operations, JAR replacement or world mutations were
made for this review. Existing rebuild files were preserved.

Movement is one gameplay capability exposed through several MCP protocol calls.
Observation and chat are supporting interfaces. Opening doors and placing
support blocks exist as navigation implementation branches; their presence does
not establish physically accepted gameplay capability.

## Findings

### R1 [P1] Ordinary armor incorrectly reduces predicted fall damage

Location: `AnytimeNavigationPlanner.java:261-265`; multiplier source:
`NavigationSnapshotBuilder.java:197-205`.

The planner multiplies all predicted damage, including falls, by a value derived
from ordinary armor points. A source-compiled probe produces a feasible
nine-block descent for a body with three health and a 0.2 armor multiplier,
predicting only 1.2 damage. The identical geometry without that multiplier is
rejected. Ordinary full armor without protective enchantments or effects does
not make this fall survivable.

Confirmed against the local Forge 65.0.9 source artifact:
`LivingEntity.calculateFallDamage` and `getDamageAfterArmorAbsorb`, and the
Minecraft 26.2 data tag `data/minecraft/tags/damage_type/bypasses_armor.json`,
which includes `minecraft:fall`. This is a planning reproduction plus a game
rule check, not an observed in-world fall.

Fix: estimate damage by source. Do not apply armor-point reduction to falls;
account for relevant attributes/effects separately and conservatively. Verify
that armor alone never turns a lethal fall into a feasible route.

### R2 [P1] Rounded start positions can create an endless empty-route replan

Location: `AnytimeNavigationPlanner.java:98-99,497-502`; exact position is lost
in `NavigationSnapshotBuilder.java:54-55`.

Reproduction: actual body `(2.1,1,0.5)`, target `(4.4,1,0.5)`, acceptance radius
2. The actual distance is 2.3, but the planner tests the start cell center
`(2.5,1,0.5)`, whose distance is 1.9. It returns a feasible route with zero
steps. The follower immediately calls `finish`, the exact-coordinate arrival
check rejects it, and replanning from the unchanged body returns the same empty
route. The empty-route output was directly reproduced; the repeated runtime
cycle is inferred from the coordinator/follower control flow.

The per-waypoint early-exit tolerance can also leave the body short of the goal
region even when the waypoint center is inside it.

Fix: retain the exact start position, use an exact-position check for zero-step
success, and execute a physical final approach when required. Give the final
waypoint an arrival rule consistent with the requested region. Do not weaken
the final-coordinate verifier to hide the error.

### R3 [P1] Live route validation misses disappearing support and new fluids

Location: `NavigationFollower.java:202-210`; unconnected invalidation method:
`NavigationToolCoordinator.java:160-165`.

For ordinary walking, the live check inspects only collision in the body/head
cells. Removing the floor below the next waypoint leaves both checks false.
Introducing fluid likewise does not cause this predicate to reject the step.
`markWorldChanged()` has no call sites in the current source tree, so there is
no independent event-driven invalidation to cover the missing checks.

A player breaking the next floor block after route selection can therefore
leave the old walking instruction active toward an unsupported step. This is
a source-traced failure scenario, not a completed physical reproduction.

Fix: validate loaded state, footing, fluids/hazards and movement-specific
preconditions against the live corridor before committing movement. Connect
bounded corridor invalidation to relevant world changes. Test removal of floor
and introduction of hazards after planning, not only newly placed walls.

### R4 [P2] COMPLETED does not establish a stable physical stop

Location: `NavigationFollower.java:347-370`, especially `363-366`.

After position/facing checks, `finish` clears the control input and immediately
emits `COMPLETED`. Clearing input does not remove existing physical momentum;
there is no grounded/stable-state or velocity observation window here. The
waypoint check permits vertical deviation below 1.25 blocks, so an airborne
body inside the radius can reach this completion path.

The backend gate's separate stable-stop observation does not implement that
guarantee in the production navigation protocol. On a jump or slippery surface,
a consumer may observe completion before the body has settled. This finding is
based on source control flow, not an observed failure on ice.

Fix: retain route ownership while settling under ordinary physics, recheck the
destination, and only then emit completion. Use an appropriate stable-state
definition for each supported movement medium. Preserve the request-bound
terminal evidence. Test drift after the first in-radius tick.

### R5 [P2] Fresh-world observation cannot identify the human destination

Location: `CodexToolService.java:186-201` and its `bodyState` method.

The MCP observation includes the Agent, inventory and recent chat but no online
human-player list. The client requires a name for a player target. On a fresh
world with no prior chat and no player identity supplied in the Codex request,
the public Skill cannot resolve the user's instruction to come beside them.
It must ask for the name or wait for a normal in-game chat message.

Fix: expose a bounded list of non-Agent players with name/UUID, dimension and
position. Resolve an unambiguous sole player; clarify if multiple players are
plausible. Make target UUID handling agree with the advertised schema (the
current player-name resolver only matches names). Test the exact fresh-world
workflow without relying on a fixture's `TestHuman` name.

## Additional improvements within the current tool

- Generate `navigation_status.validActions` from the current phase rather than
  the last event. `acknowledgementSent()` changes phase without changing that
  event, so the status can still advertise `say` when the next action is plan.
- Profile server-thread snapshot capture separately from worker-thread search.
  No server-tick performance regression was measured during this review.
- Keep unverified door/support/swimming branches visibly unaccepted. Support
  inventory counting and actual slot selection currently use different block
  predicates and need agreement before bridging is accepted.
- During interactive Codex operation, read new player chat between bounded
  navigation status polls so in-game stop/change requests are not ignored. The
  current one-shot CLI calls do not create a continuously listening companion.

## Evidence and reproduction

`NavigationReviewProbe.java` invokes the current pure Java planner. Compile it
with the repository JDK and these production files into a temporary directory:
`AnytimeNavigationPlanner`, `NavigationWorldSnapshot`, `NavigationPlan`,
`NavigationPlannerConfig`, `RouteOption`, and `TravelPace`. Run
`NavigationReviewProbe` with that temporary directory as its classpath.

Observed output from the equivalent source-compiled review probe:

```text
ARRIVAL: actual distance=2.3000000000000003, radius=2, steps=0, feasible=true
FALL: health=3, drop=9, predicted damage=1.2000000000000002, feasible=true
CONTROL: same cliff without armor multiplier is rejected
```

The probe intentionally reproduces defects; its successful exit does not mean
the navigation tool passes acceptance. After fixes, replace these defect
expectations with regression assertions of the required behavior.

The previous assessment in this task passed four JUnit and two Python client
tests. Those existing tests do not exercise these findings. They were not
rerun merely for documentation changes in this review.

## Current live attempt

The public `observe` client was invoked first. It failed because the configured
XMCL instance token was unavailable; a subsequent existence check confirmed
that file did not exist. No bearer value was read or exposed.

The user was asked to enter the matching test world or identify their actual
instance while source review continued. No greeting or navigation was sent,
and there are no final body coordinates from this attempt. Live acceptance
remains pending. The Skill prohibits launching/changing a world without the
user requesting that action.

Once connected, verify `online` and `externalControlAvailable`, identify the
player, retain starting state, reserve navigation, send the request-correlated
Chinese greeting/acknowledgement, plan, compare feasible routes, choose, and
observe terminal state. Independently check final coordinates against the
resolved target and requested radius, then check physical stability. Do not
report arrival from chat or a chosen/started route.
