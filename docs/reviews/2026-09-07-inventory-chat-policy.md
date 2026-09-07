# Acquisition communication policy

Date: 2026-09-07. User clarification: receiving items permits conversation, but
unnecessary announcements should be avoided unless requested or directly related
to the player. Gifts, loans, explicitly requested collection and requested
reports may justify brief optional communication. Repeated confirmations across
pickup, inventory updates and idle organization should not annoy the player;
explicit requests for repeated/detailed reporting remain an exception.

## Implementation

The model receives this rule in its stable instructions and the installed Skill.
Inventory decisions include a typed `speech_reason`: none, player gift/loan,
requested collection, requested report or concrete direct player relevance.
The host suppresses inventory speech with no relevant reason, and idle-review
speech unless reporting was requested. Classification/notes still execute while
silent. Relevance is interpreted by the model from public source/chat evidence;
it is not a promise of perfect semantic classification.

Recent acquisition evidence is retained separately from the notification queue.
A later disappeared-target event can still correlate the actual pickup UUID,
even if an earlier inventory notification already consumed the queued event.
Already-reported batches are marked, so ending the corresponding navigation does
not announce the same pickup again. Unrelated gains without a matching entity
identity cannot be attributed to the target. Blank/whitespace model replies are
silence, not invalid empty `say` calls. A silent cancellation still supplies a
valid internal cancellation reason without sending another chat message.

A failed or partially completed navigation still gets a public failure outcome
when the model omits its message; quieter acquisition reporting does not mean
silently dropping a player's failed movement request.

## Verification

39 Python tests passed, including ordinary silent gains, allowed gift
acknowledgement, target-loss correlation after earlier notification, duplicate
suppression, explicit report exceptions, empty replies and missing target IDs.
Skill validation passed. Java and the JAR are unchanged.

The initial real dedicated-server trial in `run-luna-speech-policy-20260907`
physically acquired two cobblestone and completed classification with no chat
for 40 seconds. Requested apple collection physically acquired three apples,
but then an empty model reply triggered an error report repeating that fact.
That speech-policy failure is retained in `physical-chat-results.json`; pickup
success does not erase the duplicate-chat failure. Empty-reply handling was
fixed after this trial.

Real-world retest results are recorded separately in the final trial directory;
only actual physical/chat observations establish their outcomes. These policy
trials use the Skill's persistent Luna model session, not the independently
spawned subagent used in `2026-09-07-spawned-luna.md`.

### Final real-world retest

`run-luna-speech-final-20260907/physical-chat-results.json` passed both bounded
checks. Two naturally picked-up cobblestone entered the inventory and received
classification/notes without any chat over 40 seconds. On the explicit chat
request to collect nearby apples and report their count, three apples physically
entered the inventory after 14.951 seconds. Visible chat contained one navigation
acknowledgement and one requested report: three apples collected. No additional
chat occurred during the 35-second observation after pickup, including idle
organization. The earlier two cobblestone remained unchanged in count.

The listener and server were stopped/saved after this bounded test. Gift/loan
and explicit repeated-report exceptions have host unit coverage but were not
separately exercised with a real human player in this trial. This verifies a
small set of situations, not all future wording or an unlimited quietness guarantee.
