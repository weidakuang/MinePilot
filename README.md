# MinePilot

MinePilot is a Forge server-side survival companion with a native player body.
Players join with an unmodified Minecraft Java 26.2 client. Dialogue uses the
persistent server-side model listener; no client mod, UI or streaming reply is required.

## Current status

The September 12 integration adds loaded-world perception up to 150 blocks,
continuous resource acquisition, native crafting/container/furnace/food actions,
water self-preservation, persistent conversation/workstation memory, and a small
camp blueprint that can resume after interruption. Adapted Numen components use
our existing movement, mining, placement and inventory/provenance systems.

Commands such as stopping and placing or reclaiming a nearby crafting table have
local execution paths. Other language requests use the world profile model. The current local play server
uses DeepSeek `deepseek-flash` with native tools and private SSE; complete messages
are delivered to vanilla chat. Codex transport remains available as a profile option. Model reply time is measured separately from
native action time; a sub-two-second language response is not guaranteed.

Use `/minepilot_mark` or chat `标记这里` to share the server ray from your crosshair.
An unmodified client cannot transmit a custom N-key binding, so no such binding
is claimed. Physical mining, placement, drops and movement retain normal reach,
materials and survival physics. Loaded-but-obscured findings are labelled; unknown
chunks are never treated as proof that resources do not exist.

The current integration is undergoing the acceptance recorded in
[the migration report](docs/reviews/2026-09-12-numen-implementation.md).
Older reports and [the handoff](docs/HANDOFF.md) retain historical limitations.
A passing build alone does not establish natural-world or rendered-client success.

## Platform

- Minecraft Java 26.2
- Forge 65.x, declared loader range `[65,66)`
- Java 25
- Mod ID `mcai_companion`

## Server installation and joining

Install Forge 65.x and the MinePilot JAR only on the Minecraft Java 26.2 server.
Run the companion's persistent Codex listener on the server host for AI dialogue
and decisions. Players do not install Forge, MinePilot or an AI application:
they use the unmodified Minecraft Java 26.2 client and join the server address
through Multiplayer. Chat and `/minepilot_mark` use vanilla Minecraft packets.

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

Original MinePilot code is licensed under Apache License 2.0. Adapted Numen
components remain LGPL-3.0-only; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Their exact modified sources and license texts are included in the JAR.
