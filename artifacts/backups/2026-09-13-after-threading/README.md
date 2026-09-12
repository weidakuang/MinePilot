# Post-threading backup — 2026-09-13

Server-only Minecraft 26.2 MinePilot JAR, source and tests are preserved on
`codex/backup-after-threading-20260913`. Vanilla clients need no mod.

See [implementation and validation](../../../docs/THREADING_E5_20260913.md)
for worker limits, main-thread boundaries, 46 Java / 128 Python test results,
native acceptance gates and normal-speed production smoke measurements.
No E5 hardware benchmark was performed.

The pre-optimization backup remains on `codex/backup-before-threading-20260913`.
Worlds, credentials and private runtime configuration are excluded.
