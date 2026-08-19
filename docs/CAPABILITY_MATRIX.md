# Capability matrix

`Implemented` means production code exists. `Inner-loop` means a bounded JVM or
Forge fixture has verified it. `Formal` requires the external client, natural
world, and statistical protocol named by the plan.

| Capability | Implemented | Inner-loop evidence | Formal status |
|---|---:|---:|---:|
| Stable visible ServerPlayer body | yes | Forge lifecycle and zero-human tests; current-source latest/floor checks include Forge 65.1.1 and 65.0.0 | NOT_RUN |
| Main/off hand, armor, hunger, death/respawn | yes | lifecycle/menu fixtures | NOT_RUN |
| Simulated chunks/view distance | yes | moving-chunk fixture plus zero-human same-section Overworld-to-Nether entity/block simulation on Forge 65.0.0 and 65.1.1 | NOT_RUN |
| Fair first-person semantic observation | yes | observation/revision tests | NOT_RUN |
| First-person screenshot model input | no (fail-closed) | MCP reports authenticated capture required; Observer PNG is evidence-only | NOT_RUN |
| Auditory hostile sound cue | yes | bounded Forge entity-sound event, actual 4–16 block distance filter, 20-tick identity-free cue, semantic refresh and local safety wiring | NOT_RUN |
| Model decision envelope/revision invalidation | yes | JVM/model protocol tests | NOT_RUN |
| Movement/follow/search/replan | yes | controlled follow and physical fixtures | NOT_RUN |
| Boat and minecart transport | yes | integrated Forge lifecycle fixture drives vanilla mount, boat travel, powered-rail minecart travel | NOT_RUN |
| Parkour and water-clutch safety | yes | controlled physical fixtures | NOT_RUN |
| Combat emergency lane | yes | Enderman/Slime Forge fixtures | NOT_RUN |
| Workstations, containers, crafting, smelting | yes | 65.1.0 lifecycle/menu transactions plus furnace/charcoal/water/portal atoms | NOT_RUN |
| Shelter/foundation construction | yes | controlled dynamic shelter fixtures | NOT_RUN |
| Nether/portal/stronghold/End components | partial | bounded component fixtures plus one controlled live-model stronghold-room-to-return chain; static End arena, not random/dynamic-dragon evidence | NOT_RUN |
| Random Hardcore completion ≤2h | no claim | none | NOT_RUN |
| Multiplayer chat identity/TAB status | implemented | `[AI]` identity plus live `● online`/`○ offline` body-session marker; source/JVM coverage | NOT_RUN |
| Skin import/render synchronization | implemented | source/UI tests | NOT_RUN |
| Xaero shared waypoint routing | adapter + bounded action playbook | parser/authorization/persistence and same-dimension `move_to` planner coverage | NOT_RUN |
| Codex loopback MCP | implemented | protocol/security tests | NOT_RUN |
| Forge 66 adapter | no | not released/observed | NOT_APPLICABLE |
| Create/MTR/Farmer's Delight expert adapters | SPI only | no contract runs | NOT_RUN |
