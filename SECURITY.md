# Security Policy

## Scope

This policy covers the MinePilot source tree and Forge development JAR. The
current `0.2.0-dev-mc26.2` branch is a clean baseline and contains no Agent,
model, credential, MCP, memory, or gameplay subsystem.

## Reporting

Do not include API keys, access tokens, world saves, player addresses, private
logs, or personal data in a public issue. Use the repository's private
security channel, or publish only a redacted description and request private
contact.

## Rebuild requirements

Any future credential or network feature must keep secrets out of Git, worlds,
databases, logs, crash reports, screenshots, and evidence. Any future gameplay
feature must use normal server rules and must not introduce hidden-world reads,
teleports, direct inventory edits, or synthetic success evidence.
