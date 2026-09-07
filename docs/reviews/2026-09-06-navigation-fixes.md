# Navigation repair results

Date: 2026-09-06

The five findings in `2026-09-06-navigation.md` have been addressed in source.
This report records the tested scope, including failed attempts. No commits,
GitHub operations, legacy implementation restoration or user-world mutations
were performed. The user's currently running XMCL instance still uses its
previous JAR; the replacement artifact has been built but not installed live.

## Changes

1. Fall damage is calculated separately from ordinary armor mitigation, using
   captured safe-fall-distance and fall-damage-multiplier attributes. Beneficial
   enchantments/effects remain conservatively uncredited. Three health no longer
   makes a nine-block descent feasible merely because the body wears armor.
2. Navigation snapshots retain exact body coordinates alongside grid position.
   A start cell whose center is inside the destination radius no longer yields
   an empty path when the actual body is outside. A physical approach step is
   emitted, and the final waypoint checks the actual requested region. Final
   approach slows through normal control input.
3. The imminent route segment is checked before authoritative body physics and
   again before assigning a new control frame. Checks cover loaded state, world
   border, collision, supporting floor, fluids and classified hazards. An
   observed unsafe corridor advances its revision and requests replanning.
   A grounded body receives one normal counter-input frame to brake residual
   momentum; no position or velocity assignment is used by this production fix.
   Planning also rejects hazardous footing the follower will not traverse.
4. Completion retains route ownership while the body settles. It requires
   twenty consecutive stable ticks, the requested position/radius and optional
   facing. Settling has a bounded timeout. MCP body observations now also expose
   grounded state, velocity and server tick for external verification.
5. MCP observation includes up to 64 non-Agent players with name, UUID,
   dimension, position and alive state, plus total count and truncation status.
   Player targets resolve by UUID as well as exact case-insensitive name. The
   Skill documents unambiguous player selection and reading chat between polls.

Related corrections: status valid-actions are derived from the actual phase;
the prompt no longer offers nonexistent post-completion idle/sneaking tools;
support-slot selection uses the same full-block criterion as inventory counting.

## Final validation

All Java runs used the repository JDK 25 and Forge 65.0.9.

| Check | Result | Evidence boundary |
| --- | --- | --- |
| `build --rerun-tasks --no-build-cache` | Passed; 10 JUnit tests, zero failures/errors/skips | Compilation, packaging, planner and target regressions |
| Python Skill client tests | 2 passed | Redirect isolation and profile handling |
| Default headless physics GameTest | One required test passed | Continuous physical player movement |
| Navigation repair GameTest | One required test containing five scenarios passed | Controlled physical regressions through the production coordinator/follower |
| Final public Skill GameTest | One required test passed; CLI driver exited zero | Public MCP/Skill flow, visible Chinese acknowledgement, request-bound physical result |

The controlled repair suite verifies:

- the exact-start boundary case physically advances approximately 0.448 blocks
  instead of looping over an empty route;
- an externally applied upward impulse prevents completion until actual
  landing and twenty stable ticks;
- removing the next floor block after movement begins causes braking and
  replanning, with the body settling before the missing floor;
- introducing lava into the imminent segment after movement begins causes the
  same safe stop, without health loss in this controlled case;
- moving the human fixture after the Agent begins walking triggers replanning
  and approximately 9.926 blocks of actual Agent displacement toward the updated
  player target, followed by a stable, in-radius stop.

These fixture tests directly drive the coordinator; they are not independent
language-model black-box acceptance. Fixture setup moves bodies and prepares
blocks deliberately. Movement after each request is supplied by production
player control. The moving-target fixture is repositioned to provoke target
invalidation; the Agent is not repositioned during its route.

The final public-interface test invoked the bundled Python Skill CLI throughout.
It started with an empty chat history, identified the sole human through
`onlinePlayers`, targeted its UUID, reserved navigation, sent a correlated
Chinese acknowledgement, planned, inspected all route candidates, chose a
zero-predicted-damage walking route, read chat between polls and verified the
terminal result. The driver was authored by the implementation assistant; it
was not a separate, source-blind AI operator.

