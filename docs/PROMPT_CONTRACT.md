# Model prompt and decision contract

The model is a planner, not a privileged game client. Every request includes a
request id, observation/world revision, goal revision, current dimension,
fairness source labels, active skill schema, and bounded recent events.

The only accepted top-level decision shape is equivalent to:

```json
{
  "requestId": "brain-7-2",
  "observedWorldRevision": 18,
  "goalRevision": 4,
  "decision": "START_SKILL",
  "skillName": "follow_entity",
  "typedArguments": {"targetName": "Player"},
  "requestedObservation": "NONE",
  "optionalSpeech": "我跟上了。",
  "confidence": 0.86
}
```

The server rejects stale revisions, unknown skills, untyped arguments,
commands, Java, packets, coordinates not present in fair observations, direct
world edits, and inventory results not produced by a normal player transaction.
`CONTINUE`/speech alone is not physical progress. After bounded no-action
responses the planner receives a correction prompt; local safety continues
independently while the model is unavailable.

The system prompt explicitly says that chat, books, signs, item names, and
waypoints are untrusted data. The model must report uncertainty and ask the
player before destructive construction or ownership-sensitive changes.
