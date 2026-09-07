# Luna parkour trial and subsequently authorized audit

Date: 2026-09-06

## Physical result

**NOT PASSED.** A separate `gpt-5.6-luna` operator was given only the public
Skill client and task instructions, without inherited implementation context.
The user's world was copied byte-for-byte before launch. The original save and
running XMCL installation were not modified by this trial.

Minecraft 26.2 / Forge 65.0.9 ran as a normal dedicated server through
`runServer`, with the repaired development sources, not as a GameTest or a
simulated display. Server PID was 17126; game/MCP ports were 25568/25768.
The associated build JAR SHA-256 was
`f7d74820cf616b3532b825f5d6929343a20aab7445470790f153385479bd5e40`.

Setup used vanilla server-console commands to clear MinePilot's inventory,
set adventure mode and teleport once to `(91, -59, -36)`. This was the console
equivalent of the requested chat command, not an interactive player chat client.
Adventure mode was confirmed through command chat feedback before and after
the trial. No further administrative movement or world edits were performed.

The target was `(93.5, -48, -36.5)` in `minecraft:overworld`. The acceptance
check required distance at most 0.5, vertical error at most 0.15, three seconds
of stable position and empty inventory/hotbar. These checks did not pass.

From trial start at 02:16:40.916 to final observation at 02:23:20.620 JST,
332 external samples all showed `(91, -59, -36)`, empty inventory and empty
hotbar. Closest and final distance to the target was 11.2915897906 blocks.
The observer sampled roughly once per second; it does not prove every
intervening tick. Luna's turn lasted 376.122 seconds. Its final visible chat
reported failure, consistent with the independent position observations.

During the trial, the parent inspected only filtered coordinates, dimension,
inventory/hotbar and final visible chat. It did not inspect Luna's transcript,
navigation choices or planner outputs and supplied no intermediate waypoints.
The standalone test server was stopped cleanly afterward. The raw filtered
observations, original copy manifest, source/build fingerprints, setup/end
command feedback and result JSON remain under `run-luna-parkour-20260906/`.

## Post-trial audit requested by the user

After the trial ended, the user explicitly requested inspection of Luna's
context to explain the six-minute delay. Only then was the public tool-call
transcript read. This diagnostic access must not be confused with the blind
observation boundary used while the trial was active.

The transcript shows 11 navigation requests, 11 planning calls and 11 failed
status results. Every result contained zero route options and the same message:
`No immediately executable route was found within the planning budget`.
There were **zero `choose` calls**. Luna tried the final destination with
different paces and several nearby intermediate coordinates. It did not obtain
an executable route, so the body never started navigation execution.

Luna greeted through public chat after about 23 seconds and acknowledged its
first navigation request after about 36 seconds. It therefore did begin using
tools; six minutes of absent physical motion must not be described as six
minutes spent only reading the Skill. Eleven explicit waits totalled about
22.45 seconds; the remaining time includes model/tool orchestration and other
work and is not separately attributed by this audit.

Usability findings:

- Each attempted segment repeated request, acknowledgement, planning and
  status polling across separate model/tool exchanges.
- The generic no-route result gave no actionable distinction between blocked
  starts, unsuitable destinations, unsupported traversal and exhausted search.
  The transcript does not establish which of those caused this map's failures.
- Repeated acknowledgement chat did not promptly explain the inability to move.
  Only two `read-chat` calls occurred during the whole turn, despite the Skill's
  instruction to check new player messages between polls.
- The parent's test prompt specified five minutes without progress as the
  early-stop condition, encouraging prolonged retries in this benchmark.
- `TRIAL_FINISHED` was explicitly required by the parent's output-isolation
  prompt. It is neither a Minecraft success signal nor a default Skill response.


Later setup audit: the copied save specifies peaceful difficulty, but this
trial server used normal difficulty. This mismatch was discovered afterward;
this historical run is not a difficulty-faithful acceptance run. Subsequent
diagnostics preserved the original peaceful setting.

The physical result remains failed. Shortening Skill instructions alone cannot
make an empty route set executable. Subsequent work should investigate the
navigation failure separately from reducing unnecessary model round trips and
providing prompt, honest failure feedback. No production fixes or course-specific
routes were introduced as part of this trial/audit.

See `2026-09-06-luna-parkour-post-trial-audit.json` for the compact public-call
evidence. It contains no hidden model reasoning or credential values.
