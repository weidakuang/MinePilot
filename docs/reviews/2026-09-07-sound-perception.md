# Native subtitle hearing tool

Date: 2026-09-07. Platform: Minecraft Java 26.2, Forge 65.0.9, Java 25.

## Implemented contract

The public read-only `listen` tool returns recent native Chinese subtitle text
and translation keys, eight horizontal directions relative to the current body
heading, vertical relation, three-dimensional distance from the first-person
eye position, and source identity/confidence. Player sources include a `Player:`
name label and UUID; entity and block names use the native translation catalog.
Self sounds are explicitly labelled as self rather than a horizontal direction.
The response binds the listener's eye coordinates/heading and dimension to the
result. Queries do not move, turn, spend items or send chat.

`limit` is 1..64 (default 32). Optional `after_sequence` supports bounded newer
results; omit it to view all retained in-range captions. Captions have the vanilla
default three-second monotonic lifetime. Repeated playback refreshes the same
caption/source position; different positions remain separate. Storage is capped
at 128 captions and output reports truncation and capacity-related cursor loss.
An empty list means no retained in-range caption, not universal silence.

Sound data comes from actual outbound `ClientboundSoundPacket` and
`ClientboundSoundEntityPacket` delivery to the headless player's embedded
connection. This preserves server broadcast range and excluded-recipient rules.
The tool then applies the selected vanilla sound resource's attenuation range,
including seeded weighted sound variants, referenced sound events and volume.
There is no visual occlusion requirement. Subtitle callbacks precede user volume
mute filtering in vanilla, so mute does not suppress this hearing contract.

An entity-bound packet identifies an actor. A positional packet does not.
Position-matched entities and blocks are explicitly unconfirmed candidates; no
match or multiple candidates produces unknown. Matching uses native packet
quantization, including float precision loss at large world coordinates. Native
sound names never prove an emitter: a note block may imitate a mob, for example.

The tool is exposed through authenticated MCP, the direct Skill CLI, the
persistent Codex typed decision host and the internal model's idle/executing
schema. It does not bypass navigation's acknowledgement barrier. Model/Skill
instructions require respecting uncertainty and avoid unsolicited sound reports.

## Vanilla reference and boundaries

Pinned local Forge source was inspected for `SubtitleOverlay`, `SoundEngine`,
`SoundManager`, `WeighedSoundEvents`, the sound packet types, `ServerLevel`,
`EntityBoundSoundInstance` and `LevelEventHandler`. Related primary references:
[Forge sound-engine patch](https://github.com/MinecraftForge/MinecraftForge/blob/26.2/patches/minecraft/net/minecraft/client/sounds/SoundEngine.java.patch)
and [Forge server-level patch](https://github.com/MinecraftForge/MinecraftForge/blob/26.2/patches/minecraft/net/minecraft/server/level/ServerLevel.java.patch).
The implementation targets the locally pinned 65.0.9 source, not a moving branch.

`scripts/generate-sound-catalog.py` validates the installed 26.2 asset index and
asset SHA-1s, then extracts only subtitle/name/range/selection metadata into
`data/mcai_companion/sound_catalog.json`: 1,968 event definitions and 3,128 short
native labels. Audio files and client classes are not included. Source asset
hashes are retained in that generated resource.

This is not full client subtitle parity. Client-local sounds, sounds generated
only by client handling of level/entity events, custom resource packs and
unknown mod sound catalogs are not emulated. A headless body has no human
client's subtitle-duration preference; it uses the default three seconds.
Vanilla UI collapses equal captions to a nearest source and retains a caption's
initial range; this tool intentionally keeps each source and its playback range
so distinct emitters and volume changes remain attributable. Entity-bound sounds
whose entity is already absent when the packet is consumed cannot be resolved.
These limitations are exposed in tool coverage, not filled with invented sounds.

## Verification and retained failures

The source-informed sound gate ran a real Forge GameTest server and queried the
same public tool dispatcher as MCP. It verified eight directions, native mob and
chest captions, hearing a real zombie through a stone wall, actual chest opening,
position-candidate versus entity-packet confidence, player name/UUID, and hearing
direction after a physical public-tool turn. Coordinates stayed fixed and the
adventure-mode inventory stayed empty throughout listening.

A chest at 13 blocks with normal volume was omitted; an ordinary 16-block sound
at 13 blocks remained audible; a volume-2 chest at 20 blocks used its native
24-block range. Undelivered 17-block ordinary sounds and unsubtitled music were
omitted. Pagination was exercised through the public dispatcher. Separate clock
boundary cases used injected packets and a fake monotonic clock for exact expiry,
refresh and overflow checks; those are not labelled as live network evidence.

The first gate failed because fixture heading setup retained its previous control
frame. The second failed because tightly spaced overflow fixtures collapsed into
identical native packet positions at large coordinates. Fixture setup was fixed;
production candidate matching was also corrected for that packet precision loss.
Both failure logs remain in `run-sound-gate-20260907/`.

Final evidence:

- `run-sound-gate-20260907/minepilot-sound-gate-20260907-final.log`: exactly one
  required real sound test selected and passed; Gradle exit zero.
- `run-sound-gate-20260907/gametestserver/gametestworld/sound-tool-evidence.json`:
  public caption/source/direction/distance snapshots.
- `run-sound-knowledge-regression-20260907/acceptance.log`: real inventory,
  protected support placement, turning, vision and dynamic pickup regressions
  passed with final coordinate/inventory/block observations.
- 23 JUnit tests and 40 Python tests passed; Skill validation passed using the
  existing validation venv. System and bundled Python lack PyYAML; no global
  dependency changes were made.
- Final build succeeded. No live internal provider or Luna sound-conversation
  acceptance was performed; schema/host exposure is not a claim of model behavior.

## Artifact and continuation

JAR: `build/libs/mcai_companion-0.2.0-dev-mc26.2.jar`.
SHA-256: `26d45ab995a09617517d70e8ac1238bcce544c4d14225f56b1baf807f1f6a790`.
All bounded test servers stopped after saving. No original save, installed XMCL
JAR, user server configuration/OP state, commit or GitHub state was changed.

Run the sound gate with:

```bash
./gradlew runGameTestServer --offline --no-build-cache \
  -Pforge_compile_version=65.0.9 \
  -Pminepilot_run_dir=run-sound-gate-20260907 \
  -Dminepilot.soundTest=true
```

Use the repository JDK 25 as documented in HANDOFF. In a running world using the
new JAR, direct operators can call `minepilot.py tool --name listen` immediately.
Complete client-event coverage and independent model sound interaction remain
separate acceptance work. Earlier parkour and broader product claims remain
unaccepted.