Final public-interface evidence:

- Request: `e433e73a-43a4-4681-8fe5-8268dd075339`.
- Target UUID: `5815d267-8acc-4e0a-b30a-7ffb055a728c`.
- Start: `(-4378120.5, -48.0, -11966413.5)`.
- Final: `(-4378111.486967094, -48.0, -11966413.499971462)`.
- Displacement: approximately `9.013032906` blocks.
- Distance to resolved destination: approximately `0.986967095` blocks,
  inside the requested `2.0`-block radius.
- Health and food: 20 before and after; empty inventory/hotbar unchanged.
- Terminal phase: `COMPLETED`; grounded, with negligible horizontal velocity.

Full public trace: `navigation-fixes-final-skill-evidence-2026-09-06.json`.
The earlier manual CLI pass is retained separately in
`navigation-fixes-skill-evidence-2026-09-06.json`.

## Failed attempts and test conditions

- The first public Skill attempt encountered a route with about 15 predicted
  health loss from nearby-entity exposure. It was cancelled without moving and
  is not counted as a pass. The flat Skill test environment now explicitly
  disables natural mob spawning. That test does not establish hostile-terrain
  navigation capability; survival damage remains enabled.
- The strengthened lava test initially failed because zero movement input left
  enough momentum for the body's edge to touch the adjacent lava. The normal
  counter-input brake was added in response; the strengthened test then passed.
- A later public test could not bind its MCP endpoint because the user had
  started the actual XMCL instance on port 25766. Only the isolated test process
  was stopped. The final run used the already supported `MINEPILOT_MCP_PORT`
  override at 25767 and a separate non-secret client profile. The user's world
  and normal Skill profile were left active and unchanged.

Final test run directories, ignored by Git:

- `run-navigation-final-body-20260906`
- `run-navigation-final-repair-20260906`
- `run-navigation-final-skill-20260906-b`

The review probe was converted from defect expectations to regression checks.
It now reports one physical approach step, six predicted fall damage despite
armor, and rejection of the lethal fall both with and without armor.

## Artifact

Built JAR: `build/libs/mcai_companion-0.2.0-dev-mc26.2.jar`

SHA-256:
`f7d74820cf616b3532b825f5d6929343a20aab7445470790f153385479bd5e40`

The previous built artifact was preserved under
`run-navigation-artifact-backups/`. The installed XMCL JAR was not replaced
while Minecraft was running. Full game exit is required before installing the
new artifact; leaving and re-entering a world alone does not reload mod classes.

No current-JAR real-client acceptance, live remote-provider acceptance, general
natural-terrain reliability, sustained follow capability, or broad survival
claim follows from these repairs. The previously documented XMCL Forge-version
discrepancy remains to be checked when the replacement is launched.

## Reproduction commands

Use the repository JDK as documented in `HANDOFF.md`. Optional
`-Pminepilot_run_dir=run-UNIQUE-NAME` gives each GameTest a separate disposable
working directory, preserving earlier worlds and logs.

```sh
./gradlew build --rerun-tasks --no-build-cache -Pforge_compile_version=65.0.9
python3 -m unittest discover -s skills/minepilot-companion/tests -v
./gradlew runGameTestServer --no-build-cache -Pforge_compile_version=65.0.9
./gradlew runGameTestServer --no-build-cache -Pforge_compile_version=65.0.9 \
  -Dminepilot.navigationRepairTest=true
```

The public Skill gate remains opt-in with `-Dminepilot.codexSkillTest=true` and
requires an external operator to perform the documented Skill protocol. Test
selection flags are mutually exclusive; unselected gates cannot count as
passing. If the user's game is running, use a separate loopback port and a
separate `MINEPILOT_CODEX_CONFIG` profile for the isolated test.
