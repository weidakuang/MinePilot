# DeepSeek runtime repair — 2026-09-12

## Deployed

The local vanilla-client server is listening at `127.0.0.1:25565`, Minecraft Java 26.2. Its persistent listener uses `deepseek-flash` at `https://api.deepseek.com`, 128000 configured context, 2048 output tokens, native function calls and thinking disabled. Model-to-listener SSE stays private; only complete accepted messages reach player chat. The API key lives outside the repository.

Installed JAR SHA-256: `8248033976d3f3b775b964f8d36f69dec9aeb1d3422101456351c57fd0680982`.

Recovery backup: `.minepilot-backup/deepseek-deploy-20260912-1740/` includes the cleanly stopped production world, old JAR, profile, config and listener sources. Restore only with the server and listener stopped. The installed skill is a symlink to this repository, so it already uses these sources. The production inventory item/count list matches the pre-repair snapshot; current health is 20. No test items or test terrain were added to the production world.

## Concrete changes

- Native streaming tool calls replace the long structured narrative decision path for compatible providers. The actual tool schemas are disclosed; common actions are resident and other contracts are fetched only when needed.
- Numen's ENTITY_PROMPT is directly adapted, with live state attached only to the current turn. Inventory is summarized by item count; recent conversation is bounded. Existing Codex transport remains optional.
- `collect` starts approach, native breaking and causal pickup in one call. Exact workbench recovery uses this entry point instead of a preview and a separate approval.
- Collection checks reachable blocks before drop routing, searches candidate stances with one shared navigation budget, and allows bounded drop/corridor repair without another model round.
- Natural-canopy remnants no longer require grounded, completely observed whole-tree proof for quantity collection. Whole-tree claims still require bounded extent. Protected farms, constructed wood and living fixtures remain protected.
- Numen's `Interaction.fireUseBlock` execution is extracted into `BlockInteraction` and shared by placement and workstation use. Placement retains its selected hand and physical/material receipts; generic interaction tries main/offhand in order.
- Safe navigation repairs are bounded; introduced hazardous fluids stop execution. The existing MinePilot path search, player body and causal mining remain in use. The complete Numen path executor and BlockDigger were **not** wholesale copied.
- Failed model responses are journaled with a bounded diagnostic and retried once before reporting failure. No physical action was dispatched for those failed responses, so the retry does not repeat accepted work.

The exact copied component inventory and licenses are in THIRD_PARTY_NOTICES.md. Adapted Java source and license texts are embedded in the JAR; the adapted Python prompt is distributed as source with LGPL/GPL texts in the skill.

## Verification

- 43 Java unit tests, 114 Python tests: pass.
- Native collection gate: all stages including eleven tree species, protected sources, real drops, interrupted work, stone alternatives, one-call workbench recovery, and overhead natural remnants: pass.
- Native navigation repair gate: 14 scenarios including detours and changed lava: pass.
- Native follow gate: three moving legs resumed in 5/7/6 ticks with 0/29, 0/72 and 0/75 stopped moving ticks. Nearby-player jump, chat, cancellation and lost target: pass. A fixture assertion was corrected to use horizontal resting spacing during the intentional jump; actual initial arrival still requires 3D distance.
- Native placement and survival/workstation gates: pass after the shared Numen interaction adapter.
- Natural-world vanilla-protocol test: no Forge/client handshake, survival companion, ordinary tick rate. Creative test player has no rendered-client physics; this is not a rendered visual acceptance test.

First DeepSeek natural run: full model decisions approximately 0.7–1.5 s, nearby workbench placement verified by the 1.25 s sample; workbench recovery/pickup succeeded. Wood gathering acquired only three logs before the scheduled stop, exposing the overstrict tree filter. This failed sample is retained.

After the tree repair: four requested oak logs acquired in approximately 15.65 s from chat submission, inventory 13 → 17, followed by a truthful completion message. The model gather call took 1.26 s. This measures real travel, breaking and pickup, not just an acknowledgement. The second run also recovered the workbench. Stop processing probes measured 7.47/8.79 ms, but their scheduled stops do not both prove interrupting active movement; native cancellation tests cover that separately.

One model response failure occurred during the second run. The previous listener did not retain its exact exception, so its cause is not established. Diagnostic journaling and a single retry were added; this is not evidence that the remote service can never fail. No universal two-second guarantee is made.

Evidence is retained under `.minepilot-backup/o-c1-20260912-160110/` as `deepseek-probe.json`, `vanilla-deepseek-runtime.json` and `vanilla-deepseek-runtime-2.json`. The directory name records the previous provider, not the currently deployed model. Earlier failing LAN-model samples were preserved.

Full village/150-block visibility, every water topology and ten minutes of rendered mixed companionship were not reaccepted in this repair. This report does not claim the entire original 180-minute feature plan is complete.
