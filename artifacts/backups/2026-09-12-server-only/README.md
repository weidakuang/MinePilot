# MinePilot server-only backup — 2026-09-12

This snapshot preserves the current mod, companion controller, sources and
verified production JAR. Install the JAR on a Minecraft Java 26.2 / Forge 65.x
server running Java 25. Run the persistent companion listener on the server
host. Players use the unmodified Java 26.2 client; they need no MinePilot or
Forge installation. See the repository README for setup and public controls.

## Recorded validation

- 42 Java unit tests and 103 Python tests passed.
- Fourteen native ordinary-navigation scenarios passed.
- Native slow following: 0 stopped ticks / 72 measured moving ticks; continuous
  turning: 0 / 75. WALK and AUTO were exercised, with rest, chat and cancellation.
- The exact included JAR passed vanilla protocol login, enter-play and
  `/minepilot_mark` on an installed Forge 65.0.9 server.

These are the preserved test results, not a new all-feature certification.
The full Numen migration remains incomplete: persistent failed-location memory,
automatic semantic conversation compression, a natural village expedition and
one clean ten-minute final mixed experience remain missing or unverified. See
`docs/reviews/2026-09-12-numen-completeness-audit.md` in the repository.

## Restore

Check out this backup commit to restore the source and companion scripts, or
copy the included JAR into the stopped server's `mods/` directory after saving
and backing up that server. Verify SHA256SUMS before installation. This snapshot
does not include worlds, runtime credentials, local configuration or user saves.
Those remain in separate local backups. Preserve any newer gameplay progress.

Adapted Numen sources and LGPL/GPL notices are included in the repository and
inside the JAR. The remaining original source retains its Apache-2.0 license.
