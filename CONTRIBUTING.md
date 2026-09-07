# Contributing to MinePilot

MinePilot is currently a clean Agent rebuild. Read
[CODEX_GOAL_REBUILD.md](CODEX_GOAL_REBUILD.md) and [AGENTS.md](AGENTS.md)
before changing production code.

## Rules

- Add one externally observable capability at a time.
- Do not restore the retired implementation wholesale.
- Do not add unused abstractions, placeholder skills, scripted demonstrations,
  or claims without physical evidence.
- Gameplay success must be verified from actual game state such as coordinates,
  inventory, equipment, blocks, entities, menus, and vanilla statistics.
- Model output or an internal completion flag is not gameplay evidence.
- Keep public repository content in English.
- Never commit credentials, private logs, worlds, generated artifacts, or
  player data.
- A Forge major line requires its own mapped artifact.

## Commits

Use a concise English subject and keep one purpose per commit. Stage only the
intended files, then run:

```bash
git diff --cached --check
scripts/preflight-before-commit.sh
```

Do not create duplicate, empty, formatting-only, placeholder, or no-op commits.

Original contributions are released under Apache License 2.0.
