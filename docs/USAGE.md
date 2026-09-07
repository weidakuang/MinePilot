# MinePilot Clean Baseline Usage

The `0.2.0-dev-mc26.2` build is intentionally not an Agent.

## Build

Use Java 25 and select the Forge 65 patch used by the target instance:

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew clean build \
  -Pforge_compile_version=65.0.9
```

## Install

Copy the single JAR from `build/libs` into a Minecraft 26.2 Forge 65.x
instance.

The current expected behavior is limited to successful Mod discovery and
startup. No AI body, chat response, movement, model connection, UI, skin,
memory, or gameplay action should appear. Those features will be added in
later explicit rebuild steps.
