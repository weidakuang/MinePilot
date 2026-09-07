# MinePilot

MinePilot is being rebuilt from a clean Forge baseline.

## Current status

Version `0.2.0-dev-mc26.2` is an early rebuild checkpoint. It contains a visible
headless player, chat and physical navigation, a persistent external Codex
controller, bounded perception and sound queries, inventory acquisition records,
item policies, and named waypoints. Internal model interfaces are implemented;
a working external provider has not completed acceptance.

Controlled movement, follow and knowledge scenarios have physical test evidence.
The supplied parkour still FAILS, ordinary-language replies/stops still take
seconds, and general three-dimensional travel is not accepted. Mining, crafting,
combat, autonomous survival, complete provenance and structure recognition remain
unfinished. This is not a production companion release.

Start with [the handoff](docs/HANDOFF.md) and
[the latest movement report](docs/reviews/2026-09-07-movement-continuation.md).
The September 8 backup preserves the current rebuild on a separate branch;
historical reports retain their original dates and test limitations.

This branch exists so each capability can be added and physically verified one
at a time without inheriting the retired implementation.

## Platform

- Minecraft Java 26.2
- Forge 65.x, declared loader range `[65,66)`
- Java 25
- Mod ID `mcai_companion`

## Build

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build \
  -Pforge_compile_version=65.0.9
```

The installable development JAR is produced in `build/libs`. A successful build
does not establish gameplay acceptance. Do not rebuild development classes while
a test server is using them.

## Rebuild rules

The active objective is [CODEX_GOAL_REBUILD.md](CODEX_GOAL_REBUILD.md).
Retired code and claims remain available through Git history but are not part
of this branch.

## License

Original MinePilot code is licensed under Apache License 2.0.
