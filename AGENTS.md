# Repository execution rules

This repository is governed by `CODEX_GOAL_REBUILD.md`. Read it in full before
changing production behavior, compatibility metadata, tests, or release status.

## Current baseline

- Commit `467afa7` is the clean baseline. The subsequent rebuild now contains
  Agent/runtime, navigation, bounded knowledge and controller code. Read the
  latest HANDOFF checkpoint for actual acceptance and remaining limitations;
  do not treat the baseline's former empty state as the current implementation.
- Do not restore, copy, or adapt the retired Agent implementation unless the
  user explicitly requests a specific concept.
- Add one observable capability at a time, followed by its physical acceptance
  test. Model text and internal success messages are never gameplay evidence.
- Preserve worlds, logs, installed JARs, credentials, and the recoverable Git
  stash created before the reset.
- Keep credentials out of source, configuration committed to Git, worlds,
  databases, logs, crash reports, screenshots, and evidence.
- Public repository content must be English. Runtime chat and tests may use
  other languages.

## Commit discipline

Before every commit, stage only the intended files and run
`scripts/preflight-before-commit.sh`. Do not create duplicate, empty,
placeholder, formatting-only, or evidence-free commits.
