# Codex recovery checkpoint

## 当前恢复摘要（2026-08-14T02:49:00Z）

- 第二次真实 MiMo 连续 M2 运行已明确失败在末地战斗：模型真实选择了
  `fight_ender_dragon`，但日志只出现一次 `begin_mining`，龙保持 200 点生命，身体
  随后被龙的魔法伤害击杀。此前的首登身体引用竞态已越过，故本次不是模型请求或
  ServerPlayer 生命周期失败；M2 仍记为 `FAIL`，不宣称通关。
- 针对该根因已实现并提交前验证：`FightEnderDragonSkill` 活动时声明接触威胁、可见
  投射物和近距离龙目标责任；受击/龙息无实体线索时用危险信号做有界第一人称侧移，
  投射物闪避带 6 tick 节流；龙靠近时优先攻击龙而不是继续拆晶体铁栏，并可中断
  正在卡住的铁栏拆除。所有动作仍通过原版 `ServerPlayer` 交互/移动，未读隐藏实体、
  未传送、未直接改世界。新增的末地技能测试和 JDK25 全量离线测试通过。
- 下一步在提交和 GitHub 备份后，用同一真实 MiMo M2 连续切片重跑；只有看到龙血下降、
  `low_level_actions_issued` 的攻击/移动、`DRAGON_KILLED` 与返回传送门事件，才会升级
  结果；否则继续保留 `FAIL` 并按日志定位。M0–M4 正式统计门禁和随机 Hardcore 仍为
  `NOT_RUN`。

## 当前恢复摘要（2026-08-14T02:41:00Z）

- 真实 MiMo `mimo-v2.5` / `https://api.slomerex.xyz` 的同一条连续
  `real_player_task_to_live_model_nether_materials_to_victory` 已从下界物资阶段
  走到主世界、要塞三角测量、末地门激活和末地进入；模型实际选择了
  `craft_recipe`、`return_via_verified_portal`、`triangulate_stronghold_search_area`、
  `reach_observed_stronghold`、`search_stronghold_portal_room`、
  `activate_observed_end_portal`、`find_and_enter_observed_portal` 和
  `fight_ender_dragon`。这次运行不是聊天/模型等待假死，而是在真实末地战斗中
  按 Hardcore 规则被末影龙击杀，龙仍为 200 点生命，Gradle/GameTest 退出码为 1，
  用时约 14.34 分钟；因此记录为 `FAIL`，不宣称 M2 或通关。
- 第一次尝试在模型动作前因首登初始锚定的 ServerPlayer remove/relogin 窗口使用了
  旧引用而失败；已将 `LiveEyeCraftReturnStrongholdScenario` 改为每 tick 重新获取
  同 UUID 的权威身体，并在 `FAILED` 以外等待临时空窗。第二次运行证明该竞态已越过，
  但暴露末地龙息/火球期间应急车道抢占击龙技能、身体停在危险区的真实缺口。
- 当时未提交源码修复：新增技能能力 `managesVisibleProjectileThreats`，只让
  `FightEnderDragonSkill` 在活动期间拥有可见投射物邻近的有限责任；应急监督器仍保留
  接触、着火、坠落、空气、生命、食物和未被该技能声明覆盖的投射物。击龙技能加入
  基于第一人称可见投射物/无方向危险信号的有限反向移动和转向，不读隐藏实体、不传送。
  定向 JDK25 SkillSupervisor/Fight/Emergency 测试已通过，下一步是真实 MiMo 重跑，
  观察龙血下降、闪避动作和最终死亡事件；若仍失败继续以 `FAIL` 记录。
- 本轮已创建公开仓库 `weidakuang/MinePilot` 的分支
  `codex/minepilot-0.1.9`。提交 `8261db8` 已上传完整源码树、Apache-2.0、项目规范、
  使用教程、贡献纪律和 `scripts/preflight-before-commit.sh`；远端 `main` 未被覆盖。
  本机 HTTPS 推送因缺少 `gh` 凭据助手失败，后续提交继续使用已验证的 GitHub 连接器。
- 密钥只作为当前测试进程输入，未写入源码、世界、SQLite、日志、工件或 Git；测试后
  建议轮换凭据。M0–M4 正式门禁、真实 Actor/Observer、Hardcore 随机种子统计仍是
  `NOT_RUN`，受控切片不升级正式状态。

## 当前恢复摘要（2026-08-14T02:10:41Z）

- 使用用户临时进程凭据真实运行 `mimo-v2.5` / `https://api.slomerex.xyz`，Forge 65.1.1、Minecraft 26.2、JDK 25 GameTest `real_player_task_to_live_model_foundation_bootstrap`。从空背包开始，真实模型依次选择木材、基础合成、石工具、食物、铁工具、工作站、箱子/供应品和 `build_shelter_step`；服务端验证木材、石器、食物、铁工具、工作台/熔炉/箱子、封闭紧凑避难所和门均完成。
- GameTest `1/1` 通过，用时 `9.342 min`（Gradle 进程 `BUILD SUCCESSFUL`，约 9 分 35 秒）。SQLite `event_log` 核对到 `conversation_task_accepted`、21 次感知、16 条 HTTP 200 模型响应、16 条 `decision_revision_accepted`、16 次 `skill_started` 和 16 条 `low_level_actions_issued`；模型使用量为 17 次计费请求、153442 输入、2668 输出、156110 总 token，未出现 no-action/safe-idle 终止。
- 这是当前源码的受控真实模型 M1 基础生存纵向切片，不升级随机 Hardcore、两小时通关、双客户端 Actor/Observer、M0–M4 统计门槛；这些仍保持 `NOT_RUN`。密钥只在进程环境中使用，未进入源码、配置、SQLite、日志、工件或提交。
- 本轮代码已在本地提交 `f158072`（`Harden live model crop maintenance`）。公开 GitHub 备份仍未上传：工作区没有 `origin`，`gh` CLI 未安装，连接器没有创建新公开仓库的接口；需用户提供新建的公开仓库 URL（或安装并认证 `gh` 后配置 remote）。

## 当前恢复摘要（2026-08-13T16:54:00Z）

- 本轮真实使用用户临时提供的 `mimo-v2.5` / `https://api.slomerex.xyz`，在 Forge 65.1.1、Minecraft 26.2、JDK 25 的真实 GameTest `real_player_task_to_live_model_farm_work` 复现并修复了一条“光说不做”路径。第一次重跑中，模型已选择 `maintain_observed_crop_field`，但其参数沿用了语义观察中的 `minecraft:wheat`，旧解析器只接受 `wheat`，因此被本地严格门禁拒绝；此前同一任务还曾在第二格后持续 REPLAN，第三格保持成熟。
- 修复内容：明确农务目标在当前视野没有成熟目标时仍保留服务端已注册的 `maintain_observed_crop_field` 复合技能；当前视野有成熟作物时同时保留逐格 `harvest_and_replant_step`。复合技能仍只使用第一人称调查、有限路线和原版玩家交互，不读取隐藏区块、不猜坐标、不直接改世界。农作物参数解析只增加受限等价别名（`minecraft:wheat` 等方块 ID→短名），仍拒绝大小写错误、种子物品、坐标和范围越界。
- 定向 JVM 测试先通过；随后第二次真实 MiMo 重跑成功：模型先选择两次 `harvest_and_replant_step`，在两次 REPLAN 后选择 `maintain_observed_crop_field`（`maximumPlants=3`），三格小麦均完成原版收割/补种，GameTest `1/1` 通过，Gradle `BUILD SUCCESSFUL`（约56秒）。SQLite 事件至少包含两条原子技能链和一条复合技能链的 `ai_perception_received → model_request_started → model_response_received(HTTP 200) → decision_schema_validated → decision_revision_accepted → skill_started → low_level_actions_issued`；最终三格世界状态与收割统计门禁通过。
- 这只是当前源码的受控真实模型农务纵向切片，不升级 M1 基础生存、M2/M3/M4、随机 Hardcore、两小时通关、正式 Actor/Observer 或 PVP 统计门禁；这些仍为 `NOT_RUN`。本次进程密钥未写入源码、配置、SQLite、日志、工件或提交。
- 当前未提交改动涉及 `MinecraftPlannerInputFactory`、`FarmingSkillParameters`、`HarvestAndReplantStepSkill`、`LiveModelChatGameTests` 及对应测试；下一步先执行完整 JDK25 发布门禁和 Python E2E，再更新 `GOAL_STATE.json`、提交，并继续处理公开 GitHub 备份条件。
- 发布门禁已复跑并通过：JDK25 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker jarJar --offline` 为 16 actionable tasks，Python `unittest discover` 为 61/61，`git diff --check` 和 GOAL_STATE JSON 校验通过。当前唯一产品 JAR 为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `83208dd1fb3296b5710682d836f7d42c0b69e291e4f66c030ad95bd120dfdc7d`，17,192,735 bytes；`formalMatrixComplete=false`，M0–M4 和正式真实客户端门禁仍未运行。

## 当前恢复摘要（2026-08-13T16:16:00Z）

- 使用用户本轮临时进程凭据真实运行 `mimo-v2.5`（`https://api.slomerex.xyz`）Forge 65.1.1 / Minecraft 26.2 / JDK 25：`real_player_task_to_live_model_movement`、`real_player_task_to_live_model_zombie_defense`、`real_player_task_to_live_model_follow` 均为 GameTest 1/1 通过，分别约15.30秒、17.40秒、12.72秒。
- 移动切片中真实模型返回 `START_SKILL travel_to`；僵尸防御切片从一次模型 `ASK_PLAYER` 规划进入 `START_SKILL engage_observed_entity`，触发原版 `Monster Hunter` 进度；跟随切片返回 `START_SKILL follow_entity`。三次 SQLite 均核对到 `ai_perception_received → model_request_started → model_response_received(HTTP 200) → decision_schema_validated → decision_revision_accepted → skill_started → low_level_actions_issued(move)`；密钥未写入源码、配置、SQLite、日志或工件。
- 这些是受控的真实模型纵向切片，证明“模型决定技能并产生原版 ServerPlayer 动作”，不等于随机 Hardcore 2小时通关、十僵尸十骷髅PVP、双客户端或 M0–M4 统计门禁；这些继续为 `NOT_RUN`。
- 当前源码还补回 `PortalBuildSkills` 两个注册入口的空参数门禁；JDK25 `test --offline` 通过。完整发布门禁将在最终文档更新后重跑。
- 最终离线发布门禁已通过：Gradle 16 actionable tasks（`test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker jarJar`）和 Python E2E `61/61`；当前产品 JAR 为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `155617a967c212ddde02075b0b079c4a7c612251c16973bcc9504f9d426951d9`，17,189,892 bytes。Forge 65.1.1/MC26.2 当前兼容检查通过，但正式 Forge 补丁矩阵仍未完成。

## 当前恢复摘要（2026-08-13T16:24:00Z）

- 首次真实 MiMo 视野外突袭切片确实失败，错误为 `No value present`：测试在首个真人登录触发 AI 初始锚定的 remove/relogin 窗口中继续使用旧 `ServerPlayer` 引用。已修复场景等待权威替换身体、对在线身体缺失做有界等待，并重新运行 `real_player_chat_to_surprise_zombie_defense`；Forge GameTest 1/1 通过，约14.46秒。
- 复跑中本地20 TPS应急层在模型响应前完成转身、移动和反击，触发原版 `Monster Hunter`；随后真实模型响应选择了 `survey_surroundings`。这是“被打后立即自救”的应急证据，不是模型已经完成高层PVP决策；SQLite 仍记录过期响应被安全丢弃，不能把它升级为模型战斗因果链。
- 同轮真实金苹果切片身体确实在低血量下完成原版吃苹果并获得吸收效果，但模型决策在应急动作之后且出现过期响应；该结果只证明安全应急层，不证明模型主动选择进食。所有 M0–M4、Hardcore 2小时和双客户端门禁继续 `NOT_RUN`。

## 当前恢复摘要（2026-08-13T17:10:00Z）

- 本轮使用用户临时进程凭据运行真实 `mimo-v2.5`（`https://api.slomerex.xyz`）Forge 65.1.1 / Minecraft 26.2 / JDK 25 GameTest `real_player_task_to_live_model_nether_portal_build_and_entry`。密钥未写入源码、配置、SQLite、日志或工件。
- 真实模型先返回 `START_SKILL build_and_light_nether_portal`，随后返回 `START_SKILL find_and_enter_observed_portal`；通过第一人称当前准星、有限移动接近、原版 ServerPlayer 使用动作放置并点燃14块黑曜石，随后正常进入下界。GameTest 1/1 通过，用时55.08秒。
- SQLite 因果链已核对：`ai_perception_received → model_request_started → model_response_received(HTTP 200) → decision_schema_validated → decision_revision_accepted → skill_started → low_level_actions_issued`，随后记录 `NETHER_ENTERED`/技能完成；未出现技能失败或 SAFE_IDLE。
- 修复根因：建造技能不再复用过期语义射线命中点；生产路径只接受当前第一人称准星对已观察支撑面的命中，必要时用原版前进输入接近；支撑墙可用时以实体面为目标；底梁从远侧向身体方向施工以避免已放置方块遮挡。兼容无准星测试夹具保留语义面适配器。
- 已清理临时诊断日志和大段证据输出；`./gradlew test --offline` 通过。完整发布门禁尚待本轮最终源码清理后重跑。Forge 65.1.1/MC26.2 是当前开发线，原始 Forge 26.1.2 目标不应混写；正式 M0–M4、随机 Hardcore、双客户端 Actor/Observer 仍为 `NOT_RUN`。
- GitHub 备份仍受本机条件阻断：仓库没有 remote，`gh` CLI 不可用，当前 GitHub 连接器没有创建新公开仓库接口；提交后需要用户提供新建公开仓库 URL 或可用 `gh`/remote。

## 当前恢复摘要（2026-08-13T14:30:59Z）

- 本轮用用户临时注入的 `mimo-v2.5` / `https://api.slomerex.xyz` 在 Forge 65.1.1、JDK 25 的真实 GameTest 服务端复跑 `real_player_task_to_live_model_end_victory_and_return`。密钥只通过进程环境传入，未写入源码、配置、SQLite、日志、工件或提交。
- 末地返回门先保持为合法的未激活状态，只有服务器确认末影龙死亡后才激活；此前“模型误踏人工激活返回门而离开末地”的失败属于不公平测试夹具，已丢弃，不计入通过证据。
- 最新运行 `session 9416` 通过：模型真实返回 `START_SKILL fight_ender_dragon` 和 `START_SKILL find_and_enter_observed_portal`；审计顺序包含 `ai_perception_received → model_request_started → model_response_received(HTTP 200) → decision_schema_validated → decision_revision_accepted → skill_started → low_level_actions_issued`，随后记录 `DRAGON_KILLED`、`RETURNED_FROM_END`、`server_verified_auto_complete`，最终 `goalStatus=COMPLETED`。SQLite 中无 `skill_failed`、`stale_world_revision` 或 SAFE_IDLE 终止，Forge GameTest 1/1 通过，约 1.22 分钟。
- 根因修复包括：只允许明确声明世界/路线过渡能力的末地/传送门技能跨越一次服务器世界版本变化；完成路线将末影龙/返回传送门技能绑定到服务器验证里程碑后安全收口；公平夹具不再预先激活返回门；测试只有在目标真实 `COMPLETED` 时才结束。
- 这是一条受控的真实模型末地胜利/返回纵向切片，不是随机 Hardcore 2 小时通关，也不把正式 M0–M4、双客户端 Actor/Observer、200/1000 种子统计门禁升级为通过；这些状态继续保持 `NOT_RUN`。完整发布门禁将在本次未提交源码和文档更新后重跑。
- 随后完整 Gradle 门禁 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker jarJar` 通过（16 actionable tasks），Python E2E 契约测试 `61/61` 通过，`git diff --check` 通过。当前产品 JAR 为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `f49c905f9e4aefd37170dcff2a806501d977f4d57be7a7b854f4708102dd3753`，17,188,274 bytes。
- 当前工作树仍有本轮源码/测试改动，尚未提交；GitHub 仍无本地 remote、`gh` CLI 或创建公开仓库接口，不会上传到不明确的既有仓库。待用户提供明确的新公开仓库 URL/remote 后再备份推送。

## 当前恢复摘要（2026-08-13T14:42:00Z）

- 在提交 `8075d91` 的当前源码上再次使用临时 MiMo 凭据运行 `real_player_task_to_live_model_follow`（Forge 65.1.1/JDK25）。真实模型返回 `START_SKILL follow_entity`，SQLite 因果链为 `conversation_task_accepted → ai_perception_received → model_request_started → model_response_received(HTTP 200) → decision_schema_validated → decision_revision_accepted → skill_started → low_level_actions_issued(move)`；GameTest 1/1 通过（测试服务端约 20.26 秒）。
- 该运行只证明受控的模型跟随纵向切片和原版移动动作，最终退出时测试生命周期将目标收为 `SAFE_IDLE`，不等同于正式多人双客户端、长期跟随或 M0–M4 统计门禁；正式门禁继续 `NOT_RUN`。

## 当前恢复摘要（2026-08-13T13:56:55Z）

- 随后用同一临时 MiMo 凭据运行 `real_player_task_to_live_model_zombie_defense`：真实模型从 `ASK_PLAYER` 进入 `START_SKILL engage_observed_entity`，原版 ServerPlayer 完成攻击并击杀僵尸（获得 `Monster Hunter`），GameTest 1/1 通过、19.28 秒；这不是十僵尸十骷髅 PVP 或 Hardcore 统计门禁。
- 本轮使用用户临时注入的 `mimo-v2.5` / `https://api.slomerex.xyz` 在 Forge 65.1.1、JDK 25 的真实 GameTest 服务端重跑 `real_player_task_to_live_model_foundation_bootstrap`；密钥只存在于进程环境，未写入源码、配置、SQLite、日志或工件。
- 该真实模型切片通过：从空背包完成木材采集、基础合成、石/铁工具、食物、工作台/箱子、庇护所材料；随后以原版 ServerPlayer 移动、瞄准和放置完成 54 个结构步骤，服务端最终记录 `goalStatus=COMPLETED`、`server_verified_auto_complete`，并报告 `All 1 required tests passed`（约 11.90 分钟）。
- 因果审计包含多次真实 `model_request_started → model_response_received → decision_schema_validated → decision_revision_accepted(START_SKILL) → skill_started → low_level_actions_issued`；模型实际选择了 `gather_visible_block_cluster`、`prepare_basic_crafting`、`prepare_stone_tools`、`secure_visible_food_reserve`、`prepare_iron_toolkit`、`establish_foundation_workstations`、`prepare_foundation_shelter_materials` 和 `build_shelter_step`。没有命令、传送、直接世界/库存写入或预设蓝图注入。
- 上一轮失败根因已被真实复跑验证修复：`MoveToSkill` 完成后同一服务器 tick 的重复观察现在返回幂等 `COMPLETED`，本轮未出现 `move_to.invalid_state`、`repeated_skill_failure_without_progress` 或 SAFE_IDLE 终止。连续木材采集失败时也进入第一人称探索并找到新的树群；动态决策别名包含 MiMo 实际返回的 `gather_cluster`。
- 当前未提交改动包括：`DecisionEnvelopeValidator`/验证测试、`MoveToSkill`/测试、庇护所角色恢复/测试、基础材料连续失败探索/测试、`FoundationActionAudit` 及运行时 wiring。`git diff --check` 通过。
- 定向离线回归通过：`DecisionEnvelopeValidatorObservationTest`、`PrepareFoundationShelterMaterialsSkillTest`、`MoveToSkillTest`、`BuildShelterStepSkillTest`。真实 GameTest 本轮为 M1 基础生存切片正式通过，但不是随机 Hardcore、两小时通关、PVP、双客户端盲测或 M0–M4 统计通过。
- 发布复核已完成：`test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker jarJar` 通过（16 actionable tasks），Python E2E `61/61` 通过；唯一活动产品为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `2da0c27b0e518e28d953b3bbe5880984ecee63b85472ed4385c33994fd1bac4d`。`compat-checker` 仍为 `formalMatrixComplete=false`；Forge 26.1.2 原始目标与 Forge 65/MC 26.2 当前开发线不应混写。
- 本轮已建立本地提交（`Harden live shelter execution and wood exploration`）；恢复检查点随后仅补录上述工件和门禁证据，工作树保持 clean。
- GitHub 备份仍未推送：本地无 `gh` CLI、无 Git remote，且当前连接器没有创建公开仓库接口。不会把源码上传到不明确的既有仓库；需要用户提供新建公开仓库 URL（或准备可用 `gh`/remote）后再推送。密钥已用于测试，建议测试后轮换。

## 当前恢复摘要（2026-08-13T12:42:47Z）

- 真实 MiMo `mimo-v2.5` / `api.slomerex.xyz` 基础生存重跑使用了临时进程凭据（未落盘）。真实审计顺序包含 `model_response_received → decision_revision_accepted → skill_started → low_level_actions_issued`，并实际完成木材、基础合成、石材、食物、铁工具、工作台/箱子和 shelter 材料阶段；没有命令、传送或直接世界/库存写入。
- 第一轮基础切片因 `prepare_stone_tools` 响应在模型等待期间被精确世界版本门禁丢弃而停在上一技能；已将规划位置指纹量化为四格单元，保留维度、生命、饥饿、背包、菜单、危险和路线阶段的失效条件。该修复的 JDK25 源契约单测通过。
- 工作站技能真实日志确认新 `MOVE_TO_FIXTURE` 接近逻辑生效：身体从远处走回记忆工作台/箱子，重新获得第一人称可见后完成正常菜单事务；路线 guard 也能把同一技能区分为 `WORKSTATIONS_ESTABLISHED` 与 `SUPPLIES_STORED`，不再自锁循环。
- 当前真实切片的首个未通过点是 shelter 屋顶施工：`BuildShelterStepSkill` 在多个公平站位反复得到 `crosshair_wrong_block`/`No shelter aim vantage`，随后仍在受限重规划，未完成最终 shelter 验收。本轮手动停止运行，不能宣称 M1 完成。
- 定向 JDK25 测试（planner、brain、route、workstation、decision codec/validator、observation source contract）通过；真实 Forge GameTest 本轮为部分阶段通过、shelter 未完成。正式双客户端 Actor/Observer、随机 Hardcore、2小时通关、M0–M4 统计仍 `NOT_RUN`。
- 随后执行 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker jarJar` 全部通过（16 actionable tasks）；兼容检查仍报告 `formalMatrixComplete=false`。当前唯一产品 JAR 为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `03a0dab67e98c8ccf8425e3a366a5b7979d2f0c4bccf222d66f39d8b11222df6`。
- 本轮工作树无密钥命中；已生成此检查点。下一步是修复 shelter 施工的公平站位/瞄准恢复，并重新跑真实模型切片；发布前仍需重新打包和完整 release gate。
- 本轮已在本地提交 `853bf55`（`Harden live model action routing and fixture movement`）；当前没有 remote，因此尚未推送到 GitHub。

## 当前恢复摘要（2026-08-13T12:18:25Z）

- 本轮继续使用用户临时提供的 `mimo-v2.5` 与 `https://api.slomerex.xyz`；密钥只注入真实 GameTest 进程环境，未写入配置、SQLite、日志、源码、提交或工件。
- 真实模型基础生存切片已实际完成木材、基础合成、石材、食物和铁资源阶段；随后工作站/储存复合技能在 Forge 65.1.0/65.1.1 运行中停滞，另一轮因供应商响应超过硬期限超时。尚未有完整基础生存 GameTest PASS，因此不能宣称 M1 或“光说不做”已彻底解决。
- 已修复路线取消自锁：路线守卫取消技能后不再在同一 tick 内调用 `skills.tick()`；并根据 `nextObjectives` 将同一工作站技能区分为 `WORKSTATIONS_ESTABLISHED` 与 `SUPPLIES_STORED`，避免储存阶段被静态里程碑反复取消。
- 已修复 `MinecraftPlannerInputFactory` 的能力边界回归：最后一次路线能力重应用现在仅在存在服务端已验证 FOUNDATION/COMPLETION 路线时使用完整模型技能；普通观察绑定任务保留收集、跟随、收割和食品交接的窄技能面。定向 JDK25 回归为 BUILD SUCCESSFUL。
- 当前定向测试命令覆盖 `MinecraftPlannerInputFactoryTest`、`BrainOrchestratorTest`、`SurvivalRouteTrackerTest`、`EstablishFoundationWorkstationsSkillTest`、决策编解码/校验测试，全部通过（Gradle 4 actionable tasks）。完整 Forge、真实双客户端 Actor/Observer、Hardcore 随机种子、2 小时通关和 M0–M4 统计仍为 `NOT_RUN`。
- 工作树包含本轮源码和测试改动，尚未提交。GitHub 连接器已确认登录 `weidakuang`，但当前插件没有“创建新仓库”接口，本机也没有 `gh` CLI，且本地没有 remote；不能伪造公开仓库或把代码写入其他项目。后续需用户在 GitHub 创建一个明确的公开仓库，或提供可用 `gh`/remote 后再推送。
- 下一步优先：修复工作站技能“看见已记忆工作台但未移动过去”的实际动作停滞，补充无真人启动后延迟首登的专用回归证据，然后重新运行真实 MiMo 工作站/基础生存切片；通过后再打包并执行发布门禁。

## 当前恢复摘要（2026-08-13T10:24:00Z）

- 农场真实模型首跑在 Forge 65.1.1/JDK25 下按实际请求选择了
  `harvest_and_replant_step`，但模型把木材采集技能的
  `blockId/maxBlocks/clusterRadius/toolItemId` 参数混入农场调用；本地
  严格参数门禁连续拒绝并以 `model_failures_exhausted` 安全终止。该次是
  可复现的模型参数形状错误，不是网络或测试脚本故障。
- 已改 `MinecraftPlannerInputFactory.java`：在当前第一人称语义看到同一
  类型成熟作物且任务明确要求收割并补种时，把模型技能面收窄到
  `harvest_and_replant_step`；路由提示明确要求仅发送
  `dimension,crop,sampleSequence,x,y,z,face` 七个字段，并禁止木材技能
  字段。已改 `MinecraftPlannerInputFactoryTest.java`，验证技能白名单、
  成熟年龄和提示契约。
- 重新使用用户本轮临时提供的 `mimo-v2.5`/`api.slomerex.xyz` 真实凭据
  运行 `real_player_task_to_live_model_farm_work`：模型先返回规划确认，
  随后实际接受三次 `START_SKILL harvest_and_replant_step`，分别绑定
  `sampleSequence=21/43/108` 与三格小麦坐标，Forge GameTest 1/1 通过，
  用时约 37.5 秒；未记录响应正文或密钥。该结果证明本农务切片的模型→
  技能→原版收割/补种链已通过，不升级为 M1–M4 统计门禁。
- 定向 JDK25 `MinecraftPlannerInputFactoryTest` 通过；正式 Linux/Xvfb
  双客户端 Actor/Observer、随机 Hardcore、2 小时通关及 M0–M4 统计仍为
  `NOT_RUN`。下一步提交本修复并继续验证模型驱动的战斗/移动/维度链，
  同时保留失败日志与因果审计，不用无模型测试替代真实模型证据。
- 重新运行 `jarJar` 后，唯一可安装产品为
  `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 为
  `8a8af4923f9d2572a7ffb1273b260c0c0e97c5f44a1f5c5373bca709259fca68`；
  已从包内 class 常量核对本次农场 handoff 文本存在。
- 同一临时模型凭据随后重跑 `real_player_task_to_live_model_movement`
  与 `real_player_task_to_live_model_zombie_defense`：前者实际选择
  `START_SKILL travel_to` 并完成 ServerPlayer 原版移动，后者在本地
  `GUARDING` 应急反射期间实际选择 `START_SKILL
  engage_observed_entity` 并击杀僵尸；两项 Forge GameTest 各 1/1
  通过（约 14.49 秒、19.26 秒）。这些是当前模型→技能→身体动作切片，
  不是 PVP 或随机 Hardcore/M0–M4 统计证明。
- JDK25 离线发布门禁 `test check verifyReleaseJar e2eClientJar
  e2eOracleJar compat-checker` 已通过（16 actionable tasks）；兼容性检查
  仍明确 `formalMatrixComplete=false`，因此不会把 Forge 65 运行时 smoke
  或这些受控 GameTest 升级为全版本/正式统计通过。

## 当前恢复摘要（2026-08-13T09:00:00Z）

- 使用本轮用户临时提供的 `mimo-v2.5`/`api.slomerex.xyz` 真实凭据完成了“从可见箱子取物”Forge 65.1.1/JDK25 切片；密钥仅存在于测试进程环境，没有写入配置、SQLite、日志、提交或工件。
- 真实失败根因已确认：初始锚定的 ServerPlayer 正常 remove/relogin 期间，箱子场景把目标提交给旧实体；修复夹具改为让人类观察者保持在线，等待当前权威 AI 实体重登、重新朝向同一箱子并获得新一帧公平可见语义后才提交聊天。所有实体/菜单读取改为等待期间的 Optional，不再以 `No value present` 假设实体连续存在。
- 另一个真实模型失败根因是模型在看到箱子后反复返回 `survey_surroundings`/无效 `use_block`，没有执行动作。`MinecraftPlannerInputFactory` 现在仅在第一人称语义明确看到目标容器时提供 `use_block`；打开菜单并观察到请求物品和空玩家槽后，仅提供 `transfer_menu_item`。没有可信证据时保持完整技能面，不猜坐标、槽位或方块。
- 修复后的真实运行日志顺序为：模型 `START_SKILL use_block`（sampleSequence=3，west 面）→模型 `START_SKILL transfer_menu_item`（sampleSequence=24，containerId=1、sourceSlot=0、destinationSlot=27、count=3）→原版菜单事务完成；GameTest `real_player_task_to_live_model_container_withdrawal` 1/1 通过，11.30 秒。
- 本轮新增/修改文件：`LiveModelChatGameTests.java`、`MinecraftPlannerInputFactory.java`、`MinecraftPlannerInputFactoryTest.java`。目标阶段测试已通过；正式 Linux/Xvfb 双客户端 Actor/Observer、Hardcore 随机种子和 M0–M4 统计门禁仍为 `NOT_RUN`，不能把单切片绿灯宣称为完整通关或专业陪玩完成。
- 下一步：提交这组观察绑定修复，继续用同一临时模型做跑酷/水桶自救或农务等真实动作切片；每次结果都记录模型决策、技能开始和原版世界变化，失败则按实际日志修复，不使用无模型脚本结果冒充模型证据。

## 当前恢复摘要（2026-08-13T10:03:00Z）

- 跑酷真实切片首跑曾在首个真人登录触发 AI 初始锚定 remove/relogin 的瞬间以 `No value present` 失败；这不是模型动作证据。跑酷夹具的 BODY、SETTLE、RUN 阶段现在在权威实体短暂不存在时等待替换实体，不把正常生命周期空窗当作模型失败。
- 修复后使用同一临时 `mimo-v2.5` 凭据执行 `real_player_task_to_live_model_parkour`：模型实际返回 `START_SKILL parkour_to`（目标 `-320.5,-60,-313.5`、`maxJumps=3`、`maxGap=1`），ServerPlayer 通过原版跳跃跨越三个缺口，未填补缺口、未受伤；Forge GameTest 1/1 通过，28.64 秒。模型期间出现多次有效重试/技能租约更新，但最终到达条件和 vanilla jump 统计均满足。
- 本轮已验证的真实模型切片累计包括：移动、跟随、僵尸防御、低血量食用、可见掉落物收集、可见箱子开箱+菜单转移、跑酷。它们仍是受控单切片，不等同于随机 Hardcore 通关或 M0–M4 统计门禁。
- 当前下一步：提交跑酷生命周期修复后继续验证水桶自救/落差控制；若模型选择不动作，记录其原始合法决策和身体/伤害结果再修复，不用本地脚本替模型完成普通目标。

## 当前恢复摘要（2026-08-13T10:28:00Z）

- 水桶真实切片首两次失败均是测试参与者生命周期问题，而非模型证据：聊天提交后立即断开会让异步对话没有产生 `conversation_task_accepted`；把测试玩家保持在线后，又发现默认 `(0,0,0)` 登录点会让它在模型等待期间死亡，导致初始锚点被清空。现在测试玩家在预置落地点上方的安全站立坐标登录，并保持在线直到目标 revision 真正接受。
- 修复后 `real_player_task_to_live_model_water_clutch` 使用同一临时模型通过 Forge 65.1.1/JDK25 运行，11.63 秒。审计到模型真实响应为 `ASK_PLAYER`、随后 `REPLAN`，没有把水桶技能及时交给规划器；本地 20 TPS 应急生存层随后依次进入 `EQUIPPING_WATER`、`PREPARING_WATER`、`DEPLOYING_WATER`、`BRACING_FALL`，通过原版 ServerPlayer 装备和放水完成自救。GameTest 验证水方块、空水桶、下落观测和零摔落伤害。
- 该结果明确证明“模型不动作时身体不会站着等死”的安全底线，但不是模型主动完成水桶技能的通过；后续要单独修复水桶任务的观察绑定/技能交接，使模型能在正常时间窗选择 `water_clutch`，同时保留应急抢占。
- 当前真实模型动作切片已覆盖移动、跟随、战斗、食用、掉落物、箱子事务、跑酷和应急水桶；正式双客户端 Actor/Observer、Hardcore 随机种子、M0–M4 统计门禁仍为 `NOT_RUN`。

## 当前恢复摘要（2026-08-13T09:36:00Z）

- 本轮已使用用户临时注入的 `mimo-v2.5` 与 `api.slomerex.xyz` 做真实 provider 探测；密钥只存在于进程环境，没有写入配置、世界、SQLite、日志、提交或工件。原样裸主机先前会请求 `/responses` 并被网关 200 包装体误判为 `MALFORMED_RESPONSE`；`EndpointValidator` 现在把无路径主机规范为 `/v1`，`EndpointValidatorTest` 已覆盖。原样 URL 的能力探测通过，实际验证到 Responses/函数调用能力，未暴露响应正文或凭据。
- 真实 Forge 65.1.1/JDK25 模型切片（同一临时凭据）结果：`real_player_task_to_live_model_movement` 通过（模型实际返回 `START_SKILL travel_to`，ServerPlayer 完成原版移动）；`real_player_task_to_live_model_follow` 通过（实际返回 `START_SKILL follow_entity`，连续跟随完成）；`real_player_task_to_live_model_zombie_defense` 通过（本地应急先 `GUARDING`，模型实际选择 `engage_observed_entity` 并击杀目标）；`real_player_chat_to_critical_golden_apple` 通过（真实掉落/拾取/低血量，模型选择 `consume_owned_food`，原版食用并产生吸收效果）。这些是实际模型+Forge ServerPlayer 的动作证据，不是 M0–M4 统计门禁。
- 本轮暴露并修复两个真实竞态：首个真人登录时 AI 初始锚定会触发正常 remove/relogin，移动/金苹果夹具不能对短暂空的 `PlayerList` 使用 `orElseThrow()`；更严重的是旧连接尚未从 `PlayerList` 移除时立即重登会触发 `uuid_already_online` 且永不重试。`AiPlayerManager` 现在记录真实锚点，等待旧 UUID 真正移除后在 server tick 通过普通 `PendingPlayerSpawn` 重登；移动和金苹果场景都等待权威实体，不传送、不创建替身。
- 定向回归：`AiPlayerManagerInitialAnchorSourceContractTest`、`EndpointValidatorTest`、`ModelApiAuthenticationTest` 通过；Forge 65.1.1/JDK25 `delayed_human_login_after_zero_human_active` 1/1 通过。金苹果首跑的 `No value present` 已由上述竞态修复，重跑通过。
- 最近失败门禁：真实移动切片首跑因上述重登竞态失败，随后修复后通过；黄金苹果首跑因把物品交给旧 ServerPlayer 失败，随后修复后通过。正式 Linux/Xvfb 双客户端 Actor/Observer、Hardcore 随机种子、M0–M4 统计仍为 `NOT_RUN`，不能用这些单切片绿灯宣称通关或专业陪玩完成。
- 完整离线构建门禁随后通过：JDK25 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`，16 actionable tasks；Python E2E 61/61 通过。重新打包的唯一产品 JAR 为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `79e6fd0bb50013280519bdb3cd828578a33f59afe580d423346df5acbd1b75bc`。`compat-checker` 仍明确 `formalMatrixComplete=false`，不得宣称 Forge 全矩阵或 M0–M4 已完成。
- 当前工作树有本轮代码和检查点未提交改动；下一步提交本轮变更并保留可追溯审计。GitHub 公共仓库仍需可创建仓库的 GitHub CLI/权限，不能伪造已上传。

## 当前恢复摘要（2026-08-12T13:09:32Z）

- 本轮版本门禁根因已修复：`e2e/test_orchestrator.py` 仍把工件版本硬编码为旧的 `0.1.6-dev-mc26.2`，导致版本推进到 `0.1.9-dev-mc26.2` 后出现假失败。测试现在从当前 `build.gradle` 读取并校验开发版本格式与源码绑定；`e2e/test_worker_protocol.py` 也改为动态读取当前版本，`e2e/README.md` 的活动示例同步到 `0.1.9-dev-mc26.2`。
- `.gitignore` 新增 `run-debug*.log`，避免本地 GameTest 调试日志进入备份仓库；没有删除任何用户文件或运行产物。
- 定向 Python E2E 全部通过：`python3 -m unittest discover -s e2e -p 'test_*.py'`，61/61，0.951 秒。此前 JDK25/Forge 65.1.1 全量 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`、延迟首登锚定、应急、跑酷、金苹果和跟随切片结果保持不变；当前产品 JAR 仍为 `build/libs/mcai_companion-0.1.9-dev-mc26.2.jar`，SHA-256 `42a24bbef9b46d129aee65b79449b03ebc1337ee9dfd6a4c9be83a00447e1ac2`。
- 最新 `python3 e2e/orchestrator.py preflight --forge-version 65.1.1` 仍 fail-closed：Darwin、无 Xvfb、无当前进程模型配置/授权密钥；`ready=false`，没有发送 provider 请求，也没有把预检当作玩法通过。
- 版本门禁修正后的定向 JDK25 测试再次通过：`ModelApiAuthenticationTest`、`BrainOrchestratorTest`、`MinecraftPlannerInputFactoryTest`，Gradle `BUILD SUCCESSFUL`（4 actionable tasks，3 秒）。
- GitHub 备份尚未执行：连接器确认已登录 `weidakuang`，但它没有创建新仓库的接口；本机也没有 `gh` CLI，当前仓库没有 remote/首个提交。按发布规范不能假装创建公开仓库或把代码推入已有的其他项目；代码继续保留在本地，下一步需要可创建仓库的 GitHub CLI/权限或一个明确的新仓库目标后再提交并推送。
- 本地可追溯备份已建立：分支 `agent/minecraft-ai-companion-0.1.9`，当前 HEAD 提交消息为 `MCAI 0.1.9 development snapshot`。工作树干净；该提交不包含 `build/`、调试日志、密钥或世界运行数据。
- 正式模型与双客户端 Actor/Observer、Hardcore、M0–M4 统计门禁仍 `NOT_RUN`；历史 MiMo 401 凭据不重用、不写入日志或仓库。

## 当前恢复摘要（2026-08-12T13:05:00Z）

- 本轮先核对了用户反馈的“重进世界后 API Key 失效”路径：`CompanionRuntime.onServerStarted` 会创建新的服务端 `ModelRuntime`，随后 `ModelBootstrapCoordinator.requestOrdinaryStartupRestore()` 在服务端线程登记一次启动恢复；`ModelRuntime.prepareConfiguredProfile()` 在后台从 macOS Keychain、Windows DPAPI、Linux Secret Service 或显式环境注入恢复凭据，并优先安装已验证的非秘密能力缓存。没有缓存时才进行一次受控探测，失败不会重复扣费；设置页面只等待本地恢复后显示状态。未发现需要再次输入 Key 的生产分支。
- 定向 JDK25 测试通过：`ModelRuntimeTest`、`ModelBootstrapCoordinatorTest`、`ApiKeyManagerRestartPersistenceTest`、`PlayerTaskIntentTest`、`ConversationGroundingTest`，Gradle `BUILD SUCCESSFUL`（4 actionable tasks）。这些是跨实例/启动恢复和编排合同证据，不是实时供应商证据。
- 无真人服务器首登锚定回归已在 Forge 65.1.1/JDK25 通过：`delayed_human_login_after_zero_human_active` 1/1（2.441 秒），`delayed_human_login_while_emergency_active` 1/1（4.297 秒）。后者确认应急生存占用时不会移除身体，威胁清除后才通过正常 remove/relogin 生命周期完成一次性初始锚定。
- 当前仍无有效、获准用于自动化的模型凭据；历史 MiMo 凭据探测为 401，未在本轮重用或记录任何密钥。因此真实 provider、双客户端 Actor/Observer、Hardcore、PVP、M0–M4 统计门禁继续保持 `NOT_RUN`，不得用这些离线/无模型绿灯替代。
- Forge 65.1.1 当前源码实体动作切片随后通过：`real_emergency_iron_golem_duel` 1/1（6.211 秒）、`real_emergency_zombie_skeleton_horde` 1/1（6.171 秒，真实近战/受伤/位移约束）和 `real_parkour_course` 1/1（26.88 秒，真实跳跃、碰撞与落地）。三者均为真实 `ServerPlayer` 的无模型本地安全/物理证据；不能升级为实时模型 PVP、客户端盲测或 M0–M4。
- 下一步：继续完善模型请求→决策→技能→低层动作因果链，并对通用任务的 speech-only/SAFE_IDLE 只做可审计的重规划与诚实停机，不凭空替模型选择砍树、战斗或路线，也不把“收到”当作技能开始。

## 当前恢复摘要（2026-08-12T13:18:00Z）

- 本轮生产改动：`MinecraftPlannerInputFactory` 暴露与食品 schema 共用的只读 `ImmediateFoodHandoff`；`BrainOrchestrator` 在模型已经返回合法但无动作的 `CONTINUE`、`REPLAN`、`ASK_PLAYER`、`SAFE_IDLE` 或过早 `COMPLETE_GOAL` 时，仅对明确的玩家/MCP 食用任务恢复当前公平证据对应的 `collect_observed_item` 或 `consume_owned_food`。跟随恢复优先，通用砍树、战斗、寻路仍不由本地代码替模型选技能。
- 新增回归：拥有金苹果时 speech-only “我先留着”会启动原版食用技能；只看到玩家丢下的金苹果时 ASK_PLAYER 会启动原版观察掉落物拾取技能；两条都要求正常 `SkillSupervisor` 参数校验和 `SKILL_STARTED` 审计。`BrainOrchestratorTest` 与 `MinecraftPlannerInputFactoryTest` 定向通过。
- 版本推进为 `0.1.9-dev-mc26.2`，尚未重新打包最终 JAR。源码仍为 dirty/non-release；重新打包后需更新 SHA-256 并再次执行 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`。
- `0.1.9-dev-mc26.2` 已重新打包：`build/libs` 仅有一个活动产品 JAR，大小 17,171,586 bytes，SHA-256 为 `42a24bbef9b46d129aee65b79449b03ebc1337ee9dfd6a4c9be83a00447e1ac2`；旧 0.1.8 已可恢复地归档到 `build/archive-libs`。JDK25 全量 `test check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker` 通过（16 actionable tasks）；compat checker 明确仍为 `formalMatrixComplete=false`。

## 当前恢复摘要（2026-08-12T12:42:23Z）

- 当前修复根因：普通“帮我砍点树/采木头”仍让模型同时看到旅行、菜单和其他无关技能，容易回复承诺而没有技能租约。现已在公平第一人称射线看见完整原木/木头/树干表面时，把模型技能面收窄到 `gather_visible_block_cluster`；没有当前表面证据时不猜坐标，继续要求刷新/调查。模型仍决定精确种子、方块类型、数量、半径和工具，技能逐帧用原版路径复核。
- 本次已改文件：`MinecraftPlannerInputFactory.java` 新增观察绑定的可见木材采集 handoff，并把阶段/观察白名单改成显式有序流水线，避免新增 handoff 意外重新加入阶段已退役技能；`MinecraftPlannerInputFactoryTest.java` 新增可见木材与无证据回归。此前绑定跟随、可见掉落物、过早 `COMPLETE_GOAL` 恢复仍保持。
- 最近失败门禁：木材回归第一次把测试语义 JSON 与 trusted JSON 传反，导致预期的收窄测试误报全量技能；已修正测试输入并通过。没有生产编译失败遗留。
- 最近定向验证：JDK25/Forge 65.1.1 `MinecraftPlannerInputFactoryTest` + `BrainOrchestratorTest` 通过；真实 Forge ServerPlayer 的 `real_player_chat_to_immediate_bound_follow` 1/1 通过（1.316 秒，无模型）；`real_emergency_zombie_skeleton_horde`（10 僵尸 + 10 骷髅）1/1 通过（1.189 秒，无模型）；完整离线 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker` 通过（16 actionable tasks）。这些只证明合同、身体调度和本地紧急生存，不是实时 provider、渲染客户端或正式 PVP 证据。
- 版本与工件：开发版本已推进到 `0.1.8-dev-mc26.2`。唯一可安装构件是 `build/libs/mcai_companion-0.1.8-dev-mc26.2.jar`，SHA-256 `5be8d2033f1fcb4d29cea563253f190e75869f1b2488a3cb5a76e8b798d2636f`，17,168,587 bytes；旧 `0.1.7` 已自动移动到 `build/archive-libs/`，没有在 `build/libs` 留下重复 Mod。
- 下一步：继续处理一般任务在真实模型返回 speech-only/错误决策时的因果审计；之后在合规 Linux/Xvfb 环境、用新近授权的有效模型配置运行 Actor/Observer 因果链。没有该环境与凭据前，真实模型聊天→动作、Hardcore、M0--M4 和统计门禁保持 `NOT_RUN`。

## 当前恢复摘要（2026-08-12T08:41:24Z）

- 当前修复根因：高层规划提示词把真实语义 JSON 中实体元数据字段 `properties` 错写成 `visibleProperties`。这会让模型在明明有公平可见玩家/掉落物的情况下读不到 `playerName` 或 `itemId`，表现为“答应跟随/拾取却不动作”。另一个缺口是“给金苹果快吃”在拾取后可被模型一句 `COMPLETE_GOAL` 提前结案。
- 已改文件：`MinecraftPlannerInputFactory.java` 统一提示词字段名，新增“可见食物掉落 → 原版拾取 → 已拥有食物 → 原版食用”的观察绑定交接和阶段性技能白名单；`BrainOrchestrator.java` 只在 `consume_owned_food` 的库存变化验证完成后允许该类明确金苹果任务完成；`PlayerTaskIntent.java` 保留“给金苹果”任务的自然第二句“那你就快吃吧/吃了吧/eat it now”上下文，避免丢失物品语义；`CompanionConversationCoordinator.java` 对有当前第一人称峡谷/崖壁/洞穴样线索时的“只是一堆石头”虚构回复做保守纠正。测试覆盖这些路径。`build.gradle` 同时修复了版本提升首次触发时旧 JAR 归档的 Groovy 换行解析错误。
- 版本与工件：开发版本已从 `0.1.5` 推进到 `0.1.6-dev-mc26.2`，唯一可安装构件是 `build/libs/mcai_companion-0.1.6-dev-mc26.2.jar`，SHA-256 `e339c8cd3af297cc5e904a90b56992b8c5a88050775719fea3615ce8112eb6a8`，17,163,610 bytes；旧 `0.1.5` JAR 已移动到可恢复 `build/archive-libs/`，没有在 `build/libs` 留下重复 Mod。
- 最近验证：JDK25 `BrainOrchestratorTest`、`MinecraftPlannerInputFactoryTest`、`ConversationGroundingTest` 通过；完整离线 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker` 成功（16 actionable tasks）；Python E2E 61/61、突变门禁 10/10 caught、JSON 与 diff 检查通过。
- 最后失败/未运行门禁：没有发送任何 provider 请求。Darwin 本机仍缺 Linux/Xvfb 双真人客户端运行环境和新近授权的自动化模型配置，因此真实模型聊天→动作、渲染客户端、Hardcore、M0--M4 与统计门禁仍 fail-closed `NOT_RUN`；本轮离线测试不能替代它们。下一步是用该精确 `0.1.6` JAR 在授权 Linux/Xvfb worker 执行真实 Actor/Observer 因果链，再根据结果修复实际模型输出问题。

## 当前恢复摘要（2026-08-12T08:10:35Z）

- 当前修复根因：Windows DPAPI 与 Linux Secret Service 已成功保存并完成验证的凭据，被设置页的旧状态映射错误写成“仅当前进程”；这会让用户误以为重进世界必然丢失 API Key，并重复覆盖一个本来可恢复的密钥。
- 已改文件：`ModelSetupModule.java` 现在按实际持久化存储返回 macOS Keychain、Windows DPAPI、Linux Secret Service 或通用安全存储状态；`ModelSetupScreen.java` 与中英文语言文件显示对应且不含密钥的提示；`ModelSetupCredentialStatusTest.java` 覆盖三平台、未来持久化存储和无安全存储回退。
- 最近验证：JDK25 定向模型设置/运行时测试通过；完整离线 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker` 成功（16 actionable tasks）；Python E2E 61/61、突变门禁 10/10 caught，语言 JSON 可解析。
- 当前开发构件：`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 `5b9d7a9d5737ec351049a58ccf45702b33e218db8154a5ec3b9b91f3dee6eb3f`，17,154,299 bytes；源码仍 `DIRTY_NO_COMMIT`、构件仍 `NON_RELEASE`。
- 此修复不发送模型请求，不能替代真实模型、Linux/Xvfb 双客户端、Hardcore 或 M0--M4 正式门禁；这些继续是 fail-closed `NOT_RUN`。下一步回到玩家消息→模型决策→技能→原版动作的正式链路，并在合规、授权的真实模型环境中运行。

## 当前恢复摘要（2026-08-12T07:58:00Z）

- 当前修复的黑盒证据根因：旧验证器只按时间邻近关联聊天与模型动作，且把不同精度的 ISO 时间当字符串比较；理论上旧目标或无关请求可以误满足“聊天→模型→技能→动作”链。现在每个已接纳的聊天任务都会写入不含原文的 `conversation_task_accepted` 审计行（发送者 UUID、标准化聊天的 SHA-256、意图码和 `goalRevision`），Oracle 独立记录同一正常聊天的 UUID；验证器要求三者及目标版本一致，并使用解析后的 UTC 时间比较。
- 同时已把一般规划器的首次无动作 `CONTINUE`/`REPLAN` 立即写入 `planner_no_action` 纠错码，让下一次模型请求明确要求从服务器允许的技能中选取 `START_SKILL`；它不为砍树、战斗或移动凭空选择技能，模型与公平感知仍是普通任务的唯一高层决策源。
- 本轮已改文件：`BrainEvent.java`、`CompanionConversationCoordinator.java`、`MinecraftBrainEventSink.java`、`MinecraftPlannerInputFactory.java`、`BrainOrchestrator.java`、E2E Oracle/编排器及其 Java/Python 回归测试。
- 最近验证：离线 JDK25 完整 `./gradlew test` 通过；`e2eOracleJar`/`e2eClientJar` 通过；Python E2E 60/60 通过；`mutation_gate.py` 为 10/10 caught。一次联网 Gradle 配置阶段因 Minecraft 启动清单连接超时未执行测试，随后已用现有缓存离线重跑成功。
- 当前开发构件为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为 `172c0bcaa953f91e12504d3c1a1312ddfa70e85bd7aa1c3ac58f352a02a99bab`，17,153,658 bytes；源码仍 `DIRTY_NO_COMMIT`、构件仍 `NON_RELEASE`。
- 最后正式门禁仍是 fail-closed `NOT_RUN`：本机是 Darwin，缺少 Linux/Xvfb 离屏 Actor/Observer 环境，也没有新近确认可用于自动化的授权模型配置；本轮没有发送 provider 请求。因此 M0--M4、真实模型、双客户端渲染、Hardcore 和统计门禁均未提升。
- 下一步：在合规 Linux/Xvfb worker 上使用有效、获准自动化的凭据运行精确 JAR 的 `server-smoke → anchor-smoke → real_client_chat_follow_inventory`，验证同一 UUID/消息摘要/目标版本上的 `model_response_received → decision_revision_accepted → skill_started → low_level_actions_issued`，再推进真实 M1--M4 场景与统计。

## 当前恢复摘要（2026-08-11T14:47:05Z）

- 当前 bug 根因：已确认的“光说不做”分支是已授权跟随目标被模型误答为
  `SAFE_IDLE` 后直接进入等待/终止；已在
  `BrainOrchestrator.java` 加入仅限绑定玩家跟随的公平样本恢复，并在
  `BrainOrchestratorTest.java` 加回归测试。
- 当前已改并验证的关键文件还包括首登锚定的
  `AiPlayerManager.java`、软期限 wiring 的 `CompanionRuntime.java`/
  `CompanionConversationCoordinator.java`，以及对应源契约测试。
- 最后正式门禁结果不是失败而是 fail-closed `NOT_RUN`：当前 Darwin 没有
  Linux/Xvfb，也没有授权的 `MCAI_BASE_URL`、`MCAI_MODEL` 或密钥，因此没有
  启动离屏 Actor/Observer，也没有发送新的 provider 请求。
- 下一步：在合规 Linux/Xvfb worker 注入授权模型后，按
  `server-smoke → anchor-smoke → real_client_chat_follow_inventory` 的
  因果顺序验证 `model_response_received → decision_revision_accepted →
  skill_started → low_level_actions_issued`；在资源出现前不把任何内环
  GameTest 绿灯升级为 M0--M4 正式通过。

本轮最新 `python3 e2e/orchestrator.py preflight --forge-version 65.1.1`
仍为 fail-closed `ready=false`：Darwin 主机没有 Linux/Xvfb，且本进程没有
`MCAI_BASE_URL`、`MCAI_MODEL` 或授权密钥；因此没有发送 provider 请求。

随后在全新 Forge 65.1.1/JDK25 工作目录运行了
`mcai_companion:real_emergency_iron_golem_duel`
（`run-debug315-emergency-golem-current`）。GameTest 1/1 通过，测试用时
6.159 秒；真实测试玩家统计记录 36 点承伤和 280 点造成伤害，确认这不是
站桩或只检查聊天的假绿灯。该切片只验证 ServerPlayer 的本地应急战斗、
原版近战与生存反射，不是实时模型 PVP、渲染客户端、Hardcore 或 M0--M4
门禁。关闭时的 closed embedded channel 警告属于测试连接正常回收。

当前开发 JAR 仍为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为
`498c5e44fa782f1683840711c5b6b63dd2b6aa874625e1afea80c8c76251b960`，
17,151,193 bytes；源码仍 `DIRTY_NO_COMMIT`，产物仍 `NON_RELEASE`。

本轮收尾校验：JDK25 `./gradlew test` 通过（4 actionable tasks，其中测试
任务为 up-to-date），Python E2E `59/59` 通过，`GOAL_STATE.json` 可解析；
M0--M4 及正式模型/客户端/Hardcore 门禁仍逐项保持 `NOT_RUN`。

随后又在同一当前源码和 Forge 65.1.1/JDK25 下重跑了用户指定的十僵尸加
十骷髅压力场：`real_emergency_zombie_skeleton_horde`，目录为
`run-debug316-emergency-horde-current`。GameTest 1/1 通过，6.157 秒；
真实测试玩家统计记录 47 点承伤和 221 点造成伤害。该夹具还要求多个目标
真实掉血、身体位移和原版剑耐久变化，因此站着“用眼神攻击”不能通过；它
仍是无模型本地应急/PVE证据，不是实时模型 PVP、随机种子或正式门禁。

## 2026-08-11 continuation: Forge 65 lifecycle-performance floor/latest recheck

The current source completed the same real Forge lifecycle GameTest on both
ends of the declared Forge 65 range. Forge 65.1.1 in
`run-debug309-lifecycle-current` passed 1/1 with 6,126 samples (average
300,549 ns, rolling p95 1,432,750 ns, window maximum 22,004,041 ns). Forge
65.0.0 in `run-debug310-lifecycle-floor` passed 1/1 with 5,755 samples
(average 316,992 ns, rolling p95 1,357,500 ns, window maximum 32,314,625 ns).
This is real server-side headless-player/vanilla lifecycle evidence and
compatibility coverage only. It uses no model and no rendered client, so the
formal Actor/Observer/model, Hardcore and M0--M4 gates remain `NOT_RUN`.
The earlier lifecycle p95 failures are retained as historical audit evidence;
this run records the current source after the hot-path and survival fixes.

## 2026-08-11 continuation: bound-follow SAFE_IDLE recovery and artifact revalidation

A model response of `SAFE_IDLE` for an already authorized, server-bound player
follow goal could previously leave the body motionless. The current
`BrainOrchestrator` accepts that response as a recovery candidate only for
that narrow goal, only from the fair visible-player sample (or its bounded
survey reacquisition), and then uses the ordinary skill supervisor and
vanilla actuator. It does not invent a skill for general tasks or alter
Hardcore/evaluation authority. The new regression and the full JDK25 JVM suite
pass; Python E2E is 59/59. Package/compatibility (`check verifyReleaseJar
e2eClientJar e2eOracleJar compat-checker`) passes 16 actionable tasks. The
current development JAR is
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`498c5e44fa782f1683840711c5b6b63dd2b6aa874625e1afea80c8c76251b960`,
17,151,193 bytes. No provider request was made and formal real-client/model,
Hardcore and M0--M4 gates remain `NOT_RUN`.

Forge 65.1.1 then reran the no-model physical follow selector after the
planner change: `run-debug311-follow-after-safeidle` passed
`real_player_chat_to_immediate_bound_follow` 1/1 in about 1.39 s. The log
shows the server-bound chat goal, stable headless-body replacement and normal
vanilla follow completion. It does not prove a provider response or a
rendered client.

The same continuation added a production completion guard: the server
completion verifier disallows model `COMPLETE_GOAL` self-certification for
PLAYER_CHAT/MCP gameplay goals until a real local skill lease has started.
Unverified claims remain running and are replanned; verified route milestones
and ordinary recovery goals keep their existing completion semantics.

After that guard was rebuilt, Forge 65.1.1 reran the physical
`real_player_chat_to_immediate_bound_follow` slice as
`run-debug312-follow-after-completion-guard`. It passed 1/1 in about 1.431 s
with the real ServerPlayer/PlayerList path. This is still provider-disabled
inner-loop evidence: it does not prove a model response, a rendered client,
or any Hardcore/M0--M4 gate.

The server-side event sink now also broadcasts one explicit correction when
the production completion guard rejects a model `COMPLETE_GOAL` before any
skill lease starts. The player is told that no action has executed and that
planning is being retried, so an earlier “task accepted” line cannot look like
a silent successful completion. Locked evaluations suppress this social
status; it does not grant world authority.

The current source then reran both delayed first-login anchor slices on Forge
65.1.1/JDK25: `run-debug313-delayed-anchor-normal` passed 1/1 in 2.448 s,
and `run-debug314-delayed-anchor-emergency` passed 1/1 in 4.308 s. The first
verifies an ACTIVE zero-human body is removed/relogged beside the first human;
the second verifies that emergency ownership defers replacement and retries
only after the threat clears. Both are no-model server-side evidence.

## 2026-08-11 continuation: official Forge discovery recheck

`python3 scripts/discover-forge-lines.py --json --check-patches` was rerun at
2026-08-11T14:06:49Z. It returned `PASS` with Forge 65/Minecraft 26.2,
latest 65.1.1, recommended 65.1.0, no missing/stale 65.x patches and no
promoted Forge 66 line. This is source discovery, not runtime compatibility
evidence; the complete real patch matrix remains `NOT_RUN`.

## 2026-08-11 continuation: Forge 65.1.1 exact-JAR server smoke

Ran the provider-disabled `e2e/orchestrator.py server-smoke --forge-version
65.1.1` against the current product JAR. Run directory:
`e2e/results/no-commit-dirty/20260811T135504Z-e2e4d81b1652159`. Verdict `PASS`:
the exact product SHA-256 was installed in every instance copy, the dedicated
server started without human clients, the headless AI ServerPlayer joined,
SQLite loaded from the Jar-internal library, and the server exited cleanly.
The verifier explicitly reports `functionalAiClaim=false`; this does not
upgrade real-model/client/Hardcore/M0--M4 gates, which remain `NOT_RUN`.

Followed with `restart-smoke --forge-version 65.1.1` on the same exact-JAR
world. Run directory:
`e2e/results/no-commit-dirty/20260811T135715Z-e2e2df1f5f42ce7`. Restart verdict
`PASS`: two server starts and stops exited cleanly, one stable companion UUID
and one SQLite database were observed, and the verifier again recorded
`functionalAiClaim=false`. This remains lifecycle/persistence evidence only.

The formal Gradle `e2eFunctional -Pforge_runtime_version=65.1.1` task was then
run directly. It correctly exited nonzero for a `NOT_RUN` infrastructure
preflight, not a gameplay failure. The immutable record is
`e2e/results/no-commit-dirty/20260811T140533Z-formal20260811140533`; it lists
Darwin/no Xvfb and absent `MCAI_BASE_URL`, `MCAI_MODEL` and authorized key,
with `physicalDisplayUntouched=true` and no provider request.

## 2026-08-11 continuation: Forge 65.1.1 physical recheck and anchor safety contract

Fresh Forge 65.1.1/JDK25 physical checks passed `real_emergency_iron_golem_duel`
1/1 in 1.083 seconds and `real_parkour_course` 1/1 in 2.851 seconds. The
former covers the real headless ServerPlayer's local damage/target/vanilla
melee and shield response; the latter covers ordinary jump, collision and
landing physics. Both are deliberately no-model server-side checks and do not
promote PVP, rendered-client, live-model, Hardcore or M0--M4 evidence.

Added `src/test/java/dev/mcai/companion/embodiment/AiPlayerManagerInitialAnchorSourceContractTest.java`.
It locks the production boundary that first-human reanchor checks both the
EmergencySurvivalController state and the `BehaviorArbiter` emergency claim,
and that a deferred login is retried only by the server-tick normal
remove/relogin path. The existing Forge delayed-emergency-login fixture is the
runtime proof of that path.

Full JDK25 JVM tests, Python E2E (59/59), JDK25 package/compatibility
(`check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`, 16
actionable tasks) all pass. Product artifact remains
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
`63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`,
17,150,533 bytes. Formal real-model/client/Hardcore/M0--M4 gates remain
`NOT_RUN` because this Darwin host still lacks Linux/Xvfb and an authorized
model configuration. The 13:51:51Z preflight explicitly reports missing
`linux_host`, `Xvfb`, `MCAI_BASE_URL`, `MCAI_MODEL`, and
`MCAI_API_KEY_or_MCAI_API_KEY_FILE`; it made no provider request.

The production zero-human Forge 65.1.1 selector was rerun with
`-Pzero_human_autospawn_test=true`: `zero_human_dedicated_server_chunk_and_respawn`
passed 1/1 in 1.636 seconds. It confirms the headless body remains active and
handles chunk/death/respawn work with no human client; it is not a rendered
client, model or Hardcore completion gate.

## 当前继续记录（2026-08-11，Forge 65.1.1 首登应急回归与模型软期限契约）

- 重新运行当前源码的 Forge 65.1.1/JDK25 GameTest
  `mcai_companion:delayed_human_login_while_emergency_active`，全新临时世界
  1/1 通过（约 0.97 秒测试时间，运行目录
  `/tmp/mcai-anchor-current-continuation.hzJAv5`）。日志证明：专服先无真人、AI
  进入 ACTIVE 并遭遇实际伤害；真人随后经 `PlayerList` 登录时初始锚定被延迟，威胁解除后
  才走正常 remove/relogin 到真人附近，没有 gameplay teleport 或 UUID/背包替换。
- 同一当前源码还重新运行了 `real_player_chat_to_immediate_bound_follow`
  和 `real_emergency_zombie_skeleton_horde`，分别 1/1 通过；前者是无模型聊天→绑定跟随的
  ServerPlayer 物理切片，后者是实际伤害后的盾牌/攻击/退避安全切片，均不是实时模型或真人
  客户端证据。
- 重新运行完整 JVM 套件 `./gradlew test`（JDK25）通过；新增
  `ConversationCommitmentSourceContractTest.runtimeWiresConfiguredSoftDeadlineIntoBothModelLanes`
  锁住 `MODEL_SOFT_TIMEOUT_SECONDS` 的跨字段校验结果会同时传入规划和对话模型通道。Python
  E2E 59/59 通过。新增测试只验证配置 wiring，不产生 provider 请求。
- 当前产品工件仍为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`、
  17,150,533 bytes；源码无提交（`DIRTY_NO_COMMIT`），工件为 `NON_RELEASE`。
- 真实模型 Actor/Observer、渲染客户端、Linux/Xvfb、Hardcore 隐藏种子以及 M0--M4
  正式门禁没有被这些离线/服务端测试升级，仍保持 `NOT_RUN`。当前机器没有
  `MCAI_BASE_URL`、`MCAI_MODEL` 或授权凭据；不发送新的 provider 请求。
- 随后执行 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`
  完成 16 个 actionable tasks，兼容发现器确认 Forge 65.x 已发布的 12 个 patch 全部列入
  入口（`formalMatrixComplete=false` 仍是预期）。Python E2E 仍为 59/59，GOAL_STATE JSON
  解析通过；产品 JAR SHA-256 未变。
- 最新正式黑盒预检仍返回 `NOT_RUN`（2026-08-11T13:38:46Z）：当前 Darwin 主机缺少
  Linux、Xvfb、`MCAI_BASE_URL`、`MCAI_MODEL` 和授权密钥，因此未启动真人客户端，也未
  发送任何 provider 请求。

## 当前继续记录（2026-08-11，Forge 65.1.1 矩阵入口修复）

- 兼容锁与官方发现器已包含 65.1.1，但完整 GitHub Actions 65.x 矩阵和手动工作流选择器
  漏列了当前 Latest 65.1.1；已补入两个 workflow 和 E2E README。矩阵目前覆盖 65.0.0--65.0.9、
  65.1.0、65.1.1 共 12 个已发布 patch。
- 这是入口完整性修复，不是运行结果；Linux/Xvfb、真实模型、客户端/菜单/保存矩阵仍需逐 patch
  实跑，当前正式兼容 gate 继续 `NOT_RUN`。
- `./gradlew compat-checker --no-daemon --no-configuration-cache` 随后通过，报告 published=12、
  runtimeSmoke 仍未升级 formalMatrixComplete。
- 同步收紧 reusable workflow：`MCAI_API_KEY` 不再是整个 job 的环境变量，只在校验、只读预检和
  真实功能步骤按 step 注入；新增静态回归确认 secret 未扩大到 checkout、构建、汇总和上传步骤。
- 最后用 JDK25/Forge 65.1.1 运行 `check verifyReleaseJar e2eClientJar e2eOracleJar compat-checker`，
  `BUILD SUCCESSFUL`（16 actionable tasks）；产品 JAR SHA-256 仍为
  `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`、17,150,533 bytes。

## 当前继续记录（2026-08-11，worker 场景与 PASS 证据绑定修复）

- 审计发现 provider-neutral worker 的任务清单可以标为 `m4-hidden-hardcore`，但执行器实际只
  调用聊天/跟随切片；这会把错误场景误计为 M4。新增 `worker_scenario_errors` 与执行前门禁：当前
  仅实现 `real_client_chat_follow_inventory` 且必须是单 case，其他场景明确记录 `NOT_RUN`，不会
  用替代场景冒充统计结果。
- worker `PASS` 现在必须包含精确 source/Forge/Minecraft/JAR/model 绑定、`e2e-verdict.json`
  的空缺证据列表、两条 `brain-*` 模型到动作因果链、独立 Oracle PASS、三份产品副本哈希，以及
  非空的 model/action/world 审计产物；仅有自洽的结果哈希或“ready”预检不能通过验证。
- 新增 Python 回归并通过：worker 定向 12/12，完整 E2E 59/59，`py_compile` 与
  `git diff --check` 通过。真实 Linux/Xvfb/授权模型门禁仍未运行，未升级任何 M0--M4 状态。
- 随后实际运行 `python3 e2e/formal_gates.py --gate e2eChat --forge-version 65.1.1
  --nonce worker-contract-20260811`，结果仍为 `NOT_RUN`，记录目录为
  `e2e/results/no-commit-dirty/20260811T130941Z-worker-contract-20260811`，缺失资源逐项为
  `linux_host`、`Xvfb`、`MCAI_BASE_URL`、`MCAI_MODEL`、`MCAI_API_KEY_or_MCAI_API_KEY_FILE`。

## 当前继续记录（2026-08-11，正式门禁预检证据修复）

- 实际运行 `python3 e2e/formal_gates.py --gate e2eChat --forge-version
  65.1.1 --nonce recovery-preflight-20260811`，当前机器按预期返回 `NOT_RUN`，不是产品
  失败或伪造通过。正式记录现在明确保存 `preflightMissing` 和逐项
  `missingEvidence`：`linux_host`、`Xvfb`、`MCAI_BASE_URL`、`MCAI_MODEL`、
  `MCAI_API_KEY_or_MCAI_API_KEY_FILE`。
- 下一步必须在授权的 Linux/Xvfb worker 上先运行
  `python3 e2e/orchestrator.py preflight --forge-version 65.1.1`，再注入
  `MCAI_API_KEY_FILE`、`MCAI_BASE_URL`、`MCAI_MODEL` 执行
  `python3 e2e/formal_gates.py --gate e2eFunctional --forge-version 65.1.1`；在这些资源出现前不要重试
  macOS，也不要把当前 `NOT_RUN` 改成 PASS。
- 受影响的聊天/动作路径定向 JVM 套件（`BrainOrchestratorTest`、`PlayerTaskIntentTest`、
  对话承诺与通信权限源契约）在 JDK25 下 `BUILD SUCCESSFUL`；它只证明确定性安全边界，
  不替代真实模型 Actor/Observer 因果证据。
- 修复 `e2e/formal_gates.py` 的基础设施证据归档，并新增回归测试；Python E2E 由 52 增至
  53 项，定向和完整套件均通过。该修复不把缺少外部资源转换为 PASS，M0--M4 与真实模型
  门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，Forge 官方版本事实复核）

- 官方 Forge 26.2 下载页与 promotions 文档已重新核对：65.1.1 是 Latest，65.1.0 是
  Recommended；65.0.0--65.0.9、65.1.0、65.1.1 均列在已发布补丁中，未发现 Forge 66。
- `scripts/discover-forge-lines.py --json --check-patches` 返回 `status=PASS`、
  `missingPatches=[]`、`stalePatches=[]`；`scripts/validate-compat.py` 也通过。事实文档和
  `compat/forge-lines.toml` 的核验时间已更新为 2026-08-11 12:47:17 UTC。
- 这只是版本资料与声明边界的校正，不是兼容矩阵或游戏能力通过；Forge 65 全补丁正式矩阵、
  真实客户端/模型门禁和 M0--M4 仍保持 `NOT_RUN`。
- 资料更新后的 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 重新通过（16 actionable
  tasks）；开发 JAR 未改变，SHA-256 仍为
  `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`（17,150,533 bytes）。
- 使用 `--rerun-tasks` 分别以 Forge 65.0.0（编译地板）和 65.1.0（当前推荐）编译 `compileJava`
  均通过；随后以 65.1.1 重新完整构建并通过 `check`/`verifyReleaseJar`。这仍然只是 API/构建
  兼容证据，不替代 12 补丁的真实客户端矩阵。

## 当前继续记录（2026-08-11，延迟首登真实客户端场景）

- 为验证“专服先无真人运行、AI 已进入 ACTIVE、首个真人稍后登录仍能安全贴近”这一此前只由
  GameTest 覆盖的生命周期边界，新增了独立的 `anchor-smoke` 编排场景。它启动精确产品 JAR
  的 dedicated server，等待 `delayed_anchor_zero_human_active`，保持至少 40 个服务端 tick 的
  无真人窗口，然后才启动真实 offscreen Actor 与 Observer；Oracle 不对生产 AI 或真人调用
  gameplay teleport，只设置后续真人的 vanilla respawn 点并只读采样。
- `verify_delayed_anchor_evidence` 现在要求 Oracle 事件顺序、`zeroHumanTicks >= 40`、正常
  `delayed_initial_anchor_passed` 结果、三份精确产品哈希，以及 Actor/Observer 自己渲染到的
  同维度 12 格内 AI 观察；anchor 场景禁止发送聊天。Python 编排器测试由 49 增至 51 项，全部
  通过；`py_compile` 与 `git diff --check` 通过。
- 已通过 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`（16 actionable tasks）。本次
  场景/文档只影响测试 JAR 与证据，不改变产品 JAR 的正式能力声明。
- 用正确的 `-Pzero_human_autospawn_test=true` 属性在 Forge 65.1.1/JDK25 重新跑了生产 GameTest：
  `delayed_human_login_after_zero_human_active` 1/1（2.370 秒）和
  `delayed_human_login_while_emergency_active` 1/1（4.305 秒）；后者日志已归档到
  `/tmp/mcai-delayed-emergency-current-20260811.log`。第一次错传属性的 tick-0 夹具拒绝已单独
  标明，未计入产品结果。
- 实际执行 `anchor-smoke` 在 macOS 上按预期发现 Xvfb 缺失：专服曾记录真实 AI 登录和
  `delayed_anchor_zero_human_active`，随后安全退出（退出码 0）；修复后的异常路径将本次目录
  `e2e/results/no-commit-dirty/20260811T123924Z-e2e014c4ed981e2` 的 manifest 与
  `anchor-verdict.json` 正确收束为 `NOT_RUN`，没有把半个生命周期结果伪装成 PASS。
- 当前主机仍为 Darwin，缺少 Linux/Xvfb 和授权模型环境，因此 `anchor-smoke`、真实模型
  Actor/Observer、渲染客户端、Hardcore 随机种子以及 M0--M4 正式门禁仍保持 `NOT_RUN`；源码
  仍为 `DIRTY_NO_COMMIT`，不得将本切片写成陪玩或通关通过。
- 最终本轮复核：JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过（16 actionable
  tasks），Python E2E 52/52、`py_compile`、GOAL_STATE JSON 和 `git diff --check` 全部通过；开发
  JAR SHA-256 仍为 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`（17,150,533
  bytes）。

## 当前继续记录（2026-08-11，跟随竞态修复后的完整门禁复核）

- 跟随夹具的首登 remove/relogin 窗口与近距离停留竞态已修复后，Forge 65.1.1
  `real_player_chat_to_immediate_bound_follow` 新鲜运行 1/1 通过（13.00 秒，日志
  `/tmp/mcai-follow-final3-ZGbHHg/logs/latest.log`）。
- 随后的 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过（16 actionable tasks）；
  Python E2E 49/49、兼容性检查、`git diff --check` 与 `GOAL_STATE.json` 解析均通过。
- 当前开发工件仍为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，17,150,533 bytes，
  SHA-256 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`。
- 以上仍是无模型的 Forge `ServerPlayer`/生命周期物理证据；真实模型 Actor/Observer、客户端渲染、
  Hardcore 随机种子、两小时速通和 M0--M4 正式门禁继续 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`，
  工件仍 `NON_RELEASE`。
- 最新黑盒预检（`e2e/orchestrator.py preflight --forge-version 65.1.1`）仍明确 `ready=false`：
  当前主机为 Darwin、没有隔离 Xvfb，且没有 `MCAI_BASE_URL`、`MCAI_MODEL` 或授权凭据；因此没有
  启动真实客户端或再次发送模型请求。

## 当前继续记录（2026-08-11，首登重锚与跟随竞态复核）

- `mcai_companion:real_player_chat_to_immediate_bound_follow` 的第一次 Forge 65.1.1 新鲜运行
  暴露了测试/生命周期竞态：首个真人登录触发初始锚点 remove/relogin 时，AI 在数个 tick 内合法地
  不在 `PlayerList`，旧测试直接 `orElseThrow()` 报 `No value present`；重登后安全出生点又与真人
  只有约 1--2 格，跟随控制器正确判定“已在距离内”，而旧测试却等待身体先走 2 格，形成夹具死锁。
- 已修复 release-excluded 跟随场景：等待 `onlinePlayer` 恢复后再发聊天，并在锚定完成后把测试真人放到
  合法的 6 格领跑距离；不改生产移动/传送权限。Forge 65.1.1 新鲜运行随后 1/1 通过（13.00 秒，
  日志 `/tmp/mcai-follow-final3-ZGbHHg/logs/latest.log`）。该切片仍是无模型 ServerPlayer 物理证明，
  不升级为真人渲染或模型因果门禁。
- 这次先失败后修复的记录保留在审计中；它不是“脚本绿灯冒充模型通过”。下一次正式模型运行仍必须
  检查 `model_response_received → decision_revision_accepted → skill_started → low_level_actions_issued`。

## 当前继续记录（2026-08-11，应急中的首登重锚回归）

- 新增 `mcai_companion:delayed_human_login_while_emergency_active`：在无人专服超过初始
  40 tick 后，先用真实 `ServerPlayer.hurtServer` 触发本地紧急生存状态，再让第一个真人走
  `PlayerList.placeNewPlayer` 登录。Forge 65.1.1 新鲜目录通过 1/1（4.302 秒，日志
  `/tmp/mcai-delayed-emergency3-5wAJUR/logs/latest.log`）：登录瞬间保留原身体/UUID，记录
  deferred anchor，威胁解除后才按正常 remove/relogin 到真人附近；未使用 gameplay teleport。
- 该回归曾在第一次夹具尝试中失败（测试身体仍保持无敌状态，伤害未进入生存控制器）；夹具已改为
  明确 Survival、清除 invulnerable 能力并重新运行通过。第一次失败保留为开发调试，不计产品失败。
- 普通 `delayed_human_login_after_zero_human_active` 在同一源码/Forge 65.1.1 再次通过 1/1
  （2.395 秒，日志 `/tmp/mcai-delayed-normal4-nEFXtP/logs/latest.log`）。
- 变更只扩展 release-excluded GameTest 与资源；JDK25 `check verifyReleaseJar e2eClientJar
  e2eOracleJar` 16 actionable tasks、Python E2E 49/49、兼容性和 diff/JSON 检查均通过。正式开发
  JAR 仍为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，17,150,533 bytes，SHA-256
  `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`；真实模型/渲染客户端/
  Hardcore/M0--M4 门禁仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，战斗安全切片定向复核）

- 在当前源码、JDK25 与 Forge 65.1.1 上重新运行了两个由真实 `ServerPlayer` 物理路径驱动的战斗切片：
  `mcai_companion:real_emergency_iron_golem_duel` 1/1 通过（6.374 秒，日志
  `/tmp/mcai-combat-final-CyZYeF/logs/latest.log`），以及
  `mcai_companion:real_emergency_zombie_skeleton_horde` 1/1 通过（6.229 秒，日志
  `/tmp/mcai-horde-final-rKcHzE/logs/latest.log`）。两者都验证了受击、目标重新获取、原版攻击/盾牌与
  紧急生存链；关闭测试连接时的 fallback system-chat 警告是 GameTest 断开时序，不是测试失败。
- 这两项没有调用模型，也没有客户端渲染、PVP 真人对手、随机 Hardcore 或速通证明；不能把它们写成
  “AI 已会战斗”或 M1--M4 通过。真实模型 Actor/Observer 门禁仍为 `NOT_RUN`。
- 软暂停/无动作定向 JDK25 测试随后通过：`BrainOrchestratorTest` 与
  `MinecraftBrainEventSinkSourceContractTest` 均绿；SAFE_IDLE 对活动玩家任务不会静默宣称完成，且
  模型未产生技能时不会伪造低层动作。下一步仍是有授权模型和真实客户端时审计
  `server_chat_received → model_response_received → decision_revision_accepted → skill_started → low_level_actions_issued`。

## 当前继续记录（2026-08-11，模型动作因果链与专服 Actor 授权修复）

- 当前真实黑盒门禁仍未运行：本机为 Darwin，没有 Linux/Xvfb，环境中没有可用的
  `MCAI_BASE_URL`、`MCAI_MODEL` 和授权凭据；此前 MiMo capability probe 的最后真实结果是
  HTTP 401。因此没有把本次代码测试写成真实模型通过。
- 根因一：`BrainOrchestrator` 对绑定跟随目标无条件走 `tryStartImmediatePlayerFollow`，即使已安装
  已验证模型也绕过 `MODEL_REQUEST_STARTED → START_SKILL` 因果链；这会让物理切片通过却让正式
  Actor/Oracle 的“模型决定后动作”证据失败。现在 `ModelGateway` 增加显式 `configured()` 就绪位，
  `SwitchableModelGateway`/`JdkModelGateway` 实现；仅在没有已验证模型时保留受限离线回退，模型就绪时
  必须先经过模型决策。
- 根因二：正式 dedicated E2E 的 `MCAIActor` 是非 OP，隔离服务器没有写入
  `chat.allowedSenders`，因此普通聊天在模型前就被权限边界拒绝。现在 E2E 编排器按 vanilla
  offline-mode UUID（`73328ef7-064e-35fd-adaf-fdc3c77b8fdf`）写入隔离实例配置；生产默认仍不扩大
  非 OP 权限，也没有给 Actor 加 OP。
- 已改文件：`src/main/java/dev/mcai/companion/model/ModelGateway.java`、
  `src/main/java/dev/mcai/companion/model/JdkModelGateway.java`、
  `src/main/java/dev/mcai/companion/runtime/SwitchableModelGateway.java`、
  `src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`、
  `src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`、
  `e2e/orchestrator.py`、`e2e/test_orchestrator.py`。
- 定向验证：JDK25 `BrainOrchestratorTest` 及 gateway 回归通过；Python `e2e.test_orchestrator` 14/14
  通过，并新增 offline Actor UUID/配置断言。随后完整 `check verifyReleaseJar e2eClientJar
  e2eOracleJar` 通过（16 tasks），Python E2E 总计 49/49；当前开发 JAR 为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，17,148,068 bytes，SHA-256
  `cd439586671c56f0cf6b03f59ccc5fe429060d7a76d034cf198970e125576835`。尚未运行真实客户端。
- 最后正式门禁状态：`realE2eVerticalSlice`、`chatToAction`、`movement`、`inventory`、
  `dualClientRendered`、M0--M4 以及 soak 均仍为 `NOT_RUN`。下一步是先做编译/协议回归，再在有
  Linux/Xvfb 与明确授权模型凭据的环境运行 functional E2E；审计必须看到
  `server_chat_received → model_response_received → decision_revision_accepted → skill_started →
  low_level_actions_issued`，否则继续按失败根因修复。

## 当前继续记录（2026-08-11，Forge 65.1.1 兼容性定向回归）

- 官方 Forge 26.2 索引当前最新发布补丁为 65.1.1，推荐 MDK 仍为 65.1.0；兼容锁已更新为
  `latestForge=65.1.1`，但没有把 `[65.0.0,66.0.0)` 声明误写成正式全补丁通过。
- 65.1.1/JDK25 已完成真实编译，并在干净目录通过：无人服务器区块/重生 1/1（1.708 秒）、
  玩家聊天绑定跟随 1/1（12.93 秒）、僵尸/骷髅接触防御 1/1（6.273 秒）、真人登录自动出现
  1/1（2.754 秒）、真实跑酷物理 1/1（26.75 秒）、熔炉菜单事务 1/1（10.27 秒）。日志分别位于
  `/tmp/mcai-forge6511-lifecycle-20260811/logs/latest.log`、
  `/tmp/mcai-forge6511-follow-20260811/logs/latest.log`、
  `/tmp/mcai-forge6511-horde-20260811/logs/latest.log`、
  `/tmp/mcai-forge6511-presence-20260811b/logs/latest.log` 和
  `/tmp/mcai-forge6511-parkour-20260811/logs/latest.log`、
  `/tmp/mcai-forge6511-furnace-20260811/logs/latest.log`。
- 这些都是无模型的真实 Forge `ServerPlayer` 生命周期/物理切片；没有升级为模型决策、渲染
  Actor/Observer、PVP、Hardcore 速通或 M0--M4 正式证据。`formalMatrixUnverifiedPatches` 仍包含
  65.0.0 到 65.1.1 全部补丁。
- 之前一次 `real_player_auto_presence_on_human_login` 选择器名称错误导致“找不到测试”并退出 255，
  已用正确的 `mcai_companion:auto_presence_on_human_login` 重新运行通过；这不是产品失败。
- 同步官方补丁夹具后，Python E2E 48/48 与 mutation 10/10 通过；JDK25 Gradle
  `check verifyReleaseJar e2eClientJar e2eOracleJar` 也通过。当前开发 JAR 为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，17,147,953 bytes，SHA-256
  `5b793d0bdf6c486ddb5d3a894bbb25f152a0623c054c136f6a105a47f1273e68`。

## 当前继续记录（2026-08-11，普通聊天任务权限边界修复）

- 根因：普通聊天入口把 `CompanionCommandAccess.mayAdmin(source)` 当成了
  `maySetGoal`。专用服务器上这只允许管理员，普通非 OP 玩家会得到对话确认但在
  `GoalCoordinator` 之前被拒绝，因此模型/身体都没有可执行目标。
- 已改：`CompanionCommandAccess.mayControlCompanion(source)` 将游戏任务控制与管理命令分开。
  单人所有者和专用服管理员默认允许；普通队友必须在 Forge 配置的
  `chat.allowedSenders` 中填写 UUID。UUID 解析失败时拒绝；`/mcai` 管理与评测命令仍保持
  `mayAdmin`，没有扩大权限。
- 已增：`CompanionCommandAccessTest` 的 UUID 白名单单测，以及
  `CommunicationModuleSourceContractTest` 对聊天入口使用新权限边界的源契约。
  JDK25 定向测试通过（4 个测试）。
- 已增：未授权且已明确指向 Agent 的高置信游戏命令在模型请求前直接给出权限说明，
  写入 `conversation_task_permission_denied`，不再让模型先说“任务已接受”再被权限边界
  静默拒绝；源契约与前述聊天测试一起通过。
- 证据边界：这修复了一个确定的生产前置阻断，但没有伪造模型响应；专用服普通非 OP
  的真实聊天→模型→`START_SKILL`→低层动作链、渲染客户端和 M0--M4 正式门禁继续 `NOT_RUN`。
- 权限改动后的 Forge 65.0.0 既有 `mcai_companion:real_player_chat_to_immediate_bound_follow`
  选择器重新运行 1/1 通过（GameTest 汇总 12.88 秒，日志
  `/private/tmp/mcai-permission-follow-650-20260811/logs/latest.log`）。这是无模型的单人所有者/管理员
  路径回归，只说明旧路径未被权限拆分破坏；没有升级真实模型或非 OP 白名单门禁。
- 重新打包开发工件：`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，
  17,147,953 bytes，SHA-256 `5b793d0bdf6c486ddb5d3a894bbb25f152a0623c054c136f6a105a47f1273e68`。
- 同一工件在 Forge 65.0.0 重新运行 `mcai_companion:real_emergency_zombie_skeleton_horde`，
  1/1 通过、GameTest 汇总 6.272 秒，日志为
  `/private/tmp/mcai-permission-horde-650-20260811/logs/latest.log`。这是无模型的受伤/接触
  紧急控制回归，不是 PVP、十目标战斗或真人陪玩验收。
- 新增权限拒绝分支后再次运行 `mcai_companion:real_player_chat_to_immediate_bound_follow`，
  Forge 65.0.0 仍为 1/1、GameTest 汇总 12.86 秒，日志为
  `/private/tmp/mcai-permission-denial-follow-650-20260811/logs/latest.log`；这只证明已授权的
  所有者/管理员旧路径没有回归，未调用模型。

## 当前继续记录（2026-08-11，物理接触下的敌对目标重新夺回）

- 根因：敌对实体进入 AI 身体碰撞箱、但 AI 视线朝向别处时，原感知仍要求视锥和视觉遮挡，
  因而紧急层只能收到受伤/声音线索而没有可操作的目标，形成“挨打但只看着”的停顿。
- 已改 `FairPerceptionSampler` 与 `VisibleEntity`：对已经与身体物理接触且来自有界已加载实体索引的
  当前威胁，允许独立的 `PHYSICAL_CONTACT` 实体观察，作为重新转身/锁定提示；不宣称视觉 LOS。
  `FairPlayerActuator` 在真正攻击前仍重新检查第一人称交叉线、触及距离和方块遮挡，未引入实体雷达、
  隐藏区块扫描、传送或直接改世界。
- 新增 Forge GameTest `mcai_companion:real_emergency_contact_reacquisition` 及对应测试实例/环境资源。
  Forge 65.0.0 精确选择器新鲜运行 1/1 通过，游戏测试本体约 758.1 ms，日志为
  `/tmp/mcai-contact-recheck-20260811-d/logs/latest.log`。
- 新增 `FairPerceptionContactSourceContractTest`，与 `SemanticObservationJsonCodecTest`、
  `FairPerceptionSamplerTest` 定向运行均通过（JDK25 Gradle test）。
- 同一选择器在 Forge 65.1.0 新鲜目录也通过 1/1，约 794.0 ms，日志为
  `/tmp/mcai-contact-recheck-20260811-651/logs/latest.log`。
- 当前开发工件重新打包为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，大小
  17,146,697 bytes，SHA-256 为
  `97bf0d11a27b21e8748c905232a8cf72708574dd1dcf8f8914f40082d35ec32a`。
- `check verifyReleaseJar e2eClientJar e2eOracleJar`、Python E2E 48/48、mutation 10/10、
  JSON/diff 检查均通过；完整构建仅是代码/证据回归，不读取或发送模型密钥。
- 以上均为无模型的真实 Forge `ServerPlayer` 物理/公平感知切片；真实模型 Actor/Observer、客户端视觉、
  Hardcore 随机种子、M0--M4 和 soak 门禁继续 `NOT_RUN`。源码仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，Forge 65.0.0 聊天绑定跟随物理复核）

- 新鲜 Forge 65.0.0 GameTest 选择器
  `mcai_companion:real_player_chat_to_immediate_bound_follow` 通过 1/1，完成时间约 1.276 s。
  日志位于 `/tmp/mcai-follow-recheck-20260811/logs/latest.log`；日志确认测试玩家发送聊天后收到
  `[AI] MCAI：收到，任务已经创建；我正在规划第一个动作。`，随后收到跟随执行状态，并完成真实
  `ServerPlayer` 绑定跟随物理切片。
- 该切片不调用模型，只验证 Forge 65.0.0、系统聊天入口、任务绑定与身体动作链；测试结束时连接按
  GameTest 设计关闭，关闭包警告不影响测试通过，也不能作为真实客户端渲染或真人陪玩证据。
- 当前生产 JAR SHA-256 仍为
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`；正式真实模型、Actor/Observer、
  Hardcore 随机种子、M0--M4 和 soak 门禁全部继续 `NOT_RUN`。源码仍为 `DIRTY_NO_COMMIT`，工件仍为
  `NON_RELEASE`。

## 当前继续记录（2026-08-11，真实客户端系统聊天证据链修复）

- 根因：`src/e2eClient/java/dev/mcai/e2e/client/McaiE2eClientMod.java` 在记录普通客户端聊天时
  直接调用 `event.getSender().toString()`；服务端系统消息可以没有 sender，导致测试客户端抛出空指针并
  停止写后续事件。这样会丢失后续 AI 聊天、TAB 和移动样本，不能作为“AI 没有回应”的产品结论。
- 已改：sender 为空时写入空字符串并继续记录；新增
  `src/test/java/dev/mcai/companion/e2e/E2eClientSourceContractTest.java` 锁定空 sender 分支。
- 定向验证：JDK25 `./gradlew test e2eClientJar --no-daemon --no-configuration-cache` 通过，7 个任务中
  4 个执行；生产 JAR SHA-256 仍为
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`，e2e 客户端 JAR SHA-256 为
  `03bdb9ddb59f89341dfb33da86bfe2616e8be115e0ad492e8f5664f447c60613`。
- 证据边界：这是客户端编译/源码契约修复；本机仍没有 Linux/Xvfb 和当前授权的真实模型凭据，未运行
  真实 Actor/Observer、聊天到动作、Hardcore 随机种子或 M0--M4 正式门禁，全部继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，当前生产 JAR 专服 smoke 复核）

- 精确产物专服 smoke `e2e/results/no-commit-dirty/20260811T093116Z-e2e385185ed6ad6` 在 Forge
  65.1.0、JDK25 上通过生命周期检查：服务端启动、精确 JAR 哈希、三份安装副本一致、AI
  `ServerPlayer` 加入、Jar-in-Jar SQLite、运行时打开内存库、正常退出均通过，退出码为 0。
- 产物与加载产物 SHA-256 均为
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`；该 smoke 的
  `functionalAiClaim=false`。无凭据 Oracle 在服务停止前未拿到结果，记录为
  `server_stopped_before_result`，不能当作聊天/动作通过。
- 这次只证明封装 JAR 和无模型生命周期没有回归；真实模型、Actor/Observer、第一人称视觉、
  Hardcore 随机种子、24/100 小时 soak，以及 M0--M4 正式门禁仍全部 `NOT_RUN`。源码仍为
  `DIRTY_NO_COMMIT`，工件仍为 `NON_RELEASE`。

## 当前继续记录（2026-08-11，20 TPS 观察热路径性能修复）

- 根因：`MinecraftObservationProvider.Fingerprint.criticalBodyChanged` 每个服务端 tick 都
  调用 `withCurrentBody`，无谓地复制背包、菜单和危险列表；长生命周期切片曾在约 6,600 tick
  报告滚动 p95 约 3.8 ms。
- 已改：该 20 Hz 热路径现在直接比较维度、方块位置、半心、饥饿、空气带、着火和入水等既有
  标量失效字段；完整列表仍只在 4 Hz 语义采样时生成。新增源码契约测试，防止以后恢复完整
  Fingerprint 分配。
- 性能证据：Forge 65.1.0 长生命周期切片 5,870 samples，平均 289,585 ns、rolling p95
  1,292,541 ns，1/1 通过；Forge 65.0.0 下限 6,087 samples，平均 344,459 ns、rolling p95
  1,259,667 ns，1/1 通过。两者都是真实 Forge ServerPlayer 物理长跑、无模型请求。
- 最新复核：JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable tasks、
  Python E2E 48/48、mutation gate 10/10、JSON 与 `git diff --check` 通过。当前 JAR
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `2c4247de457df402207185838fe3db6b3333198c48236690859b85b00d688ae1`，大小 17,146,468 bytes。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、工件 `NON_RELEASE`；没有读取/发送模型密钥，没有
  Linux/Xvfb Actor/Observer、真实模型、Hardcore 随机种子或 24/100 小时 soak；M0--M4 和正式门禁
  全部继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，普通世界身体失败自愈）

- 根因：`CompanionRuntime` 原先只在已有 RUNNING 目标时处理身体缺失；普通无目标世界若首次
  出生/区块放置遇到瞬态 `FAILED`，之后不会再申请出生，于是世界可能永久没有可见 AI 身体。
- 已改：`src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java` 增加独立的
  `bodySpawnRetryAfterTick` 限频状态，仅对 `SessionState.FAILED`、非 Hardcore 世界调用
  `AiPlayerManager.requestSpawn(server)`；接受后 20 tick、拒绝后 200 tick 重试，绝不覆盖用户
  主动移除的 `ABSENT`，也不绕过原版 ServerPlayer 生命周期。新增
  `CompanionRuntimeEmbodimentRecoverySourceContractTest` 锁定该边界。
- 定向证据：JDK25 源码契约测试通过；新鲜 Forge 65.1.0 `zero_human_dedicated_server_chunk_and_respawn`
  1/1（约 1.654 s）通过、`real_player_chat_to_immediate_bound_follow` 1/1（约 1.255 s）通过；
  Forge 65.0.0 下限同两项也分别 1/1（约 1.664 s、1.249 s）通过。
  这些运行没有模型请求，不能替代真实 AI 聊天到动作门禁。
- 最新复核：`check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable tasks 通过；Python
  E2E 48/48、mutation gate 10/10、JSON 与 `git diff --check` 通过。当前 JAR
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `31fcda6960400f8fbf2e89c7fdc77cf051e1fa5e2b496b48500d79295397e882`，大小 17,146,462 bytes。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、工件 `NON_RELEASE`；没有读取/发送模型密钥，没有
  Linux/Xvfb Actor/Observer、真实模型、Hardcore 随机种子或 24/100 小时 soak；M0--M4 和正式门禁
  全部继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，敌对声音来源过滤后的最终回归）

- Forge 65.x `PlayLevelSoundEvent.AtEntity` 没有可用的实例取消状态方法；已按实际 API 修正，
  只接收仍存活的 `Enemy`，再由 frame source 做真实 4--16 格距离过滤。未引入不兼容调用。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable tasks 通过；Python
  E2E 48/48、mutation gate 10/10、`GOAL_STATE.json` 和 `git diff --check` 通过。最新生产 JAR
  `mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256：
  `9fc3f2be6773b0d5280a4ebf8b6efc2928657239853c09c8669d87232bbe9636`。
- 仍无模型凭据读取/发送、真实 Actor/Observer、Linux/Xvfb、Hardcore 随机种子或正式门禁；M0--M4
  继续 `NOT_RUN`，工作树 `DIRTY_NO_COMMIT`，工件 `NON_RELEASE`。

## 当前继续记录（2026-08-11，听觉线索距离边界修复）

- 审计发现 Forge `PlayLevelSoundEvent.AtEntity` 是服务端事件，可能不等价于客户端真正听到；
  之前只按音量计算 4--16 格上界，没有实际距离过滤。现在在保存线索前检查实体与 AI 身体的
  实际距离，超过该上界直接丢弃；仍只保留 20 tick、威胁等级、方向和距离上界，不保留实体身份
  或精确坐标。
- 已改文件：`ServerCoreSkillFrameSource.java` 增加距离拒绝路径；
  `AuditoryThreatSourceContractTest.java` 增加契约断言。定向测试通过。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable tasks 通过；Python
  E2E 48/48、mutation gate 10/10、生产 JAR 哈希为
  `7f86ca25d78b1db6bd257d990fdb8c4a392aba94beedd2267d20e7f1e60a19ff`。
- 仍然只是无模型/离线和组件回归；真实模型、Linux/Xvfb Actor/Observer、Hardcore 随机种子和
  正式 M0--M4 门禁均为 `NOT_RUN`，工作树 `DIRTY_NO_COMMIT`，工件 `NON_RELEASE`。

## 当前继续记录（2026-08-11，公平听觉威胁线索与完整回归）

- 根因/改动：之前的安全感知只有视锥实体、接触和受伤事件；敌对声音不会触发下一次语义刷新，
  容易出现“被打后只看着”的停滞。`ServerCoreSkillFrameSource` 现在接收 Forge
  `PlayLevelSoundEvent.AtEntity` 的敌对声音，保留最多 20 tick、4--16 格距离上界、0.50--0.90
  威胁等级和近似方向；不保留声音/实体身份、精确坐标或隐藏实体信息。`CompanionRuntime`
  只过滤 `Enemy` 并请求 `SEMANTIC_REFRESH`，本地 20 TPS 安全层先响应。
- 已改文件：`PerceptionProvenance`、`DangerSignal`、`ServerCoreSkillFrameSource`、
  `CompanionRuntime`、`FairPerceptionSampler`；新增 `AuditoryThreatSourceContractTest`，并补充
  `DangerSignalTest` 的有界方向覆盖。音频事件不会改变原版声音或直接生成模型动作。
- 验证：JDK25 定向测试通过；`check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable
  tasks 通过；Python E2E 48/48、mutation gate 10/10、JSON 和 `git diff --check` 通过。当前
  工件 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `08edbc2073f8307739af84d9ee1315668d4a9513cd3deb596b7f7a2d897b89d4`。
- 证据边界：以上是源码/离线/无模型回归；没有读取或发送 API Key，没有真实模型、Linux/Xvfb
  Actor/Observer、Hardcore 随机种子或 24/100 小时 soak。正式 M0--M4 和所有正式门禁仍为
  `NOT_RUN`，源码工作树仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，MiMo边界改动后的完整回归）

- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过（16 actionable tasks）。
  Python E2E 48/48、mutation gate 10/10、`GOAL_STATE.json` 解析和 `git diff --check` 通过。
- 当前生产工件为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `1d9c9ea68f456795b9d34d6dc07201bde427706402927ab3823220f8c0e872a6`，17,143,136 bytes。
  由于工作树仍 `DIRTY_NO_COMMIT`，工件仍标记 `NON_RELEASE`。
- 这些是离线协议、构建和验证器回归；没有读取/发送 API Key，没有真实模型请求，没有
  Linux/Xvfb Actor/Observer，也没有提升任何正式 M0--M4 门禁（全部仍 `NOT_RUN`）。

## 当前继续记录（2026-08-11，MiMo 请求契约与截图能力边界）

- 根因/风险：当前 `mimo-v2.5` 配置可走 MiMo Token Plan 的 Responses 或 Chat
  Completions，但截图仍没有经过认证的 AI 第一人称捕获链路。此前 `get_screenshot` 只有
  `available=false` 和内部码，调用方容易把它误读为空画面或把 Observer 画面当成模型输入。
- 已改文件：`src/main/java/dev/mcai/companion/mcp/MinecraftMcpBackend.java` 的截图响应现在明确
  返回 `capturePath=headless_server_player_unavailable`、`modelInput=false`、
  `observerCameraAllowed=false`、`requiresAuthenticatedClientCapture=true`；新增
  `src/test/java/dev/mcai/companion/mcp/MinecraftMcpBackendSourceContractTest.java`。
  新增 `src/test/java/dev/mcai/companion/model/MiMoProviderContractTest.java`，锁定 Token Plan
  的 `mimo-v2.5` Chat 请求使用 `max_completion_tokens`、`thinking.type=disabled`、严格
  `response_format`，Responses 请求使用 `store=false`、`max_output_tokens`、
  `reasoning.effort=none` 和严格 `text.format`；不触发网络或读取密钥。
- 定向验证：JDK25 Gradle 定向模型/MCP测试通过（4 actionable tasks，BUILD SUCCESSFUL）。
  这只证明离线请求构造和 fail-closed 状态，不是视觉能力、真实模型或陪玩门禁。
- 最后失败门禁：本机 Darwin 没有 Linux/Xvfb，当前环境没有允许使用的真实模型凭据；真实
  Actor/Observer、第一人称客户端捕获、M0--M4 随机种子和 soak 仍 `NOT_RUN`。源码仍
  `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。
- 下一步：在不借用 Observer 画面的前提下设计并实现“服务端请求→认证客户端第一人称截帧→
  PNG 脱敏/尺寸/nonce校验→模型多模态输入”的独立协议；在 Linux/Xvfb 和真实 MiMo 凭据上
  先跑单次 Chat→Action 纵向切片，成功后才升级正式门禁。若客户端捕获门禁无法通过，保持
  截图不可用而不把语义视野或 Observer 画面冒充视觉。

## 当前继续记录（2026-08-11，渲染 Observer 证据防线）

- 根因：真实客户端验证器此前只要求 Observer 登录、TAB 和坐标样本，没有要求实际渲染产物，
  因此“进服但无可审阅画面”可能被误记为 rendered Observer。
- 已改文件：`src/e2eClient/java/dev/mcai/e2e/client/McaiE2eClientMod.java` 在 Observer 首次
  看到 AI 时捕获一帧第一人称 `screenshots/observer-rendered.png`，轮询并校验 PNG 头、尺寸和
  大小后写入 `observer_screenshot_saved`；`e2e/orchestrator.py` 要求同 nonce 的文件、事件和
  有序生命周期，`e2e/ci_evidence_summary.py` 展示截图证据状态；`e2e/test_orchestrator.py`
  增加缺失截图失败测试。
- 定向验证：JDK25 `e2eClientJar` 编译成功；Python E2E 48/48 通过；尚未运行 Linux/Xvfb、
  真实模型或双客户端，因此没有升级正式 Render/UI、realE2eVerticalSlice 或 M0--M4。
- 当前根因/阻断未变：本机 Darwin 缺 Linux/Xvfb 和模型凭据，源码仍 `DIRTY_NO_COMMIT`，工件仍
  `NON_RELEASE`；下一步是在真实 worker 上运行该截图门禁并人工审阅画面，不能用合成 PNG 提升
  正式状态。
- JDK25 全量复核：`check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks 成功；Python
  E2E 48/48、mutation gate 10/10、JSON 和 `git diff --check` 成功。生产 JAR 未因测试客户端
  改动变化：`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`，17,143,018 bytes。
  当前 preflight 仍为 Darwin/无 Xvfb/无 `MCAI_*`，退出 2，未启动客户端或供应商请求。

## 当前继续记录（2026-08-11，Forge 65.x 真实客户端矩阵入口）

- 已改文件：`.github/workflows/real-client-functional-e2e.yml` 增加 `workflow_call` 接口；新增
  `.github/workflows/real-client-functional-e2e-matrix.yml`，手动触发后对 65.0.0--65.0.9、
  65.1.0 逐补丁并行运行同一精确工件、真实 Actor/Observer、Linux/Xvfb 和模型门禁，逐份保存
  nonce 绑定证据。
- 仅新增外部运行入口，没有自动触发、没有使用本机旧密钥、没有把入口定义当成通过；`forge65PatchMatrix`
  和 M0--M4 仍 `NOT_RUN`，待真实 worker 完成全部 11 个补丁并聚合审计。

## 当前继续记录（2026-08-11，真实客户端证据链假通过修复）

- 根因：`e2e/orchestrator.py` 的 `verify_evidence` 只要求 Observer 有位置/TAB样本，未要求
  `client_mod_ready`、连接、登录和准备标记；一个没有真实 Observer 登录的合成事件文件也可能
  通过，不能作为真实渲染客户端证据。Actor 也缺少有序客户端生命周期门禁。
- 已改文件：`e2e/orchestrator.py` 新增 `has_ordered_event_chain`，要求 Actor 按加载→连接→登录→
  准备→发言→观察到跟随到达→第二任务，Observer 按加载→连接→登录→准备→世界样本的单调序列；
  `verify_evidence` 现在硬要求双方真实客户端生命周期事件。`e2e/test_orchestrator.py` 更新完整
  合成夹具并新增“删除 Observer 登录必须失败”的故障注入测试。
- 定向验证：`python3 -m unittest e2e.test_orchestrator` 11/11 通过；完整
  `python3 -m unittest discover -s e2e -p 'test_*.py'` 46/46 通过；`python3 e2e/mutation_gate.py --json`
  10/10 变异全部捕获。该验证器改动尚未运行真实 Actor/Observer/模型，不能升级任何正式门禁。
- 当前工件/状态：源码仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`；M0--M4、真实模型、双客户端
  渲染、Hardcore 随机种子和 soak 门禁继续 `NOT_RUN`。下一步是用 Linux/Xvfb/真实模型 worker
  跑真实纵向切片；本机 Darwin 缺少这些外部前置，不能用本地合成事件替代。

## 当前继续记录（2026-08-11，E2E 跨运行串档防线）

- 进一步发现验证器没有核对客户端/Oracle 事件的运行 nonce，理论上可拼接不同运行的证据。
  `verify_evidence` 现在要求 Oracle 事件只有一个 nonce，Actor、Observer 和 `oracle-result.json`
  全部必须绑定同一个 nonce；缺失或篡改会失败。合成夹具已加入 nonce，新增混合运行事件失败测试。
- 定向验证：Orchestrator 12/12、完整 Python E2E 47/47、突变门禁 10/10 通过。正式真实客户端、
  模型、Hardcore 随机种子和 M0--M4 仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，Forge 65.x 外部运行入口补齐）

- 手动真实客户端工作流现在可选择已发布的全部 Forge 65.x 补丁（65.0.0--65.0.9、65.1.0），
  不再只暴露 65.0.0/65.1.0 两个选项。每次运行仍只针对一个补丁并上传独立证据；完整补丁矩阵
  仍未运行，不能把工作流选项声明成兼容性通过。

## 当前继续记录（2026-08-11，验证器改动后的 JDK25 工件回归）

- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks 成功；兼容性检查报告已发布
  Forge 65.x 补丁 11 个，正式矩阵仍未完成。生产 JAR 未因 Python/工作流改动变化：
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`，17,143,018 bytes。
- 该 JAR 仍来自 `DIRTY_NO_COMMIT` 源码，标记 `NON_RELEASE`；没有供应商请求、真人客户端或
  Hardcore 随机种子运行，正式 M0--M4 继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，聊天模型失败可见性与 Forge 边界回归）

- 根因：会话协调器在模型传输失败、超时、限流、格式错误或没有结果时，之前统一回复“我刚才没听清”，把供应商/网络故障伪装成玩家表达不清，也没有明确说明该条消息未执行。现在按失败类别给出一次有限重试状态；耗尽后明确说明消息未执行并给出检查网络、额度或模型设置的下一步。不会创建目标、声称动作开始或重复发送请求。
- 已改文件：`src/main/java/dev/mcai/companion/communication/CompanionConversationCoordinator.java` 新增 `transientRetryMessage`、`exhaustedFailureMessage` 和 `missingOutcomeMessage`，并在传输失败/模型失败/缺失结果分支使用；新增 `src/test/java/dev/mcai/companion/communication/ConversationFailureMessageTest.java`，覆盖中英文、超时、限流、网络、格式错误和未执行承诺。
- JDK25 定向聊天测试通过；随后完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 个任务通过，Python E2E 45/45，JSON、Forge 兼容性和 `git diff --check` 通过。当前产品 JAR 为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 `71365be616fd7b74ef43e57de171d034ff62f79d23fa2ec1e9e921436a81be46`，大小 17,143,018 字节；源码仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。
- 真实 Forge 专服回归：Forge 65.0.0 目录 `/tmp/mcai-conversation-follow-20260811-oPOp5i` 与 Forge 65.1.0 目录 `/tmp/mcai-conversation-follow-651-20260811-FCuu0n` 均运行完整 selector `mcai_companion:real_player_chat_to_immediate_bound_follow`，各 1/1 通过（GameTest 约 1.298 s / 1.178 s）。日志分别位于各目录的 `logs/latest.log`；证明真实 ServerPlayer、玩家聊天、目标安装和本地绑定跟随链路在两个 Forge 65 边界可用，但没有模型请求，不能升级 `realE2eVerticalSlice`、`chatToAction` 或 M0--M4。
- 第一次裸 selector `real_player_chat_to_immediate_bound_follow` 被 Forge 正确判定为无测试并退出 255；这是编排参数错误，已作为丢弃记录，不计产品证据。正式 M0--M4、真实模型/Actor/Observer、Hardcore 随机种子、双客户端和 soak 仍保持 `NOT_RUN`。
- 下一步：保持失败状态诚实，优先在 Linux/Xvfb/真实模型凭据可用的 worker 上运行真实聊天→模型→技能→运动纵向切片；本机 Darwin 缺少该离屏环境且没有可用 `MCAI_*` 模型环境，不能用无模型替代。

## 当前继续记录（2026-08-11，失败可见性修复后的完整复核）

- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 个任务通过；Python E2E 45/45，
  JSON、Forge 兼容性和 `git diff --check` 通过。当前产品 JAR 为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `7de533f4ca1e9db425e97a2938e710f6c27906052e4277eb62b3e6453761ccde`，大小 17,141,866 字节。
- 新鲜 Forge 65.1.0 服务器 `real_dispenser_button_activation` 1/1、595.7 ms 通过，运行目录
  `/tmp/mcai-status-sink-651-YKxWD5`，日志 `/tmp/mcai-status-sink-651.log`；该切片仍无模型，
  只验证状态接收器改动没有破坏真实 ServerPlayer/红石生命周期。
- 状态改动后的新鲜 Forge 65.1.0 `real_emergency_zombie_skeleton_horde` 也通过 1/1、1.153 s，
  运行目录 `/tmp/mcai-status-horde-651-ULCkbi`，日志 `/tmp/mcai-status-horde-651.log`；十僵尸/十
  骷髅场景仍由本地安全层完成装备、移动和战斗，说明“被打时身体不动”的物理回归没有被本次
  状态广播改动破坏。它依然不是模型 PVP 证据。
- 同一工件在无真人服务器的 `zero_human_dedicated_server_chunk_and_respawn` 通过 1/1、1.546 s，
  运行目录 `/tmp/mcai-status-zero-human-651-pubeOZ`，日志 `/tmp/mcai-status-zero-human-651.log`；
  AI ServerPlayer 自动加入、远端区块/重生切片完成，确认没有真人在线时身体仍能存在。该切片无模型，
  不代表自动通关。
- `docs/progress/GOAL_STATE.json` 已更新为 526 个 developmentEvidence 键，正式 M0--M4 和真实
  模型/客户端门禁仍 `NOT_RUN`；源码仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，启动失败提示修复）

- `ModelBootstrapCoordinator` 不再把缺少凭据、配置错误和认证/供应商失败只显示成内部英文码；
  现在明确说明“AI 身体仍在世界中，但不会执行模型动作”，并给出保存/验证 API Key 或检查配置
  的下一步。错误码仍只经过安全裁剪，未加入密钥、响应体或玩家文本。
- `ModelBootstrapStatusMessageSourceContractTest`、`ModelBootstrapCoordinatorTest`、
  `ModelRuntimeTest` 和失败状态接收器定向测试通过；随后完整工件复核为 JDK25 16 tasks、Python
  E2E 45/45、兼容/JSON/diff 通过，JAR 哈希为 `7de533f4…3761ccde`。正式门禁状态不变。
- 包含启动提示改动的最终 JAR 在 Forge 65.1.0 无真人服务器
  `zero_human_dedicated_server_chunk_and_respawn` 通过 1/1、1.614 s，运行目录
  `/tmp/mcai-bootstrap-zero-human-651-LueZuU`，日志 `/tmp/mcai-bootstrap-zero-human-651.log`；
  自动出生、区块/重生切片和关闭链正常。

## 当前继续记录（2026-08-11，模型失败状态可见性修复）

- 根因：`BrainOrchestrator` 已经对超时、传输失败和连续失败发出受控 `BrainEvent.Notice`，但
  `MinecraftBrainEventSink` 之前只写入记忆数据库；普通玩家可能只看到 AI 停在原地，不知道模型
  是否失败或是否真正执行过动作。
- 已改文件：`src/main/java/dev/mcai/companion/runtime/MinecraftBrainEventSink.java` 新增一次性、
  非模型生成的失败状态广播；按目标 revision 去重，极限评测仍静默，终止状态单独提示“已暂停
  自动操作”。新增 `src/test/java/dev/mcai/companion/runtime/MinecraftBrainEventSinkSourceContractTest.java`
  锁定失败代码、诚实措辞和评测静默边界。
- 定向测试：JDK25 `MinecraftBrainEventSinkSourceContractTest`、`BrainOrchestratorTest`、
  `ModelBootstrapCoordinatorTest` 已通过；下一步是当前工件全量复核和一次 Forge 物理切片。该改动
  不会把模型失败伪装成动作成功，也不升级任何 M0--M4 正式门禁。

## 当前继续记录（2026-08-11，发射器与按钮红石回归）

- 当前源码在两个独立的新鲜 Forge 65.1.0 GameTest 服务器中通过
  `real_dispenser_transaction` 1/1（目录 `/tmp/mcai-current-real-dispenser-transaction-651-T0WdCU`，
  日志 `/tmp/mcai-current-real-dispenser-transaction-651.log`）和
  `real_dispenser_button_activation` 1/1（目录 `/tmp/mcai-current-real-dispenser-button-activation-651-rX1Apt`，
  日志 `/tmp/mcai-current-real-dispenser-button-activation-651.log`）。
- 结果覆盖真实第一人称瞄准、发射器菜单槽位事务、按钮供能和箭矢消耗/发射；没有模型请求、
  没有命令或直接世界修改，因此只属于红石/功能方块组件证据，M3 和 M0--M4 仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，工作站与存储扩展回归）

- 当前源码在四个独立的新鲜 Forge 65.1.0 GameTest 服务器中分别通过：
  `real_cartography_table_transaction` 1/1（目录 `/tmp/mcai-current-real-cartography-table-transaction-651-huULjk`，
  日志 `/tmp/mcai-current-real-cartography-table-transaction-651.log`）、
  `real_stonecutter_transaction` 1/1（目录 `/tmp/mcai-current-real-stonecutter-transaction-651-dKGv3l`，
  日志 `/tmp/mcai-current-real-stonecutter-transaction-651.log`）、
  `real_barrel_transaction` 1/1（目录 `/tmp/mcai-current-real-barrel-transaction-651-p2fKUy`，
  日志 `/tmp/mcai-current-real-barrel-transaction-651.log`）和
  `real_hopper_transaction` 1/1（目录 `/tmp/mcai-current-real-hopper-transaction-651-GcShmc`，
  日志 `/tmp/mcai-current-real-hopper-transaction-651.log`）。
- 这些是原版第一人称可达面、菜单槽位、转移/结果核验的真实身体切片；没有模型请求、没有
  直接库存写入，也不能升级完整工作站知识、长期生电或 M3，正式门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，原版功能方块事务回归）

- 当前源码在三个独立的新鲜 Forge 65.1.0 GameTest 服务器中分别通过：
  `real_brewing_stand_batch` 1/1（目录 `/tmp/mcai-current-real-brewing-stand-batch-651-6O0ZQS`，
  日志 `/tmp/mcai-current-real-brewing-stand-batch-651.log`）、
  `real_ender_chest_transaction` 1/1（目录 `/tmp/mcai-current-real-ender-chest-transaction-651-atsIxq`，
  日志 `/tmp/mcai-current-real-ender-chest-transaction-651.log`）和
  `real_shulker_box_transaction` 1/1（目录 `/tmp/mcai-current-real-shulker-box-transaction-651-D5mLvr`，
  日志 `/tmp/mcai-current-real-shulker-box-transaction-651.log`）。
- 这些切片使用真实 headless `ServerPlayer` 完成原版菜单观察、物品槽位事务和结果核验，未调用
  模型、未直接写背包/世界，也没有供应商请求；它们只是 M3 原版工作站/存储组件证据，不能升级
  专家陪玩、完整工作站矩阵或 M0--M4，正式门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，当前工件最终复核）

- 当前源码重新执行 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`，16 个任务成功；
  `python3 -m unittest discover -s e2e -p 'test_*.py' -v` 为 45/45，兼容性校验、JSON 校验和
  `git diff --check` 均通过。
- 当前产品工件为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `f38cd15e63482a7d235a98cee82f1d83342983336153aca1c749f8fa35912b04`，大小 17,140,455 字节；
  源码仍 `DIRTY_NO_COMMIT`，因此该工件仍是 `NON_RELEASE`。
- 构建中出现的受控异常日志来自既有单元测试（技能异常转换、导航耗尽）并未使测试失败；没有
  供应商请求、真人客户端或模型通关证据，正式 M0--M4 与真实模型门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，末地胜利返回物理回归）

- 当前源码在新鲜 Forge 65.1.0 GameTest 目录
  `/tmp/mcai-current-end-return-651-INwhHe` 中运行
  `mcai_companion:real_end_victory_and_return`，1/1 通过，测试耗时约 6.947 秒；日志为
  `/tmp/mcai-current-end-return-651.log`。
- 真实 headless `ServerPlayer` 完成末地进入、末影龙击杀、原版 `Free the End` 进度以及返回
  传送门；服务端日志还记录了实际的 Monster Hunter/Free the End 进度和正常离场。
- 这是受控无模型维度/战斗组件证据，没有随机种子、真人客户端、供应商请求或 Hardcore 统计，
  不能升级 chat-to-action、PVP、M2 或 M0--M4；正式门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，跨平台凭据恢复定向回归）

- 使用 JDK25 对当前源码执行了凭据/启动恢复定向测试：
  `ApiKeyManagerRestartPersistenceTest`、`CrossPlatformCredentialSourceTest`、
  `ModelRuntimeTest`、`ModelSetupCredentialStatusTest` 和
  `ModelBootstrapCoordinatorTest`，Gradle `test` 成功，相关 29 个用例全部通过。
- 覆盖范围包括：新进程从持久安全存储恢复、`MCAI_API_KEY` 注入优先于旧存储、写入后
  读回不匹配时不误报“可跨重启”、本地恢复不产生供应商请求、认证失败后不自动复用旧密钥，
  以及普通世界启动只进行一次模型探测并保留安全状态。
- 本次没有读取、记录或发送任何真实 API Key；没有启动真人客户端或供应商模型，因此这只是
  凭据/启动组件证据，不能升级聊天到动作、真实模型或 M0--M4 门禁。正式门禁继续 `NOT_RUN`，
  源码仍为 `DIRTY_NO_COMMIT`，工件仍为 `NON_RELEASE`。

## 当前继续记录（2026-08-11，应急战斗物理复核）

- 当前源码在 Forge 65.1.0 新鲜 GameTest 目录
  `/tmp/mcai-current-horde-651-jjMNKz` 中运行
  `mcai_companion:real_emergency_zombie_skeleton_horde`，1/1 通过，约 1.160 秒；日志
  `/tmp/mcai-current-horde-651.log`。
- 真实 headless `ServerPlayer` 在十僵尸/十骷髅压力场景中完成本地应急生存和普通战斗动作，
  并正常离场；这是 20 TPS 安全反射层/原版身体事务证据，不代表模型选择了攻击、走位或 PVP
  策略。关闭阶段的包发送警告仍不改变 1/1 结果。
- 没有供应商请求、真人客户端、随机种子或 Hardcore；正式 chat-to-action、PVP 和 M0--M4 继续
  `NOT_RUN`。

## 当前继续记录（2026-08-11，聊天路由后的跟随物理复核）

- 当前源码在 Forge 65.1.0 新鲜 GameTest 目录
  `/tmp/mcai-current-follow-651-pebFVo` 中运行
  `mcai_companion:real_player_chat_to_immediate_bound_follow`，1/1 通过，约 1.298 秒；日志
  `/tmp/mcai-current-follow-651-retry.log`。
- 真实 headless `ServerPlayer` 与测试真人先后加入，普通聊天建立跟随目标，随后“走啊”被识别为
  当前跟随任务的提醒而未覆盖目标；身体生命周期正常结束。GameTest 关闭测试玩家后出现的
  `StacklessClosedChannelException` 只是关闭阶段的包发送警告，不影响 1/1 结果。
- 这是本地无模型身体/聊天物理切片；没有供应商请求、双客户端渲染、PVP、Hardcore 随机种子或
  M0--M4 证据，正式门禁继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，无真人专用服务器与双启动复核）

- 运行了 `python3 e2e/orchestrator.py server-smoke --forge-version 65.1.0`。
  在没有任何真人客户端、没有模型凭据的真实 Forge 专用服务器上，精确产品 JAR
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`（SHA-256
  `f38cd15e63482a7d235a98cee82f1d83342983336153aca1c749f8fa35912b04`）通过
  `server-smoke-verdict.json`：专服启动、Jar-in-Jar SQLite、MCAI ServerPlayer 自动加入、
  精确 JAR、正常退出和移除全部通过。证据目录为
  `e2e/results/no-commit-dirty/20260810T212610Z-e2e4db73497bb7c`。
- 运行了同一精确工件的 `restart-smoke --forge-version 65.1.0`。两次独立启动共用一个世界，
  数据库数量为 1、生命周期事件为 4、启动/停止各 2 次、companion UUID 保持唯一且两次
  正常退出；`restart-verdict.json` 为 `PASS`。证据目录为
  `e2e/results/no-commit-dirty/20260810T212645Z-e2ee5c662e6b423`。
- 两项都明确是 `functionalAiClaim=false`：没有模型请求、真人客户端、聊天到动作、战斗、
  Hardcore 随机种子或 M0--M4 证据。真实客户端预检仍是 Darwin 缺少 Linux/Xvfb 和当前
  环境模型配置，因此正式门禁继续保持 `NOT_RUN`；这次没有读取或发送任何 API Key。

## 当前继续记录（2026-08-11，多人聊天修复后的工件复核）

- 当前生产开发 JAR 已因真人计数聊天路由重新打包：
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `f38cd15e63482a7d235a98cee82f1d83342983336153aca1c749f8fa35912b04`，17,140,455 bytes。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过；强制重跑
  `ChatAddressingTest` + `ConversationGroundingTest` 通过；Python E2E 45/45、兼容/JSON/diff
  校验通过。Forge 65.1.0 新鲜专服 `auto_presence_on_human_login` 1/1 通过（日志
  `/tmp/mcai-chat-count-presence-651-log-kA1FxB`，目录 `/tmp/mcai-chat-count-presence-651-mLSb2O`）。
- 该物理切片只验证 ServerPlayer 生命周期和在线身份，不包含模型请求；正式真实客户端多人聊天、
  模型、Hardcore 随机种子与 M0--M4 仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，多人聊天边界修复）

- `CommunicationModule` 以前用 `!server.isDedicatedServer()` 推断单人；集成服开放 LAN 后，
  多个真人的普通闲聊会被误当成对 AI 的命令。现在按在线玩家列表统计真人数量并排除当前
  AI 的稳定 UUID：真人数不超过 1 时无需 `@`，真人数大于 1 时必须明确 Agent 名称或 `@`。
  `ChatAddressing` 保留原布尔兼容重载，并新增带真人计数的校验入口。
- `ChatAddressingTest` 覆盖单/多人边界和负数计数拒绝；该修复只改变聊天路由，不给模型增加
  隐藏实体或世界写入权限；新增 `CommunicationModuleSourceContractTest` 防止回退到按服务端
  类型判断。真实客户端多人门禁仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，隔离 Forge GameTest 构建输出）

- 上一轮并行运行 65.0.0/65.1.0 时，两个 Gradle 进程共享 `build/classes`，导致一个进程
  启动时短暂看不到 `AbstractBoatControlInvoker`。为消除这个可重复的编排风险，
  `build.gradle` 现在会在显式提供 `gametest_working_dir` 时按 Forge patch 和临时运行目录
  将构建输出隔离到 `build/gametest-<forge>-<run>/`；普通 `check`、发布 JAR 和用户安装路径
  仍使用稳定的 `build/` 布局。
- 这项改动只影响测试编排，不扩大 Mod 的兼容声明、不改变游戏线程权限，也不把并行或无模型
  GameTest 升级为正式 AI 门禁；下面的并行回归已经按该边界完成。
- 隔离回归已完成：Forge 65.0.0 的 `real_emergency_zombie_skeleton_horde` 与 Forge 65.1.0
  的 `real_player_chat_to_immediate_bound_follow` 同时启动，各自 1/1、约 2 秒完成；两个
  临时构建目录分别为 `/tmp/mcai-isolated-parallel-650-vNR9Am` 和
  `/tmp/mcai-isolated-parallel-651-AZm0rp`，日志为 `/tmp/mcai-isolated-parallel-650-log-0MhOiV`
  与 `/tmp/mcai-isolated-parallel-651-log-tAosYY`。这是编排隔离证据，仍是无模型物理切片。

## 当前继续记录（2026-08-11，Forge 65.0.0 串行物理门禁复核）

- 前一轮把 Forge 65.0.0 与 65.1.0 同时启动，两个 Gradle 运行共享 `build` 输出，65.0.0
  在启动阶段曾短暂报告 `AbstractBoatControlInvoker` Mixin 类不存在；该结果被标记为测试
  编排竞争，未作为产品兼容性失败。
- 在新临时目录 `/tmp/mcai-colloquial-threat-horde-650-single-RQw7Ot` 中以 JDK25、
  `-Pforge_compile_version=65.0.0` 串行重编译并运行
  `mcai_companion:real_emergency_zombie_skeleton_horde`，Forge 65.0.0 的真实 headless
  `ServerPlayer` 物理切片通过 1/1（1.108 s）；日志确认 Mixin 正常加载且包含
  `All 1 required tests passed`。这是无模型应急生存证据，不是实时模型、客户端或 M0--M4
  通过。
- 本次源码对应开发 JAR 为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `756a777d8c56bd0d5fffb5f097b2cad9294b056ff15c8aad8df762bcc1eaffde`，17,139,996 bytes。
  正式模型/真实客户端/Hardcore 随机种子/M0--M4 仍为 `NOT_RUN`；本次没有读取或发送用户
  API Key。
- 隔离改动后的稳定发布路径复核通过：JDK25 `check verifyReleaseJar e2eClientJar
  e2eOracleJar` 16 个任务成功，Python E2E 45/45、JSON/兼容校验和 `git diff --check` 通过，
  产品 JAR 哈希未变。它仍是 dirty-tree 开发工件，不是正式发布或模型门禁通过。

## 当前继续记录（2026-08-11，口语化“谁在打我/我没看到”威胁纠正）

- 扩展 `CompanionConversationCoordinator.correctNearbyThreatClaim` 的自然语言边界：中文“打你、打我、
  在打、伤害、受伤”和英文 “attacking/hitting/hit you/hit me” 现在会被识别为附近威胁问题；
  “我没看到/我看不到/I don't see” 等否认也会在同一规则下接地。
- 纠正仍要求当前第一人称语义中可见僵尸、可信威胁信号或最近受伤至少一项；没有公平信号时不
  臆造敌人，不暴露隐藏实体，也不启动技能。新增两个 JVM 用例覆盖中文口语命中问题和英文命中问题。
- `ConversationGroundingTest` 在 JDK25 `--rerun-tasks` 下通过。后续完整打包与 Forge 物理切片需要在本次
  源码变更后重新生成工件；正式模型/真实客户端/Hardcore 随机种子/M0--M4 仍 `NOT_RUN`，本次没有读取或发送用户 API Key。

## 当前继续记录（2026-08-11，泛化威胁否认纠正）

- 修复了对话事实接地的一个可复现缺口：模型只在玩家明确说出“僵尸”时才会被纠正，
  对“有怪吗/你安全吗/你被攻击了吗”等自然说法可能仍把当前伤害或威胁信号说成“没有怪、
  很安全”。`CompanionConversationCoordinator` 现在把这些问题和中文/英文泛化否认统一交给
  `correctNearbyThreatClaim`；只有当前第一人称语义样本、20 TPS 接触/伤害信号或最近受伤状态
  已授权时才纠正，仍不暴露遮挡实体身份、坐标，也不启动技能。
- 新增 `ConversationGroundingTest` 三个用例：中文最近受伤泛化否认、英文可见僵尸否认、无
  公平威胁信号时不臆造危险。JDK25 完整 `check verifyReleaseJar e2eClientJar e2eOracleJar`
  通过；Python E2E 45/45、JSON/兼容校验和 `git diff --check` 通过。
- 当前精确 Forge 物理切片回归：Forge 65.1.0 的
  `real_player_chat_to_immediate_bound_follow` 1/1（日志输出目录
  `/tmp/mcai-threat-follow-651-NXUF9V`），Forge 65.0.0 的
  `real_emergency_zombie_skeleton_horde` 1/1（`/tmp/mcai-threat-horde-650-gjwOpC`）。两者均
  为无模型 ServerPlayer 物理/应急证据，不升级真实模型聊天、双客户端、PVP 或 M0--M4 门禁。
- 当前开发 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `0de1e6065ba056363c3229a5fd43921b4f85897ea6c2d809bac40e2aeaa01187`，17,139,723 bytes；
  dirty worktree，仍 `NON_RELEASE`。正式模型/真实客户端/Hardcore 随机种子/M0--M4 继续
  `NOT_RUN`，本次没有读取或发送用户 API Key。

## 当前继续记录（2026-08-11，跨平台凭据恢复审计）

- 对 `ApiKeyManager`、`ModelRuntime.restoreCredential` 和三种平台存储实现做了只读审计：
  显式 `MCAI_API_KEY`、`MCAI_API_KEY_FILE` 及 systemd
  `$CREDENTIALS_DIRECTORY/mcai-api-key` 在新进程中优先恢复；未提供注入时再尝试 macOS
  Keychain、Windows 当前用户 DPAPI 或 Linux `secret-tool`/Secret Service。启动恢复仍在
  后台执行，模型探测前会等待该本地恢复，不会把进程内副本误报成重启安全存储。
- Debian/Ubuntu 无桌面 Secret Service 时不会偷偷创建明文文件：设置页只报告
  `saved_verified_process_restart_required`，下一进程应使用只读 Secret 文件或环境注入；Windows
  DPAPI 密文只绑定当前服务账号，Linux 桌面存储依赖实际 Secret Service 会话。
- JDK25 定向回归通过：`CrossPlatformCredentialSourceTest`、
  `ApiKeyManagerRestartPersistenceTest`、`ModelRuntimeTest`、
  `ModelSetupCredentialStatusTest`（BUILD SUCCESSFUL）。本次没有读取、记录或发送任何用户
  API Key，也没有改变生产源码；真实供应商/客户端/Hardcore/M0--M4 门禁仍 `NOT_RUN`。
- 随后一次物理回归编排误用了不存在的 `-Pgametest_selector` 参数，实际启动了完整离线夹具；
  已立即中止，日志 `/tmp/mcai-credential-audit-presence-final.log`、工作目录
  `/tmp/mcai-credential-audit-presence-EPfkyJ` 仅作为编排失败留痕，不计为产品通过/失败，也不
  改变任何正式门禁状态。后续精确 selector 必须使用当前构建脚本定义的参数。
- 使用正确的 `-Plive_model_selector=mcai_companion:auto_presence_on_human_login` 在当前精确
  工件上重跑 Forge 65.1.0，结果 `All 1 required tests passed`（日志
  `/tmp/mcai-credential-audit-presence-correct.log`，目录
  `/tmp/mcai-credential-audit-presence-correct-OKeWeF`）。该切片没有模型请求或真人控制，
  只证明生产 ServerPlayer 生命周期没有回归。

## 当前继续记录（2026-08-11，启动验证期间聊天保留）

- 修复了一个真实的无回应路径：`CompanionConversationCoordinator.submit` 在模型能力探测进行时
  以前直接丢弃普通聊天。现在 runtime 传入 `ModelRuntime.snapshot().probeInFlight()`；只有探测
  确实在进行时才把消息放入最多 32 条、最多 600 ticks 的有界队列，并向发送者明确显示“模型
  正在验证，消息已排队”。队列消息保留原始来源、目标、重试次数和入队 tick，不执行任何世界
  写入或绕过模型决策。
- 探测失败、认证失败、缺少端点或配置替换时不进入该队列；如果探测在排队期间失败，队列会
  在下一服务器 tick 立即清空并逐条明确提示“未执行，请检查 API Key 后重新发送”。等待超过
  600 ticks 的旧消息也会被丢弃并明确提示“未执行，请重新发送”，避免换 Key 后执行陈旧命令。
  重试消息保持原始队列类型，不会被错误地当作启动等待消息过期。
- 新增 `ConversationCommitmentSourceContractTest` 覆盖队列条件、上限、过期信号、失败清空和
  runtime wiring；定向测试通过。JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`
  通过，Python E2E 45/45、JSON/兼容校验通过；Forge 65.1.0
  `auto_presence_on_human_login` 新鲜专服 GameTest 1/1 通过（日志
  `/tmp/mcai-startup-queue-gate-final.log`，目录 `/tmp/mcai-startup-queue-gate-fTOgmK`）。
  当前产品 JAR SHA-256 为
  `0ee45f14002270e2a2ca9ca9ce987f175d02882657aa3ad0d0554548d0051ced`，大小 17,138,102 bytes。
- 这修复的是启动阶段消息丢失，不等于真实模型聊天到动作或 M0--M4 通过；正式模型/客户端、
  Hardcore 统计、双客户端渲染和 M0--M4 仍 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`，产物仍
  `NON_RELEASE`。

## 当前继续记录（2026-08-11，任务承诺不再阻塞动作）

- 修复规划器的另一条“光说不做”路径：`BrainOrchestrator` 在收到 `ASK_PLAYER` 时，除了
  “好的/我这就来”等短承诺，也识别“目标已接受”“任务已创建”“开始执行”“正在前往”以及
  对应的英文状态承诺。它们会被当作未启动技能的承诺，清除等待玩家状态并触发有界重规划；不
  会凭空选择技能、生成坐标或写入世界。聊天层同步把违反 `REPLAN` 约束的同类承诺拦截为
  “未创建任务”，避免广播已经在行动的假话。
- `BrainOrchestratorTest`、`PlayerTaskIntentTest` 定向测试通过；随后 JDK25
  `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，Forge 65.1.0 新鲜专服
  `auto_presence_on_human_login` 1/1 通过（日志 `/tmp/mcai-action-commitment-gate-final.log`，
  目录 `/tmp/mcai-action-commitment-gate-8xCBvu`）。当前产品 JAR SHA-256 为
  `c2b51def6ac70c003e0166e1e829ca7070ef70f475ffb569a2585edc946bdf4d`，大小 17,138,918 bytes。
- 该修复只消除“状态话术让规划器进入 WAITING_FOR_PLAYER”的确定性死路；真实供应商模型的
  任务理解、实时客户端、Hardcore 随机种子和 M0--M4 仍 `NOT_RUN`，不能据此宣称专业陪玩或
  两小时通关。

## 当前继续记录（2026-08-11，Forge 65.x 物理子集兼容矩阵）

- 使用已注册的完整 GameTest selector（`mcai_companion:...`）和独立临时工作目录，当前源代码
  对官方已发布的 Forge 65.0.0、65.0.1、65.0.2、65.0.3、65.0.4、65.0.5、65.0.6、
  65.0.7、65.0.8、65.0.9、65.1.0 逐项运行了 `real_parkour_course`、`real_furnace_batch`、
  `auto_presence_on_human_login`、`real_emergency_zombie_skeleton_horde`。
- 结果为 44/44 个独立服务端运行通过（每次 1/1，日志均含 `All 1 required tests passed`）。
  机器可复核的完整摘要在 `/tmp/mcai-compat-65x-python-correct-summary.txt`；对应日志为
  `/tmp/mcai-compat-correct-65.*.log`，每行同时记录隔离 run directory。该摘要不是模型请求、
  客户端渲染或隐藏种子结果。
- 之前的裸 selector 和 macOS `timeout` 编排失败已保留在独立 `/tmp` 日志中，分别属于命令编排
  错误（未找到测试/`rc=255`）与宿主工具缺失（`rc=127`），不计入产品失败或通过；本次 44/44
  使用完整命名空间和 Python 跨平台硬超时重新执行。
- 这是当前源的无模型物理兼容子集证据：跑酷、原版熔炉事务、无人服务器自动在场和十僵尸/十骷髅
  应急生存。它不能升级完整 Forge patch matrix、chat-to-action、PVP、双客户端、Hardcore
  随机种子或 M0--M4；正式模型/客户端与 M0--M4 仍 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`，产物
  仍 `NON_RELEASE`。
- 收尾校验通过：`python3 -m json.tool docs/progress/GOAL_STATE.json`、
  `python3 scripts/validate-compat.py`、JDK25 `./gradlew build --no-daemon`（16 tasks）和
  Python E2E `45/45`。当前唯一产品 JAR 为
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为
  `1c8ea9740091e3385f53c61adf9e7b3ffcd4e44d15edb5389e65320d313cba7d`、17,136,675 bytes；
  这仍是 dirty-tree 开发产物，不是发布包。

## 当前继续记录（2026-08-11，Forge 主版本发现门禁）

- 官方 Forge 26.2 页面与 `promotions_slim.json` 在 2026-08-10 19:08:36 UTC
  复核仍显示 65.0.0--65.0.9、65.1.0，Latest/Recommended 为 65.1.0，没有 66 主线。
- 新增 `scripts/discover-forge-lines.py` 与 5 个离线夹具测试；它按官方促销清单发现
  Forge 65+ 主版本，并把官方下载索引的已发布 patch 与锁文件逐项比较，发现未声明适配
  模块或新 patch 时返回失败，不能把 65 JAR 伪装成 66。Gradle `forge-major-discovery`
  在线任务实际通过；Python E2E 当前为 44/44。
- 这只是版本发现/发布阻断门禁，不是 Forge patch 矩阵、模型客户端、M0--M4 或陪玩能力证据；
  当前源码仍 `DIRTY_NO_COMMIT`，产物仍 `NON_RELEASE`，正式 AI 门禁保持 `NOT_RUN`。

## 当前继续记录（2026-08-11，状态证据去重审计）

- `docs/progress/GOAL_STATE.json` 已完成重复键审计（508 个唯一键、无重复），并保留较早
  的同名证据为带时间戳的历史键；`M0--M4` 与正式模型/客户端门禁状态没有被改写。

## 当前继续记录（2026-08-11，跑酷物理双 Forge 回归）

- `real_parkour_course` 在新鲜工作目录中以真实 headless `ServerPlayer` 通过 Forge
  65.1.0 与 65.0.0 各 1/1（2.426 s、2.478 s）。日志确认 AI 身体实际完成跳跃/平台
  物理路径并正常离场；没有模型请求、传送、隐藏区块读取或预置胜利状态。
- 这只证明本地运动执行器的跑酷物理下限，不证明模型会观察、规划或在随机世界自主跑酷，
  也不能升级 movement、M1--M4 或专业陪玩门禁；正式实时模型/客户端门禁仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，补齐零真人基础场景的 live-model 伪通过防线）

- 审计发现 `real_zero_human_dedicated_server_foundation` 虽然委托
  `LiveModelChatGameTests.zeroHumanDedicatedServerToLiveModelFoundation`，但 selector 名不含
  `live_model`，此前可能在无 `-Plive_model_test=true` 时走 `helper.succeed()` 旁路；该结果不计
  为任何基础生存证据。
- `build.gradle` 已将 `zero_human_dedicated_server_foundation` 纳入配置期 fail-closed 选择器。
  定向复跑 Forge 65.1.0 `/tmp/mcai-zero-human-foundation-selector-guard-20260811` 在配置阶段
  exit 1，明确要求真实模型开关和配置，没有启动游戏、没有供应商请求；正式模型/客户端及
  M0--M4 仍 `NOT_RUN`。

## 当前继续记录（2026-08-11，选择器防线后的完整工件重建）

- 在上述 `build.gradle` 防线之后重新运行 JDK25
  `./gradlew --no-daemon check verifyReleaseJar e2eClientJar e2eOracleJar`，16 个任务通过。
  产品 JAR 仍为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`，17,135,452 bytes。
  该构建只重建验证工件；正式模型、真实客户端和 M0--M4 仍 `NOT_RUN`。
- 当前精确 JAR 的 Forge 65.1.0 dedicated-server smoke
  `/Users/weida/Documents/minecraft-ai-companion-forge/e2e/results/no-commit-dirty/20260810T184143Z-20260811-selector-guard-final`
  为 `PASS`：ServerPlayer 生命周期、Jar-in-Jar SQLite、精确 SHA 和正常退出均通过；该
  smoke 明确标记 `functionalAiClaim=false`，oracle 因 smoke 主动停止服务器而非玩法通过。

## 当前继续记录（2026-08-11，无真人专服远端模拟与重生双 Forge 回归）

- `zero_human_dedicated_server_chunk_and_respawn` 在 Forge 65.1.0
  `/tmp/mcai-zero-human-remote-simulation-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-zero-human-remote-simulation-20260811-floor` 均通过 1/1（1.692 s、1.775 s）。
  测试断言无真人玩家、AI `ServerPlayer` 自己维持远端模拟区块/更新、受到致命伤害后走
  原版非 Hardcore 重生并更新会话代数；日志只出现 `MCAI[embedded]`。这是无模型身体与
  服务器票据证据，不是模型寻路、战斗或 M0--M4 正式通过。

## 当前继续记录（2026-08-11，生产 ServerStarted 自动在场双 Forge 回归）

- 以 `-Pzero_human_autospawn_test=true` 运行同一无真人切片，验证的是生产
  `CompanionRuntime.onServerStarted` 自动 `requestSpawn` 路径而非测试手动 spawn：Forge 65.1.0
  `/tmp/mcai-zero-human-autospawn-production-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-zero-human-autospawn-production-20260811-floor` 均 1/1 通过（1.474 s、1.700 s）。
  0 个真人在线时 AI 自己进入 `PlayerList`，随后通过远端模拟和原版重生断言；没有模型请求。

## 当前继续记录（2026-08-11，铁傀儡与群体攻击应急回归双 Forge）

- `real_emergency_iron_golem_duel` 在 Forge 65.1.0 与 65.0.0 各通过 1/1（1.010 s、981.6 ms）。
  真实 headless `ServerPlayer` 使用原版装备/盾牌和本地应急控制，未站桩死亡；没有模型请求。
- `real_emergency_zombie_skeleton_horde` 在 Forge 65.1.0 与 65.0.0 各通过 1/1（1.154 s、1.190 s），
  日志出现原版 `Monster Hunter`，证明群体接触下有真实伤害/动作链；这是本地应急生存下限，
  不是模型选择目标、PVP、客户端观感或正式 M0--M4 证据。

## 当前继续记录（2026-08-11，API Key 恢复与启动协调定向 JVM 回归）

- JDK25 定向运行 `ApiKeyManagerRestartPersistenceTest`、`ModelRuntimeTest` 和
  `ModelBootstrapCoordinatorTest`，共 29/29 通过（0 failures、0 errors、0 skipped）。覆盖
  process/持久化 credential 恢复、认证失败后隔离、普通世界启动 restore 与单次 probe 协调；
  不读取或发送用户 token，也不等价于供应商实际可用。

## 当前继续记录（2026-08-11，最新真实功能 preflight 仍外部阻断）

- `python3 e2e/orchestrator.py preflight --forge-version 65.1.0` 于
  `2026-08-10T18:50:03Z` 返回 `ready=false`、exit 2：当前宿主 Darwin，缺少隔离 Linux worker、
  Xvfb、`MCAI_BASE_URL`、`MCAI_MODEL` 与 `MCAI_API_KEY(_FILE)`。预检未启动客户端、未触碰
  物理显示器、未发起供应商请求；真实模型聊天/动作、客户端和 M0--M4 继续 `NOT_RUN`。

## 当前继续记录（2026-08-11，切片后完整构建与协议回归）

- 在末影人、传送门、基础合成和遮挡工作台切片后，JDK25
  `./gradlew --no-daemon check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks；
  当前产品 JAR 为 `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
  `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`（17,135,452 bytes）。
- `python3 -m unittest discover -s e2e -p 'test_*.py' -v` 为 39/39；它仍只覆盖证据协议、
  兼容性、工件绑定和安全拒绝，不是模型/客户端玩法通过。JSON 与 `git diff --check` 均通过。

## 当前继续记录（2026-08-11，遮挡工作台侧移恢复双 Forge 回归）

- `occluded_iron_toolkit_table` 在 Forge 65.1.0
  `/tmp/mcai-occluded-table-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-occluded-table-gate-20260811-floor` 均通过 1/1（1.788 s、1.846 s）。
  真实 headless `ServerPlayer` 在熔炉/方块遮挡工作台时，按第一人称可见面多次合法侧移，
  重新获得可达交互面，再通过原版菜单完成铁镐链；没有读取隐藏方块或传送。
- 这是针对卡方块/卡门类失败的无模型 M1 物理回归，不是模型自主判断、客户端观感或正式
  M1--M4；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，基础合成与菜单事务双 Forge 回归）

- `reachable_basic_crafting` 在 Forge 65.1.0
  `/tmp/mcai-basic-crafting-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-basic-crafting-gate-20260811-floor` 均通过 1/1（1.628 s、1.685 s）。
  真实 headless `ServerPlayer` 通过第一人称可达工作台、原版菜单槽位与配方结果点击完成
  基础合成；没有预填产物或直接修改背包。
- 这是 M1 基础技能的无模型物理下限，不能替代模型自主资源判断、客户端观感或 M1--M4
  正式统计；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，已验证传送门往返双 Forge 回归）

- `real_verified_portal_return` 在 Forge 65.1.0
  `/tmp/mcai-portal-return-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-portal-return-gate-20260811-floor` 均通过 1/1（3.139 s、3.069 s）。
  真实 headless `ServerPlayer` 通过原版下界传送门进入、在下界执行返回路线并完成回返
  验证；门禁保留了碰撞/落点诊断日志，没有使用传送指令或隐藏区块数据。
- 这是受控无模型跨维度/移动物理证据，不是供应商模型的寻路、视觉理解、随机 Hardcore 或
  M0--M4；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，末影人攻击下的即时防御双 Forge 回归）

- `real_emergency_enderman_defense` 在 Forge 65.1.0
  `/tmp/mcai-enderman-defense-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-enderman-defense-gate-20260811-floor` 均通过 1/1（1.462 s、1.637 s）。
  真实 headless `ServerPlayer` 在单个末影人攻击下完成原版护甲/盾牌/武器事务，进入本地
  `EMERGENCY_SURVIVAL`、产生合法位移并存活；没有直接伤害目标或写入作弊方块。
- 这是针对“被打后只看着、完全不还手/不撤退”的本地生存下限回归，未调用模型，不能替代
  真实聊天到动作、客户端动画、PVP 或 M0--M4；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，真实功能预检的基础设施与配置外部阻断）

- 运行 `python3 e2e/orchestrator.py preflight --forge-version 65.1.0` 的只读预检，结果为
  `ready=false`、exit 2。当前宿主是 Darwin；没有隔离 Linux worker、Xvfb、
  `MCAI_BASE_URL`、`MCAI_MODEL` 或 `MCAI_API_KEY(_FILE)`。Java 存在，但没有启动客户端、
  触碰物理显示器、读取或发送密钥，也没有发起供应商请求。
- 因此前一次显式 live-model GameTest 的 `INVALID_CONFIGURATION` 仍是最后真实模型门禁结果：
  真实专服/AI 身体启动成功，但 `requestsMade=0` 后失败；聊天到动作、真实客户端和 M0--M4
  继续保持 `NOT_RUN`。源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。
- 下一步继续做可复核的本地原版物理切片；获得合规模型配置和 Linux/Xvfb worker 后，必须从
  preflight 开始重新跑真实 dedicated + Actor/Observer 流程，不能沿用无模型结果。

## 当前继续记录（2026-08-11，真实 live-model 探测的诚实外部阻断）

- 在显式 `-Plive_model_test=true` 下运行
  `real_player_chat_to_surprise_zombie_defense`（Forge 65.1.0，
  `/tmp/mcai-live-preflight-gate-20260811`）。测试确实启动专服并创建 AI 身体，随后模型探测
  以 `ModelFailure[kind=INVALID_CONFIGURATION, safeMessage=Base URL or model name is not configured]`
  失败，`requestsMade=0`，GameTest exit 1；不是 skip、不是动作通过，也没有消耗 token。
- 这把“实际模型门禁”和“无模型物理门禁”分开了：当前机器没有可用的模型 Base URL/Model
  配置与凭据，真实聊天→战斗仍是外部阻断。源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`，
  M0--M4/正式模型客户端门禁继续 `NOT_RUN`；拿到可验证配置后必须重新跑，不沿用这个失败作
  成功声明。

## 当前继续记录（2026-08-11，落水自救与史莱姆接触防御双 Forge 回归）

- `real_water_clutch` 在 Forge 65.1.0 `/tmp/mcai-water-clutch-gate-20260811-a` 与
  Forge 65.0.0 `/tmp/mcai-water-clutch-gate-20260811-floor` 均通过 1/1（784.8 ms、
  693.1 ms）。日志显示真实 headless `ServerPlayer` 进入 `PREPARING_WATER`、
  `DEPLOYING_WATER`、`BRACING_FALL`，水桶使用和落地由本地应急层完成。
- `real_emergency_slime_defense` 在 Forge 65.1.0 `/tmp/mcai-slime-defense-gate-20260811-a`
  与 Forge 65.0.0 `/tmp/mcai-slime-defense-gate-20260811-floor` 均通过 1/1（815.3 ms、
  882.4 ms），日志含原版 `Cover Me with Diamonds`、`Monster Hunter`，说明史莱姆接触下
  的装备/攻击链没有站桩等死。两条均为无模型本地应急/原版物理证据，不代表实时模型、
  客户端观感、随机 Hardcore 或正式 PVP/M0--M4；源码仍 `DIRTY_NO_COMMIT`、产物仍
  `NON_RELEASE`。

## 当前继续记录（2026-08-11，修复 live-model 选择器的伪通过路径）

- 审计用户要求的“所有测试必须基于 AI”时发现：`real_player_chat_to_surprise_zombie_defense`
  在没有 `-Plive_model_test=true` 时会沿用测试类的 no-model `helper.succeed()` 旁路，先前
  那次 65.1.0 运行的 `All 1 required tests passed` 不构成战斗证据，已明确作废。
- 已在 `build.gradle` 增加选择器门禁：名称含 `live_model`、`surprise_zombie_defense` 或
  `critical_golden_apple` 的场景若未显式开启真实模型，配置阶段直接失败并提示使用
  `-Plive_model_test=true` 与有效凭据；不会再把跳过模型的结果记成通过。刚才的定向复跑
  `/tmp/mcai-zombie-chat-defense-gate-20260811-a2` 按预期以该清晰错误退出（exit 1），没有
  启动游戏、没有调用供应商，也没有消耗 token。
- 当前仍没有有效供应商凭据/客户端环境可完成真实聊天到动作门禁；源码 `DIRTY_NO_COMMIT`、
  产物 `NON_RELEASE`，M0--M4 与正式模型/客户端门禁继续保持 `NOT_RUN`。下一步继续运行
  仅限无模型的本地物理下限，所有 live-model 选择器都必须在有效环境中显式运行。
- 修复后 `./gradlew --no-daemon check` 在 JDK25 下通过（compat checker、JVM tests、编译均
  成功）；这只验证测试门禁和源码构建，没有把真实模型/客户端门禁升级为通过。
- 随后完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks；当前产品 JAR
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`。这是可复核的
  非发布工件，不能替代真实供应商模型/客户端门禁。
- 精确产品 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T182412Z-20260811-selector-guard-water-slime/`
  为 `PASS`：服务端退出码 0、产品 SHA 匹配、SQLite Jar-in-Jar/ServerPlayer 生命周期均通过，
  `functionalAiClaim=false`。其中 Oracle 的 `server_stopped_before_result` 是该 smoke 主动先停服
  的预期结果，不是模型动作失败，也不被当作聊天/移动验收。
- 离线 Python E2E `python3 -m unittest discover -s e2e -p 'test_*.py' -v` 当前为 39/39；它
  只验证证据协议、兼容性、工件绑定和安全拒绝路径，不替代真实客户端或供应商模型。

## 当前继续记录（2026-08-11，真人登录自动在场与 TAB 身份回归）

- 针对“单人/专服里没有 AI 身体、按 TAB 看不到在线状态”的反馈，运行当前源码的
  `auto_presence_on_human_login`：Forge 65.1.0
  `/tmp/mcai-auto-presence-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-auto-presence-gate-20260811-floor` 均通过 1/1（602.9 ms、598.7 ms）。
  真实 `PlayerList` 登录日志确认 `TestHuman` 后自动加入 `MCAI`；门禁断言权威
  `ServerPlayer`、同维度、距离真人不超过 12 格、TAB 名称以 `[AI] ` 开头，并在无已验证
  模型时保持身体稳定 40 tick。
- 这条证明的是无模型时的自动在场与公开身份/安全待机，不证明模型能理解聊天、行动或战斗，
  也不替代客户端实际 TAB 渲染；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`，M0--M4
  与正式模型/客户端门禁仍 `NOT_RUN`。下一步继续验证受攻击时的即时防御和模型恢复后的动作，
  然后再进入可用凭据与 Linux/Xvfb 真实客户端门禁。

## 当前继续记录（2026-08-11，真人聊天触发即时跟随与 AI 在场回归）

- 针对“跟我走只说不做/世界里看不到 AI”的反馈，运行当前源码的
  `real_player_chat_to_immediate_bound_follow`：Forge 65.1.0
  `/tmp/mcai-follow-presence-gate-20260811-a` 与 Forge 65.0.0
  `/tmp/mcai-follow-presence-gate-20260811-floor` 均通过 1/1（分别 1.300 s、
  1.308 s）。日志确认真实 `TestHuman` 与 `MCAI` 同时加入 `PlayerList`，聊天栏出现
  `[AI] MCAI：收到，任务已经创建；我正在规划第一个动作。`，随后跟随状态通知；测试对
  实体加入、真人聊天绑定和实际身体跟随作了断言。
- 这条是服务端真实玩家生命周期/物理回归，使用受控无模型 gateway，不能冒充供应商模型
  视觉理解、客户端渲染、随机世界或专业陪玩验收；源码仍 `DIRTY_NO_COMMIT`、产物仍
  `NON_RELEASE`，M0--M4 与正式模型/客户端门禁仍 `NOT_RUN`。下一步继续验证自动在场/Tab
  在线状态与受攻击后的即时防御，再转入可用凭据和 Linux/Xvfb 的真实模型客户端门禁。

## 当前继续记录（2026-08-11，临界金苹果真实双 Forge 回归）

- 针对“4 点血仍等更低再吃”的反馈，当前源码运行
  `real_offline_critical_golden_apple`：Forge 65.1.0
  `/tmp/mcai-golden-apple-gate-20260811-a` 通过 1/1（605.1 ms），Forge 65.0.0
  `/tmp/mcai-golden-apple-gate-20260811-floor` 通过 1/1（611.1 ms）。受控场景不给
  模型决策，真实 ServerPlayer 在临界生命值下通过本地应急层把自有金苹果走普通库存
  装备和持握使用路径，并以消费/生命/吸收结果核验；headless 收尾聊天回退警告不影响
  `All 1 required tests passed`。
- 这只证明本地生存下限不会等待模型的危险话术；它不代表模型能理解所有赠礼语境，也不
  代表随机 Hardcore、客户端动画或完整通关已通过。源码仍 `DIRTY_NO_COMMIT`、产物仍
  `NON_RELEASE`；下一步继续以同样边界验证战斗/装备组合，并准备真实模型门禁。

## 当前继续记录（2026-08-11，掉落物拾取链真实双 Forge 回归）

- 为区分“预置背包装备”与用户实际把东西丢给 AI，本次运行了
  `headless_player_lifecycle_state_and_fair_action`。Forge 65.1.0
  `/tmp/mcai-lifecycle-item-gate-20260811-a` 通过 1/1（24.61 s），Forge 65.0.0
  `/tmp/mcai-lifecycle-item-gate-20260811-floor` 通过 1/1（30.44 s）。同一真实
  `ServerPlayer` 生命周期先完成登录/重登、菜单和维度事务，再在第一人称语义观察到
  掉落物后以有限步幅走近，依靠 vanilla `ItemEntity` 碰撞拾取，最后以自身库存增长
  核验；日志还包含下界/末地、战斗和返回流程。
- 这条证据确认“掉落物→靠近→原版拾取→库存事实”在无模型 headless 物理链可用，
  但本次生命周期夹具用受控 gateway 直接驱动技能，不是供应商模型聊天，也不证明
  真人客户端渲染、随机种子或完整陪玩。Forge 65.0.0 的运行时窗口统计为
  `averageNanos=335441`、`rollingP95Nanos=1255833`，属于测试服务器指标，不能替代
  正式性能门禁。
- 本批没有新增生产代码；装备与金苹果校正后的产品 JAR 不变，源码仍
  `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。下一步把这条链与真实模型的聊天到
  `collect_observed_item`/`equip_item` 决策接上，并保留失败闭环。

## 当前继续记录（2026-08-11，装备反馈的真实无模型回归）

- 针对“给它东西也不穿戴”的反馈，复跑了仓库中的 `offline_idle_equipment`：真实
  `PlayerList`/headless `ServerPlayer` 通过原版库存菜单把已拥有的铁头盔和盾牌装备到
  对应槽位，并获得原版 `Suit Up` advancement。Forge 65.1.0 在
  `/tmp/mcai-equipment-gate-20260811-a` 通过 1/1（573.4 ms），Forge 65.0.0 在
  `/tmp/mcai-equipment-gate-20260811-floor-a` 通过 1/1（630.3 ms）。
- 该门禁没有模型请求，也没有直接改装备 NBT；它只证明“物品已经在 AI 自己背包里时，
  vanilla inventory/menu 装备路径可用”。它不证明玩家丢物品后的拾取、模型理解或完整
  陪玩链路；专服收尾的 headless 聊天回退警告不影响 `All 1 required tests passed`。
- 本批没有新增生产代码，最新产品 JAR 仍为金苹果事实校正后的
  `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`；源码仍
  `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。下一步继续把“丢弃物品→公平拾取→观察库存→
  装备”的链路拆成独立门禁，再转入真实客户端/供应商模型环境。

## 当前继续记录（2026-08-11，金苹果库存事实校正）

- 根因：对话事实校正原先只检查“低血量却等待更低再吃”，没有检查模型是否凭空声称
  “我已经有一个金苹果”。新增 `CompanionConversationCoordinator.correctGoldenAppleInventoryClaim`，
  只依据服务端权威背包数量纠正中英文拥有/没有陈述；它不拾取、生成、装备、食用物品，也不
  选择技能。新增 `ConversationGroundingTest` 4 项测试，中英文反事实和无关知识性描述均通过。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39；
  产品 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` 为 17,135,452 bytes，SHA-256
  `7d2686a0710d46373068d6122e5fd589e587208fa3f57b5a7f4448f12ed575a6`。精确 JAR 专服 smoke
  `e2e/results/no-commit-dirty/20260810T180414Z-20260811-golden-apple-grounding/` 为 PASS，
  退出码 0、Jar-in-Jar SQLite/ServerPlayer 生命周期通过，`functionalAiClaim=false`。
- 本批仍没有真实供应商模型/客户端动作证据；最后失败门禁仍是外部模型/客户端环境缺失，
  源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。下一步继续把用户反馈转为可验证的本地事实边界，
  不把聊天纠正误报成“AI 已会玩”。

## 当前继续记录（2026-08-11，模型动作防线与跨平台凭据恢复定向验证）

- 定向 JVM 回归通过：`PlayerTaskIntentTest`、`BrainOrchestratorTest`、
  `EmergencySurvivalControllerTest`、`ConversationCommitmentSourceContractTest`；模型返回
  “好的/我这就来”但未产生 `START_SKILL` 时不会把动作说成已完成，普通目标在有限重试后进入
  明确等待/安全状态，跟随请求仍可由服务端绑定目标直接启动。
- 凭据/设置定向 JVM 回归通过：`ApiKeyManagerRestartPersistenceTest`、
  `CrossPlatformCredentialSourceTest`、`ApiKeyManagerConcurrencyTest`、
  `ModelSetupCredentialStatusTest`、`ModelProfileStoreTest`、`ModelRuntimeTest`。这验证了
  macOS Keychain、Windows DPAPI、Linux Secret Service/进程凭据的选择与重启恢复契约；真实
  OS 凭据后端和用户模型服务仍需实际机器/有效密钥确认，不能把单元测试当成现场 API 通过。
- 本批没有生产代码修改，仅有该检查点和后续目标记录；最后失败门禁仍为真实供应商模型/客户端
  环境缺失，源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。下一步继续保留“不把话术当动作”的
  证据边界，推进真实客户端/有效模型门禁准备。
- 随后当前源完整打包校验通过 `check verifyReleaseJar e2eClientJar e2eOracleJar`（16 tasks），
  Python E2E 39/39；精确产品 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` 为
  17,134,421 bytes、SHA-256 `9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`。
  最新专服 smoke `e2e/results/no-commit-dirty/20260810T180105Z-20260811-followup-parkour-zero-human/`
  为 PASS，但 `functionalAiClaim=false`。

## 当前继续记录（2026-08-11，跑酷与无真人专服区块/重生回归）

- 跑酷：`real_parkour_course` 在 Forge 65.1.0 `/tmp/mcai-parkour-gate-20260811-a`
  通过 1/1（2.460 s），Forge 65.0.0 `/tmp/mcai-parkour-gate-20260811-floor-a` 通过
  1/1（2.574 s）。这是 headless `ServerPlayer` 的真实跳跃、落地和障碍物物理，不是模型
  轨迹脚本，也不等于复杂地形/随机种子跑酷成功率。
- 无真人专服：`zero_human_dedicated_server_chunk_and_respawn` 在 Forge 65.1.0
  `/tmp/mcai-zero-human-gate-20260811-a` 通过 1/1（1.682 s），Forge 65.0.0
  `/tmp/mcai-zero-human-gate-20260811-floor-a` 通过 1/1（1.744 s）。测试验证没有真人在线时
  AI 自动出现、远端实体/区块继续 tick、死亡和重生；收尾断开产生的 headless 聊天回退警告
  不影响 `All 1 required tests passed`。
- 根因/改动边界：本批没有生产代码修改；它验证了“远离玩家不应因没有客户端视距而停止”和
  “基础跑酷物理”两条底层前提，但仍不能解释或掩盖实时模型只说不做。已改文件只有本检查点、
  `IMPLEMENTATION_STATUS.md`、`changelog.txt` 和 `GOAL_STATE.json`；源码仍 `DIRTY_NO_COMMIT`、
  产物仍 `NON_RELEASE`。
- 最后失败门禁仍是正式实时模型/客户端门禁缺失；下一步保持同一证据边界，继续原版能力切片，
  然后转入 Linux/Xvfb 真实客户端与有效供应商凭据，而不是用无模型 GameTest 代替。

## 当前继续记录（2026-08-11，真实应急 PVE 回归：十僵尸十骷髅与铁傀儡）

- 针对“被怪物攻击仍站着不动”的反馈，复跑了无模型、真实 `ServerPlayer` 的本地应急控制门禁：
  10 个僵尸 + 10 个骷髅在 Forge 65.1.0 `/tmp/mcai-horde-gate-20260811-a`（1/1，
  1.105 s）和 Forge 65.0.0 `/tmp/mcai-horde-gate-20260811-floor-a`（1/1，1.282 s）均通过。
- 铁傀儡单挑同样通过：Forge 65.1.0 `/tmp/mcai-golem-gate-20260811-a`（1/1，1.031 s），
  Forge 65.0.0 `/tmp/mcai-golem-gate-20260811-floor-a`（1/1，993.1 ms）。两条均经过真实
  `PlayerList` 登录、原版伤害/实体 tick 和应急控制器，不是站桩脚本；收尾时出现的
  `StacklessClosedChannelException` 是 headless 测试主动断开后的聊天回退警告，测试仍明确为
  `All 1 required tests passed`。
- 本批没有新增生产代码；证据确认现有 `EmergencySurvivalController` 在无模型时会进入应急战斗，
  但不等于实时模型 PVP、客户端观感、随机 Hardcore 或 M0--M4 正式通过。源码仍
  `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。
- 当前反馈的可证根因边界是“模型/客户端真实动作链尚未通过正式门禁”，而不是把无模型应急
  控制器的通过误报成模型已会战斗；本批已改文件只有本检查点、`IMPLEMENTATION_STATUS.md`、
  `changelog.txt` 和 `GOAL_STATE.json`，没有借测试夹具掩盖生产行为。
- 最后失败门禁仍是更早的真实客户端/模型门禁缺失（不是本批 PVE）；下一步继续独立原版能力
  的双 Forge 回归，随后必须转入真实客户端与有效模型凭据门禁，不能把这些无模型结果升级为
  “专业陪玩”或“两小时通关”。

## 当前继续记录（2026-08-11，木门开关真实空手交互）

- 为直接覆盖“卡门”反馈，新增 `RedstoneGameTests.doorOpenClose`、包装器
  `real_door_open_close` 和对应 test-instance。真实 `ServerPlayer` 通过第一人称射线与
  `FairPlayerActuator.useOnBlock` 空主手连续开门、关门，状态变化由原版 `DoorBlock` 决定。
- Forge 65.1.0 `/tmp/mcai-door-gate-20260811-d` 和 Forge 65.0.0
  `/tmp/mcai-door-gate-20260811-floor-b` 各通过 1/1。首轮失败是测试断言把采样器的
  `BlockCoordinate` 与 `BlockPos` 直接 `equals`，日志显示射线实际命中木门；修正逐坐标
  比较后，第二轮又验证了开门后门扇位置变化，加入有限的第一人称重新瞄准后通过。
- 这条门禁与按钮/发射器门禁共同验证空手方块交互修复，但仍不是模型聊天、导航、PVP、
  随机 Hardcore 或 M0--M4 正式门禁。源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`。
- 随后重新执行 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`，通过 16 tasks；
  Python E2E 39/39、JSON 校验也通过。产品 JAR SHA-256 为
  `9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`。精确 JAR 专服
  smoke `e2e/results/no-commit-dirty/20260810T175120Z-20260811-door-open-close/`
  为 `PASS`，退出码 0、精确 JAR、SQLite Jar-in-Jar 和 ServerPlayer 生命周期通过，
  `functionalAiClaim=false`。下一步继续下一条独立能力或转入真实客户端/模型基础设施，
  不把脚本/无模型结果冒充专业陪玩。

## 当前继续记录（2026-08-11，按钮到发射器的真实空手红石交互）

- 新增 `RedstoneGameTests.dispenserButtonActivation` 及其 `EmbodimentGameTests` 包装器和
  `real_dispenser_button_activation` test-instance。它使用合法 `GameProfile`、真实
  `PlayerList.placeNewPlayer`/`handleAcceptPlayerLoad` 生命周期、第一人称射线和
  `FairPlayerActuator.useOnBlock`，验证按钮通电、发射器消耗箭并生成原版箭实体，以及
  原版延时复位；没有直接改方块或实体。
- Forge 65.1.0 全新 `/tmp/mcai-dispenser-button-gate-20260811-a16` 与 Forge 65.0.0
  下限 `/tmp/mcai-dispenser-button-gate-20260811-floor-a1` 均通过 1/1，日志均为
  `All 1 required tests passed`。首次失败已保留为诊断证据：射线几何命中错误、裸 mock
  玩家不在 PlayerList、客户端加载保护以及在原版调度前断言按钮状态；最终测试改为真实
  登录生命周期并等待原版时序。
- 生产根因是无客户端 `handleUseItemOn` 对空主手返回 dispatch，却没有执行按钮等方块的
  `useWithoutItem` 副作用。`FairPlayerActuator.useOnBlock` 现在在既有公平射线、触及距离、
  权限和冷却检查后，对空主手调用 `ServerPlayerGameMode.useItemOn` 并挥手；非空物品仍走
  原版数据包路径。这是修复 headless 原版语义，不是绕过公平边界的世界修改。
- 修复后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python
  E2E 39/39，JSON 校验通过；产品 JAR SHA-256 为
  `9662a352a3cc414d150c2e4e70756f1ced5e27ebc4dee6f49f127daef9b7d665`。精确 JAR 专服
  smoke `e2e/results/no-commit-dirty/20260810T174258Z-20260811-redstone-button/`
  为 `PASS`，退出码 0、精确 JAR、SQLite Jar-in-Jar 和 ServerPlayer 生命周期均通过，
  `functionalAiClaim=false`。
- 当前源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`；实时供应商模型、真实客户端
  Actor/Observer、随机 Hardcore 与 M0--M4 正式门禁仍 `NOT_RUN`。本批最后失败门禁是
  a15 在原版复位后的 tick 21 仍断言按钮通电，已改为 tick 10 检查通电、tick 25 检查复位。
- 下一步继续补一条尚无独立双版本证据的原版/运输原子能力，或转入所需真实客户端/模型
  基础设施；每条能力都重复 GameTest、打包和精确 JAR smoke，绝不把无模型门禁升级为
  专业陪玩、PVP 或两小时通关声明。

## 当前继续记录（2026-08-11，漏斗/发射器公平红石库存事务回归）

- 在木桶、潜影盒和末影箱之后，新增 `real_hopper_transaction` 与
  `real_dispenser_transaction`。两者都通过真实方块菜单观察玩家/容器槽位，使用普通菜单
  转移、取出和快速移动；发射器红石发射时序仍未被这条库存门禁冒充。
- 第一次尝试诚实捕获了两类测试编排错误：把两个选择器放进一次参数时 Forge 只按一个
  匹配串运行，不能据此宣称两项都通过；新 test-instance JSON 还漏了必需的 `type`，导致
  注册表在服务器启动前拒绝加载。改成仓库现有 `minecraft:function` 资源格式并逐项重跑后，
  Forge 65.1.0 的 `/tmp/mcai-hopper-menu-gate-20260811-a`、
  `/tmp/mcai-dispenser-menu-gate-20260811-a` 和 Forge 65.0.0 下限的
  `/tmp/mcai-hopper-menu-gate-20260811-floor`、`/tmp/mcai-dispenser-menu-gate-20260811-floor`
  各自通过 1/1，日志均为 `All 1 required tests passed`。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E
  39/39，JSON 校验通过；产品 JAR SHA-256 为
  `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。精确产品 JAR 专服
  smoke 目录 `e2e/results/no-commit-dirty/20260810T172049Z-e2efe261a0229e6/` 的
  `server-smoke-verdict.json` 为 `PASS`，退出码 0、精确 JAR、SQLite Jar-in-Jar、
  ServerPlayer 生命周期通过，`functionalAiClaim=false`。
- 当前下一步：继续补尚未有独立双版本证据的红石/运输原子能力，并在每条能力后重复
  GameTest、打包和精确 JAR smoke；真实模型/客户端与 M0--M4 正式门禁仍保持 `NOT_RUN`。

## 当前继续记录（2026-08-11，木桶/潜影盒/末影箱公平仓储事务回归）

- 在石切机之后继续收口容器事务面：新增 `real_barrel_transaction` 与
  `real_shulker_box_transaction`。两者都由真实方块实体提供容器，先观察玩家库存和容器槽位，
  再用普通菜单转移、取出和快速移动完成木桶/潜影盒存取；测试夹具只初始化物资，生产执行器
  没有直接写容器 NBT。
- 新增 `real_ender_chest_transaction`。第一次 Forge 65.1.0 运行诚实暴露入口错误：末影箱
  不通过 `getMenuProvider` 直接打开，而是走原版 `BlockState.useWithoutItem`，由玩家设置活动
  末影箱库存后创建菜单。修正为带合法命中面的真实右键入口后，Forge 65.1.0
  `/tmp/mcai-ender-chest-menu-gate-20260811-a2` 与 Forge 65.0.0 下限
  `/tmp/mcai-ender-chest-menu-gate-20260811-floor` 各通过 1/1，日志均为
  `All 1 required tests passed`。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E
  39/39，JSON 校验通过；产品 JAR SHA-256 为
  `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。精确产品 JAR 专服
  smoke 目录 `e2e/results/no-commit-dirty/20260810T171504Z-e2e22b70b16efa6/` 的
  `server-smoke-verdict.json` 为 `PASS`：退出码 0、精确 JAR、SQLite Jar-in-Jar、
  ServerPlayer 生命周期均通过，`functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端
  Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。
- 当前根因记录：末影箱首次失败是测试入口误用 `getMenuProvider`，不是生产菜单执行器或原版
  末影箱语义失败；现已改用 `useWithoutItem`。已改文件包括
  `src/main/java/dev/mcai/companion/skills/menu/MenuGameTests.java`、
  `src/main/java/dev/mcai/companion/skills/EmbodimentGameTests.java`、三个
  `src/main/resources/data/mcai_companion/test_instance/real_*_transaction.json`，以及本检查点、
  `docs/IMPLEMENTATION_STATUS.md`、`changelog.txt` 和 `docs/progress/GOAL_STATE.json`。
- 本批最后失败门禁为 Forge 65.1.0 末影箱首次运行（tick 0，菜单未打开）；修正后 65.1.0/65.0.0
  均通过。下一步继续从尚未有独立双版本证据的原版能力切片推进，并对每个切片重复同一套
  专服 GameTest、产品打包和精确 JAR smoke；不把无模型证据升级成陪玩或通关声明。

## 当前继续记录（2026-08-11，石切机公平菜单事务回归）

- 在制图台之后继续收口已有 `select_menu_option` 生产执行面：新增 `real_stonecutter_transaction`。石料先从观察到的玩家库存槽经 `StonecutterMenu` 转移，随后只选择观察帧中的 `stonecutter_recipe` option；石切机原版生成结果，最后从结果槽通过普通 `quick_move` 取回。没有直接写配方、库存或 NBT。
- 第一次 Forge 65.1.0 门禁诚实捕获了测试断言错误：石切机一次结果只消耗一个输入，不能把两块石头都断言为已消耗；修正为原版真实剩余数量后，Forge 65.1.0 全新 `/tmp/mcai-stonecutter-menu-gate-20260811-a2` 与 Forge 65.0.0 下限 `/tmp/mcai-stonecutter-menu-gate-20260811-floor` 各通过 1/1，日志均为 `All 1 required tests passed`。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON 校验通过；产品 JAR SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T170557Z-e2ed81e16c94b30/` 为 `PASS`，退出码 0、精确 JAR、SQLite Jar-in-Jar、ServerPlayer 生命周期均通过，`functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，制图台公平菜单事务回归）

- 在高炉与烟熏炉之后继续扩展原版工作站面：新增 `real_cartography_table_transaction`。服务器先生成真实填充地图和纸张，均通过观察到的玩家库存槽绑定，再经 `CartographyTableMenu` 放入地图/附加物；输出缩放地图由原版菜单生成，随后只从观察到的结果槽快速移动取回。没有直接写地图组件、库存、方块或 NBT。
- Forge 65.1.0 全新 `/tmp/mcai-cartography-menu-gate-20260811-a` 与 Forge 65.0.0 下限 `/tmp/mcai-cartography-menu-gate-20260811-floor` 各通过 1/1，日志均为 `All 1 required tests passed`。这是无模型真实 ServerPlayer/vanilla menu 证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON 校验通过；产品 JAR SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T170127Z-e2e9d68ee200776/` 为 `PASS`，退出码 0、精确 JAR、SQLite Jar-in-Jar、ServerPlayer 生命周期均通过，且 `functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，高炉与烟熏炉公平熔炼事务回归）

- 在酿造台之后继续扩展同一 `SmeltMenuBatchSkill` 事务：新增高炉与烟熏炉的独立真实 GameTest。两者都先从公平观察帧绑定玩家库存槽，再经原版菜单放入输入/燃料，等待原版加速熔炼或食物熏制完成，最后从观察到的纯输出槽快速移动取回。没有直接写炉子、库存或 NBT。
- Forge 65.1.0 全新 `/tmp/mcai-blast-smoker-gate-20260811-a` 中，高炉 `real_blast_furnace_batch` 与烟熏炉 `real_smoker_batch` 各通过 1/1；Forge 65.0.0 下限 `/tmp/mcai-blast-furnace-gate-20260811-floor` 与 `/tmp/mcai-smoker-gate-20260811-floor` 也各通过 1/1。日志均为 `All 1 required tests passed`。这是无模型真实 ServerPlayer/vanilla menu 证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 重新执行 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39；GameTest 资源不进入生产 JAR，产品 SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。随后精确 JAR 专服 smoke `e2e/results/no-commit-dirty/20260810T165525Z-e2ee1a0065091bd/` 为 `PASS`，`functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，酿造台公平菜单事务回归）

- 在铁砧之后继续补齐原版工作站面：新增 `real_brewing_stand_batch` 真实 GameTest。三瓶水瓶、下界疣和烈焰粉均先通过观察到的 `BrewingStandMenu` 槽位转移；随后等待原版 400 tick 酿造计时，刷新观察三个 Awkward 药水，再用普通观察槽位快速移动取回。药水组件、原料消耗和燃料消耗均由原版酿造台决定，没有直接写物品、容器或 NBT。
- 第一次 65.1.0 专服运行诚实发现两处门禁问题：缺少 Forge test-instance JSON 导致选择器“found no tests”；补资源后又发现酿造瓶槽既是输入又是输出，不能使用 `outputOnly=true`。修正资源注册与普通 `quick_move` 语义后，Forge 65.1.0 全新目录 `/tmp/mcai-brewing-menu-gate-20260811-a3` 通过 1/1，日志为 `All 1 required tests passed`，并出现原版 `Local Brewery`。
- Forge 65.0.0 下限全新目录 `/tmp/mcai-brewing-menu-gate-20260811-floor` 同一酿造门禁也通过 1/1。两次都是无模型真实 `ServerPlayer`/原版菜单和计时证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON/兼容校验通过。产品 JAR SHA-256 为 `3ab40062df265d69824edb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`；精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T164850Z-e2e3711f4096cb2/` 为 `PASS`，`functionalAiClaim=false`，oracle 的 `server_stopped_before_result` 仍表示 smoke 主动停服而非玩法通过。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，铁砧公平菜单事务回归）

- 在砂轮之后，继续把铁砧纳入同一真实菜单执行面：两个玩家自有的受损钻石剑先经观察帧绑定并转移到原版 `AnvilMenu` 输入槽；刷新后观察原版结果和 XP 成本，再只通过普通 `quick_move` 取回结果，并确认耐久按原版合并、输入按原版消耗。没有直接写库存、方块或 NBT。
- 全新 Forge 65.1.0 工作目录 `/tmp/mcai-anvil-menu-gate-20260811-a` 的 `headless_player_lifecycle_state_and_fair_action` 通过 1/1；Forge 65.0.0 下限 `/tmp/mcai-anvil-menu-gate-20260811-floor` 同一选择器门禁也通过 1/1，均以 `All 1 required tests passed` 收口。两者均为无模型真实 ServerPlayer/vanilla menu 事务证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON/Forge 兼容校验通过。产品 JAR SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`；精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T164010Z-e2e27edac751582/` 为 `PASS`，`functionalAiClaim=false`，oracle 在服务端主动关闭前无玩法结果仍是预期 smoke 边界。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，砂轮公平菜单事务回归）

- 在锻造台之后，继续把砂轮纳入同一真实菜单执行面：两个玩家自有的受损钻石剑先经观察帧绑定并转移到原版 `GrindstoneMenu` 两个输入槽；刷新观察原版修复结果槽后，只通过普通 `quick_move` 取回结果，并确认两个输入被原版消费。没有直接写库存、方块或 NBT。
- 全新 Forge 65.1.0 工作目录 `/tmp/mcai-grindstone-menu-gate-20260811-a` 的 `headless_player_lifecycle_state_and_fair_action` 通过 1/1；Forge 65.0.0 下限 `/tmp/mcai-grindstone-menu-gate-20260811-floor` 同一选择器门禁也通过 1/1，均以 `All 1 required tests passed` 收口。两者均为无模型真实 ServerPlayer/vanilla menu 事务证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON/Forge 兼容校验通过。产品 JAR SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`；精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T163605Z-e2e7a8b9c862456/` 为 `PASS`，`functionalAiClaim=false`，oracle 在服务端主动关闭前无玩法结果仍是预期 smoke 边界。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，锻造台公平菜单事务回归）

- 在附魔台和织布机之后，继续把锻造台纳入同一真实菜单执行面：先观察三个输入槽，再把自有下界合金升级模板、钻石剑和下界合金锭经普通菜单转移放入 `SmithingMenu`；刷新观察原版结果槽后，仅通过 `quick_move` 取出下界合金剑，并确认模板、基底和添加物按原版消耗。没有直接写装备、容器或 NBT。
- Forge 65.1.0 全新工作目录 `/tmp/mcai-smithing-menu-gate-20260811-a` 的 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，日志最终为 `All 1 required tests passed`；Forge 65.0.0 下限使用选择器在全新目录 `/tmp/mcai-smithing-menu-gate-20260811-floor-focused` 通过 1/1，同样收口为 `All 1 required tests passed`。两者都是无模型真实 ServerPlayer/vanilla menu 事务证据，不是实时模型、PVP、随机 Hardcore 或 M3 完成声明。
- 曾误运行一次未带选择器的 74 夹具全套；其中既有 stronghold 夹具失败，退出码 10。该运行没有用于锻造台判定，随后用项目的 `-Plive_model_selector=mcai_companion:headless_player_lifecycle_state_and_fair_action` 重新执行了唯一目标门禁并通过。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON/Forge 兼容校验通过。产品 JAR SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`；精确 JAR 专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T163103Z-e2eb028d8f611f6/` 为 `PASS`，`functionalAiClaim=false`，oracle 在服务端主动关闭前无玩法结果是预期 smoke 边界。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，织布机公平菜单事务回归）

- 在附魔台链之后，继续把织布机纳入同一真实菜单执行面：自有白色旗帜和蓝色染料先经观察帧绑定后转移到原版 `LoomMenu`，重新观察可用图案，只选择已观察的 `loom_pattern` 按钮，再从原版输出槽以普通 `take_menu_output` 快速移动取回成品。没有直接改旗帜组件、方块或容器 NBT。
- 全新 Forge 65.1.0 工作目录 `/tmp/mcai-loom-menu-gate-20260811-a` 的 `headless_player_lifecycle_state_and_fair_action` 通过 1/1；Forge 65.0.0 下限 `/tmp/mcai-loom-menu-gate-20260811-floor` 也通过 1/1，二者均以 `All 1 required tests passed` 收口。该证据是无模型 ServerPlayer/vanilla loom 事务回归，不是实时模型、PVP 或 M3 完成声明。
- 随后 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，JSON/Forge 兼容校验通过。精确产品 JAR SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`；专服 smoke 目录 `e2e/results/no-commit-dirty/20260810T162122Z-e2ee6f18fd49fa4/` 的 verdict 为 `PASS`，`functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，附魔台公平菜单事务回归）

- 为继续收口 M3 原版工作站链，补充了 `MenuGameTests.vanillaMenuTransactions` 的真实附魔台事务：模型可见的菜单帧先观察空输入，再通过普通菜单转移把自有钻石剑和青金石放入输入槽；刷新状态后只选择已观察且可负担的原版按钮，验证附魔直接写回输入槽，再用观察槽位 `QUICK_MOVE` 取回背包。没有直接写方块、物品或 NBT。
- 这次专服门禁先后诚实捕获并修正三类测试/执行语义错误：附魔台不会自动吸入背包输入；附魔台没有熔炉式输出槽而是原位附魔；原位附魔槽可放置物品，不能走 `outputOnly` 快速移动。修正后全新 Forge 65.1.0 复合 `headless_player_lifecycle_state_and_fair_action` 通过 1/1，日志包含原版 `Enchanter`，并以 `All 1 required tests passed` 收口。
- 同一源码在 Forge 65.0.0 最低线全新目录 `/tmp/mcai-enchant-menu-gate-20260811-floor` 也通过 1/1。两次都是无模型 ServerPlayer 菜单/物理回归，不是实时模型、PVP 或 M3 完成声明。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 39/39，兼容性校验通过。测试类按发布规则不进入生产 JAR，当前产品 SHA-256 仍为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。
- 同一精确 JAR 的 Forge 65.1.0 专服 smoke 目录为 `e2e/results/no-commit-dirty/20260810T161619Z-e2e2fa869e7371c/`，`server-smoke-verdict.json` 为 `PASS`：退出码 0、精确 JAR、SQLite Jar-in-Jar、ServerPlayer 生命周期通过；`functionalAiClaim=false`。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。

## 当前继续记录（2026-08-11，跟随再捕获等待窗口与提示启动回归）

- 现场反馈中“跟随目标离开视野后原地转很久”的停顿根因已进一步收窄：已绑定跟随再捕获仍按普通地形调查的每视角新鲜观察等待窗口执行。生产路径现在只为这条已绑定跟随路径使用 4 个水平扇区和 `observationWaitTicks=12`；普通模型选择的地形调查仍默认 40 tick，未扩大视野、读取隐藏坐标或引入传送。
- 为了让可选参数仍兼容旧模型决策，`survey_surroundings` 保留三参数形式（默认 40），并接受严格规范的第四参数 `observationWaitTicks`（4..40，拒绝前导零/符号）。定向 `SurveySkillsTest`、`BrainOrchestratorTest` 通过。
- 第一次真实 Forge 复跑没有被掩盖：服务端在行为测试前因新增提示文字超过 13,000 字符硬上限（13,176）启动失败；压缩后再次发现仍为 13,033。最终继续压缩提示并在全新目录 `/tmp/mcai-follow-reacquire-wait12-retry2-20260811` 重跑，Forge 65.1.0 `real_player_chat_to_immediate_bound_follow` 真实 ServerPlayer/聊天夹具通过 1/1，日志为 `All 1 required tests passed`。日志中的关闭连接告警来自夹具主动结束，不是技能失败。
- 最终源码的 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks；Python E2E 39/39；`validate-compat.py` 报 Forge 65 已发布 11 个版本、形式矩阵仍未完成。当前产品 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为 `3ab40062df265d69824edeb930c32e34b26a8f3f2ffe1b5cae1fc7aca91bcf82`。
- 该精确 JAR 的 Forge 65.1.0 专服 smoke 目录为 `e2e/results/no-commit-dirty/20260810T155824Z-e2eafa39b81c9dd/`，`server-smoke-verdict.json` 为 `PASS`：退出码 0、精确 JAR、SQLite Jar-in-Jar、ServerPlayer 生命周期通过；`functionalAiClaim=false`。附带 oracle 的 `server_stopped_before_result` 是 smoke 主动停服且模型配置为空的预期状态，不是 AI 玩法通过。
- 同一源码在 Forge 65.0.0 最低线全新目录 `/tmp/mcai-follow-reacquire-wait12-floor-20260811` 的同一 `real_player_chat_to_immediate_bound_follow` 也通过 1/1；这只是该跟随参数/物理切片的双版本回归，不代表 65.x 完整功能矩阵已经通过。
- 证据边界不变：源码 `DIRTY_NO_COMMIT`、产物 `NON_RELEASE`；真实供应商模型、真实客户端 Actor/Observer、随机 Hardcore 以及 M0--M4 正式门禁仍为 `NOT_RUN`。没有把这次无模型服务端链路写成“AI 陪玩”或“通关”结论。

## 当前继续记录（2026-08-11，跟随丢失后的低延迟再捕获）

- 现场反馈中的“跟我走后原地慢慢转”有一个可复现的局部原因：已明确绑定玩家、但目标暂时离开当前第一人称语义帧时，`BrainOrchestrator` 会启动 `survey_surroundings`；该专用再捕获路径此前固定做 8 个水平采样，每个采样都等待新鲜语义帧。
- 现在只把这条“已绑定跟随目标”的低风险再捕获改为 4 个水平扇区，并保留 `includeVertical=false`、第一人称语义采样、同维度和普通技能监督边界；模型自行请求的通用地形调查仍可使用 8+视角，未扩大视野或引入隐藏坐标。
- `BrainOrchestratorTest` 与 `PlayerTaskIntentTest` 定向通过；全新 Forge 65.1.0 `real_player_chat_to_immediate_bound_follow` 真实 ServerPlayer/真人聊天夹具通过 1/1（`/tmp/mcai-follow-reacquire-20260811`，GameTest 日志 `All 1 required tests passed`）。这仍是无模型服务端绑定跟随物理证据，不等于自然语言任意任务或 M0--M4 正式通过。
- 本切片随后完成 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`（16 tasks）、Python E2E 39/39、JSON/兼容校验；产品 JAR SHA-256 为 `f00441e3f82f44da8a74c67f9c0d22ec4cf3207470a7b6107547f0a4cd24c30e`。同一精确 JAR 的 Forge 65.1.0 专服 smoke 目录为 `e2e/results/no-commit-dirty/20260810T154508Z-e2e747d9d1f6a98/`，verdict `PASS`，`functionalAiClaim=false`；附带 oracle 的 `server_stopped_before_result` 仍是 smoke 主动停服的预期状态。正式模型/客户端 Actor/Observer、随机 Hardcore 和 M0--M4 仍 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，金苹果切片打包与精确 JAR smoke）

- `real_offline_critical_golden_apple` 在全新 Forge 65.1.0 GameTest 工作目录
  `/tmp/mcai-golden-offline-20260811` 通过 1/1；随后当前源码的
  `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过 16 tasks，Python E2E 通过 39/39，
  `GOAL_STATE.json` 可解析。
- 当前唯一产品 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `6ce64e88b35e47a9a39e428b9ead6a6643580190a96cfd0ef47bc58d7f2b39cc`。精确 Forge 65.1.0
  专服 smoke 结果目录为
  `e2e/results/no-commit-dirty/20260810T152951Z-e2e6135dcca3a8f/`，`server-smoke-verdict.json`
  为 `PASS`：退出码 0、精确 JAR、SQLite Jar-in-Jar、ServerPlayer 加入及优雅生命周期通过，
  `functionalAiClaim=false`。附带 oracle 的 `server_stopped_before_result` 是 smoke 主动停服的
  预期场景状态，不改变专服 smoke verdict。
- 正式模型/客户端 Actor/Observer、真实聊天到动作、随机 Hardcore 和 M0--M4 仍为 `NOT_RUN`；
  源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。
- 同一 `real_offline_critical_golden_apple` 夹具又在 Forge 65.0.0 最低线全新工作目录
  `/tmp/mcai-golden-offline-6500-20260811` 通过 1/1；这只是该动作切片的双端版本回归，
  不是 Forge 65 每个功能/聊天/客户端门禁已完成。

## 当前继续记录（2026-08-11，危急生命金苹果真实原版交互门禁）

- 为回应“收到金苹果却只说不吃”的具体失败模式，新增了
  `real_offline_critical_golden_apple` Forge GameTest。夹具仅在初始化时给真实
  `ServerPlayer` 一枚金苹果并将生命值设为 4；之后关闭模型/规划器，由生产
  `EmergencySurvivalController`、库存菜单适配器和普通持物使用动作完成装备与进食。
- 精确 Forge 65.1.0、全新工作目录 `/tmp/mcai-golden-offline-20260811` 已通过 1/1，日志明确为
  `All 1 required tests passed`。断言包含身体存活、库存金苹果减少和原版吸收效果；这证明的是
  本地 20 TPS 危急生存动作链，不是模型聊天理解、PVP 或 M1--M4 通关。
- 该门禁没有注入世界方块、健康、效果或“已吃完”状态；这些只由测试初始夹具设置，运行过程通过
  真实库存交易和原版持物使用观察。正式模型/客户端 Actor/Observer、随机 Hardcore 和 M0--M4
  仍为 `NOT_RUN`；源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-11，观察式甘蔗单株真实物理门禁收口）

- 本轮把观察式甘蔗单株技能接入了真实 Forge GameTest，而不是只用 JVM 假执行器。首次失败的根因已
  由日志和方块状态确认：测试夹具把重力方块沙放在空气上，沙块在观察后的下一 tick 正常下落，随后
  原版准星只能命中空气；这是夹具物理错误，不是生产技能绕过。已在夹具中补充真实泥土底座，并保留
  相邻水、第一人称 OUTLINE 命中、普通 `equipMainHand`/`useOnBlock` 和新观察确认。
- 清理调试输出后，精确 Forge 65.1.0 的 `mcai_companion:real_plant_observed_sugarcane` 在全新工作目录
  `/tmp/mcai-sugarcane-clean` 通过 1/1；服务端日志为 `All 1 required tests passed`。这证明沙块保持、
  真实准星命中 `sand` 顶面、消耗一根自有甘蔗并在 `support.above()` 看到甘蔗，仍是无模型受控原子门禁，
  不是完整甘蔗机、随机生存或 M3 通过。
- `PlantObservedSugarcaneSkillTest` 同步覆盖了“先发出转向、下一观察帧再装备/使用”的真人时序；定向
  JDK25 测试通过。仓库已移除 `SUGAR_DEBUG_*` 临时输出，产品 JAR 也不含这些字符串。
- JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar --offline --no-daemon` 通过（16 tasks），Python
  E2E 39/39、`GOAL_STATE.json`、Forge 兼容性校验通过。当前唯一产品 JAR
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `6ce64e88b35e47a9a39e428b9ead6a6643580190a96cfd0ef47bc58d7f2b39cc`。
- 随后精确 Forge 65.1.0 专服 smoke 结果为 `e2e/results/no-commit-dirty/20260810T151942Z-e2e2ec651a76d7f/`，
  `server-smoke-verdict.json` 为 `PASS`，服务器退出码 0、精确 JAR、SQLite Jar-in-Jar、headless
  ServerPlayer 生命周期全部通过；`functionalAiClaim=false`。附带 oracle 的“server stopped before
  result”是 smoke 主动停服的场景状态，不是专服生命周期失败。
- 证据边界不变：源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`；真实模型/客户端 Actor/Observer、
  随机 Hardcore 和 M0--M4 正式门禁仍为 `NOT_RUN`。本轮只收口一个可复现的原版物理/技能切片。

## 当前继续记录（2026-08-10，观察式甘蔗单株技能）

- 新增 `PlantObservedSugarcaneSkill` 与 `PlantObservedSugarcaneParameters`，并注册为
  `plant_observed_sugarcane`。它只接受同一第一人称语义采样中的沙/红沙/泥土/草方块顶面、相邻
  可见水和自有甘蔗；通过正常 `equipMainHand`/`useOnBlock` 一次性种植，再以新观察或库存减少
  确认。没有坐标外推、世界扫描、直接改方块或固定建筑蓝图；这是一株甘蔗原子技能，不是完整
  甘蔗机或 M3 农场通过。
- `FarmingSkillParametersTest`、`PlantObservedSugarcaneSkillTest`、注册测试以及提示长度边界
  定向通过。曾短暂超出 13,000 字符 planner guide 硬上限，已压缩说明并保留约束；没有提高预算
  来掩盖问题。
- 固定 Temurin JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar --offline --no-daemon`
  通过（16 tasks）；Python E2E 39/39、GOAL_STATE JSON 与 Forge 兼容校验通过。当前唯一产品
  JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `d6d7ae585022f63351aa65d860a0f3aeb08fde24295472e293891d084b3fd1d9`。
- 精确 Forge 65.1.0 专服 smoke 结果为 `e2e/results/no-commit-dirty/20260810T144225Z-sugarcane-skill-contract/`，
  verdict `PASS`，服务器退出码 0，精确 JAR、SQLite Jar-in-Jar、headless ServerPlayer 加入和
  优雅生命周期均通过；`functionalAiClaim=false`，没有客户端或模型请求。
- 证据边界不变：源码仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`；真实模型/客户端 Actor/Observer、
  随机 Hardcore 和 M0--M4 正式门禁仍为 `NOT_RUN`。本轮只收口一个可审计的生产技能切片。

## 当前继续记录（2026-08-10，危险状态下规划器禁止口头拖延）

- 为直接收口“光说不做”路径，`MinecraftPlannerInputFactory` 的可信提示现在明确规定：
  `currentSafetyDeficits` 报告受击、着火、坠落、溺水、危急生命或其他即时生存缺口时，
  若当前白名单有适用技能必须立即返回完整参数的 `START_SKILL`，不得用带语音的
  `CONTINUE/REPLAN` 假装正在格挡、后退、吃东西、战斗或逃跑；若没有适用技能只能裸
  `REPLAN`/按评测规则 `SAFE_IDLE`。提示同时区分 20 TPS 应急反射已经持有输入与规划器
  没有启动技能，避免把本地反射误报成模型动作。
- 新增 `MinecraftPlannerInputFactoryTest` 契约断言，连同 `BrainOrchestratorTest` 定向运行
  通过。该改动只约束模型输出，不绕过技能白名单、第一人称证据或原版执行器。
- 同一源码重新执行 JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar`：16 tasks
  `BUILD SUCCESSFUL`；Python E2E 39/39、GOAL_STATE JSON 和 Forge 兼容校验通过。当前产品
  JAR SHA-256 为 `f5b05e1eb1a446fc92632e276c2f21e7528cc4d9b490e3d4d4ef1557afa50cf1`，
  client/oracle 测试 JAR 哈希仍为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` /
  `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。
- 当前精确产品 JAR 的 Forge 65.1.0 专服 smoke 也为 `PASS`，结果目录为
  `e2e/results/no-commit-dirty/20260810T142046Z-safety-prompt-contract/`；服务器退出码 0，
  精确 SHA、SQLite Jar-in-Jar、headless ServerPlayer 加入和优雅生命周期均通过，
  `functionalAiClaim=false`（没有客户端或模型请求）。
- 这仍是提示/协议/无模型内循环证据；真实模型、Linux/Xvfb Actor/Observer、随机 Hardcore
  和 M0--M4 正式门禁继续保持 `NOT_RUN`，工作树仍 `DIRTY_NO_COMMIT`、产物仍 `NON_RELEASE`。

## 当前继续记录（2026-08-10，连续受击空中脱离回归已收口）

- 最新完整生命周期回归 `run-debug308` 重新验证了此前 `run-debug291` 的真实失败：末地水晶造成无方向连续伤害时，控制器不再因短暂 `onGround=false` 而原地扫描，已在有限受击窗口发出普通水平脱离。Forge 65.1.0 `headless_player_lifecycle_state_and_fair_action` 现 1/1 通过；运行时样本 5967、平均 269540 ns、滚动 p95 1272625 ns，均低于 1 ms/2 ms 目标。日志为 `/tmp/mcai-lifecycle-after-air-knockback-fix.log`。
- 根因修复位于 `EmergencySurvivalController.java`，并新增 `EmergencySurvivalControllerTest.directionlessDamageStillSeparatesDuringShortKnockbackAirTime`；定向 JUnit 通过。未知投射物仍不会触发这条移动分支，水中、着火和真正下落仍由更高优先级应急车道处理。
- 当前修订又在全新 Forge 65.1.0 工作目录重跑 `real_emergency_zombie_skeleton_horde` 与 `real_emergency_iron_golem_duel`，各 1/1 通过（GameTest 分别 1.185 秒、1.089 秒）；这保持了十僵尸/十小白压力场和铁傀儡近战的原版应急证据。日志为 `/tmp/mcai-horde-after-air-knockback-fix.log` 与 `/tmp/mcai-golem-after-air-knockback-fix.log`，仍不等于模型 PVP 或正式 Hardcore。
- 刚用新产品 JAR `63f1b80e…f3dc5d00` 运行一次 Forge 65.1.0 精确专服 smoke：verdict `PASS`，服务器退出码 0，JAR/SQLite Jar-in-Jar/ServerPlayer/生命周期校验全部通过，结果目录为 `e2e/results/no-commit-dirty/20260810T141222Z-air-knockback-fix/`。该命令没有启动客户端，`functionalAiClaim=false`；随附 oracle 的“server stopped before result”是因为 smoke 场景主动停服，不是专服失败。

## 当前继续记录（2026-08-10，农田水边逃逸与移动加速度回归已收口）

- 本轮最后两个真实 Forge 65.1.0 失败已闭合：水边一次跳跃的候选坐标原先漏检脚下 farmland，导致小麦支撑被踩成 dirt；农田移动每帧先调用交互式 `stop()`，清空原版加速度，导致潜行拾取只转向不前进。
- 已改 `HarvestAndReplantStepSkill.java`：水逃落点同时核验当前/上方坐标的 farmland 与作物块，并且每个新鲜非农田落点最多跳一次；新增不清空连续输入的移动准星，精确交互仍使用 stop。此前的中心准星门禁、事务掉落关联、有界路线继续保留。
- fresh-world Forge 65.1.0 定向结果：`real_maintain_observed_crop_field`（wheat）、carrot、potato、beetroot、expanded wheat 各 1/1 通过；本轮 source compileJava/compileTestJava 通过。随后完整 JDK25 package 16 tasks 与 Python E2E 39/39 通过；当前产品 JAR SHA-256 为 `63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`，client/oracle 测试 JAR 哈希保持 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` / `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。
- 同一最终源码再跑了与用户反馈直接相关的 fresh Forge 65.1.0 物理回归：`real_parkour_course`、`real_emergency_iron_golem_duel`、`real_emergency_zombie_skeleton_horde` 各 1/1 通过；这验证受控 headless 身体的原版移动/应急车道，不是模型控制、PVP 或正式 M1--M4。
- 继续跑了 fresh Forge 65.1.0 的维度/终局与在线生命周期回归：`real_end_portal_activation`、`real_end_victory_and_return`、`zero_human_dedicated_server_chunk_and_respawn`（`mcai.zeroHumanAutoSpawnTest=true`）和 `auto_presence_on_human_login` 各 1/1 通过。终局日志确认获得 `The End?`、`Free the End` 并完成返回；无人服务器日志确认生产 ServerStartedEvent 自动登录 MCAI，玩家进入日志确认 TestHuman 与 MCAI 同时在线。`zero_human` 的死亡包警告属于预期关闭 headless 通道，不改变 GameTest 通过结果。
- 再跑了 `real_furnace_batch` 与 `real_charcoal_furnace_batch` 的 fresh Forge 65.1.0 功能方块回归，各 1/1 通过；并用 JDK25 定向重跑 `ApiKeyManagerRestartPersistenceTest`、`ApiKeyManagerConcurrencyTest` 与 `ModelRuntimeTest`，全部通过，未把凭据写入日志或世界数据。
- 当前源码还重跑了 `real_player_chat_to_immediate_bound_follow`：fresh Forge 65.1.0 1/1，通过日志可见 TestHuman 的任务消息被安装，MCAI 以原版 ServerPlayer 跟随而非只发送“我来了”；这是服务端绑定跟随的无模型物理回归，不等于任意模型自然语言任务均能动作。
- 基础生存/技术路径 fresh Forge 65.1.0 也已补跑：`real_zero_human_dedicated_server_foundation`、`real_prepare_and_plant_plot`、`real_build_hydrated_crop_field`、`real_verified_portal_return` 各 1/1。最后一项保留了服务端的到达诊断，结论只取 GameTest 的实际通过，不把调试距离或无模型脚本外推成正式通关。
- 当前源码最终复核：JDK25 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 tasks 通过，Python E2E 39/39、GOAL_STATE JSON 和 Forge 兼容脚本均通过；唯一产品 JAR 仍为 `63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`，测试 JAR 哈希未变。兼容输出仍是 Forge 65.x 11 个 patch、`formalMatrixComplete=false`。
- 官方下载页在 2026-08-10 UTC 复核仍将 Minecraft 26.2 的 Latest/Recommended 列为 Forge 65.1.0，并列出 65.0.0--65.0.9；没有 Forge 66 发布条目。因此没有把 Forge 65 JAR 的声明范围扩大到 66，事实记录和 `compat/forge-lines.toml` 的检查时间已更新。
- 当前源码在 Forge 65.0.0 最低线 fresh GameTest `real_zero_human_dedicated_server_foundation` 也通过 1/1；日志确认 Forge 65.0.0 加载 71 个测试函数、SQLite Jar-in-Jar 运行时和基础无人服务器场景。该结果只是最低线启动/基础内循环，不是 65.x 全补丁正式兼容矩阵。
- `offline_idle_equipment` 在 fresh Forge 65.1.0 通过 1/1，日志出现原版 `Suit Up` 成就；这是给定装备后的真实 ServerPlayer 穿戴/耐久路径，不是模型决定或人工客户端验收。
- `real_emergency_enderman_defense` 当前源码 fresh Forge 65.1.0 通过 1/1，日志出现 `Cover Me with Diamonds`；这补齐了末影人近战/防御的无模型应急回归，仍不等于模型 PVP 或随机 Hardcore。
- 本次继续在全新 Forge 65.1.0 工作目录运行 `real_nether_blaze_rod_acquisition`，1/1 通过；日志确认 MCAI 以原版近战击杀烈焰人、拾取烈焰棒并获得 `Into Fire`。这是下界战斗/掉落拾取的受控无模型组件证据，不是自然要塞发现、模型控制或 M2 通关证据。命令末尾仅有 zsh 保留变量名造成的壳层退出，Forge/Gradle 日志本身为 `BUILD SUCCESSFUL`，不作为游戏测试失败。
- 随后在全新 Forge 65.1.0 工作目录运行 `real_stronghold_reach`，1/1 通过（GameTest 14.16 秒，Gradle `BUILD SUCCESSFUL`）；日志确认 MCAI 从已观察入口外沿普通行走、下降挖掘并消耗铁镐/火把后完成测试。这是已验证目标的接近/挖掘内循环，不是自然随机种子定位、模型控制或正式 M2 证据。
- 再在全新 Forge 65.1.0 工作目录运行 `real_ender_pearl_reserve`，1/1 通过（GameTest 7.798 秒）；日志显示真实 ServerPlayer 完成受控战斗/拾取并达到 13 颗末影珍珠储备。这是预置资源条件下的末影珍珠收集组件，不是自然探索、模型控制或随机 Hardcore 证据。
- 再在全新 Forge 65.1.0 工作目录运行 `real_nether_blaze_material_reserve`，1/1 通过（GameTest 5.145 秒）；日志确认真实 ServerPlayer 完成可见烈焰人近战、原版掉落拾取并达到 7 根烈焰棒储备，技能状态为 `COMPLETED`。这是受控下界材料储备组件，不是模型控制、自然要塞路线或 M2 通关证据；日志保存在 `/tmp/mcai-next-blaze-material-65.1.0.log`。
- 当前源码又逐个在全新工作目录运行 Forge 65.x 全部 11 个已发布 patch 的 `real_zero_human_dedicated_server_foundation`：65.0.0、65.0.1、65.0.2、65.0.3、65.0.4、65.0.5、65.0.6、65.0.7、65.0.8、65.0.9、65.1.0 均 `rc=0` 且 1/1 通过。对应日志保存在 `/tmp/mcai-forge65-<version>-foundation.log`。这是兼容启动/加载/无人 ServerPlayer 基础 smoke，不是每个 patch 的聊天、移动、菜单、保存、重启完整矩阵。
- 当前正确 CLI 的正式客户端 preflight 也已重跑（`python3 e2e/orchestrator.py preflight --forge-version 65.1.0`）：`ready=false`，明确缺少 Linux host、Xvfb、`MCAI_BASE_URL`、`MCAI_MODEL` 和 `MCAI_API_KEY(_FILE)`；物理显示器保持未触碰，未启动客户端、未读取或发送密钥。输出保存在 `/tmp/mcai-preflight-current.log`，因此真实 Actor/Observer/模型门禁继续是 `NOT_RUN`，不是游戏失败。
- 随后在同一 11 个 Forge 65 patch 上逐个运行 `zero_human_dedicated_server_chunk_and_respawn` 与 `auto_presence_on_human_login`，共 22/22 通过（每个 `rc=0`、1/1）；对应日志为 `/tmp/mcai-forge65-<version>-zero_human_dedicated_server_chunk_and_respawn.log` 与 `/tmp/mcai-forge65-<version>-auto_presence_on_human_login.log`。这补齐了 patch 级无人自动出生/重生与真人登录并存的生命周期内循环，不等于每 patch 的聊天、移动、菜单、保存/重启完整门禁。
- 在上述矩阵后再次执行固定 Temurin JDK25 的 `check verifyReleaseJar e2eClientJar e2eOracleJar`：16 tasks 通过；Python E2E 39/39、GOAL_STATE JSON 与 compat 校验也通过。修复空中脱离后产品 JAR SHA-256 更新为 `63f1b80e9e6712e1d3c1f807017f86d927d03dca9c483501914aee03f3dc5d00`，client/oracle 仍为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` / `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。
- 证据边界不变：以上均为无模型、受控 headless ServerPlayer 内循环，不是 AI 聊天到动作、真人客户端、随机 Hardcore 或 M1--M4 通过。正式模型/Actor/Observer 门禁仍因上游 401 与本机 Darwin 无 Linux/Xvfb 为 `NOT_RUN`，工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

## 当前继续记录（2026-08-10，农田拾取定向修复进行中）

- 最近一次真实 Forge 65.1.0 `real_maintain_observed_crop_field` 仍失败在
  `timed_out_collecting_harvest`：第一人称感知已经看见与本次 wheat 事务关联的掉落物，身体在水田边
  通过受限普通步进向其靠近，但 180 tick 拾取窗口结束时仍相距约 2.8 格。此前还复现过水边逃逸
  盲跳导致农田支撑变 dirt，以及同类根作物把旧种子掉落误关联为本次收获；这些都已分别加上
  无跳跃/低空气反射保护、事务内新鲜掉落关联和短期第一人称掉落记忆。
- 本轮已改 `HarvestAndReplantStepSkill.java`（水边不盲跳、农田着陆自适应潜行、普通步进速度与
  停滞阈值、可见掉落物关联/记忆、拾取窗口延长）和
  `EmergencySurvivalController.java`（看见农田时不触发低空气反射跳跃）。相关 farming JVM
  测试可编译通过，但最新 wheat Forge GameTest 尚未重跑收口，不能把 farming 矩阵写成通过。
- 最后失败门禁是上述 fresh wheat 1/1；此前 carrot/beetroot/potato 有通过也有水边/拾取间歇失败。
  这仍是无模型、受控 headless 内循环，不是 AI 陪玩、真人客户端、随机 Hardcore 或 M1--M4 证据。
- 下一步立即在当前源码上重跑 fresh wheat，再按结果收窄拾取步进/事务超时；若 wheat 收口，重跑
  carrot、beetroot、potato、wheat 重复矩阵，随后更新实现状态、GOAL_STATE、changelog，最后重跑
  JDK25 package 与 Python E2E。正式模型/Actor/Observer 仍受 401 授权边界和本机 Darwin 无
  Linux/Xvfb 阻断，所有 M0--M4 继续保持 NOT_RUN。

## 当前继续记录（2026-08-10）

- 真实 Forge 65.1.0 `real_maintain_observed_crop_field` 新鲜临时世界首次复现了
  `harvest_and_replant_step.harvest_drop_not_collected`：日志中的真实麦子掉落物仍在约
  1.8 格外，身体只在作物中心附近转向，未完成普通拾取。这不是模型或脚本通过，已按失败保留。
- 生产修复已落在 `HarvestAndReplantStepSkill`：当普通第一人称感知实际看到匹配的收获掉落物时，
  在已重植地块、在地面、非水的有界走廊内直接以掉落物当前位置作为短距离移动目标；不扫描实体、
  不直接改背包或世界。`HydratedCropFieldPlanService.planMaintenance` 同时移除永久
  `UnsupportedOperationException` 默认生产桩，并让维护规划服务契约显式实现。
- 修复后的定向农田 JVM 测试通过；同一 `real_maintain_observed_crop_field` 在全新 Forge
  65.1.0 世界通过 1/1，服务端日志为 “All 1 required tests passed”。这是无模型受控内循环证据，
  不提升 M1--M4、真实聊天到动作、Actor/Observer 或随机 Hardcore 结论。
- 随后完整 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar --offline --no-daemon`
  通过（16 tasks；Forge 65.x published=11，formalMatrixComplete=false），Python E2E 通过 39/39。
  新产品 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
  `9114adea1edb3a4dfef943fa57b172640ac710dc9062d62d54fb1975f3f92bcd`；客户端/Oracle 测试 JAR
  分别为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` 与
  `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`。这仍是源码/封装/无模型
  内循环证据，formal AI gates unchanged。
- 当前需要继续：跑完整 JDK25 Gradle/package/Python 门禁，更新精确产品 JAR 哈希；真实模型仍受
  之前的 401/授权边界与本机 Darwin 无 Linux/Xvfb 阻断，正式模型/客户端和 M0--M4 仍保持未运行。

## 恢复审计摘要（2026-08-10）

- 当前用户反馈的“光说不做/没有身体”不能归结为一个单一 bug：没有通过能力验证的模型时，生产
  高层移动/采集/战斗技能会故意 fail-closed；身体和 20 TPS 紧急生存车道仍在线。此前唯一真实
  MiMo 请求使用 `api-key` 头仍返回 HTTP 401，因此真实模型动作门禁没有启动；真实 Actor/Observer
  门禁在本机 Darwin 还缺 Linux/Xvfb。这两项是当前外部阻断，不是通过脚本可伪造的成功。
- 本轮先补了 Tab 列表的真实身体在线标记，随后修复模型断线时 active skill 永久 RUNNING 的
  生产缺口：已授权技能现在只收尾当前安全原子段，紧急车道会先释放旧输入再安全摘除技能。
  没有借此把产品源码或 JAR 改成未验证的“AI 已完成”状态。最新开发 JAR SHA-256 为
  `731b7a4d572ffdb8494c98026c1da0fd008c156a78e54ceeba388185278eae21`。
- 最后失败/未运行的正式门禁仍是：真实模型 movement/chat-to-action（上游 401）、真实客户端
  Actor/Observer（Darwin 无 Xvfb）、以及所有 M0--M4/随机 Hardcore 统计；这些全部保持 `NOT_RUN`
  或 `BLOCKED`，工作树仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。
- 下一步必须在取得有效且获准用于该自定义应用的模型凭据、可运行真实客户端的 Linux/Xvfb worker
  后，先跑真实 dedicated + Actor/Observer 聊天到动作垂直切片；通过前继续补生产技能与定向
  Forge 门禁，但不扩大声明范围。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: worker job creator (open)

继续补齐跨平台真实验收的协调端：新增 `scripts/create-worker-job.py`。它从指定 Git
checkout 读取完整 40 位提交、从精确产品 JAR 计算 SHA-256、从 HTTPS Base URL 只提取主机名，
并从仅含 64-hex 承诺的文件生成 canonical job manifest；默认发现脏工作树即拒绝，开发诊断的
`--allow-dirty` 仍在 manifest 中保留 `source.dirty=true`。脚本从不读取或序列化 API key、原始
seed、世界路径或玩家身份；模型凭据只通过显式公开的 `credential-present/source` 元数据声明，
实际值仍由 Linux worker 注入。worker 启动前和子运行结束后还会比较实际模型名、Base URL 主机、
凭据存在性及声明来源；`injected` 只允许 worker 的环境变量或安全文件呈现，不比较或记录密钥内容。
`e2e/test_worker_protocol.py` 新增干净提交、脏工作树、原始 seed
拒绝和 URL 路径不泄露测试；完整 Python E2E 通过 39/39，随后 Gradle `check
verifyReleaseJar e2eClientJar e2eOracleJar --offline` 通过（JDK 25，Forge 65 兼容检查
published=11，formalMatrixComplete=false）。生产 JAR 哈希仍为
`731b7a4d572ffdb8494c98026c1da0fd008c156a78e54ceeba388185278eae21`；正式模型/客户端门禁仍未运行。

下一步：在具备 Linux/Xvfb、精确 Forge 65.x JAR 和获授权模型凭据的隔离 worker 上使用 creator
生成 job，再运行真实 dedicated + Actor/Observer；当前 macOS 仍只允许 `BLOCKED_INFRA`，M0--M4
及隐藏 Hardcore 统计保持 `NOT_RUN`，源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: provider-neutral Linux worker contract (open)

为正式 Actor/Observer 和隐藏种子分片补上了一个不绑定云厂商的 worker 契约：
`e2e/worker_protocol.py`、`scripts/run-e2e-worker.py` 与
`scripts/verify-worker-result.py`。作业只携带 64 位种子承诺，不接受原始 seed、API key、
世界路径或玩家身份；作业哈希绑定源码提交、精确产品 JAR SHA-256、Minecraft/Forge/Java、
模型主机和分片。worker 只把 `MCAI_API_KEY(_FILE)` 作为进程注入，不把凭据写入结果，且发布前
扫描工件中的密钥。结果必须逐文件 SHA-256、canonical result hash 和 job manifest hash 均通过，
才能交给中央聚合器；缺少 Linux/Xvfb 或凭据分别记录为 `BLOCKED_INFRA`/`BLOCKED_CREDENTIAL`，
不会被提升为游戏通过。

定向验证：worker 协议单测与完整 Python E2E 共 34/34 通过；在当前 Darwin 主机用临时公共 job
运行 worker，正确返回 `BLOCKED_INFRA`、`functionalAiClaim=false`，随后中央 verifier 仍能验证
结果包。该运行没有启动 Minecraft 客户端，也没有调用模型，因此正式真实模型、Actor/Observer、
Hardcore 随机种子和 M0--M4 状态不变，继续为 `NOT_RUN`/外部阻断；工作树仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: offline active-skill teardown (open)

本轮修复了一个会直接造成“模型说要做、随后身体停住”的运行时缺口：模型连接丢失时，运行时
原先完全跳过已经授权的 active skill tick；`SkillSupervisor` 因而一直保持 `RUNNING`，而
finally 代码又释放了所有身体输入。现在 `CompanionRuntime` 在模型离线时仍允许已存在的 skill
走一次正常 supervisor 收尾路径；若紧急生存车道先取得所有权，则调用
`SkillSupervisor.abandonForModelDisconnect()` 记录 `model_disconnected` 的安全终态。该路径不
启动新技能、不发起规划请求、不传送、不改世界，也不会把“模型在线”伪装成在线。

验证结果：`SkillSupervisorTest`、离线 active-skill source contract 通过；完整 JVM `check`
通过（当前报告 1076 tests、0 failures、0 errors、2 skips），`verifyReleaseJar`、客户端/Oracle
测试 JAR、Forge 兼容检查和 Python E2E 30/30 均通过。最新精确产品 JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`731b7a4d572ffdb8494c98026c1da0fd008c156a78e54ceeba388185278eae21`；释放旧输入后的 Forge
65.1.0 exact-JAR 专服 lifecycle smoke `e2e/results/no-commit-dirty/20260810T104022Z-e2e071cf86ffb19/` 为
`PASS`，但 `functionalAiClaim=false`，仍不是模型或陪玩验收。

正式模型/Actor+Observer/随机 Hardcore/M0--M4 继续 `NOT_RUN` 或受外部凭据/客户端环境阻断；
工作树仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步仍需在 Linux/Xvfb 注入经授权的模型环境，
先完成真实聊天到动作垂直切片，再继续统计门禁。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: formal chat preflight (not run)

正式 `e2eChat` 前置检查明确返回 `NOT_RUN`，不是动作失败：当前主机是 Darwin，未发现隔离
`Xvfb`，且本次测试进程没有 `MCAI_BASE_URL`、`MCAI_MODEL` 或 `MCAI_API_KEY(_FILE)`。因此
没有启动客户端、没有调用模型、没有写入任何凭据，也没有改变 M0--M4 门禁状态。结果文件位于
`e2e/results/no-commit-dirty/20260810T102441Z-formal20260810102441/`，其中
`functional-preflight.json` 的 `ready=false`，缺失项为 `linux_host`、`Xvfb` 和模型环境。

下一步必须在 Linux/Xvfb worker 上注入经授权用于该自定义应用的模型配置，再运行真实 dedicated
server、Actor、Observer 和聊天到动作；当前继续保持 `NOT_RUN`/`NON_RELEASE`，不把预检或无模型
GameTest 当作陪玩验收。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: exact JAR dedicated lifecycle smoke (open)

刚用最新产品 JAR 在 Forge 65.1.0 独立专服运行 `python3 e2e/orchestrator.py server-smoke`。
`server-smoke-verdict.json` 为 `PASS`：专服干净退出、Mod 加载、SQLite Jar-in-Jar、生产
headless `ServerPlayer` 登录与关闭生命周期均通过，加载的 JAR SHA-256 与
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` 的
`ceefa3e120c13d1bbc71cb1fa8787af1fb4a185c2426b907f6856062684065cb` 完全一致。Oracle 的任务
结果仍是 `functionalAiClaim=false`（没有模型凭据且没有启动聊天/移动/物品任务），所以这只是
精确工件的服务器生命周期证据，不是 AI 陪玩或 M1--M4 通过。

运行归档：`e2e/results/no-commit-dirty/20260810T102609Z-e2e42b26f3709cf/`。下一步不变：在
Linux/Xvfb 和有效模型环境中跑真实 Actor/Observer 聊天到动作。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: Tab presence status (open)

本轮修复了一个可见性缺口：Tab 列表此前只有 `[AI]` 前缀，不能区分在线身体和离线/正在
重建的会话。`src/main/java/dev/mcai/companion/embodiment/EmbodimentModule.java` 现在
只对真实服务端 `ServerPlayer` 读取 `AiPlayerManager.Status.online()`，显示 `[AI] Name  ●
online` 或 `[AI] Name  ○ offline`；它不把模型未验证误报成在线模型，也不读取隐藏世界数据。

定向 JVM 编译/测试通过，完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，Python
E2E 30/30 通过。新开发 JAR 为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256 为
`ceefa3e120c13d1bbc71cb1fa8787af1fb4a185c2426b907f6856062684065cb`；客户端实际视觉门禁仍
因 Darwin 无 Xvfb 为 `NOT_RUN`。真实模型、随机 Hardcore 与 M0--M4 仍未通过，工作树继续
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

下一步仍是有效模型凭据和 Linux/Xvfb worker 就绪后，先跑真实 dedicated + Actor/Observer
聊天到动作垂直切片，再继续正式统计；本次 Tab 状态改动不扩大产品声明。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.36 Forge 65.0.0 floor smoke (open)

同一当前源码在声明的最低支持版本 Forge 65.0.0 上运行独立专服
`zero_human_dedicated_server_chunk_and_respawn`，结果 1/1、1.539 秒；日志确认 Forge 65.0.0
加载 Mod、无真人时创建 headless `ServerPlayer`、远端玩家区块模拟和普通重生链均通过。Forge
版本检查提示 65.0.0 不是 65.1.0 最新补丁，这是预期提示而非失败。该结果只证明最低补丁的
启动/身体/区块内循环，不代表 65.x 全矩阵、真实模型、真人客户端或正式 M0--M4。

没有产品源码变更，产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`；工作树仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续生产技能缺口和外部模型/客户端门禁，不把此兼容
smoke 冒充 AI 陪玩验收。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.35 Python E2E and compatibility audit (open)

离线 Python E2E 结构/安全/兼容回归 `python3 -m unittest discover -s e2e -p 'test_*.py'` 已通过
30/30。它检查工件清单、测试代码隔离、密钥不泄露、Forge 65.x 声明和失败闭合协议，不启动
Minecraft 客户端、不调用模型，也不改变正式门禁状态。当前产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`；真实模型、真人客户端、随机 Hardcore 和 M0--M4 继续
`NOT_RUN`。下一步回到生产技能实现/验证，不会把离线审计当成 AI 陪玩结果。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.34 full JVM/package gates (open)

在上述定向回归后运行了 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar --offline`：
Forge 兼容检查通过（当前 65.1.0 运行线，正式矩阵仍未完成），完整 JVM `check`、发布 JAR 内容
门禁、客户端/Oracle 测试 JAR 构建均通过。测试中的受控异常日志和无效环境覆盖仍按设计被安全
处理；没有向任何模型提供商重新发送用户凭据。产品 JAR SHA-256 保持
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，GOAL_STATE JSON 也已验证。

这只是源码、封装和受控内循环证据，不能升格为真实模型/真人客户端/随机 Hardcore 或 M0--M4
通过；工作树仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续实现和验证生产技能，不会把脚本
模拟当作真实 AI 陪玩结果。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.33 headless lifecycle and menu transactions (open)

Forge 65.1.0 独立 GameTest `headless_player_lifecycle_state_and_fair_action` 已通过 1/1，
耗时 28.79 秒。它让真实 PlayerList-backed headless `ServerPlayer` 完成登录/移除/重登录和状态
保持，并经过原版箱子存取、熔炉装载/取出、切石机菜单选项、交易、船乘坐/航行、动力铁轨矿车
乘坐/航行以及其他生命周期/公平动作检查；
日志显示所有必需测试通过。该测试使用受控物品与结构、无模型和无自然随机世界，只证明身体、菜单
事务和会话生命周期链，不能升级为模型陪玩、PVP 或 M0--M4 通关证据。

没有新的产品源码变更，产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`。正式 M0--M4、真实模型、
真人客户端、聊天到动作和随机 Hardcore 通关继续为 `NOT_RUN`；工作树仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续选择生产技能缺口做定向门禁，并保留真实模型/客户端
前置条件的阻断记录。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.32 zero-human simulation window (open)

Forge 65.1.0 独立专服 GameTest `zero_human_dedicated_server_chunk_and_respawn` 已通过 1/1，
运行约 1.736 秒。测试确认没有真人玩家时，生产启动路径仍创建唯一在线的 headless
`ServerPlayer`；把它移动到远离测试结构的区块后，原版玩家 ticket 维持了远端区块和相邻模拟区块，
非玩家实体/方块 tick 正常，随后普通死亡/重生生命周期也完成。测试没有使用强制加载区块、模型、
命令传送或隐藏扫描，因此只证明“无人在线仍有身体和正常区块模拟”这一门槛，不证明模型会自主游玩。

当前仍没有产品源码变更，产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`。正式 M0--M4、真实模型、
真人客户端、聊天到动作和随机 Hardcore 通关继续为 `NOT_RUN`；工作树为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续做能直接暴露生产技能的交互/运输原子链，同时维持
真实模型与客户端门禁不冒充通过。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.31 water, charcoal, and portal atoms (open)

当前源码的 Forge 65.1.0 定向 GameTest 又通过三条真实原版交互链：
`real_prepare_water_source` 1/1（普通水桶取水/放置水源，631.2 ms）、
`real_charcoal_furnace_batch` 1/1（木材转木炭并完成熔炉批处理，651.2 ms）以及
`real_portal_cast_and_light` 1/1（岩浆/水浇筑并点燃下界门，1.246 秒）。这些测试都由真实
headless `ServerPlayer` 经过原版物品、方块和菜单路径完成，使用受控夹具且无模型；它们只证明
局部物理原子链，不代表自然随机世界、真实模型、Hardcore 或正式 M2/M4 通关。

本次没有新的产品源码变更，产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`。正式 M0--M4、真实模型、
真实客户端、聊天到动作和随机种子通关继续保持 `NOT_RUN`；工作树仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步先补齐本轮证据与配置门禁核对，再继续寻找能直接暴露
生产技能的原版容器/运输、附魔或村民原子链；不会用这些无模型结果冒充专业陪玩验收。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.28 end victory and return regression (open)

当前源码的 Forge 65.1.0 定向 GameTest `real_end_victory_and_return` 已完成 1/1：真实
headless `ServerPlayer` 进入末地、完成末影龙击杀、触发 `Free the End`，再从返回传送门回到
主世界并完成退出；日志中的最终状态为 `All 1 required tests passed :)`，运行时间 8.464 秒。
该测试使用无模型内循环/受控战斗和世界夹具，只证明维度、胜利状态和返回生命周期没有被近期
受击/导航改动破坏，绝不等价于随机种子、Hardcore、真实模型或真人客户端通关。

本检查点没有新的产品源码变更；当前产品 JAR SHA-256 仍为
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`。正式 M0--M4、真实模型、
真实客户端、聊天到动作、双客户端/PVP 和随机种子通关继续保持 `NOT_RUN`；工作树仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续补充可复现的 M2/M3 原子链并维持每项“无模型内循环”
边界，之后再处理合规模型和 Actor/Observer 外部门禁。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.29 M2/M3 workstation and farming atoms (open)

在没有新的产品源码变更的情况下，当前 Forge 65.1.0 独立 GameTest 又通过三条真实原版交互链：
`real_furnace_batch` 1/1（熔炉批处理/燃料/产物）、`workstation_wood_prerequisite_composition`
1/1（从木材前置到工作台和基础工具组合）以及 `real_prepare_and_plant_plot` 1/1（准备耕地、
水源和播种，触发 `A Seedy Place`）。这些测试运行在真实 headless `ServerPlayer` 和 vanilla
菜单/交互路径上，但使用受控夹具且无模型，不代表自然世界、长期农业或正式 M3/M1 门禁。

正式 M0--M4、真实模型、真人客户端、Hardcore 随机种子和双客户端/PVP 仍为 `NOT_RUN`；工作树
仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步继续选择独立的容器/运输或附魔/村民原子链，优先
查找能直接暴露生产技能而不是只测工具类的场景。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.30 local combat and food atoms (open)

当前源码在 Forge 65.1.0 又通过两条独立物理回归：`real_emergency_slime_defense` 1/1（真实
headless `ServerPlayer` 受击后的护盾/近战/装备链）和 `real_food_animal_hunt` 1/1（公平第一人称
观察、接近并获取食物）。它们覆盖用户反馈的“被怪物打后只看不做”这一局部身体路径，但都是
受控夹具、无模型内循环，不能宣称模型 PVP、自然随机世界或 M0--M4 通过。

接下来审计模型未就绪分支：确保任何高层任务都不会在没有真实模型决策时伪装成已执行，同时保留
本地紧急生存和低风险装备的合法反射；正式模型请求仍因先前上游 401/Token Plan 使用边界保持
停止，不重复发送用户凭据。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.27 movement and damage-response audit (open)

本轮先核对了用户反馈中的“转头很慢、跟随只说不走、受击后盯着怪物不动”。当前代码证据显示：
`FairPlayerActuator` 每 tick 最多旋转 20°，180° 只需约 9 ticks；`MoveToSkill` 仅在水平误差
达到阈值后推进，并在 20/80 ticks 的对齐停滞窗口重规划。因此暂时没有证据把旋转上限本身改大；
高层技能仍严格要求模型网关就绪，无 API 时只运行本地生存反射，这个边界与产品声明一致。
受击路径已是 20 TPS `LivingDamageEvent -> recordDamage -> requestObservation(SEMANTIC_REFRESH)`，
并由公平视锥/遮挡采样提供实体；不允许通过隐藏实体扫描“解决”误报。正确选择器
`live_model_selector=mcai_companion:real_player_chat_to_immediate_bound_follow` 已在 Forge 65.1.0
当前源码通过 1/1，但这是 HoldingModelGateway 内循环证据，不是真实模型或真实客户端门禁。

本检查点之后尚无新的产品源码变更；此前的 Keychain `-w <password>`、保存后读回核验、软期限配置、
以及不确定地形形状提示均已在 v91.24--v91.26 记录。最近一次正式失败仍是上游模型 401，以及
Darwin 无 Xvfb 的 Actor/Observer 真实客户端前置失败；M0--M4、真实模型、聊天到动作和移动正式门禁
继续保持 `NOT_RUN`，第一次错误使用选择器启动的整批测试不计证据。

下一步立即运行正确选择器的无模型专服受击/十僵尸十骷髅回归，若出现可复现停滞再修改最小公平
动作链；若通过则只记录内循环证据，并继续检查真实模型/真实客户端的外部阻断，不把脚本或无模型
通过冒充专业陪玩验收。

后续定向结果：十僵尸/十骷髅与铁傀儡场景均在 Forge 65.1.0 通过 1/1；`offline_idle_equipment`
也通过 1/1，确认没有模型时仍可通过原版背包菜单自动穿戴已拥有的铁帽和盾牌。它们都只是
当前源码的真实 headless/无模型内循环证据，不能替代用户 API 的真实模型请求、真人客户端或正式
PVP/M0--M4 门禁。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.26 bounded terrain-shape cues (open)

为回应“深峡谷被描述成石头堆、站在地形里不知如何走”的可复现感知缺口，
`SemanticObservationJsonCodec` 在既有第一人称表面射线摘要上增加三个不确定提示：
`possible_canyon_or_cliff_wall`、`possible_confined_uneven_terrain`、
`possible_drop_or_overhang`。它们只由当前已命中的上/下/侧表面、相对高度跨度和最近距离推导，
不扫描邻区块、不读取隐藏地形；JSON 警告明确标为 uncertain hypothesis，规划器仍被要求重新观察
后再跳跃、搭桥、下降或穿越。新增 codec 回归覆盖四个可见表面和提示边界；定向感知/规划器测试通过。
这改善模型输入可解释性，但没有真实模型或随机峡谷纵向门禁证据，不能宣称地形专家已验收。

当前源码重新打包与精确专服 smoke：产品 JAR SHA-256
`2f46900f12f330371e8de15e5f62916d1c2ec7f5f3b5c7a71d82816f43ae7030`，审计 slim JAR SHA-256
`129c7d1ec0d7fd3240b104114f80f8295ccc4139fc66a44b7db7bb49d792bb69`；归档
`e2e/results/no-commit-dirty/20260810T092830Z-e2e58d5314bb4e8`。生命周期和精确 SHA 通过，
Oracle 无模型配置停止，`functionalAiClaim=false`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.25 persistent-store round-trip guard and status clarity (open)

在 v91.24 的 macOS helper 修复上再加了跨平台保存后读回核验：
`ApiKeyManager.saveFromSetup` 只有在平台安全存储读回值与候选凭据逐字符一致时才返回
`persistent=true`；写入成功但读回为空/失败会明确返回
`process_only_secure_store_unavailable`，不会把下一次重启风险隐藏起来。加载副本只在内存中
比较后立即清零。新增 `ApiKeyManagerRestartPersistenceTest` 的 write-only store 回归，和原有
跨管理器重启/并发测试均通过。

设置界面补齐 `ready`、Keychain、unchanged、process-only、恢复失败等状态的中英文说明，避免
把原始内部状态码显示成“无响应”或把仅当前会话的凭据误报为重启安全。

当前工件已重新打包：`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar` 通过；产品
JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`39edbe51a4acd144af8893147d327757f3d8e831431e4655f219830ea7a9e5a2`，审计 slim JAR SHA-256 为
`ba4473ffc39af80b4e56366f27acf1f6f85757943265361bbe07af646d247671`。精确产品 JAR 专服 smoke
归档为 `e2e/results/no-commit-dirty/20260810T092229Z-e2e787c3b3608de`，专服启动、AI
ServerPlayer、SQLite Jar-in-Jar、精确 SHA 和干净退出均通过；Oracle 无模型配置而停止，
`functionalAiClaim=false`。

同一当前工件类路径创建全新的 `ApiKeyManager` 读取现有 macOS Keychain（只输出布尔值、长度和
格式形状，不输出密钥内容），结果 `fresh_manager_restored=true credential_length=51
prefix_shape=true`；这证明“新服务运行时恢复”本地链路已可用。该结果不等价于服务商认证成功，
也不改变已知上游 401 和 Token Plan 使用范围限制。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.24 macOS Keychain persistence root-cause fix (open)

复核用户反复反馈的“重进世界必须重新输入 API Key”后，确认不是设置界面单纯丢状态，而是
`MacOsKeychainCredentialStore.save` 的真实实现错误：`security add-generic-password -w` 在
非交互进程中不从 stdin 读取密码，旧代码会返回成功但创建空密码项；随后新的
`ApiKeyManager` 能找到 Keychain 项却拿不到实际凭据。已修复为系统工具支持的非交互
`-w <password>` 形式；父进程不记录命令行、不写日志，启动子进程后立即清空命令数组，错误输出仍被丢弃，
密钥不会进入 TOML、世界、SQLite、日志、崩溃报告或截图。Windows DPAPI、Linux Secret Service、
`MCAI_API_KEY(_FILE)` 注入路径未改变。

验证：在当前 macOS 主机用独立测试 service/account 写入一个非用户密钥占位值，随后通过同一
适配器读取并核验长度/首字符，最后删除测试项；结果 `loaded_length=24 first_char_matches=true`，
退出码 0，未接触用户 Keychain 项。定向 `ApiKeyManagerRestartPersistenceTest`、
`ApiKeyManagerConcurrencyTest`、`ModelRuntimeTest` 通过。该证据只证明本地凭据持久化边界，
不证明 MiMo 凭据有效或真实模型陪玩；现有真实模型 401、Actor/Observer 缺失和 M0--M4 正式门禁
仍保持 `NOT_RUN`。

下一步：重新打包并运行精确产品 JAR 的专服生命周期 smoke，更新工件哈希；随后继续真实模型
纵向切片所需的外部凭据/隔离客户端检查，不把无模型 GameTest 升格为功能通过。

重新打包结果：`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar` 通过；产品 JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`6a6f9bf293047794cb277453f2911feb0ffb99ba0e3bc0ee66a228bcf20db341`，审计 slim JAR SHA-256 为
`73169613e79cb4919debeca41a5777dac993a15fc4cc1a9df31d9ed8f438d678`。精确产品 JAR 专服
smoke 归档为 `e2e/results/no-commit-dirty/20260810T091548Z-e2ee8179f946fe8`，生命周期检查
通过且精确 SHA 一致；Oracle 因没有模型配置在结果前停止，`functionalAiClaim=false`，因此
不是聊天到动作或正式 M0--M4 证据。工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.23 immediate follow Forge retest (open)

当前产品源码在 Forge 65.1.0 / MC 26.2 实际 GameTest
`real_player_chat_to_immediate_bound_follow` 1/1 通过。测试由正常 ServerPlayer 聊天提交自然
跟随请求，服务端绑定发起者身份、安装普通 `follow_entity` 技能并推进身体；第二条跟随催促保留
原目标且没有重复替换。该链路不依赖模型往返，但仍仅覆盖受控测试场，不是复杂峡谷/随机地形或
真实模型专家证明。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.22 Enderman regression (open)

在 v91.21 方向性受击修复后，Forge 65.1.0 / MC 26.2 实际服务端
`real_emergency_enderman_defense` 1/1 通过；末影人受击、传送后的有限重捕获、盾牌/近战和身体
生命周期均完成，未调用模型网关。该结果只属于当前源码的无模型物理回归，不改变真实模型、双客户端、
Hardcore 随机种子或 M0--M4 正式门禁的 `NOT_RUN` 状态。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.21 directional damage footwork and Forge combat retest (open)

当前可复现的“光说不做/受击只举盾”身体缺口已做一个窄范围修复：
`src/main/java/dev/mcai/companion/skills/core/EmergencySurvivalController.java` 现在只在
自身公平伤害事件携带非零方向时，允许已处于 `GUARDING`/`HOLDING` 的状态重新选择一个刚观察到的
安全相邻格；没有完整格证据时，仍只发有界的原版潜行退避输入。它不读取攻击者身份、隐藏坐标或玩家意图，
也不会因被玩家击中自动还击。盾牌连续使用与 `RETREATING` 的时间上限保持不变。对应回归测试在
`src/test/java/dev/mcai/companion/skills/core/EmergencySurvivalControllerTest.java`，覆盖方向性
受击、盾牌和潜行后退。

验证结果：定向 `EmergencySurvivalControllerTest` 通过；Forge 65.1.0 / MC 26.2 实际服务端
`real_emergency_iron_golem_duel` 1/1 通过；`real_emergency_zombie_skeleton_horde`（十僵尸、十骷髅）
1/1 通过。两项均为无模型身体/原版物理证据，不是模型决策或正式 PVP 统计。

当前源码重新完成 `check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar`，全部通过；产品 JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`34a4bff873d9b8a46510f78632e94daefbf3402c805749afcf8023904dabc50e`，审计 slim JAR
`1921dd57474b3404bbd4de052163f8c2f54a5722da52a01dd5a231dc9755ecb5`。精确产品 JAR 专服 smoke
归档为 `e2e/results/no-commit-dirty/20260810T085930Z-e2eee78ce9309ca`，生命周期检查通过，
`functionalAiClaim=false`；工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

最后的正式阻断仍未改变：真实 MiMo 凭据能力探测此前收到上游 401，当前 macOS 无 Linux/Xvfb
Actor/Observer worker；真实模型 chat-to-action、双客户端、Hardcore 随机种子和 M0--M4 继续
`NOT_RUN`。下一步是继续做可复现的本地动作/模型协议修复，并在合规有效凭据和隔离客户端 worker
到位后先跑真实 dedicated + Actor/Observer 纵向切片；不把本地 GameTest 或无模型身体测试升级为
“专业陪玩”通过。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.20 package and lifecycle retest (open)

本轮变更后的完整 JVM/工件链已通过：`check jar jarJar verifyReleaseJar e2eClientJar
e2eOracleJar --offline --no-daemon --rerun-tasks -Pforge_compile_version=65.1.0`，以及 Python
E2E `30/30`、Python 编译和 JSON/兼容校验。当前产品工件为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`176b197edfc5db4240090c77c35b92d1eb7dd4775a88c62fd1ab604f578a6c67`；测试用 slim JAR
`9f8b0f1b5af55be6344c4b95399601b4e404043d4690d16a7413d42a03779318`，仍不属于可安装产品。

精确产品 JAR 的 Forge 65.1.0 dedicated-server smoke 也通过：AI `ServerPlayer` 出现、
SQLite 仅从产品 Jar-in-Jar 加载、精确 SHA 绑定、干净退出均通过；归档为
`e2e/results/no-commit-dirty/20260810T084901Z-e2eed372da18cab`。Oracle 在无模型配置下
按设计停止，`functionalAiClaim=false`，所以该归档不是聊天到动作、双客户端、模型或 M0--M4
证据。工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，正式门禁保持 `NOT_RUN`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.19 punctuation-safe follow intent (open)

本轮先复核了当前用户侧“光说不做”的可复现边界：离线装备 GameTest 在 Forge 65.1.0 /
MC 26.2 实际服务端启动并通过，说明已经拥有的铁头盔和盾牌会通过原版背包菜单事务穿戴；
它不是当前停滞的根因。当前真实模型门禁仍在能力握手阶段收到上游 `AUTHENTICATION`/
401，因而高层技能合法保持停用，不能把无模型 GameTest 当作陪玩通过。官方 MiMo 文档仍
确认 Token Plan 使用 `tp-` 凭据和专用 Base URL，并允许 `api-key` 或 Bearer 认证；本次
没有输出、写入或重新传播任何密钥。

为减少真实聊天中的“跟我，走”/“跟我……”被当成普通对话，已改
`src/main/java/dev/mcai/companion/communication/PlayerTaskIntent.java`：只在跟随短语
匹配阶段去除标点和空白，增加“跟我去”“跟过来”“跟着走”等自然变体；原始玩家文本仍保留
在审计/目标上下文中，不改变模型权限或世界写入边界。新增断言在
`src/test/java/dev/mcai/companion/communication/PlayerTaskIntentTest.java`。

定向验证：`PlayerTaskIntentTest` 通过；Forge 65.1.0 实际 GameTest
`mcai_companion:offline_idle_equipment` 通过（1/1，普通菜单装备，零模型）。最后失败门禁
仍是 Darwin 无 Linux/Xvfb 且当前真实凭据 401；Actor/Observer、真实模型 chat-to-action、
M0--M4 继续 `NOT_RUN`。下一步是获得合规且有效的模型凭据后，先跑真实 dedicated + Actor /
Observer 纵向切片；在此前继续只做可复现的本地安全/动作修复并保持正式门禁未通过。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.18 provider-auth rejection quarantine (open)

真实 provider 探测只发出 1 个请求，当前本机安全存储中的凭据被服务商拒绝；结果为
`AUTHENTICATION`，没有把失败伪装成可用模型，也没有输出密钥。这个失败解释了“进世界后
API 又失效/身体只说不做”的一条真实根因：认证失败后旧凭据仍可能被同一运行时再次恢复。
本轮再次用同一真实配置运行 `LiveProviderSmokeTest`，仍在第 1 个请求收到
`AUTHENTICATION`；失败发生在能力握手，不是游戏脚本或模型动作结果。

现在 `ModelRuntime` 在 401/403 后会清除进程内凭据、标记
`credentialRejected`，阻止恢复/能力探测/缓存模型重新启用，并让设置快照报告凭据不可用；
不自动删除 Keychain、DPAPI 或 Secret Service 中的持久密钥。只有用户在设置页显式保存一
个替换凭据，运行时才解除隔离；空凭据更新会返回
`api_key_rejected_requires_replacement`。新增 `ModelRuntimeTest` 覆盖恢复阻断、探测阻断、
空凭据拒绝和新凭据解除隔离。

定向 `ModelRuntimeTest` 在 Temurin JDK 25/Forge 65.1.0 下通过。当前仍未有有效的真实
模型凭据，所以 Actor/Observer、自然聊天到动作、Hardcore 随机种子和 M0--M4 继续
`NOT_RUN`；最近正式可复现的专服 smoke 仍仅证明生命周期，不能证明陪玩功能。下一步是
把新的隔离状态接入真实客户端设置页回归，并在用户重新输入且 provider 探测成功后运行
真实模型纵向切片；在此之前不升级任何正式门禁。随后完整
`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar`、30/30 Python 审计和兼容
校验均通过；当前产品 JAR SHA-256 为
`bc5e16d096ca797383cd80f6371dadb95e42934d7c29730628d6f095fb9b1753`，审计 slim JAR 为
`9bc89eeeb6877b7a61ecc314ae98efebf393eea76317469f9b0f109d158cc278`。
随后以同一精确产品 JAR 运行
`e2e/results/no-commit-dirty/20260810T083151Z-e2ef802762c8cd6` 专服 smoke：自动出现
AI `ServerPlayer`、SQLite 仅从产品 Jar-in-Jar 加载、干净退出和精确 SHA 绑定均通过；
Oracle 因无有效模型配置在结果前停止，`functionalAiClaim=false`，所以这只是生命周期
证据，不是模型聊天或动作通过。
设置页还增加了 `api_key_rejected`、`saved_probe_authentication` 等状态的中英文可读
提示，避免用户把认证失败误解为模型仍在思考；未知服务端代码继续原样保留用于诊断。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.17 explicit credential precedence (open)

又闭合一个可复现的跨平台恢复边界：`ApiKeyManager.unlockPersisted()` 之前先读
Keychain/DPAPI/Secret Service，再读 `MCAI_API_KEY(_FILE)`；当服务账号留下旧安全
存储项时，管理员轮换的显式注入会被旧值遮蔽，表现为“重新进世界后 API 失效”。
现在显式外部注入优先，若没有注入才读取平台安全存储；凭据仍只进可擦除进程内存，
不会写入世界/TOML/SQLite/日志，且同一进程内保存的设置值仍由 session store 优先。
新增重启持久化回归覆盖旧持久值被新注入覆盖，README、SECURITY、IMPLEMENTATION_STATUS
同步了跨平台语义。

验证：固定 Temurin JDK 25/Forge 65.1.0 下
`ApiKeyManagerRestartPersistenceTest` 通过；完整
`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar --offline --no-daemon`
通过；当前唯一可安装产品 JAR SHA-256 为
`ad83a6a11756e24a41add0b2910752179e4c8727c24e532d2aeb7ec9d643c08d`，审计 slim JAR
为 `e9a070a3f4f410a8b87e12cad5c914db1a6aac198fa400504f0724732b11d66b`。这些是
本地凭据/工件门禁，不是实际模型或游戏陪玩证明。

最后失败门禁仍是真实客户端 preflight：Darwin 无 Linux/Xvfb、模型端点/名称和凭据；
真实 Actor/Observer、模型 chat-to-action、M0--M4 仍 `NOT_RUN`。下一步是在可用的
隔离 Linux/Xvfb worker 和合规模型凭据上运行现有 functional 及 M3 摘要协议；没有这些
外部前置时继续保持 `NOT_RUN`，不把本地测试冒充正式通过。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.16 M3 evidence protocol (open)

本轮继续的可执行缺口不是再写一个“理论上支持生电”的声明，而是 M3 正式
门禁此前只有一个泛化 `scenario_runner_not_implemented` 分支，无法审计陪玩、
建筑、农场/机器变体和长期记忆摘要。新增 `e2e/m3_protocol.py`、
`scripts/aggregate-m3-summaries.py`，并将 `e2eM3` 接入正式入口。协议拒绝
凭据、提示词、世界路径、玩家身份、原始种子、重复 case 或 partial shard；每个
case 必须由真实 dedicated server、正常客户端、实际配置模型和只读 Observer
共同证明，且明确无作弊命令、无直接世界/背包修改、重启/区块卸载/玩家中断均已
验证。聚合器还要求至少 50 个自然语言陪玩 case、30 个未见场地建筑 case、协议
列出的每种农场和机器能力各 3 个未见变体，以及 100 小时、10,000 标点、100,000
资产和查询 p95 预算；精确产品 SHA-256 与 40 位源码提交绑定后才有资格进入
release pass。

已改文件：`e2e/m3_protocol.py`、`scripts/aggregate-m3-summaries.py`、
`e2e/formal_gates.py`、`e2e/test_m3_protocol.py`、`e2e/test_formal_gates.py`、
`e2e/README.md`、`docs/verification/M3.md`、`docs/progress/GOAL_STATE.json`。
定向 M3/formal-gate 测试、Python 全套 `30/30`、py_compile 和 GOAL_STATE JSON
校验通过。协议小夹具仅证明 fail-closed wiring，不是模型或游戏功能证据。

最后的真实功能门禁仍是 `python3 e2e/orchestrator.py preflight
--forge-version 65.1.0` 返回 `ready=false`：当前宿主 Darwin，缺 Linux/Xvfb、
`MCAI_BASE_URL`、`MCAI_MODEL` 和 `MCAI_API_KEY`/文件注入；真实 Actor/Observer、
模型聊天到动作、M0--M4 仍为 `NOT_RUN`。下一步是获得合规模型凭据和隔离 Linux/Xvfb
worker 后运行现有 `functional`，然后按 M3 摘要协议归档真实分片；在此前继续做
本地可验证的安全/动作回归，但不把任何无模型 GameTest 或协议夹具提升为专业陪玩
通过。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.15 real-client preflight boundary (open)

最新 `python3 e2e/orchestrator.py preflight --forge-version 65.1.0` 仍明确返回
`ready=false`：当前宿主为 Darwin，缺少 Linux host、Xvfb、`MCAI_BASE_URL`、`MCAI_MODEL` 和
`MCAI_API_KEY`/`MCAI_API_KEY_FILE`。预检没有启动 Minecraft 客户端、没有触碰物理显示器、没有
读取或发送任何密钥；这是真实功能门禁的基础设施状态，不是产品 PASS/FAIL。故 chat-to-action、
双客户端渲染、M1--M4 随机 Hardcore 统计继续保持 `NOT_RUN`。本轮可完成的凭据、保存/重启、
WAL、工件和本地回归已继续闭合，下一步外部恢复命令仍是把合规模型与 Linux/Xvfb worker 注入同一
`e2e/orchestrator.py functional`/隐藏种子 runner。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.14 WAL restart audit closure (open)

重启门禁暴露并修复了一个真实的审计竞态。`restart-smoke` 第一次读取时偶发得到
`readable_production_memory_database`/零生命周期行，但随后同一归档的 SQLite 已出现完整
4 行（两次 `started`、两次 `stopping`）；不是产品生命周期丢失，而是 Forge userdev 退出尾部的
WAL/共享内存锁和只读 URI 的即时读取相撞。生产 `MemoryDatabase.close()` 现在在释放 JDBC 连接前
做有界的 `wal_checkpoint(TRUNCATE)`；`e2e/restart_gate.py` 使用 `query_only`、30 秒 busy timeout；
`e2e/orchestrator.py` 对只读验证做最多 30 秒的有界重试，不修改数据库、不放宽缺失证据。

定向 MemoryDatabase/JUnit 与 Python restart-gate/orchestrator 测试通过；随后固定 JDK 25 的完整
Gradle `check`、Python E2E `26/26`、Python 编译、JSON 校验和兼容声明校验也通过。最新真实两次启动归档
`20260810T074848Z-e2e1d12655b10fc` 在 Forge 65.1.0 为 `PASS`（`verifierAttempts=1`，4 条
lifecycle audit，稳定 companion UUID，start goal revisions `[0, 0]`，两次 clean exit），产品
SHA-256 为 `520bdfcda0e6296db08d3cfa5ed231267dde4037e74939dfe7c19aab8901aed9`，但
`functionalAiClaim=false`。这仍是生命周期/保存恢复证据，不是模型聊天、实时客户端或 M0--M4
通过；正式状态继续 `NOT_RUN`，工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.13 injected-credential restart regression (open)

为直接覆盖 Debian/Windows Server/容器的无 UI 路径，新增
`ApiKeyManagerRestartPersistenceTest.aFreshServerManagerRestoresAnInjectedCredentialWithoutUiInput`：
两个独立服务端作用域使用 `MCAI_API_KEY` 注入，在没有平台持久存储时都能从启动注入恢复，且
测试会清零读取到的临时 `char[]`。该测试与 secure-store 进程降级、ModelRuntime 和设置状态
测试均通过固定 Temurin JDK 25/Forge 65.1.0 定向 Gradle 运行；随后完整 `./gradlew check`
也通过（含兼容检查与全部 JVM 测试）。生产 exact-JAR smoke 的上一
次绑定仍是 `20260810T073450Z-e2e4a5896da4719`，未因测试源文件改变产品逻辑；正式 M0--M4
仍保持 `NOT_RUN`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.12 setup status correctness (open)

定向复核又发现一个 UI 语义 bug：`ModelSetupModule` 之前用
`!outcome.credentialPersistent()` 直接计算 `restartRequired`，所以玩家只改 Base URL 或
Model Name、继续使用已从 Keychain/DPAPI/Secret Service/环境注入恢复的凭据时，也会看到错误的
“restart required”。现在由 `requiresCredentialRestart` 只把 `process_only` 与
`process_only_secure_store_unavailable` 标记为需要重启注入；`unchanged` 和实际平台安全存储都
不会误报。新增 `ModelSetupCredentialStatusTest` 覆盖这些存储值。

固定 Temurin JDK 25、Forge 65.1.0 的定向 Gradle 测试通过（凭据恢复、ModelRuntime、设置状态），
此前 exact-JAR smoke 仍为 `20260810T073125Z-e2e0e32248a9531` PASS。该修复只改善设置反馈，不
改变模型请求、安全边界或正式门禁；M0--M4/真实 Actor+Observer/真实模型继续 `NOT_RUN`。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.11 packaged fallback verification (open)

跨平台凭据修复已完成定向闭环：固定 Temurin JDK 25、Forge 65.1.0 下 `./gradlew check
--offline --no-daemon --rerun-tasks` 成功；随后 `jar jarJar verifyReleaseJar e2eClientJar
e2eOracleJar` 成功。随后 exact-JAR dedicated-server smoke
`20260810T073450Z-e2e4a5896da4719` 在 Forge 65.1.0 通过：启动、Oracle、headless
ServerPlayer、SQLite Jar-in-Jar、精确工件和优雅退出均通过，`functionalAiClaim=false`。
该次实际生产工件为
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，SHA-256
`1094ac0e9fe8ecd5032f99cfb8a9fdc7172aa463b4c0f6ba4d8261339c3ffc55`，审计 slim JAR 仍隔离在
`build/audit-libs/`；Python E2E 26/26、JSON 校验、Python 编译和 secret-pattern 扫描通过。
这只是源码/工件回归，未运行真实模型或双客户端，不改变 formal M0--M4 的 `NOT_RUN` 状态。

下一步优先级保持不变：在可用合规模型、Linux/Xvfb Actor+Observer 和隐藏随机 Hardcore worker
就绪后，运行真实 chat-to-action、移动、背包/菜单与长时门禁；在这些前置条件缺失时继续修复可验证
的本地安全边界，并保持 `DIRTY_NO_COMMIT`/`NON_RELEASE`，不把本地夹具结果包装成专业陪玩证明。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.10 cross-platform credential fallback (open)

本轮继续处理“换平台/重启后 API Key 不可用”的确定性缺口。根因不是 API Key 被写入日志或
模型网关重复请求，而是 `ModelRuntime.storeAndInstallProfile` 在设置页请求持久化保存时，把
“当前平台没有可用安全凭据存储”错误地当成硬失败；这会阻断 Debian 无桌面 Secret Service、
受限 Windows/macOS 环境，以及服务器进程内通过环境变量注入的合法降级路径。`ApiKeyManager`
本来已经支持 macOS Keychain、Windows DPAPI、Linux Secret Service、`MCAI_API_KEY` 和
`MCAI_API_KEY_FILE`，但此前最后一个分支被 runtime 拒绝，导致用户看见保存失败或下次进入世界
再次输入。

已改文件：`runtime/ModelRuntime.java` 接受并明确标记
`process_only_secure_store_unavailable`（只在当前进程有效，不伪装成重启安全保存）；
`modelsetup/ModelSetupModule.java` 将该结果映射为 secret-free 的
`saved_verified_process_restart_required` 状态并设置 `restartRequired=true`；英文/中文语言包
增加对应说明；`credential/ApiKeyManagerRestartPersistenceTest.java` 改为验证 secure store
不可用时仍可用当前会话、但 `credentialPersistent=false`。下一次启动应通过系统安全存储或在启动前
设置 `MCAI_API_KEY`/`MCAI_API_KEY_FILE` 恢复，运行时不会把明文密钥写入世界、TOML、SQLite、
日志或崩溃报告。

本轮最后一次定向 Gradle 运行（固定 Temurin JDK 25、Forge 65.1.0、offline、rerun）中
`ApiKeyManagerRestartPersistenceTest` 与 `ModelRuntimeTest` 通过，`BUILD SUCCESSFUL`；尚未把
此结果升级为正式模型或游戏门禁。正式 M0--M4、真实模型、Actor/Observer、随机 Hardcore 统计
仍为 `NOT_RUN`，工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步是跑完整 `check`、Python E2E
和 JSON/密钥审计，再在具备合规模型与隔离 worker 时运行真实分片；不可用外部前置条件时继续
保持正式状态为 `NOT_RUN`，不凭本地夹具宣称陪玩可用。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.09 hidden-seed statistical gate (open)

本轮没有把受控 GameTest 或单个成功样本提升为 M4。当前正式缺口是隐藏种子评测只有分片
`summary.json` 生成器，没有安全的跨分片审计入口；直接相信分片计数会允许重复用例、准备未运行
用例、原始种子泄露或被篡改的小时比例进入统计结果。已新增共享协议模块
`e2e/hidden_seed_protocol.py`、命令 `scripts/aggregate-hidden-seed-summaries.py`，并将
`aggregateHiddenSeeds` 接入 `e2e/formal_gates.py`。

聚合器现在逐分片重算终态、Hardcore/evaluation lock、污染/死亡、末影龙/返回传送门、1/2/6 小时
计数和比例；拒绝 raw seed、private salt、caseDir、重复 `caseId`/seed commitment、非 terminal
或 harness failure。completion 的 1,000-case M4 只在 2 小时至少 `ceil(95%)` 且 6 小时至少
`ceil(99%)` 时才返回 PASS；缺少 `MCAI_HIDDEN_SEED_SUMMARIES` 时正式门禁明确返回 `NOT_RUN`。
M1 foundation 和 M2 completion 的聚合阈值也在共享协议中保留，尚未执行任何正式样本。

本轮定向证据：Python E2E `26/26` 通过，hidden summary 协议/工件绑定/重复/计数篡改/原始种子/慢样本/Hardcore
死亡分母测试通过，M1/M2/M4 三条统计门禁的无摘要边界通过，正式 release 还要求
`MCAI_EXPECTED_PRODUCT_SHA256` 精确绑定，CLI 缺文件 fail-closed，
`aggregateHiddenSeeds` 无摘要返回 `NOT_RUN`，Python 文件编译通过；
固定 Temurin JDK 25 下 `./gradlew check --offline -Pforge_compile_version=65.1.0` 也通过。
没有真实模型、Linux/Xvfb Actor+Observer 或随机 Hardcore 评测运行，因此 M0--M4、
`m4Hidden1000` 仍为 `NOT_RUN`；工作树仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步是把该统计
入口在可用合规模型与隔离评测 worker 上运行真实分片，并在干净 release commit 上复核，不伪造统计结果。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-10 continuation checkpoint: v91.08 lifecycle safety and p95 closure (open)

本轮已闭合当前真实整合门禁的最后一条性能阻断，但仍未提升正式 M0--M4 状态。根因链如下：
`run-debug293` 在末地入口持续收到无方向 `RECENT_DAMAGE_EVENT`，原安全扫描只转头不脱离；新增
的第一次方向性探针在 `run-debug295` 被证明方向错误，身体进入入口石墙并发生窒息。探针改为沿当前
第一人称扫描方向的有界潜行输入后，`run-debug297` 已真实走完末地击龙/返回路径，但 rolling p95
为 3.669 ms，性能门禁失败。随后 `run-debug298` 的导航/应急 JVM 定向测试通过；导航射线采样和
重复 AIR 证据做了无行为变化的分配优化，并在 `PerceptionNavMapper` 对同一语义 revision 缓存不可变
`LocalNavSnapshot`，避免多个读者重复复制滚动地图。

当前源码真实 Forge 65.1.0 无客户端 lifecycle 复测为 `run-debug302`（终端会话最终完整收尾）：
登录/移除/重连、原版菜单与装备、移动、下界传送门、掉落物拾取、末地入口、末影龙击杀、返回
传送门全部完成；`samples=6159`、平均 `641592 ns`、rolling p95 `1497500 ns`、窗口最大值
`57745041 ns`，低于 2 ms p95 门禁，GameTest `1 required tests passed`，Gradle `BUILD SUCCESSFUL`。
这是无模型、无真人客户端的 embodied inner-loop 证据；不是实时模型、Actor/Observer、Hardcore
随机种子或 M0--M4 统计通过。当前模型凭据历史探测仍为一次 HTTP 401，不能据此宣称 AI 陪玩。

已改文件：`skills/core/EmergencySurvivalController.java`、其定向测试、
`navigation/PerceptionNavMapper.java`（射线分配优化与快照缓存），以及本检查点和状态证据。
随后完成的当前源码审计为 `check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar` 全部成功、
JVM `1066`（0 failures/0 errors/2 skipped）、Python E2E `20/20`、Forge 65.0.0 compile 成功、
兼容声明和密钥模式扫描通过；`run-debug303` 零真人区块/死亡/重生和 `run-debug304` 真人登录邻近
出生也各自 `1/1 PASS`。下一步仅在存在合规可用模型和真实 Actor/Observer 环境时运行聊天到动作门禁，
继续保持正式 M0--M4 `NOT_RUN`，不把这些无模型生命周期结果包装成陪玩或通关证明。

当前源码的两条局部战斗复测也通过：Forge 65.1.0 `run-debug305` 铁傀儡单挑 `1/1`（4.192 s），
`run-debug306` 十僵尸/十小白局部生存 `1/1`（4.224 s）。它们证明无模型时本地应急 PVE 车道不会
原地挨打，但不等于模型控制 PVP 或正式 M0--M4 统计门禁。

Updated: 2026-08-10 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.07 coordinate-free crop maintenance (open)

当前 bug 根因已经收敛：整田维护并非缺少模型话术，而是普通玩家身体在处理后半块农田时会被水渠、
不可达旧工作位和过长巡检拖住，最终表现为“接受任务但不完成”。修复保持公平边界：技能仍只消费自身
第一人称滚动观察，不读隐藏区块或世界坐标答案；每次破坏和补种前重新观察并绑定 revision。不可达格
改为延后处理，巡检改用有 240 tick 上限的滚动 `TravelToSkill`，并让 `MoveToSkill` 在水中只朝新鲜观察
到的安全落脚格持续游动/跳跃。机制地图保留期延长到 12,000 tick，但动作仍现场重验，且总半径、容量、
会话和维度边界不变。

最后一次失败门禁为 `run-debug180`：完成 3/8 后巡检耗尽；随后 `run-debug181`、`run-debug182` 在
Forge 65.1.0 两个独立 dedicated GameTest 进程中连续 1/1 PASS。每次都要求真实 headless
`ServerPlayer` 完成八次原版挖掘、拾取至少八份小麦、八次原版补种，并最终看到全部八格为 0 龄小麦。
这只是 release-excluded、无模型、无真人客户端的 embodied inner-loop 证据，不是 M3 或随机种子
通关通过。

最低版本复验又发现并闭合了一个此前被几何运气掩盖的生产缺陷。`run-debug183` 在 Forge 65.0.0
完成 4/8 后于水渠旁耗尽；给短程 `TravelToSkill` 加上同等的公平岸边游泳恢复后，`run-debug184`
仍在 3/8 后失败。其日志证明身体实际站在无碰撞的小麦内，但导航把所有非空气视觉表面都归类为
`SOLID`，形成“身体占用的头部格是墙”的自锁。语义格式 7 现从已被第一人称射线命中的原版方块状态
只发布 `EMPTY` 或 `OBSTRUCTED_OR_PARTIAL` 粗粒度碰撞 affordance，不暴露碰撞箱或相邻隐藏方块；
导航把 `EMPTY` 映射为可穿行且保留独立来源。修复后 `run-debug185`（32.05 秒）和离线缓存复跑
`run-debug187`（18.52 秒）在 Forge 65.0.0 连续 1/1 PASS。`run-debug186` 仅在启动前下载 launcher
manifest 超时，未进入 Minecraft，单独保留为基础设施失败而非功能结果。

本轮主要改动文件为 `skills/farming/MaintainObservedCropFieldSkill.java`、
`HarvestAndReplantStepSkill.java`、`skills/core/MoveToSkill.java`、
`skills/core/TravelToSkill.java`、`navigation/PerceptionNavMapper.java`、
`perception/VisibleBlockFace.java`、两个生产感知采样器、`mechanism/MechanismSurveyPolicy.java`，以及
对应 planner/skill/GameTest 测试和 test resources。恢复时的下一步是完成正在运行的完整 JVM
回归，再在 Forge 65.1.0 复验当前源码并进行组装和 release JAR 内容审计；
正式 Actor/Observer、合法真实模型、无玩家服务器长时运行和 M1--M4 统计门禁继续保持 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.06 field commissioning (open)

`run-debug161` 在前一版整田物理门禁之上增加了投产判据并再次 1/1 PASS（Forge 65.1.0，
GameTest 约 1.552 分钟）。`FieldScenario` 只在 skill 启动前把测试专用
`random_tick_speed` 设为 128，cleanup 恢复原值；生产 Mod 和技能均不会修改 GameRule。施工完成
后测试不再立即成功，而是继续等待原版随机刻，始终要求一格水源、八格耕地和八株小麦不退化，
最终只有八格 `FarmlandBlock.MOISTURE == 7`、八个作物格原版亮度均 >=9、且小麦年龄总和 >0
才通过。由此证明当前结构能经原版水合并至少发生一次真实生长，但加速样本不是默认速度
items/hour 测率，也不替代重启、区块卸载和三场地泛化门禁。

新增改动集中于 release-excluded `FarmingGameTests.FieldScenario`，并使用公开
`GameRules.RANDOM_TICK_SPEED`、`FarmlandBlock.MOISTURE` 和 `CropBlock.AGE` 只读验收；没有直接调
`randomTick`、设置作物年龄或在 skill 边界后改方块。定向 JVM 编译/测试 PASS。正式状态仍为
M0--M4 `NOT_RUN`、`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步实现一个坐标无关的整田维护技能：
滚动勘测成熟作物、生成现场工作顺序、普通步行并逐格委托现有
`harvest_and_replant_step`，最终核验每个成熟格已补种及收获物确实进入自身背包；随后再做真实
物理门禁。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.05 physical whole field (open)

`build_hydrated_crop_field` 的首条 release-excluded 真实 Forge 物理门禁已经完成失败到修复闭环。
`run-debug158` 在第 0/9 个水源作业只记录到装备铲子后失败，且多层技能前缀超过
`SkillFailure` 的 64 字符边界，真实原因被错误归一成 `skill_failure`。现在
`PrepareWaterSourceSkill` 将 `break_block`/`use_item` 子错误映射为有界、稳定、可操作的
`break_*`/`place_*`，整田执行器继续映射为 `water_*`/`plot_*`；定向测试保证嵌套错误既具体又
不越界。`run-debug159` 随即准确暴露
`build_hydrated_crop_field.water_break_action_world_denied`。

拒绝来自正式 `PlayerSupportBlockGuard`，不是 Forge 权限或视角：旧施工停靠半径 0.35 格会让
宽 0.6 格的玩家停在工作格边缘并把包围盒压到相邻待挖格，安全层因该方块仍承载玩家而正确拒绝。
`MoveToParameters` 现在允许最低 0.10 格的普通精确停靠，整田工作位使用 0.15 格；在最坏轴向误差
下仍给相邻方块留下至少 0.05 格净空，不绕过支撑保护，也不传送。移动参数、完整 MoveTo、支撑
保护、水源原子技能与整田技能定向测试全部 PASS。

全新 `run-debug160` 在 Forge 65.1.0、真实 dedicated GameTest 服务端和真实 Headless
`ServerPlayer` 上 1/1 PASS（GameTest 约 1.551 分钟，Gradle 任务约 1分57秒）。它从第一人称全景
证据生成现场 3x3/8 格方案，普通步行到每个工作位，实际挖一格并放置一个水源，耕作八格、消耗
八颗种子并生成八株小麦，随后重新全景复核；fixture 还核验水桶变空桶、铲/锄耐久和恰好一次
`begin_mining`、一次 `use_item`、十六次 `use_on_block`。这只是无模型、无真人客户端的 inner-loop
施工证据，不是 M3 专业生电玩家或极限通关通过。

本轮主要改动文件：`skills/farming/PrepareWaterSourceSkill.java`、
`BuildHydratedCropFieldSkill.java`、对应两个测试、`skills/core/MoveToParameters.java` 和
`CoreSkillParametersTest.java`；上一检查点所列整田、亮度、异步规划和 GameTest 文件继续属于同一
未提交变更。最后实际失败门禁 `run-debug159` 已由 `run-debug160` 定向通过取代；正式
Actor/Observer/真实模型、长期作物生产率和 M0--M4 仍 `NOT_RUN`，工作树仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步只扩展该机制的长期生产验收：让普通随机刻推动至少一株
作物生长，并验证水合/光照在一段真实时间内保持；通过后再继续下一类参数化农场机制，不先跑
无关完整回归。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.04 whole-field executor (open)

M3 水合农田已从“规划器＋两个单格原子动作”推进到生产注册的整块任务
`build_hydrated_crop_field`。模型参数只有维度、wheat/carrot/potato/beetroot、8--80 格规模与
单区块约束，不能提交坐标或方块数组。执行器使用已有第一人称 `ShelterFrameSource` 做 24 视角
滚动勘测，将不可变的有界证据提交给容量为一执行、一排队的后台机制规划服务；服务端 Tick 只
轮询结果。生成后按远离服务通道到靠近通道的蛇形顺序，逐项走到现场约束得到的相邻工作位，
重新转头取得当前可见上表面，再委托普通 `prepare_water_source` 或
`prepare_and_plant_plot`，最终等待并重新全景观察水源和全部作物。动作前仍重新核验身体会话、
维度、危险、观察 revision、触及与目标状态；失败不会被伪装为完成。

旧规划中的 `skyVisible` 没有可靠生产来源，直接使用会让夜间或室内场地猜测天光。当前生产
`VisibleBlockFace` 已增加相邻可见单元原版亮度（未知为 -1），只在真实命中面旁读取，不使用
高度图、区块扫描或隐藏方块；中央农田规划现在接受已观察到的作物格亮度 >=9，未知或不足均
失败关闭，同时保留旧测试的正向天光证据兼容。语义 JSON 升级为格式 6，并只在已知时公开
`adjacentLight`。本地 Forge 65.1.0 注入源码确认 `CropBlock.randomTick` 的生长门槛确为
`getRawBrightness(pos, 0) >= 9`。

已改文件包括 `perception/VisibleBlockFace`、两个生产采样器与 JSON codec，`mechanism/` 的异步
规划服务和亮度约束，以及 `skills/farming/BuildHydratedCropField*`、注册、运行时生命周期和定向
测试。新增后首个失败门禁为 `CompanionRuntimePlannerGuideTest`：生产技能指南 13,129 字符超过
13,000 硬限制；已压缩新说明且未放宽边界。机制、感知、farming 与指南定向共 74 tests 当前
PASS。下一步不是跑无关全量回归，而是增加 release-excluded 3x3/8格真实 Forge GameTest，验证
真实身体全景勘测、后台规划、普通移动、水槽/水桶、8次耕种播种与最后第一人称复验；失败后只
按最先暴露的物理根因修复。

正式状态未改变：当前 Darwin 没有隔离 Linux/Xvfb、真实 Actor/Observer 客户端和合规可用模型
环境，M0--M4 继续 `NOT_RUN`；当前工作树仍是 `DIRTY_NO_COMMIT`/`NON_RELEASE`，以上均为
unit/inner-loop 证据，不是专业陪玩或随机种子通关声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.03 physical water primitive (open)

M3 参数化农田的第二条生产原子动作 `prepare_water_source` 已实现并注册。它绑定 AI 自己的
第一人称方块面、世界维度、观察序号和身体会话，拒绝下界、侧面目标、自身脚下支撑、缺少水桶、
过期/替换身体；施工时经普通挖掘技能移除一格泥土，重新观察坑底、正常切换水桶并沿原版
`use_item` 路径放水。完成必须同时看到目标格的水方块且自身 `water_bucket -1`、`bucket +1`；
只发出动作、只改变背包或只看到水均不能完成。失败状态现在稳定保留首个具体原因，不再被下一
tick 的泛化 `invalid_state` 覆盖。参数解析、注册、失败关闭及生产规划指南长度定向测试通过。

真实物理门禁保留了完整失败到修复证据。`run-debug152` 暴露失败原因被后续 tick 覆盖；修复诊断
后 `run-debug153` 明确为 `timed_out_revealing_bottom`。`run-debug154` 仍复现该问题，随后发现测试
工作位离坑两格，挖开后相邻地面遮挡坑底；改为合法的相邻玩家工作位后，`run-debug155` 进一步
暴露 `water_not_confirmed`：坑底已由公平射线看到，但 `use_on_block` 不是水桶正常放水语义。
参考仓库内已经通过物理门禁的落地水/浇筑路径，改用原版 `use_item`，并在装备后等待新语义帧
确认主手确为水桶。全新 `run-debug156` 1/1 PASS（约 794.4 ms），验证真实水源、坑底石块未破坏、
石铲耐久恰好 +1、水桶恰好变为空桶、且审计恰有一次 `begin_mining` 和一次 `use_item`。
`run-debug157` 又独立复跑耕地播种 1/1 PASS（约 528.3 ms），未出现回归。

这些仍是 release-excluded、无模型、无真人客户端的 inner-loop Forge GameTest，不能提升正式
M0--M4 状态。当前 Darwin 仍缺少隔离 Linux/Xvfb、真实 Actor/Observer 客户端和合规可用模型
环境，正式门禁继续 `NOT_RUN`，工作树继续 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步是把每帧
最多 24 个可见方块面的限制纳入设计：实现有界、按身体会话/维度隔离的滚动现场勘测证据，再让
现场生成规划器消费这些证据；随后才能用普通移动 + 水源 + 耕种原子技能执行整块农田，而不是
让单帧测试伪造 80 个同时可见表面。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.02 parameterized crop mechanism (open)

物理门禁已开始形成失败到修复的闭环。`run-debug150` 在进入场景前真实崩溃：生产
`CORE_SKILL_GUIDE` 因新增技能达到 13,359 字符，超过 13,000 硬边界。没有放宽 Token/上下文
限制；压缩重复 farming 指南后，`CompanionRuntimePlannerGuideTest` 与 farming 定向测试通过。
全新隔离进程 `run-debug151` 随后 1/1 PASS（约 640.3 ms）：真实 headless `ServerPlayer` 经
生产 `ServerOwnedCoreSkillActuator`/`ServerOwnedInteractionSkillActuator` 发出两次普通方块使用，
将泥土变为耕地、让石锄耐久恰好 +1、种子恰好 -1，并生成 0 龄小麦；测试预置的相邻水源保持
存在。测试类、环境和实例资源均被 release JAR 排除。该证据仍仅为无模型、无真人客户端的
inner-loop 物理门禁，不能提升 M3 或陪玩声明。

当前 M3 缺口不是语言模型能否描述农场，而是计划尚未拥有可验收的普通玩家动作链。已新增
`MechanismSpec`/`MechanismPlan` 及坐标无关的 `HydratedCropFieldPlanner`：它只使用 AI 第一人称
保留的当前地面、通行和背包证据，按现场尺寸、朝向、作物、障碍与单区块约束生成中央水源农田
施工 DAG，不保存绝对坐标蓝图。规划器现会拒绝侧面射线冒充地面、过期导航证据和无上表面证据，
平移后的相同现场会得到整体平移而非世界坐标相关布局；定向规划测试 9/9 通过。

为避免再次出现“模型说会种地但身体不做”，已把第一条施工原子动作
`prepare_and_plant_plot` 注册到生产技能表。它绑定具体观察序号与朝上的可见方块，重新核验当前
泥土/草方块/耕地、触及距离、会话和身体实例，随后只经普通背包槽位事务、转头与原版
`useOnBlock` 依次装备锄、耕地、装备种子、播种；只有新观察看到幼苗或自身种子数量下降才允许
完成。缺少锄/种子、视野过期、方块变化、确认超时或身体替换均失败关闭。对应 farming/planner
JVM 定向测试已通过。

已改文件集中在 `mechanism/`、`skills/farming/` 及其测试；最后失败的正式门禁仍未变化：当前
Darwin 没有隔离 Linux/Xvfb、真实 Actor/Observer 客户端和合规可用模型环境，正式 M0--M4
继续 `NOT_RUN`，源码为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。上述 JVM 结果不能作为陪玩或生存
能力通过。下一步立即增加 release-excluded Forge GameTest，让真实 `AiServerPlayer` 在物理世界
中实际损耗锄、把泥土变成耕地、消耗种子并生成小麦；只先跑这一条定向门禁。通过后再把单格
原子技能接入整块农田施工/验收调度。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.01 immediate bound follow (open)

物理集成门禁第一次运行已明确失败，不能计入通过：`run-debug143` 中
`mcai_companion:real_player_chat_to_immediate_bound_follow` 在 5000 tick 超时，GameTest 汇总为
1 required test failed。SQLite 审计只有 `conversation_model_not_ready`，最终 goal revision 2 为
`SAFE_IDLE/PLAYER_CHAT`，没有 `direct_player_skill_started`。根因不是身体寻路，而是该无模型
fixture 在聊天协调器的“模型尚未验证”入口即被诚实拒绝，命令从未安装；它因此尚未测到即时跟随
执行层。下一步只修测试边界：给 release-excluded fixture 临时安装一个会记录任何调用的 holding
gateway，使聊天入口处于“已验证”状态，同时断言即时路径对它的请求数始终为零；产品仍保持无
API/未验证模型时不接受聊天动作。随后重跑这一条物理门禁，再决定是否存在真实移动缺陷。

修正测试前置后，`run-debug144` 曾最终通过但 SQLite 暴露一次
`tick_budget_exceeded` 后自动重启；门禁因此被收紧为 FOLLOW 阶段逐 tick 保持同一运行中技能。
`run-debug145` 严格通过，但独立重复的 `run-debug146` 明确失败：第 52 个执行 tick、身体路径仍为
0 时，导航样本已有 2090 个体素，连续三次超出 2 ms 后 `follow_entity` 进入 FAILED。根因现收敛
到 `MoveToSkill` 每次规划重试都重新复制整份 bounded-memory snapshot，而内层 A* 的 2 ms 上限
又与外层整个技能 tick 的 2 ms 上限相同；内层设计的 8 次有界延迟尚未到达，外层已在第 3 次
终止。最后失败门禁是 `run-debug146` 1/1 FAIL，不得引用 `run-debug145` 作为稳定通过。下一步缓存
同 revision/start 的不可变规划记忆快照，并缩小 arrival-goal 枚举开销；保持 2 ms 产品门槛与严格
连续性断言不变，然后执行独立重复门禁。

该缺陷现已修复并完成定向收敛。`MoveToSkill` 只复用完全相同的不可变 navigation source 与脚下
格对应的安全融合快照；新观察对象或移动到新格立即重建。`LocalAStarPlanner` 的到达候选改为在
精确 arrival cube 和已观测 key set 之间选择较小集合，所有候选仍逐个执行原 `insideArrivalRegion`
与 `isValidGoal` 公平/安全校验。未提高 `SkillRuntimePolicy` 的 2 ms 上限。导航、移动、跟随和监督器
定向 JVM 59/59 通过；完整 JVM 1016 cases、0 failures、0 errors、2 skipped。

修复后的严格物理门禁在 `run-debug147`、`run-debug148`、`run-debug149` 三个独立 Java/Forge
进程中连续 1/1 PASS（分别 2.335、2.728、3.340 秒），每次都要求普通 Forge 聊天、服务器绑定
玩家身份、单次 `follow_entity`、连续技能所有权、两段追随、body path >= 7、单 tick <= 0.9，且
holding gateway 请求数为零；SQLite 无 `skill_failed`、`tick_budget_exceeded` 或模型审计。
完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 与兼容检查通过，mutation 在
`2026-08-09T11:03:40Z` 捕获 10/10。新 fat/slim SHA-256 为
`e760f4387c3079f49f216402c22c5d9f2ad78990f76f843210eff6fec51d1696` /
`a04217873ddd8d2078c1a397f969a7b24d9182fcf3f6f56be6776d699c50326e`；精确 Forge 65.1.0
专服 smoke `e2e/results/no-commit-dirty/20260809T110358Z-e2eaf8f76907f1a` PASS、加载哈希一致、
server exit 0、SQLite 仅 Jar-in-Jar、生命周期正常。该证据仍为 inner-loop/lifecycle，
`functionalAiClaim=false`；真实 Actor/Observer/模型与正式 M0--M4 仍 `NOT_RUN`。

根因：明确的玩家聊天“跟我/跟我走”虽然已经在服务端绑定发话者姓名与 UUID，但旧执行顺序仍先
发起高层模型请求；只有模型返回无技能决策后才恢复 `follow_entity`。因此慢请求、失效端点或模型
答非所问仍会直接表现成“答应但站着不动”。现在 `BrainOrchestrator` 在没有活动技能与请求时，
只针对 `PLAYER_CHAT` 来源、由服务端绑定了发话者身份的跟随目标，使用 AI 自己当前的公平语义样本
立即启动正常类型化 `follow_entity`。目标不在当前视野时只允许一次 `survey_surroundings`，之后交还
普通模型规划，避免无限转头。该路径不携带坐标、不追踪遮挡目标、不传送，仍经过
`SkillSupervisor` 和原版移动；审计明确写成直接玩家指令，绝不伪造 provider trace。改动文件为
`BrainOrchestrator.java` 与 `BrainOrchestratorTest.java`。

定向 JVM 契约 58/58 通过，覆盖可见玩家零模型往返启动、不可见玩家只搜索一次、原模型无动作/
澄清恢复路径继续有效。完整 `check verifyReleaseJar e2eClientJar e2eOracleJar`（Forge 65.1.0）与
兼容检查通过；当前 mutation 在 `2026-08-09T10:37:46Z` 仍捕获 10/10。唯一产品 fat JAR
SHA-256 为 `dc8f9b33af8e20f7fefb03bb27dc1bf5092417beae6c6c306a06c140db9c1beb`，审计 slim JAR 为
`9e9807fb927bc5a90fdb24bcbdf2a1c8571b70ca53c9e902c7c9eebfdedba182`。精确专服 smoke 归档
`e2e/results/no-commit-dirty/20260809T103752Z-e2e340cf381816e` 为 PASS、加载哈希完全一致，
但范围仍仅为生命周期且 `functionalAiClaim=false`。

最后正式失败/未运行门禁未改变：真实 Actor/Observer 客户端与合规模型环境在当前 Darwin 上
不可用，M0--M4 继续 `NOT_RUN`，源码继续 `DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步先给这条
即时跟随补真实 headless `ServerPlayer` 的物理位移集成场景，再继续 M3 参数化机制规格与施工/
验收链；不得把无模型 JVM/GameTest 写成正式陪玩通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v91.00 bounded no-action escalation (open)

根因：虽然短确认和动作承诺不再错误进入 `WAITING_FOR_PLAYER`，一般 `CONTINUE`/`REPLAN`
仍可无限返回而不启动任何技能；这会继续消耗模型请求，且在锁定 Hardcore 评测中形成无声站桩。
`BrainOrchestrator` 现在只给这种无技能决策三次有界重规划机会。第四次时，普通
`PLAYER_CHAT` 目标进入可由下一条真实玩家消息唤醒的 `WAITING_FOR_PLAYER`；MCP/恢复目标与
外部写入锁定评测则以 `SAFE_IDLE/planner_no_action_exhausted` 终止，绝不请求人工干预或虚构动作。
`MinecraftBrainEventSink` 对普通聊天目标额外广播一次诚实的“尚未找到可执行动作，请把目标说得
更具体”状态，且不在评测锁定时广播。改动文件：
`BrainOrchestrator.java`、`MinecraftBrainEventSink.java` 和
`BrainOrchestratorTest.java`。

定向 JVM 回归验证了：四次无动作后不会继续花费请求；下一条玩家消息可恢复规划；锁定 Hardcore
不等待人工而保留安全终止审计。随后 `check verifyReleaseJar e2eClientJar e2eOracleJar`
（Forge 65.1.0）、兼容检查与当前源 mutation 门禁均通过；mutation 在
`2026-08-09T10:24:32Z` 仍 10/10 捕获。精确专服 smoke 归档
`e2e/results/no-commit-dirty/20260809T102438Z-e2e519d82e232c1` 为 PASS，唯一产品 fat JAR
SHA-256 为 `9c8892bcd5cba1dca7b5bfefbe5373e33a4ca2c6bb05a1a852dca52a291ad1c0`，审计 slim JAR
为 `eb7e864092065fa311708cb41e53b8c0627976b841dee72228e425b792457aac`。该 smoke 仅证明
ServerPlayer/Jar 生命周期，`functionalAiClaim=false`。

最后失败/未运行门禁仍是正式真实客户端模型路径：`e2eChat` 预检归档
`20260809T092655Z-recoverychat20260809`，当前 Darwin 缺少隔离 Linux/Xvfb 与模型环境；尚未
启动客户端或读取密钥。下一步先审计并实现 M3 参数化机制规格与施工/验收链，同时在授权
Linux/Xvfb + 合规有效模型环境到位后重跑 `python3 e2e/formal_gates.py --gate e2eChat
--forge-version 65.1.0`。正式 M0--M4 继续 `NOT_RUN`，源码继续 `DIRTY_NO_COMMIT`/
`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.99 formal chat gate preflight (open)

按持久目标再次真实调用 `python3 e2e/formal_gates.py --gate e2eChat --forge-version
65.1.0 --nonce RECOVERYCHAT20260809`。它在启动任何客户端、触碰物理显示或读取密钥前
诚实返回 `NOT_RUN`：归档
`e2e/results/no-commit-dirty/20260809T092655Z-recoverychat20260809` 的
`functional-preflight.json` 标明当前 Darwin 缺少隔离 Linux/Xvfb、`MCAI_BASE_URL`、
`MCAI_MODEL` 和凭据；`physicalDisplayUntouched=true`。这次没有发送模型请求，也没有将
无模型/无客户端结果写成聊天到动作通过。当前源和最新产品哈希没有变化，后续恢复命令仍是
在授权 Linux/Xvfb worker 注入有效模型环境后重跑同一 gate。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.98 no-action status visibility + fresh package/smoke (open)

在上一条短确认防线之上，`MinecraftBrainEventSink` 现在对同一目标 revision 的
`planner_no_action_backoff` 最多向玩家广播一次诚实状态：`[AI] 名称：我还没有选定可执行动作，正在重新规划。`
它不伪造技能启动、移动或完成结果，并且评测锁定时不广播，避免污染正式评测。字段只在服务端
事件消费线程使用，目标完成或 revision 变化后自然允许下一次状态。定向
`BrainOrchestratorTest`、`PlayerTaskIntentTest`、`ConversationCommitmentSourceContractTest`、
`RuntimeActionTraceTest` 及完整 `check` 均通过。

本次新源重新打包后的 fat JAR SHA-256 为
`9ab54de35550d5606c77626487ebf6dec0807a9881b9795672edbb4274c12c13`，审计 slim JAR 为
`1b52c936abec92701f992612133e1304c299d7f2b0fa24ab285b1a4ea48f1b73`。当前源 mutation 门禁归档
`20260809T092022Z` 仍 10/10 捕获；Forge 65.1.0 专服 smoke 归档
`20260809T092026Z-e2e6c00a8c279d5` 为 PASS，精确加载同一 fat JAR，SQLite 仅来自产品
Jar-in-Jar，`functionalAiClaim=false`。兼容检查、JSON 校验和秘密扫描也通过。

这仍不是正式产品门禁：真实模型、真人客户端、Actor/Observer、随机 Hardcore 和 M0--M4
统计继续 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。当前 macOS 真实客户端预检
仍因无隔离 Linux/Xvfb 且没有模型环境而 `ready=false`，没有读取或发送用户密钥。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.97 bare-acknowledgement no-action guard (open)

修复了一个会把模型的短确认误判成“需要玩家继续回答”的停滞路径：规划器在玩家任务仍为
`RUNNING` 时返回 `ASK_PLAYER` + `好的`、`收到`、`OK` 等无问句短语，之前会进入
`WAITING_FOR_PLAYER`，身体既没有技能租约也不会继续请求规划。现在这些短确认与既有的
“我这就来/I'm on my way”承诺一样，只会触发有界重规划，抑制未被技能接受的语音，不宣称
已经移动；新增 `BrainOrchestratorTest` 和 `PlayerTaskIntentTest` 均通过。改动文件为
`BrainOrchestrator.java`、`PlayerTaskIntent.java` 及对应测试。

当前最后门禁仍不是正式产品门禁：本轮只完成定向 JVM 测试；真实模型、真人客户端、Actor/
Observer、随机 Hardcore 和 M0--M4 统计继续 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`/
`NON_RELEASE`。下一步先运行完整 `check`/打包与当前源 mutation/smoke，确认这条防线未破坏
动作、生命周期和发布边界，再继续处理真实环境缺失导致的验证阻断。

本轮完整包门禁通过，当前 fat JAR SHA-256 为
`e2f3e04522cc39f8d562629186c36fc5b2fcdbecca7d3ac63dfbf1a5dbc10b99`，审计 slim JAR 为
`1e4f36268306de897b9fb6bd46bbba0eb6502f66fd7d597e756e7e8ece412de1`；mutation
`20260809T090612Z` 仍 10/10 捕获，Forge 65.1.0 专服 smoke
`20260809T090616Z-e2e52778ef17299` 为 PASS（仅生命周期，`functionalAiClaim=false`）。

Updated: 2026-08-09 (Asia/Tokyo)

最新真实客户端预检 `2026-08-09T09:08:39Z` 仍为 `ready=false`：当前 Darwin 缺少隔离
Linux/Xvfb，且没有从环境读取模型端点、模型名或凭据；预检未启动客户端、未触碰物理显示、
未读取或发送密钥。真实 Actor/Observer、模型聊天到动作和 M0--M4 继续 `NOT_RUN`。

## 2026-08-09 continuation checkpoint: v90.96 current-source mutation gate (open)

软期限修复后的当前脏源码重新运行了故障注入门禁：归档时间
`20260809T085848Z`，10/10 变体均被捕获，包括聊天丢失、只说不做、技能启动空操作、
移动空操作/瞬移、菜单空操作、直接写背包、虚假完成和数据包泄漏。证据分类为
`DETERMINISTIC_FAULT_INJECTION`、`releaseEligibleSource=false`、`functionalAiClaim=false`；
它只证明测试能拒绝坏实现，不替代真实客户端、真实模型或 M0--M4。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.95 model soft-deadline wiring (open)

修复了配置声明存在但没有生效的模型等待边界：`model.softTimeoutSeconds` 现在同时接入
BrainOrchestrator 和聊天 lane。规划请求首次跨软期限只记录一次
`model_request_soft_deadline` 审计事件，不取消、不重试、不产生动作；聊天请求首次跨软期限
只向原发消息玩家发送一次“仍在处理”的明确进度。软期限与硬期限做跨字段保护：当用户把硬
期限设得低于软期限时，服务端记录不含密钥的警告并使用 `hard - 1` 秒，避免启动构造
`BrainPolicy` 失败。`CompanionConfigTest` 三个边界用例和 `BrainOrchestratorTest` 软期限
单次通知/不取消用例通过。

当前源码随后通过 `check verifyReleaseJar e2eClientJar e2eOracleJar`、兼容检查、JVM 测试、
Python E2E `20/20`、JSON 校验和秘密扫描；当前可安装 fat JAR SHA-256 为
`dfe417b7dfb4dc90055891b92e15208fcf61d657844f5cbcc40bb12317905d94`，审计 slim JAR 为
`307c0ff901aa8e0d338cc252e3a6a2fca0f2fe66d8a7b51ab18aaa760843f9c8`。精确 Forge 65.1.0
专服 smoke 归档 `20260809T085339Z-e2e6ce26d860d9f` 为 `PASS`，三份产品副本哈希一致，
但 `functionalAiClaim=false`，仍只证明 JAR 生命周期。真实模型/Actor/Observer、PVP、
正式 M0--M4 和随机 Hardcore 统计继续 `NOT_RUN`；源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.94 external functional preflight (open)

最新预检（2026-08-09T08:41:27Z）仍诚实返回 `ready=false`：宿主为 Darwin，缺少隔离
Linux/Xvfb，也没有 `MCAI_BASE_URL`、`MCAI_MODEL` 和模型凭据。预检没有启动客户端、没有
触碰物理显示、没有读取或发送任何 API Key。因此真实 Actor/Observer 聊天到动作、模型
移动/背包以及 M0--M4 仍是 `NOT_RUN`，本轮的服务端/物理/打包 PASS 不会替代它们。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.93 packaging regression guard (open)

`verifyReleaseJar` 现在还会检查 `build/libs` 中 `mcai_companion-*.jar`（排除 slim）必须
恰好只有当前 fat JAR；否则构建直接失败。旧产物处理已改为可恢复移动到
`build/archive-libs`，不会删除文件。随后当前源码的 `check verifyReleaseJar e2eClientJar
e2eOracleJar`、Forge 65.1.0 兼容检查、JVM 测试（0 failures/0 errors/2 skips）、Python
E2E `20/20`、JSON 校验和秘密扫描均通过。`build/libs` 当前仍只有
`mcai_companion-0.1.5-dev-mc26.2.jar`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.92 exact single-artifact smoke (open)

构建边界修复后的精确 Forge 65.1.0 专服 smoke 已完成：归档
`e2e/results/no-commit-dirty/20260809T083522Z-e2e17481252e31d` 的
`server-smoke-verdict.json` 为 `PASS`，server/actor/observer 三份安装副本都严格匹配
`f040fdc8c4e8a95292db74e1be6d25520260a56f29d4e5e3521f76acbcf857f4`，只加载一份
Jar-in-Jar SQLite，headless ServerPlayer 正常加入并优雅退出，未出现 mod loading failure。
这仍是精确 JAR 生命周期证据，`functionalAiClaim=false`；没有真人客户端画面、真实模型
请求或 M0--M4 统计门禁。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.91 single-installable-artifact boundary (open)

修复了用户曾遇到的“只加入一个模组却出现两个无关/重复模组”的构建层根因：Gradle 在升级
版本时不会自动移除 `build/libs` 中的旧产品 JAR，复制整个目录会把旧的可安装 fat JAR
和新 JAR 一起交给 Forge。现在 `jarJar` 先运行受限的 `archiveStaleProductJars`，把
`build/libs/mcai_companion-*.jar` 旧产物移动到可恢复的 `build/archive-libs`；开发 slim
JAR 改写到 `build/audit-libs` 且继续不含
`META-INF/mods.toml`。当前 `build/libs` 只剩一个可安装 JAR：
`mcai_companion-0.1.5-dev-mc26.2.jar`；`verifyReleaseJar` 通过，fat SHA-256 为
`f040fdc8c4e8a95292db74e1be6d25520260a56f29d4e5e3521f76acbcf857f4`，审计 slim SHA-256
为 `b1a3a3984084ecded7c8912215cf9df11d1088490517b7b3eb9df02ebf4aa96a`。

这只是发布/安装边界修复，不提升模型、真人客户端或 M0--M4 门禁；源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.90 hostile-contact retest (open)

针对“被僵尸打后站着不动”的本地安全路径，当前 Forge 65.1.0 源码在新隔离目录
`run-debug142` 通过 `real_emergency_zombie_skeleton_horde` 1/1（1.450 s）：十只僵尸和
十只骷髅对真实 headless `ServerPlayer` 施压，生产 20 TPS 紧急生存车道完成防御/移动，
测试服务器报告 required test passed。该场景没有模型网关，所以只能证明无 API 时的受击
自救不会把身体冻结，不能证明模型控制的 PVE/PVP、聊天动作链或随机 Hardcore 通关。

此前同一源码的落水自救 `run-debug140` 和跑酷 `run-debug141` 仍分别为 1/1；当前工作树仍
`DIRTY_NO_COMMIT`，真实模型/客户端预检仍被 macOS 无 Linux/Xvfb 和模型环境阻断。下一步
继续保留真实失败边界，优先做可独立验证的服务端/身体路径；不把无模型测试改写成陪玩完成。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.89 focused water-clutch and parkour gates (open)

当前定向问题已完成一个可核验闭环：`run-debug140` 在 Forge 65.1.0 当前源码通过
`real_water_clutch` 1/1（844 ms），日志记录生产紧急车道进入 `DEPLOYING_WATER` 并在坠落中
实际放水；`run-debug141` 通过 `real_parkour_course` 1/1（2.849 s），使用真实 headless
`ServerPlayer` 的普通物理移动完成跑酷。两项都没有模型网关，属于无模型本地自救/移动证据，
不能冒充实时 AI、PVP、真人客户端或 M0--M4 正式门禁。

当前根因与边界仍未改变：设置界面请求持久化时，平台安全存储不可用必须失败关闭，已修复为
`persistent_credential_store_unavailable`；没有 API Key 时身体仍可按本地安全车道出生、受击和
自救，但高层聊天、跟随、采集、建造和通关必须等待真实可用模型。MiMo 之前唯一真实探测为
HTTP 401，本轮没有重试或发送凭据。下一步继续只做有证据的定向实现/验证，并保持正式 M0--M4
和真实客户端预检为 `NOT_RUN`，直到合规 Linux/Xvfb、真实模型与真实客户端基础设施可用。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.88 persistent credential fail-closed (open)

修正了一个会直接造成用户现场“本次验证成功、重新进入世界又要求输入 API Key”的配置缺口。
设置界面一直请求重启安全存储，但 `ApiKeyManager` 在平台安全存储不可用或保存失败时会返回
`process_only_secure_store_unavailable`，`ModelRuntime` 仍把端点和进程密钥安装为成功配置。
现在只要用户界面请求持久化而安全存储没有实际落盘，配置事务会返回
`persistent_credential_store_unavailable`，清掉新缓存的进程密钥，不安装新端点，也不再伪装成
“已保存”。无桌面安全存储的服务器应使用 `MCAI_API_KEY`/`MCAI_API_KEY_FILE` 注入；正常
macOS Keychain、Windows DPAPI、Linux Secret Service 成功时仍可跨重启恢复。

新增跨实例/失败存储回归：`ApiKeyManagerRestartPersistenceTest` 的持久化成功恢复和不可用
安全存储拒绝路径均通过；随后完整 `check`、工件校验、兼容性检查和 Python E2E 均通过。
Forge 65.1.0 的 `zero_human_dedicated_server_chunk_and_respawn`（`run-debug138`）和
`auto_presence_on_human_login`（`run-debug139`）也各 1/1 通过。此前真实 MiMo 探测仍只有
一次 HTTP 401，本轮没有重试或读取/输出用户凭据；真实客户端/模型和 M0--M4 仍是 NOT_RUN。
最新 `e2e/orchestrator.py preflight`（2026-08-09T08:23:38Z）仍诚实返回
`ready=false`：当前 macOS 没有 Linux/Xvfb 和模型环境变量，未启动客户端、未触碰物理显示、
未读取或发送任何密钥。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.87 recent-damage isolation retest (open)

上一轮 `run-debug131` 在集中隔离入口后的生命周期复测暴露了真实运行时 p95 门禁超限（约
2.030 ms）；`run-debug132` 与 `run-debug133` 又在末地水晶后的安全等待阶段以合法的
`RECENT_DAMAGE_EVENT` 失败，说明不能把一次成功重跑包装成确定性修复。当前仅在
GameTest 夹具中加入了受限诊断日志（世界 tick、语义帧 tick、伤害/无敌状态），并把测试
等待上限从 80 调整为 100 tick；没有清除生产伤害信号、没有放宽生产技能安全前置，也没有
改变生产自动重生策略。

Forge 65.1.0 当前源码定向复测：`run-debug134` 和 `run-debug135` 的
`headless_player_lifecycle_state_and_fair_action` 均 1/1 通过（约 27.51/27.90 秒），
两次都完成真实 headless `ServerPlayer` 的连接、菜单/物理动作、下界与末地往返、末影龙
阶段和清理；运行时滚动 p95 分别约 1.397/1.414 ms。`run-debug132`/`133` 仍保留为失败
边界，不能删除或改写成通过。

随后 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar --no-daemon` 通过，
JVM 测试为 1005（0 failures、0 errors、2 skipped），兼容检查通过，Python E2E
`20/20`；fat/slim 工件哈希仍为
`ef06e0ec5f6dc48a062920cc7b604eb1286f1bfd54a28f9c6a3a9a17214d95c9` /
`a05d9cff2f448fa14d127fd3b6136b69ef0c11b37fc3d4b1be5d3336152227d5`。这些是无模型的真实
服务端/物理和构建证据，不是实时模型、真人客户端、PVP、M0--M4 或随机 Hardcore 门禁。

另外，测试专用的 live-model 场景入口现在也复用同一隔离清理边界，避免共享
GameTestServer 在失败场景后把旧 body、动作租约或紧急车道带入下一场；该类仍被发布 JAR
排除，未改变生产启动/重生行为。

Forge 65.1.0 离线本地战斗复核也通过：`run-debug136` 的十僵尸＋十骷髅压力场景 1/1
（约 1.874 秒），`run-debug137` 的铁傀儡单挑 1/1（约 1.618 秒）。两者均由真实
headless `ServerPlayer` 触发生产 20 TPS 紧急车道，包含实际移动、装备和近战；这是无模型
PVE/自救物理证据，不是 PVP、真实模型或 M0--M4 正式门禁。

源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`；MiMo Token Plan 之前的 HTTP 401 仍是唯一一次
真实探测，本轮没有重试或暴露凭据。正式下一步是继续降低批量 GameTest 的共享状态/性能
抖动，并在合规的 Linux Xvfb + 真实模型环境可用后运行真实 Actor/Observer 门禁；在此之前
不宣称专业陪玩或两小时通关完成。

本轮重新运行真实客户端预检（未启动客户端、未触碰物理显示、未读取或发送任何密钥）：
当前宿主是 macOS，缺少 Linux/Xvfb 与 `MCAI_BASE_URL`、`MCAI_MODEL`、模型凭据，预检明确
返回 `ready=false`/退出码 2。因此真实 Actor/Observer 和模型聊天到动作仍是外部基础设施
阻断，不被任何无模型 GameTest 结果替代。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.86 isolated lifecycle cleanup (open)

补上了仅供 GameTest 使用的 `GameTestCompanionSpawn.resetForIsolatedFixture`，在普通批次
夹具开始前终止遗留目标、停止动作车道、释放技能监督器并移除旧 headless body；生产 JAR
不包含该测试类，也没有改变生产自动重生策略。`zero_human_dedicated_server_chunk_and_respawn`
只在普通 selector 路径调用它，`mcai.zeroHumanAutoSpawnTest=true` 的生产启动断言保持不触碰
初始 body。

Forge 65.1.0 定向结果：`run-debug128` 普通零人路径 1/1（约 6.184 秒），
`run-debug129` 真正生产零人自动出生路径 1/1（约 6.230 秒），
`run-debug130` `headless_player_lifecycle_state_and_fair_action` 1/1（约 36.04 秒）。
后者此前批量中出现的 `fight_ender_dragon.session_mismatch` 在新目录中未复现；日志还记录了
真实传送、下界/末地、掉落收集、紧急落水/护甲和末影龙阶段。它们均为无模型的真实服务端
headless `ServerPlayer` 物理/生命周期证据，不是模型聊天到动作、真人客户端、PVP 或 M0--M4
正式门禁。

此前 `run-debug127` 的 Forge 65.1.0 默认全批次在约 90 分钟后因严重停滞被安全中断（退出码
130），已保留日志；中断前观察到零人测试的 `already_active` 和生命周期的旧会话不匹配，
不能记为通过。完整批次仍未重新运行，当前证据只支持“隔离测试修复后定向通过”，不支持
63/63 全批次通过。

源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`；没有新的模型请求，M0--M4、真实客户端和随机
Hardcore 统计门禁继续 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.85 full Forge batch diagnostic (open)

在 `localGeometry` 当前源码上用 Forge 65.1.0 做了完整默认 GameTest 批次（`run-debug121`）。
服务端确实跑完了 63 个测试，但进程以退出码 4 结束，4 个必需测试失败：

* `headless_player_lifecycle_state_and_fair_action`：`return_via_verified_portal.timeout`，
  tick 3248，停在 `RETURNING_TO_NETHER_PORTAL`；
* `real_nether_blaze_material_reserve`：tick 4010 超过窗口；
* `shelter_material_wood_exploration`：tick 0 要求 isolated body；
* `roof_jump_placement`：tick 0 要求 isolated body。

这次批次不能记为 63/63 通过，也不能用历史 38/38 内循环记录替代它。前两个技能超时和后两个
初始隔离断言在独立的新目录重跑后分别通过：`run-debug122` 1/1（约 39.06 秒）、
`run-debug123` 1/1（约 4.151 秒）、`run-debug124` 1/1（约 21.67 秒）、
`run-debug125` 1/1（约 31.35 秒）。因此目前证据指向批次内状态/清理与负载顺序诊断，尚未证明
`localGeometry` 有确定性回归；这也不等于产品门禁通过。下一步保持测试隔离问题显式可见，检查
GameTest 失败后的 headless session 清理和批量超时预算；在修复前不把批量结果包装成 PASS。

本轮仍没有真实模型请求、真实观察客户端、PVP 统计或隐藏随机种子；M0--M4 正式门禁仍为
`NOT_RUN`，源码为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.84 Forge 65.1 combat + parkour retests (open)

在加入有界 `localGeometry` 后，Forge 65.1.0 当前源码再次通过三个定向真实服务端
物理场景：`real_parkour_course` 在 `run-debug117` 通过 1/1（约 4.247 秒），
`real_emergency_iron_golem_duel` 在 `run-debug118` 通过 1/1（约 1.817 秒），
`real_emergency_zombie_skeleton_horde` 在 `run-debug119` 通过 1/1（约 1.970 秒）。
它们使用真实 headless `ServerPlayer`、原版碰撞/攻击/装备/移动和生产紧急生存车道，
覆盖用户点名的铁傀儡单挑及十僵尸十小白压力场景；仍然没有模型请求、真人客户端或
PVP 统计，因此不能冒充真实 AI 对战、视觉判断或 M0--M4 正式门禁。测试结束时连接
正常清理；Forge 测试夹具偶发的关闭通道发送 WARN 不影响必需测试结果，也未观察到
生产 tick 失败。

当前生产 fat/slim 工件仍为本轮 `localGeometry` 构建的 SHA-256
`ef06e0ec5f6dc48a062920cc7b604eb1286f1bfd54a28f9c6a3a9a17214d95c` /
`a05d9cff2f448fa14d127fd3b6136b69ef0c11b37fc3d4b1be5d3336152227d5`；源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步仍是优先完善可由真实客户端验证的端到端
聊天/动作链，并在合规的真实模型和客户端环境可用后重新运行正式门禁；不会把这些
无模型 GameTest 结果升级成专业陪玩或两小时速通承诺。

Updated: 2026-08-09 (Asia/Tokyo)

补做 `auto_presence_on_human_login`（`run-debug120`）后，测试在没有模型凭据的情况下仍
让 `TestHuman` 与 `MCAI` 在同一服务器相邻出生，1/1 通过（约 1.018 秒）；这验证了
“无 API 也创建可见身体、API 只影响说话和高层动作”的生命周期约束以及公开身份路径。
它仍不是渲染客户端 TAB 截图，也不证明真实模型可用。

## 2026-08-09 continuation checkpoint: v90.83 bounded localGeometry perception (open)

为改善模型对峡谷、墙面、上/下方表面和狭窄空间的理解，语义感知 JSON 现在加入
`localGeometry`：它只从本次有限第一人称射线已经命中的表面、清晰射线片段和预算计数
派生 `surfaceHits`、面方向计数、相对高度范围、最近表面距离和有限形状提示，并明确
标注“缺失不能证明开放空间”。该改动参考 Numen 公开的自我中心语义表征原则，但未复制
其代码、资源或实现；没有邻居扫描、隐藏方块读取、强制加载或坐标定位。

`SemanticObservationJsonCodecTest` 定向通过；随后 Temurin JDK 25 下完整 `check`、
`verifyReleaseJar`、客户端/Oracle 工件和兼容检查通过，JVM `1005`（0 failures、0 errors、
2 skipped），Python E2E `20/20`。Forge 65.1.0 无真人专服 `run-debug116` 仍通过
`zero_human_dedicated_server_chunk_and_respawn` 1/1（约 2.100 秒），证明新增感知字段不
破坏身体生命周期；新生产 fat/slim SHA-256 为
`ef06e0ec5f6dc48a062920cc7b604eb1286f1bfd54a28f9c6a3a9a17214d95c9` /
`a05d9cff2f448fa14d127fd3b6136b69ef0c11b37fc3d4b1be5d3336152227d5`。

这仍不是模型视觉、真实客户端聊天到动作、PVP 或 M0--M4 正式门禁；源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`，MiMo Token Plan 请求没有重试。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.82 Forge 65.1 no-human + Observer task audit (open)

当前源码在 Forge 65.1.0 隔离 `zero_human_dedicated_server_chunk_and_respawn` 通过
1/1（`run-debug115`，约 1.890 秒）：没有真人客户端时，生产 runtime 自动创建真实
headless `ServerPlayer`，完成模拟区块窗口、死亡/原版重生和断开清理。该证据不含模型，
不等于客户端聊天到动作或 M0--M4 通过。

同时用 Temurin JDK 25 对真实客户端任务做了配置级审计：
`runE2eObserverClient`、`runE2eInstalledObserverClient`、`e2eClientJar` 和
`e2eOracleJar` 的 Gradle 配置 `--dry-run` 通过，未启动窗口或发送供应商请求；随后
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过。当前生产 fat/slim 工件哈希仍为
`a69fe0438e1764fad6a3551e00cc079a1e0b2dce01ed39cf475bf9744138a766` /
`85373c5cfd6dc130b2fb093057e8cb895f0a15f9b2bc4034bfcb19cbde9ffac6`。

这轮没有修改生产逻辑、没有重试 MiMo Token Plan 请求；正式真实客户端、真实模型、
Hardcore 随机种子和 M0--M4 仍为 `NOT_RUN`，源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.81 evidence seal (open)

本轮文档与状态封存前的定向审计通过：`docs/progress/GOAL_STATE.json` 可解析，
`scripts/validate-compat.py` 通过，Python E2E `20/20`，长 Token 扫描无匹配；本轮
没有生成新生产 JAR，也没有发送新的供应商请求。所有正式模型/客户端/M0--M4 门禁仍按
当前外部条件保留 `NOT_RUN`，不是从无模型物理测试推断出来的 PASS。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.80 Forge 65.1 travel-detour retest (open)

Forge 65.1.0 隔离 `real_travel_diagonal_detour` 在当前源码通过 1/1（`run-debug114`，约
3.839 秒）。真实 headless `ServerPlayer` 完成对角障碍绕行和路线恢复；该证据仍是无模型
物理/导航回归，不能代表真实聊天、视觉、复杂地形或 M1--M4 随机种子统计。源码保持
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.79 Forge 65.1 water-clutch retest (open)

用户此前询问的落地水已在 Forge 65.1.0 隔离 `real_water_clutch` 通过 1/1
（`run-debug113`，约 797 ms）。日志显示真实 headless `ServerPlayer` 触发
`PREPARING_WATER`/`DEPLOYING_WATER`/`BRACING_FALL` 紧急车道并完成场景；这是本地
20 TPS 无模型物理证据，不代表模型会在自然随机世界中可靠识别每次悬崖，也不提升
聊天到动作、视觉、PVP 或 M1--M4 正式门禁。源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.78 post-parkour package gate (open)

跑酷回归后的当前源码再次通过 Temurin JDK 25 下的 `compat-checker`、`check` 和
`verifyReleaseJar`（`BUILD SUCCESSFUL`，测试/产物均为增量未变）。Forge 65 发布补丁仍仅
声明 65.0.0--65.1.0，正式矩阵、真实客户端/模型和 M0--M4 继续 `NOT_RUN`；源码保持
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.77 Forge 65.1 parkour retest (open)

当前源码在 Forge 65.1.0 的隔离 `real_parkour_course` 通过 1/1（`run-debug112`，约
3.095 秒）。该场景由真实 headless `ServerPlayer` 经过原版碰撞、跳跃和落差移动并完成
清理；它是无模型的物理回归证据，不等同于真实客户端视觉、模型聊天到动作、PVP、陪玩
或 M1--M4 正式门禁。源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，此前官方 MiMo Token
Plan 限制仍使用户提供的 `tp-` 凭据不可继续用于自动化游戏控制；没有发送新的供应商请求。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.76 post-research code gate (open)

供应商条款研究后的当前源码快速收尾门禁通过：`compat-checker`、`check` 和
`verifyReleaseJar` 在 Temurin JDK 25 下 `BUILD SUCCESSFUL`（测试与工件保持上一轮
`1005/0/0/2`、Python E2E `20/20`）。本轮只更新研究/状态文档与跨补丁证据，不改变
生产 JAR；源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，真实模型请求不再重发。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.75 provider-use boundary (blocked)

重新核对 MiMo 官方 Token Plan 文档后确认：`token-plan-cn.xiaomimimo.com/v1`
和 `tp-` 凭据格式本身正确，`api-key`/Bearer 鉴权均有官方说明；但 Token Plan
订阅条款把用途限定在编程工具，并禁止用于自动化脚本或自定义应用后端。持续驱动
Minecraft 的模型网关不应继续消耗该 Token Plan 凭据，除非用户取得供应商明确授权。
此前真实探测的唯一请求仍为 HTTP 401，故没有再次发送请求；正式模型/M0--M4 门禁继续
`NOT_RUN`。恢复条件是提供允许此用途的 API 凭据或供应商书面授权，然后按真实客户端
命令重跑，不把本地 GameTest 提升成模型证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.73 offline-fixture package retest (open)

离线装备夹具隔离改动后的当前源码重新完成包门禁：固定 Temurin JDK 25 下 JVM
`1005`（0 failures、0 errors、2 skipped），`check`、`verifyReleaseJar`、客户端/Oracle
工件任务和 Python E2E `20/20` 均通过；`build/libs` 仍为 `0.1.5-dev-mc26.2`。
这次只改 GameTest 夹具隔离，不改变生产 fat JAR，因此生产 JAR 仍以 v90.66 的精确
SHA 和 smoke 归档为准。模型、真实客户端、Hardcore 随机种子和 M0--M4 仍是
`NOT_RUN`；此前 MiMo 实测唯一请求为 HTTP 401，不能伪称聊天到动作或专业陪玩通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.74 Forge 65.1 lifecycle retest (open)

当前源码在 Forge 65.1.0 的隔离 `headless_player_lifecycle_state_and_fair_action`
通过 1/1（`run-debug111`，约 26.59 秒）。该链实际覆盖 headless ServerPlayer 加入/重登、
原版菜单事务、移动/跳跃、落水自救、浇筑并进入下界门、下界材料、末地/强hold链、返回门
和关闭清理；运行时 20 TPS 样本平均约 `0.283 ms`、滚动 p95 约 `1.420 ms`。
日志中的 portal/drop 诊断 WARN 来自 release-excluded GameTest 观测夹具，测试结果为
`All 1 required tests passed`，不提升为真实模型、客户端视觉、PVP 或 M0--M4 证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.72 isolated Nether acquisition (open)

`real_nether_blaze_rod_acquisition` 在单独 Forge 65.0.0 运行 `run-debug110` 通过 1/1
（约 2.044 秒），完成真实下界实体战斗、掉落和拾取交接。历史批次中的
`already_active` 是共享 GameTestServer 的 AI 单例/并发夹具冲突，不是该生产技能的
独立失败；批次 OOM 与并发污染仍保留，不改写成全批通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.71 offline fixture isolation (open)

离线装备夹具的批次污染也已隔离：前一个聚焦场景留下 HoldingGameTestGateway，令
`offline_idle_equipment` 的“无模型”断言误报。夹具现在在断言前清除 verified delegate，
保持生产网关和安全回退不变；`run-debug109` Forge 65.0.0 通过 1/1（约 709 ms），
仍通过原版菜单装备铁头盔和盾牌。历史批次失败仍保留，不把夹具修复当模型能力证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.70 post-fixture gate (open)

强hold安全等待修复后的完整 JVM/package 门禁再次通过：`test`/`check`/
`verifyReleaseJar`、兼容检查和客户端/Oracle 工件通过，JVM 1005（0 failures、0 errors、
2 skipped），Python E2E 20/20。生产 fat JAR 未因这次仅测试夹具改动而改变，仍以
`v90.66` 的精确 SHA 和 smoke 归档为准；正式 M0--M4、真实模型和客户端继续 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.69 stronghold safety-cue fixture fix (open)

历史批次中的 `headless_player_lifecycle_state_and_fair_action` 单独复现为
`run-debug107` 失败：不是路径或强hold理解错误，而是前一阶段 End 水晶留下的真实
短时 recent-damage cue，生产 `search_stronghold_portal_room` 正确返回
`danger_detected`。测试夹具原来只在第一次 Eye 投掷前等待该 cue，却直接启动传送门
房搜索。已在夹具中加入同一公平语义的有界自然等待，不清除危险状态、不放宽生产技能。

修复后 `run-debug108` Forge 65.0.0 通过 1/1（约 27.42 秒），完成整条 headless
生命周期/菜单/移动/下界/末地/强hold/返回门链，运行时指标样本 5835、平均约 0.271 ms、
滚动 p95 约 1.427 ms。`run-debug107` 保留为修复前失败证据；这是无模型的受控 Forge
链，不提升 M0--M4、自然种子、真实聊天到动作或专业陪玩声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.68 Forge 65.1.0 combat retest (open)

当前源码在 Forge 65.1.0 的两个隔离真实 PVE 场景通过：
`run-debug105` `real_emergency_iron_golem_duel` 1/1（约 1.147 秒），
`run-debug106` `real_emergency_zombie_skeleton_horde` 1/1（10 僵尸＋10 骷髅，约 1.312 秒）。
这证明服务端原版身体的受击、移动/攻击和紧急生存车道在该补丁版本仍可工作；无模型、无
自然种子，不提升 PVP、聊天到动作或 M0--M4 门禁。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.67 Forge 65.1.0 roof compatibility (open)

当前屋顶修复在 Forge 65.1.0 继续通过：`run-debug104` 选择
`roof_jump_placement` 1/1（约 24.05 秒）。这扩展了当前源码的两个 65.x 定向物理
证据（65.0.0 的 `run-debug103` 与 65.1.0 的本次运行），不等同于完整 65.x 正式矩阵，
也不声明 Forge 66 已适配。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.66 post-roof package and exact smoke (open)

屋顶修复后的当前源码已完成完整工件门禁：固定 Temurin JDK 25 下 JVM `1005`（0 failures、
0 errors、2 skipped），`check`、`verifyReleaseJar`、客户端/Oracle 工件和 Python E2E
`20/20` 均通过。当前 0.1.5 fat/slim SHA-256 为
`a69fe0438e1764fad6a3551e00cc079a1e0b2dce01ed39cf475bf9744138a766` /
`85373c5cfd6dc130b2fb093057e8cb895f0a15f9b2bc4034bfcb19cbde9ffac6`。

精确产品 Forge 65.0.0 专服 smoke 归档
`20260809T050723Z-e2ead06345411b8`：生产 JAR、服务端/Actor/Observer 副本哈希一致，
Forge 启动、Jar-in-Jar SQLite、headless ServerPlayer 加入和优雅退出均通过。Oracle
因本次没有模型凭据按设计停止，`functionalAiClaim=false`；这不是聊天到动作通过。
源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，M0--M4、真实客户端、有效 MiMo（此前唯一一次
HTTP 401）和隐藏 Hardcore 统计继续 `NOT_RUN`。

下一步继续隔离真实 Forge 物理/动作审计；不把无模型 GameTest、生命周期 smoke 或已知
批次 OOM 说成专业陪玩或两小时速通证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.65 roof-jump clean retest (open)

移除临时诊断日志后重新执行 `run-debug103`：Forge 65.0.0
`roof_jump_placement` 1/1（约 24.22 秒），真实服务端身体完成屋顶跳跃/放置、外侧
观察和保护边界校验。`run-debug102` 的首次修复通过也保留；`run-debug96`--`101`
仍作为修复前失败证据。当前待做的是重新打包和产品级 smoke，尚未改变 M0--M4 或真实
模型门禁状态。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.64 roof-jump traversal fix (open)

当前源码的隔离屋顶跑酷门禁已在 Forge 65.0.0 修复并通过：`run-debug102`
`roof_jump_placement` 1/1（约 23.68 秒）。根因是顶面支撑候选只从半径 2--3
开始，紧凑庇护所外墙 apron 恰好只有一格宽，导致所有合法第一人称跳跃站位落在
工作带之外；同时观察站位和实际瞄准历史曾共用集合，造成已观察站位不能重新作为
合法瞄准点。当前修复分离两类历史，并让顶面候选从半径 1 开始；候选仍必须通过
公平安全站位、跳跃净空、观察射线、计划通行约束和原版触及距离检查。

`run-debug96`--`run-debug101` 的失败证据仍保留，未改写为通过；本轮临时候选诊断日志
已移除。该门禁仍是无模型的真实 Forge 物理证据，不是陪玩、真实聊天到动作或 M1--M4
统计证据。源码待重新打包，当前仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`；M0--M4、真实客户端、
MiMo（此前唯一一次 HTTP 401）和隐藏 Hardcore 矩阵继续 `NOT_RUN`。

下一步：用移除诊断后的当前源码重跑屋顶门禁，再执行 JVM/package、Python E2E 和精确
Forge 65.0.0 fat-JAR smoke，记录新工件哈希；随后继续按隔离场景审计，不把已知批次 OOM
或无模型测试提升为专业陪玩声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.63 zero-human and login-presence lifecycle retest (open)

当前源码继续通过两个专服生命周期场景（Forge 65.0.0）：

- `run-debug94` `zero_human_dedicated_server_chunk_and_respawn` 1/1（约 1.738 秒）：没有
  真人客户端时自动创建可见 headless ServerPlayer，保持模拟区块窗口，按原版受击死亡并重生；
- `run-debug95` `auto_presence_on_human_login` 1/1（约 647.8 ms）：真人登录时 AI 同步出现、
  在玩家附近安全出生，并由 TAB/玩家列表公开显示身份。

这两项没有模型凭据，只证明身体和生命周期；不等同于真实聊天到动作、PVP、陪玩或速通。
`run-debug84` 的 63 项无过滤批次仍因原版结构生成 4 GiB heap OOM 失败，不能提升为批次通过。
当前源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`，真实 MiMo 仍只有一次 HTTP 401，M0--M4、
真实客户端和隐藏 Hardcore 统计继续 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.62 isolated physical matrix and batch OOM boundary (open)

当前源码在 Forge 65.0.0 的隔离、单场景 GameTest 继续通过：

- `run-debug85` `real_emergency_iron_golem_duel` 1/1（1.218 秒）；
- `run-debug86` `real_emergency_enderman_defense` 1/1（1.434 秒）；
- `run-debug87` `real_emergency_zombie_skeleton_horde` 1/1（10 僵尸＋10 骷髅，1.272 秒）；
- `run-debug88` `real_parkour_course` 1/1（2.738 秒）；
- `run-debug89` `offline_idle_equipment` 1/1（577.9 ms）；
- `run-debug90` `real_travel_diagonal_detour` 1/1（3.466 秒）；
- `run-debug91` `shelter_material_wood_exploration` 1/1（17.59 秒）；
- `run-debug92` `real_portal_cast_and_light` 1/1（1.395 秒）；
- `run-debug93` `real_water_clutch` 1/1（771.1 ms）。

重新启动的 63 项无过滤批次 `run-debug84` 没有形成批次通过证据：Forge GameTest 在连续
生成全部结构后于约 104 秒以 `OutOfMemoryError: Java heap space` 崩溃（4 GiB heap，栈在
原版 `StructureTemplate.loadPalette`），此前已经记录的测试单例冲突、离线配置污染和屋顶
失败也仍然有效。后续使用单场景隔离或串行小批次；不把 OOM、并发夹具或旧失败改写成产品
功能通过。

这些是无模型的原版身体/公平感知/技能物理证据，不是实时模型聊天到动作，也不提升 M0--M4、
隐藏 Hardcore 统计或 Forge 66；真实 MiMo 仍只有一次 HTTP 401。当前源码仍
`DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.61 current-source package and exact smoke (open)

修复后的当前源码已重新打包。固定 Temurin JDK 25 下 `test check verifyReleaseJar
e2eClientJar e2eOracleJar` 通过：JVM 1005（0 failures、0 errors、2 skipped），Python
E2E 20/20。最新生产 fat/slim JAR SHA-256 为
`32900a438282ae4ca343229736c3fb9d8e183a42a7c4ec818e7a290330d14c56` /
`11623bbfec5b6f3527eeb224dbc13288460e5e599656fee9787b059be581c4e8`。

当前产品 JAR 的 Forge 65.0.0 精确专服 smoke 归档为
`20260809T043610Z-e2e5dac10058c64`：生产 JAR、服务端加载副本、Actor 和 Observer
副本哈希全部一致，专服启动、SQLite Jar-in-Jar、headless ServerPlayer 加入和优雅
退出均通过；`functionalAiClaim=false`，因为本次没有模型凭据，不能把生命周期 smoke
写成聊天到动作通过。真实 MiMo 仍只有此前一次 HTTP 401；M0--M4、真实客户端、隐藏
Hardcore 统计和 Forge 66 继续未通过/未适配。

下一步继续处理 `run-debug74` 完整 63 项中仍存在的并发单例夹具、离线配置污染、屋顶
跳跃与战斗/传送失败；使用隔离、定向的真实 Forge 场景记录证据，不用修改测试去掩盖
失败。当前源码仍 `DIRTY_NO_COMMIT`/`NON_RELEASE`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.60 Blaze hand-off starvation fix and Forge retest (open)

本轮针对现场“模型已经选了战斗/采集但身体站住”的同类根因做了真实服务端诊断。最新
`run-debug77` 的 `real_nether_blaze_material_reserve` 不是烈焰人攻击算法失败：上一只
目标死亡并生成下一只目标时，资源子技能仍处于拾取交接阶段；20 TPS 生存监督器看到旧的
敌对帧后进入 `GUARDING`，技能车道再也没有机会处理新目标，最终执行计数停在 43。

最终保留的生产修复位于 `SecureNetherBlazeMaterialSkill` 与
`AcquireNetherBlazeRodSkill`：选择阶段允许无投射物且没有未授权敌对实体的有界车道交接，
并允许资源子技能在拾取交接期间处理一个远离近战距离的已授权 Blaze；投射物、其他敌对
实体和近身接触仍由紧急车道优先。仅用于诊断的 `EmbodimentGameTests` 仲裁日志和状态字段
已移除。`SecureNetherBlazeMaterialSkillTest` 定向 JVM 测试通过。

真实 Forge GameTest 证据：`run-debug81` Forge 65.0.0 `real_nether_blaze_material_reserve`
通过 1/1（约 2.599 秒），移除诊断日志后的 `run-debug82` 仍通过 1/1（约 3.624 秒）；
Forge 65.1.0 `run-debug83` 同一场景通过 1/1（约 7.656 秒）。这证明了连续击杀、普通掉落、
拾取和下一目标交接，仍不是模型聊天、自然种子或 M1--M4 门禁证据。完整 63 项并发批次的
`run-debug74` 仍记录 10 项失败（其中包含并发单例夹具、离线配置污染、屋顶跳跃和下界
材料等），不可写成整批通过；M0、M1、M2、M3、M4 与隐藏 Hardcore 统计门禁继续
`NOT_RUN`。Forge 65.1.0 通过不等于 Forge 66 已适配。

当前源码仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。此前 MiMo `mimo-v2.5` 真实请求只有一次
HTTP 401，未重复消耗或写入密钥；下一步是以当前源重新生成包/精确 smoke，并继续拆分完整
GameTest 的并发夹具问题与真实战斗/屋顶失败，不把无模型测试冒充专业陪玩。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.58 exploration route regression and fair Forge retest (open)

本轮先把现场导航缺陷写清楚再继续实现。前一轮 Forge 65.1.0/65.0.0 的真实
`shelter_material_wood_exploration` 在 tick 5003 确定性卡在探索子航段；根因是探索
航点沿着“稍近但偏航”的局部格子反复消耗长航段预算。最终保留的修复是：
`RollingTravelPlanner` 对目标推进候选显式要求目标方向投影不为负，探索组合使用短而有界的
`TravelSkillPolicy.explorationDefaults()`（900 总 tick、240 无进展 tick、12 次扫描），
并删除会把真实对角分支拉回基线的过强物理回程触发器。临时进度日志已全部移除；新增
`TravelSkillPolicyTest` 覆盖探索预算严格短于普通远程旅行预算。

证据按全新隔离 Forge 65.0.0 世界记录：`run-debug68` 的
`real_travel_diagonal_detour` 1/1（约 3.281 秒）、`run-debug69` 的
`shelter_material_wood_exploration` 1/1（约 17.38 秒）、`run-debug70` 的
`workstation_wood_prerequisite_composition` 1/1（约 5.788 秒），以及
`run-debug71` 的 `zero_human_dedicated_server_chunk_and_respawn` 1/1（约 1.739 秒）。
中间实现的 `run-debug65`/`run-debug66` 对角回归失败（`no_progress`）已由移除过强回程
触发器修复，不能被误写成通过；更早的 63 项批次仍是 57/63，六项失败也仍保留为历史
失败，不提升为整批通过。

本 checkpoint 对应源码状态仍为 `DIRTY_NO_COMMIT`/`NON_RELEASE`。真实 MiMo
`mimo-v2.5` 请求此前只得到一次 HTTP 401 `Please provide valid API Key`；没有模型的
GameTest 只能证明原版身体/物理与本地安全车道，不能替代真实聊天到动作。M0、M1、M2、M3、
M4 以及隐藏随机种子统计门禁继续 `NOT_RUN`。下一步是用当前源码重跑 JVM/package，生成
新工件后执行精确 fat-JAR 专服 smoke；拿到有效凭据和独立 Linux/Xvfb 客户端环境后，才
继续真实模型黑盒聊天/移动门禁。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.59 navigation-fixed package and exact smoke (open)

当前源码已完成导航修复后的完整包：固定 Temurin JDK 25 下 JVM 1005（0 failures、0 errors、
2 skipped），`check`、`verifyReleaseJar`、客户端/Oracle 工件和 Python E2E 20/20 通过。
产品 fat/slim SHA-256 为
`a3ef87d39f8c204b9b04ee3b414b4a057124af8fef0b31fe48dfa4303591fd8d` /
`3aaef97bb59d5b0463c24f70be3702d3a6d01774c9d2567f71a202a0b6580066`。
精确产品专服 smoke `20260809T041117Z-e2ee0a41b042bcf` 在 Forge 65.0.0 通过启动、
精确哈希、Jar-in-Jar SQLite、headless ServerPlayer 和优雅退出；oracle 因没有模型
凭据按设计失败，`functionalAiClaim=false`。

这只更新开发证据，不提升 M0--M4：真实 MiMo 之前已收到 HTTP 401，Linux/Xvfb 真实
客户端环境仍未就绪，Hardcore 隐藏种子矩阵继续 `NOT_RUN`。

随后用 Forge 65.1.0 当前发布补丁复核同一修复：`run-debug72` 的
`shelter_material_wood_exploration` 1/1（17.39 秒），`run-debug73` 的
`real_travel_diagonal_detour` 1/1（3.439 秒）。这扩大了当前源码的定向运行证据，
不是完整 65.x patch matrix，也不改变 Forge 66 未适配声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.57 immediate social lane, physical retest, and exact smoke (open)

当前现场问题的两个根因仍需分开看：模型链路不是“光说不做”的可验收证据，因为本轮真实
MiMo capability probe 已实际发出请求并收到 HTTP 401；同时没有模型时，原先普通闲聊会
等待高层规划，造成玩家看到长时间无回应。已补充低风险本地即时社交回复（Hello/Hi、
询问中文/英文），仍先经过模型可用性边界，未配置或认证失败时不伪造模型回复；任务、移动、
战斗和世界修改仍必须由真实模型/本地公平应急车道决定。跨实例凭据测试也确认第二个
`ApiKeyManager` 可从同一持久化凭据存储恢复密钥，不再依赖每次进世界重新输入。

本轮已改文件：`PlayerTaskIntent.java`、`CompanionConversationCoordinator.java` 及其
`PlayerTaskIntentTest.java`；并更新 `BuildInfo`/发布工件账本和本检查点。最后一次实际 Forge
65.0.0 物理回归 `run-debug58` 的 `real_emergency_zombie_skeleton_horde` 为 1/1 通过
（真实 10 僵尸＋10 骷髅、无模型本地生存车道）。最新精确 fat JAR 专服 smoke
`20260809T033729Z-e2eb8f364fe1c3d` 通过启动、精确哈希、Jar-in-Jar SQLite、headless
ServerPlayer 和优雅退出；`functionalAiClaim=false`，oracle 功能动作仍未声称通过。
最新产品/ slim JAR SHA-256：
`6b7c7d28bb87209d7d50443ea661f2e1f0f861fafba8bca0e05f62f078481a56` /
`3ea8c8c94d1282259a1d9d90875cdf125c78d5cc99860d394a4da121b72dd2ca`。

最后失败门禁：`run-live-movement-0.1.5b` 真实 MiMo 请求 HTTP 401（不是 mock、不是脚本
替代），所以真实客户端聊天到动作、M0、M1、M2、M3、M4 仍是 `NOT_RUN`；当前源码仍为
`DIRTY_NO_COMMIT`/`NON_RELEASE`。下一步不再重试已确认无效的密钥：等待有效凭据或替换
密钥后，重新运行同一个真实客户端/模型黑盒选择器；在此之前继续做不依赖外部凭据的公平
动作、重启、传输和故障审计，但不把这些提升为陪玩或速通产品声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.56 real-model movement probe (blocked, open)

按用户要求实际启动 Forge 65.0.0 GameTest，只选择
`real_player_task_to_live_model_movement`，没有使用 fixture/mock 模型。第一次隔离目录
缺少端点，先安全返回 `INVALID_CONFIGURATION`（requestsMade=0）；随后只注入非秘密的
`MCAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1` 与
`MCAI_MODEL=mimo-v2.5`，API Key 由 macOS Keychain 自动恢复。真实 capability probe
发出 1 个请求后收到 HTTP 401，服务端明确返回 `Please provide valid API Key`，因此
聊天到动作场景在模型请求前失败，不能把这次运行记成 PASS，也没有继续重试或消耗更多
请求。该结果同时证明当前版本确实走到了真实 MiMo 认证链路；需要用户在设置中替换为
有效 Key 后，才能继续真实聊天/移动及 M1--M4 黑盒门禁。工作树仍为
`DIRTY_NO_COMMIT`，所有正式门禁保持 `NOT_RUN`。

Run directories: `run-live-movement-0.1.5` (missing endpoint),
`run-live-movement-0.1.5b` (real probe HTTP 401).

随后新增的跨实例 `ApiKeyManagerRestartPersistenceTest` 通过；完整 Gradle 包为
1003 JVM tests（1003 total、0 failures、0 errors、2 skipped），`check`、
`verifyReleaseJar`、客户端/Oracle 工件、Python E2E `20/20` 和 Forge 兼容性校验均通过。
产品/ slim JAR 哈希未因测试代码变化而改变：
`37d3c6ee0976710b40f204b1fbe0d9b9618eca2f367461fb91f0ad5f0318c82f` /
`0fd56fdd31bceed8997e710b571f1a6ceaec2b3754d0d9e4fecb7ce6cc1b5226`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.55 emergency combat suite retest (open)

在 v90.54 的受击事实桥接后，Forge 65.0.0 当前源码用
`-Plive_model_selector='mcai_companion:real_emergency_*'` 跑了四个真实服务端场景：
铁傀儡、僵尸/骷髅、史莱姆和末影人，4/4 全部通过，GameTest 总耗时约 3.359 秒。
这组测试验证原版 `ServerPlayer` 的公平视角、盾牌/装备、反击或退避、末影人有限
重获和紧急车道仲裁；测试没有模型网关，因此不能提升为聊天到动作、PVP 或 M1--M4。
工作树仍为 `DIRTY_NO_COMMIT`，当前产品精确 smoke 与模型门禁状态保持前一条记录。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.54 trusted damage bridge and physical horde retest (open)

修复“模型/对话语义样本说没有僵尸，但身体刚刚确实受击”的事实延迟：
`ServerCoreSkillFrameSource` 现在向对话层只提供一个有时限的
`hasRecentThreatSignal` 布尔事实，来源仍是本体的公平 20 TPS 伤害/威胁帧；不暴露
攻击者 UUID、隐藏位置或区块信息。对话的 `trustedBodyStatus` 标记该事实，僵尸否定句
在当前语义样本落后一拍时也会被改成诚实的不确定/受击说明。生产运行时已接入该边界。

Forge 65.0.0 当前源码定向 GameTest `real_emergency_zombie_skeleton_horde` 在
`run-debug56` 通过 1/1（约 1.205 秒），测试实际创建 10 个僵尸和 10 个骷髅并由
无模型本地紧急车道完成防御；这不是模型聊天、PVP 或 M1--M4 证据。新增代码的
定向 `PlayerTaskIntent`/`BrainOrchestrator` JVM 测试仍通过。产品包尚未因本次改动重打，
随后已完成完整 JVM/package、精确 JAR smoke。当前产品 JAR SHA-256 为
`37d3c6ee0976710b40f204b1fbe0d9b9618eca2f367461fb91f0ad5f0318c82f`，slim 为
`0fd56fdd31bceed8997e710b571f1a6ceaec2b3754d0d9e4fecb7ce6cc1b5226`；JVM 为
`1002`（1000 passed、0 failures、0 errors、2 skipped），Python E2E `20/20`，兼容性与
发布 JAR 校验通过。精确 smoke `20260809T031954Z-e2effac8762a49f` 在 Forge 65.0.0
通过，且 `functionalAiClaim=false`、工作树 `DIRTY_NO_COMMIT`。真实模型/客户端门禁仍需
有效凭据和隔离环境，不能提升为通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.53 versioned 0.1.5 package and exact smoke (open)

本轮将开发工件推进到 `0.1.5-dev-mc26.2`，没有改写历史证据。产品 JAR
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` SHA-256 为
`c3d5e931f1fba9fb56168a4ca66fef5317e5f937ea2a28415dbf926ab4fd6796`，slim 工件为
`16b857c99dd775f2c7154ffcefdf7fddcede8e0ef810250655a1ff0ab0baeb2f`；固定 Temurin
JDK 25 下完整 Gradle 包含 `1002` 个 JVM 测试用例（1000 passed、0 failures、0 errors、
2 skipped），`check`、`verifyReleaseJar`、客户端/Oracle 工件和兼容性校验均通过，
Python E2E 为 `20/20`。旧 0.1.4 JAR 仍可留在本地构建目录，E2E 编排器现按当前
`build.gradle` 版本精确选择产品、客户端和 Oracle 工件，不会误用旧包。

精确产品 JAR 专服 smoke `20260809T030913Z-e2ed1e2e2db2264` 在 Forge 65.0.0
通过启动、精确哈希复制、SQLite Jar-in-Jar、headless ServerPlayer 加入与优雅退出；
`functionalAiClaim=false`，工作树仍为 `DIRTY_NO_COMMIT`。这轮没有模型凭据和 Linux/Xvfb，
所以真实客户端聊天到动作、PVP、Hardcore 隐藏种子及 M0--M4 继续保持 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.52 follow liveness and zero-human audit (open)

继续针对现场“跟我只得到口头回应、视野丢失后站着不动”的链路修复：中文短命令
`跟我`现在由服务端任务分类器直接绑定发话玩家；绑定玩家名比较改为大小写不敏感，
避免 `Alex` 被保存为 `alex` 后永远无法匹配。模型返回 CONTINUE、REPLAN 或
ASK_PLAYER 而绑定玩家暂时不在当前第一人称语义样本时，BrainOrchestrator 现在启动
`survey_surroundings` 的 8 向有界真实环视，重新获取新鲜公平样本；不追踪隐藏位置、
不注入坐标、不传送，重新看到玩家后仍由正常 `follow_entity`、SkillSupervisor、
局部 A* 与原版玩家输入执行。

本轮聚焦 JVM：PlayerTaskIntent/BrainOrchestrator 定向测试通过，新增“短命令、目标离开
视野、有大小写差异”回归。Forge 65.0.0 无真人服务器定向 GameTest
`zero_human_dedicated_server_chunk_and_respawn` 在 `run-debug55` 以 1/1、约 1.693 秒
通过；日志确认生产 runtime 自动创建真实 headless ServerPlayer，测试随后按设计杀死并
验证远区块/重生路径。macOS Keychain 用独立占位值完成 add/find/delete 控制探针，结果
通过，未读取或写入用户 API Key。

仍需明确：这些是源码、真实 Forge 身体/物理和本机密钥存储证据，不是模型聊天到动作、
真实客户端、PVP、随机 Hardcore 或 M1--M4 统计证据。当前 macOS preflight 仍缺
Linux/Xvfb、模型环境和有效凭据，故所有真实模型/客户端/M0--M4 门禁保持 `NOT_RUN`；
工作树仍为 `DIRTY_NO_COMMIT`。下一步继续审计模型可用性/客户端前置条件，并在具备真实
凭据和隔离客户端后运行同一黑盒门禁，不把本地 GameTest 提升为产品声明。

## 2026-08-09 continuation checkpoint: v90.51 preflight/evidence boundary (open)

定向审计继续确认：当前源码已实现双启动生命周期审计、生产连接队列审计、Xaero
授权坐标的动作边界和变异门禁；最近在 macOS 主机执行 `e2e/orchestrator.py
preflight --forge-version 65.1.0`，结果严格为 `ready=false`，缺少 Linux/Xvfb、
`MCAI_BASE_URL`、`MCAI_MODEL` 和有效模型凭据。因此真实模型/真实客户端聊天到动作、
M1--M4 仍是 `NOT_RUN`，不能把 GameTest 或本地脚本结果提升为通过。

本次仅更新形式门禁文案并完成定向核验，未改变产品 JAR 的作弊边界。下一步仍是：
在具备 Linux+Xvfb+真实模型凭据后运行同一 `functional` 黑盒归档；若条件仍缺失，
继续完善不依赖外部条件的动作/故障审计，并保持 `NOT_RUN`。

## 2026-08-09 continuation checkpoint: v90.50 real two-boot restart archive (open)

新增 `restart-smoke` 真实运行器在同一隔离世界连续启动/停止两次，加载相同精确 fat
JAR；归档 `20260809T022742Z-two-boot-audit` 的生产 SQLite 实际有四条生命周期行
（started→stopping→started→stopping），两次启动共享 companion UUID，SavedData
goalRevision 均为0且 schema 版本为3。修正相对 SQLite URI 后
`python3 e2e/restart_gate.py` 返回 `PASS`；通过 `formal_gates.py --gate e2eRestart`
在当前 `DIRTY_NO_COMMIT` 源上记录为 `NOT_RUN`，原因是
`restart_archive_passed_on_non_release_source`。这是真实双启动持久化证据，但不是
模型、双客户端或 M0--M4 通过。

## 2026-08-09 continuation checkpoint: v90.49 restart evidence boundary (open)

`CompanionRuntime` 现在把启动/停止时的稳定 UUID、SavedData 目标修订号、目标状态/来源、
身体是否曾出生、Hardcore/评测锁、数据库 schema 版本写入无正文的
`runtime_lifecycle_audit`。新增 `e2e/restart_gate.py` 只读验证真实归档：必须有同一
SQLite 中两次有序启动和停止行、稳定 UUID及单调修订号；单次专服 smoke 被明确拒绝。
`formal_gates.py --gate e2eRestart` 没有 `MCAI_RESTART_RUN_ROOT` 时返回
`NOT_RUN/restart_archive_required`，避免把一次生命周期测试冒充重启通过。

新建包包含该事件；精确 JAR smoke `20260809T022349Z-lifecycle-audit-smoke` 在 Forge
65.0.0 的启动/停止生命周期 PASS，重启 verifier 正确以缺少第二次启动行而 FAIL。
Python E2E 19/19，仍没有真实双启动归档、模型客户端或 M0--M4正式通过。

## 2026-08-09 continuation checkpoint: v90.48 Xaero coordinate action boundary (open)

针对“收到共享坐标后只回复、不走”的实际链路，规划输入现在识别服务端已经持久化
的 Xaero 授权坐标目标：同维度且当前帧安全时明确要求模型立即选择
`START_SKILL move_to` 并复制目标的 dimension/x/y/z；跨维度只有已验证传送门边才
允许 `travel_to`，否则请求重规划。该 playbook 不执行传送、命令、小地图读取或
模型外部坐标推断，坐标目标仍必须由模型返回结构化技能决定。

新增 `MinecraftPlannerInputFactoryTest.xaeroWaypointGoalReceivesImmediateCoordinateActionPlaybook`
通过；当前 Forge 包门禁、transport smoke 和正式状态将在本轮定向修改后重新生成。

## 2026-08-09 continuation checkpoint: v90.47 production transport audit (open)

将包队列门禁从合成 JSON 合约提升为生产证据：`HeadlessConnectionPump` 现在记录出站
队列高水位、未释放包数、keepalive/传送/区块批确认及断开处理状态；运行中的身体按
低频写入 `connection_transport_audit`，服务器停止时由 `AiPlayerManager` 在真实
vanilla disconnect 回调之后写入最终快照。外部 `e2e/orchestrator.py` 读取同一 SQLite
事件并拒绝缺失、未释放或未正确断开的链路；`PACKET_LEAK` mutation 现在直接篡改该
生产事件验证，而不是另造旁路文件。

定向结果：Forge `compileJava test`、完整 `check verifyReleaseJar e2eClientJar
e2eOracleJar` 通过；Python E2E `16/16`，mutation `10/10`。当前 fat/slim SHA-256
为 `4b1ab93f9f9c0f38fcedd0f577de2daab5517e74c996cf2c18f06b1cbe75b37b` /
`d43d2c2c846ac22392f9fe30ccf55d075f1e4695616e4efaf1014f0b29b8580e`。工作树仍为
`DIRTY_NO_COMMIT`；没有 Linux/Xvfb 或有效模型环境，本轮不提升真实客户端、模型、
Hardcore 或 M0--M4 门禁。

随后精确 fat JAR 专服 smoke `20260809T021444Z-transport-audit-smoke` 在 Forge
65.0.0 通过：产品哈希加载一致、Jar-in-Jar SQLite 与优雅生命周期通过；数据库中
实际写入运行中和最终断开两条 `connection_transport_audit`，最终快照为
`disconnectHandled=true`、`unreleasedOutboundPackets=0`。该 smoke 仍标记
`functionalAiClaim=false`。

## 2026-08-09 continuation checkpoint: v90.46 inventory provenance and mutation gate (open)

发现并修复外部库存门禁的完整性缺口：Oracle 现在只在 Forge 原版
`PlayerEvent.ItemPickupEvent` 对准确的测试掉落实体触发后，才允许库存目标通过；单纯
增加背包数量或删除掉落实体不再足够。`e2e/orchestrator.py` 要求拾取事件及累计数量，
`test_orchestrator.py` 新增直接写背包故障回归。

新增 `e2e/mutation_gate.py` 并接入 `formal_gates.py` 的真实 `mutationGate` 入口；
聊天丢失、AI 回复丢失、只说不做、技能空操作、移动无动作、瞬移、菜单无效、直接写
背包、虚假完成和包队列泄漏 10/10 均被捕获。该门禁是确定性故障注入证据，不冒充
真实模型或 M0--M4。当前 Python E2E 16/16；固定 JDK25 下
`check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，fat/slim SHA-256 仍为
`824b56125e734958e03aaed090167e667a6fe6037432ad4f379243302d7fed61` /
`303f6ff1f8dda125de76358bef086c301cd2f908a0fccf09cf533da56326cd79`，Oracle 测试
JAR 更新为 `97ee9280a2b160b7082d3b993601a646ac030e8c62665facef63937f32809dae`，
且产品 JAR 不含测试客户端/Oracle 类。

## 2026-08-09 continuation checkpoint: v90.45 focused post-audit (open)

账本格式与兼容声明再次通过：`python3 -m json.tool docs/progress/GOAL_STATE.json`、
`python3 scripts/validate-compat.py` 和 Python E2E 16/16；固定 Temurin JDK 25 下
`./gradlew verifyReleaseJar --no-daemon` 通过，当前 fat/slim SHA-256 仍为
`824b56125e734958e03aaed090167e667a6fe6037432ad4f379243302d7fed61` /
`303f6ff1f8dda125de76358bef086c301cd2f908a0fccf09cf533da56326cd79`。通信、凭据恢复、
单人免 `@` 与承诺链定向 JVM 测试通过；这只是当前源码和工件审计，不提升任何模型、
客户端、Hardcore 或 M0--M4 门禁。

## 2026-08-09 continuation checkpoint: v90.44 chat task-authority guard (open)

继续针对“模型把聊天误报成目标、然后身体停住”的现场症状收紧服务端边界：
`PlayerTaskIntent` 新增会话问题/问候识别；单人普通消息仍无需 `@`，但多人未明确
寻址的消息不能创建目标。即使模型错误地把“Could you speak Chinese?”、“Hello”或
“What do you mean by speak Chinese?” 返回成 `ASK_PLAYER`，只要它不是本地高置信
游戏祈使句，运行时会抑制目标写入并保留会话路径；非疑问的陌生建造描述仍可由
模型接管。该修复不选择技能、不写世界，也不替代有效模型。

变更文件：`PlayerTaskIntent.java`、`CompanionConversationCoordinator.java`、
`PlayerTaskIntentTest.java`、`ConversationCommitmentSourceContractTest.java`。
定向聊天测试通过。随后完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过：
JVM 998/0/0/2、Python E2E 15/15；fat/slim SHA-256 为
`824b56125e734958e03aaed090167e667a6fe6037432ad4f379243302d7fed61` 与
`303f6ff1f8dda125de76358bef086c301cd2f908a0fccf09cf533da56326cd79`，客户端/Oracle
仍为 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` /
`bbb03590de866bf1ba4de0cdd18fd21a282ec19b79b8070a2c10861463a515fc`。精确 JAR
专服 smoke `20260809T013915Z-e2e9503d501c9cc` 的加载、headless 加入、SQLite、
精确哈希、优雅退出通过；Oracle 因无有效模型仍为
`FAIL/server_stopped_before_result`，`functionalAiClaim=false`。

通信边界改动后的物理回归也通过：`offline_idle_equipment` 在 `run-debug49`、
Forge 65.0.0 以 1/1、约610.3 ms 再次完成铁帽/盾牌原版菜单穿戴；
`real_emergency_zombie_skeleton_horde` 在 `run-debug50` 以 1/1、约1.203 s
再次完成十僵尸十骷髅压力场景，身体移动、存活并造成伤害。这些仍是无模型受控
本地证据，不是模型聊天、真人 PVP 或 M0--M4 证据。

本轮还复核了公开的 Dwinovo/minecraft-numen 架构说明：其可借鉴点是把真玩家身体、
受限感知、原版动作和“动作结果反馈回模型”拆成独立层，并用 Skill/MCP 扩展能力；
本仓库已有同等方向的 `ServerPlayer`、公平语义观察、技能监督器、失败反馈和适配器
边界。没有复制其源码、资源或作弊通道；该复核不会把对方 README 的能力宣称当成本
仓库实测证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.43 offline equipment physical gate (open)

无模型装备车道已从源码契约提升为真实 Forge GameTest：隔离 `run-debug48` 使用
Forge 65.0.0、无模型网关，启动 headless `ServerPlayer` 后由测试夹具把铁帽和
盾牌放入其普通背包；`IdleEquipmentController` 通过原版 `inventoryMenu` 事务在
614.7 ms 内完成头盔与副手穿戴，1/1 required tests passed。该测试只证明已拥有
物品的本地维护，不证明模型聊天、移动、战斗策略或 M0--M4。

新增测试及资源：
`src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`、
`src/main/resources/data/mcai_companion/test_instance/offline_idle_equipment.json`、
`src/main/resources/data/mcai_companion/test_environment/exclusive_offline_idle_equipment.json`。

随后当前源码 `check verifyReleaseJar e2eClientJar e2eOracleJar` 通过：JVM 998/0/0/2、
Python E2E 15/15；fat/slim 产品 SHA-256 为
`9535d4d4d8cd21a5bf2f91fac501d63176d45998f37bdbfe1ffcc0fe82c495fc` 与
`e36984abe405aa08ca75eac7f78b23dbbf22a82e891f8ebd76f810aebf887522`，测试客户端与
Oracle 工件保持 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf` 与
`bbb03590de866bf1ba4de0cdd18fd21a282ec19b79b8070a2c10861463a515fc`。精确产品 JAR
专服 smoke `20260809T013113Z-e2efe9f19e6c9d4` 的启动、Oracle 加载、headless
加入、单一 Jar-in-Jar SQLite、精确哈希和优雅退出通过；Oracle 因本机没有有效模型
凭据仍为 `FAIL/server_stopped_before_result`，`functionalAiClaim=false`。工作树
仍是 `DIRTY_NO_COMMIT`，这些不是发布版或 M0--M4 证据。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.42 offline equipment lane (open)

现场“给了装备但身体不穿”的缺口继续收紧：没有已验证模型时仍禁止高层技能、移动
和聊天，但 `IdleEquipmentController` 现在作为低风险独立行为车道继续运行，通过
正常库存菜单事务装备已拥有的护甲升级、盾牌和更好的普通武器。它不创建目标、不
选择模型技能、不传送、不直接改库存。`ModelBootstrapSourceContractTest` 及完整
源码包通过；Forge 65.0.0 当前源码 `run-debug45` 的十僵尸十骷髅真实压力场景仍
1/1 通过（约1.225秒）。

最新 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` 为 JVM
998/0/0/2、Python E2E 15/15。当前 fat/slim 产品 JAR SHA-256 为
`9535d4d4d8cd21a5bf2f91fac501d63176d45998f37bdbfe1ffcc0fe82c495fc` 与
`e36984abe405aa08ca75eac7f78b23dbbf22a82e891f8ebd76f810aebf887522`。
精确 JAR 专服 smoke `20260809T012442Z-e2ee1bb30e5c8da` 的加载、AI
ServerPlayer 加入、SQLite Jar-in-Jar、哈希和优雅退出通过；独立 Oracle 因无有效
模型凭据仍为 `FAIL/server_stopped_before_result`，`functionalAiClaim=false`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.41 current-source package and exact-jar smoke (open)

当前工作树在单人聊天寻址修复后重新打包。普通单人聊天现在由服务端明确标记为
`addressedForServer=true`，因此不需要 `@` 或 Agent 名；专用多人服务器仍要求
显式地址，避免 Agent 抢答无关队伍聊天。变更文件为
`src/main/java/dev/mcai/companion/communication/ChatAddressing.java`、
`CommunicationModule.java` 及其定向测试。`ChatAddressingTest` 和通信相关定向测试通过。

当前 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` 通过：JVM
998/0/0/2，Python E2E 15/15。产品 JAR SHA-256 为 fat
`c2782fed27256521f5381306236cde2517b1a829d3fe41e3ffe5e43b7a7e654e`、slim
`10b263f1452ba1ee82c2c93a637a0d0aa5cda3dffe7fd3ccec9b39d36c6a83a7`；测试客户端和
Oracle 工件哈希保持 `d714b3d7e2a4744d5577e0073bb525e5a2452498662b0c98698983cde952adcf`
与 `bbb03590de866bf1ba4de0cdd18fd21a282ec19b79b8070a2c10861463a515fc`。

当前源码 Forge 65.0.0 `run-debug44` 的真实
`real_emergency_zombie_skeleton_horde` 通过 1/1（约1.194秒）；精确产品 JAR
专用服务器 smoke `20260809T011912Z-e2eae301135c205` 的生命周期、零真人身体、
SQLite Jar-in-Jar、哈希和优雅退出通过，但独立 Oracle 为
`FAIL/server_stopped_before_result`，`functionalAiClaim=false`。这仍是受控服务端
与生命周期证据，不是实时模型聊天、真人客户端、随机 Hardcore 或 M0--M4 通过。
当前正式预检仍受 Darwin、无 Xvfb/Linux worker 和无有效模型凭据阻断，不能把这些
结果升级成专业陪玩或两小时通关声明。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.39 action-promise ASK_PLAYER recovery (open)

继续复盘“模型说我这就来但身体不动”链路时发现，规划层对已经由玩家聊天创建的
目标返回无疑问的 `ASK_PLAYER` 行动承诺（如“我这就来”“I'm on my way”）会直接
进入 `WAITING_FOR_PLAYER`，即使没有任何需要玩家回答的问题。现在仅对
`GoalSource.PLAYER_CHAT` 且文本明确是行动承诺、没有问号的响应，压掉该话术，保留
原目标，设置 `planner_no_action` 纠正并按短退避重新请求模型的具体
`START_SKILL`；真实问题、Hardcore 锁定目标和普通不确定语句仍走原安全澄清路径。
该恢复不选择技能、不写世界、不生成坐标。

变更文件：`src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`、
`src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`。新增回归通过：
玩家聊天目标不会再被此类空头 ASK_PLAYER 锁死，且没有错误播报。正式模型、真人
客户端和 M0--M4 仍按 v90.37 保持 NOT_RUN。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.38 follow physical-progress watchdog (open)

源码审计发现 `FollowEntitySkill` 会把 `MoveToSkill` 的局部扫描/转身 tick 当作
“进度”。在通道受阻或路线未知时，身体可以持续转头而没有任何位移，监督器因而
不会触发停滞重规划。当前修复在跟随技能内记录身体第一人称位置：连续80 tick
（4秒）超过0.08格没有真实位移就安全停止并返回
`follow_entity.no_physical_progress`，由 `SkillSupervisor` 结束当前原子技能，随后
规划器重新取样/选择路线；搜索重新看到目标时会重置停滞计时。该看门狗不写坐标、
不传送、不读取隐藏实体位置。

变更文件：`src/main/java/dev/mcai/companion/skills/core/FollowEntitySkill.java`、
`src/test/java/dev/mcai/companion/skills/core/FollowEntitySkillTest.java`。
定向 FollowEntitySkillTest 已通过，新增回归证明固定身体位置的转身不能无限维持
跟随。当前工作树仍为 DIRTY_NO_COMMIT；完整包与 Forge GameTest 将在本次定向修复
后重跑，正式模型/真人客户端/M0--M4 仍受 v90.37 的 Linux/Xvfb 与有效凭据门禁
阻断，不能把该源码回归写成真实 AI 通关。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.37 formal preflight (strict external block)

最新 `python3 e2e/orchestrator.py preflight --forge-version 65.0.0` 于
`2026-08-09T01:00:41Z` 返回 `ready=false`：主机为 Darwin，缺少隔离 Linux/Xvfb
和模型环境（`MCAI_BASE_URL`、`MCAI_MODEL`、`MCAI_API_KEY`/文件）。预检没有
接触物理显示器、没有启动真人客户端、没有写入或记录密钥，也没有改变任何
正式门禁状态；因此 live Actor+Observer、聊天到动作、随机 Hardcore 与 M0--M4
保持 `NOT_RUN`。恢复条件是提供 Linux/Xvfb（或已配置的隔离 Linux worker）和
一次有效模型凭据，然后运行 `python3 e2e/formal_gates.py --gate e2eChat`
及其余正式门禁。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.36 current-source human-login presence (open)

在当前源码/Forge 65.0.0 隔离 `run-debug42` 重跑 `auto_presence_on_human_login`，
1/1 通过（597.6 ms）：普通真人测试玩家先登录，AI 身体随后通过正常
`PlayerList`/嵌入连接路径在安全邻近位置加入，TAB 身份标识和无模型静止约束
保持有效。该场景不请求模型，不证明聊天、移动、随机 Hardcore 或 M0--M4。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.35 current-source iron golem duel (open)

按用户指定的单挑场景，在当前源码/Forge 65.0.0 隔离 `run-debug41` 重跑
`real_emergency_iron_golem_duel`，1/1 通过（934.5 ms）。这是实体真实生成、
原版 ServerPlayer 身体、装备/移动/攻击与受控中立铁傀儡压力响应的回归；没有
模型请求、真人 PVP、随机种子或胜率统计，因此不能提升为完整 PVP/M0--M4 通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.34 current-source hostile pressure recheck (open)

在暂停菜单修复后的当前源码上，Forge 65.0.0 GameTest
`real_emergency_zombie_skeleton_horde` 于隔离 `run-debug40` 重新通过 1/1
（1.243 秒）。场景实际生成十只僵尸和十只骷髅，headless `ServerPlayer` 通过
原版连接/玩家列表路径加入、移动、存活并完成本地应急战斗。嵌入连接关闭时的
系统消息 fallback 仍是已知拆除警告，不是测试断言失败；该场景没有模型请求、
真人客户端、随机 Hardcore 或 PVP 胜率，不提升 M0--M4。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.33 pause-menu anchor and package recheck (open)

修复暂停菜单“AI 陪玩”覆盖“游戏菜单”的现场问题。原因是本地化后的“回到游戏”
按钮可能是已解析的 literal，旧代码只比较 translatable `Component`，找不到锚点
就使用固定 y=54；现在同时比较最终显示文本，并在仍找不到时从最上方全宽主按钮
推断同列位置，避免固定坐标覆盖标题。变更文件为
`src/main/java/dev/mcai/companion/client/modelsetup/ClientModelSetupRegistration.java`。

本次与 v90.32 的响应式设置页修复一起完成了完整
`./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar`，Python E2E 15/15，
设置/皮肤网络定向测试通过。最新 fat/slim JAR SHA-256 为
`e84b41a5fc26389069470937c6b5169f8cde441ca41049976b644998e69c9070` 与
`fddb88a5f944b23492753c25ae4f7ecec535ba0ed78b01e2ac8264e2ee260672`。
精确 Forge 65.0.0 smoke `20260809T005504Z-e2ef47399ffee9f` 的服务端启动、
零真人身体、Jar-in-Jar SQLite、精确哈希和优雅退出均通过；oracle 因无有效模型
凭据为 `FAIL/server_stopped_before_result`，`functionalAiClaim=false`。正式
真实模型、Linux/Xvfb Actor+Observer、随机 Hardcore、M0--M4 仍 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.32 responsive model setup layout (open)

修复了设置页在窄窗口上的实际布局断点：原实现把表单最小宽度固定为220像素，
同时保留146像素侧栏，导致字段、配色/温度控件以及保存/返回按钮越过屏幕或
互相覆盖。`ModelSetupScreen` 现在根据内容面板宽度计算侧栏和表单宽度，配色/
温度与保存/返回按钮使用可容纳的分栏尺寸，教程控件也随侧栏缩放；表单滚轮
区域扩展到整个左右内容面板，并在滚动前后夹紧偏移。没有伪造多Agent能力，
仍保持首发单Agent后端契约。

定向设置/皮肤网络测试通过；源改动后的完整
`./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，Python E2E
15/15 通过。新精确 fat/slim JAR SHA-256 分别为
`e1b37b4458c8c5c933d715bc1fd33db01cec18c682067db889e61dc60a4e97bf` 与
`be2ac128963361f51f82745c7d6a24360e9fada5635ab4e8b6399f3577b35087`。
Forge 65.0.0 精确 JAR 生命周期 smoke
`20260809T005023Z-e2edac5b8bfdf42` 通过加载、零真人身体、SQLite 和退出校验，
但独立 oracle 因没有有效模型凭据而为 `FAIL/server_stopped_before_result`，
`functionalAiClaim=false`。正式 Linux/Xvfb、有效模型、真人 Actor/Observer、
随机 Hardcore 与 M0--M4 仍 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.31 formal chat gate recheck (open)

重新调用 `python3 e2e/formal_gates.py --gate e2eChat --forge-version 65.0.0`
后，门禁在 `CONTINUEASKPLAYER` run 中按契约记录为 `NOT_RUN`：当前工作树无
提交、主机为 Darwin、无 Xvfb 且没有可用模型凭据。该归档没有启动真人客户端、
没有写入密钥，也没有将前面的精确服务器 smoke 或 GameTest 提升为聊天通过。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.30 hostile pressure recheck (open)

在 bound-follow `ASK_PLAYER` 恢复改动后，真实 Forge 65.0.0 GameTest
`real_emergency_zombie_skeleton_horde` 于隔离 `run-debug39` 重新运行并通过
1/1（1.249 秒）。场景仍是真实生成的十只僵尸和十只骷髅，身体通过原版
ServerPlayer 路径加入、移动、存活并完成生产应急战斗车道；关闭时嵌入连接的
fallback 警告仍是已知清理行为。该结果不含模型请求、真人客户端、随机种子或
PVP 胜率，M0--M4 继续 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.29 bound-follow ASK_PLAYER recovery (open)

真实编排器审计确认 `build_artifacts` 的所有调用点与两参数签名一致，未发现
之前 v90.28 所写的实际参数错误；该条只作为审计假设保留，不能当作失败证据。
本轮真正修复了现场“已答应但站着不动”的窄路径：对已由玩家聊天绑定的
`serverBoundPlayerName` 跟随目标，如果当前公平语义样本确实看见同名、非敌对
玩家，而模型返回 `ASK_PLAYER`，Brain 不再把它锁成 `WAITING_FOR_PLAYER`，而是
生成当前样本绑定的 `follow_entity` 决策，再经 SkillSupervisor、观察绑定、局部
A* 和原版玩家移动。普通 ASK_PLAYER、不可见目标、名称不匹配和损坏样本仍走原
来的澄清/安全路径。

变更文件：`src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`、
`src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`、
`docs/IMPLEMENTATION_STATUS.md`、`docs/progress/GOAL_STATE.json`。定向测试通过；
完整 `./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` 通过，JVM 为
996/0/0/2，Python E2E 为15/15。最新精确 Forge 65.0.0 smoke
`20260809T003432Z-e2e811774ddd5e0` 生命周期通过，fat SHA 为
`cf3475665f6b3baf4e7fd111969422e76b20adcc2836730b89a184a238628381`；同 run
独立 Oracle 因当前 MiMo 凭据 HTTP 401 而 `FAIL/server_stopped_before_result`，
`functionalAiClaim=false`。Linux/Xvfb/有效模型/真实 Actor+Observer 仍是外部门禁，
M0--M4 继续 `NOT_RUN`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.28 real-client runner audit (open)

继续目标仍未完成：当前正式模型/真人客户端/M0--M4 门禁没有任何一个可提升为
PASS。上一轮新增的 bound-follow recovery 已经接入 BrainOrchestrator，相关
`BrainOrchestratorTest` 与当前 995 项 JVM 套件通过；但这只是源代码和受控
GameTest 证据，不能解释现场“光说不做”或冒充真实模型动作。

本轮审计发现真实 Actor/Observer/Oracle 工件与独立验证器已经存在，正式链路仍
受 Linux/Xvfb 和有效模型凭据阻断。当前 `e2e/orchestrator.py` 的
`command_server_smoke` 还存在参数调用不一致风险（`build_artifacts` 的调用点
需要与其两参数签名一致），必须先定向修复/验证；任何修复都不能把无模型
oracle 的 FAIL 改成 PASS。下一步：修正该 runner 的最小缺陷，补充真实聊天到
动作证据链的健壮性审计，运行 Python/Gradle 定向门禁，再更新状态与当前 SHA。

已改文件（最近）：`src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`、
`src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`、
`docs/IMPLEMENTATION_STATUS.md`、本检查点；最后模型-backed smoke 仍为
HTTP 401/`functionalAiClaim=false`，Darwin 仍缺 Linux/Xvfb。恢复命令：
`python3 e2e/orchestrator.py preflight --forge-version 65.0.0`。

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.27 final source audit (open)

The final source audit for this continuation passed `./gradlew check` and the
compatibility checker.  The JVM result is 995 tests, 0 failures, 0 errors, and
2 skips; the current production and slim artifact hashes remain
`15b257d1aed11ad15c62c4293b16a81bf65b30121c2d5780ff6b32b75f8f68a0` and
`4e47bd327b06c93f0bad5704141f9f5779d7b02b6482beea2efcb36e09b86ddc`.

The repository still deliberately distinguishes controlled Forge lifecycle,
combat, and unit evidence from live-model gameplay.  MiMo remains HTTP 401 in
the independent probe; no model/client/Hardcore M0--M4 gate is promoted.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.26 combat pressure recheck after Brain change (open)

The real Forge 65.0.0 GameTest `real_emergency_zombie_skeleton_horde` was
rerun in isolated `run-debug38` after the bound-follow recovery changed the
Brain source.  The server spawned exactly ten vanilla zombies and ten vanilla
skeletons, the accountless body joined through the normal player path, and the
required pressure-response assertion passed 1/1 in 1.380 seconds.  The
embedded-channel warning at teardown is the known cleanup fallback.

This confirms that the new high-level recovery did not disable or alter the
20-TPS emergency combat lane.  It remains a bounded controlled PVE pressure
test, not a model-backed fight, PVP result, random Hardcore statistic, or
M0--M4 promotion.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.25 exact-JAR smoke after follow recovery (open)

The exact fat JAR containing the bound-follow recovery was staged into a clean
Forge 65.0.0 dedicated server and passed the lifecycle-only smoke in run
`20260809T002006Z-e2edab04dfdcca1`.  Forge loaded without a mod failure, the
headless player joined, the Jar-in-Jar SQLite payload opened, the server exited
cleanly, and the staged artifact matched the production SHA-256
`15b257d1aed11ad15c62c4293b16a81bf65b30121c2d5780ff6b32b75f8f68a0`.

The same run intentionally records the oracle as `FAIL` with
`functionalAiClaim=false`: no valid model credential was available, so no
model chat or movement result was fabricated.  The release contract reports
995 JVM tests (0 failures, 0 errors, 2 skips); formal M0--M4, live model,
rendered client, random Hardcore, and professional陪玩 gates remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.24 bound-follow action recovery (open)

The “光说不做” path now has a narrowly scoped recovery in
`BrainOrchestrator`: when a player-authored goal contains the server-bound
player name and the current fair semantic sample visibly contains that exact
non-hostile player, a provider `CONTINUE`/`REPLAN` cannot leave the body idle.
The runtime synthesizes only the typed `follow_entity` high-level envelope
with the observed handle and sample sequence, then routes it through the
existing `SkillSupervisor` preconditions, leases, local A* movement and
vanilla player actuator.  Missing/malformed/occluded samples still take the
ordinary model retry path; no coordinates, teleport, entity lookup or hidden
world read was added.

Evidence: `BrainOrchestratorTest` now has 36 passing tests, including positive
visible-bound recovery and a negative wrong-player binding case.  The normal
speech-only `CONTINUE`/`REPLAN` tests remain passing, so ordinary goals still
suppress action narration and keep their bounded backoff.

This is a code-level action-stall mitigation, not a live-provider, rendered
client, random Hardcore, PvP, or M0--M4 promotion.  The supplied MiMo token
still returns HTTP 401 in the independent probe.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.23 release artifact recheck (open)

After the combat, Numen prompt, and zero-human lifecycle changes, the source
release contract was rerun with the current files:
`./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` completed
successfully.  The JVM suite totals 993 tests with 0 failures, 0 errors, and 2
skips; the compatibility checker passes while its formal matrix remains
incomplete.  The installable fat JAR and slim JAR hashes are respectively
`696d8760ee55c9748da3ff8c887793efb6f36579003c84e109c1bcc33c8372d2` and
`a29504d2dd438990012ee8c905b525bd8ec12d23924b86f64ea081b7d331c26d`.

This is a source/package regression result, not a model-backed or client
playability result.  The MiMo credential remains HTTP 401, and M0--M4,
rendered-client, live-chat, random Hardcore, and professional陪玩 claims stay
`NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.22 zero-human remote-chunk/respawn recheck (open)

The focused Forge 65.0.0 GameTest `zero_human_dedicated_server_chunk_and_respawn`
was rerun in an isolated `run-debug37` lifecycle with no human client.  The
headless `ServerPlayer` loaded the remote test area, joined through the normal
player-list path, was killed through vanilla damage, and completed the bounded
respawn/rejoin assertion before the server shut down.  The server reported
`All 1 required tests passed` in 1.816 seconds.  The closed-channel fallback
warning is the expected teardown of the embedded connection, not a failed
GameTest assertion.

This remains a lifecycle proof only: no live model request, rendered client,
random unseen Hardcore seed, or two-hour completion statistic was involved.
The supplied MiMo credential is still independently rejected with HTTP 401,
so model-backed M0--M4 gates remain `NOT_RUN` and `functionalAiClaim` remains
false.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.21 Numen reference and prompt-boundary recheck (open)

The public `Dwinovo/minecraft-numen` repository was reviewed for architecture,
not copied.  Its useful principles are an actual server-side player body,
tool-result feedback, background long-running jobs, failure-directed recovery,
and an explicit “act rather than narrate” prompt.  This project already has
those boundaries through `ServerPlayer`, fair observations, skill leases,
checkpoints, and the single high-level `DecisionEnvelope`; the comparison is
recorded in `docs/REFERENCE_NUMEN_ANALYSIS.md`.

An attempted duplicate static follow-routing hint was rejected by the existing
planner prompt-length gate (three focused prompt tests failed).  It was removed
immediately; the existing goal-specific `TRUSTED_IMMEDIATE_FOLLOW_PLAYBOOK`
remains the sole routing hint, and
`MinecraftPlannerInputFactoryTest` passes all 24 cases after the correction.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.20 combat-pressure recheck (open)

The combat response lane was tightened and then exercised in real Forge
65.0.0/Minecraft 26.2 GameTest server lifecycles.  Iron golems remain neutral
by default: proximity alone does not create a target.  An explicit combat
decision, or a recent fair golem damage/contact lease, is required before the
local emergency controller may reacquire one.  This closes the first honest
golem failure, where the body attacked once and then returned to `CLEAR` while
the golem was still visible.

Evidence:

- `real_emergency_iron_golem_duel`: 1/1 passed in `run-debug35` after the
  lease fix.  The body used ordinary sword/shield movement and caused
  meaningful target damage while surviving the pressure window.
- `real_emergency_zombie_skeleton_horde`: 1/1 passed in `run-debug36` with
  exactly ten vanilla zombies and ten vanilla skeletons.  The body survived,
  moved, consumed the ordinary combat lane, and damaged multiple targets.

The first golem run (`run-debug32`) failed at tick 1683 with the target still
at 79 health and `survival=CLEAR`; that failure was retained as diagnostic
evidence rather than hidden.  A later full-kill-only assertion also timed out,
so the gate was corrected to measure bounded pressure-response evidence, not
to pretend that a deterministic fixture proves PvP expertise.  These tests
still use no live model request, no rendered client, no hidden-world reads,
and no random Hardcore seed.  M0--M4 and model-backed gates remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.19 physical movement and survival recheck (open)

After the release-contract check, the current source was exercised again in
separate real Forge 65.0.0/Minecraft 26.2 GameTest server lifecycles.  The
following focused tests each completed 1/1 with an embedded `ServerPlayer` and
vanilla movement/menu paths:

- `real_water_clutch` (fall-water deployment and bracing);
- `real_parkour_course` (multi-step jump course);
- `real_travel_diagonal_detour` (diagonal travel with an obstruction and
  recovery);
- `real_furnace_batch` and `real_charcoal_furnace_batch` (ordinary and
  charcoal smelting);
- `real_food_animal_hunt` (visible animal food acquisition).

An attempted combined selector using a regular expression was rejected by
Minecraft's selector matcher before any test ran; it was not recorded as a
product failure.  The tests were therefore rerun one at a time and all passed.
This evidence still does not promote M1--M4: no live model request, rendered
client observation, random unseen Hardcore seed, or long-run success-rate
measurement was involved.  The supplied MiMo credential remains independently
401-rejected, so the model-backed gates remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.18 release-contract recheck (open)

The final source-side release contract for this continuation passed:
`./gradlew check verifyReleaseJar e2eClientJar e2eOracleJar` completed
successfully, including the JVM suite, compatibility contract, release-JAR
audit and both test-only client/oracle artifacts.  The installable fat JAR
remains SHA-256
`4d0a28c14de668341a164d8509c25467d6c37e997ff72e12685e188409c963b3`.

No formal milestone is promoted by this command.  The compatibility task
reports all eleven Forge 65 patches as declared/published but keeps
`formalMatrixComplete=false`, and the model, rendered-client and hidden-seed
evaluators remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.17 presence/restart regression gates (open)

The latest source also re-passed the two lifecycle paths behind the earlier
“世界里没有 AI” report on Forge 65.0.0/Minecraft 26.2:

- `zero_human_dedicated_server_chunk_and_respawn`: 1/1; with no human client,
  the body was spawned, remained simulated outside the observer area, died
  through vanilla damage, and rejoined through normal respawn handling;
- `auto_presence_on_human_login`: 1/1; a human login caused the AI body to
  appear at a nearby safe point with its `[AI]` identity, and the no-credential
  safety rule kept it still rather than pretending to be model-ready.

These are lifecycle/availability gates only.  They do not prove that an
invalid or absent provider credential can produce speech or ordinary goals;
the earlier independent MiMo probe remains HTTP 401 and model gates stay
`NOT_RUN`.

The bounded emergency combat recheck also passed both matched tests in one
Forge server run (`real_emergency_slime_defense` and
`real_emergency_enderman_defense`, 2/2).  This confirms the latest source still
leaves the local damage-response lane active when no model credential is
available; it is not a PvP or random-world success-rate claim.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.16 fat-JAR lifecycle audit (open)

The freshly regenerated installable fat JAR was staged into a clean dedicated
server by `python3 e2e/orchestrator.py server-smoke --forge-version 65.0.0`.
The exact artifact SHA-256 is
`4d0a28c14de668341a164d8509c25467d6c37e997ff72e12685e188409c963b3` and the
server, actor and observer staging copies all match it.  The declared
`dedicated_server_exact_jar_lifecycle_only` verdict is `PASS`: Forge loaded,
the accountless headless player joined, SQLite loaded from the Jar-in-Jar
payload, and shutdown was clean.  As designed, the oracle gameplay portion is
`FAIL`/`functionalAiClaim=false` because this run has no model credential and
does not claim chat-to-action or movement.

The complete JVM suite and the cleaned focused/dragon GameTests also pass.
The repository remains dirty and this is development evidence only; formal
M0--M4, live model-backed chat-to-action, rendered/offscreen client E2E and
unseen-seed Hardcore statistics remain `NOT_RUN`.

`./gradlew compat-checker` also passes the repository contract for the Forge 65
line: published entries `65.0.0` through `65.1.0` are listed, while the report
correctly keeps `formalMatrixComplete=false` until each runtime matrix case is
actually executed.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.15 End entry/dragon/return recheck (open)

After the diagnostic cleanup, the broader controlled server chain was rerun:
`real_end_victory_and_return` passed 1/1 on Forge 65.0.0/Minecraft 26.2 in
`run-debug17`.  The same headless `ServerPlayer` entered the End, completed
the ordinary dragon fixture using vanilla combat attribution, earned
`Free the End`, and physically returned through the central portal to the
Overworld.  The run also exercised the normal damage/emergency observation
path; no direct dragon kill, teleport, inventory injection, or portal block
write was added by the test.

This is still a deterministic controlled GameTest and therefore does not
promote M2, M4, a live-model claim, a rendered client claim, or a random
Hardcore-seed statistic.  Formal M0--M4 and the model/client/offscreen gates
remain `NOT_RUN`.

The post-fix focused portal JVM regression and full Forge `check` also pass on
Forge 65.1.0 (the suite reports zero failures/errors and two external skips),
and the compatibility schema validator passes. The expected warning/error
lines are bounded-failure test diagnostics; Gradle completed successfully.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v90.14 end-portal interaction fix (open)

The focused real Forge GameTest now passes after the end-frame interaction
correction.  The failure chain had progressed through portal return, stronghold
room search, and crystal combat, then repeatedly stalled while filling the
remaining End portal frames.  The cause was not a model response or a hidden
world read: the semantic ray fan could expose a different face of the same
frame while the body turned, and the skill sent that newer face to the vanilla
actuator instead of reusing the exact fair hit that started the alignment.

`ActivateObservedEndPortalSkill` now keeps the first observed block/face/hit
for an alignment, upgrades only when a later fair top hit of that same frame is
visible, and sends that stored `BlockInteractionTarget` through the normal
server crosshair, face, reach, inventory and block-use checks.  The temporary
crystal, eye, portal-search and end-frame action diagnostics were removed from
the production skill and GameTest after the cause was reproduced.

Evidence:

- `compileJava`: `BUILD SUCCESSFUL` after the interaction-target fix;
- `runGameTestServer -Pgametest_working_dir=run-debug15
  -Plive_model_selector=mcai_companion:real_end_portal_activation`: 1/1
  required test passed on Forge 65.0.0/Minecraft 26.2;
- the run consumed all twelve ordinary `ender_eye` items and produced all
  nine vanilla `end_portal` blocks; no direct frame/world mutation was added.

This is a controlled fair-player server gate, not a model-backed completion,
rendered-client gate, Hardcore random-seed result, or M0--M4 pass.  Formal
M0--M4, live model-backed chat-to-action, client/offscreen E2E, and hidden-seed
Hardcore statistics remain `NOT_RUN`.  Next is a clean compile plus the
broader no-human completion-chain gate; the supplied MiMo credential remains
unverified (the earlier endpoint probe returned 401), so no model claim will be
made without a valid credential and an auditable run.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v81--v83 handoff correction and next gate (open)

v81 captured the failure owner: the portal-return tick was claimed by
`EMERGENCY_SURVIVAL` in `GUARDING` even though the current frame had no danger
and the return skill did not own hostile proximity.  The prior Blaze combat
timestamp was the only reason the 120-tick reacquisition window stayed alive.
The fix adds a one-shot `SkillSupervisor` completion edge and clears only that
bounded timestamp at the next skill handoff; fresh visible hostiles, recent
damage, physical contact, fire, falls, air, and food still preempt normally.

v82 showed the production-only handoff was insufficient because the lifecycle
GameTest manually ticks a skill before the emergency controller.  v83 changed
the shared handoff edge so the test path consumes it too.  The portal-return
portion then passed: the body walked through the real corridor and reached the
verified portal area with `survival=CLEAR` and the active-skill lease.

The same v83 run exposed the next genuine lifecycle failure at the subsequent
crystal-cage pillar: `tower_up.visible_support_face_unavailable` at tick 2490.
Its manual test order lets `tower_up` rotate first and then lets a fresh
`WARNING_REACTING`/`GUARDING` emergency overwrite the first-person view every
tick, so the skill exhausts its alignment counter even though production would
not tick the lower skill while emergency owns the body.  The next change is to
make the GameTest actuator order match the production arbiter (emergency first;
run the active skill only when emergency passes), then rerun the same full
lifecycle gate.  v83 is a failure and is not counted as a lifecycle pass.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v78--v80 portal-return failure (open)

The fresh lifecycle gate was rerun after the planner start/support changes and
is still a real failure, not a pass:

- v78 `headless_player_lifecycle_state_and_fair_action` failed at
  `return_via_verified_portal` with the body held at the one-block portal
  approach (`travel_to.stuck`);
- v79 confirmed that accepting the current grounded cell as a bounded start
  lets the body leave the original cell, but it then selected an unhelpful
  reverse frontier and stopped;
- v80 added a 128-observation-revision memory for only body-contact-confirmed
  support cells.  It still failed at `travel_to.stuck`, ending near
  `(-46.450740872484914,49.0,-0.16741180191619878)` for the verified target
  `(-46.5,48,-11.5)`.

The changed files are `LocalAStarPlanner`, `MoveToSkill`, `RollingTravelPlanner`
and `TravelToSkill`; they do not authorize unknown blocks, hidden world reads,
teleportation, or direct world edits.  The last failed run is
`run-patch-matrix/forge-65.0.1-v80-body-lifecycle` (Gradle exit 1).  The next
step is to capture the emergency-survival/behavior-arbiter state during the
same return tick, because the logs show a stale `GUARDING` intervention after
Blaze combat and the final movement input is false.  If that lane is the
owner, the handoff will be narrowed so only an actual fresh threat can preempt
portal travel; if it is clear, the portal corridor/rolling planner trace will
be corrected instead.  No v78--v80 result promotes M0--M4; formal M0--M4,
live model-backed chat-to-action, client/offscreen E2E, and unseen-seed
Hardcore statistics remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v74--v77 physical safety gates

Fresh Forge GameTest server runs against the current source passed four
player-body physics gates, each 1/1 on Forge 65.0.0/Minecraft 26.2:

- v74 `real_emergency_enderman_defense`;
- v75 `real_emergency_slime_defense`;
- v76 `real_water_clutch` (the log records real
  `DEPLOYING_WATER`/`PREPARING_WATER` interventions);
- v77 `real_parkour_course`.

These gates use the actual fair headless `ServerPlayer`, vanilla collision,
inventory and movement actuators.  They cover the concrete field failures of
standing still while attacked, failing to self-rescue from a fall, and being
unable to traverse a bounded parkour course.  They do not prove model
classification, natural conversation, PvP, or survival completion.

Formal M0--M4, live model-backed chat-to-action (the supplied MiMo endpoint
currently returns 401), rendered/offscreen client E2E, Hardcore unseen-seed
statistics, and the Forge compatibility matrix remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: dedicated playerless lifecycle smoke

The exact regenerated fat artifact was staged into a fresh dedicated-server
run by `python3 e2e/orchestrator.py server-smoke --forge-version 65.0.0`.
The server ran with no human client and no model credential.  The immutable
verdict is `PASS` for its declared lifecycle scope: dedicated server started,
the oracle loaded, the headless `MCAI` ServerPlayer joined, runtime memory
opened SQLite only from the product Jar-in-Jar payload, the exact product hash
was preserved in all staged copies, and graceful shutdown removed the player
with exit code 0.  Artifact SHA-256 was
`8e5dae81bd8543c9564c096ebb03f89c26155873438b8a58642b9a707a2e2fab`.

The associated oracle gameplay result is intentionally `FAIL`/`setupComplete`
false because this smoke has no model and does not run chat or movement; the
separate `server-smoke-verdict.json` correctly reports
`functionalAiClaim=false`.  This is a real playerless-server lifecycle gate,
not a functional companion or M1 result.  The E2E Python audit suite also
passed all 15 tests.  Formal M0--M4, live model-backed chat-to-action, rendered
client/offscreen, Hardcore unseen-seed statistics, and Forge compatibility
matrix remain `NOT_RUN`.

Run evidence: `e2e/results/no-commit-dirty/20260808T223019Z-e2e28dfca768af5`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v72--v73 scan-frame fix and package refresh

The v63 Ender reserve failure was isolated to `SecureEnderPearlReserveSkill`:
the scan-alignment timeout was charged once per server tick even when the
semantic perception frame had not changed.  At the fixture's lower-frequency
observation cadence this could exhaust the setup budget while the body was
still correctly waiting for a fresh aligned frame.  The scan phase now counts
only a new `observationRevision`, records the revision it last charged, and
resets that marker on alignment and every new scan turn.  This preserves the
bounded timeout while avoiding duplicate charges from the same observation.

Fresh real Forge GameTests after the correction:

- v72 `real_ender_pearl_reserve`: 1/1, `All 1 required tests passed`;
- v73 `real_ender_pearl_reserve` recheck: 1/1, `All 1 required tests passed`,
  Gradle `BUILD SUCCESSFUL`.

The focused JVM suite had already passed with the correction.  The installable
fat artifact was then regenerated with `jarJar` (not only the slim `jar` task):
`build/libs/mcai_companion-0.1.4-dev-mc26.2.jar`, SHA-256
`8e5dae81bd8543c9564c096ebb03f89c26155873438b8a58642b9a707a2e2fab`.
The slim development archive is
`build/libs/mcai_companion-0.1.4-dev-mc26.2-slim.jar`, SHA-256
`e4c418789a787303787bb3dc194e6cb309df2abd78dde72f2f7f34e8afb90df1`.
Both archives contain the same corrected `SecureEnderPearlReserveSkill`
class; only the fat archive includes the bundled SQLite dependency and mod
metadata.

Formal M0--M4, live model-backed chat-to-action (the supplied MiMo endpoint
currently returns 401), real client/offscreen observation, playerless-server
lifecycle, Hardcore unseen-seed statistics, and the Forge compatibility matrix
remain `NOT_RUN`.  These v72/v73 results are directed fair-player server
skill gates, not a final professional-companion or speedrun claim.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v58--v68 pearl-merge race correction

The fresh v58/v59 Forge runs passed the corrected blaze-material and
workstation gates.  The first fresh Ender reserve rerun (v60) failed with
`collect_observed_item.item_lost_without_pickup`; v61 reproduced the same
failure.  This was not a random no-drop case: the fixture gives the held
Enderman a guaranteed pearl drop, and the failure snapshot showed the bound
drop UUID gone while another pearl ItemEntity remained nearby.

The cause is vanilla `ItemEntity` merging.  Minecraft removes one entity UUID
and keeps a nearby same-item entity as the stack survivor.  The fair collector
was bound forever to the removed UUID, while the Ender wrapper's filter also
hid the survivor when it had been visible before the current combat attempt.
That made a normal vanilla merge look like a lost drop.

The correction is bounded and observation-only:

- `CollectObservedItemSkill` rebinds only when both synchronized core and
  interaction frames currently show a different same-item entity within the
  two-block merge radius of the last genuinely observed position.  No entity
  query, hidden stack count, teleport, or direct inventory edit is used.
- `AcquireShelteredEnderPearlSkill` allows that nearby survivor through the
  already-authorized drop filter, including a pre-combat survivor only inside
  the same bounded death-site radius.  Inventory growth remains the success
  authority.
- `CollectObservedItemSkillTest` now covers the merge-radius boundary.

Evidence after the correction: v62 passed the focused reserve, v63 exposed a
separate pre-target `scan_alignment_timed_out` setup flake and is not counted
as a pearl result, and v64, v66, v67, and v68 each passed a fresh real
`real_ender_pearl_reserve` server gate.  v65 was the instrumented failure
capture used to identify the removed/surviving UUIDs; temporary production and
GameTest instrumentation has been removed.  The runtime printed Forge
65.0.0/Minecraft 26.2 in these runs (the working-directory labels are not a
version claim).

Formal M0--M4, live model-backed chat-to-action (the supplied MiMo endpoint
currently returns 401), real client/offscreen observation, Hardcore unseen
seed statistics, and the Forge compatibility matrix remain `NOT_RUN`.  The
next exact work is a focused JVM regression including all modified skills,
fresh blaze/workstation/diagonal gates, then `jar` packaging and artifact
inspection.  No milestone is promoted by these directed passes.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v69--v71 adjacent gates and package

After the merge correction, fresh directed Forge gates also passed:

- v69 `real_nether_blaze_material_reserve`: 1/1, physical combat, visible
  blaze-rod collection, and vanilla inventory confirmation;
- v70 `workstation_wood_prerequisite_composition`: 1/1 (the normal closed
  channel warning occurs during GameTest shutdown after the pass);
- v71 `real_travel_diagonal_detour`: 1/1, including the bounded reverse
  frontier and diagonal arrival.

The focused JVM suite covering `RollingTravelPlanner`, `TravelToSkill`, the
emergency survival controller, observed-item collection, Enderman and Blaze
acquisition, and both secure reserve wrappers passed.  `gradlew jar` also
passed.  The packaged fat artifact is
`build/libs/mcai_companion-0.1.4-dev-mc26.2.jar` (SHA-256
`d903b44c943de7d17adcf130044fdbf5466d71224a0d3bedc0e96a6a0f764d58`); the
slim development artifact is retained separately.  Its metadata explicitly
declares Forge `[65.0.0,66)` and Minecraft `[26.2]`; these runs printed Forge
65.0.0/Minecraft 26.2 and do not establish compatibility with 64.x, 65.0.8,
66.x, or the older 26.1.2 target.

These are directed inner-loop/skill gates only.  Formal M0--M4, the live
model-backed chat path (the supplied MiMo endpoint returns 401), real client
and offscreen visual E2E, playerless-server lifecycle, Hardcore unseen-seed
statistics, and the Forge compatibility matrix remain `NOT_RUN`.  The next
work is to preserve these artifacts/checksums and move to the dedicated
offscreen/model gates; no final product or “professional companion” claim is
made from this package alone.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v48--v57 adjacent real gates

The fresh regression sequence found and corrected three adjacent production
issues instead of treating the earlier green runs as a completion claim.

- v48 `real_nether_blaze_material_reserve` failed at
  `collect_observed_item.item_lost_without_pickup`: the emergency hostile
  reacquisition sweep reclaimed the actuator after combat had already handed
  the first-person frame to pickup.
- v49 then exposed the same stale lease during
  `SecureNetherBlazeMaterialSkill` selection, causing
  `secure_nether_blaze_material.scan_alignment_timed_out` while the body was
  stationary.  `AcquireNetherBlazeRodSkill` and the secure material
  selection/exploration phases now suppress only a clean stale sweep; a newly
  visible hostile/projectile still returns ownership to emergency safety.
- v50 `real_nether_blaze_material_reserve` passed 1/1 on a fresh server, with
  physical combat, drops, pickup, and a completed reserve.  v51
  `workstation_wood_prerequisite_composition` passed 1/1 on a fresh server.
- v52 and v53 reproduced the same `real_travel_diagonal_detour` failure:
  `travel_to.route_unknown` at tick 532.  Terminal evidence showed the sole
  target-advancing safe cell `(-291,-42,-292)` was permanently rejected even
  though it had previously been reached successfully.  v54 added bounded
  terminal evidence confirming `rejected=true` rather than an observation
  flake.
- Removing successful endpoints from the rejection set (v55) changed the
  failure to a real oscillation and `travel_to.no_progress`, which proved
  that simply allowing revisits is also unsafe.  A temporary segment trace in
  v56 showed the body oscillating around the dead-end baseline and repeatedly
  entering course recovery without taking the unexplored branch.
- The final correction adds a separate, bounded reverse-frontier choice in
  `RollingTravelPlanner`: only when no non-rejected target-advancing cell is
  available, an adjacent fully observed safe cell may move away from the
  waypoint by at most the existing detour budget.  It is still routed through
  vanilla `MoveTo`, has no world query or teleport, and is covered by
  `boundedReverseFrontierEscapesARejectedDeadEnd()`.
- v57 `real_travel_diagonal_detour` passed 1/1 on a fresh server and completed
  the diagonal branch and arrival.  Temporary segment tracing has been
  removed; the bounded terminal observation evidence remains for audit.

The actual runtime printed by every v50--v57 server is Forge 65.0.0 for
Minecraft 26.2 (Forge reports target 65.1.0), despite the historical working
directory names.  This is compatibility evidence for that runtime only, not
proof of Forge 65.0.1 or 26.1.2 support.

Formal M0--M4, live model-backed chat-to-action (the supplied MiMo endpoint
currently returns 401), real client/offscreen observation, Hardcore unseen
seed statistics, and the Forge compatibility matrix remain `NOT_RUN`.
The next exact work is to rerun the corrected blaze and workstation gates,
run the focused JVM suite without diagnostic noise, then package and inspect
the JAR. No milestone is promoted by these directed passes.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: v40--v47 real pickup/return gate

The focused Forge 65.0.1 `real_ender_pearl_reserve` run v47 passed 1/1 on a
fresh working directory (`run-patch-matrix/forge-65.0.1-v47`).  The log ends
with `Completed focused real Ender pearl reserve acquisition GameTest`,
`All 1 required tests passed`, and Gradle `BUILD SUCCESSFUL`.  This is a
directed, deterministic server GameTest using the fair `ServerPlayer` body;
it is not an AI-model test and it is not an M1--M4 acceptance result.

The repeat history is intentionally retained: v40 passed, v41 reproduced a
stationary collection failure, v42 traced the failure to the emergency guard
lane claiming the actuator while collection was active, v43 removed that
lease conflict and exposed a real sheltered-return `move_to.route_unknown`,
v44 exposed a second collection movement-gate race, and v45/v46 passed after
the race correction.  v47 is the first repeat after removing the temporary
observed-item trace logging; the remaining ender-reserve diagnostic lines are
the existing bounded GameTest audit, not a production recovery trace.

Current production corrections:

- `EmergencySurvivalController` no longer steals the movement lease from an
  active collection/combat skill merely because the local attack cooldown is
  active; physical contact and unmanaged hostile proximity remain emergency
  owned.
- `AcquireShelteredEnderPearlSkill` releases collection when a causally
  correlated new pearl is visible even if the defeated Enderman UUID lingers
  for one semantic observation.  A reappearing bound target on a newer
  observation revision is still rejected, and inventory growth remains the
  pickup authority.
- `SecureEnderPearlReserveSkill` now gives bounded re-observation/retry for
  only `move_to.route_unknown`, `move_to.stuck`, and `move_to.turn_stuck` while
  returning to the verified shelter.  It never teleports, inserts items, or
  bypasses the hardcore danger gate.
- Temporary `CollectObservedItemSkill` reacquisition diagnostics have been
  removed.  The focused JVM suite covering the modified controller, acquire,
  collect, engage/collect, and secure-return skills passes.

The v47 run still shows physically moving vanilla-player motion, repeated
combat/drop/pickup cycles, shelter verification, and normal shutdown.  It does
not prove model responsiveness, passive chat interpretation, navigation in a
playerless client, PvP, parkour, chunk-ticket policy, or survival completion.
Formal M0--M4, real model-backed chat-to-action, real client/offscreen
observation, Forge 65.x compatibility matrix, and unseen-seed Hardcore
statistical gates remain `NOT_RUN`.

Next exact work is a fresh Forge 65.0.1 regression for the blaze reserve,
wood/workstation prerequisite composition, and diagonal travel gates, then a
compile/package check.  Any failure will be recorded rather than hidden.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: sheltered pearl return and pickup repeat

The previous Forge 65.0.1 ender-pearl reserve run exposed a second, distinct
failure after the bounded observed-item approach fix.  At the end of a real
combat cycle the body was physically back inside its shelter, but a fresh
first-person sample could temporarily omit the roof proof while pickup
entities were still settling.  Starting the precision return mover from that
sample caused the hardcore residual-risk gate to reject with
`move_to.hardcore_danger` (v36).

`SecureEnderPearlReserveSkill.tickHunter` now treats a completed hunt already
inside the shelter as a selection reset/re-observation boundary.  It no
longer starts a precision return solely because the just-consumed semantic
sample lacks transient roof proof.  It still uses the normal return path when
the body is actually outside the shelter, and the builder path remains the
authority when a later fresh observation confirms that the shelter is not
safe.

The next run (v37) crossed that failure and reached repeated physical
Enderman/drop cycles, but failed at
`collect_observed_item.item_lost_without_pickup`.  A temporary physical/entity
trace in v38 showed live pearl entities outside the one-block pickup envelope
and intermittent first-person visibility while the collection lease was still
waiting for the combat child to authorize the drop.  This was diagnostic only;
no teleport, direct item insertion, hidden-world scan, or synthetic pickup was
used.  The same source completed the required reserve in v39 (Forge 65.0.1,
`real_ender_pearl_reserve`, 1/1, Gradle `BUILD SUCCESSFUL`), but that single
pass does not erase the v38 flake or establish stability.

Temporary production and GameTest logging used for v38/v39 has now been
removed.  Focused JVM tests for `SecureEnderPearlReserveSkill`,
`AcquireShelteredEnderPearlSkill`, and `CollectObservedItemSkill` pass after
cleanup.  The immediate next gate is to repeat the real reserve on fresh
Forge worlds (v40 and, if needed, v41), then rerun the diagonal travel gate.
Only repeated clean runs will allow this narrow inner-loop fix to be marked
stable.  Formal M0--M4, real model-backed chat-to-action, offscreen client,
and unseen-seed Hardcore statistical gates remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-09 continuation checkpoint: observed drop pickup lease ordering

The Forge 65.0.1 Blaze-reserve gate reproduced a concrete production failure:
the drop was still alive and first-person visible at roughly 1.85--2.3
blocks, but `CollectObservedItemSkill` queued a direct forward frame and then
cancelled the stale `MoveToSkill`.  `MoveToSkill.cancel()` quiesces the fair
actuator, clearing that just-queued input.  The body therefore rotated and
waited until the 50-tick lost-target grace expired with
`collect_observed_item.item_lost_without_pickup`.

Source correction in
`src/main/java/dev/mcai/companion/skills/loot/CollectObservedItemSkill.java`:

- quiesce an old route before issuing a direct visible/remembered approach;
- permit one bounded same-level approach frame when the body is on ground,
  not in water, no unmanaged threat is present, and the known support is not
  liquid, even if the exact centre-below voxel is absent from the current ray
  fan; the next semantic frame remains the collision/inventory authority;
- retain the ordinary vanilla inventory-growth confirmation as the only
  pickup success signal.

Evidence:

- `CollectObservedItemSkillTest`, `EngageAndCollectObservedDropSkillTest`,
  and `SecureNetherBlazeMaterialSkillTest`: PASS;
- Forge 65.0.1 `real_nether_blaze_material_reserve` v34: PASS 1/1, Gradle
  `BUILD SUCCESSFUL`; logs show the player physically approaching drops and
  completing repeated combat/pickup cycles.  This is an inner-loop directed
  gate, not formal M1--M4 acceptance.
- v32 and v33 remain recorded as failures before the final bounded-approach
  correction; no earlier failure is being erased.

Last current next work: run fresh Forge 65.0.1 regressions for
`workstation_wood_prerequisite_composition`, `real_ender_pearl_reserve`, and
`real_travel_diagonal_detour`; update the progress evidence and only then
rebuild/package. Formal M0--M4, model-backed chat-to-action, real offscreen
client, and unseen-seed Hardcore gates remain `NOT_RUN`.

Updated: 2026-08-09 (Asia/Tokyo)

## 2026-08-07 continuation checkpoint: observed travel still fails the shelter gate

Current root cause: the fair body is moving through vanilla `ServerPlayer`
physics, but `TravelTo` can choose a locally safe cell that is only mildly
closer (or later farther) from a remote exploration waypoint.  Repeated local
segments then consume the exploration budget without reaching the wood.  This
is a production navigation defect, not a model-response or chunk-loading
claim.

Latest source changes:

- `TravelToSkill` keeps bounded rejected frontier cells across tiny target
  improvements, records nested `MoveTo` checkpoints, and resets its no-progress
  watchdog only for target-closing or map-distance progress; sideways motion
  no longer counts as successful journey progress.
- `RollingTravelPlanner` preserves `CANDIDATE_UNREACHABLE` as an explicit
  reject/replan result instead of silently replacing it with another frontier.
- `ExploreForObservedTargetSkill`, `PrepareFoundationShelterMaterialsSkill`,
  and the shelter GameTest expose nested child state, position, and look for
  reproducible diagnosis.

Directed evidence after these changes:

- `TravelToSkillTest` and `RollingTravelPlannerTest`: PASS.
- `real_travel_diagonal_detour` on Forge 65.0.1: PASS 1/1; this does not cover
  the remote wood-search failure.
- `shelter_material_wood_exploration` v24, v25, v26, and v27: FAIL 1/1 each.
  v25 showed 223 completed local segments and a body physically moved to
  `(-300.3865,-42,-298.8492)` while the target remained outside arrival.
  v26/v27 showed target-distance deterioration after a best distance near
  `3.01`, with nested `MoveTo` still following a local cell; v27 correctly
  timed out/replanned rather than treating that wandering as progress.

Last failed gate: the isolated Forge 65.0.1 shelter-material GameTest timed
out at 5000 ticks.  Formal M0--M4, model-backed chat-to-action, real-client,
and unseen-seed Hardcore gates remain `NOT_RUN`; no milestone is promoted.

Next exact work: add a target-direction invariant to rolling detour selection
and a bounded course-recovery transition when the body is materially farther
from the waypoint than its best observed distance; then rerun the shelter gate
and the three previously passing component gates (`real_ender_pearl_reserve`,
`real_nether_blaze_material_reserve`, and
`workstation_wood_prerequisite_composition`) on fresh Forge 65.0.1 worlds.

Updated: 2026-08-06 (Asia/Tokyo)

## 2026-08-06 continuation checkpoint: current-source compatibility evidence

The credential restore and authentication-status fixes are now built from the
same source against the Forge 65.0.0 compile floor.  `compileJava` also passes
for 65.0.8 and 65.0.9, so the source does not depend on the newer patch API.
This is compile evidence only: the complete Forge 65 patch matrix, client
rendering, and model-backed gameplay gates remain `NOT_RUN`.  The authoritative
artifact is the package gate below; its hash is unchanged by the documentation
update.

The next independent check is limited to JSON/hash/Python audit consistency and
the focused credential/lifecycle tests.  No provider request is made by that
check and no key is written to repository artifacts.

The focused JVM set passed, followed by fresh Forge 65.1.0 lifecycle checks:
zero-human dedicated-server chunk/death/respawn 1/1 in 1.724 seconds and
human-login auto-presence 1/1 in 649.9 ms.  The Python audit is 15/15 and the
product hashes remain the values recorded below.  These checks prove body
admission/lifecycle and credential state handling only; they do not prove model
authentication, ordinary task execution, combat competence, or M0--M4.

## 2026-08-07 continuation checkpoint: Forge 65 compile coverage

The current source was compiled independently against every published Forge 65
patch: `65.0.0`, `65.0.1`, `65.0.2`, `65.0.3`, `65.0.4`, `65.0.5`,
`65.0.6`, `65.0.7`, `65.0.8`, `65.0.9`, and `65.1.0`. The compatibility
declaration now records all eleven in `compileVerifiedPatches`.

This is deliberately not a runtime compatibility claim. The formal patch
matrix still requires exact product loading plus chat, movement, menu,
save/restart, and real-client checks for each patch; those remain `NOT_RUN`.

The two bounded lifecycle tests were then run on each of the same eleven
patches. `zero_human_dedicated_server_chunk_and_respawn` and
`auto_presence_on_human_login` passed 1/1 on every patch, 22/22 total. This
proves only body admission, chunk/death/respawn, and login presence; the
patch-level chat/movement/menu/save/restart and exact-client matrix remains
`NOT_RUN`.

## 2026-08-06 continuation checkpoint: setup-screen credential restore race

The setup screen could ask for the API key again immediately after a world
restart even when the key was stored in the platform credential store.  The
server created a new `ApiKeyManager`, while its Keychain/Secret-Service load
ran only on the model bootstrap worker; `handleOpen` sent the UI state before
that local restore completed, so `credentialAvailable` was briefly false.

`ModelRuntime.restoreCredential()` now performs exactly one local restore
operation off the server thread and returns only a boolean.  The setup-open
handler waits for that completion before sending the secret-free state.  It
does not contact the provider, expose the key, or write it to the world.  A
focused `ModelRuntimeTest` verifies that this path makes credential
availability visible without creating a provider probe.  The existing
macOS-Keychain, Windows-DPAPI, Linux-Secret-Service, systemd credential-file,
and environment injection boundaries remain unchanged.

This fixes the observed re-entry race in source and focused JVM tests; it is
not a claim that the supplied MiMo credential is valid (the independent live
probe remains HTTP 401), nor a real-client or M1--M4 pass.

After the change, the complete package gate passed 987 JVM tests (0 failures,
0 errors, 2 skips), with product SHA-256
`d686592b129711b6689a0d96657597f8c1f924840163101ce5743c64b5232093` and slim
SHA-256 `a2fb43449a396badfd100c54fe58e1f0e329eee72844ffd0678dbf338f121e5d`.
The exact-JAR Forge 65.1.0 lifecycle smoke
`e2e/results/no-commit-dirty/20260806T145900Z-e2e73cc54db7b60` passed cleanly
with matching expected/loaded product hashes and `functionalAiClaim=false`.
The Python audit remains 15/15 and `./gradlew check` passes.

The setup state also now preserves a non-empty runtime configuration error
when the requested state code is merely `ready`; after a real provider 401 the
screen therefore shows `api_key_rejected` instead of a misleading healthy
status. `ModelRuntimeTest` covers this snapshot contract. The subsequent full
package passed 988 JVM tests (0 failures, 0 errors, 2 skips), with product
SHA-256 `02a90602ad25414be26f0f024e6d2adaca6e81de53ca93c0f33ade7173272898`
and slim SHA-256
`edde0aafdfeccb73dca74a05393fb15ea605b22958a52c09e2fa51ae004e27a8`.
Exact smoke `e2e/results/no-commit-dirty/20260806T150608Z-e2e0fe4e64a48f8`
passed with matching product hashes and `functionalAiClaim=false`.

The credential manager now serializes persistent save/restore/clear/close
operations. This prevents an old platform-store read from overwriting a newly
submitted key in the process cache while setup and world-start bootstrap run
concurrently. `ApiKeyManagerConcurrencyTest` reproduces that ordering with a
blocked store and passes.

The final package after this concurrency fix passed 989 JVM tests (0 failures,
0 errors, 2 skips), with product SHA-256
`d903b44c943de7d17adcf130044fdbf5466d71224a0d3bedc0e96a6a0f764d58` and slim
SHA-256 `18083297d1ae2b243d28e8fd1a10c73040470ebe2d941e0d55b158aed1692526`.
Exact smoke `e2e/results/no-commit-dirty/20260806T151632Z-e2e8fd9f4119cab`
passed with matching hashes and `functionalAiClaim=false`.

After restoring the Forge 65.0.0 compile floor, the exact product was rerun
once more as `e2e/results/no-commit-dirty/20260806T152134Z-e2e26cc46bb701f`;
the expected and loaded SHA-256 values still match and the lifecycle verdict
is PASS with `functionalAiClaim=false`.

The current source was also rerun on real Forge 65.1.0 with no model
credential: `zero_human_dedicated_server_chunk_and_respawn` passed 1/1 in
1.795 s and `auto_presence_on_human_login` passed 1/1 in 726.4 ms. These
verify only real ServerPlayer lifecycle, safe spawn, death/respawn, and
human-login presence; they do not promote chat, movement, model, or M1--M4.

## 2026-08-06 continuation checkpoint: Forge compatibility declaration and lifecycle smoke expansion

The compatibility declaration had drifted from the objective's machine-readable
contract: it used `[[lines]]` and snake_case fields, so an automated checker could
not reliably consume the required Forge line metadata. `compat/forge-lines.toml`
now uses schema version 2 with `[[line]]`, `forgeMajor`, `minimumForge`,
`recommendedForge`, `module`, `status`, explicit published patches, and separate
compile/runtime/formal-matrix evidence arrays. Forge 66 remains explicitly
unreleased and unclaimed.

`scripts/validate-compat.py --json` passes on the host's Python 3.9 using its
dependency-free TOML fallback parser. It reports Forge 65 as required, 11
published 65.x patches, four lifecycle-smoke-verified patches, and
`formalMatrixComplete=false`.

The same two real Forge GameTests were rerun on every cached floor/recommended
candidate that is available locally:

- `zero_human_dedicated_server_chunk_and_respawn`: Forge 65.0.0, 65.0.8,
  65.0.9, and 65.1.0 — 1/1 each.
- `auto_presence_on_human_login`: Forge 65.0.0, 65.0.8, 65.0.9, and 65.1.0
  — 1/1 each.

This is 8/8 targeted lifecycle tests, not a formal compatibility matrix: the
unavailable 65.0.1–65.0.7 patches and all chat, movement, menu, save/restart,
real-client, model, and statistical gates remain unverified/`NOT_RUN`.

The current exact product JAR was then smoke-tested again in
`e2e/results/no-commit-dirty/20260806T144411Z-e2e364577cc21c8` on Forge 65.1.0:
expected and loaded SHA-256 both equal
`f1bf8301ec09c25934c0c396aa4be849de431b4e6f0b7ae8c47256c232df0440`, SQLite
loaded only from Jar-in-Jar, lifecycle and clean exit passed, and
`functionalAiClaim=false`.

## 2026-08-06 continuation checkpoint: zero-human spawn deadlock correction

The first real Forge 65.1.0 zero-human run after the login-anchor grace fix
found a production-side blocking defect. The server thread entered
`PrepareSpawnTask.Ready.spawn -> ServerLevel.waitForEntities` and remained
there for more than four minutes; no GameTest timeout or model request caused
it. The same stack reproduced when the test requested an unanchored spawn
directly, so the previous zero-human gate was recorded as a real failed/hung
attempt and the process was stopped rather than reported as a pass.

Production correction:

- `AnchoredPlayerSpawn` now prepares the bounded anchor chunk asynchronously,
  locates the vanilla-safe player position only after that load completes, and
  hands off through the ordinary `ServerPlayer` and
  `PlayerList.placeNewPlayer` lifecycle without the synchronous
  `waitForEntities` call.
- `PendingPlayerSpawn` uses this same non-blocking path for both a real login
  anchor and the no-human fallback. A saved player position/dimension is
  preferred; otherwise the world's vanilla respawn position is used. The
  40-tick unanchored login grace and replacement-only-before-ACTIVE rule are
  unchanged.
- No direct block/entity/world mutation was added; the body remains a real
  `ServerPlayer`, with normal player-data loading, connection pump, inventory,
  and death/respawn lifecycle.

Directed verification after this correction:

- Focused JVM tests (`PendingPlayerSpawnTest`, `FollowEntitySkillTest`, and
  `EmergencySurvivalControllerTest`): PASS.
- Real Forge 65.1.0
  `zero_human_dedicated_server_chunk_and_respawn`: PASS 1/1 in 1.670 s. The
  log shows `MCAI[embedded]` joining with no human client, the remote
  simulation/death/respawn assertions completing, and clean shutdown.
- Real Forge 65.1.0 `auto_presence_on_human_login`: PASS 1/1. The human and
  `[AI]` body were both present at the bounded safe positions.
- The current full package gate passed 986 JVM tests, 0 failures, 0 errors, and
  2 skips with `jar`, `jarJar`, `verifyReleaseJar`, `e2eClientJar`, and
  `e2eOracleJar`. Product SHA-256 is
  `f1bf8301ec09c25934c0c396aa4be849de431b4e6f0b7ae8c47256c232df0440`; slim
  audit SHA-256 is
  `b54747c5495d9c5db99881250e823342e518b71d44baa6c1522a59dd755bcb28`.
- Exact-JAR dedicated-server smoke
  `e2e/results/no-commit-dirty/20260806T140920Z-e2eb3454cd62bfc` passed on
  Forge 65.1.0 with server/Actor/Observer product copies matching that
  product hash, SQLite loading from Jar-in-Jar, clean lifecycle, and
  `functionalAiClaim=false`. Python E2E audit tests: 15/15 PASS, including
  compatibility declaration and legacy-schema rejection checks.

The MiMo credential is still independently observed as HTTP 401 and this
macOS host still lacks the Linux/Xvfb Actor + Observer environment. Real model
chat-to-action, formal mutation, M0--M4, unseen-seed Hardcore, and
professional-companion claims remain `NOT_RUN`.

## 2026-08-06 continuation checkpoint: login-time spawn-anchor race fix

The next lifecycle symptom under review was a body that could be present on a
dedicated server but not beside the player who opened a single-player world.
`CompanionRuntime` is allowed to start an unanchored vanilla `PendingPlayerSpawn`
at `ServerStartedEvent` so a server with zero human players still has a real
body.  When the first human then logged in, `ensureSpawnNear` treated the
`PREPARING` state as final and left that preparation at its saved/world spawn.
This was a real ordering gap, not a model decision or a teleport failure.

Production correction:

- `PendingPlayerSpawn` now exposes only an `anchored()` lifecycle bit.
- `AiPlayerManager.ensureSpawnNear/requestSpawn` replaces an unanchored,
  still-pending preparation with a new bounded `SafeCompanionSpawnLocator`
  preparation around the logged-in player.  The replacement is limited to
  the pre-login `PREPARING` window; an `ACTIVE` body is never moved by login.
- `PendingPlayerSpawnTest` records the source contract for this narrow branch.

Directed verification after this fix:

- Focused Gradle tests for `PendingPlayerSpawnTest`, `FollowEntitySkillTest`,
  and `ConversationCommitmentSourceContractTest`: PASS.
- Full current package: 985 JVM tests, 0 failures, 0 errors, 2 skips; Forge
  65.0.0 compile floor, `jarJar`, `verifyReleaseJar`, `e2eClientJar`, and
  `e2eOracleJar` passed. Product SHA-256 is
  `85c227c3d3161f465499cbb2171b94658ee9dfdba5917f4fc71251705fdeea60`; the
  slim audit artifact is `6f36a8218e0248b2b9bc193509a66fbbd11b3505491a9d15479d9d46300e461d`.
- Exact product smoke
  `e2e/results/no-commit-dirty/20260806T134602Z-20260806-login-anchor-final-smoke`
  passed all dedicated-server lifecycle/hash/SQLite checks on Forge 65.1.0
  with `functionalAiClaim=false`. The real Forge `auto_presence_on_human_login`
  GameTest also passed 1/1 on the current source.
- No gate failed in this slice. Real-model chat-to-action, rendered client,
  M1--M4 and professional-companion gates remain `NOT_RUN`/blocked by the
  observed provider HTTP 401 and missing Linux/Xvfb Actor+Observer worker.

## 2026-08-06 continuation checkpoint: delayed follow binding no-op fix

The remaining field symptom being addressed in this slice is the truthful
acknowledgement followed by no movement during `follow_entity`.  The direct
execution race was in `FollowEntitySkill`: its documentation promised a
bounded search after a short line-of-sight loss, but `preconditions` and
`start` rejected any target that was not still in the newest camera frame when
the model response arrived.  A player turning a corner during provider
latency therefore produced `follow_entity.target_not_currently_visible`
before the skill could issue even one legal input.

Production correction:

- `src/main/java/dev/mcai/companion/skills/core/FollowEntitySkill.java` now
  accepts only a recent, same-dimension, non-hostile target from the exact
  fair authored sample; it binds the private entity identity and enters the
  existing bounded `SEARCHING` phase when the target is temporarily absent.
  The normal `lostGraceTicks` timeout, first-person scan, visibility filter,
  and no-teleport/no-hidden-position rules remain unchanged.
- `src/test/java/dev/mcai/companion/skills/core/FollowEntitySkillTest.java`
  now verifies that a recent target leaving the current view starts a
  stop-and-scan search instead of being rejected, while stale observation
  windows and forged indices remain rejected.

Directed verification:

- `./gradlew test --tests dev.mcai.companion.skills.core.FollowEntitySkillTest
  --tests dev.mcai.companion.communication.ConversationCommitmentSourceContractTest
  --rerun-tasks --no-daemon`: PASS.
- Full package after this source change: 984 JVM tests, 0 failures, 0
  errors, 2 skips; `jarJar`, `verifyReleaseJar`, `e2eClientJar`, and
  `e2eOracleJar` passed. The final rebuild used the Forge 65.0.0 API floor
  (the produced bytes are identical to the previously smoked artifact).
  Product SHA-256 is
  `e82c63f4b303236621b202adb9d77e22fea43e1d963c926503a30e8b32c352d1` and
  slim SHA-256 is
  `dd55f73f33dabae8b041395735853358a0582c240281a4d0ed4eeeee3358fa90`.
- Exact-JAR smoke
  `e2e/results/no-commit-dirty/20260806T133127Z-20260806-follow-search-final-smoke`
  passed all dedicated-server lifecycle/hash/SQLite checks on Forge 65.1.0;
  `functionalAiClaim=false` remains explicit.
- The no-human Forge GameTest was rerun from the same source after this change:
  `zero_human_dedicated_server_chunk_and_respawn` passed 1/1 in 1.840 s on
  Forge 65.1.0. It verified body creation on a dedicated server with no human
  client, simulated-chunk/lifecycle continuity, ordinary death and respawn,
  and clean shutdown; no model credential or model decision was involved.
- The same production source also compiled against the declared Forge API
  floor 65.0.0 after the follow correction: `compileJava` PASS. This is a
  compatibility compilation check, not a runtime matrix or formal milestone.

The provider credential remains independently observed as HTTP 401 and the
macOS host still lacks Linux/Xvfb Actor + Observer infrastructure.  Therefore
real model chat-to-action, formal mutation, M0--M4, unseen-seed Hardcore, and
professional-companion claims remain `NOT_RUN`.

## 2026-08-06 continuation checkpoint: formal gate entrypoints and preflight evidence

The current gameplay blocker is unchanged: the configured MiMo credential
still fails the real provider probe with one HTTP 401, and this macOS host has
no Linux/Xvfb environment for the real Actor + Observer clients. Therefore
real model chat-to-action, movement, inventory, mutation, M0--M4, and hidden
Hardcore gates remain `NOT_RUN`; the recent Enderman/Slime GameTests are only
fair local survival evidence.

This continuation fixed two audit/infrastructure gaps rather than weakening a
gate:

- `e2e/orchestrator.py` now creates and prints an immutable functional run
  before preflight. A blocked launch writes `manifest.json` with
  `status=NOT_RUN`, `functionalAiClaim=false`, a secret-safe model summary,
  `functional-preflight.json`, and `infrastructure-error.json`. The actual
  run `e2e/results/no-commit-dirty/20260806T131041Z-20260806-preflight-archive`
  records Darwin, missing Linux/Xvfb/model prerequisites and no credential
  value or path.
- `e2e/formal_gates.py` and `build.gradle` now expose all 17 named formal
  entries from the plan (`e2eFunctional`, `e2eRendered`, `e2eChat`,
  `e2eMovement`, `e2eInventory`, `e2eRestart`, `e2eXaero`, `e2eM1`,
  `e2eM2`, `e2eM3`, `e2eM4Shard`, `aggregateHiddenSeeds`, `soak24h`,
  `soak100h`, `recordHumanBaseline`, `naturalnessReport`, `mutationGate`).
  The first five invoke the real external-client runner; the others record
  explicit `NOT_RUN` until their gate-specific scenarios exist. A dirty
  checkout, preflight failure, or absent verdict cannot become `PASS`.

Verification:

- Python orchestrator tests: 8/8 PASS; CI evidence tests remain green.
- `python3 -m py_compile e2e/orchestrator.py e2e/formal_gates.py`: PASS.
- `./gradlew tasks --all`: all 17 formal task names present.
- `./gradlew e2eChat -Pforge_runtime_version=65.1.0`: expected non-zero
  `NOT_RUN`; archived gate record at
  `e2e/results/formal-gates/no-commit-dirty/20260806T131501Z-e2eChat.json`.
- The exact packaged-JAR dedicated-server smoke was rerun after these
  entrypoints as `20260806T131656Z-20260806-formal-entry-final-smoke-2` and
  passed all lifecycle/hash/SQLite checks on Forge 65.1.0 with product SHA
  `0ab57f138c9cefa48cf07bae3ffb49368fbaff354fdc519049243ee6b3a054e6`;
  `functionalAiClaim=false` remains explicit.
- After the truthful immediate-task acknowledgement change, the final
  package gate passed with 984 JVM tests (0 failures, 0 errors, 2 skips),
  product SHA
  `f7a2303e5288965b6b204d31a43f2f662f1e4aa01391a5ab2bac2222af971955`, and
  slim SHA
  `87aa0a75f81622f2a2b5c06ead9655599dc396ebfe134fdcae7196db73a8c702`.
  Exact-JAR smoke `20260806T132222Z-20260806-commitment-fix-final-smoke`
  passed all lifecycle/hash/SQLite checks on Forge 65.1.0; it remains
  `functionalAiClaim=false`.

Next action is still a real Linux/Xvfb run with a valid, authorized model
credential; no local script result will be promoted to a gameplay or
professional-companion claim.

## 2026-08-06 continuation checkpoint: Enderman footwork ownership fix and final rebuild

The first post-package Enderman rerun failed honestly despite a long shield
window: `maximumShieldTicks=131` but `maximumDistanceFromStart=0.019` and no
footwork bit. Three consecutive reruns reproduced this, proving that the
failure was real. The cause was two-sided: the top-level attack-cooldown branch
called the ordinary hazard guard, whose `stop()` erased the combat movement
lease, and an Enderman teleport removed the target from the current semantic
sample before the body could request the next backstep.

The production controller now keeps a bounded direction from the last fair
close hostile observation (no entity id or hidden position), preserves the
movement lease while the shield is held during combat cooldown, and reissues a
short first-person-certified separation input for at most 40 ticks after a
legal attack. If the local map cannot certify an adjacent cell, a small
collision-authoritative backstep is allowed while grounded or outside a fall
hazard; no position, block, reach, or entity state is written directly. The
physical gate now measures maximum observed excursion rather than only net
displacement, so a legal step followed by knockback/teleport cannot be erased
by returning to the starting cell.

Latest real Forge 65.1.0 evidence, with no model and no test gateway:

- `real_emergency_enderman_defense`: PASS 1/1 after the fix, then PASS on
  three consecutive independent reruns (3/3).
- `real_emergency_slime_defense`: PASS 1/1 after the same source rebuild.

Latest package/lifecycle evidence after this source change:

- Gradle package gate: PASS, 983 tests, 0 failures, 0 errors, 2 skipped;
  `verifyReleaseJar`, `e2eClientJar`, and `e2eOracleJar` passed.
- Product JAR SHA-256:
  `0ab57f138c9cefa48cf07bae3ffb49368fbaff354fdc519049243ee6b3a054e6`.
- Slim audit JAR SHA-256:
  `4fa0f9a2042abf2cbc1e1bd052b631e5f30c8716350a1c1d61da8d60bc62b18d`.
- Python E2E: 10/10 PASS.
- Exact product server smoke:
  `e2e/results/no-commit-dirty/20260806T130153Z-20260806-offline-emergency-final-jar`;
  all lifecycle/hash/SQLite checks passed and `functionalAiClaim=false`.
- Functional preflight at `2026-08-06T13:03:21Z`: `ready=false` on Darwin;
  Xvfb/Linux worker and model environment are absent. No secret value or
  secret path was written to the report.

The configured MiMo probe remains a single HTTP 401, and this macOS host
still lacks Linux/Xvfb Actor+Observer infrastructure. Real model
chat-to-action, navigation, inventory, mutation, M0--M4, and statistical
Hardcore gates remain `NOT_RUN`. The physical gates above are local fair
survival evidence only, not a professional-companion or two-hour completion
claim.

## 2026-08-06 continuation checkpoint: shared interaction release and attack-cooldown regression

The first corrected no-model Slime run exposed a real production bug rather
than a test-harness issue: the offline runtime finalizer called
`ServerOwnedInteractionSkillActuator.quiesceNow()` every tick. That actuator
shares the same fair player-use path as the emergency shield, so its release
packet immediately cancelled the shield selected by the local survival lane.
The test therefore observed a real hostile, but `evidence=27` and
`maximumShieldTicks=0`. The runtime now releases menu/boat/minecart ownership
offline, while leaving interaction ownership intact whenever emergency
survival has intervened; the ownership-change callback still quiesces stale
interaction state when control actually changes.

The Enderman rerun then exposed a separate vanilla timing race: after a recent
attack, an Enderman could teleport just outside the current bounded scan before
the next decision, so the body attacked again instead of holding the shield
during the sword/axe recharge window. `EmergencySurvivalController` now keeps
only a bounded recent local attack timestamp and raises the shield for the
conservative ten-tick recharge floor, including when the hostile temporarily
leaves the current sample. No entity tracking, hidden coordinates, teleport,
direct block mutation, or model shortcut was added.

Latest physical evidence (real Forge server GameTests, no model and no test
gateway):

- `real_emergency_slime_defense`: PASS 1/1 on Forge 65.1.0.
- `real_emergency_enderman_defense`: PASS 1/1 on Forge 65.1.0 after the
  shared-use-release and recent-attack cooldown fixes.

Latest package and lifecycle evidence:

- Gradle package gate: PASS, 983 tests, 0 failures, 0 errors, 2 skipped;
  `verifyReleaseJar`, `e2eClientJar`, and `e2eOracleJar` all passed.
- Product JAR SHA-256:
  `6c39604b9ee1da214d513c118b2076cb4bd1489cae10b61732bec40421c69253`.
- Slim audit JAR SHA-256:
  `01c24578c6989b6f571ee97b403fa5344316c8c71010c7175a93ce64c424b2b3`.
- Python E2E harness: 10/10 PASS.
- Exact product dedicated-server smoke:
  `e2e/results/no-commit-dirty/20260806T124225Z-20260806-offline-emergency-fix`;
  clean lifecycle, exact server/Actor/Observer hash equality, Jar-in-Jar
  SQLite load, join/removal, and graceful exit all passed. This remains
  `functionalAiClaim=false` and is not a model gameplay run.

The real configured MiMo probe still returned one-request HTTP 401, and this
macOS host still has no Linux/Xvfb Actor+Observer environment. Therefore real
model chat-to-action, movement, inventory, mutation, M0--M4, and all
statistical Hardcore gates remain `NOT_RUN`. These two GameTests are local
fair-survival evidence only; they do not justify a professional companion or
two-hour completion claim.

## 2026-08-06 continuation checkpoint: no-model physical emergency regression

The focused Forge GameTest `mcai_companion:real_emergency_slime_defense` was
rerun without installing a test gateway or model fixture. It passed on Forge
65.1.0, so the offline lane now has a real server-side physical regression:
with the high-level gateway unavailable, the body can still use fair owned
items and local 20 TPS survival control against an actual hostile. This is
not a model gameplay result and does not prove chat, navigation, or M1--M4.

Verification after the test-only regression change:

- `runGameTestServer -Pforge_compile_version=65.1.0
  -Plive_model_selector=mcai_companion:real_emergency_slime_defense`: PASS,
  one required test, Forge 65.1.0, normal real-time server tick.
- Full JVM/package gate: PASS, 981 tests, 0 failures, 0 errors, 2 skipped;
  `verifyReleaseJar`, `e2eClientJar`, and `e2eOracleJar` passed.
- Product JAR SHA-256 remains
  `2b4eff1c6b8c191a7a6474f4cefa35393a76163c0ad9b8f57968d0eabfe4df60`;
  slim JAR remains
  `d1607f6f5d5f19a78970af7173b9d339685dff4de2be38742596d42f6204a4c0`.
- Python E2E evidence harness: 10/10 PASS.

The real configured MiMo provider still returns one-request HTTP 401, and this
macOS host still lacks the Linux/Xvfb Actor+Observer infrastructure. Formal
M0--M4, real chat-to-action, movement, inventory, and Hardcore gates remain
`NOT_RUN`; the focused GameTest is inner-loop physical evidence only.

## 2026-08-06 continuation checkpoint: offline emergency lane when model is unavailable

The previous runtime returned before behavior arbitration whenever the verified
model gateway was unavailable. That was too broad: it correctly stopped
high-level skills and speech, but also prevented the local fair 20 TPS survival
controller from reacting to a nearby hostile, critical food/health state, fall,
fire, or water hazard. A rejected credential therefore looked like a body that
stood still and died.

`CompanionRuntime` now records `modelControlEnabled` once per server tick and
always runs the emergency survival candidate. When the gateway is unavailable,
the arbiter admits no active model skill and no idle-equipment lane; emergency
ownership is forced for hostile/physical-contact threats so a stale skill cannot
claim a threat while its tick is paused. Menu, boat, and minecart controls are
released offline. The emergency lane still uses only the body’s owned items and
the current fair first-person frame, and it never starts a goal, calls the
model, or emits speech. When the gateway is verified again, the normal three
lane priority order resumes.

Changed files:

- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/test/java/dev/mcai/companion/runtime/ModelBootstrapSourceContractTest.java`

Verification:

- Focused source/emergency tests: PASS.
- Full Gradle package gate on Forge 65.1.0: PASS; 981 tests, 0 failures, 0
  errors, 2 skipped; `verifyReleaseJar` PASS.
- Product JAR SHA-256: `2b4eff1c6b8c191a7a6474f4cefa35393a76163c0ad9b8f57968d0eabfe4df60`.
- Exact-JAR dedicated-server smoke:
  `e2e/results/no-commit-dirty/20260806T121522Z-20260806t2115offline`, PASS
  for lifecycle/loading only; `functionalAiClaim=false`.
- Python E2E harness tests: 10/10 PASS.

This does not prove a real provider gameplay run or promote any formal M0–M4
gate. The configured MiMo credential still returns one-request HTTP 401, and
the macOS host still lacks the Linux/Xvfb Actor+Observer infrastructure.

## 2026-08-06 continuation checkpoint: MiMo authentication header

The current persisted MiMo credential still fails the real one-request
capability probe with `AUTHENTICATION` / HTTP 401, so no real gameplay claim is
made. Before accepting that as only an external credential problem, the
provider contract was checked against Xiaomi's current official examples:
MiMo requires `api-key`, whereas the gateway previously sent only
`Authorization: Bearer`. The production gateway and capability probe now use
`api-key` for validated `*.xiaomimimo.com` endpoints and preserve Bearer for
other providers. A focused four-test provider/authentication contract suite
passes, and the corrected real probe still returns one safe 401 request; this
indicates the stored credential or Token Plan entitlement itself must be
replaced/renewed before live gameplay can resume.

Changed files: `src/main/java/dev/mcai/companion/model/ModelApiAuthentication.java`,
`src/main/java/dev/mcai/companion/model/JdkModelGateway.java`,
`src/main/java/dev/mcai/companion/model/JdkProviderCapabilityProbe.java`,
`src/test/java/dev/mcai/companion/model/ModelApiAuthenticationTest.java`, and
the platform facts/this checkpoint. No credential value was recorded.

Evidence:

- Focused gateway/probe/authentication tests: 4 tests, 0 failures.
- Real provider capability probe after the fix: 1 request, HTTP 401,
  `AUTHENTICATION`; formal real-client and M1–M4 gates remain `NOT_RUN`.
- Xiaomi's official documentation confirms the `api-key` header and the
  `tp-...` Token Plan credential format.

Recovery command after the user supplies a valid, authorized credential:

```text
env MCAI_LIVE_CAPABILITY_TEST=true MCAI_LIVE_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1 MCAI_LIVE_MODEL=mimo-v2.5 ./gradlew test --tests dev.mcai.companion.model.LiveCapabilityVerificationTest --no-daemon
```

## 2026-08-06 continuation checkpoint: local credential-restore retry

The exact server smoke showed the body joining while the model lane stayed
fail-closed with `startup_model_unavailable_invalid_configuration`. The
ordinary startup bootstrap now distinguishes that local, no-credential case
from provider authentication/billing/rate-limit failures. It retries only the
local Keychain/Secret-Service/environment unlock after a bounded 100-tick
interval; it never repeats a billable provider probe after a credential is
present but rejected. This keeps an online body honest and lets a desktop
keychain that becomes available after world load recover without forcing the
player to re-enter the key.

Changed files: `src/main/java/dev/mcai/companion/runtime/ModelBootstrapCoordinator.java`
and `src/test/java/dev/mcai/companion/runtime/ModelBootstrapCoordinatorTest.java`.
The targeted bootstrap suite passes, including the new local-only retry case.
Final-source verification then passed `981 tests / 0 failures / 0 errors /
2 skipped`, `verifyReleaseJar`, Python E2E `10/10`, and exact-JAR server-smoke
run `20260806T115007Z-e2ee4adb9c882af` on Forge 65.1.0. The installable
product JAR hash is
`7e1977ad24aed0809be72bf5c773d2f31b15c44fd25ae50697f529caed5f0eb9`; all
staged server/Actor/Observer product copies matched it. The smoke verdict is
still lifecycle-only with `functionalAiClaim=false`.
The functional model/client gate remains `NOT_RUN` on this macOS host because
Xvfb/Linux and a valid configured model environment are still unavailable.
The one-shot opt-in MiMo capability probe was also run through the real
provider path and returned `AUTHENTICATION`/HTTP 401 after one request; this is
recorded as a provider failure, not converted into a gameplay result.
The current source also compiles against Forge 65.0.0, 65.0.8, and 65.0.9;
this is a compatibility compilation check only, so the formal runtime patch
matrix remains `NOT_RUN`.

## 2026-08-06 continuation checkpoint: functional E2E preflight boundary

The exact real-client vertical-slice artifacts are now build-verified: the
production product JAR and the test-only `mcai-e2e-client` and
`mcai-e2e-oracle` JARs compile against Forge 65.1.0, and the test classes are
not inputs to the production JAR. The functional orchestrator now has a
read-only `preflight` command and reuses the same report before launching
Xvfb, the dedicated server, or either client. It reports missing Linux/Xvfb,
Java 25, or model credentials without exposing secret values or paths; a
non-ready report is infrastructure `NOT_RUN`, never gameplay PASS/FAIL.

定向验证：

- `./gradlew e2eClientJar e2eOracleJar -Pforge_compile_version=65.1.0`：PASS。
- `python3 -m unittest discover -s e2e -p 'test_*.py' -v`：10/10 PASS。
- 当前 macOS 主机没有 Xvfb，且当前进程没有模型凭据；真实 Actor + Observer
  功能门禁仍保持 `NOT_RUN`，不能用预检替代它。

已改文件：`e2e/orchestrator.py`、`e2e/test_orchestrator.py`、
`e2e/README.md`、`.github/workflows/real-client-functional-e2e.yml`、
`docs/IMPLEMENTATION_STATUS.md`、`docs/progress/GOAL_STATE.json`、
`changelog.txt`。随后完整 Gradle 门禁为 `tests=980 failures=0 errors=0
skipped=2`，`verifyReleaseJar` 通过；当前生产 JAR SHA-256 仍为
`e67bd10048cb65495920cdd91939765703ecb47c01362a2a8367d460bb44dbcc`，
两个测试 JAR 也均成功生成。下一步是在可用的真实 Linux/Xvfb/有效模型
凭据上执行 `functional`；在此主机只保存明确的基础设施证据。

## 2026-08-06 continuation checkpoint: Enderman gate failure and directional shield fix

The first final-source `real_emergency_enderman_defense` rerun failed
honestly at tick 236: the real Enderman encounter killed the headless player
while the local survival state was `BRACING_FALL`. The Slime comparison gate
passed, so this was a specific teleport/knockback and shield-facing failure,
not a lifecycle or packaging failure. The emergency guard now uses the fair
recent-damage direction to face the attacker directly before falling back to
the bounded scan; it does not add entity tracking, hidden coordinates,
teleportation, or direct world writes.

Verification after the fix:

- `EmergencySurvivalControllerTest`: PASS.
- `real_emergency_enderman_defense`: 5/5 consecutive PASS on Forge 65.1.0.
- `real_emergency_slime_defense`: PASS on Forge 65.1.0.

The added diagnostic failure context remains in the physical gate so any
future death reports body position, velocity, ground state, and the fair
target position. Full JVM/package verification is the next action. Formal
M0–M4, random Hardcore, and live MiMo gameplay gates remain NOT_RUN.

## 2026-08-06 continuation checkpoint: player input wakes a paused brain

Root cause confirmed: `BrainOrchestrator.prioritizePlayerConversation()` only
cleared `WAITING_FOR_PLAYER` when a planner request was already in flight. A
short follow-up such as `走啊` could therefore receive a conversation reply
while the gameplay brain stayed paused indefinitely. The method now clears the
pause, resets the no-action streak/backoff, and cancels only the superseded
planner request; `CompanionConversationCoordinator.submit()` invokes it for
every new player utterance after the gateway-ready check. This does not choose
a skill or write the world.

Added regression coverage:

- `BrainOrchestratorTest.playerConversationWakesPlannerAfterAskPlayer`.
- Targeted `BrainOrchestratorTest` run: PASS, 2 tests, `BUILD SUCCESSFUL`.

The targeted physical follow/combat validation is now complete on the final
source; the Enderman gate initially exposed the separate shield blind spot
recorded below. Formal M0–M4 and live MiMo gates remain NOT_RUN; the previously
stored provider credential still independently returns HTTP 401 `invalid_key`.

The full post-fix JVM/package gate then passed:

- `./gradlew test jar jarJar verifyReleaseJar -Pforge_compile_version=65.1.0`:
  `tests=980 failures=0 errors=0 skipped=2`, `BUILD SUCCESSFUL`.
- Installable product JAR SHA-256:
  `e6e1e33729fdc955dba9e7637d9633fc2a47c0574191190ecea7a1bc87683f0b`.
- Slim audit JAR SHA-256:
  `45c506ecfa7d2e921f5b01c63423436194b628b4bd40364a6289c6b6f03d8e59`.
- Full JAR contains `META-INF/mods.toml` and Jar-in-Jar SQLite; slim contains
  no mod descriptor. `verifyReleaseJar` passed.

The rebuilt exact packaged-JAR dedicated-server smoke then passed as run
`20260806T111344Z-e2e0923c2655398` on Forge 65.1.0. Server, Actor, and Observer
copies all matched the new product SHA-256 above; clean startup, SQLite
Jar-in-Jar loading, headless join, and graceful lifecycle were verified.
`functionalAiClaim=false` remains explicit.

After the directional shield fix, the final-source package gate was rebuilt
again. The current installable product JAR is
`e67bd10048cb65495920cdd91939765703ecb47c01362a2a8367d460bb44dbcc`, and the
slim audit JAR is
`9e8ceade036af59b7c972077fe456edb6f860a345f7c26f1d0a571c2120b2ca3`.
The exact-JAR dedicated-server smoke
`20260806T112409Z-e2eae638ec04412` passed on Forge 65.1.0 with exact product
hash equality, Jar-in-Jar SQLite, headless join and graceful removal;
`functionalAiClaim=false` remains explicit.

The current post-fix source also compiles successfully with
`-Pforge_compile_version=65.0.0`, `65.0.8`, and `65.0.9`; the release metadata
continues to advertise the bounded `[65.0.0,66.0.0)` Forge range.

## 2026-08-06 continuation checkpoint: hostile reacquisition

Root cause confirmed: the emergency survival lane only reacquired nearby
melee targets and dropped its lease when a hostile moved or naturally
teleported outside melee reach. That could leave the body standing while the
model spoke. `EmergencySurvivalController.java` now retains only a bounded
timestamp from a fair first-person observation (no entity handle or hidden
position), refreshes it while a nearby hostile remains visible, and continues
the shield/scan lease through a natural teleport. `EmbodimentGameTests.java`
defines the honest physical outcome as target defeat or an eight-block
safe-stand-off with forty ticks of stable target health and no body damage.
The focused Forge 65.1.0 Enderman and Slime tests now pass 1/1; the post-change
JVM suite is 979/0/0/2 and `verifyReleaseJar` passes with the installable full
Jar boundary recorded below. Formal M0-M4 gates remain NOT_RUN.

## 2026-08-06 follow-up verification: physical defense, compatibility, and release artifact

The follow-up source changes were verified without claiming a model response:

- `real_emergency_enderman_defense`: Forge 65.1.0, 1/1 required, 1.493 s;
  the target was defeated or held at least eight blocks away with stable health
  and no body damage for forty ticks. This is local survival evidence only.
- `real_emergency_slime_defense`: Forge 65.1.0, 1/1 required, 1.797 s.
- Forge compatibility compilation: 65.0.0, 65.0.8, and 65.0.9 all passed;
  metadata remains `[65.0.0,66.0.0)`.
- `zero_human_dedicated_server_chunk_and_respawn`: Forge 65.1.0, 1/1
  required, 1.952 s; no human client or model credential was used.
- Exact packaged-Jar `server-smoke` run
  `20260806T110027Z-e2e9c51eea93c54` passed on Forge 65.1.0: clean dedicated
  startup, identical product SHA-256 in staged copies
  (`fbfe0c4b2df5f2f93fdcd5fd4c2158b78409ddb557e48824533dc4d340856f67`),
  SQLite loaded from Jar-in-Jar, headless player joined, and graceful removal;
  `functionalAiClaim=false`.
- JVM suite: 979 tests, 0 failures, 0 errors, 2 skipped.
- `jarJar verifyReleaseJar`: passed.
- The slim archive is now explicitly non-installable (no
  `META-INF/mods.toml`); only the unclassified Jar carries the Mod descriptor,
  preventing an accidental duplicate when a user copies build output.
- Repeated planner no-action backoff is now capped at 2 seconds (down from
  10 seconds), while the model remains the only source of ordinary skill
  selection; the existing repeated-no-action BrainOrchestrator test passes.

The current full product artifact is
`build/libs/mcai_companion-0.1.4-dev-mc26.2.jar` with SHA-256
`fbfe0c4b2df5f2f93fdcd5fd4c2158b78409ddb557e48824533dc4d340856f67`;
the slim audit artifact SHA-256 is
`38e7eb36f9a186d96d38c02255a70f4d4584e91af811cc67c97643b273b5cf86`.
The full artifact contains `META-INF/mods.toml` and the slim artifact does
not; `verifyReleaseJar` passed. The JVM suite is 979 tests, zero failures,
zero errors, and two skips.
These are inner-loop and artifact-integrity results only. Formal M0-M4,
random Hardcore statistics, and live MiMo gameplay remain unpromoted because
the supplied provider credential independently returns HTTP 401 `invalid_key`.

## 2026-08-06 follow-up: damage refresh and speech-only planner recovery

The remaining field-facing symptoms had two separate causes. A hostile hit
was recorded as a directional cue, but the fair entity list could remain on
the previous 4 Hz semantic sample for several ticks; this made a newly
arrived Zombie look absent to the local combat lane. The server damage event
now requests one bounded next-tick `SEMANTIC_REFRESH`, while the sampler still
uses only the companion's own distance/FOV/block-clip view. This does not
identify an occluded attacker or grant hidden coordinates.

The second cause was a valid model `CONTINUE`/`REPLAN` with optional speech but
no started skill. After two consecutive no-action decisions the next planner
request now carries the trusted `planner_no_action` correction, requiring the
model to choose an admitted actionable skill when one is currently possible;
it still cannot choose a Java command, teleport, or direct world mutation.

Files changed in this follow-up:

- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
  requests a fair semantic refresh after damage.
- `src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`
  preserves the no-action correction for the next request.
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
  documents the trusted action-required correction.

定向门禁：`BrainOrchestratorTest` and `MinecraftPlannerInputFactoryTest` pass;
the zero-human dedicated-server chunk/death/respawn GameTest passes 1/1 in
2.042 seconds on Forge 65.1.0. The provider credential remains externally
invalid (HTTP 401), so another real MiMo gameplay gate is intentionally not
claimed until a replacement key is verified.

## 2026-08-06 exact MiMo chat-to-victory rerun: functional PASS, timing invalidated

The second exact configured-`mimo-v2.5` continuous chain completed the full
single-player chat-to-victory route through the production model gateway and
the same `ServerPlayer` body. The only human input was the ordinary initial
player chat; the test player disconnected immediately afterward. The model
then selected and physically completed:

```text
craft_recipe (14 Eyes)
-> return_via_verified_portal
-> triangulate_stronghold_search_area
-> reach_observed_stronghold
-> search_stronghold_portal_room (142 visited stations)
-> activate_observed_end_portal
-> find_and_enter_observed_portal
-> fight_ender_dragon
-> find_and_enter_observed_portal (return)
```

Physical evidence in the same run:

```text
End portal: 12 observed frame insertions, 9 vanilla portal blocks
End fight: 1 cage bar mined, 3 recorded shots, 46 recorded melee actions
Advancements: The End?, Take Aim, Monster Hunter, Free the End
Terminal: dragon return-portal entry and normal AI disconnect
Forge GameTest: 1 required test passed
```

The run is `PASS` for this exact functional chain. It is not a formal M2/M4
or release result. The macOS host suspended twice before the caffeinated
rerun guard was installed; process wall time was about 44:55 while the
GameTest's real-time 20-TPS clock reported 14.27 minutes. Therefore this run
does not provide clean performance or two-hour timing evidence. The next
live check is a real ordinary-chat combat/defense gate under a host-awake
guard, followed by a clean exact chain if that gate exposes a new causal
failure.

## 2026-08-06 real ordinary-chat surprise-defense rerun: FAIL at provider auth

The host-awake real-time rerun used the normal player chat event and the
production `real_player_chat_to_surprise_zombie_defense` fixture. It did not
silently pass a talk-only body:

```text
initial chat acknowledgement: received
provider request: HTTP 401 / invalid_key
local damage response: WARNING_REACTING -> COUNTERATTACKING -> RETREATING
zombie health: 20.0 -> 0.0
body displacement: 4.393 blocks
body health: 20.0
```

The gate correctly returned `FAIL` because `modelResponded=false`; local
emergency survival is not model evidence. SQLite recorded the causal failure
as `model_failure.authentication`. A direct endpoint check using the stored
credential (with the credential withheld from output) independently returned
HTTP 401 `invalid_key`, so this run is an external credential failure, not a
request-body or movement no-op. The stored item was present in macOS Keychain,
but its provider-side authorization is no longer valid.

The runtime correction now applied:

- an authentication failure clears the verified gateway delegate;
- cached capability metadata is removed while the non-secret URL/model remain;
- the next world cannot silently restore the stale capability cache;
- the brain emits an explicit Chinese `[AI]` setup message and enters safe idle;
- the ordinary conversation path reports the same actionable status instead
  of saying only “没听清”.

Directed JVM verification after this correction:

```text
BrainOrchestratorTest: PASS
SwitchableModelGatewayTest: PASS
ModelProfileStoreTest: PASS
```

The provider credential must be replaced before another real-MiMo gameplay
gate can be honestly run. Formal M0-M4 and all release gates remain `NOT_RUN`.

The focused no-human-server regression was rerun after the cache/auth changes:

```text
mcai_companion:zero_human_dedicated_server_chunk_and_respawn
PASS / 1 required / 1.634 s / Forge 65.1.0
```

It spawned the real headless `ServerPlayer` with no human online, exercised
the out-of-player simulation window and vanilla death lifecycle, and shut
down without a forced chunk ticket. This remains an inner-loop body gate,
not the formal M0 24-hour or dual-client result.

## 2026-08-06 continuous rerun: partial crystal retreat observation

The portal-accounting correction passed the exact configured-`mimo-v2.5`
continuous chain. The same body had one Eye legitimately recovered from a
trace throw, and the new evidence correctly proved:

```text
activationEyesBefore=13
activationFilledBefore=0
activationEyesConsumed=12
activeEndPortalBlocks=9
```

The run then entered the End and selected `fight_ender_dragon`. Its first
combat instance normally mined the cage bars but failed with:

```text
fight_ender_dragon.crystal_standoff_unobserved
```

The model recovered by starting the combat skill again, but that fresh
instance no longer retained the observed unsafe crystal retreat. It attacked
the nearby dragon instead, eventually triggered the vanilla `[Free the End]`
advancement and the `DRAGON_KILLED` milestone, but left no proof that the
crystal had been shot. The release-excluded Oracle correctly failed:

```text
Continuous dragon milestone lacked physical combat evidence
```

At failure the body was alive in the End, the dragon death process had health
`1.0`, the skill had completed after 1,528 ticks, and the run had consumed no
arrow in its second instance. This run is `FAIL`, not completion evidence.

Root cause:

- `EndCrystalStandOffPlanner` required a single current first-person
  navigation frame to contain a complete safe destination at least 13.25
  horizontal blocks from the crystal;
- after looking up to mine the cage, a legitimate narrow semantic frame could
  expose only the first safe part of the open-floor retreat;
- twelve repeated looks could therefore fail despite a safe route consisting
  of multiple progressively observed steps.

Correction:

- prefer a fully observed final firing cell when available;
- otherwise select the farthest currently observed, supported, dry,
  low-danger cell that moves strictly away from the crystal;
- rescan from each reached intermediate cell until the unchanged 12.5-block
  firing threshold is satisfied;
- permit only monotonic safety-improving aggregate-risk overrides and increase
  the bounded movement-attempt allowance;
- add a unit regression whose observed fan ends before the final firing
  distance.

Directed evidence:

```text
EndCrystalStandOffPlannerTest
FightEnderDragonSkillTest
ShootObservedEntitySkillTest
  PASS

mcai_companion:real_end_victory_and_return
  PASS / 1 required / 1.947 min / real-time 20 TPS
```

The next command is another exact MiMo continuous chain to prove that the
formerly partial crystal observation no longer loses the combat handoff.

Formal M0-M4, external exact-JAR Actor/Observer, unseen random Hardcore,
soak, performance, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-06 continuous rerun: portal evidence false negative

The exact configured-`mimo-v2.5` continuous gate ran for 13 minutes and
physically completed every autonomous handoff from the one ordinary-player
chat message through End-portal activation:

```text
craft_recipe (14 Eyes)
-> return_via_verified_portal
-> triangulate_stronghold_search_area
-> reach_observed_stronghold
-> search_stronghold_portal_room
-> activate_observed_end_portal
```

The same body travelled the diagonal course with `travelRecoveries=0`,
normally mined into and searched the stronghold, filled all twelve initially
empty frames, and created all nine vanilla portal blocks. It failed only at
the release-excluded evidence assertion:

```text
End portal activated without the observed skill and exact Eye consumption
```

The failure was an Oracle/accounting defect, not a model or physical-action
failure:

- the assertion required zero Eyes after activation, although an ordinary
  trace Eye dropped before the stronghold handoff could later be picked up;
- `ActivateObservedEndPortalSkill` completed as soon as the final interaction
  made the portal visible, before its `VERIFYING` phase could validate the
  final one-item delta and increment `eyesInserted`.

The correction now:

- forces the final successful interaction through `VERIFYING`;
- captures Eye inventory and filled-frame count when the activation skill is
  first observed;
- proves the exact delta equals the number of still-empty frames, so unrelated
  legitimately recovered Eyes neither cause a false failure nor mask missing
  consumption;
- includes the before/after accounting in diagnostics.

Files changed:

- `src/main/java/dev/mcai/companion/skills/portal/ActivateObservedEndPortalSkill.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- this checkpoint

Last failed gate:

```text
mcai_companion:real_player_task_to_live_model_nether_materials_to_victory
FAIL only at the former zero-remaining-Eyes Oracle assertion
```

Next:

1. compile and run the focused real-time physical End-portal gate;
2. verify its persisted checkpoint records all twelve insertions;
3. rerun the exact MiMo chat-to-victory chain and continue from its first new
   causal boundary.

Focused verification is now complete:

```text
compileJava: PASS
ObservedEndPortalGeometryTest + PortalSkillsRegistrationTest: PASS
mcai_companion:real_end_portal_activation:
  PASS / 1 required / 34.08 s / real-time 20 TPS
```

The physical run normally reduced the inventory from twelve Eyes to zero,
left all twelve frames eyed, and created all nine portal blocks. Its persisted
terminal checkpoint contains:

```json
{"phase":"COMPLETED","eyesInserted":12,"stationVisits":3,
 "interactionRejections":0,"lastInteractionOutcome":"COMPLETED"}
```

The next command is therefore the exact real-provider continuous chain, not
another synthetic portal test.

Formal M0-M4, external exact-JAR Actor/Observer, unseen random Hardcore,
soak, performance, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-06 End-crystal retreat and dragon adaptation: focused PASS

The exact real-provider continuous run described below reached the End but
died after firing at a crystal from about 7.5 blocks. The causal correction
is now implemented:

- close clear crystals are inspected for thin cage bars before retreat;
- an observed-safe local standing cell at least 13.25 horizontal blocks from
  the crystal is selected and reached through ordinary movement;
- crystal firing is forbidden below 12.5 blocks;
- transient dragon-part reach misses trigger a bounded bow fallback, while
  high reachable parts cause ordinary jump attacks and four completed bow
  shots reopen melee instead of permanently locking to one ineffective mode;
- forced-Hardcore physical scopes and the live continuous fixture now treat a
  dead body as an immediate terminal failure before respawn or another model
  request.

Directed JVM contracts pass:

```text
EndCrystalStandOffPlannerTest
FightEnderDragonSkillTest
ShootObservedEntitySkillTest
BUILD SUCCESSFUL
```

The focused physical gate initially exposed three additional real boundaries:

```text
1. retreat before a thin aligned bar -> cage_water_bucket_required
2. repeated vanilla TARGET_OUT_OF_REACH -> skill_stalled
3. permanent bow fallback -> 21 shots without a full-health kill
```

After the controller correction it safely killed the full-health dragon, but
the fixture still failed because its nominal cage rail was outside normal
block reach and was removed by the crystal explosion. A live SQLite
checkpoint during the diagnostic run proved:

```json
{"phase":"TRAVELLING","cageBarsMined":0,
 "cageStatus":"SAFE_TRAVERSAL_UNAVAILABLE"}
```

The physical fixture now asserts the cage is within the ordinary 4.5-block
reach boundary and uses a real two-block iron-bar collider. The passing run's
mid-fight checkpoint proved normal action:

```json
{"phase":"SEARCHING","shots":1,"melee":3,
 "cageBarsMined":2,"cageStatus":"NONE"}
```

Final focused evidence:

```text
mcai_companion:real_end_victory_and_return
PASS / 1 required / 1.945 min / real-time 20 TPS
```

That gate requires normal pickaxe durability, ordinary ammunition, at least
12.25 blocks of measured crystal stand-off, survival in ordinary iron armour,
a credited full-health dragon kill, vanilla death animation, central return
portal handling, and the same companion UUID back in the Overworld.

Files changed for this correction:

- `src/main/java/dev/mcai/companion/skills/combat/EndCrystalStandOffPlanner.java`
- `src/main/java/dev/mcai/companion/skills/combat/FightEnderDragonSkill.java`
- `src/main/java/dev/mcai/companion/skills/combat/RangedCombatSkillPolicy.java`
- `src/test/java/dev/mcai/companion/skills/combat/EndCrystalStandOffPlannerTest.java`
- `src/test/java/dev/mcai/companion/skills/combat/FightEnderDragonSkillTest.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`

Next:

1. run the narrow terminal lifecycle contracts;
2. rerun the exact configured-`mimo-v2.5` autonomous completion chain from
   the ordinary player chat boundary;
3. capture only its first new causal boundary if it fails.

Formal M0-M4, external exact-JAR Actor/Observer, unseen random Hardcore,
soak, performance, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-05 continuous rerun: End-crystal self-explosion FAIL

The diagonal-course correction is now physically and causally verified:

```text
RollingTravelPlannerTest + TravelToSkillTest: PASS / 19 tests
mcai_companion:real_travel_diagonal_detour:
  PASS / 1 required / 40.46 sec / real-time 20 TPS
```

The exact configured-`mimo-v2.5` continuous chain then passed the former
failure boundary with ordinary travel and no fixture movement:

```text
craft_recipe -> return_via_verified_portal
-> triangulate_stronghold_search_area
-> reach_observed_stronghold
-> search_stronghold_portal_room
-> activate_observed_end_portal
-> find_and_enter_observed_portal
-> fight_ender_dragon
```

Recorded travel checkpoints advanced diagonally from about
`[-190,-38,-1637]` through `[-237,-38,-1679]` and
`[-279,-38,-1710]` to the stronghold, with `travelRecoveries=0`.
The body subsequently explored the stronghold, consumed ordinary food,
activated the portal, and physically entered the End.

The first new causal failure is End-crystal safety:

```text
fight_ender_dragon:
  mined one observed cage bar
  dispatched one bow shot at a crystal about 7.5 blocks away
  death message: "MCAI was blown up by MCAI"
```

The continuous fixture intentionally preserves the route body's iron armour.
`FightEnderDragonSkill.clearCrystalIndex` and
`RangedCombatSkillPolicy.defaults` both accept a seven-block crystal
stand-off. That margin is not Hardcore-safe. Merely rejecting the crystal
would also deadlock because the coordinator has no travel purpose for
retreating from an unsafe clear crystal.

The same run exposed a separate evaluation-integrity fault. This controlled
world forces Hardcore skill policy but is not itself a Hardcore server, so
`AiPlayerSession` performed its ordinary respawn. The running model then
repeated `fight_ender_dragon.wrong_dimension` from the Overworld. The run was
intentionally stopped with exit 130 after the causal boundary was captured;
it is a FAIL and not release evidence.

Files changed for the now-passing diagonal correction:

- `src/main/java/dev/mcai/companion/skills/core/RollingTravelPlanner.java`
- `src/main/java/dev/mcai/companion/skills/core/TravelToSkill.java`
- `src/test/java/dev/mcai/companion/skills/core/RollingTravelPlannerTest.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_real_travel_detour.json`
- `src/main/resources/data/mcai_companion/test_instance/real_travel_diagonal_detour.json`

Next:

1. add an observation-bounded crystal stand-off/retreat phase before firing;
2. prove its distance with an actual explosion-survival physical gate using
   the same ordinary combat equipment boundary;
3. make a locked or forced-Hardcore evaluation treat body death as an
   immediate permanent terminal failure, with no respawn/model retry;
4. run only the dragon and death-lifecycle contracts, then rerun the exact
   real-provider continuous chain.

Formal M0-M4, external exact-JAR Actor/Observer, unseen random Hardcore,
soak, performance, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-05 continuous rerun: diagonal-course travel FAIL

The corrected stronghold wall-entry gate passes physically:

```text
mcai_companion:real_stronghold_reach
PASS / 1 required / 3.420 min / real-time 20 TPS
```

The subsequent exact configured-`mimo-v2.5` continuous run selected the
correct production sequence:

```text
craft_recipe -> return_via_verified_portal
-> triangulate_stronghold_search_area
-> reach_observed_stronghold
```

It then failed before excavation. The model was not the causal boundary.
`TravelToSkill` moved the same body from the Eye-trace endpoint almost
entirely along X while Z remained about `-1546.5`, then exhausted fair
observation at `[-273,-38,-1547]`. The target segment was around
`[-303,-38,-1725]`. The runtime recorded
`travel_to.route_unknown` followed by
`repeated_identical_skill_failure`; the run was intentionally stopped.

Current root cause:

- the controlled course has a perpendicular Eye-triangulation baseline and
  a diagonal approach corridor; from the measured endpoint, entering that
  corridor requires a short, normal detour that can temporarily increase
  Euclidean distance to the target;
- `RollingTravelPlanner` rejects every non-distance-reducing candidate, so it
  greedily follows the baseline instead of entering the diagonal corridor;
- candidate sorting plans only to the single closest candidate and reports
  `NEEDS_OBSERVATION` if that one candidate is disconnected, even when another
  observed-safe candidate is reachable;
- the journey has no course-deviation/backtrack state, so each parent
  recovery starts another greedy segment from the newly displaced body.

Files already changed in the preceding wall-entry correction:

- `src/main/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkill.java`
- `src/test/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkillTest.java`
- this checkpoint

Last directed gates:

```text
focused mining + stronghold contracts: PASS
real_stronghold_reach: PASS / 3.420 min
real_player_task_to_live_model_nether_materials_to_victory:
  FAIL / travel_to.route_unknown before excavation
```

Next:

1. add a bounded start-to-target course deviation and hysteretic recovery
   mode to rolling travel, permitting only observed-safe short detours;
2. try the ordered reachable candidate set instead of treating the first
   disconnected candidate as global route failure;
3. add deterministic diagonal/detour and disconnected-candidate contracts;
4. run only the travel unit contracts and a focused physical diagonal-course
   gate, then rerun the exact real-provider chain.

Formal M0-M4, external exact-JAR Actor/Observer, random unseen Hardcore,
soak, performance, and two-hour completion gates remain `NOT_RUN`.

Implementation now in the dirty working tree:

- `RollingTravelPlanner` carries a bounded start-to-target course band,
  explicit disconnected-candidate retry, and a hysteretic observed-ground
  recovery path that may briefly move away from the destination.
- `TravelToSkill` preserves the journey origin, failed recovery side, and
  recovery hysteresis across local segments instead of restarting greedy
  Euclidean progress after every scan.
- `RollingTravelPlannerTest` contains directed contracts for disconnected
  candidates, course return, and opposite-side retry.
- `EmbodimentGameTests` plus the dedicated test-environment/test-instance
  resources contain `real_travel_diagonal_detour`, a physical fixture whose
  only valid route begins with an away-from-goal movement.

The planner and `TravelToSkill` JVM contracts pass. The new physical gate is
the next and only immediate command; its result is not yet evidence and must
not be promoted to M1-M4.

## 2026-08-05 continuous rerun: wall-above-probe FAIL

The next exact real-provider rerun again completed crafting, verified portal
return, Eye triangulation, and ordinary travel. It entered excavation at
about `[-301,-47,-1726]`. The first parent ended before handoff; the model
started the same skill once more. The restart correctly skipped surface
travel and resumed the new local depth probe:

```text
phase=DEPTH_PROBING
body=[-287,-50,-1726]
completedExcavationLegs=0
completedDepthProbeLegs=1
travelRecoveries=0
```

The run was intentionally stopped because the horizontal probe had crossed
beneath the entire fixture wall without observing masonry. The production
maze's masonry ring spans Y=-48..-41 while the probe feet layer is Y=-50.
The focused fixture had extended its ring down to the probe layer, so its
preceding PASS did not cover this vertical relationship.

Current root cause:

- a purely horizontal safe-depth search can remain two or more blocks below
  the structure;
- an upward look may expose masonry but does not by itself prove the
  traversable room-floor elevation;
- immediately entering at the first low wall face can create a legal tunnel
  under the room and still leave no portal-search frontier.

Next implementation direction:

1. add a bounded, observation-driven `ASCENDING` stair mode to the ordinary
   excavation primitive, including normal jump/movement, support, clearance,
   durability, lighting, and fall-closed checks;
2. make stronghold wall entry an elevation search: probe horizontally,
   verify the interior frontier, retreat through the already opened corridor
   when no frontier exists, climb one supported step parallel to the wall,
   and retry;
3. bound all retries and preserve wall/floor evidence;
4. correct the focused fixture so its wall bottom is above the safe-depth
   probe exactly like the continuous maze before another paid rerun.

Formal M0-M4 and hidden-seed gates remain `NOT_RUN`.

## 2026-08-05 safe-depth local search: focused PASS

The exact `safe_depth_limit -> underground restart -> route_unknown` geometry
now has a deterministic and physical correction:

- a descending search at the safe-Y boundary switches to a bounded horizontal
  expanding spiral (eight legs, 8/8/16/16/24/24/32/32 blocks);
- stronghold targets still come only from current first-person block-face
  observations and stop the mining child;
- recoverable support/fluid directions rotate locally instead of asking the
  model to repeat a compound;
- a fresh skill started near the triangulated column below Y=-46 resumes
  local excavation instead of invoking surface travel.

Directed results:

```text
ReachObservedStrongholdSkillTest: PASS
SearchObservedStrongholdPortalRoomSkillTest: PASS
ExcavateSafeTunnelSkillTest: PASS
mcai_companion:real_stronghold_reach:
  PASS / 1 required / 2.177 min / real-time
  travelled about 96 blocks normally
  descended from Y=-42 to the safe-depth boundary
  horizontally exposed the occluded east ring wall at X=-192
  adjusted down along the wall and crossed into the lower room
  consumed ordinary pickaxe durability and owned torches
  retained stone-brick evidence and an eastward current-revision frontier
```

The focused fixture is now deliberately homologous to the failed continuous
case: its first descending leg passes beneath a buried masonry ring, the next
descent would violate the global safe-Y bound, and the receiving room is two
levels below the horizontal probe.

Next: rerun the exact real-provider continuous victory chain. This remains a
controlled inner-loop pass, not an M0-M4 or hidden-seed result.

## 2026-08-05 continuous rerun: depth-limit recovery FAIL

The exact configured-`mimo-v2.5` continuous chain was rerun after the lowered
entry gate passed. It physically completed Eye crafting, verified portal
return, triangulation, and about 311 blocks of ordinary Overworld travel.
The body reached the search area and descended from Y=-38 to Y=-50. The run
was intentionally stopped after one causal failure and three identical
recovery failures:

```text
reach_observed_stronghold:
  first terminal reason=reach_observed_stronghold.safe_depth_limit
  body about [-298,-50,-1726]
model restart of the same skill:
  terminal reason=travel_to.route_unknown x3
  body and semantic observation unchanged
```

Read-only checkpoint evidence before the underground search:

```text
distance to search center: 311.356 -> 221.329 -> 120.043 -> 16.295 -> 2.500
travelRecoveries=0
phase changed APPROACHING -> EXCAVATING
```

This proves the model and long-distance locomotion were active. The current
production gap is local:

- the bounded descent reached its safe depth without observing masonry from
  this approach side;
- terminal failure discarded the underground search state;
- a fresh skill incorrectly restarted with surface `APPROACHING`, attempted
  to route through its just-mined enclosed station, and correctly found no
  advancing ordinary-navigation frontier;
- returning that local geometry to the model created the same open-loop
  retry pattern.

Next:

1. retain the same parent skill at the safe-depth boundary and perform a
   bounded horizontal/four-view masonry probe instead of terminal failure;
2. make restart/resume recognize that the body is already inside the bounded
   search column, so it never invokes remote surface travel there;
3. add deterministic contracts and a focused physical fixture for this exact
   no-masonry-at-depth case before another real-provider continuous rerun.

Formal M0-M4 and hidden-seed gates remain `NOT_RUN`.

## 2026-08-05 lowered stronghold entry recovery: focused PASS

The bounded local correction for a stronghold room below the first observed
wall approach now passes both deterministic contracts and the real-time
server-side physical gate:

```text
ReachObservedStrongholdSkillTest: PASS
SearchObservedStrongholdPortalRoomSkillTest: PASS
ExcavateSafeTunnelSkillTest: PASS
mcai_companion:real_stronghold_reach:
  PASS / 1 required / 1.662 min / real-time
  ordinary travel from x about -296 to the observed masonry
  iron-pickaxe damage: 0 -> 21
  owned torches: 32 -> 28
  body descended through the deliberately lowered fixture,
  crossed the masonry plane, and exposed the directional room frontier
```

The production parent skill now retains ownership of an
`unsafe_support` entry failure, performs bounded one-step descending legs
parallel to the wall with alternating directions, and retries the horizontal
crossing. Adjacent bounded excavation legs may reuse a currently visible
torch within six blocks. The model is no longer asked to solve this
tick-level geometry failure.

Next: rerun the exact configured-`mimo-v2.5` continuous
`real_player_task_to_live_model_nether_materials_to_victory` gate and require
the same embodied player to complete portal-room DFS, activation, End entry,
dragon combat, and return. This focused pass is not an M0-M4 or hidden-seed
claim.

## 2026-08-05 continuous victory chain: portal-search handoff FAIL

The latest exact configured-`mimo-v2.5` run of
`real_player_task_to_live_model_nether_materials_to_victory` was stopped
after the runtime's repeated-identical-failure fuse fired. It is a failed
controlled inner-loop run, not a pass:

```text
craft_recipe -> return_via_verified_portal
-> triangulate_stronghold_search_area
-> reach_observed_stronghold: completed
-> search_stronghold_portal_room: search_exhausted x3
```

The body physically travelled from about 296 blocks away to the measured
stronghold, excavated, crossed the east masonry plane, and retained visible
stone-brick evidence. The portal-room DFS nevertheless had exactly one
visited station and no movable frontier. Its station was
`[-295,-43,-1725]`; the receiving room's intended feet layer was Y=-42 and
its stone-brick floor was Y=-43. The reach compound therefore completed with
the body in the floor layer/trench below the accessible room. The DFS
correctly refused to invent a route through unknown or solid cells.

Root cause:

- wall entry selected `DESCENDING` whenever the nearest visible masonry was
  below the current feet;
- after an already descending stronghold search, that adds an unnecessary
  second descent while crossing the wall;
- entry verification checked displacement, stable dry ground, and surviving
  masonry, but did not prove a legal adjacent interior frontier;
- the focused physical gate checked crossing the masonry plane but repeated
  the same missing handoff assertion.

Files already changed in this chain:

- `src/main/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/StrongholdSkills.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkillTest.java`
- this checkpoint

Last passing directed gate:

```text
mcai_companion:real_stronghold_reach:
  PASS / 1 required / 1.468 min
```

Last failed gate:

```text
mcai_companion:real_player_task_to_live_model_nether_materials_to_victory:
  FAIL (manually stopped after the product fuse recorded three identical
  search_stronghold_portal_room.search_exhausted outcomes)

mcai_companion:real_stronghold_reach after the first handoff correction:
  FAIL / 1.470 min
  body=(-198.841,-46.0,-294.500)
  test assertion expected the wrong fixture feet layer
```

The focused failure also proved that an undirected adjacent-frontier check can
accept the freshly excavated retreat tunnel behind the body. The current
correction therefore requires a frontier specifically in the wall-crossing
direction. The focused room's real feet/floor offsets are -4/-5 from its
search plane, matching the measured body Y=-46 and preserved brick Y=-47.

Correction verification:

```text
ReachObservedStrongholdSkillTest: PASS
SearchObservedStrongholdPortalRoomSkillTest: PASS
mcai_companion:real_stronghold_reach:
  PASS / 1 required / 1.458 min / real-time
  final body=(-198.836,-46.0,-294.500)
  iron-pickaxe damage: 0 -> 13
  owned torches: 32 -> 30
  eastward current-revision ordinary-nav frontier: present
```

The next exact real-MiMo continuous rerun then exposed a second geometry
case and was intentionally stopped after repeated alternation:

```text
first wall approach:
  body=[-298,-40,-1725]
  observed masonry=[-296,-41,-1725]
  requested entryMode=HORIZONTAL, entryMaximumSteps=3
  failure=excavate_safe_tunnel.unsafe_support
later retries:
  reach_observed_stronghold:
    excavate_safe_tunnel.torch_placement_not_observed
  search_stronghold_portal_room:
    search_exhausted
```

The receiving-room feet/floor layers were Y=-42/-43, two levels below the
first safe wall approach. A purely horizontal child correctly refused the
unsupported far-side cell. Returning that bounded geometry failure to the
model caused it to alternate reach/search without changing the body state.

Current implementation direction:

- keep wall crossing horizontal when the far-side support is valid;
- on `unsafe_support`, retain the same parent skill and descend one ordinary
  mined step parallel to the wall, alternating sides and bounding attempts;
- then retry the horizontal crossing from the new supported elevation;
- reuse a currently visible torch within six blocks when composing adjacent
  bounded excavation legs, instead of attempting an unobservable duplicate
  in the player's occupied station;
- make the focused physical room two levels lower so this recovery is proved
  before another paid continuous run.

Next:

1. run the focused mining/stronghold contracts;
2. rerun `real_stronghold_reach` against the lowered room;
3. only after it passes, rerun the same real-provider continuous victory
   chain and require portal-room DFS, activation, End fight, and return.

Formal M0-M4, external exact-JAR Actor/Observer, random unseen Hardcore
seeds, soak, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-05 Live-model Eye craft/portal return/triangulation gate: PASS

The active directed gate starts with the embodied survival player physically
entering the Nether through a vanilla portal, receiving fourteen blaze powder
and fourteen Ender pearls through normal item pickup, and then accepting one
ordinary player-chat goal. The same configured `mimo-v2.5` must select and
complete `craft_recipe`, `return_via_verified_portal`, and
`triangulate_stronghold_search_area` without post-command fixture control.

Resolved setup root causes:

- The Nether arrival setup could be entered twice through nested vanilla
  inventory/advancement callbacks. The second invocation treated the already
  relocated body as a new portal arrival. The phase is now committed before
  mutation and guarded by an exactly-once flag.
- The fixture put its iron pickaxe in inventory slot 11 and then selected slot
  11, but vanilla selected hotbar slots are limited to 0-8. Survival support
  items now occupy hotbar slots 4-7 and the pickaxe selects slot 5.
- The first live rerun then proved real Eye crafting, but the return skill
  correctly stalled in `LOOKUP`: this combined gate had allowed vanilla to
  carry the body through the initial portal without the entry skill callback,
  so no verified edge had been persisted. The setup now records the two
  physically observed endpoints before the command and waits for that durable
  write. It still performs no memory, coordinate, item, or milestone mutation
  after ordinary player chat.
- With the verified edge present, MiMo selected the return and triangulation
  skills and the body physically returned to the Overworld. The first Eye
  trace succeeded, but both 256-block baseline candidates failed at
  `travel_to.route_unknown`. A focused non-portal triangulation control
  traversed the same kind of corridor normally, isolating the defect to a
  portal-frame obstacle at the returned pose. Remote travel scanned only
  within 60 degrees of the target bearing, so it could not observe the
  perpendicular doorway around the portal frame. Its bounded scan now covers
  the full 360 degrees while retaining the same fail-closed voxel rules.
- The 360-degree live rerun disproved angle coverage as the remaining root
  cause. At every terminal scan the returned body was still physically at
  `(-7.5,-38,-6.5)` inside the source portal. The current semantic revision
  contained only the two body-clearance cells and one on-ground contact
  (`observedVoxels=3`); it contained no currently observed destination support
  (`currentSafeFeet=0`). Consequently the planner correctly refused to walk.
  The active defect is the post-traversal portal egress/perception boundary,
  not the model, the Eye trajectory, or the 256-block rolling route.
- Minecraft 26.2 gives the translucent Nether-portal sheet a thin OUTLINE
  shape. A normal block-picking ray that starts inside the sheet therefore
  terminates at almost zero distance. Fair perception now reports that
  plainly visible portal separately, but continues the same finite,
  loaded-only first-person ray through the translucent sheet to the first
  opaque block or fluid. It does not expose an unloaded or hidden voxel.
- The next real MiMo rerun proved that correction: the same body walked from
  the returned portal to `(243.054,-38,-49.500)`, consumed exactly two Eyes,
  recorded two physical Eye traces about 251 blocks apart, and computed a
  bounded intersection. The only failed assertion was a one-tick progress
  commit race: the skill was already `COMPLETED`, but
  `STRONGHOLD_SEARCH_AREA_TRIANGULATED` had only been refreshed indirectly
  when the next model observation was built. The skill now commits the two
  evidence-backed stronghold milestones before publishing `COMPLETED`.

Changed files:

- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_live_model_eye_craft_return_and_stronghold.json`
- `src/main/resources/data/mcai_companion/test_instance/real_player_task_to_live_model_eye_craft_return_and_stronghold.json`
- `src/main/java/dev/mcai/companion/skills/core/TravelToSkill.java`
- `src/test/java/dev/mcai/companion/skills/core/TravelToSkillTest.java`
- `src/main/java/dev/mcai/companion/perception/FairPerceptionSampler.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/TriangulateStrongholdSearchAreaSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/StrongholdSkills.java`
- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/test/java/dev/mcai/companion/skills/stronghold/TriangulateStrongholdSearchAreaSkillTest.java`

Last failed gate before the terminal-commit correction:

```text
MiMo selected and completed all three requested skills. The body physically
walked the full baseline, two Eye traces produced an intersection, and the
compound returned COMPLETED. The immediate test callback still saw only
STRONGHOLD_BEARING_MEASURED because the triangulated route milestone was
formerly refreshed only on the next planner-observation construction.
```

Final directed result:

```text
mcai_companion:real_player_task_to_live_model_eye_craft_return_and_stronghold
PASS / all 1 required tests / 3.422 min / real mimo-v2.5
model decisions: craft_recipe -> return_via_verified_portal
                 -> triangulate_stronghold_search_area
same embodied player: 14 Eyes crafted, normal portal return, two physical
Eye throws about 251 blocks apart, 12 Eyes remaining, bounded intersection,
terminal stronghold-bearing and search-area milestones committed
BUILD SUCCESSFUL in 3m 40s
```

This closes the focused continuous handoff from collected Nether materials
through a server-verified Overworld stronghold search area. It remains a
controlled inner-loop test, not an unknown-seed completion claim.

Next:

1. inspect and run only the existing `real_stronghold_reach` handoff;
2. repair the first production causal gap from the triangulated search area
   to a physically observed stronghold;
3. then connect portal-room search, activation/entry, and the late-End
   completion chain;
4. separately harden portal return so terminal completion includes a safely
   observed egress cell rather than leaving the body in the portal surface.

Formal M0-M4 statistical, unknown-seed, soak, performance, and Hardcore
completion gates remain `NOT_RUN`.

### Active continuation: triangulation -> observed stronghold

The focused `real_stronghold_reach` physical gate still passes on Forge
65.1.0 (`1 required / 1.341 min`). It walked more than 90 blocks, descended
underground, damaged the ordinary iron pickaxe, consumed an owned torch, and
stopped on current first-person stone-brick evidence.

The remaining gap is continuous orchestration rather than the local action
skill. The live Eye/return/triangulation gate previously terminated as soon
as the intersection appeared. Its pre-command fixture now positions the
course 192 blocks from the same vanilla Eye target, builds two safe diagonal
approach corridors, and buries a representative stone-brick wall in an
ordinary stone search volume. After ordinary player chat the fixture remains
strictly observational. The same configured model must additionally select
`reach_observed_stronghold`; final success requires material X/Z travel,
underground descent, pickaxe damage, owned-torch consumption, an unmined wall,
and current first-person stone-brick evidence. `compileJava` passes.

Next: rerun only
`real_player_task_to_live_model_eye_craft_return_and_stronghold` with the
configured real provider and follow its first factual terminal result.

First upgraded live run was stopped after the audit proved three identical
`trace_stronghold_eye.thrown_eye_not_observed` failures. Each ordinary use
action was dispatched. The new course had placed the target along the
horizontal axis of the source portal, so an Eye thrown while the body was
still inside the portal sheet travelled toward its obsidian side frame.
The course offset is now rotated 90 degrees so the launch exits through the
portal face, matching the already proven fair-visibility geometry; no
perception cone, timeout, or trace evidence rule was relaxed.

The rotated rerun crossed that launch boundary and the real model selected
`reach_observed_stronghold`. The body travelled from the second throw to the
search area and descended from Y=-38 to Y=-48, then the child correctly
failed `unsafe_support`. The failure position was X=-289 while the
pre-command solid search fixture ended at X=-290: a legal twelve-step east
descending leg had walked one block beyond the fixture, not beyond production
evidence. Later retries from the sealed underground pose correctly failed
`travel_to.route_unknown`. The run was stopped. The fixture search volume is
now widened to cover the uncertainty radius plus a full descending leg, and
the buried stronghold fragment is a square inner wall so any legal first
search direction can expose representative evidence. Production mining,
support, and visibility rules remain unchanged.

Final directed result after that fixture-only correction:

```text
mcai_companion:real_player_task_to_live_model_eye_craft_return_and_stronghold
PASS / all 1 required tests / 8.449 min / real mimo-v2.5
model decisions: craft_recipe -> return_via_verified_portal
                 -> triangulate_stronghold_search_area
                 -> reach_observed_stronghold
same embodied player: >=180 blocks of post-triangulation travel, underground
descent, ordinary iron-pickaxe wear, owned-torch consumption, preserved
stone-brick evidence, and current first-person visible stone bricks
terminal checkpoint: phase=COMPLETED, position=[-298,-40,-1725],
bestHorizontalDistance=5.733, observedStrongholdBlock=[-296,-41,-1725]
BUILD SUCCESSFUL in 8m 44s
```

This remains controlled GameTest inner-loop evidence, not formal unknown-seed
or release evidence. The next uncovered causal boundary is real-model
selection of `search_stronghold_portal_room`, followed by physical activation
and entry using the same body.

### Active continuation: stronghold interior -> End entry

Root causal gap:

- The physical `real_end_portal_activation` gate already proves the local
  DFS search, dead-end backtracking, first-person ring discovery, and normal
  Eye placement controller.
- The existing live-model activation/entry gate starts with the full frame
  already visible. It therefore does not prove that the configured model can
  select `search_stronghold_portal_room` from ordinary stronghold evidence
  and then preserve the same goal/body through activation and entry.

Current implementation:

- `LiveStrongholdPortalRoomScenario` builds an opaque pre-command
  stone-brick maze with a dead branch, a second corridor, and a hidden empty
  twelve-frame portal ring.
- One logged-in player sends an ordinary Chinese completion request and
  disconnects. A prior-route checkpoint is installed before autonomous skill
  execution; after that point the fixture performs no block, entity,
  inventory, pose, or goal mutation.
- The real configured model must select
  `search_stronghold_portal_room -> activate_observed_end_portal ->
  find_and_enter_observed_portal`.
- Success requires the same survival UUID to enter the dead branch, backtrack
  through the second turn, travel materially, consume exactly twelve owned
  Eyes through vanilla interaction, create all nine portal cells, and
  physically enter the End.

Changed files:

- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_live_model_stronghold_portal_room_and_entry.json`
- `src/main/resources/data/mcai_companion/test_instance/real_player_task_to_live_model_stronghold_portal_room_and_entry.json`

Last gate:

```text
compileJava -Pforge_compile_version=65.1.0
PASS / BUILD SUCCESSFUL in 11s
```

Final directed result:

```text
mcai_companion:real_player_task_to_live_model_stronghold_portal_room_and_entry
PASS / all 1 required tests / 1.253 min / real mimo-v2.5
model decisions: REPLAN -> search_stronghold_portal_room
                 -> activate_observed_end_portal
                 -> find_and_enter_observed_portal
same embodied player: occluded dead branch entered, physical backtracking
through the second corridor, twelve owned Eyes consumed through ordinary
interactions, nine active End-portal cells, End dimension entered, and
vanilla advancement [The End?] awarded
BUILD SUCCESSFUL in 1m 25s
```

The exact next causal gap is not another already-in-End short task. It is one
ordinary completion chat whose same body and goal continue from stronghold
interior discovery through dragon combat and the central return portal.

That continuous variant is now registered as
`real_player_task_to_live_model_stronghold_portal_room_to_victory`.
It reuses the proven maze and inventory but asks for the full remaining
completion route in the initial chat. Only after genuine End entry does the
release-excluded test create the deterministic full-health combat arena; it
does not select a skill, credit damage, kill the dragon, or move the body.
The model must subsequently select `fight_ender_dragon` and
`find_and_enter_observed_portal`, with server-attributed dragon death and the
same UUID physically returning to the Overworld. `compileJava` passes.

Next exact gate:

```text
./gradlew runGameTestServer \
  -Pforge_compile_version=65.1.0 \
  -Plive_model_test=true \
  -Prealtime_gametest=true \
  -Plive_model_selector=mcai_companion:real_player_task_to_live_model_stronghold_portal_room_to_victory \
  --no-daemon
```

Final directed result:

```text
mcai_companion:real_player_task_to_live_model_stronghold_portal_room_to_victory
PASS / all 1 required tests / 2.377 min / real mimo-v2.5
model decisions: REPLAN -> search_stronghold_portal_room
                 -> activate_observed_end_portal
                 -> find_and_enter_observed_portal
                 -> fight_ender_dragon
                 -> find_and_enter_observed_portal
same embodied player: occluded dead branch and backtrack, twelve ordinary
Eye placements, physical End entry, crystal/cage combat, server-attributed
dragon death with [Free the End], and physical central-portal return to the
Overworld
BUILD SUCCESSFUL in 2m 32s
```

The next M2 integration target is a single ordinary completion chat spanning
the already-proven Nether-material/Eye/return/triangulation/reach chain and
this newly proven stronghold-search/activation/entry/fight/return chain.
Until that passes, these remain overlapping controlled inner loops rather
than one uninterrupted completion route.

The integration variant is now registered as
`real_player_task_to_live_model_nether_materials_to_victory`. Its pre-command
search fixture adds a body-accessible chamber at the measured target and a
roofed, two-turn, first-person-occluded portal corridor. The initial Nether
inventory retains the existing physical Eye/return/reach resources and adds
ordinary late-End combat gear. One chat goal must span:

```text
craft_recipe -> return_via_verified_portal
-> triangulate_stronghold_search_area -> reach_observed_stronghold
-> search_stronghold_portal_room -> activate_observed_end_portal
-> find_and_enter_observed_portal -> fight_ender_dragon
-> find_and_enter_observed_portal
```

`compileJava` passes. Next exact gate:

```text
./gradlew runGameTestServer \
  -Pforge_compile_version=65.1.0 \
  -Plive_model_test=true \
  -Prealtime_gametest=true \
  -Plive_model_selector=mcai_companion:real_player_task_to_live_model_nether_materials_to_victory \
  --no-daemon
```

## 2026-08-04 Verified portal return handoff: PASS

The missing Nether-to-Overworld handoff after collecting the End route
materials is now implemented and physically verified. The body no longer
needs model-authored coordinates or an invented reverse transport edge. A
production skill queries the durable destination endpoints of prior
body-observed traversals, walks back to the nearest remembered arrival area,
re-observes the actual portal surface, and crosses through the vanilla portal
controller. Only that physical return traversal may create the reverse edge.

Resolved root cause:

- Portal memory previously indexed only directed source endpoints. After the
  first Overworld-to-Nether traversal, the Nether arrival existed only as the
  destination half of the durable edge and was therefore unavailable as a
  return navigation anchor.
- The post-Ender route selector exposed only Eye crafting and stronghold
  triangulation. A body still in the Nether could consequently produce Eyes
  but had no truthful capability with which to return to the Overworld.
- Memory schema version 3 adds a destination endpoint index and a bounded,
  world-scoped arrival query. It does not fabricate a reverse edge.
- The planner now exposes `return_via_verified_portal` only while the
  completion body is in the Nether, and exposes triangulation only after the
  body is authoritatively observed in the Overworld.

Changed files:

- `src/main/java/dev/mcai/companion/BuildInfo.java`
- `src/main/java/dev/mcai/companion/memory/MemoryDatabase.java`
- `src/main/java/dev/mcai/companion/memory/transport/VerifiedPortalEdgeRepository.java`
- `src/main/java/dev/mcai/companion/skills/portal/VerifiedPortalArrivalLookup.java`
- `src/main/java/dev/mcai/companion/skills/portal/ReturnViaVerifiedPortalSkill.java`
- `src/main/java/dev/mcai/companion/skills/portal/PortalSkills.java`
- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- the matching JVM tests and focused GameTest resources

Last failed gate before correction:

```text
No production skill could address the Nether destination endpoint because
the verified edge repository exposed source endpoints only.
```

Directed repository, skill, planner-schema, and argument-canonicalization JVM
tests pass. The focused `real_verified_portal_return` GameTest also passes:
the same embodied player built and lit a portal, entered the Nether normally,
explored away from it, returned by the new parameterless skill, re-observed
the portal, and arrived in the Overworld. All 1 required tests passed in
5.197 seconds.

Next: exercise the real-model continuous phase selector across the actual
Eye-crafting and dimension handoff boundary, then connect it to the already
focused stronghold triangulation/reach/End chain. Formal M0-M4 statistical,
unknown-seed, soak, performance, and Hardcore completion gates remain
`NOT_RUN`; this focused pass must not be reported as full plan completion.

## 2026-08-04 Real-time Ender reserve chain: PASS

The exact ordinary-player-chat -> automatically restored saved provider
profile -> real `mimo-v2.5` -> production `secure_ender_pearl_reserve` chain
now passes. An ordinary test player joined, sent the task through the normal
chat path, received the immediate `[AI]` acknowledgement, and disconnected.
The model returned a schema-valid `START_SKILL` decision after 5,713 ms
(6,692 input and 128 output tokens). The headless survival body then built the
safety roof, removed its temporary pillar, fought ordinary controlled
Endermen through vanilla melee/durability/drop/pickup paths, repeatedly
returned to the work centre, and completed with at least fourteen
server-observed pearl-derived route units.

SQLite terminal evidence:

```text
phase=COMPLETED
enderRouteUnits=14
attemptedTargets=11
exhaustedTargets=10
returnsCompleted=10
extraDropsCollected=2
sheltersBuilt=1
skill_completed.secure_ender_pearl_reserve
```

The required real-time GameTest passed in 2.798 minutes; the Gradle invocation
completed successfully in 2 minutes 58 seconds. This proves the repaired
controlled inner loop only. It does not satisfy the formal M2 unknown-seed
criterion or any M0-M4 statistical/soak gate.

Resolved root cause:

- `ServerCoreSkillFrameSource.current()` already fuses authoritative live
  `ServerPlayer` pose and survival state at 20 TPS; stale body pose was not the
  defect.
- `MoveToSkill` could reach a partially observed frontier, incorrectly treat
  it as final arrival, begin unplanned precision docking, and later revisit
  frontier cells around the true target indefinitely. Tangential movement
  kept resetting the generic displacement watchdog.
- Tangential steps remain legal because real obstacle detours need them.
  `MoveToSkill` now retains reached frontiers for one invocation, rejects a
  repeat, stops at a partial frontier, waits for a newer fair semantic
  observation, and permits exact-point docking only for a genuine arrival
  route.
- The physical fixture now uses the production 4 Hz semantic cadence between
  explicit fixture mutations. It exposed and then verified the bounded-
  frontier fix before the real-model rerun.

Changed files in the recovery chain:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/main/java/dev/mcai/companion/skills/core/CoreSkillGeometry.java`
- `src/main/java/dev/mcai/companion/navigation/LocalAStarPlanner.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/BuildEndermanSafetyRoofSkill.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- matching focused JVM tests
- this checkpoint

Last failed gate before correction:

```text
real_player_task_to_live_model_ender_pearl_reserve
phase=RETURNING, enderRouteUnits=2, returnsCompleted=0
distance to fixed roof centre oscillated about 0.6-1.1 blocks for >1,000 ticks
```

Directed navigation/MoveTo JVM contracts, the 4 Hz physical reserve gate, and
the real chat/profile/MiMo production reserve gate now pass. Next: inspect the
existing M2 phase-selector and cross-skill continuity contracts, then add the
smallest missing causal handoff from the completed pearl reserve into the
already proven stronghold/End sequence. Do not claim full random-seed
Hardcore completion until the formal Actor/Observer and statistical gates
have actually run.

## 2026-08-04 Ender post-kill precision re-dock: active

The newest deterministic physical rerun disproved the previous
route-evidence and planner-budget failures: the skill built the roof, removed
the temporary pillar, completed repeated ordinary combat/drop/pickup/return
cycles, and reached 12 pearl-derived route units. It then failed on a
different boundary:

```text
mcai_companion:real_ender_pearl_reserve
  phase before failure=SELECTING
  enderRouteUnits=12
  attemptedTargets=9
  exhaustedTargets=9
  returnsCompleted=7
  extraDropsCollected=1
  terminal=move_to.hardcore_danger
```

Root cause:

- the ninth combat child completed while the body was inside the verified
  shelter's 0.25-block centre radius, so the parent correctly returned to
  `SELECTING` and discarded its active-target binding;
- ordinary post-combat inertia then moved the body just outside that radius
  while its feet remained in the same shelter cell;
- the next `SELECTING` tick started a precision re-dock without carrying the
  exact just-defeated target identity. The current fair frame still held one
  proximity-only danger sample from that kill, so the new `MoveToSkill`
  correctly failed closed;
- the next semantic frame had zero risk. This is a one-frame target-binding
  lifetime defect, not permission to ignore general Hardcore danger.

Changed files in the current recovery chain:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/main/java/dev/mcai/companion/skills/core/CoreSkillGeometry.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- the matching focused tests under `src/test/java`
- this checkpoint

Immediately preceding corrections, already covered by focused JVM tests:

- a return cell's stale support is refreshed by looking at the actual nearby
  floor point instead of using a fixed 50-degree pitch that overshot it;
- wall-clock A* budget jitter is retried for at most eight consecutive ticks,
  while node-budget exhaustion and dangerous route voxels still fail
  immediately;
- the exact post-kill return authorization cannot permit contact,
  projectiles, body hazards, a different hostile, unattributed risk, a stale
  revision, a different goal/cell, or a dangerous route voxel.

Next: retain the exact defeated-target binding for a very short,
revision-bounded `SELECTING` precision re-dock, clear it on any new target or
unrelated transition, add a directed state regression, and rerun only the
physical reserve gate. If it reaches 14 route units, run the exact ordinary
player chat -> restored MiMo profile -> production reserve gate. Formal
M0-M4 statistical and soak gates remain `NOT_RUN`.

## 2026-08-04 Ender return residual-risk authorization: active

Current proven root causes and corrections:

- the earlier real-chat/MiMo return failure was not an action-owner, item-use,
  or survival-lane stall. `ACTIVE_SKILL` retained the body and issued physical
  movement, but `MoveToSkill` dropped into a rescan after an empty same-cell
  route; same-cell precision motion now continues every tick and its directed
  regression passes;
- the deterministic physical reserve then killed and collected from two
  ordinary Endermen and killed target three before failing
  `move_to.hardcore_danger`;
- the third target's just-ended `HOSTILE_PROXIMITY` semantic sample remained
  for one frame under the verified roof. The reserve parent correctly
  recognized that exact transient, but its newly constructed return
  `MoveToSkill` independently rejected the raw aggregate risk before the
  parent could enter `RETURNING`.

Changed files in this active slice:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/test/java/dev/mcai/companion/skills/core/MoveToSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- `src/test/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkillTest.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- this checkpoint

Last failed gate:

```text
mcai_companion:real_ender_pearl_reserve
  roof built and temporary pillar removed
  two pearl cycles completed through ordinary combat/drop/pickup/return
  third ordinary target killed
  checkpoint phase=HUNTING, enderRouteUnits=2, attemptedTargets=3
  terminal: move_to.hardcore_danger
```

Next: finish a fail-closed child authorization that is valid only in the
verified shelter cell and only for the exact just-defeated Enderman's
proximity residue. Contact, projectiles, body hazards, other entities,
unattributed risk, and every route-voxel danger remain rejected. Compile and
run the focused JVM contracts, rerun the deterministic physical reserve to
14 server-observed route units, then rerun the exact ordinary-player-chat ->
real MiMo -> production reserve gate. Formal M0-M4 and statistical/soak gates
remain `NOT_RUN`.

## 2026-08-04 Real-chat/MiMo Ender reserve gate: failed after first pearl

The exact ordinary-player-chat -> restored saved provider -> real MiMo ->
production-skill path is verified through model selection and the first
physical acquisition:

```text
Forge 65.1.0 / Minecraft 26.2
saved provider profile restored without re-entering the API secret
TestHuman submitted the task through ordinary server chat, then disconnected
MiMo returned START_SKILL / secure_ender_pearl_reserve
the production body built the complete roof and removed its temporary pillar
the body killed one ordinary controlled Enderman and picked up one vanilla drop
terminal: move_to.stuck while RETURNING, after 1 pearl
body=(-295.91109020674463,64,-294.52219897613963)
roof centre=(-295.5,64,-294.5), remaining horizontal distance ~=0.412
```

The failure is downstream of the model and combat/pickup transaction. The
return child repeatedly accepted movement requests but made less than the
0.10-block watchdog progress threshold before exhausting four bounded
recoveries. The exact remaining boundary is whether low-speed sneaking
precision was suppressed by the production behavior/action owner, lingering
item use, or the vanilla travel magnitude at this close distance. The
deterministic direct skill gate does not reproduce the production runtime
ownership path.

Changed files in the active chain:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/AcquireShelteredEnderPearlSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/LootPickupReceiptSource.java`
- `src/main/java/dev/mcai/companion/skills/loot/VanillaLootReceiptLedger.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- focused JVM contracts under the matching `src/test` packages

Next: expose the return child's phase/action lease, current vanilla input,
item-use state, arbiter winner, and survival state in the bounded live
diagnostic; reproduce only this gate; then correct the proven owner or
precision-controller defect and rerun the same real-chat/MiMo gate to 14
server-observed pearls. Formal M0-M4 and statistical/soak gates remain
`NOT_RUN`.

## 2026-08-04 Ender reserve deterministic physical gate: PASS

The focused production-skill physical gate now passes end to end:

```text
mcai_companion:real_ender_pearl_reserve
  Forge 65.1.0 / Minecraft 26.2
  complete 3x3 roof built; temporary pillar removed
  11 controlled ordinary Endermen fought through vanilla melee
  sword durability consumed
  ordinary mob drops and player pickup transactions reached 14 pearls
  ENDER_PEARL_OBTAINED and skill COMPLETED
  PASS / 31.18 s accelerated GameTest time
```

Two additional physical failures were found and corrected before this pass:

- a killed Enderman's pearl could enter the vanilla player-pickup AABB before
  the next semantic camera sample exposed the item entity. The inventory
  increase was therefore incorrectly rejected as
  `acquire_sheltered_ender_pearl.unverified_pearl_increment`;
- `VanillaLootReceiptLedger` now records a bounded causal chain from Forge's
  `LivingDropsEvent` (killer UUID, victim UUID, exact item-entity UUID) to
  the post-inventory `PlayerEvent.ItemPickupEvent`. The child accepts an
  early inventory increment only when receipts after its start watermark
  cover that increment and match the exact bound victim, player, item, and
  dimension. A different victim remains rejected;
- after a kill, one semantic frame could retain only an Enderman
  `HOSTILE_PROXIMITY` signal while the combat child had already entered drop
  handoff. Under a still-verified roof this no longer terminates the skill.
  Contact, projectiles, fire/body hazards, low health, bad food, an unsafe
  roof, and visible non-Enderman hostiles remain rejected. The emergency
  lane still owns proximity and physical contact during drop handoff.

Directed JVM contracts for causal pickup acceptance/rejection, stale
distance-only handoff, roof memory, precision docking, and builder
positioning all pass.

Next: run the exact ordinary-player-chat -> real MiMo decision -> production
Ender reserve -> 14 server-observed pearls gate, then audit its provider,
skill, physical-state, and SQLite evidence.

Formal M0-M4 and statistical/soak gates remain `NOT_RUN`.

## 2026-08-04 Ender reserve physical rerun: active

The final-cell orbit and its immediate regressions are now corrected:

- `MoveToSkill` uses low-speed precision docking only for exact arrival
  radii at or below 0.25 blocks. Ordinary 0.30-radius build positioning
  retains the established full-speed route behaviour;
- directed contracts cover precision final-cell movement, same-cell
  correction, and the ordinary-radius non-regression;
- `BuildEndermanSafetyRoofSkill` now preserves the exact child failure code
  instead of overflowing the bounded diagnostic field into generic
  `skill_failure`;
- the safety-roof semantic memory remains fair first-person evidence but is
  bounded at 600 revisions, long enough for the sequential roof scan,
  target handoff, and one melee exchange. A directly observed contradiction
  still replaces a voxel immediately, and revision 601 is rejected.

The latest deterministic physical run built and removed the temporary
pillar, killed two ordinary controlled Endermen, picked up two ordinary
pearls, returned under the roof twice, and spawned target three. It then
failed after 500 lure ticks with `hardcore_risk_exceeded`.

Retained root cause:

- both deterministic and real-provider fixtures disabled target mob AI and
  placed each Enderman exactly 3.0 blocks from the roof centre;
- a legal centre-docking position can be 0.25-0.30 blocks toward the
  opposite roof edge, putting the target beyond the production skill's
  3.0-block initial melee limit;
- a natural Enderman would answer the lure by approaching, but this
  controlled target cannot move. The skill therefore remained in `LURING`
  until the oldest roof evidence fairly expired at revision 601.

Active correction:

- both controlled fixtures now use a named 2.5-block offset and assert that
  the target remains beyond the 3x3 roof footprint while no farther than
  2.81 blocks from the actual body position;
- this is test setup before the command. It does not grant post-command
  items, change production reach, suppress risk, or modify combat.

Next: compile, rerun the deterministic physical pearl-reserve gate to 14
server-observed pearls, then rerun the exact ordinary-player-chat -> real
MiMo -> production-skill gate.

Formal M0-M4 and statistical/soak gates remain `NOT_RUN`.

## 2026-08-04 live Ender-pearl reserve handoff: active

Resolved first boundary and retained current failure:

- `SecureEnderPearlReserveSkill` had the same runtime arbitration gap as the
  Blaze reserve: under a verified safety roof, a newly visible Enderman could
  keep the emergency lane active before `SELECTING` assigned
  `activeTargetId`;
- a narrowly scoped bridge now owns only visible-hostile proximity for a
  currently observed Enderman under a fair-evidence roof. Physical contact
  remains emergency-owned until the combat child starts;
- the directed JVM regression passes;
- the new real player chat + MiMo gate selected the correct production skill,
  physically built the complete 3x3 roof, removed its temporary pillar,
  repeatedly fought seven ordinary targets, consumed sword durability, and
  picked up ten pearls;
- the first five-minute run expired just after target seven died and the
  checkpoint entered `RETURNING`;
- a second run with bounded diagnostics disproved the initial
  deadline-only diagnosis: after the third target, the body repeated four
  points about 1.12 blocks from the roof centre while remaining in
  `RETURNING`;
- `MoveToSkill` stops and clears its final grid step as soon as the body's
  feet enter that cell, then its same-cell correction still uses full
  `forward=1.0` movement with the ordinary 12-degree alignment tolerance.
  Vanilla inertia carries the body through the centre into another adjacent
  cell, creating an endless replan/orbit cycle.

Last failed gate:

```text
mcai_companion:real_player_task_to_live_model_ender_pearl_reserve
  real player chat -> MiMo secure_ender_pearl_reserve: accepted
  roof=true, temporary pillar removed=true
  spawned/attacked targets=7, pearls=10, health=20
  checkpoint phase=RETURNING, executedTicks=6001
  result: FAIL / 5.157 min (fixture deadline)
```

Latest failed rerun:

```text
mcai_companion:real_player_task_to_live_model_ender_pearl_reserve
  real player chat -> MiMo secure_ender_pearl_reserve: accepted
  roof physically complete; targets 1-3 killed; pearls=3
  RETURNING positions repeated:
    (-296.4807,64,-293.9550)
    (-296.0450,64,-295.4806)
    (-294.5194,64,-295.0450)
    (-294.9550,64,-293.5193)
  expected roof centre=(-295.5,64,-294.5)
  result: manually stopped after proving a deterministic local-navigation
          orbit; waiting ten minutes could not change that state
```

Files in the active correction:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/test/java/dev/mcai/companion/skills/core/MoveToSkillTest.java`
- a short physical final-cell docking regression in
  `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- this checkpoint

Next: implement precision docking only for the final route cell and same-cell
micro movement, pass the directed JVM contract and short physical regression,
then rerun the exact real-chat/MiMo Ender gate to 14 server-observed pearls
and `ENDER_PEARL_OBTAINED`.

Formal M0-M4 and statistical/soak gates remain `NOT_RUN`.

## 2026-08-04 live Nether Blaze handoff: PASS twice

Resolved root cause:

- a real logged-in player submitted the ordinary completion chat request and
  disconnected;
- the configured MiMo provider correctly selected
  `secure_nether_blaze_material`;
- the supervisor accepted and started that production skill, but after three
  minutes its snapshot was still `RUNNING` with `executedTicks=0`;
- the controlled visible Blaze remained alive at full fixture health, the
  body never attacked, and no rod entered inventory;
- `SecureNetherBlazeMaterialSkill.start` enters `SELECTING`, while its
  `managesVisibleHostileProximity` and
  `managesPhysicalContactThreats` methods returned false until
  `HUNTING`. The higher-priority emergency lane therefore claims every tick
  for the already-visible Blaze and permanently suppresses the first
  supervisor tick that would transition `SELECTING` to `HUNTING`;
- the reserve skill now owns exactly that transition when its current fair
  first-person frame contains an eligible Blaze. It does not suppress
  emergency survival while no Blaze is visible.

Files changed in this slice:

- `communication/LiveModelChatGameTests.java`
- `embodiment/EmbodimentGameTests.java`
- `skills/loot/SecureNetherBlazeMaterialSkill.java`
- `SecureNetherBlazeMaterialSkillTest.java`
- the release-excluded live Blaze GameTest instance/environment JSON.

Last failed gate:

```text
mcai_companion:real_player_task_to_live_model_nether_blaze_material
  real player chat: accepted
  real MiMo decision: START_SKILL secure_nether_blaze_material
  supervisor after 3 minutes: RUNNING, executedTicks=0
  body: alive in Nether, health=20, rods=0
  controlled Blaze: alive, health=5
  result: FAIL / 3.160 min
```

Latest applicable evidence:

```text
directed SecureNetherBlazeMaterialSkill JVM tests: PASS
mcai_companion:real_player_task_to_live_model_nether_blaze_material
  run 1: PASS / 44.18 s / 7,178 tokens
  run 2: PASS / 44.93 s / 7,185 tokens
  each run: ordinary player chat -> real MiMo START_SKILL
  -> repeated vanilla sword combat/durability -> ordinary Blaze-rod pickups
  -> at least 7 rods -> BLAZE_MATERIAL_OBTAINED -> skill COMPLETED
  latest SQLite: zero failure events
```

Next: run project quality/release-JAR checks, audit the same pre-first-tick
ownership boundary in the Ender-pearl reserve controller, then close that
next real-player-chat/real-provider physical completion-route handoff.

This remains controlled inner-loop M2 evidence. Formal M0-M4, exact-JAR
Actor/Observer, 200 unseen M2 seeds, 1,000 hidden M4 seeds, and soak/statistical
gates remain `NOT_RUN`.

## 2026-08-04 live Nether build-to-entry handoff: PASS twice

Resolved root-cause chain:

- a real logged-in player submitted one ordinary completion chat message and
  disconnected;
- the configured MiMo provider selected
  `build_and_light_nether_portal`; the survival body physically consumed all
  fourteen obsidian, spent one flint-and-steel durability, built the complete
  frame, and created all six Nether-portal blocks;
- after the server-confirmed build, the planner now exposes only the
  parameterless `find_and_enter_observed_portal`, avoiding model-authored ray
  coordinates;
- MiMo selected that correct finder three consecutive times, so this is not a
  talk-only or model-selection failure;
- every local child initially failed with the retained exact code
  `enter_observed_portal.approach_stuck`. The final body position proved X had
  aligned but Z remained exactly at the scaffold edge. From the raised
  half-slab, the standing 1.8-block player intersected the obsidian top beam;
- portal approach now uses a bounded ordinary-player recovery: after measured
  direct-motion stagnation it crouches through the normal input path, lowering
  the vanilla pose and retaining enough slab-edge support to overlap the
  portal. If crouching still produces no measured progress, the existing
  bounded jump and terminal failure remain active.

Files already changed in this active slice:

- `communication/LiveModelChatGameTests.java`
- `embodiment/EmbodimentGameTests.java`
- `skills/portal/BuildAndLightNetherPortalSkill.java`
- `skills/portal/PortalBuildSkills.java`
- `runtime/MinecraftPlannerInputFactory.java`
- `skills/portal/EnterObservedPortalSkill.java`
- `MinecraftPlannerInputFactoryTest.java`
- `EnterObservedPortalSkillTest.java`
- the release-excluded live Nether GameTest instance and environment JSON.

Retained failure evidence:

```text
mcai_companion:real_player_task_to_live_model_nether_portal_build_and_entry
  real player chat: accepted
  real MiMo builder selection: accepted
  physical build/light: completed
  real MiMo finder selection: accepted three times
  local physical entry: FAIL
  exact child code: enter_observed_portal.approach_stuck
  SQLite terminal notice: repeated_identical_skill_failure
```

Latest applicable evidence:

```text
directed portal JVM tests: PASS
mcai_companion:real_player_task_to_live_model_nether_portal_build_and_entry
  run 1: PASS / 33.40 s
  run 2: PASS / 34.85 s
  real player chat -> real MiMo builder -> 14 ordinary obsidian placements
  -> one flint-and-steel durability -> six portal blocks -> real MiMo finder
  -> crouched physical entry -> We Need to Go Deeper -> same body in Nether
  latest SQLite: 14,748 tokens, both skills COMPLETED, zero failure events
```

Next: run the project-level quality/release-JAR checks, then close the next
continuous M2 handoff from verified Nether entry to the existing physical
blaze-material reserve capability under real model control.

This remains controlled inner-loop M2 evidence. Formal M0-M4, exact-JAR
Actor/Observer, 200 unseen M2 seeds, 1,000 hidden M4 seeds, and soak/statistical
gates remain `NOT_RUN`.

## 2026-08-04 portal handoff and real Slime defense: resolved

Resolved root-cause chain:

- the real-player-chat/real-MiMo late-End chain physically completed
  activation, ordinary End entry, dragon combat, return-portal discovery, and
  ordinary return with the same body and goal, but its first SQLite audit
  recorded the entry child as failed;
- the compound entry wrapper lengthened the child failure beyond
  `SkillFailure`'s 64-character contract, hiding it as `skill_failure`;
- preserving the exact child code exposed
  `dimension_changed_before_entry`. The End portal's vanilla inside shape
  ends at 12/16 block height, while the commit envelope measured a standing
  player's feet from the block's integer Y. One ordinary movement/gravity
  step could therefore traverse before commitment was recorded;
- after that fix, the full chain exposed a separate physical failure: a
  natural Slime killed the diamond-armored companion while logs repeatedly
  said `GUARDING`. The controller released and restarted shield use every
  tick, so vanilla never completed the continuous shield warmup.

Implemented:

- committed traversal tolerates at most ten temporarily absent live frames;
  pre-commit absence still fails closed, the wait is bounded, and no movement
  is emitted while the body cannot be verified;
- the End-portal commit envelope is measured from vanilla's 12/16 portal
  surface and remains horizontally bounded to the observed portal cell;
- compound portal entry preserves the exact bounded child failure code;
- continuing guard keeps one uninterrupted shield-use action; a real state
  transition still releases it before attack, retreat, food, or equipment;
- a release-excluded physical Slime gate begins with the hostile behind the
  survival body and requires sustained shield use, weapon equip and
  durability, emergency ownership, footwork, survival, and a real kill.

Retained failure evidence:

```text
portal audit attempt 1:
  physical PASS but entry recorded generic skill_failure
portal audit attempt 2:
  physical PASS / 29.56 s but exact result was
  enter_observed_portal.dimension_changed_before_entry
late-End rerun before shield fix:
  FAIL / 2.999 min / natural Slime killed the body while telemetry repeatedly
  reported GUARDING; the respawned body then lost the portal and timed out
```

Latest applicable evidence:

```text
directed portal + emergency JVM tests:
  PASS
real-player-chat + real-MiMo activation -> entry:
  PASS / 32.60 s
  SQLite: both skills COMPLETED, zero failure events
mcai_companion:real_emergency_slime_defense:
  PASS / 5.507 s
  hostile approached from behind; uninterrupted vanilla shield warmup,
  diamond-sword equip/durability, cooldown attacks, footwork, and real kill
real-player-chat + real-MiMo late-End completion chain:
  PASS / 1.526 min
  same goal/body activated, entered, killed the dragon, obtained Free the
  End, found the return portal, and returned Overworld
  SQLite: activation, entry, dragon fight, and return all COMPLETED;
  zero failure events
check + verifyReleaseJar:
  PASS
```

Files added or materially changed for this recovery include:

- `runtime/MinecraftPlannerInputFactory.java`
- `communication/LiveModelChatGameTests.java`
- `embodiment/EmbodimentGameTests.java`
- `skills/portal/PortalSkillPolicy.java`
- `skills/portal/EnterObservedPortalSkill.java`
- `skills/portal/FindAndEnterObservedPortalSkill.java`
- `skills/core/EmergencySurvivalController.java`
- focused planner, portal, and emergency tests;
- release-excluded activation/entry, late-End, and Slime GameTest resources.

Formal M0-M4, exact-JAR Actor/Observer, 200 unseen M2 seeds, 1,000 hidden M4
seeds, and the required soak/statistical gates remain `NOT_RUN`.

## 2026-08-04 live-model End victory/return handoff: PASS twice

Resolved root-cause chain:

- a nearby hostile let the emergency lane claim every server tick, while the
  model control plane and active-skill body tick both lived in the suppressed
  lower lane; this reproduced the field symptom "acknowledges the task but
  never acts";
- `BrainOrchestrator.tickPlanningOnly()` now advances only model
  request/response and may start an authorized skill while emergency reflexes
  retain exclusive body control for that tick;
- the model was being asked to invent dragon-combat rally coordinates and six
  exact return-portal face fields. `fight_ender_dragon` and
  `find_and_enter_observed_portal` are now parameterless and bind live body
  position plus current first-person semantic portal evidence locally;
- the deterministic live arena froze its dragon with no-AI. After physical
  kill credit, vanilla's 200-tick death animation could not progress and the
  still-hostile entity correctly kept portal movement suppressed. The
  release-excluded fixture now restores ordinary dragon AI immediately after
  the server-authoritative kill;
- the following run exposed a production problem rather than hiding it:
  emergency survival could hold a shield forever without spending a ready
  melee attack, and generic entity aim published an Enderman's eyes first.
  Emergency survival now equips an owned ordinary melee weapon, attacks at
  contact when its cooldown is ready, and uses the shield as cooldown cover.
  Fair perception publishes Enderman body aim first and eye aim only as a
  fallback. Unseen teleported targets are still not tracked through walls.

Files added or materially changed in this slice:

- `brain/BrainOrchestrator.java`
- `runtime/CompanionRuntime.java`
- `runtime/SurvivalRouteTracker.java`
- `runtime/MinecraftPlannerInputFactory.java`
- `skills/core/EmergencySurvivalController.java`
- `perception/FairPerceptionSampler.java`
- `skills/combat/FightEnderDragonParameters.java`
- `skills/combat/DragonCombatSkillParameters.java`
- `skills/combat/FightEnderDragonSkill.java`
- `skills/portal/FindAndEnterObservedPortalSkill.java`
- `skills/portal/PortalSkills.java`
- `model/KnownSkillArgumentCanonicalizer.java`
- `communication/LiveModelChatGameTests.java`
- `embodiment/EmbodimentGameTests.java`
- focused unit tests and GameTest resources, including
  `real_emergency_enderman_defense.json`.

Retained failure evidence:

```text
live-model attempt 1:
  expected FAIL / emergency lane starved all model planning
live-model attempt 2:
  dragon physically killed; exact return-portal argument contract failed
live-model attempt 3:
  real MiMo selected both parameterless skills; expected FAIL because the
  release-excluded deterministic dragon remained no-AI during death animation
live-model attempt 4:
  dragon handoff worked; natural Enderman killed the companion, exposing
  shield-only emergency combat and unsafe eye-first aim; run stopped
```

Latest applicable evidence:

```text
directed brain/route/combat/portal/planner tests: PASS
mcai_companion:real_end_victory_and_return:
  PASS / 6.800 s
  body began facing away, scanned first-person evidence, entered normally
mcai_companion:real_emergency_enderman_defense:
  PASS / 1.751 s
  real survival body equipped its sword from inventory, spent durability,
  guarded, counterattacked and moved until the naturally teleporting hostile
  was killed or repelled below half health and no longer fairly visible
live-model attempt 5:
  PASS / 53.00 s
  ordinary player chat, player disconnect, real MiMo selected
  fight_ender_dragon, physical kill/Free the End, real MiMo selected
  find_and_enter_observed_portal, ordinary return
live-model consecutive rerun:
  PASS / 52.87 s / Gradle BUILD SUCCESSFUL
```

These are controlled inner-loop End-stage gates, not a full unknown-seed
Hardcore completion. Next: run the focused JVM regression and project-wide
`check`, then continue closing the next unverified physical route boundary.
Formal M0-M4, exact-JAR Actor/Observer, 200 unseen M2 seeds, and 1,000 hidden M4
seeds remain `NOT_RUN`.

## 2026-08-03 live-model End-portal handoff: PASS twice

The high-level/local handoff no longer asks the model to invent an End portal
center. `activate_observed_end_portal` is now parameterless. A local geometry
resolver intersects only current first-person-visible frame positions and
their visible `facing` state; one frame is ambiguous and fails closed.
Stronghold DFS continues until the same current observation proves exactly one
canonical ring center.

Applicable gates:

```text
directed geometry/stronghold/planner tests: PASS
compileJava + processResources: PASS
mcai_companion:real_end_portal_activation:
  PASS / 2.624 s
mcai_companion:real_player_task_to_live_model_end_portal_activation:
  first attempt: fixture bug, no provider request (GridPos != BlockPos)
  second attempt: expected FAIL / creative-mode Eye was not consumed
  third attempt: PASS / 26.75 s / live MiMo + real player chat
  consecutive rerun: PASS / 15.04 s / live MiMo + real player chat
```

The successful live gate restored the configured profile and credential
without asking for the API key, logged in a real test player, accepted one
ordinary chat task, removed that player, received a real
`START_SKILL: activate_observed_end_portal` decision from the configured model,
then used the survival `ServerPlayer` body to consume all twelve owned Eyes and
activate all nine portal blocks.

The creative-mode failure was a useful release-excluded fixture defect: the
world changed but inventory did not, so production
`eye_consumption_unverified` correctly rejected the attempt. The fix set the
test body to survival and retained the strict production check.

Files added or materially changed in this slice:

- `skills/portal/ObservedEndPortalGeometry.java`
- `skills/portal/ActivateObservedEndPortalSkill.java`
- `skills/stronghold/SearchObservedStrongholdPortalRoomSkill.java`
- `communication/LiveModelChatGameTests.java`
- `runtime/MinecraftPlannerInputFactory.java`
- `model/KnownSkillArgumentCanonicalizer.java`
- `embodiment/EmbodimentGameTests.java`
- focused unit tests and two live GameTest resources.

Next: validate the model-selected End-entry/dragon/return handoff, first
removing any remaining requirement for the model to invent world coordinates.
This remains controlled M2 evidence, not a hidden random-seed Hardcore
completion. Exact-JAR Actor/Observer, hidden-seed statistics, and formal
M0-M4 remain `NOT_RUN`.

## 2026-08-03 stronghold interior search and activation: PASS twice

Resolved root-cause chain:

- the first implementation looked level/upward, so adjacent air was visible
  but no future standing support had current first-person evidence;
- an initial downward fix used 24 views per station and worked physically,
  but made dead-end backtracking take hundreds of ticks;
- the final scan uses four overlapping horizontal views. Each is pitched
  down 20 degrees, so the existing 70-degree vertical ray fan covers slightly
  upward head clearance, body clearance, and the adjacent sturdy top in the
  same observation revision;
- each DFS station retains its original yaw, and every parent edge is
  reobserved before backtracking. Unknown cells remain forbidden.

Files changed for this active slice:

- `src/main/java/dev/mcai/companion/skills/stronghold/SearchObservedStrongholdPortalRoomSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/StrongholdSkills.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/stronghold/SearchObservedStrongholdPortalRoomSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkillTest.java`

Latest applicable gates:

```text
directed JVM stronghold/planner tests: PASS
compileJava: PASS
mcai_companion:real_stronghold_reach: PASS twice / 12.67 s and 12.63 s
mcai_companion:real_end_portal_activation:
  PASS / 2.360 s
  PASS after diagnostic cleanup / 2.408 s
```

The focused physical fixture is now a ceilinged stone-brick maze. Completion
requires the headless survival player to enter an occluded dead branch,
backtrack to the junction, take a second corridor with two turns, see a portal
frame through the ordinary semantic fan, consume twelve owned Eyes through
ordinary use interactions, and verify the resulting 3x3 End portal. The test
does not teleport the body along the route or modify the portal after start.

Next: close the physical chain from the activated End portal through ordinary
entry, End-island navigation, dragon victory, and the return portal. Exact-JAR
Actor/Observer, hidden-seed statistics, and formal M0-M4 remain `NOT_RUN`.

## 2026-08-03 End entry, dragon victory, and return: PASS twice

A new focused physical gate now covers the remainder of the controlled M2
victory slice:

```text
mcai_companion:real_end_victory_and_return
run 1: PASS / 5.654 s
run 2: PASS / 5.693 s
```

The same headless survival `ServerPlayer`:

- entered an already activated End portal through
  `enter_observed_portal`, preserving UUID and ordinary dimension lifecycle;
- walked from End spawn, stopped at an observed void gap, placed exactly three
  owned cobblestone blocks, crossed them, and travelled materially to the
  arena;
- used `fight_ender_dragon` to destroy a visible crystal, mine an iron-bar
  cage with pickaxe durability, consume normal arrows, and sustain enough
  close melee to consume at least twenty sword durability against a full
  health dragon;
- received server-side dragon-kill credit and waited through the vanilla
  dragon death phase;
- entered a first-person-observed return portal, issued exactly one headless
  `WIN_GAME` response, returned to the Overworld, and retained the same UUID
  plus verified `DRAGON_KILLED` and `RETURNED_FROM_END` milestones.

The first attempt failed before End entry because its release-excluded fixture
placed the body four blocks from a horizontal portal surface; matching the
already proven three-block player vantage fixed the fixture without changing
production perception or portal code.

Next: verify the live high-level planner/model handoff selects this validated
skill chain from verified completion phases. The two physical gates are
controlled component evidence, not a zero-human random-seed completion run.
Exact-JAR Actor/Observer, hidden-seed statistics, and formal M0-M4 remain
`NOT_RUN`.

## 2026-08-03 stronghold-reach physical gate: PASS

The focused M2 inner-loop gate now passes twice consecutively, including once
after removing all temporary high-frequency diagnostics:

```text
Gate: mcai_companion:real_stronghold_reach
Forge: 65.0.0 / Minecraft 26.2 / Java 25
Run 1: PASS / 12.67 s
Run 2 after diagnostic cleanup: PASS / 12.63 s
Repository check: PASS
```

The production headless player consumed a goal-scoped intersection made from
two measured Eye traces, walked more than 80 physical blocks through its
normal moving-player chunk window, descended underground with ordinary
collision and mining, consumed iron-pickaxe durability, placed owned torches,
and stopped only after stone bricks entered its current first-person semantic
frame. The observed stronghold wall remained intact.

The physical failure chain exposed and corrected seven distinct causes:

1. a 0.6-wide body straddled the next lower cell, so the support guard
   correctly denied mining; descending excavation now recentres through normal
   movement before selecting its next block;
2. post-break AIR could disappear for one semantic fan or lag one revision
   during a committed step; only exact ordinarily mined and then reobserved
   cells receive the bounded bridge;
3. mapper-inferred `BODY_CONTACT` could overwrite directly observed AIR while
   an adjacent stair supported the body; direct multi-ray AIR now wins over
   that inference;
4. verified support disappeared when the camera turned forward; a committed
   step retains that exact support unless current evidence proves it unsafe;
5. descending clearance omitted the high-side head transition cell; the skill
   now mines and verifies all three required cells;
6. a lighting interval treated an off-camera floor as a missing floor; it now
   stops, looks down, reobserves, and places through the ordinary use path;
7. descending evidence was clipped by a horizontal-only Manhattan envelope;
   its bound now accounts for both horizontal and vertical travel.

The last pre-pass failure was
`reach_observed_stronghold.safe_depth_limit`. That exposed a test-fixture
error: one isolated marker block was not on any valid tunnel from the
intersection uncertainty radius. The fixture now models a fully buried
stone-brick wall through the search volume. It remains invisible from the
surface, exposes no coordinate to production code, and better represents an
actual multi-block stronghold.

Primary changed files:

- `src/main/java/dev/mcai/companion/skills/mining/ExcavateSafeTunnelSkill.java`
- `src/main/java/dev/mcai/companion/navigation/PerceptionNavMapper.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/mining/ExcavateSafeTunnelSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/mining/MiningSkillTestFixtures.java`
- `src/test/java/dev/mcai/companion/skills/interaction/PlayerSupportBlockGuardTest.java`
- `src/test/java/dev/mcai/companion/navigation/PerceptionNavMapperTest.java`

Next: close the physical handoff from observed stronghold evidence to bounded
portal-room discovery, then reverify portal activation, End entry, dragon
victory, and return-portal entry as one causal M2 slice. This pass is
controlled component evidence only. Exact-JAR Actor/Observer, 200 unseen
Hardcore seeds, and formal M0-M4 remain `NOT_RUN`.

## Exact configured-MiMo zero-human foundation gate: PASS

The first exact rerun after the exterior-vantage and bounded-turn fixes passed
end to end:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: PASS / 8.116 game-test minutes / BUILD SUCCESSFUL in 8m 15s
Human players online: 0
Model: configured MiMo profile restored without re-entering credentials
Model evidence: live START_SKILL decisions for resource preparation,
                food, iron toolkit, workstations and compact shelter
Physical evidence: vanilla log/stone/iron pickup, furnace wait, crafting,
                   52 causal shelter placements, verified completion
Roof-return evidence: exterior survey -> observed doorway corridor ->
                      interior placement; no repeated exhausted candidate
Terminal: model returned COMPLETE_GOAL after the gate verified the foundation
```

The run crossed the previously failing `confirmed=48` roof-return section,
walked eleven fairly observed return cells, retargeted newly visible supports,
placed the remaining roof faces and completed the goal. The gate did not need
the new rejected-destination fallback in this seed, but it did exercise the
per-step exterior exhaustion and bounded movement path.

Next: repeat this exact live gate on a fresh generated world for consecutive
evidence, then run the full JVM regression. This is a verified M1 foundation
scenario, not the M1 statistical exit criterion and not M2-M4 completion.
Formal M0-M4 status remains NOT_RUN.

## Consecutive rerun: FAIL on elevated exterior roof recovery

The immediately following fresh-world rerun failed honestly:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 9.911 game-test minutes / BUILD FAILED in 10m 3s
Progress: 49 causal placements confirmed
Checkpoint: phase=AIMING, stepIndex=52, deferredAimSteps=2
Body: outside the footprint at feet y=-43
Plan floor: y=-44
Terminal: repeated_skill_failure_without_progress
```

The resource, food, iron-toolkit and workstation chain completed. During the
remaining roof work, an ordinary aim-reposition MoveTo stalled beside a
natural one-block rise. Its requested stand was at the plan floor, but the
body remained outside one block higher. Roof-return classification currently
requires exact equality with the plan-floor Y coordinate. That elevated
outside position was therefore neither an exterior return position nor an
interior construction position. Three remaining roof steps repeatedly aimed
through already placed planks, were deferred, and retried in later model
batches until the supervisor correctly entered safe idle.

Next directed fix:

1. Treat fairly observed exterior return terrain within the local one-block
   slope band as exterior for return purposes (not as a legal placement
   shortcut).
2. Let the observed return graph descend/ascend one block per horizontal hop,
   still excluding the complete building footprint and still using MoveTo.
3. Reject a stalled descent destination and try another observed route using
   the existing bounded return budget.
4. Add first-red contracts for an elevated exterior body and a one-block
   observed descent before rerunning the physical roof and exact live gates.

Implemented and directed verification:

```text
elevatedExteriorRoofStandStillReturnsThroughTheDoor:
  RED on the old classifier, PASS after the fix
roofInteriorReturnCanDescendOneObservedExteriorStep:
  RED on the old graph, PASS after the fix
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 2.590 min
```

`isExteriorRoofReturnPosition(...)` now admits only a one-block local slope
band for return navigation while `isExteriorRoofApronPosition(...)` remains
strictly at the plan-floor Y for construction. The return graph allows one
horizontal hop with at most one block of vertical change and still excludes
the complete footprint. Aim MoveTo recovery also treats the new bounded
`move_to.turn_stuck` result as an alternate-vantage condition.

Next: exact configured-MiMo, player-style, zero-human foundation rerun.

## Next live rerun: FAIL on exhausted-step batch restart

The next fresh live rerun reached 47 causal placements but failed after
8.608 game-test minutes:

```text
Checkpoint: confirmed=47, stepIndex=43
deferredAimSteps=1
exhaustedExteriorRoofSteps=2
returningInsideForRoof=false
Terminal: repeated_skill_failure_without_progress
```

Two outer roof steps had already exhausted their exterior searches. That
per-step fact survived correctly, but a later model-authorized skill batch
reset the local attempt counter to zero while the body was still outside.
Exterior candidates were correctly suppressed by the persistent bit, yet the
return predicate looked only at the reset counter. With neither a candidate
nor a return transition, the same two steps were deferred across three
batches.

The first-red extension to
`exhaustedOuterRoofRepositionReturnsThroughDoor` now proves that an exhausted
step must return even with `completedExteriorAttempts=0`. The return predicate
accepts the persistent exhaustion flag, and the actual
`startAimReposition(...)` call passes the current step's bit. Full
`BuildShelterStepSkillTest` passes.

Next: physical roof gate, then the exact configured-MiMo zero-human gate.

## Active failure: generic exterior-vantage bypass and unbounded turn alignment

The post-fix exact configured-MiMo, player-style, zero-human foundation gate
ran the full fair resource chain, restored the saved model profile without
human input, acquired logs/food/cobblestone/seven raw iron, performed normal
furnace waits, crafted the iron toolkit and workstations, gathered 55 planks
plus doors, selected a natural site, and reached 50 causal shelter placements.
It then failed honestly:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 12.11 real-time minutes / tick 14531
Plan origin: [-9355563,-44,14520332]
Checkpoint: phase=RELOCATING, stepIndex=46, confirmed=50
Relocation goal: [-9355556.5,-44,14520331.5]
Return state: returningInsideForRoof=true
Return attempts: 9/24, explored stands=6
Terminal: no verified dynamic shelter before the deadline
```

The previous fix did stop exterior-exhausted steps from using doorway staging
and the apron-frontier branch, but the generic
`aimingVantageCandidates(...)` path could still select an exterior apron cell
for the same marked step. Later, the observed return graph explored around an
interrupted natural apron and started an adjacent `MoveTo`; its look alignment
never crossed the movement threshold. `MoveToSkill.aimAndMove(...)` returns
before its positional stuck watchdog whenever angular error is high, so that
single child movement remained RUNNING for roughly 155 seconds until the
outer gate expired.

Next fixes:

1. Apply the exterior-exhausted flag to every generic aim-vantage candidate,
   not only the two exterior-specific staging branches.
2. Make movement alignment itself bounded: persistent failure to align must
   replan and ultimately return `move_to.stuck`, rather than run forever.
3. Let a roof-return compound reject the failed destination and select another
   fairly observed corridor step within its existing 24-move budget.

Then rerun the directed JVM contracts, `roof_jump_placement`, and this exact
configured-MiMo zero-human gate. Formal M0-M4 status remains NOT_RUN.

Implemented and directed verification:

```text
roofAimFromExteriorCannotShortcutAcrossTheCompletedWall: PASS
roofInteriorReturnAdvancesToObservedFrontierBeforeDoorIsSafe: PASS
persistentTurnMisalignmentCannotRunForever: PASS
BuildShelterStepSkillTest + MoveToSkillTest: PASS
```

Every generic aim candidate now receives the per-step exterior-exhausted flag.
`MoveToSkill` separately bounds turn-alignment stall and total alignment time,
performs at most the existing four local recoveries, then returns
`move_to.turn_stuck`. A roof return catches that bounded failure, rejects the
failed destination from its observed graph, and selects another route within
the existing compound budget. The checkpoint now includes rejected traversal
and exhausted exterior-step counts. Next gate: physical
`roof_jump_placement`.

Physical result:

```text
mcai_companion:roof_jump_placement: PASS / 2.598 min
Evidence: confirmed=50 return completed; final inner roof completed
No unbounded MoveTo and no traversal-budget overrun observed
```

Next gate: exact configured-MiMo player-style zero-human foundation rerun.

## Active bug: exterior-exhausted roof steps restart their exterior route

The exact prior configured-MiMo foundation run failed after 8.989 real-time
minutes at `confirmed=54`, `stepIndex=47`, `phase=AIMING`. The body was already
inside, but every model-batch restart aimed the final inner roof placement at
the underside of an adjacent roof block instead of the remembered SOUTH
support. `tickAim(...)` reached its timeout before the wrong-block branch could
request a new stance, while side-face reposition candidates started two cells
away and omitted the adjacent interior stance.

The current source now tries a bounded physical aim reposition before deferring
an AIMING timeout and includes side-face candidates one cell away. The
first-red contract
`finalInnerRoofAimTimeoutTriesAdjacentRepositionBeforeDeferral` and the full
`BuildShelterStepSkillTest` class pass.

The subsequent physical `mcai_companion:roof_jump_placement` run exposed a
different deterministic loop before reaching a terminal result, so it was
stopped rather than reported as passing:

```text
Plan origin: [-10609338,-42,9373149]
Progress: confirmed=46
Repeated selection: stepIndex=38 and stepIndex=41
Return traversal counter: advanced beyond its declared budget from 24/24
                          through 30/24
Observed loop: interior -> exterior staging -> failed exterior aim ->
               inner-roof return corridor -> earlier roof step selected again
```

Inspection established that steps 38 and 41 were still genuinely pending; the
bug was not a false confirmation. They had exhausted their fair exterior
vantages, returned through the doorway, then lost that per-step fact and
started the same exterior route again. The implementation now records
exterior-exhausted roof step indexes, suppresses both doorway staging and
apron search for those steps after return, resets traversal accounting on
confirmed placement and on the start of a return, and enforces the budget
before every compound traversal move. Changed files in this active chain are:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/PlacementSupportPreferenceTest.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkills.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Directed verification now passes:

```text
innerRoofObservationStaysInsideAfterBatchResume: PASS
BuildShelterStepSkillTest: PASS
```

The first-red overload contract proves that a fresh outer step may stage
outside, while the same step after exterior exhaustion may not. Last physical
gate result:

```text
mcai_companion:roof_jump_placement: PASS / 2.606 min
Progress evidence: confirmed 44 -> 46 -> 50 -> complete
Budget evidence: every roof return restarted at 1/24; no overrun
```

Next: rerun the exact configured-MiMo player-chat/zero-human foundation gate.
Formal M0-M4 status remains NOT_RUN.

## Active rerun: inner-roof batch resume must stay inside

The next exact configured-MiMo, player-style, zero-human foundation run proved
the bounded outer-roof fallback on a different natural site, but exposed a
second entry into the same wasteful route:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 11.29 real-time minutes / tick 13551
Checkpoint: stepIndex=47, confirmed=54, phase=SURVEYING
Plan origin: [12094519,-44,-8170907]
Terminal: repeated interior -> doorway -> exterior -> interior loop
```

The run physically completed the full resource/workstation chain and advanced
from 44 to 54 causal shelter placements. The new outer fallback triggered at
five attempts and correctly walked the observed detour and doorway into the
shelter. When the model authorized the next construction batch, however,
`roofObservationStaging(...)` treated the remaining priority-positive inner
roof step like a priority-zero outer face. From an interior floor cell it
moved to the doorway and exterior; `startAimReposition(...)` then immediately
recognized the inner roof and returned through the same doorway. Each useless
round trip consumed about seventeen real-time seconds until the deadline.

A first-red contract,
`innerRoofObservationStaysInsideAfterBatchResume`, now distinguishes the two
roles. Only a priority-zero outer roof target may stage from the interior
through the doorway to inspect exterior support. A priority-positive inner
roof target retains the already-safe interior body position and performs
ordinary fresh observation/reposition there.

Current directed verification:

```text
exhaustedOuterRoofRepositionReturnsThroughDoor: PASS
innerRoofObservationStaysInsideAfterBatchResume: PASS
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 1.427 min
```

Next: rerun the exact configured-MiMo zero-human foundation gate. Formal
M0-M4 status remains unchanged until their stated statistical gates run.

## Active rerun: bounded outer-roof fallback through the doorway

The latest exact configured-MiMo, zero-human dedicated foundation gate failed
honestly after completing the entire physical foundation chain and 44 causal
shelter placements:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 11.24 real-time minutes / tick 13487
Checkpoint: stepIndex=42, confirmed=44, phase=AIMING
Executor state: aimRepositionAttempts=23, returningInsideForRoof=false
Terminal: no verified dynamic shelter before the gate deadline
```

This was neither model silence nor a fabricated placement receipt. The
remaining priority-zero roof face needed an exterior stance on the interrupted
side of the natural site. The executor physically traversed its observed
apron, but after exhausting useful exterior vantages it repeated already
observed transit cells instead of using the known doorway and completing the
same reachable face from the interior.

A first-red contract,
`exhaustedOuterRoofRepositionReturnsThroughDoor`, established the missing
transition. `BuildShelterStepSkill` now preserves the ordinary exterior search
for a fresh outer-ring target, bounds it to one generated shelter side, and
then reuses the fairly observed exterior-detour -> doorway -> interior state
machine. Inner roof faces still return immediately. The fallback performs
ordinary `MoveToSkill` traversal and normal vanilla placement; it adds no
teleport, chunk read, hidden observation, or direct block mutation.

Files changed in the active bug chain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/PlacementSupportPreferenceTest.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkills.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Current directed verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 1.419 min
Evidence: returningInside=true, incremental observed corridor traversal,
          interior retargeting and continued physical roof placement
```

Next: rerun only the exact configured-MiMo, player-style, zero-human
foundation gate. If it passes, repeat it for consecutive live evidence before
running the complete JVM regression. Formal M0-M4 status remains unchanged.

## Current failure: inner roof incorrectly searches the exterior apron

The post-return-optimization exact configured-MiMo, zero-human dedicated
foundation gate failed honestly at its real-time deadline:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 11.89 real-time minutes / tick 14254
Checkpoint: confirmed=47, phase=REPOSITIONING_FOR_AIM
Pending: inner-roof steps 46, 47, 49, 51, and 52
Terminal: no verified dynamic shelter
```

The physical resource chain completed through logs, crafting, stone, food,
seven iron ore, vanilla smelting, iron pickaxe, workstations, shelter
materials, walls, and the outer roof. The failure is not a model timeout or a
false receipt. After the 5x5 roof outer ring was complete, the executor
selected inner-ring roof cells while the body was still on the exterior
ground apron. `startAimReposition(...)` treats every roof step alike and
continues searching that apron even though completed outer roof blocks
necessarily occlude the inner ring. The trace repeatedly walked a full ring,
deferred steps 47, 46, and 49, then selected the same remaining inner steps
until the gate deadline.

Files already changed in the preceding optimization remain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/PlacementSupportPreferenceTest.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkills.java`

Last completed gates before this failure:

```text
./gradlew check: PASS / 821 tests passed, 2 skipped
mcai_companion:roof_jump_placement: PASS / 1.422 min
```

Next: add a first-red contract distinguishing outer-ring from inner-roof
work. When an inner roof step needs repositioning while the body is on the
exterior apron, enter the existing fairly observed apron -> doorway ->
interior return state machine instead of scanning another exterior circuit.
Then run the affected JVM class, the physical roof gate, and the exact
configured-MiMo zero-human gate. This failure keeps formal M0-M4 status
unchanged and does not permit an M1 claim.

That transition was implemented and passed both the focused JVM class and
the complete physical roof gate:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 1.427 min
```

The next exact MiMo zero-human run reached the new transition at 46 confirmed
structural blocks, but failed honestly after 8.730 minutes. Its generated
site had an interrupted one-cell apron: from the east side only two adjacent
apron cells were currently safe, while the doorway apron cell was not yet
visible/safe. The return helper currently builds its graph exclusively from
the one-cell apron, so it reported `no_observed_traversal_stand` twice and the
goal entered `repeated_skill_failure_without_progress`.

The next root fix is therefore a fair exterior detour graph, not another
retry-budget increase. It may use ordinary already observed safe ground in a
small band outside the shelter when the one-cell apron is interrupted, while
hard-excluding the entire building footprint so stale pre-build AIR can never
become a through-wall route. A first-red broken-apron/detour contract precedes
the implementation.

## Active bug: roof support face selected from the impossible side

The latest exact configured-MiMo, zero-human dedicated-server foundation gate
ran the full physical chain through logs, basic crafting, stone, food, seven
raw iron, normal smelting, an iron pickaxe, workstations, shelter materials,
site search, and 43 causally confirmed shelter placements. It failed after
9.983 active minutes:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / tick 11822
Terminal detail: repeated_skill_failure_without_progress
Skill failure: build_shelter_step.aim_timeout_crosshair_wrong_block
Plan: 7ee6e7d02b0fb28b
Origin: [7822128,-44,9126242]
Pending step: 39 / ROOF / target [7822132,-42,9126246]
Chosen support: [7822131,-42,9126246] EAST face
Final feet: approximately [7822128,-44,9126247]
Actual crosshair: nearer oak planks at [7822130,-42,9126246]
```

The prior heuristic-AIR contradiction did not recur. The first construction
batch completed normally at 42 confirmed placements, and the second reached
43. The new root is geometric: `aimingVantageCandidates(...)` checks a
sampled line for known solid voxels, but `hasObservedAimLine(...)` skips the
support block and does not prove that the eye is in the selected face's outer
half-space. A body west of a support can therefore be treated as a candidate
for its EAST face even though the support's own west face necessarily
occludes that east face. Exterior apron exploration eventually exhausts from
the wrong side, and three model-authorized retries safely terminate.

Files already changed for the immediately preceding fixes:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`

The next step is to add a first-red unit contract for face-side visibility,
reject impossible vantages before movement, and permit a fresh visible support
face for the same roof target to supersede the stale remembered face. Then run
the affected unit class, the physical roof-jump gate, and this exact real-MiMo
zero-human gate again. Do not claim M1 completion from these gates.

The saved Anvil chunk was subsequently decoded read-only. Its physical state
matched the causal count exactly: 30 wall blocks plus 13 roof blocks were
present, and the four missing outer-roof cells were all on the far right
edge. This ruled out both a ghost confirmation and a successfully placed block
whose receipt was lost.

The more precise control-flow cause is that the fixed eight-attempt roof
budget counted every cardinal transit hop, including repeated already visited
cells on a bounded route to a new apron frontier. A targeted support look also
does not necessarily observe the next adjacent footing around an opaque
corner. The body reached the southeast corner, could not prove the next east
apron footing, retreated over known cells, and exhausted eight moves.

Implemented:

- a generated, scale-bounded roof reposition budget covering two complete
  apron perimeters instead of eight transit moves;
- a brief downward path glance toward one adjacent unobserved apron footing;
- the unknown footing is camera-only until a newer semantic observation proves
  feet/head/support, and is never sent directly to movement;
- a 40-tick hard deadline falls back to ordinary support aiming;
- no full 24-view panorama is restored for each one-cell hop.

Verification completed:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
PASS / 2.659 min / complete physical roof and causal inventory oracle
```

The exact configured-MiMo zero-human foundation gate subsequently passed:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
PASS / 9.809 real-time minutes / 57 of 57 shelter steps confirmed
```

The run had no human player logged in. It completed the real-model decision
chain, physical log/stone/food/iron collection, vanilla smelting, iron pick,
workstation/material preparation, shelter construction, door, light, and a
final model `COMPLETE_GOAL` decision. At the previously failing opaque corner
the new path executed exactly as intended:

```text
Glancing toward unobserved shelter apron footing
Completed targeted shelter apron footing refresh
```

It then advanced from 45 to 47 confirmed roof placements and ultimately to
57/57. This proves the current corner fix on one exact real-model run; it is
not M1 completion and is not yet consecutive-run stability evidence.

The same run exposed the next measured naturalness/performance defect. After
roof work, the body returned from the far apron to the doorway through 14
ordinary one-cell moves. `tickRelocation(...)` started
`SurveySurroundingsSkill(8, true)` after every hop, making each known-corridor
cell cost about 8.4 seconds. The path was legal and completed, but an expert
player would continuously traverse already observed safe cells. The next
change must wait for the semantic frame to acknowledge each physical arrival,
chain the next already observed cardinal return hop immediately, and retain a
full survey only at an unknown frontier or after entering the shelter.

That optimization is now implemented. A physical movement completion cannot
chain from stale feet: the executor waits up to 20 ticks for the lower-rate
semantic frame to identify the exact destination grid cell. It then continues
one already observed cardinal corridor hop at a time. If the frame does not
refresh, or the next cell is not fairly observed safe, the original panoramic
survey remains the fallback.

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
PASS / 1.422 min
```

In the physical run, twelve return-corridor moves that previously each paid
for an eight-second panorama were chained through ordinary MoveTo and
arrival-observation receipts. The overall gate improved from 2.659 to 1.422
real-time minutes without teleporting, skipping cells, or weakening the
physical/causal oracle. The exact real-model zero-human gate must be rerun
because this production behavior changed.

## Resolved shelter bug and proven root cause

The current inner-loop gate is
`mcai_companion:real_player_task_to_live_model_shelter_relocation`. It sends a
player-style Chinese chat request through the production dialogue/model/skill
chain and uses the configured MiMo model.

The original failure was real: the AI physically placed all 55 structural
blocks and the door through the vanilla player interaction path, then
validation interpreted the door target's passable navigation voxel as `AIR`.
It revoked the causal placement confirmation, reported
`build_shelter_step.completed_block_missing`, and prevented the final light
step from starting.

`BuildShelterStepSkill.confirmedPlacementContradicted(...)` now distinguishes
solid structural blocks from collisionless/partial functional blocks. A
confirmed door or light is revoked only when a fair first-person semantic
observation ray-hits a different block at the exact target. Navigation
passability no longer erases a causal door receipt.

Three consecutive independent real-time runs of the player-chat -> configured
MiMo -> production skill -> vanilla world path now pass. Each reached
`confirmed=57`, placed the door normally, placed the light normally, and
passed the independent physical shelter oracle. The third run also recovered
safely from one model `invalid_skill_arguments` response.

This is strong evidence for this one inner-loop scenario, not a declaration
that M1 or the product is complete.

## Relevant changes already present

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
  - bounded aim-reposition progress and deadline;
  - wall construction stands constrained to the interior footprint;
  - corner-first wall dependency ordering;
  - functional door/light targets retain only the planned support-below hint;
  - an empty fresh crosshair starts physical repositioning instead of waiting;
  - confirmed structural blocks still use solid-occupancy contradiction, while
    partial/collisionless door and light blocks are revoked only by a
    first-person ray-hit of a different block at the exact target;
  - terminal failure stops nested movement/bridge control.
- `src/main/java/dev/mcai/companion/skills/building/PlacementSupportPreference.java`
  - lower and upper walls use support directly below;
  - roofs keep edge-placement fallbacks.
- `src/main/java/dev/mcai/companion/skills/bridging/BridgeToSkill.java`
  - crouched, vanilla-path roof-edge placement.
- `src/main/java/dev/mcai/companion/action/FairPlayerActuator.java`
  - causal vanilla interaction receipt used by placement verification.
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
  - terminal skill/rejection failures fail the live GameTest promptly.
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/PlacementSupportPreferenceTest.java`
- `src/test/java/dev/mcai/companion/skills/bridging/BridgeSkillsTest.java`

The targeted JVM tests and affected building/bridging regression passed. The
three live runs additionally validate this specific chat/model/world path.

## Latest completed gates

```text
GameTest:
mcai_companion:real_player_task_to_live_model_shelter_relocation

Result:
PASS x3 consecutive independent real-MiMo runs

Physical checkpoint:
57/57 confirmed shelter steps; independent physical oracle PASS

Affected JVM regression:
BuildShelterStepSkillTest
PlacementSupportPreferenceTest
BridgeSkillsTest
PASS
```

Authoritative runtime log:
`run/logs/latest.log`

Authoritative task checkpoint:
`run/gametestserver/gametestworld/data/mcai_companion/memory.db`

## Real-time throttle result

Long real-time runs previously emitted periodic `Can't keep up` warnings of
roughly 2000 ms / 40 ticks. Bytecode inspection of the mapped
`MinecraftServer.runServer()` and `GameTestServer.waitUntilNextTick()` proves
the test-only throttle slept 50 ms *after* each tick's work and anchored the
next sleep to the actual wake time. A few milliseconds of real work therefore
accumulated until vanilla correctly reported a false test-harness overload.

The fixed test-only wait now runs at
`waitUntilNextTick` RETURN and parks only until
`MinecraftServer.getNextTickTime()`, the absolute vanilla deadline. The
`realtime_clock_contract` gate includes 8 ms of deterministic work in 260
successive ticks plus an accumulated schedule-lag assertion.

```text
mcai_companion:realtime_clock_contract
PASS
261 ticks / 13.23 s with controlled tick load
maximum schedule lag <= 750 ms
no "Can't keep up" entry in the authoritative run log
```

## Zero-human dedicated-server result

The physical gate also passes with no human player ever logged in:

```text
mcai_companion:zero_human_dedicated_server_chunk_and_respawn
PASS / 11.82 s
```

It verified one headless `ServerPlayer` as the sole player-list entry, ordinary
player simulation tickets 640 blocks from the GameTest origin, ticking of both
an outlying non-player entity and scheduled block update, absence of forced
chunks, directional recent-damage perception, local emergency reaction, death,
fresh vanilla respawn, session-generation invalidation, and stable cleanup.

The disconnect cleanup emitted one vanilla
`PacketSendListener ... StacklessClosedChannelException` fallback warning. The
test and lifecycle state passed; this warning is seen on ordinary modern
Minecraft disconnect paths too, but it remains recorded rather than hidden.

## Latest genuine zero-human foundation failure

After correcting the cross-chunk assertion, the short physical zero-human gate
passed again. A second real-MiMo zero-human run then advanced through actual
wood gathering, basic crafting, stone, food, the iron toolkit, workstations,
and shelter-material preparation in 6.556 active minutes. It entered physical
shelter construction and failed at tick 7865:

```text
repeated_skill_failure_without_progress
build_shelter_step.place_unconfirmed
```

The persisted checkpoint is plan `57e711700fa03847`, origin
`[12679562,-44,14449672]`, step 2 (`LOWER_WALL`), with seven earlier placements
causally confirmed. The failing target is `(12679562,-44,14449674)`, supported
by dirt at `(12679562,-45,14449674)`. At each of three attempts the fair
crosshair initially resolved that exact face and the vanilla interaction was
dispatched, but the held oak-plank count stayed at 50 and the target remained
air. A read-only postmortem of the saved Anvil chunk independently confirmed
the dirt support, air target, and seven previously placed oak planks.

The fixture's surviving no-AI cow geometry overlaps that target voxel. This
matches vanilla collision rejection and the unchanged hand count. The
production shelter frame/planner currently contains fair visible block faces
but omits fair visible entities, so it can choose a structurally valid volume
that is visibly occupied by a living entity. Repeating the identical target
cannot make progress. This is the leading root-cause hypothesis; it must be
proved by a targeted collision scenario rather than hidden by removing the
cow, force-placing the block, or increasing retries.

The earlier first live run's “left asserted work chunk” failure was a faulty
test invariant because the 21x21 fixture crosses chunk boundaries. Its
replacement correctly keeps a 24-block bounded work area, verifies both the
initial anchor and current chunks are ordinarily block/entity ticking, and
rejects forced chunks. Both the short physical gate and the real run passed
that corrected contract.

## Visible-entity placement collision fix

The cow-collision hypothesis was proved. A production-dev GameTest now places a
cow across the exact target voxel and verifies that the ordinary player-use
interaction is dispatched but neither consumes the held plank nor changes the
air block. After removing the cow, the same vanilla interaction consumes and
places the plank. The same test verifies that only fairly observed entity
occupancy enters shelter memory, remains available briefly after the AI turns
away, and expires after 200 ticks.

The production shelter frame now carries revisioned, time-bounded
`RecentVisibleEntity` observations. `ServerShelterFrameSource` accumulates only
entities exposed by the first-person semantic scene; it does not query hidden
level entities. `DynamicShelterPlanner` rejects a candidate only when a recent
placement-blocking entity envelope intersects a generated shell block.

Changed files include:

- `src/main/java/dev/mcai/companion/skills/building/RecentVisibleEntity.java`
- `src/main/java/dev/mcai/companion/skills/building/ShelterFrame.java`
- `src/main/java/dev/mcai/companion/skills/building/ServerShelterFrameSource.java`
- `src/main/java/dev/mcai/companion/skills/building/DynamicShelterPlanner.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/building/DynamicShelterPlannerTest.java`
- the matching GameTest environment and instance JSON resources.

Completed focused gates:

```text
DynamicShelterPlannerTest + BuildShelterStepSkillTest: PASS
compileJava: PASS
mcai_companion:visible_entity_placement_occupancy: PASS / 1.594 s
```

## Latest genuine zero-human foundation failure

The next real-time, zero-human, configured-MiMo run did not reproduce the cow
placement failure because it exposed an earlier material-preparation defect.
It legally gathered and crafted through wood, stone, food, seven iron, the iron
toolkit, and workstations, then reached 63 oak planks. At tick 6948
(5.800 active minutes) it failed with:

```text
repeated_skill_rejection_without_world_change
```

The persisted event sequence shows
`prepare_foundation_shelter_materials` failing twice because its nested
`gather_visible_block_cluster` returned
`cluster_not_rediscovered` while looking for coal. The parent incorrectly
treats this fair-observation miss as terminal even though it is recoverable by
rescanning, exploring, selecting a different remembered face, or making
charcoal. Once the parent stopped, the model repeatedly selected
`build_shelter_step`; the local precondition correctly rejected every attempt
with `shelter.missing_door`. This is the immediate source of the apparent
“says yes but never acts” loop in this run.

Authoritative final state:

```text
planks=63
doors=0
lights=0
active checkpoint skill=prepare_foundation_shelter_materials
active phase=GATHER_COAL
scanTurns=3
elapsedTicks=193
```

The last failed gate is
`mcai_companion:real_zero_human_dedicated_server_foundation`. Its authoritative
runtime log and SQLite task history remain under `run/`.

## Subsequent real gate: iron-toolkit resource recovery

After the shelter-material repair compiled and the real charcoal furnace gate
passed, the next configured-MiMo zero-human run exposed the same class of bug
one stage earlier. The run legally gathered eight logs, completed basic and
stone crafting, secured food, and started `prepare_iron_toolkit`. A coal
cluster child ended while rescanning with zero blocks mined; the parent masked
the child reason as `prepare_iron_toolkit.coal_gather_failed` and returned
control to the model. MiMo produced nine consecutive `REPLAN` decisions, then
retried the identical skill twice. The third identical failure correctly
tripped `repeated_skill_failure_without_progress`.

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 6.219 active minutes / tick 7457
Terminal detail: repeated_skill_failure_without_progress
Final physical toolkit: iron pickaxe=false, bucket=false, shield=false,
                        furnace=false
Last child checkpoint: COAL / SCANNING / mined=0 / scanTurns=36
```

`PrepareIronToolkitSkill` now keeps fair resource-rediscovery failures inside
the local transaction. It waits for a newer semantic revision and rescans
instead of asking the model to restart. When no coal is owned but the existing
wood reserve can safely cover the shield, sticks, one fuel plank and one
preserved log, it now uses the ordinary crafting table and furnace menu to
make one charcoal before smelting iron. Safety, inventory-full and durability
failures remain terminal rather than being hidden as retries.

The focused JVM tests compile and pass. The new
`mcai_companion:real_charcoal_furnace_batch` physical GameTest also passes,
proving an oak log plus one oak plank becomes charcoal through a naturally
ticking vanilla furnace and the production menu skill.

The immediate rerun then completed `prepare_iron_toolkit` in one invocation:
seven iron ore were physically mined and collected, one charcoal and seven
iron ingots were produced by the furnace, and the iron pickaxe, bucket and
shield were recipe-crafted. The GameTest stopped at tick 4682 only because its
old independent oracle required the coal-ore route specifically. The saved
vanilla stats prove `minecraft:crafted/minecraft:charcoal = 1` and no coal was
mined or picked up. The oracle now accepts either the paired mined+picked-up
coal stats or a crafted-charcoal stat, while retaining all iron, furnace and
tool recipe assertions.

## Exact next action

1. Reproduce the latest no-effect placement with the smallest physical
   GameTest that keeps the same ordinary player interaction and fair semantic
   observation path.
2. Keep the failure inside `BuildShelterStepSkill`: require a newer fair
   observation, inspect only remembered visible occupancy, try a bounded
   alternate support/stance, and regenerate a local plan when the target is
   still unusable. Never force-place a block or ask the model to repeat the
   same action.
3. Reject model-provided scale changes while a persisted shelter plan is
   active by continuing the bound plan rather than returning
   `scale_mismatch`.
4. Run the focused building JVM tests and the new physical recovery gate; only
   then rerun the configured-MiMo zero-human foundation gate.

Do not claim M1 completion from these gates. They validate specific production
paths and expose the next real defect.

## Current bug after the route-neutral rerun

The route-neutral configured-MiMo, zero-human rerun proved the new production
paths in one ordinary survival sequence:

```text
seven iron ore mined and picked up
one charcoal made in the ordinary furnace
seven ingots smelted
iron pickaxe, bucket and shield recipe-crafted
workstations completed
door and torch material preparation completed
```

It then entered `build_shelter_step` with a standard generated plan and failed
after 7.245 active minutes. Two structural blocks were causally confirmed.
Step 14 (`LOWER_WALL`) dispatched an accepted vanilla use-on-block interaction
against dirt below the intended target, but after 60 ticks:

```text
target=(-6781897,-44,-4410306)
clicked=(-6781897,-45,-4410306), face=UP
target voxel=AIR
held oak_planks=53 -> 53
crosshair=null
failure=build_shelter_step.place_unconfirmed
```

This is a genuine no-effect placement, not a confirmation false negative:
neither the target nor inventory changed. The current executor turns that
recoverable local condition into a terminal model-visible failure. MiMo then
requested `scale=compact` three times while the persisted plan is
`scale=standard`; `prepare()` returned `build_shelter_step.scale_mismatch`
three times and the supervisor ended in
`repeated_skill_start_rejection_without_world_change`.

Relevant already-modified files for the successful resource route:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/FoundationCraftingSkills.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/skills/menu/MenuGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- matching focused tests and GameTest resources

Last focused gates:

```text
PrepareIronToolkitSkillTest: PASS
PrepareFoundationShelterMaterialsSkillTest: PASS
SmeltMenuBatchSkillTest: PASS
mcai_companion:real_charcoal_furnace_batch: PASS / 698.3 ms
```

Last failed formal gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / 7.245 active minutes
first local failure: build_shelter_step.place_unconfirmed
terminal supervisor detail:
repeated_skill_start_rejection_without_world_change
```

## Proven placement-obstruction root and local recovery

A read-only parse of the saved entity Anvil payload proved the latest
no-effect placement was another real entity collision, not an aiming or
confirmation bug. One surviving no-AI cow was persisted at:

```text
position=(-6781897.25,-44.0,-4410306.25)
intended block=(-6781897,-44,-4410306)
```

Its vanilla cow envelope intersects that exact block. The local Forge source
also confirms `BlockItem.canPlace` delegates to `Level.isUnobstructed`; this
matches the accepted packet, unchanged hand count and air target.

The production executor now:

- checks the current target with the same short-lived, first-person entity
  collision predicate used during initial site selection;
- records the obstructed target as forbidden for this goal;
- regenerates a terrain-aware plan locally rather than asking the model to
  repeat the same placement;
- treats causally confirmed structural blocks as reusable only when the new
  shell physically overlaps those exact cells;
- requires `owned inventory + exact overlap >= required blocks`, so a
  replacement plan cannot invent material credit;
- inherits matching confirmations and continues the same transaction;
- runs a local fair survey if the repaired support is not currently visible;
- keeps the first physical plan's scale authoritative even when a later model
  response sends a different scale;
- retains a bounded ordinary-movement fallback for transient non-hostile
  occupancy, but never force-places, teleports, deletes or attacks the actor.

The focused material-conservation test proves that a 55-block repair with only
53 blocks left must reuse both earlier placements. A new production-dev
physical gate fixes a plan first, inserts a no-AI cow into the selected wall
cell afterward, and then runs the actual survey, core actuator, building skill,
semantic frame sources and vanilla use-on-block path. It passed while the cow
remained alive in the original cell and the repaired plan consumed and placed
a real plank elsewhere:

```text
DynamicShelterPlannerTest + BuildShelterStepSkillTest: PASS
compileJava: PASS
mcai_companion:placement_obstruction_recovery: PASS / 4.615 s
```

Additional changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/building/DynamicShelterPlanner.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/DynamicShelterPlannerTest.java`
- matching `placement_obstruction_recovery` GameTest resources

The next gate is the same configured-MiMo, real-time, zero-human foundation
test. A pass will validate this recovery in the full resource-to-shelter chain;
a failure is the next state transition to fix, not grounds for an M1 claim.

## Latest formal gate after placement-obstruction repair

The configured-MiMo, real-time, zero-human foundation gate was rerun with
production startup auto-spawn enabled. The run legally completed the resource
chain through eight initial logs, basic crafting, stone tools, food, seven
iron ore, furnace smelting, iron pickaxe/bucket/shield, workstations, and
shelter-material preparation. No human player logged in.

The old no-effect placement is now recognized as a visible entity
obstruction, and the persisted shelter transaction retains three reusable
confirmed blocks plus the forbidden target. However, the local repair planner
returned `shelter.insufficient_observation`. `BuildShelterStepSkill` then
completed the invocation after starting a short survey instead of retaining
the repair transaction until a sufficiently new, broad semantic frame was
available. MiMo consequently restarted `build_shelter_step` three times and
the supervisor correctly ended the no-progress loop.

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 7.506 active minutes / tick 9006
Local recovery state:
  stepIndex=14
  reusable confirmed blocks=3
  avoided target=(-6545927,-44,12780559)
  repair result=shelter.insufficient_observation
Terminal detail=repeated_skill_failure_without_progress
```

The last failed gate is therefore no longer `scale_mismatch` or a blind
vanilla placement retry. The immediate bug is recovery-state ownership:
observation acquisition must remain inside the same `BuildShelterStepSkill`
transaction, wait for a newer fair scene, and retry local repair without
returning control to the model. After that fix, rerun only the focused
placement-recovery JVM/physical gates and then this same formal live gate.

## Observation-owned repair and next formal failure

`BuildShelterStepSkill` now has a dedicated
`SURVEYING_PLACEMENT_REPAIR` phase. When local obstruction repair cannot
generate a replacement because the fair navigation map is incomplete, the
same invocation performs up to two broad 16-heading, three-pitch first-person
surveys and calls the repair planner directly afterward. It does not restore
the blocked plan or return the observation miss to MiMo. Confirmed physical
blocks, the forbidden cell, the original scale, and inventory accounting stay
bound to that transaction.

```text
Focused building JVM tests: PASS
mcai_companion:placement_obstruction_recovery: PASS / 8.727 s
```

The next configured-MiMo rerun did not reach this branch. It exposed an
earlier route-dependent material failure:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 4.850 active minutes / tick 5809
Parent skill=prepare_foundation_shelter_materials
Parent checkpoint phase=GATHER_WOOD
Child failure=gather_visible_block_cluster.cluster_not_rediscovered
Physical state: planks=2, doors=0, lights=0
Terminal supervisor detail=repeated_skill_rejection_without_world_change
```

The parent previously handled fair coal rediscovery misses locally but sent
the equivalent wood miss back to the model. It now treats only bounded
rediscovery/binding/mining/pickup/stuck failures as recoverable, records and
excludes the failed wood seed, requires a newer semantic observation, and
rescans for another visible log inside the same transaction. Danger,
inventory-full, and other safety failures remain terminal. The focused
foundation-material and affected building JVM tests compile and pass.

The immediate next gate is the same configured-MiMo, real-time, zero-human
foundation scenario. The first new physical state transition remains the only
failure to fix; neither the material route nor obstruction repair constitutes
M1 completion.

## Wood-gather liveness audit

A subsequent formal run reached `GATHER_WOOD` but then remained inside the
child gatherer for 3,321 ticks with no increase in usable plank potential.
This run was deliberately interrupted rather than waiting for the parent's
9,000-tick global deadline. The child gatherer's displacement-based stuck
window could be reset by small oscillating movement, so it did not provide an
inventory-progress deadline.

The material parent now maintains a 300-tick progress lease based only on
actual owned wood/plank potential. Each real pickup renews the lease. Expiry
cancels the child through its normal actuator cleanup, records and excludes
the failed seed, requires a newer observation, and resumes local scanning.
It logs the rejected seed and reason for the next physical audit. The boundary
unit test and focused compile pass.

The last completed formal result remains the 4.850-minute material failure
above; the interrupted liveness audit is not recorded as a pass or ordinary
test failure. Next: rerun the exact formal gate and inspect the first new
transition.

## Latest formal transition: roof top-face geometry

The configured-MiMo, real-time, zero-human gate was rerun after adding the
wood inventory-progress lease. The Headless `ServerPlayer` completed the
resource chain through seven iron ore, iron tools/bucket/shield, workstations,
and shelter materials. It physically gathered fifteen reserve logs without
the former 3,321-tick false-busy state. The first material invocation ended
when one mined drop was no longer visible as a block; MiMo selected the
ordinary observed-item pickup, then the same persisted material skill
completed the door, torch, and structural inventory on its next invocation.

The builder then physically placed all thirty lower/upper wall blocks. The
next deterministic failure was every roof step:

```text
plan origin=(-13184160,-44,8614367)
confirmed wall steps=30
roof target y=-42, support y=-43, required face=UP
standing eye y=-42.379999995
actual fair crosshair face=NORTH/SOUTH/EAST
```

At that eye height the support top plane is 0.38 blocks above the eye.
Horizontal repositioning therefore cannot expose its `UP` face; the old
executor repeatedly deferred roof steps and restarted the skill. The run was
deliberately interrupted after the repeated physical geometry proved the
cause, rather than spending the remaining model budget on an impossible
route. This is not a pass.

`BuildShelterStepSkill` now queues a legal vanilla jump only after the head is
aligned when an `UP` interaction face is still above the body's observed eye.
It keeps ordinary airborne physics and waits for a later first-person sample
whose eye is above the plane before allowing placement. Side-face and
already-visible top-face placements do not jump. The focused JVM compile and
`BuildShelterStepSkillTest` pass.

Changed in this transition:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this recovery checkpoint

Next gate: a focused physical Headless-player roof jump-placement GameTest,
then the same configured-MiMo zero-human foundation gate. If crouch
reacquisition or airborne placement fails, fix that exact transition before
restarting the model gate.

## Roof jump-placement physical gate

The first focused physical run confirmed that the jump itself was legal and
raised the real `ServerPlayer` eye high enough to see the required `UP` face,
but it exposed three executor ownership bugs:

1. the pre-jump 40-tick aim deadline remained active during the jump;
2. protective sneak reacquisition lowered the eye below the roof plane again;
3. a valid airborne crosshair was saved for the next tick, where player
   motion made the actuator correctly reject the now-occluded target.

`BuildShelterStepSkill` now gives each bounded jump attempt its own aim and
observation deadline, never starts a ground relocation while the jump is in
flight, and skips protective sneak only for a roof block whose structural
support is already confirmed. Once the fair first-person ray sees the exact
support face, the executor dispatches the ordinary vanilla `useOnBlock`
interaction in that same tick. It still uses the production actuator's
reach, ray, collision, inventory, and world-effect checks; no direct block
mutation or relaxed occlusion rule was added.

The executor also performs one bounded fair first-person survey when the next
build step is not currently visible, instead of immediately returning
`no_visible_build_step`.

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 3.863 s
Physical evidence:
  production BuildShelterStepSkill
  survival Headless ServerPlayer
  30 wall blocks physically placed before the roof step
  body rise >= 0.5 blocks
  first roof block physically placed
  progress advanced beyond step 30
  structural inventory decreased
```

Files changed for this gate:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_roof_jump_placement.json`
- `src/main/resources/data/mcai_companion/test_instance/roof_jump_placement.json`
- this recovery checkpoint

The last failed formal gate remains the roof transition described above; the
focused physical roof gate now passes. Next: rerun the affected obstruction
recovery gate, then immediately rerun the exact configured-MiMo, real-time,
zero-human foundation gate and fix its first new physical transition.

## Formal rerun: falsely invisible stone

The affected obstruction regression remained green:

```text
mcai_companion:placement_obstruction_recovery: PASS / 1.598 s
```

The next exact configured-MiMo, real-time, zero-human foundation run failed
before reaching shelter construction. MiMo legally gathered four logs,
completed basic crafting, and selected `prepare_stone_tools`, but the local
skill failed three times after its 36-heading scan:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 1.233 active minutes / tick 1477
Body: survival Headless ServerPlayer; no human logged in
Checkpoint: phase=FIND_STONE, scanTurns=36
Failure: prepare_stone_tools.visible_stone_not_found
Supervisor: repeated_skill_failure_without_progress
```

The fixture's exposed stone was approximately 6–8 blocks from the body's
post-crafting position. Fair first-person block perception is bounded to 24
blocks and the production cluster gatherer already walks toward a retained
visible target, but `PrepareStoneToolsSkill` imposed an obsolete five-block
interaction-distance filter before delegating. It therefore discarded stone
the player could see and incorrectly reported that none was visible.

The selector now accepts only actual first-person ray-visible stone within
the existing 24-block perception budget. It does not inspect hidden blocks or
query the level. A focused boundary test proves a 7.25-block visible seed is
accepted and a seed beyond the fair ray budget is rejected.

Additional changed files:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareStoneToolsSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareStoneToolsSkillTest.java`
- this recovery checkpoint

```text
PrepareStoneToolsSkillTest: PASS
```

Next: rerun the exact live zero-human gate. The earlier four-log cluster
gatherer `stuck` result remains an observed weakness, but it made physical
progress and did not cause this terminal transition; address it if it becomes
the first blocking state after stone acquisition is restored.

## Formal rerun: unbounded resource aiming

The next live rerun validated the visible-stone correction with real model
control and physical survival actions:

```text
initial oak logs physically mined and picked up: 8
visible distant stone physically mined: >=5
vanilla advancements: Stone Age, Getting an Upgrade
stone pickaxe crafted through the production transaction
food reserve skill completed
```

The run then entered `prepare_iron_toolkit`. After collecting the additional
cobblestone required for a furnace, its child cluster gatherer remained on
one iron-ore target in `AIMING` for 2,238 ticks with zero iron mined. The run
was deliberately interrupted once that deterministic no-progress state was
confirmed instead of spending the full 12,000-tick parent deadline.

```text
Parent: prepare_iron_toolkit
Parent phase: GATHER_RESOURCE / IRON
Child: gather_visible_block_cluster
Child phase: AIMING
Child target: one fairly observed iron-ore block
Child progress: mined=0 after 2,238 ticks
```

The cluster gatherer previously aimed at a hit point from the lower-frequency
semantic ray fan and had no aiming lease. A refreshing ray point could keep
the angular gate from converging forever even when the tick-local centre
crosshair already selected the exact block.

It now:

- treats the current first-person crosshair hit as stronger, tick-local
  evidence and starts the ordinary mining action in that same tick;
- still delegates to the production interaction actuator, which enforces
  reach, face, occlusion, block identity, permissions, durability, and
  vanilla break progress;
- expires an unproductive aim after 80 ticks, marks only that observed target
  unavailable, and resumes fair scanning instead of remaining busy forever.

Changed files:

- `src/main/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkill.java`
- `src/test/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/gathering/GatheringSkillTestFixtures.java`
- this recovery checkpoint

```text
GatherVisibleBlockClusterSkillTest: PASS
```

Next: rerun the exact configured-MiMo zero-human foundation gate and verify
that the production iron gatherer leaves `AIMING` and records physical raw
iron pickup.

### Follow-up: semantic fan refresh loop

The next live rerun again completed eight logs, stone acquisition, stone
pickaxe crafting, food, and furnace cobblestone. The 80-tick lease prevented
the former permanent `AIMING` state, but the child then cycled between
`AIMING` and `SCANNING`: as the head turned, the selected ore briefly left
the lower-frequency semantic ray fan, so the executor discarded the target
before the tick-local crosshair could reacquire it. After 2,365 parent ticks,
zero iron had been mined; the run was intentionally interrupted.

The gatherer now retains only the exact hit point and block coordinate from
the recently fair, first-person visible face for the existing 80-tick aim
lease. It can continue turning through a short semantic refresh gap, but it
cannot mine from that memory: the current centre crosshair must re-identify
the same coordinate and block type before `beginMining` is dispatched. If
that never happens, the existing lease expires and the target is excluded.
This is bounded visual working memory, not hidden-world access.

Added regression coverage proves:

- the exact crosshair starts mining despite a stale semantic look vector;
- loss of the semantic fan does not reset a still-bounded aim;
- an aim that never gains a crosshair expires and releases the target.

```text
GatherVisibleBlockClusterSkillTest: PASS
```

Next remains the exact configured-MiMo zero-human gate, with physical raw
iron pickup as the first required new transition.

## Formal rerun: full-roof chain obstruction

The exact configured-MiMo, real-time, no-human dedicated-server gate was
rerun. It physically validated the two preceding fixes and advanced farther
than any earlier run:

```text
8 initial oak logs mined and picked up
stone acquired and stone pickaxe crafted
food reserve completed
furnace cobblestone acquired
7 raw iron physically mined and picked up
iron smelted; iron toolkit/workstation stages completed
15 reserve logs acquired for the shelter
shelter walls constructed and roof construction entered
```

The last gate failed after 9.981 active minutes / 11,975 ticks with:

```text
goal status: SAFE_IDLE
detail: repeated_skill_rejection_without_world_change
active skill: build_shelter_step
```

This is not a model-planning or resource-gathering failure. The production
builder can physically jump-place the first roof block, but its remaining
roof sequence sometimes keeps the body inside the now partially roofed
interior. A two-block-high roof blocks the jump, so the eye remains at
`y=-42.38` while the support top plane is `y=-42.0`. The executor repeatedly
reports `jump_height`, defers roof steps, occasionally side-places one block,
then enters the same deterministic loop. Model retries cannot change that
local geometry.

Relevant files already changed in this recovery:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareStoneToolsSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareStoneToolsSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkill.java`
- `src/test/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/gathering/GatheringSkillTestFixtures.java`

Last passing focused gate:

```text
mcai_companion:roof_jump_placement: PASS
```

That gate only proves the first physical roof placement and therefore missed
the newly exposed sequence failure.

Next:

1. detect when jump-placement is blocked by the partial roof using only the
   companion's fair navigation/visible observations;
2. relocate through ordinary movement to an exterior or otherwise open
   vantage, then retarget a visible side/top support and continue;
3. extend the physical GameTest to require several consecutive roof blocks
   after the first one;
4. rerun that focused gate, the building obstruction regression, and only
   then the exact live zero-human foundation gate.

### Full-roof correction and focused verification

The strengthened physical gate initially exposed that side-first placement
alone was insufficient. It progressed from 30 completed wall blocks to
42–43 total confirmations, then stranded the body outside the unfinished
roof and exhausted pending faces. One run also logged a stale-looking
step/click pairing, so the executor now both repairs such a binding from
fresh fair evidence and rejects it before any vanilla click if the clicked
face would not place into the executing plan cell.

The final correction derives a construction dependency from the generated
plan dimensions:

- roof supports prefer a visible horizontal edge after the first block;
- roof cells are completed outer ring → inner ring → centre;
- roof aiming relocations stay on observed interior floor cells and never
  stand in the target column;
- a jump vantage additionally requires an observed clear third cell;
- a confirmed roof block above the body is causal headroom-block evidence,
  so the executor relocates instead of repeatedly jumping into it.

The physical roof GameTest was strengthened from “one roof block exists” to
“all 25 roof blocks exist, all 55 structural steps are confirmed, the body
physically rose during the first jump, and 55 ordinary planks were consumed.”

Changed in this correction:

- `src/main/java/dev/mcai/companion/skills/building/PlacementSupportPreference.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`

Targeted results:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement
  complete 25-block roof / 55 structural placements: PASS (5.820 s)
mcai_companion:placement_obstruction_recovery: PASS (1.566 s)
```

These are focused inner-loop contracts, not formal M1 evidence. Next is the
exact configured-MiMo, real-time, no-human foundation gate. It must reproduce
physical resource collection and reach complete shelter verification before
this live bug is considered resolved.

### Live rerun: shelter prerequisite admission failure

The exact configured-MiMo, real-time, no-human gate was rerun after the
full-roof correction. It again physically completed the early route:

```text
6 initial logs mined and picked up
crafting table, wooden pickaxe, stone and stone pickaxe completed
food phase completed
8 furnace cobblestone and 7 raw iron physically collected
iron smelted; iron pickaxe/toolkit completed
foundation chest/workstation transaction completed
```

The first new blocking state occurred before construction began. During
`BUILD_DYNAMIC_SHELTER`, both `prepare_foundation_shelter_materials` and
`build_shelter_step` remained exposed in the model function schema even
though the required door, light, and 55 matching structural blocks were not
yet owned. MiMo repeatedly selected `build_shelter_step`; its high-level
dimension/sample/scale arguments were valid, but the local planner correctly
rejected startup with:

```text
lastStartRejection: shelter.missing_door
physical shelter construction: not started
```

The run was intentionally interrupted instead of consuming the full timeout.
This is an admission-contract bug: the deterministic route tracker already
publishes `criticalOwnedCounts` and `currentMinimumTargets`, so an impossible
construction skill must not be advertised until those server-verified
prerequisites are met.

Last failed formal gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
interrupted at BUILD_DYNAMIC_SHELTER after repeated shelter.missing_door
```

Next:

1. make the live function allow-list readiness-sensitive;
2. expose only `prepare_foundation_shelter_materials` while any shelter input
   is below its trusted minimum, then expose `build_shelter_step` once all
   three are met;
3. add focused schema-admission tests for both sides of that transition;
4. rerun the targeted planner test and the exact real-model gate.

Implemented:

- `MinecraftPlannerInputFactory` now treats `build_shelter_step` as a
  foundation phase-controlled skill instead of a generally visible skill;
- trusted `criticalOwnedCounts` are compared against
  `currentMinimumTargets` for structural material, safe doors, and lights;
- missing/malformed readiness evidence fails closed;
- before readiness, only material preparation is admitted; after readiness,
  only construction is admitted.

Changed:

- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- this checkpoint

Targeted result:

```text
MinecraftPlannerInputFactoryTest: PASS (10 tests)
```

Next gate: rerun the exact configured-MiMo, real-time, zero-human foundation
test and require a physical transition through material preparation into
construction.

### Live rerun: unreachable crafting-table placement

The next exact live rerun failed earlier, in `prepare_basic_crafting`.
MiMo recovered from one extraneous `scale` argument and started the correct
compound. The skill physically crafted and placed a table, but did not craft
the wooden pickaxe before its 600-tick bound; its first attempt ended with
`prepare_basic_crafting.timed_out`, and the formal stage deadline expired
during the retry.

The root cause was the support selector accepting the nearest arbitrary
sturdy top face. After gathering in a mixed resource fixture this can be a
tree/ore/high-column top. The table is physically placed, but is outside a
comfortable vanilla interaction range; the state machine then loops through
visible-table aiming until its total timeout.

Correction:

- table supports must place the block within one cell of the body's standing
  plane;
- the resulting table centre must be within a conservative 3.75-block
  first-person reach;
- candidates prefer the body's own floor plane before distance;
- a new physical GameTest presents a closer high-column trap and requires
  ordinary table and pickaxe recipe statistics, a real reachable table, and
  an owned wooden pickaxe.

Changed:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareBasicCraftingSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/FoundationGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_reachable_basic_crafting.json`
- `src/main/resources/data/mcai_companion/test_instance/reachable_basic_crafting.json`

Targeted result:

```text
mcai_companion:reachable_basic_crafting: PASS
physical duration: 708.1 ms (accelerated GameTest clock)
```

This remains an inner-loop physical contract, not formal M1 evidence. Next is
the exact real-time MiMo zero-human foundation gate.

### Live rerun: construction admission revoked by its own placements

The third exact real-time MiMo zero-human run validated both preceding
corrections:

```text
basic crafting completed in one 444-tick invocation
stone, food, seven iron, smelting, iron toolkit and workstations completed
prepare_foundation_shelter_materials was the only admitted compound while
  materials were deficient
16 reserve logs and one coal were physically mined
door, light and 55 matching structural blocks were prepared
the schema switched to build_shelter_step only after readiness
```

Construction then confirmed two physical blocks. A no-AI fixture cow occupied
the next wall cell. The local push attempt moved the body beyond the target
but its fair remembered entity position remained stale, so recovery ended
with `build_shelter_step.placement_obstruction_persisted`.

That exposed a second admission bug: the 55-block inventory target fell below
55 as soon as construction consumed material. The schema therefore hid
`build_shelter_step` again even though a server-owned plan and two confirmed
placements existed. MiMo repeated the now-unknown build skill until the run
was intentionally interrupted.

Last failed formal gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
interrupted after repeated unknown_skill following two confirmed shelter blocks
```

Next:

1. persist a server-verified `SHELTER_MATERIALS_PREPARED` route milestone;
2. admit both continuation and replenishment after construction commits and
   inventory is consumed, while an explicit missing-material rejection
   temporarily returns to preparation-only;
3. after an ordinary push, retry vanilla placement instead of treating a
   stale remembered entity pose as proof the target remains blocked;
4. extend targeted route-admission and physical obstruction coverage, then
   rerun the exact live gate.

Implemented:

- `SHELTER_MATERIALS_PREPARED` is sticky, goal-scoped `SavedData` evidence
  emitted only after the server observes 55 matching structural blocks, a
  safe door, and a light in owned inventory;
- while counts remain full, only construction is admitted;
- after construction consumes material, both construction continuation and
  replenishment are admitted;
- an explicit local missing-material start rejection temporarily returns the
  schema to preparation-only;
- after a physical push, old remembered entity coordinates no longer prove
  continued collision. If the body is clear and the entity was not freshly
  reobserved, the executor retries the ordinary vanilla placement path.

Changed:

- `src/main/java/dev/mcai/companion/progression/SurvivalMilestone.java`
- `src/main/java/dev/mcai/companion/progression/SurvivalRouteTracker.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`

Targeted results:

```text
MinecraftPlannerInputFactoryTest: PASS
SurvivalRouteReadinessTest: PASS
mcai_companion:placement_obstruction_recovery: PASS (1.881 s)
```

Next is the exact configured-MiMo real-time zero-human foundation gate.

### Live rerun: final roof steps need an exterior aiming stance

The fourth exact configured-MiMo, real-time, zero-human foundation run
validated the preceding admission and stale-entity corrections through real
world actions:

```text
basic crafting completed in one invocation
stone, food, seven raw iron, iron toolkit and workstations completed
foundation shelter materials completed in 1,848 ticks
construction remained admitted after inventory consumption
47 shelter placements were physically confirmed
```

The run then failed at tick 11,103 after 9.254 active minutes:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL
Terminal detail: repeated_skill_failure_without_progress
Last skill failure: build_shelter_step.aim_timeout_crosshair_wrong_block
Checkpoint: plan=8b30653ac5eb9d01, origin=[7187434,-44,1297742],
            confirmed=47, stepIndex=45
```

The remaining roof targets were behind the already built upper walls from the
body's interior-floor position. The centre ray repeatedly hit the nearer roof
or wall block. `startAimReposition(...)` could not select the geometrically
correct outside stance because `isPermittedConstructionStand(...)`
categorically restricted every roof aiming stance to the interior floor.
Deferring selected other visible roof cells and made substantial progress,
but when only similarly occluded cells remained, three bounded invocations
ended on the same aim failure and the orchestrator correctly entered
`SAFE_IDLE`.

Immediate next work:

1. permit only a tightly bounded, fairly observed exterior floor apron for
   roof aiming while retaining interior-only wall construction;
2. make the full physical roof gate force the body back into the finished
   walls before the final roof cells, so this exact recovery path is exercised;
3. run the focused JVM/building gate, then rerun the exact real-MiMo,
   real-time, zero-human foundation gate.

Implemented and focused evidence:

- roof aiming may use only an observed one-block exterior apron; wall aiming
  remains interior-only;
- when walls hide the last roof faces, the body walks to the generated open
  doorway, performs a fair survey, takes one observed safe step outside, then
  surveys again and selects an exterior aiming stance through normal
  `MoveToSkill`;
- the door/exterior recovery state survives a change of roof step within the
  same immutable plan and resets when the plan or body session changes;
- the complete physical roof gate now forces the body back to the interior
  after 36 confirmed placements and requires observing an actual exterior
  stance before accepting success.

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 12.02 s
physical evidence: 25/25 roof blocks, 55/55 structural confirmations,
                   ordinary inventory consumption, real body elevation,
                   real doorway exit and exterior stance
```

Changed for this correction:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

The last formal gate remains the failed exact MiMo run above. The immediate
next action is its real-time, zero-human rerun; do not infer M1 completion
from the focused physical pass.

### Live rerun: workstation phase has no admitted wood-recovery action

The next exact configured-MiMo, real-time, zero-human foundation rerun
validated the roof correction indirectly by reaching the deterministic
foundation route again, but exposed an earlier resource-boundary defect:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL
Active duration: 4.302 minutes
Tick: 5,154
Terminal detail: repeated_skill_rejection_without_world_change
Last start rejection:
  establish_foundation_workstations.chest_wood_required
```

Authoritative physical progress before the failure:

```text
no human player logged in
saved API profile restored
6 oak logs physically gathered
basic crafting, stone-tool and food compounds completed once each
8 cobblestone and 7 raw iron physically gathered
vanilla Acquire Hardware and Isn't It Iron Pick advancements earned
prepare_iron_toolkit completed after 2,950 ticks
```

MiMo then selected `establish_foundation_workstations` three times. The local
precondition correctly rejected each start because fewer than eight planks
could be produced for the chest. The SQLite audit records those rejections as
events 26, 29, and 32, followed by terminal event 33. The persisted
`prepare_iron_toolkit` checkpoint is `COMPLETED`; no workstation transaction
or chest fabrication occurred.

Root cause: this is not model indecision and not a flaky chest transaction.
At route objective `ESTABLISH_FOUNDATION_WORKSTATIONS`, the live function
allow-list exposes only `establish_foundation_workstations`. That compound
requires `potentialPlanks >= 8` and cannot gather wood. Consequently every
schema-valid action available to the model is locally impossible, so retrying
the model cannot create world progress.

Relevant already-modified shelter files remain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`

Last focused passing gates:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 12.02 s
```

Immediate next work:

1. make workstation admission/resource recovery closed-loop, using trusted
   owned counts and an executable fair gathering compound rather than asking
   the model to repeat an impossible workstation start;
2. add focused admission tests for deficient and sufficient chest wood;
3. add or extend a physical gate proving insufficient post-tool wood is
   gathered through ordinary world actions before the chest transaction;
4. rerun only those focused gates, then the exact real-MiMo zero-human
   foundation gate.

Implemented:

- `SurvivalRouteTracker` now publishes
  `criticalOwnedCounts.chest_plank_potential`, derived from the same
  wood-to-plank catalog used by the legal crafting executor, and requests a
  minimum of eight until workstation evidence is independently verified;
- the projected count is bounded to the route JSON inventory limit;
- workstation admission now fails closed: while chest wood is deficient it
  exposes only `prepare_foundation_shelter_materials`, whose bounded local
  state machine can fairly gather visible wood; once sufficient, it exposes
  only `establish_foundation_workstations`;
- the already verified storage phase continues to expose the workstation
  skill without demanding a second chest;
- the trusted phase playbook describes the same deterministic transition.

Changed:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareBasicCraftingSkill.java`
- `src/main/java/dev/mcai/companion/progression/SurvivalRouteTracker.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/test/java/dev/mcai/companion/progression/SurvivalRouteReadinessTest.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- this checkpoint

Targeted result:

```text
MinecraftPlannerInputFactoryTest: PASS
SurvivalRouteReadinessTest: PASS
PrepareFoundationShelterMaterialsSkillTest: PASS
compileJava + compileTestJava: PASS
```

The next gate is the exact configured-MiMo, real-time, zero-human foundation
run. This targeted result proves schema closure and readiness calculation, not
physical M1 completion.

### Live rerun: iron toolkit cannot reacquire an in-reach occluded table

The exact configured-MiMo rerun started with no human, restored the saved
model profile, and physically completed wood, basic crafting, stone, food,
eight cobblestone, seven raw iron, furnace placement, and ordinary smelting.
It then failed before reaching the newly corrected workstation admission:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL
Active duration: 4.689 minutes
Tick: 5,618
Terminal detail: repeated_skill_failure_without_progress
Repeated failure: prepare_iron_toolkit.table_not_visible
```

The SQLite audit is authoritative:

```text
events 28, 34, 40:
  prepare_iron_toolkit.table_not_visible
event 41:
  repeated_identical_skill_failure
checkpoint:
  phase=FIND_TABLE
  scanTurns=32
  elapsedTicks=276
  iron_ingots=7 in the observed run state
```

Root cause: after smelting, the remembered crafting table can be within
nominal interaction reach but occluded by the furnace the compound just
placed. `findFixture(...)` aimed at the remembered center, then performed a
full rotation scan from the same feet position. Because the table was behind
another block, rotation could never reveal it. The existing movement branch
was used only when raw distance exceeded reach, so all three invocations
repeated the same geometrically impossible stance.

Implemented:

- after a bounded in-place aim, `PrepareIronToolkitSkill` now enumerates only
  fairly observed safe standing cells around the remembered fixture;
- candidate feet/head clearance, sturdy support, danger bound, ordinary reach,
  and the observed aim corridor are checked without querying hidden world
  state;
- the normal `MoveToSkill` walks to the selected vantage; attempted vantages
  are bounded and not repeated within the invocation;
- the fixture still must become visible/current-crosshair verified before it
  can be opened;
- checkpoints now record the number of attempted fixture vantages.

Changed:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkillTest.java`
- this checkpoint

Targeted result:

```text
PrepareIronToolkitSkillTest: PASS
MinecraftPlannerInputFactoryTest: PASS
SurvivalRouteReadinessTest: PASS
compileJava + compileTestJava: PASS
```

Next: exercise the occluded-table recovery through a focused physical
GameTest, then rerun the exact real-MiMo zero-human foundation gate.

Focused physical evidence:

```text
mcai_companion:occluded_iron_toolkit_table: PASS / 2.383 s
```

The fixture placed a verified crafting table behind a two-block-high,
three-block-wide occluder, with an ordinary side route left open. The initial
fair semantic observation was required not to contain the table. The
production compound took one observed lateral survey step followed by
observed direct-aim stands, physically moved around the obstruction, freshly
ray-verified and opened the table, and crafted exactly one iron pickaxe,
bucket, and shield through vanilla recipes. The original table and furnace
remained unchanged, the body displaced from the impossible stance, and the
vanilla `Isn't It Iron Pick` advancement fired.

Additional changed files:

- `src/main/java/dev/mcai/companion/skills/foundation/FoundationGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_occluded_iron_toolkit_table.json`
- `src/main/resources/data/mcai_companion/test_instance/occluded_iron_toolkit_table.json`

The first fixture attempt correctly failed because a one-block wall still
left the table top semantically visible. The second correction intentionally
failed with zero available far-side vantages, proving that an occluded region
cannot be treated as observed. The passing implementation therefore uses a
two-stage fair survey/reposition path rather than weakening visibility.

Next gate: exact configured-MiMo, real-time, zero-human foundation rerun.

### Live rerun: occlusion recovery selected a stand but local movement did not converge

The exact configured-MiMo, real-time, zero-human rerun again started with no
human and used the production MCP goal path. It physically completed wood,
basic crafting, stone tools, food, seven iron ore pickups, furnace placement,
and ordinary smelting. The new occlusion branch then triggered in the natural
compound:

```text
Repositioning to fairly observed table occlusion recovery stand
GridPos[x=-804137, y=-44, z=6125004]; directAim=true, attempted=1
```

The stage nevertheless remained in `prepare_iron_toolkit /
MOVE_TO_FIXTURE` until the six-minute physical stage deadline:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL
Active duration: 7.456 minutes
Tick: 8,944
Inventory outcome: iron pickaxe=false, bucket=false, shield=false
Verified outcome: furnace=true, toolkit milestone=false
Checkpoint: phase=MOVE_TO_FIXTURE, elapsedTicks=6,974,
            fixtureVantages=1
```

The saved vanilla player position was
`(-804137.0450427404, -44, 6125005.480653009)`, while the selected stand
centre was `(-804136.5, -44, 6125004.5)`: the body stopped about 1.12 blocks
from the selected centre, outside the 0.5 arrival radius. The model was not
waiting and no resource was missing; a single local fixture reposition owned
the body for the remainder of the gate.

Current root-cause boundary:

- the remembered-fixture occlusion branch is now reached correctly;
- the selected natural-terrain move can make partial physical progress yet
  fail to enter its strict arrival circle;
- `MoveToSkill` has no total duration bound, and its small-motion recovery can
  be reset by partial collision motion, so the parent compound can be held
  until the outer six-minute deadline;
- the existing checkpoint omits the fixture movement child's phase/route,
  which must be added so the next failure is directly attributable.

Changed files remain the workstation routing, iron-toolkit recovery, focused
tests, GameTest registration/resources, shelter work listed above, plus this
checkpoint. No unrelated full regression was run.

Immediate next:

1. add a bounded fixture-reposition deadline and serialize the child
   movement checkpoint;
2. on a failed/stalled recovery stand, keep it excluded and fairly resample
   another local stand instead of failing or retrying the same geometry;
3. add a focused physical gate where the first recovery stand cannot reach
   its strict centre but another observed stand can;
4. run only the iron-toolkit unit/physical gates, then repeat the exact live
   zero-human foundation gate.

Implemented:

- every fixture reposition now has a 240-tick total deadline and a 100-tick
  no-target-progress deadline;
- recoverable local geometry failures (`route_unknown`, `stuck`,
  `unsupported_micro_vertical`, planning budget) stop movement, preserve the
  failed stand in the exclusion set, and resample from the body's new fair
  view;
- danger and actuator failures remain terminal rather than being hidden by
  retries;
- the parent checkpoint now embeds the actual fixture `MoveToSkill`
  checkpoint and best observed target distance;
- timeout logging records position, target, best distance, elapsed ticks and
  child phase/route without reading hidden world state.

Targeted result:

```text
PrepareIronToolkitSkillTest: PASS
mcai_companion:occluded_iron_toolkit_table: PASS / 2.139 s
```

The physical test again performed ordinary movement, fresh first-person
verification, menu opening and vanilla iron pickaxe/bucket/shield recipe
transactions. Next gate is the exact configured-MiMo, real-time, zero-human
foundation rerun.

### Live rerun: iron/workstations passed; shelter material search lacked exploration

The next exact live run restored the saved model profile with zero humans and
physically passed the former blocker:

```text
skill_completed.prepare_iron_toolkit
vanilla advancement: Isn't It Iron Pick
skill_completed.establish_foundation_workstations
```

It then entered `prepare_foundation_shelter_materials`, physically grew the
owned oak-log count to 15, and failed with:

```text
prepare_foundation_shelter_materials.wood_not_visible
checkpoint:
  phase=FIND_WOOD
  scanTurns=64
  elapsedTicks=1,936
  charcoalFallback=true
```

Fifteen logs expose exactly sixty oak-plank potential, while this route needs
sixty-one before crafting the door. Additional prepared reserve logs still
existed outside the current semantic view. The skill had bounded coal
exploration but no analogous wood exploration. Planner admission correctly
kept shelter construction closed while material readiness was false, leaving
the model to issue repeated `REPLAN`; one attempted `build_shelter_step` was
correctly rejected as `unknown_skill`. The doomed run was stopped after this
evidence rather than waiting for an unrelated outer timeout.

Implemented:

- shelter material preparation now starts bounded fair first-person wood
  exploration after its in-place scan is exhausted;
- exploration uses the existing ordinary travel/scan compound with a 32-block
  radius, eight-block steps, hardcore danger checks and a 6,000-tick bound;
- target detection accepts only freshly visible convertible wood of the
  already selected plank family and excludes rejected seeds;
- oak/mangrove-style families map to logs, crimson/warped to stems, and bamboo
  to bamboo blocks for auditable checkpoints;
- wood exploration is checkpointed and cancelled through the normal compound
  lifecycle.

New focused physical gate:

```text
mcai_companion:shelter_material_wood_exploration: PASS / 19.28 s
```

The body began with 15 logs (60 plank potential), one stick and one coal. The
only additional log was beyond the default 24-block semantic ray range. The
test required ordinary travel, fresh first-person rediscovery, physical
mining/pickup stats, return to a verified table, vanilla door/torch crafting,
and at least 55 retained structural planks.

Additional changed files:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/foundation/ShelterMaterialExplorationGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_shelter_material_wood_exploration.json`
- `src/main/resources/data/mcai_companion/test_instance/shelter_material_wood_exploration.json`
- this checkpoint

Next gate: repeat the exact configured-MiMo, real-time, zero-human foundation
run and continue from the first new physical failure.

### Latest live failure: model-dependent workstation prerequisite selection

The third exact configured-MiMo, real-time, zero-human foundation run again
physically completed visible wood gathering, basic crafting, stone, food and
the complete iron toolkit. The vanilla `Acquire Hardware` and
`Isn't It Iron Pick` advancements fired. At the next verified route objective,
the inventory held only seven original logs, so
`chest_plank_potential < 8`.

The planner admission boundary exposed only
`prepare_foundation_shelter_materials`, and the trusted phase prompt explicitly
said to choose that prerequisite. MiMo nevertheless repeatedly returned
`START_SKILL establish_foundation_workstations`. Local validation correctly
rejected every response as `unknown_skill`; after the bounded model-failure
budget the goal entered `SAFE_IDLE`:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL
Active duration: 5.055 minutes
Tick: 6,065
Goal detail: model_failures_exhausted
Verified iron toolkit: complete
Verified workstations/storage: false
```

This is an orchestration defect, not a missing-world-resource or movement
failure. A human request such as “establish the foundation” must not depend on
one model/provider reproducing an internal prerequisite skill identifier.
The local route already knows the verified unmet invariant and must compose
its legal recovery deterministically.

Current changed files relevant to the immediately preceding fixes:

- `src/main/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/foundation/ShelterMaterialExplorationGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_shelter_material_wood_exploration.json`
- `src/main/resources/data/mcai_companion/test_instance/shelter_material_wood_exploration.json`
- this checkpoint

Last passing targeted gates:

```text
PrepareIronToolkitSkillTest: PASS
mcai_companion:occluded_iron_toolkit_table: PASS / 2.139 s
PrepareFoundationShelterMaterialsSkillTest: PASS
mcai_companion:shelter_material_wood_exploration: PASS / 19.28 s
```

Last failing gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / 5.055 min / model_failures_exhausted
```

Immediate next:

1. remove the model/provider-dependent prerequisite-name handoff at the
   workstation boundary;
2. compose the verified chest-wood recovery through ordinary gathering,
   movement and recipe actions before the workstation transaction;
3. add a focused regression where the high-level intent is workstations but
   chest wood is initially deficient;
4. run only the affected JVM/physical gates, then repeat the exact live
   zero-human foundation gate.

Implemented:

- `establish_foundation_workstations` is now the stable public compound for
  the complete workstation objective. When the verified inventory has fewer
  than eight potential chest planks, it deterministically runs the existing
  fair shelter-material preparation state machine as a child, then continues
  the ordinary chest recipe, placement, opening and deposit transaction;
- the nested material transaction reserves eight additional planks, so the
  chest recipe cannot consume the later 55-block shelter structure budget;
- parent cancellation, timeout, hardcore safety and checkpoints now include
  the nested material phase;
- the model function schema always admits the public workstation compound for
  this objective. It no longer asks a provider to infer or reproduce an
  internal prerequisite skill name;
- a new physical GameTest starts with exactly one log (four potential planks)
  and a connected observed wood cluster, then requires actual mining/pickup
  stats, vanilla door/torch/chest crafting, a placed chest, a vanilla
  chest-menu deposit, and at least 55 retained planks after chest consumption.

Changed files for this fix:

- `src/main/java/dev/mcai/companion/skills/foundation/EstablishFoundationWorkstationsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/FoundationCraftingSkills.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- `src/main/java/dev/mcai/companion/skills/foundation/WorkstationPrerequisiteGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_workstation_wood_prerequisite_composition.json`
- `src/main/resources/data/mcai_companion/test_instance/workstation_wood_prerequisite_composition.json`
- this checkpoint

Targeted evidence:

```text
MinecraftPlannerInputFactoryTest: PASS
PrepareFoundationShelterMaterialsSkillTest: PASS
EstablishFoundationWorkstationsSkillTest: PASS
mcai_companion:workstation_wood_prerequisite_composition:
PASS / 6.483 s
```

The expected vanilla embedded-player disconnect fallback warning appeared
during cleanup; the physical result and bounded lifecycle cleanup passed.

Next:

1. rerun the pre-existing distant wood-exploration physical regression;
2. repeat the exact configured-MiMo, real-time, zero-human foundation gate;
3. continue from the first new physical failure rather than broad regression.

### Live proof of workstation fix and next shelter failure

The exact configured-MiMo, real-time, zero-human foundation gate was rerun.
The saved model profile restored without exposing its credential, no human
logged in, and the production body physically completed:

```text
7 initial oak logs mined and picked up
basic crafting: complete
stone tools: complete
food reserve: complete
8 furnace cobblestone: physically gathered
7 raw iron: physically mined and picked up
iron smelting and full toolkit: complete
establish_foundation_workstations: accepted and completed
shelter material preparation: complete after 16 reserve logs
```

Most importantly, after `Isn't It Iron Pick`, MiMo selected the public
`establish_foundation_workstations` skill. It was accepted immediately and
the route advanced about five seconds later. There was no hidden-skill
`unknown_skill` loop. This is real-model confirmation of the deterministic
prerequisite repair.

The first new failure occurred during dynamic shelter construction:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 6.421 active minutes / tick 7,702
Goal detail: repeated_skill_failure_without_progress
Plan origin: [-5221282,-44,-14000408]
Deferred step: 10
Blocked target: [-5221278,-44,-14000408]
Reusable confirmed blocks: 3
Avoided targets: [[-5221278,-44,-14000408]]
Local reason: shelter.no_safe_footprint
```

The skill fairly detected the obstructed target, deferred it, and attempted
local replanning. Three successive invocations nevertheless selected no safe
replacement footprint while retaining the same three-block partial shelter.
The current root-cause boundary is therefore the local shelter replan/site
scoring contract: it must distinguish the companion's own causally confirmed
partial structure from genuine external obstructions and either preserve a
compatible shifted plan or perform a bounded legal relocation/compensation.
It must not force-place through the obstruction or erase the partial build.

Latest passing affected gates:

```text
mcai_companion:workstation_wood_prerequisite_composition:
PASS / 6.483 s
mcai_companion:shelter_material_wood_exploration:
PASS / 20.30 s
```

Immediate next:

1. inspect the persisted shelter plan/checkpoint and site-selection code;
2. prove whether own confirmed blocks are poisoning replacement candidates;
3. add a focused partial-build-plus-external-obstruction physical scenario;
4. fix and rerun only the shelter regression before repeating the live gate.

### Active recovery correction: repair survey predicate mismatch

Focused inspection disproved the earlier assumption that the production
builder lacked its nested survey dependency. `CompanionRuntime` uses the full
`DynamicShelterSkills.registerAll(...)` overload and supplies the core
actuator, core frames and survey-result buffer.

The immediate cause of the persisted `placementRepairSurveyAttempts=0` is a
predicate mismatch in `BuildShelterStepSkill`:

- initial shelter planning treats both
  `shelter.insufficient_observation` and `shelter.no_safe_footprint` as reasons
  to run a fair first-person survey;
- obstruction-triggered local repair calls
  `recoverableRepairObservationFailure(...)`, which currently accepts only
  `shelter.insufficient_observation`;
- the live repair returned `shelter.no_safe_footprint`, so the survey branch
  was skipped before its attempt counter could increment, and the fallback
  later terminated as `build_shelter_step.obstruction_route_unknown`.

This explains the checkpoint and log exactly. It does not yet prove whether a
16-view survey alone supplies a viable repaired footprint, so the correction
must be protected by a partial-build-plus-entity physical regression rather
than declared fixed from a one-line predicate change.

Files already changed by the preceding workstation fix remain:

- `src/main/java/dev/mcai/companion/skills/foundation/EstablishFoundationWorkstationsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/FoundationCraftingSkills.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- `src/main/java/dev/mcai/companion/skills/foundation/WorkstationPrerequisiteGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- the two workstation-prerequisite GameTest JSON resources.

Last failing gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 7,702
shelter.no_safe_footprint -> survey skipped -> obstruction_route_unknown
```

Immediate targeted work:

1. add a JVM assertion that both repair observation outcomes are recoverable;
2. reproduce a partially constructed shelter with a visible no-AI cow on the
   next wall target and deliberately incomplete surrounding observation;
3. align the repair predicate, then require ordinary survey/replanning or
   harmless movement to make physical progress while preserving prior blocks
   and leaving the cow alive;
4. rerun only the affected JVM and physical shelter gates, then repeat the
   exact live MiMo zero-human foundation gate.

Implemented and targeted evidence:

- `recoverableRepairObservationFailure(...)` now accepts both
  `shelter.insufficient_observation` and `shelter.no_safe_footprint`;
- a focused JVM test was first observed failing on the missing
  `no_safe_footprint` branch, then passed after the correction;
- a new physical
  `mcai_companion:partial_shelter_obstruction_recovery` gate waits for at
  least three causally confirmed wall placements, inserts a no-AI cow into
  the next wall voxel, and requires a legal replacement plan, retained useful
  structure, a newly consumed/placed plank, an untouched occupied voxel and a
  live unharmed cow;
- the new gate passed in 2.112 seconds. On its fully observed flat fixture,
  local repair correctly avoided an unnecessary survey, shifted the plan one
  block and reused two placed blocks;
- the pre-existing zero-partial-build
  `mcai_companion:placement_obstruction_recovery` regression also passed in
  1.776 seconds.

New or additionally changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_partial_shelter_obstruction_recovery.json`
- `src/main/resources/data/mcai_companion/test_instance/partial_shelter_obstruction_recovery.json`

Current next gate is the exact configured-MiMo, real-time, zero-human
foundation run. Unlike the flat physical fixture, the saved live terrain
previously returned `no_safe_footprint`; this rerun must show that the repair
counter advances into the fair survey path and then either legally replans or
clears the visible non-hostile obstruction before the foundation oracle can
pass.

### Latest live gate: old obstruction failure cleared, roof-vantage loop

The next exact configured-MiMo, real-time, zero-human run reached substantially
farther. It physically completed wood, basic crafting, stone, food, eight
furnace cobblestone, seven raw iron, smelting, the iron toolkit, the composed
workstation/material prerequisite, and 47 of 55 structural shelter steps. It
did not reproduce the prior step-10 entity obstruction failure.

The authoritative new terminal state is:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / 10.78 active minutes / tick 12,934
Goal detail: repeated_skill_failure_without_progress
Skill failure: build_shelter_step.aim_timeout_crosshair_wrong_block
Plan: 47c832e02a2a1bcc
Origin: [-12104565,-44,-5667828]
Confirmed: 47
Remaining repeating roof steps: 41 and 43
Held structural planks: 9
```

Both remaining targets are far roof-edge cells. Across three model-driven
builder invocations, the local executor repeated:

```text
move to open doorway
step one block outside
aim at roof support from the same exterior lane
first-person ray is intercepted by a nearer already-placed plank
defer step 41, then step 43
return aim_timeout_crosshair_wrong_block
```

The crosshair evidence is concrete. For step 41 the intended support was
`[-12104562,-42,-5667827] EAST`, but the fair ray hit the underside of the
nearer roof plank at `[-12104562,-42,-5667825]`. For step 43 the intended
support was `[-12104561,-42,-5667825] NORTH`, but the ray hit a lower/nearer
wall plank. The skill persisted two tried vantages only within one invocation;
fresh invocations cleared that local exclusion and selected the same doorway
lane again. This is a cross-batch roof-vantage recovery defect, independent
of the now-cleared entity-obstruction defect.

Immediate work:

1. inspect exterior roof stand scoring and the reset boundary for attempted
   vantages;
2. add a physical regression with a nearly complete roof and exactly the
   occlusion geometry above;
3. choose a target-relative side/apron vantage whose actual first-person ray
   reaches the intended support, and persist bounded failed-vantage evidence
   across local batches;
4. rerun only roof/building gates, then repeat the exact live foundation gate.

### 2026-08-01 active roof-apron recovery implementation

The roof failure is now represented by a bounded fair-navigation correction:

- roof placement may try up to eight distinct aim-reposition attempts, while
  non-roof placement retains its existing lower bound;
- after direct vantages and the doorway are exhausted, the builder selects
  only already-observed, supported, feet/head-clear cells on the one-block
  exterior apron that make non-regressive progress toward the roof target;
- each apron move uses the ordinary `MoveToSkill`. On arrival the builder
  performs another first-person survey before reconsidering an interaction;
- the active apron destination is cleared across completion, cancellation,
  failure, replanning, target changes and expired movement;
- no level, chunk or hidden block lookup was added.

A focused JVM contract,
`roofApronStagingWalksTowardOccludedFarSide`, proves that the first selected
staging cell is an observed safe exterior-apron cell and geometrically
advances around the opaque corner. The full
`BuildShelterStepSkillTest` class currently passes.

The physical `roof_jump_placement` fixture has also been strengthened with a
test-only `OccludedApronShelterFrameSource`. The flat fixture normally learns
the complete apron before walls exist, unlike the live terrain failure. The
wrapper removes only those already-mapped far-apron columns after the test
forces the body inside. It restores the delegate's real map only after the
body physically reaches a still-observed door-side corner. It never reads the
level or fabricates occupancy/support evidence. Completion now additionally
requires that ordinary corner visit.

Changed files for this active correction:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- this checkpoint

Latest completed targeted gate:

```text
compileJava: PASS
BuildShelterStepSkillTest: PASS
```

Last formal inner-loop failure remains:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 12,934
build_shelter_step.aim_timeout_crosshair_wrong_block
47/55 structural blocks
```

Immediate next:

1. run only `mcai_companion:roof_jump_placement`;
2. if it passes, rerun the affected obstruction building regressions;
3. repeat the exact configured-MiMo, real-time, zero-human foundation gate;
4. continue from the first new physical failure.

### Roof-apron physical gate result

The strengthened roof gate first exposed two test-driver errors rather than
being relaxed:

- `COMPLETED` can mean the current safe atomic material/visibility batch
  ended while structural steps remain. The fixture now restarts the same
  checkpointed skill from a newer fair observation, matching the production
  supervisor, and treats completion as final only at 55/55 structural steps.
- a first run required the body to occupy the exact corner voxel even though
  it reached the adjacent apron voxel and legitimately obtained the needed
  line of sight. The gate now requires entry into the door-side corner
  observation zone (at most one apron voxel from the geometric corner), not
  an artificial exact-coordinate foot placement.

The final focused run passed:

```text
mcai_companion:roof_jump_placement:
PASS / 14.22 s

Physical contract:
- survival Headless ServerPlayer
- 55/55 structural blocks physically placed
- 25/25 roof planks physically present
- ordinary inventory consumption
- body physically rose for roof placement
- forced late interior stance
- normal doorway exit
- ordinary movement into a fairly observed apron corner-approach zone
- no direct world mutation by the production skill
```

The two affected entity-obstruction regressions also passed together:

```text
mcai_companion:partial_shelter_obstruction_recovery
mcai_companion:placement_obstruction_recovery
PASS / 4.004 s total
```

These are inner-loop physical contracts, not M1 evidence. The next and only
current gate is the exact configured-MiMo, real-time, zero-human foundation
run. It must demonstrate whether the real 47/55 roof-vantage loop is cleared.

### Latest real-MiMo rerun: transient model failure became terminal

The exact real-time, zero-human foundation rerun restored the saved model
profile without exposing its credential and physically completed:

```text
8 oak logs mined and picked up
basic crafting
3 cobblestone mined and picked up
Stone Age and Getting an Upgrade advancements
stone tools
```

It entered `secure_visible_food_reserve`, then failed before the iron phase at
tick 3,472 / 2.903 active minutes. The decision sequence was:

```text
excavate_safe_tunnel with invalid_skill_arguments -> locally rejected
REPLAN
REPLAN
REPLAN
provider response incomplete / CONTEXT_LIMIT
goal SAFE_IDLE(detailCode=model_unavailable)
```

The independent gate correctly failed because the goal became terminal before
the iron toolkit. This run therefore does not yet prove the roof-apron repair
against live terrain.

Current root-cause investigation:

1. determine why the active food phase returned to model planning without a
   productive recovery decision;
2. inspect the tunnel schema/decoder mismatch that accepted a comma-delimited
   target list from MiMo as malformed;
3. fix the supervisor so a single transient incomplete model response cannot
   permanently terminate a recoverable long-running survival goal;
4. add focused model-fault/state-machine regressions, then repeat this exact
   live gate.

Last failing gate:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 3,472 / 2.903 min
SAFE_IDLE(model_unavailable)
```

### 2026-08-01 model-recovery correction

The event database resolved the latest failure more precisely:

- `secure_visible_food_reserve` physically completed before control returned
  to the model;
- the prompt incorrectly made `prepare_iron_toolkit` sound conditional on
  ore already being visible, although that admitted compound owns fair
  scanning and exploration;
- MiMo supplied the semantically unambiguous compatibility spelling
  `ownedPickaxeItemId`, while the tunnel decoder admitted only
  `pickaxeItemId`;
- three accepted `REPLAN` envelopes requested redundant semantic refreshes;
- one subsequently incomplete provider response was classified as
  `CONTEXT_LIMIT`, and the brain incorrectly treated that single transient
  response as immediately fatal.

Production corrections:

- `BrainOrchestrator` now records `context_limit` as a planner correction and
  retries through the existing bounded transient-failure budget. One
  truncated response preserves the running goal; repeated failures still
  terminate after the policy limit.
- The foundation current-phase prompt now directs the model to invoke the
  no-argument `prepare_iron_toolkit` compound immediately and states that the
  compound owns fair scanning, exploration, gathering, smelting and crafting.
- Context-limit correction asks for the shortest valid envelope, one admitted
  phase compound, no optional speech and no redundant observation request.
- The tunnel contract names the exact canonical `pickaxeItemId` field.
- `MiningSkillParameters` accepts the one observed compatibility alias
  `ownedPickaxeItemId` and canonicalizes it. Both spellings together,
  duplicates, missing fields and unrelated unknown fields remain invalid.

Changed files:

- `src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkills.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkillParameters.java`
- `src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- `src/test/java/dev/mcai/companion/skills/mining/MiningSkillParametersTest.java`
- this checkpoint

The three focused regressions failed before the production changes and passed
after them. The full affected test classes then exposed and cleared a skill
guide length regression without increasing the 13,000-character production
limit:

```text
BrainOrchestratorTest
MinecraftPlannerInputFactoryTest
MiningSkillParametersTest
PASS
```

The last formal inner-loop gate remains the failed real-MiMo run at tick
3,472. The immediate next action is the identical configured-MiMo,
real-time, zero-human foundation rerun. If it reaches shelter construction,
that same run will also test whether the earlier live 47/55 roof-vantage loop
is genuinely cleared.

### 2026-08-01 real rerun: provider outage retry storm

The identical real-time, zero-human, configured-MiMo gate progressed beyond
the previous context-limit failure. It physically and legally completed:

```text
8 oak logs and basic crafting
stone tools and food reserve
8 additional furnace cobblestone
7 raw iron, ordinary furnace cooking, iron pickaxe, bucket and shield
foundation workstations and a real chest transaction
foundation shelter-material preparation
```

The SQLite causal trace records
`skill_completed.prepare_foundation_shelter_materials` before the failure. The
next model request hit a genuine `NETWORK_TRANSIENT`. The old supervisor then
made eight new provider requests between `04:32:34.278Z` and
`04:32:36.581Z`, consuming the entire failure budget in about 2.3 seconds.
It set the still-valid goal to:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / tick 8,196 / 6.838 active minutes
Goal: SAFE_IDLE(model_failures_exhausted)
Last completed skill: prepare_foundation_shelter_materials
```

This was a production recovery defect, not a model-planning or shelter
failure. The interactive Codex terminal was interrupted later, but the
authoritative server log and database show that the GameTest process itself
ran to its normal failed-test shutdown and saved all evidence.

The corrected `BrainOrchestrator` now:

- treats network failures, provider 5xx failures and timeouts as provider
  availability state rather than evidence that the player's goal is invalid;
- preserves the same running goal and retries at bounded
  2/4/8/16/32/60-second production intervals, capped at one request per
  minute;
- continues to respect a longer provider `Retry-After`;
- cancels only one locally timed-out model request, keeps the same goal
  revision, and retries through the same outage policy;
- retains the existing bounded terminal policy for malformed responses,
  invalid structured output and other non-outage model defects.

Two new provider-outage regressions and the revised request-timeout regression
failed against the old implementation and now pass. The full affected class
passes:

```text
BrainOrchestratorTest: PASS / 29 tests
```

Changed files:

- `src/main/java/dev/mcai/companion/brain/BrainOrchestrator.java`
- `src/test/java/dev/mcai/companion/brain/BrainOrchestratorTest.java`
- this checkpoint

Immediate next action: verify the configured provider is reachable without
logging credentials, then repeat the exact real-time, zero-human foundation
gate. The next run must reach physical shelter construction before the
roof-apron repair can be considered live-terrain evidence.

### 2026-08-01 real rerun: controlled-world wood budget and exploration

The next identical configured-MiMo gate did not reproduce the provider
outage. It physically completed logs, basic crafting, stone tools, food,
seven iron, the iron toolkit, workstations and storage. It then failed at:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / tick 12,162 / 10.14 active minutes
Skill: prepare_foundation_shelter_materials
Inventory: 62 planks, 0 doors, 0 lights
Reserve fixture: all 16 staged logs physically mined
```

The controlled world supplied 24 total ordinary pre-start logs. After the
legal crafting path and charcoal fallback, the admitted shelter transaction
needed four more logs than remained. The reserve rectangle in
`LiveModelChatGameTests` was expanded from 16 to 20 ordinary mineable logs.
No mid-run resource was inserted, no inventory was modified, and the oracle
still requires vanilla mining, pickup, recipes and construction.

A focused physical regression then exposed a separate existing defect:

```text
mcai_companion:shelter_material_wood_exploration
FAIL / tick 4,481
prepare_foundation_shelter_materials.wood_not_found
```

That scenario contains a real oak log beyond the initial 24-block semantic
range. The body exhausted its visible seed, entered fair exploration and
still did not rediscover the distant log. The parent skill previously erased
the child exploration cause, checkpoint and final position by translating
every child failure to `wood_not_found`.

Changed files so far:

- `src/main/java/dev/mcai/companion/embodiment/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareFoundationShelterMaterialsSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/ShelterMaterialExplorationGameTests.java`
- this checkpoint

Immediate next action: rerun only
`mcai_companion:shelter_material_wood_exploration` with the added causal
diagnostic, fix the exact travel/scan/visibility defect, and pass that focused
physical gate before spending another real-model run.

The focused test was intentionally repeated rather than accepting one green
run. The repeat sequence was:

```text
PASS
PASS
FAIL / phase=AIM_TABLE / elapsedTicks=5,001
```

The failing run had already discovered and physically mined the distant log;
it was cycling at the workstation. The exact defect was a reach-contract
mismatch: `RELIABLE_INTERACTION_DISTANCE` was 5.0 blocks, while an ordinary
survival player has roughly 4.5 blocks of block interaction reach. A
workstation face could therefore be admitted as "reliably actionable" while
the body's current finite vanilla crosshair ray could never select it.
`AIM_TABLE` would wait 120 ticks, rescan the same visible face, and repeat.

Production correction:

- the conservative interaction admission distance is now 3.75 blocks,
  matching the already proven iron-toolkit workstation transaction;
- table and furnace aim timeouts now begin an ordinary walking approach to
  the observed block instead of rescanning from the same unreachable pose;
- a failed child wood exploration retains a causal warning with the child
  failure, checkpoint, body position and look direction.

After that correction, the same focused physical GameTest passed three
consecutive independent server runs:

```text
mcai_companion:shelter_material_wood_exploration
PASS x3
```

Immediate next action: repeat the exact configured-MiMo, real-time,
zero-human foundation gate with the corrected 20-log controlled fixture.

### 2026-08-01 real rerun: construction consumption reopened preparation

The exact configured-MiMo gate ran for 11 minutes 49 seconds and advanced
farther than every previous run. It physically completed:

```text
basic crafting, stone tools and food
seven iron plus iron pickaxe, bucket and shield
verified workstations, chest menu and stored surplus
first shelter-material bundle
multiple dynamic-shelter build transactions
doorway exit and exterior roof-apron repositioning
```

The server log proves that the live roof recovery crossed the open doorway,
tried several exterior roof positions and retargeted a newly visible support.
The run then made a second model call to
`prepare_foundation_shelter_materials`, gathered five of the six remaining
fixture logs, entered fair exploration, and hit the scenario deadline:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / tick 14,046 / 11m49s wall clock
Skill: prepare_foundation_shelter_materials
Checkpoint phase: EXPLORE_WOOD
Skill elapsed ticks: 3,519
Shelter evidence: absent
```

Root cause:

- `SHELTER_MATERIALS_PREPARED` is a sticky, independently verified route
  milestone.
- The route projection nevertheless kept publishing a fresh inventory target
  of 55 structural blocks until `SHELTER_COMPLETED`.
- Ordinary construction therefore made the current inventory appear
  deficient after every placed block.
- The model schema exposed both `build_shelter_step` and full material
  preparation during a committed build, so MiMo legally selected the wrong
  compound and attempted to replenish a complete shelter bundle.

Two first-red regressions captured both layers:

```text
SurvivalRouteReadinessTest:
preparedShelterMaterialsRemainSatisfiedWhileBuildingConsumesThem
FAIL before / PASS after

MinecraftPlannerInputFactoryTest:
foundationSchemaAdmitsOnlyTheCurrentCompoundPhase
FAIL before / PASS after
```

Production correction:

- after `SHELTER_MATERIALS_PREPARED`, construction inputs disappear from
  `currentMinimumTargets`; placement consumption is progress, not milestone
  revocation;
- a committed build exposes only `build_shelter_step` to the model;
- preparation is exposed again only when the server records a concrete
  missing-door, missing-light or missing-structural-material start rejection;
- the trusted phase and route playbooks state this handoff explicitly.

Affected focused classes now pass 18/18:

```text
SurvivalRouteReadinessTest
MinecraftPlannerInputFactoryTest
PASS
```

Changed files:

- `src/main/java/dev/mcai/companion/progression/SurvivalRouteTracker.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/test/java/dev/mcai/companion/progression/SurvivalRouteReadinessTest.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- this checkpoint

Immediate next action: repeat the identical real-time, zero-human MiMo gate.
The decisive assertion is that construction continues with its persisted
confirmed plan instead of starting a second full material bundle.

### 2026-08-01 rerun interrupted outside the game gate

The next identical configured-MiMo run was interrupted by loss of the
controlling Codex terminal session. This is not a product or GameTest failure:
the process disappeared without a GameTest failure line, exception, shutdown
sequence or terminal assertion, and `latest.log` ends abruptly at 18:17:17.

Before interruption the NPC had physically completed:

```text
seven initial logs, basic crafting, stone tools and food
eight cobblestone and seven raw iron
smelting plus the iron pickaxe/toolkit milestone
selection of establish_foundation_workstations
twelve cumulative logs while preparing the chest/workstation transaction
```

Last gate state:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: INDETERMINATE / external process interruption
Last accepted skill: establish_foundation_workstations
Last physical event: oak-log pickup, owned=12, required=12
Last log time: 18:17:17
```

No new code failure was established. Immediate next action: rerun the same
exact live-model gate and inspect the first server-verified failure, with
particular attention to the sticky shelter-material handoff and roof-vantage
recovery.

### 2026-08-01 real rerun: visible livestock occupied placement cells

The next exact configured-MiMo gate ran for 11 minutes 43 seconds. It
physically completed all route phases through shelter materials:

```text
eight initial logs and basic crafting (after one local retry)
stone tools, food, seven iron and the complete iron toolkit
verified table/furnace/chest/storage
55 planks, three doors and lighting
sticky handoff to build_shelter_step only
```

The previous construction-consumption fix is therefore confirmed on the live
model path: after `SHELTER_MATERIALS_PREPARED`, the model never selected full
material preparation again.

The first new failure was physical entity collision:

```text
Gate: mcai_companion:real_zero_human_dedicated_server_foundation
Result: FAIL / tick 14,066 / 11m43s wall clock
Plan origin: [3582092,-44,7718472]
Blocked step: 4 at [3582092,-44,7718476]
Obstruction: minecraft:cow at [3582093,-44,7718477]
Verified shelter evidence: absent
```

Earlier in the same run, `prepare_basic_crafting` also exposed the same class
of bug. Its fair checkpoint showed a grounded, in-reach body but six table
placement attempts and zero menu-open attempts. The expected table cell
overlapped one of the eight fixture cows. A second compound invocation
eventually succeeded only after the local occupancy changed.

Root cause:

- sending a legal use-on-block packet is not proof that vanilla accepted a
  block placement; an entity collision leaves both the item and world
  unchanged;
- basic table support selection considered block support/reach but not fairly
  visible entity collision;
- the shelter planner already rejects recent visible entities, but its
  200-tick memory could expire the first livestock observation before a
  natural 360-degree survey completed;
- a new shelter plan could be synthesized from an old terrain map without a
  fresh site/entity survey, then repeatedly attempt body-push recovery against
  a stationary no-AI cow.

First-red regressions:

```text
PrepareBasicCraftingSkillTest:
placementSupportSkipsAVisibleLivestockCollision
FAIL before / PASS after

DynamicShelterPlannerTest:
fairlySeenLivestockSurvivesACompleteBuildingSurveyWindow
FAIL before / PASS after
```

Production correction:

- shared `VisibleEntityPlacementEnvelope` applies the same conservative,
  first-person-only entity collision geometry to table placement and shelter
  planning;
- table support selection skips cells intersecting a fairly visible actor;
- shelter visible-entity memory is 600 ticks, covering a complete natural
  survey while remaining bounded and expiring;
- every new goal/session shelter plan must complete one fresh first-person
  site survey before constraint synthesis;
- the physical memory-expiry gate deadline now derives from the bounded
  memory contract.

Focused verification now green:

```text
PrepareBasicCraftingSkillTest
DynamicShelterPlannerTest
BuildShelterStepSkillTest
mcai_companion:reachable_basic_crafting
mcai_companion:visible_entity_placement_occupancy
mcai_companion:partial_shelter_obstruction_recovery
PASS
```

Changed files:

- `src/main/java/dev/mcai/companion/perception/VisibleEntityPlacementEnvelope.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareBasicCraftingSkill.java`
- `src/main/java/dev/mcai/companion/skills/building/ShelterFrame.java`
- `src/main/java/dev/mcai/companion/skills/building/DynamicShelterPlanner.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/FoundationGameTests.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildingGameTests.java`
- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareBasicCraftingSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/building/DynamicShelterPlannerTest.java`
- this checkpoint

Immediate next action: repeat the identical real-time, zero-human MiMo
foundation gate and inspect the first server-verified failure after the
mandatory entity-aware site survey.

## 2026-08-01 live-gate continuation

The identical real-time, zero-human MiMo gate is still running in exec session
`22080`. The apparent reserve-log stall was not terminal: the local
`wood_no_inventory_progress` recovery rejected four stale/unproductive semantic
seeds, then the ordinary gatherer physically mined and picked up twelve more
oak logs. The fair persisted checkpoint has advanced to
`prepare_foundation_shelter_materials / FIND_COAL`.

Current root-cause status:

- the last confirmed terminal failure remains the livestock collision during
  shelter construction described above;
- that failure is corrected by entity-aware placement plus a mandatory fresh
  survey and has focused physical coverage;
- the short reserve-log pause was bounded target invalidation/reselection, not
  another terminal failure, and its recovery path produced measured inventory
  progress without intervention.

Files changed for the last confirmed bug are the entity-placement, basic
crafting, shelter planning/building, fixture, regression-test, and checkpoint
files listed in the preceding section. No speculative production edit has
been made for the recovered reserve-log pause.

Last failed gate: `real_zero_human_dedicated_server_foundation`, tick 14,066,
blocked by a cow in shelter step 4. Current rerun has passed basic crafting on
its first attempt and all earlier wood/stone/food/iron/workstation stages.

Immediate next action: keep the current process alive, inspect the first
server-verified terminal result, and patch only the next evidenced production
failure. Do not restart or substitute a scripted result while this live-model
run remains healthy.

## 2026-08-01 active-plan traversal correction

The live rerun ended at tick 9,909 after three causally confirmed shelter
placements. The model selected `build_shelter_step` three more times, but each
invocation failed with `build_shelter_step.no_visible_build_step`; the
supervisor then correctly entered `SAFE_IDLE` with
`repeated_skill_failure_without_progress`.

Root cause:

- `surveyForStep` performed a stationary 360-degree survey only;
- after the builder consumed every support within reach of its exterior
  stance, the immutable plan still had pending far-side wall steps but no
  reachable support;
- the state machine explicitly prohibited relocation for this survey result,
  so another model decision could never create physical progress.

Production correction:

- an active plan may now select an interior construction stance only from
  fairly observed navigation voxels with body clearance, solid support, safe
  danger, and no remembered visible-entity collision;
- it ranks the stance by how many pending, minimum-priority steps are within
  normal player reach;
- it walks there through the ordinary `MoveToSkill`, surveys again, and only
  then resumes ordinary first-person placement;
- initial site relocation remains separate and unchanged.

First-red / focused evidence:

```text
BuildShelterStepSkillTest:
activePlanTraversalChoosesObservedInteriorAfterReachableBatchEnds
FAIL before (method absent) / PASS after

BuildShelterStepSkillTest: 27/27 PASS
mcai_companion:partial_shelter_obstruction_recovery: PASS
```

Changed files for this correction:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Immediate next action: rerun the identical real-time, zero-human MiMo
foundation gate and verify the log contains a physical active-plan traversal
instead of another `no_visible_build_step` loop.

## 2026-08-01 outer-roof traversal correction

The next live gate proved the preceding traversal fix in production:

```text
confirmed=44
Walking through observed terrain to continue active shelter plan
from=exterior doorway apron
stand=observed interior construction cell
```

The builder advanced from the previous three-block boundary to 46 causally
confirmed structural blocks. It then exposed the next root cause: the
remaining pending steps were outer-ring roof gaps, but generic active-plan
traversal always selected an interior cell. From there the opaque completed
roof forced another door exit and corner survey; after one bounded relocation
the skill returned `shelter.insufficient_observation`. Repeating the model
decision chose the same strictly worse interior cell and eventually entered
the expected no-progress safe idle.

Production correction:

- lower/upper walls and inner roof work continue to prefer a safe observed
  interior stance;
- a pending minimum-priority outer roof gap now prefers the one-block
  exterior apron, using only fairly observed feet/head/support clearance and
  normal movement;
- if no safe apron has been observed, it conservatively falls back to the
  interior rather than inventing terrain;
- traversal never selects a future plan block as the body's feet cell.

First-red / focused evidence:

```text
activePlanTraversalUsesExteriorApronForLastOuterRoofGap
FAIL before / PASS after

BuildShelterStepSkillTest: 28/28 PASS
mcai_companion:roof_jump_placement: PASS
```

The physical roof gate independently verified every roof block, inventory
consumption, real jump elevation, door exit and exterior-apron traversal.

Changed files remain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Immediate next action: rerun the identical live-model zero-human foundation
gate and inspect the first server-verified result after exterior-apron
selection.

## 2026-08-01 multi-site initial shelter search

The next identical configured-MiMo, zero-human foundation run did not create a
shelter plan. It passed every earlier survival stage through shelter-material
preparation, then failed three model-selected `build_shelter_step` invocations
with:

```text
shelter.insufficient_observation
phase=SURVEYING
planId=""
confirmed=0
relocationPerformed=true
```

This is an initial-site failure, not a regression in the active-plan roof
traversal. The builder completed one stationary survey, walked to one
deterministically selected observed stand, completed another survey, and then
terminated. `start()` reset the single `relocationPerformed` flag on the next
model invocation, while the planner accepted no prior-site exclusions, so a
retry had no durable way to investigate a different patch.

Production correction:

- initial site search now retains rejected first-person survey centres for the
  same goal revision and body-session generation;
- relocation candidate ranking excludes the complete neighbourhood of every
  rejected survey centre instead of merely avoiding the exact block;
- the body may perform up to four ordinary `MoveToSkill` relocations, with a
  fresh fair survey after each move;
- the search remains bounded, and its memory clears only on a new goal/body
  session or after a valid shelter plan is committed;
- active-plan construction traversal remains separately bounded to one
  relocation per skill invocation;
- a stale plan from an older goal/session can no longer make initial site
  search take the active-plan branch.

First-red / focused evidence:

```text
DynamicShelterPlannerTest:
relocationExcludesPreviouslySurveyedSiteInsteadOfRepeatingIt
FAIL before (three-argument exclusion API absent) / PASS after

BuildShelterStepSkillTest:
initialSiteSearchIsBoundedButNotSingleShot
PASS

DynamicShelterPlannerTest + BuildShelterStepSkillTest: PASS
```

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/DynamicShelterPlanner.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/DynamicShelterPlannerTest.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Last failed gate:
`mcai_companion:real_zero_human_dedicated_server_foundation`, tick 9,043,
after three initial-site retries without a committed plan.

Immediate next action: run the focused physical shelter-relocation gate, then
repeat the identical real-time, zero-human configured-MiMo foundation gate and
inspect the first server-verified terminal result.

## 2026-08-01 live gate blocked before shelter by pickup debt

The focused physical shelter-recovery gate passed after the multi-site search
change:

```text
mcai_companion:partial_shelter_obstruction_recovery
PASS / 1.794 s
```

The identical real-time, zero-human configured-MiMo foundation gate then
failed before reaching shelter construction, at tick 9,333 after 7.780 active
minutes. This run therefore does not yet validate the multi-site change in the
full model chain.

Server/world evidence is exact:

```text
Stats.BLOCK_MINED minecraft:iron_ore = 7
Stats.ITEM_PICKED_UP minecraft:raw_iron = 6
all seven controlled iron fixture blocks = air
prepare_iron_toolkit final code = prepare_iron_toolkit.iron_not_found
```

The gather child mined all seven blocks through vanilla, but the seventh drop
did not enter the player's inventory. `PrepareIronToolkitSkill` treated any
positive partial inventory gain after a child failure as sufficient progress,
discarded the child's `drop_not_collected` causal debt, and returned to block
search. Because no iron blocks remained, it entered a 48-block spiral
`EXPLORE_RESOURCE` for roughly five minutes while the uncollected item could
despawn. The final checkpoint remained:

```text
phase=EXPLORE_RESOURCE
resource=IRON
scanTurns=32
elapsedTicks=7049
gathering=false
child=null
```

This is a production liveness/correctness defect and not a model-decision
failure: MiMo selected `prepare_iron_toolkit` once, and the local compound
never returned control until the test oracle timed out.

Last failed gate:
`mcai_companion:real_zero_human_dedicated_server_foundation`, tick 9,333,
`prepare_iron_toolkit`, seven blocks mined but only six drops collected.

Immediate next action: add a first-red physical/unit contract for partial
resource progress plus a final uncollected drop, keep the drop recovery inside
the fair local compound using only first-person-visible item evidence and
ordinary movement, and prevent a pickup debt from falling through to general
ore exploration.

## 2026-08-01 causal pickup-debt recovery implemented

Root cause: `GatherVisibleBlockClusterSkill` returned only the generic terminal
`drop_not_collected`; after any partial inventory increase,
`PrepareIronToolkitSkill` discarded that causal failure and entered long-range
ore exploration. The just-mined drop could then despawn while the compound
remained busy for minutes.

Production correction:

- the gather skill now retains a typed `DropCollectionDebt` containing the
  just-mined block position, expected item id, required owned count, and
  observed owned count;
- the iron-toolkit compound consumes that debt before accepting partial
  progress and enters a bounded `RECOVER_RESOURCE_DROP` phase;
- recovery first uses only matching first-person-visible item observations
  within six blocks of the causal origin, otherwise walks normally to the
  just-mined block and performs a bounded semantic scan;
- success requires a server-observed inventory increase or satisfaction of the
  resource target; failure is explicit and cannot fall through to the
  five-minute ore-search branch;
- the recovery child, origin, and state are checkpointed and cancelled
  normally.

Changed files:

- `src/main/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkill.java`
- `src/main/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkill.java`
- `src/test/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/foundation/PrepareIronToolkitSkillTest.java`
- this checkpoint

Focused evidence:

```text
GatherVisibleBlockClusterSkillTest: PASS
PrepareIronToolkitSkillTest: PASS
mcai_companion:occluded_iron_toolkit_table: PASS / 1.995 s
```

Current gate:
the next identical configured-MiMo, real-time, zero-human dedicated-server run
has passed the prior boundary (all seven raw iron items reached inventory),
completed vanilla furnace/tool preparation, prepared 55 oak planks, three
doors, and four torches, and committed a compact terrain-derived shelter plan.
At the latest observation it had server-confirmed at least 43 placements and
was traversing the doorway/exterior apron to reach the remaining roof cells.
The run is still active, so this is not yet a pass.

Last failed gate remains:
`mcai_companion:real_zero_human_dedicated_server_foundation`, tick 9,333,
before this pickup-debt correction.

Immediate next action: let the current live gate reach a server-verified
terminal result. If roof traversal fails, repair only the evidenced
reachability/liveness defect and rerun the narrow shelter physical gate before
repeating this exact live gate.

## 2026-08-01 live shelter stalled after 43 confirmed placements

The next identical configured-MiMo, real-time, zero-human foundation run
passed all prior stages, including seven-of-seven raw-iron pickup, vanilla
smelting and toolkit crafting. It committed a terrain-derived compact shelter
plan and physically confirmed 43 placements, then failed at tick 9,656 after
8.048 active minutes:

```text
goal detail = repeated_skill_failure_without_progress
skill = build_shelter_step
skill failure repeated three times = shelter.insufficient_observation
checkpoint phase = SURVEYING
confirmed = 43
```

The SQLite audit and movement trace establish two coupled local-executor
defects:

1. roof-apron staging sorted by distance to the far roof target, so it handed
   `MoveToSkill` a far-side destination; stale `AIR` observations inside the
   shell that this same skill had since filled attracted the route through the
   wall, and movement stalled at the wall;
2. after that failure, each new invocation surveyed and alternated between two
   adjacent doorway/apron cells. The executor forgot surveyed traversal stands
   and reported failure after exactly one relocation, so the brain correctly
   terminated the third identical no-progress failure.

Production correction:

- roof-apron observation staging now advances through nearest fairly observed
  exterior cells; every hop must still make geometric progress toward the
  pending roof region;
- active-plan traversal retains visited survey stands until a physical
  placement is confirmed, preventing doorway/apron oscillation across model
  invocations;
- once outside, active roof traversal only selects a cardinally adjacent cell
  on the generated one-block apron, preventing a direct path through stale
  interior navigation evidence;
- a single local skill invocation may survey the entire generated apron
  perimeter instead of reporting one failure per cell; the exact bound is
  derived from the current plan dimensions;
- traversal memory clears on a confirmed placement, new/repaired plan, or
  changed construction step, and the attempt/stand counts are included in the
  diagnostic checkpoint.

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

First-red / focused evidence:

```text
roofApronStagingWalksTowardOccludedFarSide:
FAIL before (first destination was not an adjacent apron cell) / PASS after

activePlanRoofTraversalDoesNotOscillateBetweenSurveyedApronCells:
FAIL before (visited-frontier API absent) / PASS after

activePlanTraversalMaySurveyTheWholeBoundedApronInOneInvocation:
FAIL before (bounded multi-hop contract absent) / PASS after

BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 11.86 s
```

Last failed gate:
`mcai_companion:real_zero_human_dedicated_server_foundation`, tick 9,656,
after 43 confirmed placements and three survey-only retries.

Immediate next action: repeat that exact real-time configured-MiMo,
zero-human dedicated-server gate. Treat only its next server-verified terminal
result as evidence; do not infer broader M1 completion from this focused gate.

## 2026-08-01 live roof advanced, then lacked a safe interior return

The next identical live gate again passed the complete pre-shelter chain. The
new apron logic physically advanced around the shell and increased confirmed
construction progress instead of repeating the old two-cell oscillation:

```text
adjacent exterior staging:
  west doorway -> west apron z-1 -> z-2 -> z-3
active traversal:
  attempt=1/24, confirmed=40
  attempt=2/24, confirmed=42
```

The gate nevertheless failed at tick 10,957 after 9.136 minutes. SQLite
records the three terminal child failures exactly as:

```text
build_shelter_step.no_observed_traversal_stand
build_shelter_step.no_observed_traversal_stand
build_shelter_step.no_observed_traversal_stand
```

The next evidenced boundary is a corridor-transition defect. From the exterior
apron, roof aim recovery selected observed interior floor candidates and sent a
direct `MoveTo` route across the completed wall. Two such routes stalled. After
returning to the door side, the adjacent exterior frontier was exhausted or
terrain-blocked, but active traversal had no explicit ordinary route back
through the still-open planned doorway; it therefore returned no stand.

Last failed gate:
`mcai_companion:real_zero_human_dedicated_server_foundation`, tick 10,957,
42 confirmed placements, final code
`build_shelter_step.no_observed_traversal_stand`.

Immediate next action: prohibit exterior-to-interior aim shortcuts, add a
first-person-validated one-cell return corridor along the apron through the
open doorway and into the interior, cover it with first-red contracts, rerun
the focused roof GameTest, then repeat this exact live gate.

## 2026-08-01 roof return fixed; long physical gate exposed sample eviction

The exterior/interior transition repair is now implemented. Roof aim recovery
cannot select an interior stand while the body is outside the completed wall,
and an exhausted exterior apron follows a cardinal, fairly observed corridor
back along the apron, through the still-open planned doorway, and onto the
interior floor before resuming roof work. Return state is checkpointed and
cleared whenever a placement, plan replacement, or repair invalidates it.

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Focused contracts passed:

```text
roofAimFromExteriorCannotShortcutAcrossTheCompletedWall: PASS
roofInteriorReturnFollowsApronDoorAndInteriorCorridor: PASS
BuildShelterStepSkillTest: PASS
```

The first rerun of `mcai_companion:roof_jump_placement` then advanced farther
to 47 physically confirmed placements but failed at tick 2,074 with:

```text
build_shelter_step.observation_expired
phase=FAILED
confirmed=47
```

This is a separate admission-boundary defect. `ServerShelterFrameSource`
retains 2,048 observations, while the accelerated physical scenario publishes
one every tick. `BuildShelterStepSkill.prepare` always requires the original
model-authored sample to remain in that history. Internal prepare calls after
this already-bound skill performs a fresh survey or local obstruction replan
therefore reject a current, session-matching first-person frame once the old
receipt is evicted. External precondition/start admission must continue to
require the exact authored sample.

Last failed gate:
`mcai_companion:roof_jump_placement`, tick 2,074, 47 confirmed placements,
`build_shelter_step.observation_expired`.

Immediate next action: separate external authored-sample admission from
bound internal survey admission, retain all current-frame freshness,
session/player/dimension checks, add a first-red boundary contract, rerun the
complete shelter unit suite and focused roof physical gate, and only then
repeat the real-time configured-MiMo zero-human foundation gate.

## 2026-08-01 long roof construction and interior return gate pass

The sample-admission boundary is now explicit:

- external model decisions still require their exact authored observation to
  remain retained and match player, dimension, and session;
- internal planning after a survey performed by an already admitted,
  goal/session-bound skill uses the fresh current first-person frame and does
  not depend on the old model receipt remaining in the rolling history;
- freshness, player, dimension, actuator session, and bound session checks
  remain unchanged.

The first physical rerun proved that correction by advancing past the previous
tick-2,074 sample-eviction point. It then exposed two additional, independently
reproduced movement-state defects:

1. construction selected an adjacent grid stand with `arrivalRadius=0.6`;
   centre-targeted `MoveToSkill` could legally complete 0.59 blocks from the
   target while the body remained in the previous `GridPos`, so the parent
   selected the same cell 18 times;
2. after each one-cell corridor move, the survey could expose a roof face and
   preempt the still-active return compound. That sent an aim route diagonally
   through a one-cell doorway and stalled.

Corrections:

- exact construction stands now use a 0.35-block arrival radius;
- the local `move_to` typed contract accepts radii down to 0.25, while retaining
  finite-coordinate and upper-bound validation;
- a roof return remains an exclusive local compound until the body's observed
  feet enter an interior floor cell. Intermediate surveys update fair
  navigation but cannot preempt it with a placement;
- the exterior-apron, planned doorway, and interior route remains cardinal and
  first-person-observed throughout.

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/core/MoveToParameters.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/core/CoreSkillParametersTest.java`
- this checkpoint

First-red / focused evidence:

```text
internalSurveyReplanDoesNotRevalidateAnEvictedAuthoredSample:
compile FAIL before / PASS after

constructionTraversalMustActuallyEnterTheSelectedGridCell:
compile FAIL before / PASS after

roofReturnRemainsExclusiveUntilTheBodyEntersTheInterior:
compile FAIL before / PASS after

BuildShelterStepSkillTest + CoreSkillParametersTest + MoveToSkillTest:
PASS

mcai_companion:roof_jump_placement:
PASS / 40.63 s
```

The physical gate completed all 55 structural placements, ordinary inventory
consumption, real elevation for roof work, forced late interior recovery,
doorway traversal, and exterior-apron observation. It is an inner-loop
GameTest result, not formal M1 evidence.

Immediate next action: rerun the identical configured-MiMo, real-time,
zero-human `mcai_companion:real_zero_human_dedicated_server_foundation` gate
and continue from its first server-verified terminal failure.

## 2026-08-01 real-MiMo foundation rerun: post-shelter catalogue drift

The identical configured-MiMo, real-time, zero-human dedicated-server gate was
rerun after the roof and interior-return repairs:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 14,222 / 11.85 min
Goal: SAFE_IDLE(model_failures_exhausted)
Oracle: survived=false, shelterValid=false
```

This run physically completed the earlier foundation stages: eight oak logs,
stone and iron tools, food reserve, seven raw iron, smelting, iron pick,
workstations, shelter materials, and 47 confirmed shelter placements. It also
proved the repaired exterior-to-interior return route in the live world:
far exterior apron -> exterior door side -> doorway -> interior. The former
sample-eviction, loose-arrival, and roof-return-preemption failures did not
recur.

The new terminal chain began only after the body returned inside. Two more
`build_shelter_step` decisions were accepted, then the current decision
catalogue no longer exposed that skill while MiMo continued requesting it.
Repeated `unknown_skill(build_shelter_step)` responses, one stale
`establish_foundation_workstations`, an invalid survey request, one accepted
`sleep_in_observed_bed`, and a malformed response exhausted the bounded model
validation-failure budget. No evidence yet justifies treating the objective as
complete: the authoritative oracle still reported an invalid shelter.

Changed files before this failure:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/core/MoveToParameters.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/core/CoreSkillParametersTest.java`
- this checkpoint

Last failed gate: the real-MiMo gate above. Immediate next action: inspect the
persisted audit/world state and the exact dynamic skill-catalogue transition,
identify why the skill disappeared before the server oracle accepted the
shelter, add a first-red contract around that transition, and fix completion
promotion or current-catalogue feedback without weakening the oracle or merely
raising the failure budget.

### Post-failure audit and first correction

The persisted SQLite checkpoint proves that `build_shelter_step` completed at
game tick 12,082 with all 57 plan steps confirmed. Direct read-only inspection
of the saved Anvil chunk also found the generated 5x5 structure intact: solid
dirt floor, complete oak-plank walls and roof, empty 3x3x2 interior, torch,
and both halves of the oak door closed. The saved player was alive at full
health inside the shelter.

Two independent orchestration defects caused the terminal failure:

1. once all FOUNDATION milestones were verified,
   `nextObjectives` became empty and the dynamic schema correctly removed
   past compound skills, but `routePlaybook` fell through to the generic
   all-phase route while the static guide still named every retired skill;
   MiMo copied those names and received repeated `unknown_skill` rejections;
2. `GoalCoordinator.markTerminal` advances the mutation revision, and
   `CompanionWorldData.updateGoalState` treated every revision change as a new
   goal. It erased the same goal's progress and shelter evidence, producing
   the misleading terminal `shelterValid=false` diagnostic despite the intact
   saved structure.

Corrections now implemented:

- the empty FOUNDATION objective has an explicit
  `SERVER_VERIFIED_COMPLETE` phase that tells the model to choose
  `COMPLETE_GOAL` immediately;
- the survival-night phase is also explicit and cannot fall through to the
  generic construction route;
- exact registered skill names absent from the current allow-list are removed
  from the supplied static guide;
- goal status transitions preserve audit notes and route evidence for the same
  stable goal UUID, while a genuinely new UUID still clears goal-scoped data.

First-red evidence:

```text
completedFoundationAdvertisesCompletionWithoutRetiredSkills:
compile FAIL before / PASS after

terminalTransitionPreservesEvidenceForTheSameGoal:
FAIL before / PASS after

MinecraftPlannerInputFactoryTest + CompanionWorldDataTest:
PASS
```

Changed files:

- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/world/CompanionWorldData.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- `src/test/java/dev/mcai/companion/world/CompanionWorldDataTest.java`
- this checkpoint

Immediate next action: run adjacent brain/completion/world persistence tests,
then a minimal real-MiMo completion-phase decision gate before repeating the
full real-time zero-human foundation route.

Adjacent orchestration and persistence suites passed:

```text
BrainOrchestratorTest
GoalCoordinatorTest
ServerGoalCompletionVerifierTest
SurvivalRouteReadinessTest
MinecraftPlannerInputFactoryTest
CompanionWorldDataTest
PASS
```

The opt-in `LiveProviderSmokeTest` was upgraded to send the real production
FOUNDATION-complete planner prompt through the saved configured MiMo profile.
It negotiated the provider and returned an exactly validated
`COMPLETE_GOAL` decision with matching request, observation, and goal
revisions:

```text
LiveProviderSmokeTest: PASS / real MiMo / 10.8 s
```

This is real-model evidence for the repaired terminal decision boundary, not a
substitute for the physical zero-human route. Immediate next action: rerun the
complete real-time zero-human foundation GameTest.

## 2026-08-01 real-MiMo rerun: roof recovery repeats without progress

The complete real-time, configured-MiMo, zero-human foundation gate was rerun:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 11,974 / 9.981 min
Goal: SAFE_IDLE(repeated_skill_failure_without_progress)
Shelter evidence: absent
```

The prior post-completion catalogue failure did not recur. This run physically
completed wood, basic crafting, stone tools, food, seven raw iron, vanilla
smelting and iron toolkit, workstations/storage, and shelter materials. The
builder selected a new site after three fair observed-site walks and reached
42 confirmed shelter blocks. A roof jump-height aim timeout ended one batch
safely; later batches advanced to 44 confirmations, then three model-authorized
`build_shelter_step` executions ended without any additional server-confirmed
placement. The skill-progress watchdog correctly terminated the repeating
loop rather than spending requests indefinitely.

Last failed gate: the exact live gate above. Immediate next action: query the
new SQLite notices/checkpoint and bounded live log around the last skill
executions, identify the precise local failure code and remaining roof
geometry, add a first-red recovery contract, then rerun the focused physical
roof gate before another full live route.

## 2026-08-01 roof-apron dead-end root and first-red contract

The last live body was physically alive on the south shelter apron after
reaching the selected relocation cell. The plan had 44 server-confirmed
structural placements and 11 roof blocks remaining. Its traversal state held
exactly the current cell and the immediately previous apron cell.

The active-plan selector currently removes every explored stand before it
builds the cardinal frontier. Consequently, when the newly surveyed forward
cell lacks safety-grade evidence, it cannot step back over the
body-verified previous cell to reach a different, already observed frontier.
This converts a local one-sided observation gap into
`build_shelter_step.no_observed_traversal_stand`.

First-red contract:

```text
BuildShelterStepSkillTest
  activePlanRoofTraversalBacktracksToReachObservedApronFrontier
FAIL / NoSuchElementException from the expected destination Optional
```

The fixture supplies only ordinary first-person-grade support, feet and head
evidence for the current cell, previous cell, and a new cell beyond it. No
world scan or hidden terrain is present. Expected behavior is to return the
previous cell as the first hop toward the unvisited frontier.

Changed file:

- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Last failed product gate remains:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / build_shelter_step.no_observed_traversal_stand at 44 confirmations
```

Immediate next action: replace the one-hop unvisited-only apron selector with
a bounded shortest path over observed safe apron cells, run the focused unit
suite, then rerun the physical roof GameTest before another real-MiMo route.

The selector now builds a bounded cardinal graph from only observed-safe
apron stands. Already explored stands may be used as transit, but the selected
path must terminate at a different unvisited observed frontier; only its first
one-cell hop is sent to `MoveTo`. This preserves the anti-oscillation rule and
does not authorize hidden terrain, diagonal wall shortcuts, or direct
position changes.

Verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
  PASS / 45.49 s / Forge 65.0.0 / physical ServerPlayer route
```

The physical gate constructed the complete roof through ordinary inventory,
movement, collision, aiming and placement, including exterior apron and
doorway return travel.

Changed production file:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`

Immediate next action: repeat the exact configured-MiMo real-time zero-human
foundation gate. Unit and GameTest results above remain inner-loop evidence,
not M1 completion.

## 2026-08-01 real-MiMo rerun: apron fixed, jump-aim exhaustion exposed

The exact configured-MiMo real-time zero-human foundation gate was rerun:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 10,959 / 9.135 min
Goal: SAFE_IDLE(repeated_skill_failure_without_progress)
```

This run restored the saved provider profile without another API-key entry
and physically completed the same wood, stone, food, seven-iron, vanilla
smelting, iron pickaxe, workstations/storage, shelter material and initial
construction route. At 44 confirmed structural blocks, the previous
`no_observed_traversal_stand` failure did not recur. The body walked six
additional observed exterior-apron cells and reached another side of the
shelter.

The new terminal failure is independent:

```text
build_shelter_step.aim_timeout_jump_height
same roof step and same stance repeated across three model-authorized batches
```

Root cause:

1. after four accepted jump-aim inputs fail to expose the top face, the
   grounded state has no transition to `startAimReposition`;
2. it waits stationary until the generic aim deadline;
3. `deferCurrentAimStep` cannot select another immediately reachable block
   and returns empty;
4. the next external skill invocation clears the lone deferred mask and
   selects the identical step again.

First-red contract:

```text
BuildShelterStepSkillTest.exhaustedGroundedJumpAimRequiresAReposition
compile FAIL: jumpAimRepositionRequired(boolean,int) absent
```

Changed file:

- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this checkpoint

Immediate next action: add the bounded grounded-exhaustion transition to an
observed safe aiming stance, reset the per-stance jump budget only after
physical relocation, run the builder unit suite and physical roof gate, then
repeat the real-MiMo route.

## 2026-08-01 grounded jump-aim exhaustion fixed and physically verified

Root cause:

- Roof placement could dispatch four legitimate jumps without exposing the
  support top face.
- Once grounded again, the AIMING state had no transition to another stance.
  It waited for the generic aim deadline, failed as
  `aim_timeout_jump_height`, and a later invocation selected the same step
  and stance again.
- The jump-attempt counter was tied to the step rather than the physical
  stance, so there was no bounded “this viewpoint is exhausted” transition.

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
  - added `jumpAimRepositionRequired`;
  - added `AimProgress.JUMP_ATTEMPTS_EXHAUSTED`;
  - after four grounded jump attempts, starts a normal observed-safe aim
    reposition instead of waiting for timeout;
  - resets the jump budget only after a successful physical stance change.
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
  - added first-red
    `exhaustedGroundedJumpAimRequiresAReposition`.

Verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
  PASS / 42.97 s / Forge 65.0.0 / physical ServerPlayer route
```

The physical gate walked through observed exterior cells, emitted
`Starting shelter aim reposition`, retargeted to newly visible support, and
continued ordinary movement, aiming and placement.

Last failed full gate remains the earlier real-MiMo run:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / repeated build_shelter_step.aim_timeout_jump_height after 44 confirms
```

Immediate next action: rerun that exact player-chat-triggered, real-MiMo,
zero-human foundation gate and verify physical progress beyond the former
44-block boundary.

## 2026-08-01 real-MiMo rerun: 44 passed, opposite-apron return dead-end exposed

The exact configured-MiMo real-time zero-human foundation gate was rerun:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 12,459 / 10.39 min
Goal: SAFE_IDLE(repeated_skill_failure_without_progress)
```

What passed in the real run:

- saved provider configuration restored without API-key reentry;
- headless `ServerPlayer` joined a server with no human player;
- the model authorized the initial wood route from the player-style task;
- ordinary physical skills gathered seven logs, crafted stone tools, secured
  food, mined seven raw iron, smelted an iron pickaxe, created the foundation
  workstations/storage, gathered shelter material and began construction;
- the former 44-confirmation `aim_timeout_jump_height` did not recur;
- exterior observed-apron traversal and aim reposition progressed to plan
  step 49 and 47 physically confirmed structural blocks.

New terminal failure from SQLite audit:

```text
build_shelter_step.no_observed_traversal_stand
confirmed=47
returningInsideForRoof=true
same failure repeated across three model-authorized invocations
```

Root cause:

- `roofInteriorReturnTraversalTarget` required every exterior-apron return
  hop to strictly reduce Euclidean distance to the door.
- At the midpoint of the apron side opposite the door, both legal cardinal
  ring directions initially move farther from the door before turning a
  corner.
- The greedy rule therefore reported no observed route even though a safe
  route was already present in first-person navigation evidence.

First-red contract:

```text
BuildShelterStepSkillTest
  .roofInteriorReturnEscapesOppositeApronDistanceMinimum
FAIL / NoSuchElementException under the greedy implementation
```

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
  - replaced greedy roof-apron return with bounded BFS over only observed,
    unobstructed apron cells;
  - returns only the first ordinary cardinal hop toward the known exterior
    doorway;
  - does not accept hidden cells or route through the built shell.
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
  - added the opposite-side local-minimum regression.

Verification:

```text
BuildShelterStepSkillTest: PASS
```

Immediate next action: run the physical roof GameTest, then repeat the exact
real-MiMo zero-human foundation route.

## 2026-08-01 incremental observed-frontier return physically verified

The first physical roof rerun exposed a stricter case than the unit fixture:

```text
mcai_companion:roof_jump_placement
FAIL / 31.60 s
build_shelter_step.no_observed_traversal_stand
confirmed=47 / stepIndex=50 / returningInsideForRoof=false
```

A controlled dead-end diagnostic on the repeated run showed:

- two cardinally adjacent exterior cells were observed safe;
- the pending roof role had moved from the outer ring to the inner roof, so
  the body needed to return inside;
- the exterior doorway had been observed but its final support/clearance
  evidence was not yet safety-grade;
- the full-path BFS therefore rejected the whole return before taking its
  first safe step.

The correct fair behavior is incremental: walk to the edge of currently
proven terrain, survey again, and extend the route only when the next cell
becomes safe.

First-red contract:

```text
BuildShelterStepSkillTest
  .roofInteriorReturnAdvancesToObservedFrontierBeforeDoorIsSafe
compile FAIL before the explored-frontier overload existed
```

Changed production behavior:

- a complete observed-safe path to the exterior doorway remains preferred;
- if the doorway path is incomplete, BFS selects the closest reachable,
  unvisited safe apron frontier;
- previously explored safe cells may be used as transit but cannot be the
  frontier, preventing two-cell oscillation;
- only the first cardinal hop is dispatched, followed by another ordinary
  first-person survey.

Verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
  PASS / 41.89 s / Forge 65.0.0 / physical ServerPlayer route
```

The passing physical run reached `confirmed=47`, then walked eleven distinct
one-cell return positions while refreshing observation, re-entered the
shelter, retargeted newly visible support and continued the remaining inner
roof placements.

Immediate next action: repeat the exact configured-MiMo, player-style task,
zero-human foundation gate.

## 2026-08-01 real-MiMo rerun: convex-corner face occlusion exposed

The exact configured-MiMo zero-human foundation gate was rerun after the
incremental return fix:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 9,651 / 8.045 min
Goal: SAFE_IDLE(repeated_skill_failure_without_progress)
```

This run again physically completed the full wood, stone, food, seven-iron,
smelting, iron toolkit, workstations/storage, shelter-material and initial
construction route. The earlier return dead-end did not recur.

New terminal audit code:

```text
build_shelter_step.aim_timeout_crosshair_wrong_face
confirmed=45 / stepIndex=38
same failure across three model-authorized invocations
```

The crosshair was on an ordinary visible oak-plank support, but its EAST face
would place into a different cell while the intended roof cell required the
support's NORTH face. Clicking the visible EAST face would be an incorrect
world mutation, so the fail-closed face check was correct.

Root cause:

- reaching the NORTH face required walking around a convex shelter corner;
- `roofApronObservationStagingCandidates` allowed only moves that did not
  increase Euclidean distance to the target;
- the next fair exploration step could temporarily move farther from the
  target, so no staging move was produced and aiming timed out in place.

First-red contract:

```text
BuildShelterStepSkillTest
  .roofApronStagingMayStepAwayToExploreAroundOcclusion
FAIL / expected one observed-safe frontier, received empty list
```

Changed behavior:

- exterior aim staging now searches the observed-safe apron graph for an
  unvisited frontier, without requiring monotonic target-distance progress;
- attempted cells are excluded only as frontier destinations and remain
  legal transit;
- only the first cardinal hop is dispatched and every hop is followed by a
  fresh first-person survey;
- the exact support-face check remains mandatory before placement.

Verification:

```text
BuildShelterStepSkillTest: PASS
```

Immediate next action: run the physical roof gate and then repeat the exact
real-MiMo zero-human foundation route.

Physical verification:

```text
mcai_companion:roof_jump_placement
PASS / 51.39 s / Forge 65.0.0 / physical ServerPlayer route
```

The body took five exterior staging hops that were not constrained to
monotonic target distance, then at 47 confirmed blocks took twelve
incrementally surveyed return hops, re-entered the shelter, rebound to newly
visible support faces and continued the inner roof.

Immediate next action: repeat the exact configured-MiMo zero-human foundation
gate.

## 2026-08-01 real-MiMo rerun: completed placement revoked on next batch

The exact configured-MiMo, player-style, zero-human foundation gate was run
again after the convex-corner fix:

```text
mcai_companion:real_zero_human_dedicated_server_foundation
FAIL / tick 10,449 / 8.710 min
Goal: SAFE_IDLE(repeated_skill_rejection_without_world_change)
```

The run physically completed model-authorized wood, stone, food, seven-iron,
smelting, iron-toolkit, workstation and shelter-material stages. Construction
reached 45 causally confirmed vanilla placements. The convex-corner path
advanced through steps 41, 43 and 45 without the former wrong-face terminal
failure.

New terminal audit sequence:

```text
skill_completed.build_shelter_step (worldRevision=12, confirmed=45)
build_shelter_step.completed_block_missing x3 (worldRevision=13)
repeated_skill_start_rejection
```

Current root boundary: the construction batch correctly persisted 45
confirmed placements, but `validateConstruction` revoked at least one
structural checkpoint when the next model-authorized batch began. The audit
code currently omits the contradicted step/target and observed revision, so
the exact false-negative voxel is not yet identifiable from the database.

Changed files in the active bug chain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Last passing gates:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 51.39 s
```

Immediate next action: add a no-secret contradiction diagnostic and a
first-red contract for the actual observation semantics, then rerun only the
targeted unit/physical roof gates before repeating the live route.

Root cause confirmed:

- `OccupancyEvidence.MULTI_RAY_CLEAR` is deliberately heuristic navigation
  evidence and `isFullBodyFact()` is false;
- `confirmedPlacementContradicted` incorrectly treated every newer
  `VoxelKind.AIR`, including this heuristic, as proof that a causally placed
  structural block disappeared;
- after the executor yielded a construction batch, that false contradiction
  rejected every subsequent model-authorized batch.

First-red contract:

```text
BuildShelterStepSkillTest
  .heuristicAirDoesNotRevokeCausalStructuralPlacement
FAIL / heuristic MULTI_RAY_CLEAR incorrectly revoked the wall checkpoint
```

Changed behavior:

- clear sight rays never revoke a causal structural placement;
- `BODY_OCCUPIED` at the exact structural target remains direct proof that
  the full block is absent;
- a current semantic ray-hit of a different block at the exact target
  contradicts both structural and functional placements;
- terminal contradictions now log the step, target, expected block, voxel
  evidence, visible target faces and revisions, with no provider secret.

Verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
  PASS / 5.647 min / Forge 65.0.0 / physical ServerPlayer route
```

The physical route reached 53 confirmed placements, walked the full
observed-safe exterior apron, returned through the doorway, revisited deferred
roof steps and retargeted only newly visible exact support faces.

Immediate next action: rerun the exact configured-MiMo, player-style,
zero-human foundation gate.

## 2026-08-01 live rerun: correct but visibly slow apron panoramas

The next real-MiMo zero-human run restored the saved provider profile, then
physically completed wood, basic crafting, stone tools, food, seven iron,
smelting, iron tools, workstations and shelter materials. MiMo produced six
invalid `prepare_foundation_shelter_materials` calls with unrelated arguments;
the local schema rejected all six without mutation, fed back the validation
failure, and MiMo then emitted the valid no-argument call and continued.

The live run was manually stopped after the persisted checkpoint proved that
construction was progressing but too slowly:

```text
build_shelter_step / confirmed 40 -> 42
phase=SURVEYING / stepIndex=41
```

This was not a strict 41/43 loop as first inferred from console selection
logs. The safe checkpoint showed two additional physical placements. The
actual defect was performance and naturalness: every one-cell exterior apron
hop invoked `survey_surroundings(8, vertical=true)`, a 24-view stationary
panorama. Adjacent hops took about eleven seconds and repeatedly rotated the
head through a visibly robotic full scan.

First-red contract:

```text
BuildShelterStepSkillTest
  .roofApronArrivalUsesTargetedRefreshInsteadOfPanorama
compile FAIL before the refresh-policy contract existed
```

Changed behavior:

- the initial doorway staging and first exterior-door sample still perform a
  broad panorama to establish the safe apron;
- subsequent one-cell apron hops turn toward the active support and wait for
  a newer semantic sample;
- if targeted observation cannot expose a route, the existing bounded
  recovery may still perform one full panorama as fallback;
- no hidden block, direct chunk read, teleport or unchecked placement was
  introduced.

Verification:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement:
  PASS / 2.655 min / previously 5.647 min
```

Exterior adjacent-hop cadence improved from about eleven seconds to about
two seconds while preserving fresh semantic observation. The remaining
incremental return-to-door hops still take about seven to eight seconds
because that separate relocation path retains a panorama; this is a measured
optimization target, not a correctness bypass.

Immediate next action: repeat the real-MiMo zero-human foundation gate with
the targeted apron refresh.

## 2026-08-01 roof fallback state across model batches

Latest exact live evidence:

- one configured-MiMo, player-style, zero-human foundation run completed the
  complete resource/workstation/shelter chain and passed in 8.116 minutes;
- the immediate fresh rerun reached 49 causal placements, then exposed an
  elevated exterior body position that was neither a legal construction apron
  nor accepted by the strict return predicate;
- accepting only a one-block observed return slope fixed that state, with the
  legal construction-apron predicate left unchanged;
- the next live rerun exposed a second batch-boundary defect: persistent
  per-step exterior exhaustion survived, but the local attempt counter reset,
  so an exhausted roof step could be deferred forever instead of returning.
  The return decision now accepts the persistent exhaustion bit.

Changed files in this bug chain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `src/test/java/dev/mcai/companion/skills/core/MoveToSkillTest.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Last passing targeted gates:

```text
BuildShelterStepSkillTest: PASS
MoveToSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 2.590 min
configured-MiMo zero-human foundation: PASS / 8.116 min
```

Latest failing gate:

```text
mcai_companion:roof_jump_placement: STOPPED after >6 min
confirmed placements: 48
repeating transition:
  exhausted step 43 -> observed doorway -> interior
  visible target changes to step 38 -> doorway staging
  crosshair changes back to exhausted step 43 -> interior
```

Current root-cause hypothesis is now supported by the repeated physical trace:
the exterior-exhaustion bit is per target step, but a centre-crosshair
adaptation can change from an exhausted roof target to an unexhausted roof
target while already inside. That loses the active interior-fallback intent,
sends the body outside again, and the reverse crosshair adaptation selects the
old exhausted target. No placement progress is made.

Immediate next action: add a first-red contract that interior roof fallback is
carried across a same-phase visible retarget, implement that state transfer,
then rerun only `BuildShelterStepSkillTest` and the physical roof gate. Repeat
the exact configured-MiMo zero-human foundation gate only after both pass.
Formal M0-M4 statistical gates remain NOT_RUN.

## 2026-08-01 same-step support-face oscillation

The interior-fallback chain above was extended with three bounded corrections:

- roof fallback now belongs to the active roof-priority ring instead of one
  transient step, so a same-phase step selection cannot silently send the
  body back outside;
- a fully deferred interior ring relocates to another observed interior
  stance while retaining explored-stance history;
- elevated horizontal roof faces use jump aiming when the standing eye is
  still below the face.

Those changes produced real physical progress from 36 to 42 confirmed
placements and reached later roof indices. The latest physical run then
exposed a different deterministic loop at step 52:

```text
NORTH support -> EAST support -> NORTH support
interior stance A -> interior stance B -> interior stance A
```

Each semantic sample saw only one of two legal support faces. Rebinding to the
new face cleared all aim progress; turning toward it made the old face visible
again, so the skill could oscillate forever without placing or reaching its
bounded timeout.

Current correction records the exact block coordinate and face whenever the
same build step abandons a support. Camera motion may still reveal and choose
a genuinely new support, but cannot return to one already abandoned by that
step. The history resets when the generated build step changes.

Changed files in the current correction:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Latest failing gate:

```text
mcai_companion:roof_jump_placement: STOPPED manually
last known confirmed placements: at least 42
last state: step 52 support/stance oscillation with no forward progress
```

Latest passing targeted gate:

```text
BuildShelterStepSkillTest: PASS
roofRetargetDoesNotReturnToAnAlreadyRejectedSupportFace: PASS
```

Immediate next action: rerun only
`mcai_companion:roof_jump_placement`. If it passes, run the exact configured
MiMo player-style zero-human foundation gate; only then run the broader local
regression. Formal M0-M4 statistical gates remain NOT_RUN.

## 2026-08-01 final roof cells after airborne-alignment correction

The physical roof gate above did not pass. It made genuine vanilla placement
progress to 52 confirmed blocks, then failed after 4.424 minutes with:

```text
mcai_companion:roof_jump_placement: FAIL
failure: build_shelter_step.no_observed_traversal_stand
remaining repeating steps: 48 and 50
confirmed placements: 52
active traversal stands exhausted: 8
```

The earlier jump instrumentation proved that jump requests execute and that
the body reaches a normal airborne apex. One observed legal side-face target
reached 2.8813 degrees of tracking error at the apex, so the old stationary
2-degree alignment gate discarded the only usable frame. The current code
therefore permits 4 degrees only while tracking an active airborne jump; it
still requires the exact current crosshair block and face and performs the
normal vanilla `useOnBlock` action. Stationary aiming remains at 2 degrees.

This correction allowed the real body to pass the earlier roof cells, but is
not sufficient for the final two cells. Their selected side-face hit points
remain unreachable from every currently enumerated interior stance, and the
bounded recovery correctly terminates instead of looping or falsely
confirming placement.

Changed files in the active bug chain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Last passing targeted gate:

```text
BuildShelterStepSkillTest: PASS
airborneJumpAimAcceptsTheMeasuredApexTrackingError: PASS
```

Last failing physical gate:

```text
mcai_companion:roof_jump_placement: FAIL / 4.424 min
step 48/50 side-face jump attempts exhausted
final failure: build_shelter_step.no_observed_traversal_stand
```

Immediate next action: add a first-red contract for selecting a reachable
support/hit point for the final roof cells (or a newly observed legal stance),
fix only that geometry/recovery defect, rerun `BuildShelterStepSkillTest`, and
then rerun the physical roof gate. The configured-MiMo zero-human foundation
gate remains blocked on that physical result. Formal M0-M4 statistical gates
remain NOT_RUN.

## 2026-08-01 complete physical roof gate

The final roof chain now passes the independent physical world oracle:

```text
BuildShelterStepSkillTest: PASS
mcai_companion:roof_jump_placement: PASS / 2.181 min
physical structural placements: 55/55
```

Three first-red contracts led to the final correction:

- an interior roof target column is a legal floor stance while that roof cell
  is still open;
- a side-face ray from directly below that opening is a grounded low-angle
  click, because jumping would move the body into the block being placed;
- the future centre-floor light target is not physical collision before the
  light is placed. Only structural plan cells are categorically excluded from
  aiming vantages; current observed collision remains authoritative for
  functional cells.

The failure-only geometry trace confirmed that the final centre stance had
safe feet/head/roof AIR, an unobstructed observed aim line, legal distance and
permissions, but was rejected solely because it also matched the later light
step. After narrowing that exclusion, the body walked to the centre opening
and placed the final roof cell through the normal vanilla interaction path.

Changed files:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Immediate next action: run the exact configured-MiMo, player-style,
zero-human foundation gate. Run the broader local regression only after that
gate passes. Formal M0-M4 statistical gates remain NOT_RUN.
## 2026-08-01 exact MiMo zero-human live-model gate

- Gate: `mcai_companion:real_zero_human_dedicated_server_foundation`
- Command profile: real-time GameTest with `live_model_test=true`, using the saved user model profile and restored secret without printing it.
- Result: **PASS**, `BUILD SUCCESSFUL`, approximately 10.09 minutes of scenario time (10m14s wall time).
- The high-level model issued and completed real decisions for wood collection, crafting, stone tools, food, iron equipment, workstations, shelter materials, and shelter construction.
- Vanilla advancement evidence included Stone Age, Getting an Upgrade, Acquire Hardware, and Isn't It Iron Pick.
- A transient provider/network failure entered the safe recovery path and resumed. One malformed skill-argument response was rejected rather than executed; the next valid decision continued the task.
- Final success did not rely on the model's chat claim: the independent GameTest world oracle verified the completed survival state and returned PASS.
- The preceding physical placement gate `mcai_companion:roof_jump_placement` also passes (55/55 structural placements through the vanilla player action path, approximately 2.181 minutes).

### Next gate

Run the broader local regression suite with `./gradlew check`. Fix the first concrete regression, rerun its focused test, then rerun `check`. Formal M0–M4 hidden-seed/statistical acceptance gates remain `NOT_RUN`.

## 2026-08-01 regression and release-package gate

- `./gradlew check`: **PASS**.
- The first `./gradlew build` correctly failed `verifyReleaseJar`: four newer
  GameTest fixture families were present in both release archives because the
  explicit development-fixture exclusion list had not been extended.
- Fixed `build.gradle` to exclude `BuildingGameTests`,
  `FoundationGameTests`, `ShelterMaterialExplorationGameTests`, and
  `WorkstationPrerequisiteGameTests`, including their nested scenario classes.
- Repeated `./gradlew build`: **PASS**, including `verifyReleaseJar`.
- Produced installable bundled artifact:
  `build/libs/mcai_companion-0.1.3-dev-mc26.2.jar`.
- Release metadata remains Minecraft 26.2, Forge loader `[65,66)` and Forge
  dependency `[65.0.0,66)`.

Next: run the current non-provider physical Forge GameTest batch (or its
smallest supported batch selector) and treat any first failure as the next
implementation target. This packaging result does not change formal M0–M4
status.

## 2026-08-01 current 36-test physical batch

Command: `./gradlew runGameTestServer`

Result: **FAIL**, 33/36 required tests passed in 57.94 seconds. The three
failures were:

1. `headless_player_lifecycle_state_and_fair_action` — inventory was not
   restored through vanilla player data at tick 220.
2. `placement_obstruction_recovery` — the companion did not become active by
   tick 3002.
3. `real_portal_cast_and_light` — the companion body disappeared at tick 761.

All three cross the shared Headless-player lifecycle boundary, while the
new shelter placement scenarios themselves completed. Next action is a focused
rerun of the shortest lifecycle test, followed by the other two if necessary,
to distinguish a standalone production regression from cross-scenario state
leakage before changing code.

## 2026-08-01 anchored spawn and player-simulation correction

The original three failures each passed alone. Batch diagnostics showed that
the vanilla `PrepareSpawnTask` first loaded the companion's remote saved
position before applying a fixture/player anchor. An unthrottled GameTest can
advance thousands of game ticks before that asynchronous remote load receives
enough wall-clock time, causing a reused body to remain pending or fall below
the fixture.

Implemented an anchored production spawn path that resolves a vanilla-safe
position beside the requesting player, asynchronously loads that anchor
neighbourhood, then uses the ordinary player-data and
`PlayerList.placeNewPlayer` lifecycle. Unanchored restart still uses vanilla
`PrepareSpawnTask`. Fixture-owned tests use a release-excluded safe-anchor
helper.

Changed files in that correction:

- `src/main/java/dev/mcai/companion/embodiment/AnchoredPlayerSpawn.java`
- `src/main/java/dev/mcai/companion/embodiment/PendingPlayerSpawn.java`
- `src/main/java/dev/mcai/companion/embodiment/AiPlayerManager.java`
- `src/main/java/dev/mcai/companion/embodiment/GameTestCompanionSpawn.java`
- the fixture-owned physical GameTest classes using that helper
- `build.gradle`

Verification:

```text
compileJava: PASS
check: PASS
auto_presence_on_human_login: PASS
second 36-test batch: 35/36 PASS
```

The prior lifecycle, placement-obstruction and real-portal failures all
disappeared in the second batch. Its sole remaining failure was
`real_nether_blaze_rod_acquisition`. Diagnostics proved a distinct accelerated
cross-dimension race: the AI stood on the ordinary Blaze drop, but the
destination chunk was not yet entity-ticking, so the item remained forever at
`age=0` with vanilla pickup delay active.

The focused Nether fixture now waits for the headless `ServerPlayer`'s own
vanilla ticket to make the destination entity-ticking before it creates the
Blaze. It does not force-load the chunk, shorten pickup delay, or grant the
item. Targeted result:

```text
real_nether_blaze_rod_acquisition: PASS / 1.775 s
drop entityTicking=true; age advanced; vanilla inventory pickup confirmed
```

Immediate next action: rerun the complete 36-test physical batch. If green,
rerun `check` and `build` so the new release-excluded helper and production
spawn path pass the package oracle. Formal M0-M4 hidden-seed/statistical gates
remain NOT_RUN.

## 2026-08-02 full physical batch and order-independent performance gate

The next complete batch first exposed two test-oracle defects rather than a
new production gameplay regression:

1. The lifecycle performance assertion used the runtime lifetime average.
   Forge's GameTest registration order changes between builds, so runs where
   the lifecycle fixture started late included thousands of samples from
   unrelated earlier fixtures and failed at about 1.09 ms. The same physical
   lifecycle chain alone measured about 0.415 ms.
2. The focused Nether fixture treated either 3,000 accelerated ticks or 15
   wall-clock seconds as a timeout. A full unthrottled batch advanced 3,002
   ticks in only 539 ms, before the vanilla asynchronous chunk workers could
   promote the player's destination ticket.

Corrections:

- `RuntimeTickMetrics` now provides a constant-size cursor and an interval
  snapshot. The lifecycle gate measures only the samples produced by its own
  full skill chain; the production limits remain exactly average <= 1 ms and
  rolling p95 <= 2 ms.
- Unit contracts prove that pre-cursor slow samples cannot pollute an
  interval, including intervals shorter than the rolling ring.
- The release-excluded Blaze fixture waits until both its accelerated-tick
  and wall-clock bounds are exceeded, and yields one millisecond while waiting
  so vanilla async chunk workers receive real execution time. It still adds
  no ticket, force-load, direct item, pickup-delay change, or teleport command.

Changed files in this correction:

- `src/main/java/dev/mcai/companion/runtime/RuntimeTickMetrics.java`
- `src/test/java/dev/mcai/companion/runtime/RuntimeTickMetricsTest.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `docs/CODEX_RECOVERY_CHECKPOINT.md`

Verification:

```text
RuntimeTickMetricsTest: PASS
headless_player_lifecycle_state_and_fair_action: PASS
  focused scenario samples=4,646
  average=0.413 ms, rolling p95=0.948 ms
real_nether_blaze_rod_acquisition: PASS
  ordinary Blaze kill, weapon durability and delayed entity pickup verified
complete Forge GameTest batch: PASS, 36/36, 1.826 min
  lifecycle samples=4,577
  average=0.497 ms, rolling p95=1.137 ms
./gradlew check build: PASS
verifyReleaseJar: PASS
bundled artifact: build/libs/mcai_companion-0.1.3-dev-mc26.2.jar
```

The last failed full-batch gate before these corrections was 35/36, solely the
accelerated Nether-ticket timeout at 3,002 ticks / 539 ms. The immediately
following complete batch is fully green. This closes the current anchored
spawn, cross-dimension simulation and performance-oracle bug chain.

Next: continue capability work from the first still-unmet M1-M4 acceptance
item, with the same rule that focused physical/model evidence precedes broad
regression. Formal M0-M4 hidden-seed/statistical acceptance gates remain
`NOT_RUN`; this 36-test pass must not be represented as the two-hour random
Hardcore objective.

## 2026-08-02 real-model causal audit and E2E hardening

The first black-box E2E slice previously accepted only server chat, world
motion, one AI-labelled chat message, and one Observer visibility sample. It
did not prove that the configured external model caused the skill, did not
prove that the skill issued a low-level action, and could stop the Observer
before its final movement frames reached disk.

Implemented:

- `BrainEvent.ModelAudit` with bounded, secret-free stages:
  `ai_perception_received`, `model_request_started`,
  `model_response_received`, `decision_schema_validated`,
  `decision_revision_accepted`, and `skill_started`;
- provider protocol, HTTP status, elapsed time and optional provider request
  ID are retained only for a successfully decoded and validated response;
- `RuntimeActionTrace` binds the first accepted material body command to the
  exact model-started skill and writes one bounded
  `low_level_actions_issued` event per model request;
- `ServerOwnedCoreSkillActuator` audits accepted non-zero movement, jump and
  use commands without allowing audit exceptions to affect legal actions;
- `e2e/orchestrator.py` reads the closed production SQLite database read-only,
  requires the ordered same-request `follow_entity -> move` causal chain, and
  exports `model-audit.jsonl`, `action-trace.jsonl`, and
  `world-events.jsonl`;
- the Observer gate now requires TAB presence plus at least two blocks of
  post-chat non-teleport motion, and the orchestrator waits for that evidence
  before terminating clients;
- API-key-file injection is now represented correctly in the manifest without
  recording the path or secret.

Targeted verification:

```text
BrainEventTest: PASS
BrainOrchestratorTest: PASS
RuntimeActionTraceTest: PASS
e2e.test_orchestrator: PASS (3 tests)
./gradlew check build e2eClientJar e2eOracleJar: PASS
verifyReleaseJar: PASS
```

Exact-JAR, zero-human dedicated-server lifecycle:

```text
runId: 20260802T084330Z-e2eaudit02
status: PASS / NON_RELEASE
product SHA-256:
  a394cd2d477363a508abe5ffe69e59e8f8b6fb31713eb5d117c5a85868cf0233
AI ServerPlayer joined without humans: PASS
SQLite Jar-in-Jar load: PASS
clean shutdown and player removal: PASS
functionalAiClaim: false
```

Real configured-model inner-loop follow:

```text
model: mimo-v2.5
result: PASS / 1 required GameTest / 9.146 s
accepted decision: START_SKILL follow_entity
model response: Responses HTTP 200 / 3,137 ms
requestId: brain-2-1
ordered SQLite stages: 7/7 present
first material action: move / QUEUED / server tick 87
```

The API key came from the platform secure store and was never printed or
written to the repository. This live-model run is still an inner-loop
GameTest. The real Actor/Observer functional gate remains `NOT_RUN`: the
current macOS host has no Docker, Podman, or configured Linux/Xvfb worker.
Formal M0-M4 status remains `NOT_RUN`.

Next: make the native-Linux functional runner reproducible in CI/container
form, then execute the exact-JAR Actor + Observer slice on a Linux/Xvfb worker.
The next functional expansion after movement is an independently scored
ordinary inventory transaction; do not promote M0 or M1 before both external
client evidence and mutation gates exist.

## 2026-08-02 Forge 65.1.0 full physical compatibility batch

The complete 36-test physical Forge batch was rerun with the current official
65.1.0 runtime:

```text
command: ./gradlew runGameTestServer -Pforge_compile_version=65.1.0 --no-daemon
result: PASS / 36 of 36 required tests / 1.542 min
product artifact SHA-256:
  a394cd2d477363a508abe5ffe69e59e8f8b6fb31713eb5d117c5a85868cf0233
```

This closes the current Forge-patch inner-loop regression check. It does not
promote the formal Forge patch matrix, M0, or any M1-M4 gate: the formal
matrix still requires the exact release JAR plus real external clients and
archived run evidence.

Current blocking environment fact: this host is macOS and has no Docker,
Podman, or configured Linux/Xvfb worker. The next implementation step is a
checked-in Linux CI entry point that runs the existing exact-JAR
Actor + Observer + configured-model functional harness and always archives
its manifest, verifier result, client observations, model causal audit, and
logs. After that, expand the functional slice with an independently scored
ordinary inventory transaction. Formal M0-M4 status remains `NOT_RUN`.

## 2026-08-02 Linux functional entry point and live-model inventory slice

### Bug root and implementation

The previous external-client functional slice proved only one chat-to-follow
movement. It could not distinguish a companion capable of a real inventory
transaction from one that merely moved and spoke, and this macOS host could
not run the required Linux/Xvfb Actor + Observer topology. The functional
orchestrator also inherited model credentials into build, smoke-server and
client processes even though only the functional AI server should receive
them.

Implemented:

- `.github/workflows/real-client-functional-e2e.yml` provides a manually
  dispatched Ubuntu 24.04 / Java 25 / Xvfb entry point for Forge 65.0.0 and
  65.1.0, with pinned action commits, bounded execution, secret-only API-key
  injection, unconditional evidence upload, and saved-exit-code enforcement;
- `e2e/orchestrator.py` now strips all supported model credential sources
  from builds, smoke servers and Actor/Observer clients, and exposes them only
  to the functional AI server;
- the formal scenario is now `real_client_chat_follow_inventory`: an Actor
  sends two ordinary chat messages, first requesting follow and then pickup
  of a currently visible dropped item;
- the independent Oracle creates an ordinary `ItemEntity` before the chat,
  then observes only server-authoritative movement, entity removal and exact
  inventory delta after the command;
- verification requires two distinct ordered production causal chains:
  `follow_entity -> move` and `collect_observed_item -> move`, plus two Actor
  chat messages, post-inventory AI speech, non-teleport approach and an exact
  three-log inventory increase;
- `MinecraftPlannerInputFactory` supplies a generic fair-observation pickup
  playbook. The model must still select the skill and must copy the exact
  current semantic-observation identifiers; no hidden coordinate, item count,
  NBT or direct inventory mutation is exposed;
- `LiveModelChatGameTests.LiveItemCollectionScenario` adds an inner-loop,
  configured-model physical gate using a normal logged-in player's chat,
  followed by zero humans, a model-selected skill, real movement and vanilla
  collision pickup.

Primary changed files:

```text
.github/workflows/real-client-functional-e2e.yml
e2e/orchestrator.py
e2e/ci_evidence_summary.py
e2e/test_orchestrator.py
e2e/test_ci_evidence_summary.py
e2e/README.md
src/e2eClient/java/dev/mcai/e2e/client/McaiE2eClientMod.java
src/e2eOracle/java/dev/mcai/e2e/oracle/McaiE2eOracleMod.java
src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java
src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java
src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java
src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java
src/main/resources/data/mcai_companion/test_environment/exclusive_live_model_item_collection.json
src/main/resources/data/mcai_companion/test_instance/real_player_task_to_live_model_item_collection.json
```

### Targeted evidence

```text
actionlint 1.7.12: PASS / 0 workflow errors
Python E2E unit tests: PASS / 8 tests
compileE2eClientJava + compileE2eOracleJava: PASS
focused MinecraftPlannerInputFactoryTest: PASS
verifyReleaseJar: PASS

exact-JAR zero-human smoke run:
  runId: 20260802T091034Z-e2einventorysmoke
  Forge: 65.1.0
  status: PASS / NON_RELEASE / functionalAiClaim=false
  product and loaded JAR SHA-256:
    4e8f8306105cc78518c2c5d606a9688b5aacff4b107c16c1d509249738133d6a
  model credential present in smoke process: false

real configured mimo-v2.5 item-collection GameTest:
  result: PASS / 1 required test / 9.952 s
  player input: normal server chat
  player presence after goal submission: zero humans
  model response: Responses HTTP 200 / 6,437 ms
  requestId: brain-1-1
  usage: 6,995 input + 163 output = 7,158 tokens
  decision: START_SKILL collect_observed_item
  material action: move / QUEUED / server tick 158
  completion condition: ordinary dropped ItemEntity removed and at least
    three oak logs owned after at least four blocks of physical approach
```

SQLite records the ordered `brain-1-1` stages from perception through HTTP
response, schema and revision acceptance, skill start and low-level move.
The test fixture creates terrain and the ordinary dropped entity before
player chat, but it does not move the AI after the command or write the AI
inventory. Passing requires the production body to reach the entity and the
vanilla pickup path to transfer it.

### Last failed gate and next step

There is no remaining focused failure in the new pickup GameTest. The last
blocked formal gate is still the native-Linux exact-JAR Actor + Observer run:
it is **NOT_RUN**, not failed, because this macOS host has no compatible
Linux/Xvfb runtime. The checked-in CI workflow is statically verified but has
not yet been executed on GitHub infrastructure.

Next: add a local deterministic verifier test for the complete two-request
artifact bundle, rerun the focused E2E/package gates after any correction,
then continue the next unmet M1 physical transaction. Once a Linux worker is
available, execute the manual workflow and archive its verdict before
promoting any formal movement or inventory gate. M0-M4 remain `NOT_RUN`.

## 2026-08-02 live-model container transaction and causal audit

### Root cause and correction

The first real configured-model container-withdrawal run opened the physical
chest, but every subsequent `transfer_menu_item` start failed with
`menu.observation_expired`. The language-model round trip took several
seconds, while the semantic sampler continued incrementing `sampleSequence`.
`ServerMenuSkillActuator` incorrectly required the response to bind the newest
sample instead of the exact fair menu frame that the model actually received.

Corrected without permitting blind or stale clicks:

- `ServerMenuSkillFrameSource` retains a bounded 512-frame history for the
  current menu session and clears it when the menu closes or the body/session
  disappears;
- the actuator resolves the exact model-observed frame, then independently
  revalidates the live session generation, UUID, dimension, container ID,
  state ID, menu class/layout, carried stack, and every observed slot before
  any vanilla click;
- `SmeltMenuBatchSkill` uses the retained start frame but continues checking
  the current live frame while cooking;
- the physical menu transaction GameTest now publishes a newer sample before
  executing a transfer bound to the older retained frame, simulating ordinary
  model latency.

### Changed files

```text
src/main/java/dev/mcai/companion/skills/menu/MenuSkillFrameSource.java
src/main/java/dev/mcai/companion/skills/menu/ServerMenuSkillFrameSource.java
src/main/java/dev/mcai/companion/skills/menu/ServerMenuSkillActuator.java
src/main/java/dev/mcai/companion/skills/menu/SmeltMenuBatchSkill.java
src/main/java/dev/mcai/companion/skills/menu/MenuGameTests.java
src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java
src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java
src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java
src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java
src/main/resources/data/mcai_companion/test_environment/exclusive_live_model_container_withdrawal.json
src/main/resources/data/mcai_companion/test_instance/real_player_task_to_live_model_container_withdrawal.json
```

### Targeted evidence and last gate

```text
headless_player_lifecycle_state_and_fair_action:
  PASS / 1 required physical GameTest / 27.18 s

real mimo-v2.5 player-chat container withdrawal after root fix:
  PASS / 1 required physical GameTest / 8.956 s
  ordinary player chat, then zero humans
  model-selected use_block followed by transfer_menu_item
  physical result: chest -3 oak planks, AI inventory +3, empty cursor

second real run after causal-audit wiring:
  PASS / 1 required physical GameTest / 14.81 s
  two incomplete menu decisions were rejected locally
  third decision supplied the complete binding and succeeded

SQLite causal evidence:
  brain-1-1 -> use_block -> use_on_block / DISPATCHED
  brain-1-4 -> transfer_menu_item -> transfer_menu_item / COMPLETED

focused JVM tests:
  RuntimeActionTraceTest + SmeltMenuBatchSkillTest: PASS
```

Interaction and menu actuators now emit authority-free accepted-action
evidence into the same bounded per-skill causal trace as movement. Audit sink
failures are swallowed after the legal action and cannot influence gameplay.

The last formal blocker is unchanged: the native-Linux exact release-JAR
Actor + Observer functional workflow is `NOT_RUN` because this macOS host has
no Docker, Podman, or configured Linux/Xvfb worker. The current source now
registers 37 physical GameTests; the complete 37-test Forge 65.0/65.1 batches
have not been rerun, so the earlier 36/36 results must not be reported as a
current full batch. M0-M4 remain `NOT_RUN`.

Next: canonicalize only the three server-authored menu binding tokens from the
exact observation supplied to the model so a correct model-selected
source/destination/count does not incur seconds of retry merely for omitting
opaque IDs. Then rerun focused model, E2E verifier, package, and compatibility
gates. This must not infer source/destination/count or weaken live menu
revalidation.

## 2026-08-02 safe-idle abandonment fix and full controlled M1 chain

The exact real-time, configured-MiMo, zero-human foundation gate initially
failed after 1.086 minutes. The body had already gathered/crafted wood and
stone and killed a cow for three beef, but the model selected the registered
`safe_idle` skill merely “to refresh view”. `SafeIdleSkill` correctly
quiesced the body and returned a safe-idle result; `BrainOrchestrator`
therefore permanently terminalized the healthy multi-stage foundation goal as
`skill_safe_idle`.

Root correction:

- keep `safe_idle` registered as an internal body-quiescence primitive;
- remove it from every model-visible function schema, including the
  foundation utility allow-list;
- tell the model explicitly that the `SAFE_IDLE` decision permanently ends
  the current goal and must never be used as a pause, wait, camera refresh, or
  ordinary recovery step;
- direct refresh/recovery to `REPLAN + SEMANTIC_REFRESH` or an admitted
  first-person survey instead.

Focused `MinecraftPlannerInputFactoryTest`,
`CoreSkillsRegistrationTest`, and `BrainOrchestratorTest` passed after the
change.

The identical live gate then passed:

```text
selector:
  mcai_companion:real_zero_human_dedicated_server_foundation
result:
  PASS / 1 required GameTest / 9.906 min
provider:
  mimo-v2.5 / Responses
human players after start:
  zero for the entire run
model requests:
  23
token usage:
  163,730 input + 2,958 output = 166,688 total
```

Server-authoritative stages completed in order:

```text
visible oak gathering and vanilla pickup
crafting table + wooden pickaxe
three cobblestone + stone pickaxe
eight safe food reserve
seven raw iron + ordinary furnace cooking
iron pickaxe + bucket + shield
crafting table + furnace + chest
ordinary menu deposit with supplies retained in the chest
dynamic material preparation
terrain/workstation-aware shelter relocation
55-step sealed compact shelter with door and light
crossing into the next Overworld day
server-verified COMPLETE_GOAL
```

Five model-authorized `build_shelter_step` batches continued the same
checkpointed generated plan. Failed right-clicks did not consume material or
count as completed placement; the local controller changed observed stance,
walked through the door, used exterior apron paths, and reverified all
remaining roof cells.

This is strong controlled inner-loop evidence, not the formal M1 result. The
fixture supplies visible resource clusters/animals and the official
`100`-unseen-Hardcore-seed evaluation remains `NOT_RUN`. It also does not
replace the native-Linux exact-JAR Actor + Observer gate.

Observed efficiency defect, not a terminal failure: after the iron stage the
model made several irrelevant/incomplete
`engage_and_collect_observed_drop` calls without `expectedItemId`; the strict
validator rejected every one, and the model later selected the correct
workstation compound. Next, narrow the model function schema for advanced
foundation phases to only the current compound plus bounded navigation,
survey, gathering, pickup and food utilities. Do not infer a drop type for
the model.

## 2026-08-02 provider-noise canonicalization and Forge patch rerun

The controlled foundation pass exposed two provider-efficiency defects after
the gameplay goal was already healthy:

- advanced foundation phases still advertised unrelated combat/drop skills,
  inviting incomplete `engage_and_collect_observed_drop` calls;
- MiMo sometimes copied the observation's opaque `sampleSequence` into
  `survey_surroundings`, although that skill has no sample-bound authority.

Corrections remain fail-closed:

- advanced foundation schemas now expose only the current compound plus the
  bounded navigation, survey, visible gathering/pickup, and food utilities;
- `KnownSkillArgumentCanonicalizer` removes exactly
  `survey_surroundings.sampleSequence`;
- every other unknown field, every missing required argument, and
  `sampleSequence` on every other skill still reaches strict validation.

Changed files:

```text
src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java
src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java
src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java
src/main/java/dev/mcai/companion/model/JdkModelGateway.java
src/test/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizerTest.java
```

Current targeted and compatibility evidence:

```text
focused model/planner tests:
  PASS
Gradle check + release-JAR audit:
  PASS
E2E evidence parser/verifier unit tests:
  PASS / 9 of 9
Forge 65.0.0 full physical batch:
  PASS / 38 required of 38 / 1.407 min GameTests
Forge 65.1.0 full physical batch:
  PASS / 38 required of 38 / 1.627 min GameTests
MCAI functions registered in each batch:
  37
```

The extra required test is Forge's batch accounting; 37 functions belong to
this mod. These are source-tree inner-loop compatibility results and do not
promote the formal `forge65PatchMatrix`, because that gate requires archived
exact release-JAR artifacts under the formal protocol. M0-M4 remain
`NOT_RUN`.

Last failed formal gate: none was newly executed. The native-Linux exact-JAR
Actor + Observer workflow remains `NOT_RUN` on this macOS-only host.

Next: rerun the real configured-MiMo, zero-human controlled foundation chain
with the narrowed schema and compare request count, rejected decisions, and
token use. Then continue an unmet natural M2 capability; do not represent
controlled fixtures as unseen-seed completion.

## 2026-08-02 live foundation material-routing regression

The post-schema-change real configured-MiMo rerun failed honestly:

```text
gate:
  mcai_companion:real_zero_human_dedicated_server_foundation
result:
  FAIL / tick 13,689 / 11.41 min
failed stage:
  SHELTER_MATERIALS
authoritative inventory:
  oak_planks=2, oak_doors=0, torches=0
active skill at timeout:
  travel_to
```

The body legally completed visible wood, basic crafting, stone tools, food,
seven raw iron plus furnace cooking, iron pickaxe/bucket/shield, and the
workstation/storage boundary. It then gathered eighteen additional logs, but
never chose `prepare_foundation_shelter_materials`; instead it repeatedly used
the simultaneously advertised survey/move/gather utilities and never
converted the logs into structural planks, a door, and torches.

Root cause: the advanced foundation admission boundary still exposed
micro-action utilities beside the one server-selected compound. That compound
already owns bounded first-person scanning, exploration, gathering, vanilla
recipes, furnace/menu use, door crafting, and torch crafting. Advertising
both levels allowed the high-level provider to bypass the durable state
machine and recreate a fragile open loop.

Two smaller provider-noise failures were also observed:

- `hunt_observed_food_animal` received an extra `dimension`;
- `collect_observed_item` received an extra `dimension` twice.

Both skills bind authority to an exact observed entity/item frame and do not
accept model-authored dimension arguments.

Next correction:

- for advanced M1 phases expose only the current server-verified compound;
- for `SECURE_FOOD_RESERVE` expose only its full local reserve compound;
- narrowly strip only the authority-free `dimension` field from the two
  exact-observation skills above while preserving every other unknown field
  for strict rejection;
- rerun focused planner/model tests, then the exact real-MiMo failed gate.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-10 latest recovery: potato regression isolated and cleared

The first potato-specific physical run after the strict replant proof exposed
one missing age-zero crop at final world verification (`run251`).  The failure
was not promoted as a pass.  I added a test-only per-`use_on_block` crop-state
trace to `FarmingGameTests.MaintenanceScenario`, without adding any world read
or mutation to production code, and reran the exact selector (`run252`).

```text
run251: FAIL / 7 of 8 final potato replants; missing target was reported
run252: PASS / All 1 required tests passed; final world and action counts valid
run253: PASS / All 1 required tests passed for the beetroot variant
run254: PASS / All 1 required tests passed for the asymmetric 15-cell wheat field
```

This closes the currently reproducible potato fixture, but it is still a
controlled no-model Forge GameTest.  It is not evidence for live-provider
chat-to-action, a real offscreen client, Hardcore completion, M1-M4, or the
two-hour random-seed target.  Those gates remain `NOT_RUN` until their real
infrastructure is executed.

## 2026-08-10 follow-up: axial field steps and strict replant proof

Additional directed runs after the previous checkpoint:

```text
run247: FAIL / substrate approach clipped the central-water corner;
        body entered a vertical water/fall oscillation
run248: FAIL / diagonal corner issue removed, but an axial step stalled at
        the wheat collision edge; no completed pickup progress
run249: PASS / `All 1 required tests passed`, strict replant proof and
        axial one-cell movement, with collision/water trace enabled
```

The movement point now preserves the non-moving horizontal coordinate for a
cardinal step, preventing a diagonal hitbox cut across a liquid corner.  The
test-only trace records ground, horizontal collision, water state and velocity
for any future physical failure.  These are still release-excluded,
no-model, controlled Forge fixtures; they provide useful body/action evidence
but do not constitute live-MiMo or random-seed Hardcore evidence.  Formal
M0-M4 and the user-facing AI/gameplay claims remain `NOT_RUN`.

## 2026-08-10 current recovery: physical crop-maintenance gate closed

This turn continued the controlled Forge 65.1.0 GameTest inner loop after
run234 exposed a real movement failure.  The prior implementation could
rotate/pitch without advancing when a freshly harvested drop was behind a
crop row, could treat a surveyed farmland support as missing, and could
reselect a one-cell route on every semantic revision.  It also treated seed
consumption as final replant proof, which allowed one server run to finish with
an actual air block.

Corrections now in the working tree:

- `HarvestAndReplantStepSkill` keeps pickup/search/plant observations
  horizontal, limits support fallbacks to parent-survey-authorized field cells,
  treats unknown frontiers as detours without authorizing them, and selects a
  verified safe fallback when no cardinal shortens the drop distance;
- pickup and substrate approach steps are bounded atomic one-cell motions with
  progress/stall detection, so semantic refresh cannot split a step into a
  headless yaw loop; water still enters the existing local escape path;
- substrate approach walks to an authorized field cell when an adjacent crop
  occludes the target farmland face;
- after a planting packet consumes a seed, completion now requires a fresh
  age-zero crop face from semantic vision or the current crosshair.  If the
  crosshair still proves the exact substrate, a maximum of two normal vanilla
  retries is allowed;
- `MaintainObservedCropFieldSkill` records the stronger child transaction
  proof and no longer performs an unbounded final route walk; its final phase
  remains a bounded settlement/output check;
- test-only position/block/drop diagnostics were retained in
  `FarmingGameTests`; `MutableFrames` now models a fair crosshair sample.

Directed evidence:

```text
run234: FAIL / pickup edge, repeated pitch/inspection oscillation
run235: FAIL / stale AIR support on an authorized crop corridor
run236: FAIL / missing-support frontier froze directional scorer
run237: FAIL / target substrate occluded by neighboring crop
run238: FAIL / same substrate visibility gap (diagnostics confirmed it)
run239: FAIL / safe pickup detour stalled at field edge
run240: FAIL / fallback route reached a water-edge/high-step transition
run241: FAIL / cached pickup direction changed at the edge
run242: FAIL / 14/15 physical replants; seed-count proof was too weak
run243: FAIL / one-cell pickup remained stalled without progress detection
run244: FAIL / substrate approach repeated a water detour at a corner
run245: FAIL / strict final re-walk hit `verification_unreachable`
run246: PASS / `All 1 required tests passed`, 15/15 harvest/replant/collect
```

Focused farming unit tests pass after the current changes.  `run246` is still
release-excluded, no-model, deterministic fixture evidence; it is not a live
provider test and does not promote M1-M4.  The remaining immediate task is one
consecutive physical run (`run247`) plus a compact non-wheat crop matrix, then
the broader JVM/release gates.  Formal M0-M4, real-model Actor/Observer,
random-seed Hardcore, long-soak, and two-hour completion gates remain
`NOT_RUN`.

## 2026-08-10 current recovery: bounded crop pickup detour

The focused JVM farming, navigation, planner, and movement contracts pass
after the latest pickup correction.  The most recent physical run before that
correction was `run-debug219-gate.log`, a retained Forge 65.1.0 controlled
inner-loop failure at 5/15 transactions.  The server had real vanilla wheat,
wheat-seed, and replant actions in its audit, but a scattered wheat drop was
still nearby when the bounded pickup child timed out.  The diagnostic showed a
single safe cardinal candidate and fresh first-person body/head/support facts;
the previous direction filter rejected the necessary side-step because it was
not sufficiently aligned with the drop.

Current correction:

- `HarvestAndReplantStepSkill.safestFieldStep` distinguishes missing/unknown
  forward evidence from a recently observed blocked or unsafe destination;
  only the latter permits a zero-dot side detour, while unknown terrain still
  requires an observation refresh.
- The pickup path remains first-person, cardinal, bounded, and vanilla-safe;
  no entity scan, direct item insertion, teleport, or hidden block read was
  added.  The detour is still limited to the exact atomic crop transaction.
- The existing body-contact support exception remains limited to fresh facts
  from the same traversal and to pickup only; it does not authorize ordinary
  navigation or construction.

Directed evidence:

```text
focused farming/navigation/planner/movement tests: PASS
run-debug214: PASS / 15 of 15 expanded-field transactions
run-debug215..219: FAIL, retained for regression diagnosis (movement/pickup)
run-debug220: pending after the bounded-detour correction
```

The expanded fixture is not a random seed, not Hardcore, and not model-backed
gameplay.  It cannot promote M1 or any M2-M4 claim.  Formal M0-M4, exact-JAR
Linux Actor/Observer, real-provider end-to-end, hidden-seed, soak, and
two-hour completion gates remain `NOT_RUN` until their declared evidence is
collected.

## 2026-08-10 expanded crop-maintenance pickup checkpoint

Current critical bug and evidence:

- `run-debug207-gate.log` is a retained `FAIL`, not a promoted gate: the
  release-excluded Forge 65.1.0 expanded 4x4 field scenario completed 11 of
  15 harvest/replant transactions and then failed to collect one vanilla
  drop (`harvest_drop_not_collected`).
- The body/head cells toward the drop were current and clear. The intervening
  farmland support carried direct `BODY_CONTACT` evidence from revision 389,
  but its visual top affordance was `UNKNOWN` because the replanted seedling
  occluded that top face. Revision 418 therefore rejected every pickup step
  even though the companion had physically stood on the needed field cell
  about seven seconds earlier.
- Earlier `run-debug192` remains the performance/root-cause failure. The
  rolling-map and retained-snapshot corrections removed its chronic
  `Can't keep up` behavior; runs 193–207 remain preserved physical failures
  used to narrow transaction safety rather than being hidden or called PASS.

Production files changed on this correction path include:

- `navigation/NavigationEvidence.java`
- `skills/farming/HarvestAndReplantStepSkill.java`
- `perception/BlockShapeAffordances.java`
- both first-person perception samplers and navigation mapping/support types
- `navigation/LocalNavSnapshot.java`
- `skills/automation/MechanismSiteSurveyAccumulator.java`
- `runtime/ServerShelterFrameSource.java`
- the crop-maintenance planner/skill and corresponding focused tests

Latest unverified correction:

- crop pickup may consume only a short (40 semantic revisions) direct
  `BODY_CONTACT` support fact, and only after current body/head traversal
  clearance has also passed;
- this does not authorize unknown terrain, liquids, generic partial blocks,
  or stale support memory.

Next exact gates:

1. run the focused `NavigationEvidenceTest` and farming skill tests;
2. rerun only `mcai_companion:real_maintain_observed_expanded_field` on
   Forge 65.1.0;
3. if it passes, repeat the exact physical gate once before compact/multi-crop
   regression and package verification;
4. keep all formal real-client/model, hidden-seed, endurance, and M0–M4 gates
   `NOT_RUN` until their declared external evidence exists.

## 2026-08-07 continuation checkpoint: isolated real-skill batch defects

The first full no-model Forge 65.0.1 inner-loop batch was repeated after
separating every singleton-body GameTest environment.  The previous
`already_active`/body-disappeared failures were an environment collision and
are no longer counted as product evidence.  The fresh batch completed 60
required tests with 54 passes and 6 required failures; no server crash,
fixture collision, or process hang occurred.

The remaining failures are genuine skill-level evidence: ender-pearl pickup
rejects a controlled hostile-proximity risk while the body is under its
verified two-block shelter; blaze-drop pickup can lose a visible stack because
the body has no safe forward input when a cardinal route proof is not yet
available; wood prerequisite/exploration does not yet make physical progress;
roof-jump placement has no observed traversal stand; verified-portal return
has no route evidence; and one ender-pearl reserve run entered a hardcore
`move_to.hardcore_danger` rejection.  These are not promoted to passes.

Files changed in this slice:

- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- five `real_*` test-instance resources and five exclusive test-environment
  resources under `src/main/resources/data/mcai_companion/`

The last exact isolated runs were `real_sheltered_ender_pearl_acquisition`
PASS 1/1 and `real_nether_blaze_material_reserve` FAIL 1/1 with
`collect_observed_item.item_lost_without_pickup` after the visible stack was
within roughly three blocks.  The next correction is to thread a narrowly
scoped shelter-risk authorization through the ender-pearl collection child
and add a bounded, first-person-visible nearby advance for ordinary drops.
After that, the exact pearl/blaze/wood tests will be rerun before any package
or formal status update.

Formal M0-M4, model-backed gameplay, unseen-seed Hardcore, and professional
companion claims remain `NOT_RUN`.

## 2026-08-05 stronghold wall-entry recovery

Current root cause:

- the live MiMo chain legitimately triangulated the stronghold and walked
  roughly 311 blocks, but the bounded descending tunnel reached the safe
  minimum depth below the observed masonry;
- the first horizontal probe could therefore pass underneath the wall, and
  a resumed task repeatedly returned `travel_to.route_unknown` without
  acquiring an interior stronghold frontier;
- the earlier focused fixture did not reproduce the production wall's
  vertical offset, so its PASS was not homologous to the live failure.

Implemented correction:

- add ordinary, collision-aware `ASCENDING` excavation alongside horizontal
  and descending modes;
- at the safe-depth boundary, probe radially outward one supported ascending
  step at a time;
- after contacting masonry, enter horizontally; if no interior frontier is
  observed, retreat through the opened corridor, climb one block alongside
  the wall, and retry, with a bounded 12-height search;
- resume the underground phase after restart instead of returning to the
  surface route;
- update the physical fixture so the first probe begins two blocks below the
  wall, matching the live chain.

Files changed for this correction:

- `src/main/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/SearchObservedStrongholdPortalRoomSkill.java`
- `src/main/java/dev/mcai/companion/skills/mining/ExcavateSafeTunnelSkill.java`
- `src/main/java/dev/mcai/companion/skills/mining/TunnelMode.java`
- `src/main/java/dev/mcai/companion/skills/mining/MiningSkills.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- focused mining/stronghold tests under `src/test/java/dev/mcai/companion/skills/`

Latest gates:

```text
focused mining + stronghold unit contracts: PASS
previous real MiMo continuous chain:
  FAIL / safe-depth probe travelled below the wall
homologous physical selector progression:
  FAIL / 2.384 min / second ascent alternated onto excavated head cell
  FAIL / 2.397 min / rising sightline occluded by current ceiling
  FAIL / 3.955 min / false completion inside room floor layer
  FAIL / 4.957 min / one-way staircase drifted beyond finite room
  FAIL / 2x2 spiral crossed the older descending corridor
  FAIL / 2.777 min / supported switchback retreat over-counted 3
         crossed blocks as 4 because of floating-point ceil()
latest focused mining + stronghold unit contracts:
  PASS / integer block-projected retreat correction
latest physical rerun:
  FAIL / 3.630 min / reached the correct supported room-feet layer
         at Y=-46, but simultaneous floor/frontier ray proof was
         impossible; the search climbed past it and later failed a
         correctly rejected occluded torch placement at Y=-45
latest floor-then-frontier verification contracts:
  PASS
first physical rerun of split verification:
  FAIL / 2.164 min / low probe correctly stood on ordinary stone,
         but the first split-proof implementation incorrectly required
         stronghold masonry before allowing any frontier scan
latest separated safe-floor/stronghold-floor contracts:
  PASS
latest homologous physical selector:
  PASS / 1 required test / 3.420 min / real-time 20 TPS
  body physically traversed about 96 blocks, descended, consumed ordinary
  pickaxe durability and owned torches, completed the bounded switchback,
  and handed off on the supported stronghold room-feet layer at Y=-46
```

The active correction keeps the support checks strict, clears each current
ceiling before a rising jump, requires visible stronghold support directly
under the body before handoff, and uses a two-cell-wide switchback outside
the wall. Entry retreat distance is now derived from the integer projection
between the recorded start-feet block and current-feet block. It no longer
uses `ceil()` on drifting physical body centres, which could make a legal
three-block probe retreat one unverified extra block onto a cleared stair
cell. Stronghold entry verification now records a first-person sturdy-floor
proof for the stationary feet block before scanning the adjacent interior;
it no longer requires a single ray frame to prove both the floor directly
below the eyes and a forward room frontier. The proof has two independent
facts: any visibly sturdy floor permits the scan to continue, while only a
visibly sturdy stronghold floor permits final handoff.

Next:

1. rerun the focused physical selector through its underground entry loop;
2. rerun the exact real-MiMo continuous chain from crafting Eyes through
   dragon return;
3. if it fails, use its persisted phase/position and physical logs to make
   the next bounded correction before spending another provider run.

This is focused inner-loop evidence. Exact-client/dedicated-server,
random-unseen Hardcore, soak, performance, and two-hour M1-M4 gates remain
`NOT_RUN`.

## 2026-08-05 continuous Nether-material-to-victory gate: fixture FAIL

The first real-`mimo-v2.5` run of
`real_player_task_to_live_model_nether_materials_to_victory` failed honestly
after 7.708 game-test minutes. The single player-chat goal and same embodied
UUID completed:

```text
craft_recipe -> return_via_verified_portal
-> triangulate_stronghold_search_area -> reach_observed_stronghold
```

The reach verifier then rejected the handoff. At failure the body was still
at Y=-38, about 42.7 horizontal blocks from the triangulated target, with
zero iron-pickaxe damage and all 32 torches intact. The controlled portal
maze had exposed its `stone_bricks` roof at Y=-36, so fair first-person
perception treated that roof as stronghold evidence before any excavation.

Files currently changed:

- `src/main/java/dev/mcai/companion/communication/LiveModelChatGameTests.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_environment/exclusive_live_model_nether_materials_to_victory.json`
- `src/main/resources/data/mcai_companion/test_instance/real_player_task_to_live_model_nether_materials_to_victory.json`
- this recovery checkpoint

Latest failed gate:

```text
FAIL / 1 required test / 7.708 min
assertion: Live stronghold reach lacked physical travel, excavation,
lighting, or preserved visible evidence
```

Next: replace only the test maze's exposed brick roof with ordinary stone,
retain the buried brick evidence and all physical travel/excavation/lighting
assertions, compile, then rerun only this exact real-model gate.

This is a controlled inner-loop fixture failure. Formal M0-M4, external
exact-JAR Actor/Observer, unseen-seed, soak, and two-hour gates remain
`NOT_RUN`.

### Follow-up run: exposed chamber floor FAIL

Replacing the corridor roof with ordinary stone removed the first false
positive: during approach the body reached 90.7 blocks with
`observedStrongholdBlock=null` and zero travel recoveries. The exact rerun
still failed after 7.429 minutes, this time at the edge of the target:

```text
body=(-314.893,-38.0,-1718.502)
target=(-304,-1728)
pickaxeDamage=0
torches=32
eyes=13
```

The central 15-by-15 chamber itself had been carved from Y=-40 through Y=-37,
overwriting the Y=-39 approach floor. From the edge, fair first-person
perception could look into that open pit and see its Y=-41 stone-brick floor.
The corridor-roof correction was therefore valid but insufficient.

Current correction moves the chamber four blocks deeper. The normal
smooth-stone surface and an ordinary-stone chamber roof remain intact, while
the existing descending excavation can open the chamber through legal block
breaking. A fixture assertion now rejects any setup that overwrites either
seal. The next gate is the same single real-model test; no production
perception, reach, excavation, or verification rule was relaxed.

### Third run: genuine excavation reached an unsupported cavity

The third real-provider run proved that the surface and roof seals work:
`observedStrongholdBlock` stayed null at 237, 132, and 20.5 blocks from the
intersection. The first `reach_observed_stronghold` then began its ordinary
descending excavation, but failed with:

```text
excavate_safe_tunnel.unsafe_support
```

The body had moved from surface Y=-38 to Y=-39. Its next descending support
cell was inside the test chamber's air volume, so the mining safety boundary
correctly refused to walk over it. Subsequent model requests selected
`reach_observed_stronghold` again, but the one-block-lower body could not
reconstruct an approach route and failed closed with
`travel_to.route_unknown`. The run was intentionally stopped after the same
causal failure repeated; it is neither a pass nor a formal result.

This also exposes a product-level integration gap: reach currently completes
on visible stronghold masonry, while portal-room DFS can only traverse
already accessible cells. A real buried stronghold handoff needs a legal,
support-checked wall entry instead of relying on an open test chamber.

Next:

1. keep the portal maze below a fully supported descending route;
2. add a first-person, ordinary-mining wall-entry handoff that preserves
   adjacent masonry evidence;
3. cover that handoff with a focused physical gate before paying for another
   full real-provider run;
4. rerun only the continuous real-model chain afterward.

### Supported wall-entry implementation and physical gate: PASS

`reach_observed_stronghold` now distinguishes accessible masonry under the
body from a side wall. A side wall no longer produces immediate success. The
compound:

1. retains the first-person masonry observation;
2. aligns to its currently visible support;
3. chooses the dominant cardinal bearing and horizontal/descending mode;
4. runs at most six ordinary two-block-high mining steps with the owned
   pickaxe and torch;
5. requires body displacement, stable ground, and fresh surviving masonry
   evidence before completion.

Focused gates:

```text
ReachObservedStrongholdSkillTest:
  PASS

mcai_companion:real_stronghold_reach:
  PASS / 1 required test / 1.468 min / real-time
  horizontal approach: about 96 blocks
  final body: one block beyond the masonry plane at Y=-46
  iron-pickaxe damage: 0 -> 14
  owned torches: 32 -> 30
  adjacent fixed masonry evidence: preserved
```

The continuous fixture now leaves the measured centre solid. Its receiving
room begins one block beyond the original east masonry wall at the physically
observed handoff depth, with a normal stone roof and a supported brick floor.
The hidden two-turn portal corridor remains pre-command and first-person
occluded. Focused unit compilation passes.

Next exact gate: rerun only
`real_player_task_to_live_model_nether_materials_to_victory` with the saved
real provider and require the same body to continue from wall entry through
portal-room DFS, twelve Eye placements, End entry, dragon combat, and the
central return portal. This remains a controlled inner-loop gate; formal
M0-M4 remain `NOT_RUN`.

## 2026-08-02 active gate: observed-stronghold approach mining rejection

The new `real_stronghold_reach` controlled Forge GameTest is the current
focused gate. The production body already:

- consumes the goal-scoped measured Eye-ray intersection;
- walks more than 80 blocks through the ordinary moving-player chunk window;
- enters the bounded safe-tunnel compound;
- consumes one torch through the normal use-on-block path.

The latest physical run failed honestly at the first underground excavation:

```text
FAIL: excavate_safe_tunnel.mining_world_denied
last observed feet: [-206,-42,-296]
last live crosshair: current support block [-206,-43,-296]
```

Two earlier defects in this chain are already corrected and covered by
directed JVM tests:

- fresh multi-ray traversal AIR is accepted as observed clear instead of
  being selected for mining;
- a stale 4 Hz semantic face can guide turning but cannot authorize a break;
  mining dispatch now requires the tick-local vanilla centre crosshair.

The remaining failure is not yet attributed. It may be the support-block
guard correctly rejecting a stale corridor destination, or a different
vanilla world-denial condition. The next run therefore records, only on the
failure path, the corridor origin, destination, active block, current feet,
selected face, and live crosshair before changing behavior.

Files changed in this focused chain:

- `src/main/java/dev/mcai/companion/skills/stronghold/ReachObservedStrongholdSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/StrongholdSkills.java`
- `src/main/java/dev/mcai/companion/skills/mining/ExcavateSafeTunnelSkill.java`
- `src/main/java/dev/mcai/companion/runtime/ServerRuntime.java`
- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_instance/real_stronghold_reach.json`
- focused mining, stronghold, planner, canonicalizer, and live-provider tests.

Latest directed gates:

```text
compileJava: PASS
ExcavateSafeTunnelSkillTest: PASS
ReachObservedStrongholdSkillTest: PASS
real_stronghold_reach: FAIL / mining_world_denied
```

Next:

1. add the failure-only dispatch diagnostic;
2. rerun only `mcai_companion:real_stronghold_reach`;
3. add a regression for the exact proven cause and correct it;
4. remove the temporary high-volume near-field GameTest logging after the
   physical gate passes;
5. update `GOAL_STATE.json` without promoting any formal milestone.

This remains controlled component evidence. Natural unknown-seed stronghold
approach, portal-room discovery, exact-JAR Actor/Observer evidence, 200 unseen
Hardcore seeds, and formal M0-M4 remain `NOT_RUN`.

## 2026-08-03 stronghold compound and moving chunk window gate passed

The focused production stronghold-triangulation compound now passes:

```text
mcai_companion:real_stronghold_triangulation
1 required / all passed
28.58 s
Forge 65.0.0
```

The gate server-verified two ordinary owned Eye throws, two first-person
trajectory measurements, at least 250 blocks of normal body travel between
throws, a bounded measured intersection, and twelve Eyes remaining for the
portal. The exact configured `mimo-v2.5` phase-selector smoke chose
`triangulate_stronghold_search_area` with no arguments and exact revisions.

Two earlier failures remain part of the audit:

1. GameTest randomly placed the fixture around x=8.7 million while generated
   strongholds remain near the origin, making the intended 256-block baseline
   nearly parallel. `GameTestServerThrottleMixin` now selects a deterministic
   near-origin development fixture; `mixin/gametest/**` remains excluded from
   the release JAR.
2. The first focused corridor rotated and floored integer samples, producing a
   one-block support hole. The production fail-closed planner returned
   `travel_to.route_unknown`. `EmbodimentGameTests` now rasterizes block
   centers against a continuous corridor projection.

The physical diagnosis also exposed a production headless-player issue:
`CommonListenerCookie.createInitial` requested only two chunks. New
`HeadlessViewDistance` and the updated `PendingPlayerSpawn` preserve vanilla
client defaults while requesting the server-configured view distance. The
focused gate verified requested view distance 10 and the normal player
tracking center moving with the body. It does not add forced chunks.
`PendingPlayerSpawnTest` covers the clamp and cookie contract.

Directed JVM suites, the focused Forge gate, and project-wide
`./gradlew check --rerun-tasks --no-daemon` all passed after these changes.

Current next root cause, corrected after tracing
`withCompletionUtility(...)` to its complete allow-list:

- the phase does already expose `travel_to`, `survey_surroundings`,
  `explore_for_observed_target`, and `excavate_safe_tunnel` alongside portal
  activation and entry;
- the missing layer is a bounded, resumable local controller that composes
  those fair primitives from the measured intersection;
- leaving every approach, depth, direction, target-block, and recovery
  parameter to separate model turns adds avoidable latency and has no durable
  recovery contract. The earlier claim that the skills were absent was an
  audit error and is explicitly superseded here.

Next implementation is a bounded, resumable, first-person stronghold approach
and exploration compound using the measured intersection, normal movement,
ordinary mining, and observed block evidence only. It must not use the seed,
structure APIs, hidden chunks, or unobserved portal-frame state.

Formal M0-M4 and all unseen-seed statistical gates remain `NOT_RUN`.

## 2026-08-03 real stronghold compound physical-gate diagnosis

Current production work adds the no-argument
`triangulate_stronghold_search_area` compound. It normally equips two owned
Eyes, performs a fair first-person Eye trace, walks a 256-block perpendicular
baseline through `TravelToSkill`, performs a second ordinary throw, and only
publishes a search area from measured rays. Completion readiness now reserves
14 Eyes: 12 worst-case portal frames plus two triangulation throws.

Files changed for this slice:

- `src/main/java/dev/mcai/companion/skills/stronghold/TriangulateStrongholdSearchAreaSkill.java`
- `src/main/java/dev/mcai/companion/skills/stronghold/StrongholdSkills.java`
- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/progression/CompletionResourceReadiness.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- the corresponding focused unit/live-provider tests and GameTest resources

Latest directed gates:

```text
stronghold unit/schema/planner contracts: PASS
real configured MiMo stronghold phase selection: PASS
broad headless lifecycle/completion-chain physical GameTest: PASS / 1 required
focused real stronghold compound:
  FAIL / tick 2,885 / intersection_unavailable
  both Eyes physically thrown; body physically travelled about 256 blocks
```

The focused failure is now measured, not guessed. Its two retained rays were:

```text
origin=(8700021.500,-698716.500), direction=(-0.996772,0.080284)
origin=(8699999.500,-698970.418), direction=(-0.996770,0.080313)
```

Their crossing angle is only about 0.002 degrees because the GameTest
structure was placed around X=8.7 million while vanilla concentric-ring
strongholds remain near the world origin. The 256-block baseline and both
fair traces worked; the fixture exercised an impossible normal-spawn
distance and correctly failed the conservative 3-degree quality gate.

Next:

1. determine why the persistent GameTest runner placed the large fixture at
   that extreme coordinate and create an isolated, reproducible near-origin
   physical gate without exposing a structure coordinate to production;
2. preserve the conservative production geometry unless real normal-distance
   measurements prove it wrong;
3. rerun only the focused physical gate, then the directed JVM suite and
   release checks;
4. record evidence in `docs/progress/GOAL_STATE.json`.

Formal M0-M4, external exact-JAR Actor/Observer, unseen-seed, soak, and
two-hour Hardcore completion gates remain `NOT_RUN`.

## 2026-08-03 Ender-reserve physical gate: fourth run

Current root-cause chain and corrections:

- the original generated safety roof left its two-block temporary placement
  pillar; the roof skill now removes both blocks through the ordinary
  first-person `BreakBlockSkill` and fairly re-observes all nine roof cells;
- long scans made valid roof evidence stale and triggered unsafe rebuilding;
  known shelters now receive a bounded first-person revalidation scan;
- multiple pearl drops could leave a visible stack behind; the compound now
  delegates that stack to the ordinary `CollectObservedItemSkill`;
- loose return tolerances could leave the body at a cell edge, so shelter
  return now settles within 0.20 blocks of the center.

Files changed for this physical gate:

- `src/main/java/dev/mcai/companion/skills/loot/BuildEndermanSafetyRoofSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- focused tests under
  `src/test/java/dev/mcai/companion/skills/loot/`
- this recovery checkpoint

Latest directed evidence:

```text
SecureEnderPearlReserveSkillTest: PASS
AcquireShelteredEnderPearlSkillTest: PASS
CollectObservedItemSkillTest: PASS
fourth real Forge 65.0.0 GameTest:
  FAIL / tick 1,293 / stage ACQUIRING_ENDER_PEARL
  physical roof present / temporary pillar removed / first Enderman killed
  terminal secure_ender_pearl_reserve.internal_failure
```

The latest failure is not yet diagnosed because the compound previously
discarded the caught exception. It now logs the exact exception and stack.
Next: rerun only `mcai_companion:real_ender_pearl_reserve`, fix the first
reported code line, and repeat the same physical gate before any unrelated
regression or provider smoke. Formal M0-M4 remain `NOT_RUN`.

## 2026-08-03 Ender-reserve gate closure and adjacent regression

The added stack trace identified the fourth-run failure exactly:

```text
IllegalArgumentException: arrivalRadius must be in [0.25, 32]
at MoveToParameters.<init>
at SecureEnderPearlReserveSkill.beginReturn
```

The preceding centering correction had used `0.20`, below the normal
`MoveToParameters` contract. The return now uses the tightest legal radius,
`0.25`, and the final shelter predicate permits only a floating-point epsilon
beyond it. A package-private parameter factory and focused assertion prevent
this composition boundary from drifting again.

Post-fix directed gates:

```text
SecureEnderPearlReserveSkillTest: PASS
AcquireShelteredEnderPearlSkillTest: PASS
CollectObservedItemSkillTest: PASS
LootSkillParametersTest: PASS
KnownSkillArgumentCanonicalizerTest: PASS
MinecraftPlannerInputFactoryTest: PASS
real_ender_pearl_reserve run 1:
  PASS / 21.83 s / 15 route units / 11 target attempts /
  12 returns / 2 extra drops
real_ender_pearl_reserve run 2:
  PASS / 23.02 s / 14 route units / 11 target attempts /
  11 returns / 1 extra drop
real configured mimo-v2.5 ACQUIRE_ENDER_PEARLS selector: PASS
real_sheltered_ender_pearl_acquisition:
  PASS / 0.881 s / vanilla durability, kill, drop and pickup verified
Gradle check: PASS
```

The new single-target physical gate initially failed before skill start
because its focused 48-block fixture spawned the Enderman at the full
160-block scenario's relative origin. `fixtureRelative` now selects the
active scenario origin; existing full-chain coordinates remain unchanged.

This closes the deterministic M2 Ender-reserve component gate only. Natural
Enderman discovery, eye crafting, stronghold travel, full dragon completion,
exact-JAR Actor/Observer, random-seed Hardcore runs, and formal M0-M4 remain
`NOT_RUN`.

## 2026-08-02 Ender-pearl reserve physical gate — current recovery point

The focused `mcai_companion:real_ender_pearl_reserve` gate now reaches and
server-confirms all eleven ordinary cobblestone placements for a generated
3x3 Enderman safety roof. Earlier failures were narrowed and corrected:

- the builder's own short jump-placement landing was being rejected as a
  generic falling hazard; only that bounded, supported, same-cell landing is
  now managed locally;
- horizontal roof placement was impossible from underneath the starter
  block; the builder now uses ordinary `MoveToSkill` positioning around each
  target and returns to its anchor;
- survey turns were counting stale semantic samples; site and roof scans now
  require a newer observation revision after alignment;
- the GameTest no longer spawns its controlled Enderman until the exact
  production fair-roof predicate has been observed while centered.

Latest failing physical gate:

```text
FAIL / tick 763
terminal detail: build_enderman_safety_roof.roof_not_verified
all 11 placements server-confirmed
```

Root cause:

- the temporary two-block pillar used to jump-place the roof starter remains
  beside the anchor;
- its upper block occludes the starter's underside from the final centered
  first-person verification;
- accepting older shelter evidence would weaken the combat safety boundary,
  so the pillar must be removed through normal player mining.

Current files changed:

- `src/main/java/dev/mcai/companion/skills/loot/BuildEndermanSafetyRoofSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkill.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/test/java/dev/mcai/companion/skills/loot/SecureEnderPearlReserveSkillTest.java`

Last directed JVM gates for the compound and single primitive pass. The
focused physical Ender gate remains red and must not be reported as complete.

Next:

1. compose the existing vanilla-path `BreakBlockSkill` into the roof builder;
2. mine the upper and lower temporary pillar blocks, return to the anchor,
   and reacquire fresh first-person evidence for all nine roof cells;
3. prevent the shelter fast path from completing a newly built roof before
   pillar cleanup;
4. rerun the directed JVM and focused physical gates before any broader
   regression or paid provider run.

Formal M0-M4, random-seed Hardcore completion, external Actor/Observer, soak,
and two-hour completion gates remain `NOT_RUN`.

## 2026-08-02 durable Blaze reserve compound and current recovery point

The completion route previously asked the high-level model to authorize one
Blaze at a time. That made a seven-rod reserve depend on repeated paid model
turns and allowed the body to stop after a single successful drop. The route
now exposes one bounded local compound,
`secure_nether_blaze_material`, which owns fair first-person search, ordinary
combat, drop collection, no-drop recovery, and the full server-authored
14-unit Blaze-material threshold.

The implementation uncovered and fixed four factual action-loop defects:

1. the compound's risk contract was absent before the first child combat and
   during child-to-child transitions;
2. the scan state advanced after issuing a look request instead of waiting
   until the smoothly actuated body was actually aligned;
3. a second visible Blaze could legitimately interrupt ordinary rod pickup,
   but that exact interruption was treated as terminal;
4. the physical oracle demanded killing a newly spawned, unnecessary Blaze
   after the reserve was already complete.

Current changed files for this chain:

- `src/main/java/dev/mcai/companion/skills/loot/SecureNetherBlazeMaterialSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/AcquireNetherBlazeRodSkill.java`
- `src/main/java/dev/mcai/companion/skills/loot/LootSkills.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/embodiment/EmbodimentGameTests.java`
- `src/main/resources/data/mcai_companion/test_instance/real_nether_blaze_material_reserve.json`
- focused tests under
  `src/test/java/dev/mcai/companion/{skills/loot,model,runtime}/`

Latest directed evidence:

```text
secure-Blaze JVM contracts and affected planner/route contracts:
  PASS / BUILD SUCCESSFUL in 4 s

real configured mimo-v2.5 completion-phase selection:
  PASS / 18.496 s
  restored the saved credential without putting a key on the command line
  selected secure_nether_blaze_material with exact empty arguments

mcai_companion:real_nether_blaze_material_reserve on Forge 65.0.0:
  PASS / 1 required / 3.165 s
  repeated ordinary combat, durability, vanilla drops and collision pickup

mcai_companion:real_nether_blaze_rod_acquisition on Forge 65.0.0:
  PASS / 1 required / 1.849 s
  proves the existing single-target physical primitive did not regress
```

The last failing physical gate in this chain was the reserve gate's premature
pickup-danger termination; it is now green. No formal M2 gate has run:
natural fortress discovery, unknown Nether terrain, the Ender-pearl reserve,
stronghold search, dynamic End traversal/dragon victory, exact-JAR
Actor/Observer execution, and 200 unseen Hardcore seeds are still missing.

Next exact implementation step: add a durable, fair
`secure_ender_pearl_reserve` compound with a physically verified local safety
structure, repeated visible-Enderman combat, ordinary drops, and the same
14-unit route threshold; then run its focused JVM, Forge physical, and real
provider-selection gates. Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 M2 completion-phase authority boundary and directed gates

Root cause addressed:

- the server already projected ordered COMPLETION milestones, but the model
  function schema was restricted only for FOUNDATION;
- during a completion goal the provider could therefore see and request
  future End, dragon, and resource skills before their prerequisites;
- the route treated one Blaze rod, one Ender pearl, or one crafted eye as a
  sufficient milestone, which was component evidence rather than
  unknown-seed completion readiness.

Implemented:

- `MinecraftPlannerInputFactory` now derives an exact allow-list from the
  trusted COMPLETION `nextObjectives` value;
- early wood/crafting/stone/food/iron phases expose their durable local
  compound controller, the Nether/resource/search phases expose only bounded
  fair-perception utilities needed for that phase, dragon exposes only
  `fight_ender_dragon`, and a complete or malformed trusted route exposes no
  mutation skill;
- a server-authored phase playbook now covers the ordinary sequence from wood
  through physical return without granting seed, structure, hidden-block,
  direct inventory, or teleport authority;
- COMPLETION guidance now explicitly requires basic crafting and a food
  reserve before iron/Nether work;
- readiness now requires 14 Blaze-powder-equivalent units, 14
  pearl-or-crafted-eye units, and 12 currently crafted eyes. Crafted eyes
  retain lawful consumed-resource evidence, so early crafting does not
  falsely revoke prior acquisition.

Files changed:

- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/progression/SurvivalRouteTracker.java`
- `src/test/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactoryTest.java`
- `src/test/java/dev/mcai/companion/progression/SurvivalRouteTrackerTest.java`
- `src/test/java/dev/mcai/companion/runtime/LiveProviderSmokeTest.java`
- this checkpoint and `docs/progress/GOAL_STATE.json`

Directed evidence:

```text
planner/progression/model/portal/loot/stronghold/combat JVM regression: PASS
configured mimo-v2.5 completion phase selector:
  PASS / 18.25 s complete provider smoke
  visible Blaze -> acquire_nether_blaze_rod
  copied sampleSequence=73 and observationId=visible-0
  registered future fight_ender_dragon was absent from the request schema
mcai_companion:real_nether_blaze_rod_acquisition:
  PASS / 1 required / 2.523 s / Forge 65.1.0
mcai_companion:real_end_portal_activation:
  PASS / 1 required / 7.765 s / Forge 65.1.0
mcai_companion:real_portal_cast_and_light:
  PASS / 1 required / 2.520 s / Forge 65.1.0
```

These are honest directed inner-loop component gates. They do not prove
natural fortress discovery, repeated seven-rod acquisition, natural Enderman
search and safe-roof construction, unknown-terrain stronghold excavation,
dynamic Ender Dragon victory, exact-JAR Actor/Observer behavior, 200 unseen
Hardcore seeds, or the two-hour 1,000-seed M4 target. Formal M0-M4 and every
unseen-seed/soak/external-client gate remain `NOT_RUN`.

Immediate next work: close the first still-missing natural M2 compound chain
instead of replaying the already-passing controlled components. The priority
is a durable first-person Nether progression controller that can discover a
visible Blaze target through ordinary exploration, acquire the full verified
material reserve over repeated vanilla drops, preserve a retreat portal, and
fail safely without structure lookup.

## 2026-08-02 fifth real-provider run passed after authoritative goal closure

Implemented a narrow completion policy:

- ordinary conversational goals still require the model to choose
  `COMPLETE_GOAL`;
- explicit foundation/completion routes may close locally only when the same
  `ServerGoalCompletionVerifier` predicate already used to reject premature
  model claims is accepted;
- locked Hardcore evaluations remain owned by the separate victory tracker
  and cannot use this shortcut;
- no milestone is inferred from model text or a no-op response.

Directed results:

```text
brain/progression/planner/model/building focused tests: PASS
real_zero_human_dedicated_server_foundation on Forge 65.1.0:
  PASS / 1 required test / 9.941 min
  zero human players for the whole run
  initial goal entered through the production MCP backend
  12 accepted Responses decisions
  0 protocol/schema/skill-start rejections
  99,963 input + 1,929 output = 101,892 model tokens
  final event: server_verified_auto_complete at world revision 15
```

The physical route started with an empty inventory, mined and picked up
ordinary drops, used vanilla recipes and furnace/menu transactions, obtained
the iron pickaxe/bucket/shield, established and used storage, generated a
terrain-bound shelter plan, crossed the open doorway to the exterior apron,
returned through the body-verified corridor, confirmed the final roof block,
retained valid shelter evidence, and reached the second day.

This closes the current controlled M1 regression. It remains a flat,
resource-controlled inner-loop fixture, not a random-seed Hardcore
distribution. Formal M0-M4, external exact-JAR Linux Actor/Observer,
unseen-seed, soak, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-02 fourth real-provider run reached all M1 evidence but did not close the goal

Directed verification before this run:

```text
building/model/brain/planner focused tests:
  PASS / 118 tests
mcai_companion:roof_jump_placement on Forge 65.1.0:
  PASS / 1 required test / 2.156 min
```

The fourth exact configured-MiMo, zero-human controlled foundation run
crossed the formerly failing doorway, completed the roof, and retained a
valid shelter. It still failed the final gate:

```text
FAIL / 15.91 min / tick 19,091
clock=37,083
all 11 foundation milestones server-verified
shelterValid=true
goalStatus=RUNNING
```

Root cause:

- after `SHELTER_COMPLETED` and `FIRST_NIGHT_SURVIVED` were both recorded,
  the server-authored route had no remaining objective;
- the provider nevertheless returned `REPLAN` with
  `SEMANTIC_REFRESH` repeatedly at the same decision epoch instead of
  `COMPLETE_GOAL`;
- each refresh was accepted and billed even though the authoritative
  completion predicate could no longer change the outcome;
- the goal therefore remained `RUNNING` until the six-minute final-stage
  timeout despite every requested world result already being verified.

Next:

1. make the local server completion verifier close a running goal once the
   same full, authoritative route predicate used for `COMPLETE_GOAL` is
   satisfied;
2. retain fail-closed rejection for premature model completion and never
   infer completion from model speech;
3. add focused tests for autonomous verified completion, incomplete-route
   refusal, and idempotence;
4. rerun the exact real-MiMo zero-human gate and then the release gates.

Formal M0-M4 remain `NOT_RUN`.

Directed verification after the doorway correction:

```text
118 focused planner/model/brain/building contracts: PASS
mcai_companion:roof_jump_placement on Forge 65.1.0:
  PASS / 1 required test / 2.156 min
  physically crossed the open doorway, traversed the exterior apron,
  committed the interior fallback, returned cardinally through the doorway,
  and completed the remaining inner roof
```

The next gate is another exact real-MiMo, zero-human controlled foundation
run. It must still be treated as inner-loop evidence only.

## 2026-08-02 support guard verified; active-plan return path is current blocker

The next exact real-MiMo zero-human foundation run honestly failed at tick
10,860 (9.052 game-test minutes). It passed logs, basic crafting, stone,
food, iron, workstations, and 53 of 55 shelter placements. It did not mine
its support floor after the new guard.

Current root cause:

- after placing the exterior roof, the body needed to return through the
  doorway for the last two interior roof blocks;
- `BuildShelterStepSkill` cleared `exploredActivePlanTraversalStands` after
  each confirmed placement and again when beginning the interior return;
- the current semantic fan still contained the recently traversed corridor
  voxels, but did not classify the body's immediately adjacent voxels as
  complete safe stands;
- the return graph therefore saw the safe doorway corridor as disconnected
  and failed three times with
  `build_shelter_step.no_observed_traversal_stand`, followed by
  `SAFE_IDLE / repeated_skill_failure_without_progress`.

Files already changed for the preceding support-floor failure:

- `src/main/java/dev/mcai/companion/embodiment/PlayerSupportBlockGuard.java`
- `src/main/java/dev/mcai/companion/embodiment/ServerOwnedInteractionSkillActuator.java`
- `src/main/java/dev/mcai/companion/skills/gathering/GatherVisibleBlockClusterSkill.java`
- `src/main/java/dev/mcai/companion/model/MinecraftPlannerInputFactory.java`
- their focused unit tests;
- `BuildingGameTests`, `EmbodimentGameTests`, and the
  `current_support_mining_guard` GameTest resources.

Last verified focused gate:

- Forge 65.1.0 physical `current_support_mining_guard`: PASS 1/1. Mining the
  current support was denied without a world change; after stepping aside,
  ordinary survival mining succeeded and consumed tool durability.

Next implementation:

1. add an active-plan-only set of body-verified transit stands;
2. record actual feet positions while the movement child is running;
3. retain those stands across individual placements and the roof-return
   transition, but clear them whenever the plan/session/goal is replaced;
4. let the roof-return graph bridge through currently observed,
   body-verified transit positions, while leaving final movement and collision
   validation to the ordinary physical movement skill;
5. prove the disconnected-fan regression with focused tests, then rerun the
   roof physical gate and only then the exact live-MiMo foundation gate.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 scale-search fix verified; unsafe floor mining exposed

The shelter-scale state leak was corrected and its focused contracts passed:

- `BuildShelterStepSkillTest`, `OwnedStructureBlockIndexTest`, and
  `MinecraftPlannerInputFactoryTest`: PASS;
- physical Forge 65.1.0 `roof_jump_placement` including the post-build
  protected-block mining attempt: PASS 1/1 in 26.14 seconds.

The third exact real-MiMo zero-human foundation rerun then failed honestly at
tick 10,149 after 8.462 minutes. It did not fail in shelter planning. The
iron toolkit had produced no iron pickaxe, bucket, shield, or verified
furnace when the stage deadline expired.

Persisted task checkpoint:

```text
skill: prepare_iron_toolkit
phase: EXPLORE_RESOURCE
resource: STONE
elapsedTicks: 2871
exploration: SEARCHING, 16 segments, 0 route failures
```

Current root cause:

- before the iron compound started, the model selected generic dirt/grass
  gathering for shelter materials;
- `GatherVisibleBlockClusterSkill` only de-prioritized the block directly
  supporting the body; if it was the only connected candidate it could still
  mine it;
- the production mining boundary therefore allowed the body to dig through
  its own floor and fall from the controlled resource-wall level to the
  lower superflat surface;
- the later fair stone explorer searched from that lower level and could not
  observe the remaining stone, coal, or iron above.

Files already changed in this recovery chain:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/main/java/dev/mcai/companion/skills/building/OwnedStructureBlockIndex.java`
- `src/main/java/dev/mcai/companion/skills/interaction/BlockBreakProtection.java`
- `src/main/java/dev/mcai/companion/skills/interaction/ServerOwnedInteractionSkillActuator.java`
- `src/main/java/dev/mcai/companion/runtime/CompanionRuntime.java`
- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- focused unit and physical GameTest sources for those changes

Last failed gate:

- real configured-model, zero-human dedicated-server foundation:
  FAIL at tick 10,149 / 8.462 minutes in
  `prepare_iron_toolkit -> EXPLORE_RESOURCE(STONE)`.

Next:

1. add a server-authoritative current-support mining guard before the
   ordinary player destroy action, so generic mining must first step off the
   block it wants to remove;
2. make the gatherer reject, rather than merely rank last, a currently
   supporting candidate and add focused regression contracts;
3. rerun the smallest unit/physical floor-mining gate, then the exact
   real-MiMo foundation gate;
4. keep formal M0-M4 and Linux Actor+Observer E2E as `NOT_RUN`.

## 2026-08-02 current failure: model dismantled its verified shelter

The exact configured-MiMo, real-time, player-chat, zero-human foundation
rerun failed honestly after the roof traversal correction:

```text
gate:
  mcai_companion:real_zero_human_dedicated_server_foundation
result:
  FAIL / 13.12 game-test minutes / tick 15,730
survival:
  true
shelter:
  invalid
terminal:
  repeated_skill_rejection_without_world_change
```

The navigation fix itself worked in the live run:

- construction reached 53/55 confirmed steps;
- exterior frontier surveys dropped from roughly 8--9 seconds to roughly
  5 seconds;
- the observed doorway return traversed 12 ordinary cells in roughly
  8 seconds without repeating panoramic surveys.

The new failure happened after the model said the shelter was complete and
selected `gather_visible_block_cluster` against a visible
`minecraft:oak_planks` wall cell. Its speech claimed it would reclaim wall
planks. The skill eventually removed confirmed lower-wall step 10 and moved
the body into that gap. Direct current semantic evidence then correctly
contradicted the saved placement:

```text
stepIndex=10
role=LOWER_WALL
target=[-10975928,-44,-10547732]
expected=minecraft:oak_planks
observed=AIR
occupancy=BODY_OCCUPIED
```

Three later `build_shelter_step` attempts were rejected because the retained
plan truthfully disagreed with the now-missing physical wall. The structure
Oracle remained false; no completion was fabricated.

Root cause and next directed fix:

1. The post-build/next-day phase still admitted generic visible-block
   gathering, permitting the high-level model to target its own generated
   structure.
2. The generic gather executor had no ownership/protected-structure guard at
   the final action boundary.
3. Add a planner admission contract that the guard/next-day phase exposes
   only its phase compound and non-destructive observation/wait behavior.
4. Add a server-authoritative protected-volume/owned-shelter check before
   any generic block break; do not rely only on prompt wording.
5. Add first-red planner and physical/actuator contracts proving a visible
   shelter wall cannot be gathered, while unrelated visible natural logs
   remain legal.
6. Keep unknown skill names fail-closed. Separately canonicalize provider
   fields only for an explicit whitelist of genuinely no-argument compounds.

Changed files before this new fix remain:

```text
src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java
src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java
docs/CODEX_RECOVERY_CHECKPOINT.md
```

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 roof-frontier transit and survey-cost correction

The live foundation failure at approximately 50/55 shelter steps was traced
to two coupled local-controller costs rather than missing materials or a
model-planning loop:

- active-plan apron traversal counted every backtracking hop against a
  one-perimeter (24-hop compact shelter) budget;
- after revisiting a body-verified transit cell, the parent ran another
  24-view stationary panorama before taking the next already observed hop.

The second cost explains the live log pattern where move attempts advanced
from 1 to 22 while distinct stands stayed around 11--13. The traversal was
finite, but repeated panoramic scans consumed the remaining real-time stage
budget before the last roof cells were physically placed.

Changed files:

```text
src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java
src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java
docs/CODEX_RECOVERY_CHECKPOINT.md
```

Implemented:

- first arrival at an unvisited frontier still requires a fresh fair
  first-person survey;
- arrival through a previously body-verified transit cell immediately
  continues toward the next destination selected from the current semantic
  navigation snapshot;
- a relocated exterior roof frontier uses four cardinal headings with all
  three vertical bands; the initial site survey remains the denser
  eight-heading panorama;
- the strict active-plan movement bound is two apron perimeters, covering one
  discovery circuit plus one complete verified backtrack without becoming
  unbounded;
- the checkpoint records whether the active destination is known transit.

Directed verification:

```text
BuildShelterStepSkillTest:
  PASS

mcai_companion:roof_jump_placement on Forge 65.1.0:
  PASS / 1 required test / 33.56 s
  physical evidence: complete survival-mode roof placement, forced opaque
                     apron, ordinary exterior stance, door-side corner,
                     observed doorway return and complete final inner roof

check + verifyReleaseJar:
  PASS
```

The focused physical gate had previously taken approximately 2.6 game-test
minutes. The new run completed in 33.56 seconds without direct movement,
world writes from production code, hidden terrain, stale-shell shortcuts, or
weakened structure verification.

One earlier command used an unrecognized `gametest_selector` property and
accidentally launched the unrelated full batch. It was terminated as soon as
this was detected; its unrelated failure is not counted as evidence for this
fix. The correct repository selector is `live_model_selector`.

Last failed relevant gate remains the preceding real configured-MiMo
foundation run at approximately 50/55 shelter steps. Next: rerun the exact
real-time, player-chat, zero-human MiMo foundation selector with the corrected
roof controller. Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 live foundation roof-stance failure

After removing the advanced micro-action bypass, the identical real-MiMo gate
advanced past the previous material failure but still failed:

```text
gate:
  mcai_companion:real_zero_human_dedicated_server_foundation
result:
  FAIL / tick 14,908 / 12.43 min
failed stage:
  SHELTER
material stage:
  PASS
last observed construction progress:
  approximately 50 of 55 generated plan steps
active skill:
  build_shelter_step
```

This run proved the routing correction: food used the single local reserve
compound, advanced workstation/storage ran through its compound, and the
server admitted `build_shelter_step` only after structural blocks, a door,
and lights were physically prepared.

New root-cause direction: the generated shelter's final roof cells were not
reachable from the controller's current observed stance set. The controller
walked around the same exterior perimeter, reporting attempts up to 22/24
while the number of distinct explored stands remained almost unchanged. It
eventually discovered one farther apron support and resumed placement, but
the stage wall-clock gate expired before server shelter evidence existed.

Next:

- inspect the active-shelter stance frontier, attempted-stand accounting, and
  roof/apron reachability;
- ensure repeated stands do not consume the bounded frontier and add a legal
  observed doorway/interior or exterior-apron route for the remaining roof
  cells;
- reproduce with the focused physical shelter GameTests before spending
  another live-model run;
- keep structure verification and timeout unchanged until the navigation
  defect is corrected.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 current recovery state: protect the generated shelter

The roof-stance defect above is no longer the current blocker. The focused
physical gate
`mcai_companion:roof_jump_placement` passed 1/1 on Forge 65.1.0 in 33.56
seconds after known transit cells were allowed to continue without another
panorama and new exterior frontiers were reduced to a fair 12-view survey.

The identical real-MiMo foundation gate then reached 53/55 confirmed shelter
steps and exercised the corrected doorway-return path. It failed at tick
15,730 because the model subsequently selected
`gather_visible_block_cluster` on an oak-plank wall belonging to that active
generated shelter. The ordinary mining path physically removed confirmed
LOWER_WALL step 10; later semantic evidence correctly reported AIR and
BODY_OCCUPIED at the missing wall, and three shelter restarts were rejected.
The terminal result was
`SAFE_IDLE / repeated_skill_rejection_without_world_change`.

Current root cause:

- the foundation planner admitted generic gathering outside its actual
  resource-acquisition phase;
- the mining execution boundary had no shared index of positions reserved by
  the active generated shelter, so a bad but schema-valid model decision
  could dismantle owned construction.

Files already changed for the preceding roof correction:

- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- `src/test/java/dev/mcai/companion/skills/building/BuildShelterStepSkillTest.java`
- this recovery checkpoint

Last focused gates:

- `BuildShelterStepSkillTest`: PASS
- physical `roof_jump_placement` on Forge 65.1.0: PASS 1/1
- `check verifyReleaseJar`: PASS
- real configured-model foundation gate: FAIL at tick 15,730 due to the
  self-dismantled wall described above

Next:

1. narrow foundation planner admission so generic gathering is callable only
   in verified gathering/resource-recovery phases;
2. add a server-authoritative shared protected-position index, register every
   active shelter-plan position as soon as the plan is generated, and reject
   mining any protected position before an ordinary destroy action starts;
3. add focused planner and gathering contracts proving natural logs remain
   gatherable while active shelter blocks are not;
4. rerun only those tests and the focused physical shelter gate, then repeat
   the exact live-MiMo foundation gate.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 owned-structure fix verified; next live failure isolated

Implemented the two-layer shelter protection:

- foundation `SURVIVE_OR_SLEEP_THROUGH_NIGHT` now exposes only a minimal
  defensive/observation allow-list, and a fully verified route exposes no
  mutation skills;
- `OwnedStructureBlockIndex` registers every active generated plan position,
  retains completed shelters, and restores persisted verified shelter
  geometry after restart;
- `ServerOwnedInteractionSkillActuator.beginMining` checks that shared index
  before dispatching any ordinary mining action, so all mining skills share
  the same server-authoritative denial boundary.

Focused evidence:

- planner/protection/source contracts: PASS;
- physical Forge 65.1.0 `roof_jump_placement`: PASS 1/1 in 34.17 seconds;
- that physical gate built the roof through survival actions, then attempted
  to mine one generated roof block and observed `WORLD_DENIED` with the block
  unchanged.

The next exact real-MiMo foundation rerun did not dismantle the shelter. It
failed at tick 8,871 after 7.395 minutes with
`SAFE_IDLE / repeated_skill_failure_without_progress`. The persisted event
audit identified all three identical failures as
`shelter.insufficient_observation`.

New root cause: the first model call requested a STANDARD shelter and fairly
exhausted four relocation candidates. Later calls reduced the request to
COMPACT, but `BuildShelterStepSkill` bound its rejected-site set and attempt
counter only to goal revision and body session. The compact footprint
therefore inherited every rejection and the exhausted budget from the larger
standard footprint and could not reconsider any site.

Current change:

- the initial site-search binding now also includes `ShelterScale`;
- a scale change resets only that bounded pre-plan site search, while retries
  at the same scale still share and exhaust the four-attempt limit;
- checkpoint JSON now includes site-search scale, relocation count, and
  rejected-site count;
- a focused unit contract proves STANDARD -> COMPACT starts a fresh bounded
  search while STANDARD -> STANDARD does not.

Next:

1. run `BuildShelterStepSkillTest`;
2. rerun the focused physical roof/protection gate;
3. rerun the exact real-MiMo foundation gate and inspect the next factual
   result.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-02 body-verified return bridge verified by the real model

The scale-search correction was followed by another exact configured-MiMo
run. It no longer fell through its support floor, reached 53/55 shelter
placements, but failed after three
`build_shelter_step.no_observed_traversal_stand` outcomes. The current
semantic fan contained the distant safe doorway corridor but lacked complete
feet/head/support evidence for the adjacent cells the body had just crossed.
Per-placement clearing of traversal history disconnected that corridor.

Implemented:

- `BuildShelterStepSkill` now records grounded body feet during the same
  committed plan, goal revision, dimension, and body session;
- this memory survives individual block placements and the exterior-to-
  interior roof transition, but is cleared when a plan is replaced or
  repaired;
- only body-verified cells still present in the incremental first-person
  navigation map may bridge incomplete stand evidence;
- ordinary `MoveTo` remains responsible for physical collision/path
  validation, so this adds neither teleportation nor hidden-world reads;
- the checkpoint now records `bodyVerifiedTraversalStands`.

Directed verification:

```text
BuildShelterStepSkillTest:
  PASS

mcai_companion:roof_jump_placement on Forge 65.1.0:
  PASS / 1 required test / 34.17 s

mcai_companion:real_zero_human_dedicated_server_foundation:
  PASS / 1 required test / 9.328 min / real mimo-v2.5
  no human player at any test tick
  initial goal entered through the production localhost MCP backend
  14 billed model responses
  117,285 input + 2,093 output = 119,378 model tokens
  4 malformed/unknown model decisions rejected without execution
  final build checkpoint COMPLETED, 57 confirmed plan steps,
  20 body-verified active-plan transit stands
```

This is controlled inner-loop evidence: the fixture supplies a flat local
course and visible wood, stone, food, coal, and iron. It is not a random
seed, not the formal Hardcore distribution, and does not promote M1 or any
M2-M4 claim. The formal unseen-seed, exact-JAR Linux Actor/Observer, soak,
and two-hour completion gates remain `NOT_RUN`.

## 2026-08-02 phase-schema and roof-return loop correction

The next real-MiMo audit first isolated avoidable model protocol churn:

- static crafting guidance advertised `craft_recipe` while the
  server-authored foundation phase admitted only `prepare_stone_tools`;
- the provider-side decision schema allowed an arbitrary `skillName`;
- MiMo sometimes selected the correct no-argument compound but added
  authority-free filler such as `hand`, `dimension`, `sampleSequence`, or
  `scale`.

Implemented:

- generic skill guidance and route playbooks are filtered against the current
  per-request allow-list;
- the actual Responses/Chat function schema now enumerates only the currently
  admitted skill names plus the mandatory empty value for non-skill
  decisions;
- six exact parameterless foundation compounds narrowly discard all provider
  filler arguments before the same local typed parser validates the call;
- real `mimo-v2.5` smoke coverage intentionally presents a dropped item and
  crafting affordance during `CRAFT_AND_MINE_STONE` and verifies that the
  provider chooses only `prepare_stone_tools`.

The first post-schema real foundation run passed in 8.401 minutes. It had zero
unknown-skill decisions, but two correct parameterless calls were rejected
before the filler canonicalization was compiled. The next compiled run
accepted every model decision with zero protocol rejection, then exposed a
separate physical shelter loop at 50/55 placements.

Root cause of that latest failed gate:

- a traversal-driven exterior-to-doorway roof return used the temporary
  `returningInsideForRoof` flag;
- unlike an aim-driven roof return, it did not commit
  `roofInteriorFallbackPriority`;
- reaching an interior floor cleared the temporary flag, after which the
  ordinary roof selector chose the exterior apron again;
- the body repeatedly circled outside and returned inside until the real-time
  foundation gate timed out at tick 13,733 after 11.45 minutes.

Files changed for the current correction:

- `src/main/java/dev/mcai/companion/runtime/MinecraftPlannerInputFactory.java`
- `src/main/java/dev/mcai/companion/model/DecisionEnvelopeCodec.java`
- `src/main/java/dev/mcai/companion/model/ModelRequestFactory.java`
- `src/main/java/dev/mcai/companion/model/KnownSkillArgumentCanonicalizer.java`
- `src/main/java/dev/mcai/companion/skills/building/BuildShelterStepSkill.java`
- corresponding focused tests in
  `src/test/java/dev/mcai/companion/{model,runtime,skills/building}/`
- this recovery checkpoint

Latest directed gates:

```text
planner/schema/canonicalizer/gateway contracts: PASS
real MiMo phase-selector smoke: PASS / 5.742 s
real zero-human controlled foundation before filler fix:
  PASS / 8.401 min / 2 filler-argument rejections / 0 unknown skills
real zero-human controlled foundation after filler fix:
  FAIL / 11.45 min / 0 model protocol rejections /
  physical roof-return loop at 50/55
mcai_companion:roof_jump_placement after roof transition fix:
  PASS / 1 required test / 2.175 min
```

Next:

1. rerun the exact real-MiMo zero-human controlled foundation gate;
2. require both server-verified completion and zero model protocol rejection;
3. inspect SQLite token/audit evidence and update `GOAL_STATE.json`;
4. run the focused release/build gates only after the live result is known.

This remains controlled inner-loop evidence, not a random-seed Hardcore M1
promotion. Formal M0-M4, external exact-JAR Actor/Observer, unseen-seed,
soak, and two-hour completion gates remain `NOT_RUN`.

## 2026-08-02 third real-provider run exposed doorway-settling evidence gap

The third exact configured-MiMo, zero-human controlled foundation run did
not pass and must not be reported as a completed gate:

```text
FAIL / 7.410 min / tick 8,884
48/55 construction steps server-confirmed
terminal detail: repeated_skill_failure_without_progress
one provider selection of unavailable legacy name `build_shelter`
```

The local authority boundary correctly rejected `build_shelter` during the
stone-tool phase and the next request selected `prepare_stone_tools`. The
body then gathered wood, stone, food, and iron, crafted the iron toolkit,
established workstations, and reached the final shelter roof sequence.

Latest physical root cause:

- the roof recovery selected the verified exterior doorway stand and ran the
  ordinary `MoveTo` plus first-person panorama;
- by the time the final blocked-jump recovery evaluated its geometry, the
  body feet were back in the doorway cell rather than the requested exterior
  apron cell;
- the completed wall then occluded the remaining outer-edge roof supports,
  while the semantic navigation graph had no complete safe apron frontier
  reachable from the doorway cell;
- steps 38, 39, and 43 were deferred, and three identical bounded skill
  failures correctly moved the goal to `SAFE_IDLE`.

Next correction:

1. make an exterior-door transition finish beyond the threshold and validate
   the body's settled cell before beginning the panorama;
2. preserve the bounded per-cell apron refresh and normal `MoveTo` collision
   checks;
3. add a focused contract for the outward-biased doorway target and a
   physical roof gate before paying for another full live-provider run;
4. retain provider enum violations as fail-closed behavior; do not execute or
   silently substitute an unavailable skill.

Formal M0-M4 remain `NOT_RUN`.

## 2026-08-10 latest recovery: run249 physical pass

After the earlier pickup/planting corrections, two more physical Forge runs
exposed and isolated edge cases:

```text
run247: FAIL / substrate approach cut across the central water corner
run248: FAIL / axial movement still stalled at a crop-cell collision edge
run249: PASS / All 1 required tests passed with strict replant proof
```

The current one-cell movement point preserves the non-moving horizontal axis,
and the test-only trace records ground, horizontal collision, water and
velocity.  This closes the current controlled farming fixture for the latest
source, but it remains no-model, release-excluded evidence.  Live provider,
random-seed Hardcore, Actor/Observer, soak, M1-M4 and two-hour gates remain
`NOT_RUN`.

## 2026-08-10 latest recovery: potato root-crop gate rerun

The first potato variant run after the strict replant proof still failed its
final physical assertion (7/8 age-zero plants), even though eight ordinary
mining and eight ordinary use actions had been accepted.  Because potatoes
use the same item as both harvest and planting material, action counts and
inventory deltas are not sufficient evidence.  I added a test-only per-use
block-state trace to `FarmingGameTests` and reran the identical Forge
GameTest (`run252`); it passed all required assertions.  The trace is
diagnostic only and is not used by production code.  A repeat run and the
beetroot variant are still required before closing this crop matrix.

This remains controlled, no-model GameTest evidence; live-provider,
random-seed Hardcore, Actor/Observer, soak, M1-M4, and two-hour gates remain
`NOT_RUN`.

Follow-up physical runs also passed: `run253` repeated the potato fixture,
`run254` covered beetroot, and `run255` repeated compact wheat.  The expanded
wheat fixture remains covered by the earlier `run249` pass.  These four crop
variants now have repeatable ordinary-action evidence in the controlled
fixture; this does not promote the farming skill to a live-model or release
gate.

## 2026-08-10 targeted JVM regression and recovery

The first full JVM run after the farming changes reported one failure in
`FairPerceptionSupportSourceContractTest`: support logic had been correctly
centralized in `BlockShapeAffordances`, while the source-contract test still
searched only the old sampler file.  The test now verifies both the sampler
delegation and the helper's conservative `UP`/full-collision/sturdy-face
condition.  Targeted and full JVM runs now pass (`1062` tests, `0` failures,
`2` skips).  The logged exception/warning lines are existing bounded-failure
and environment diagnostics covered by tests, not test failures.

The Forge 65.1.0 `check` task then passed as well, including the compatibility
schema checker and the same full JVM suite.  No formal external gate was
implicitly promoted by this build result.

## 2026-08-10 M1 menu transaction follow-up

Two isolated Forge 65.1.0 GameTests were rerun against the current source:

```text
run256: PASS / real_furnace_batch / ordinary survival menu smelting
run257: PASS / real_charcoal_furnace_batch / ordinary survival menu smelting
```

Both use a real headless `ServerPlayer` and the normal furnace/menu action
path. They are controlled no-model mechanics evidence only; they do not
establish live chat-to-action or any M1-M4 release gate.

The next isolated M1 mechanics runs also passed:

```text
run258: PASS / real_prepare_and_plant_plot
run259: PASS / real_prepare_water_source
run260: PASS / real_food_animal_hunt
```

These cover ordinary hoe/plant, bucket, animal-combat and drop collection
paths on Forge 65.1.0. They remain controlled no-model evidence and do not
change the formal M1 status from `NOT_RUN`.

`run261` then passed the multi-cell `real_build_hydrated_crop_field` gate on
Forge 65.1.0. It verifies an ordinary server-player commissioning path for a
watered field rather than a prewritten block blueprint. This is still
controlled no-model evidence and is not a live-model or release gate.

`run262` and `run263` also passed the current-source wood-exploration and
workstation-prerequisite gates on Forge 65.1.0. They close this focused M1
material/workstation slice through ordinary player interactions; formal M1 and
all live-model gates remain `NOT_RUN`.

## 2026-08-10 M2 portal activation regression and repair

The current-source End portal activation gate exposed a genuine semantic
verification race:

```text
run264: PASS / observed Nether portal cast and light
run265: PASS / observed Blaze combat, drop and pickup
run266: PASS / Ender pearl reserve
run267: PASS / stronghold triangulation
run268: FAIL / frame_activation_unverified at tick 805
run270: PASS / 12 ordinary eye uses, 12 eyed frames, 9 portal blocks
```

The failed run had already sent the fair first-person vanilla use packet, but
the finite semantic ray fan temporarily stopped reporting that frame. The
skill now accepts only the narrow transaction proof already supplied by the
ordinary actuator plus a fresh inventory decrement of exactly one eye, then
resumes a new fair search. It never reads the world directly or synthesizes a
frame state. JVM tests and the Forge 65.1.0 physical gate pass after the fix;
the retained run268 failure remains documented and is not counted as a pass.

## 2026-08-10 M2 stronghold approach runtime-budget recovery

`run-debug271` failed before meaningful excavation: the cold Forge server was
hundreds of ticks behind and the production skill supervisor terminated after
three consecutive 10 ms tick-budget breaches. The body had not moved and the
skill itself had not reported a navigation or mining failure. This was a
runtime startup false-negative, not evidence that the stronghold skill had
completed.

`CompanionRuntime` now uses an explicit twelve-breach allowance for its
production skill lane during cold class loading/chunk-ticket startup. The
rolling `RuntimeTickMetrics` 2 ms p95 audit remains unchanged and authoritative;
the unit-test default policy remains strict. After the change,
`run-debug272-stronghold-reach-gate.log` passed one required Forge 65.1.0 test
in 44.98 seconds. The log records more than 80 blocks of ordinary body travel,
descending physical excavation, pickaxe durability and torch consumption, and
current first-person stronghold evidence before clean completion. This remains
controlled no-model inner-loop evidence; M2, live-model, exact-client and
Hardcore random-seed gates remain `NOT_RUN`.

## 2026-08-10 M2 verified portal return retest

`run-debug273/logs/latest.log` passed one required Forge
65.1.0 test in 8.084 seconds. The same embodied `ServerPlayer` entered the
observed Nether portal, physically moved away, reacquired the remembered
arrival area through current first-person observations, and returned through
the vanilla portal controller. This confirms the return handoff after the
stronghold runtime-budget fix. It is controlled no-model component evidence;
continuous live-model dimension routing, natural-world discovery and formal
M2/M0-M4 gates remain `NOT_RUN`.

## 2026-08-10 End victory return race and repair

`run-debug274/logs/latest.log` failed honestly after the dragon
victory and End entry stages. The vanilla End portal changed dimension during
the same ordinary movement pulse that carried the 0.6-wide body into the
portal; the next semantic frame was already in the Overworld, so the skill had
not yet recorded `committedPosition` and rejected the traversal as
`dimension_changed_before_entry`. This was a real fair-verification race, not
a fabricated success.

`EnterObservedPortalSkill` now records a bounded 1.20-block horizontal
pre-commit envelope only for a currently observed and aligned End portal before
dispatching the normal movement pulse. A blocked pulse still has to produce a
real portal transition within the existing timeout, so this does not bypass
collision or world state. A focused JVM regression covers the one-pulse case.
The rerun `run-debug275/logs/latest.log` passed one required Forge
65.1.0 test in 25.47 seconds, including End entry, ordinary cage/dragon
actions, dragon death credit and return-portal entry. It remains controlled
no-model component evidence; live-model, natural-world and formal M0-M4 gates
remain `NOT_RUN`.

The post-fix focused portal JVM regression and full Forge `check` also pass on
Forge 65.1.0 (zero failures/errors and two external skips), and the
compatibility schema validator passes. Expected warning/error lines are
bounded-failure test diagnostics; Gradle completed successfully.

The post-fix package boundary also passed on Forge 65.1.0/JDK 25:
`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar` completed
successfully. `build/libs` contains one installable product JAR with SHA-256
`2b1602566c6187d3b39936aa2c7e48a262bcbd0a188604fd1f9d2b6de57a2f29`; the
audit slim artifact is isolated under `build/audit-libs` with SHA-256
`31f887456bfb1b213475f8484ef9bb4c1e906520964ef3b10083c5cb06efd339`.

The repository Python audit suite then passed all 20 tests (`unittest
discover -s e2e -p 'test_*.py'`). This is static/evidence validation only;
the real Actor/Observer client, live provider, unknown-seed Hardcore and
formal M0-M4 gates are still not run.

The current Forge 65.1.0 local-survival regression `run-debug276` also passed
one required real-headless-player test in 4.121 seconds against ten zombies and
ten skeletons. This validates the emergency reflex lane only; it is not a
model-controlled PVP result or a release/statistical gate.

`run-debug277/logs/latest.log` passed one required Forge 65.1.0 parkour
physics test in 4.102 seconds with the real headless ServerPlayer. This is
local movement evidence only; it does not establish model-selected parkour or
the formal M1-M4 movement gates.

`run-debug278/logs/latest.log` passed one required Forge
65.1.0 test in 3.345 seconds. A real test human submitted ordinary chat
(`跟我来…`), the server installed the bound goal without a model round trip,
and the headless companion remained under `follow_entity` ownership while
following the moving human; the follow-up `走啊` did not replace the goal.
This is controlled chat/body evidence, not the external live-model or formal
M1-M4 gate.

## 2026-08-10 Current Forge 65.1.0 Nether blaze reserve retest

`run-debug279/logs/latest.log` passed one required `real_nether_blaze_material_reserve`
GameTest on the current source and Forge 65.1.0 in 44 seconds. The real
headless `ServerPlayer` used the ordinary combat actuator against visible
blazes, consumed weapon durability, produced multiple vanilla blaze-rod item
entities, re-observed the drops through its fair semantic view, and physically
walked into the pickup area until the inventory reserve was satisfied. The log
retains the intermediate target health, line-of-sight, movement input, item
drop, and final completion evidence. This is a controlled no-model M2 component
test; it does not promote natural Nether discovery, live-model chat-to-action,
exact Actor/Observer clients, Hardcore random seeds, or any formal M0-M4 gate.

## 2026-08-10 Damage-direction and emergency escape hardening

`EmergencySurvivalController` previously received a fair directional damage
cue but ignored it in `recentDamageScanLook`, oscillating only around the old
heading. It now turns first toward the direction supplied by the vanilla
damage event, then uses a bounded ±35/±70/180-degree fan; directionless damage
uses four cardinal sectors. This is still first-person perception: no attacker
UUID, hidden position, entity query, or world mutation was added. A second
guard only permits an undirected escape into an adjacent cell when the current
frame contains a recent physical/damage contact and all body/head/support
voxels are freshly observed and safe. Ordinary unknown projectile/hostile
signals continue to refuse blind movement.

The focused JVM suite passed after the change, including the new
`recentDamageScanStartsAtFairAttackerDirection` regression and the existing
unknown-threat no-blind-movement cases. The first Forge 65.1.0 lifecycle retest
(`run-debug280`) honestly failed at the old 100-tick directionless-damage wait;
the subsequent retest (`run-debug281`) passed that stage and reached the End
return, then failed independently at the integrated rolling runtime p95
(`3,642,167 ns > 2,000,000 ns` at tick 6112). Both logs are retained rather
than promoted.

The current Forge 65.1.0 combat pressure retest `run-debug282/logs/latest.log`
passed one required test in 25 seconds. Ten zombies and ten skeletons attacked
the real headless `ServerPlayer`; it stayed alive, moved, damaged multiple
targets, and consumed the ordinary sword through the local emergency lane.
This is honest no-model emergency/PVE evidence only, not model-selected PVP or
a formal M1-M4 gate.

`run-debug284/logs/latest.log` also passed one required Forge 65.1.0
`real_emergency_iron_golem_duel` test in 26 seconds. The embodied player
completed the isolated iron-golem duel through the local fair emergency melee
lane. This is the requested PVE/PVP-like combat component evidence, but it is
not a model-selected duel, multiplayer PVP, or a formal M1-M4/statistical gate.

## 2026-08-10 Post-fix package and static audit

`run-debug283-package-after-damage-fix.log` passed
`check jar jarJar verifyReleaseJar e2eClientJar e2eOracleJar` on Forge
65.1.0/JDK 25. The installable product is
`build/libs/mcai_companion-0.1.5-dev-mc26.2.jar` with SHA-256
`d2c62be5fb1a8526ac8c1763def2bd639e5191c6afcd34a8532617eed8de1323`; the
audit-only slim artifact is isolated under `build/audit-libs` with SHA-256
`956322c4b0b32a77b188ce727b1e5766534ba1907923b276a45496510418a1e2`.
The Python audit suite passed all 20 tests and the Forge compatibility schema
validator passed (published Forge 65 patches 65.0.0–65.0.9 and 65.1.0;
formal runtime matrix remains incomplete).

`run-debug285/logs/latest.log` passed the current-source ordinary-chat follow
regression on Forge 65.1.0 (1/1 in 20 seconds). A real test human's Chinese
`跟我来…` message was acknowledged, installed as a server-bound goal, and the
headless body followed through vanilla movement; the subsequent `走啊` nudge
did not cancel or replace the goal. Packet-send warnings are expected from
the no-client GameTest harness and are not used as functional evidence.

`run-debug287/logs/latest.log` passed one required Forge 65.1.0
`zero_human_dedicated_server_chunk_and_respawn` test in 27 seconds. With no
human player online, the server auto-spawned the embedded ServerPlayer, kept
remote chunks and entities ticking through the vanilla player ticket, and
completed the ordinary death/respawn lifecycle before clean removal. This is
zero-human lifecycle evidence, not live-model behavior or a formal M0–M4 gate.

`run-debug286/logs/latest.log` passed one required Forge 65.1.0
`real_emergency_enderman_defense` test in 30 seconds. The body completed the
bounded first-person Enderman defense lane without relying on eye-target
access or hidden entity state. This is local no-model self-defense evidence,
not model-selected combat or a formal M1-M4 gate.

`run-debug288/logs/latest.log` passed one required Forge 65.1.0
`real_parkour_course` test in 28 seconds. A real headless `ServerPlayer`
completed the bounded jump/collision course with ordinary movement physics
and verified landings. This is useful body-physics evidence for parkour and
clutch work, but it is a no-model preset route and does not establish
model-selected traversal, arbitrary terrain navigation, or a formal M1–M4
gate.

`run-debug289-water-clutch-gate.log` passed one required Forge 65.1.0
`real_water_clutch` test in 1.860 seconds. The real headless body used the
production emergency lane to deploy water during a fall and completed the
vanilla landing without fall damage. This demonstrates a fair local
self-rescue actuator, not model-selected timing, arbitrary terrain
navigation, or a formal M1–M4 gate.

The integrated p95 retest `run-debug290-lifecycle-p95-retest.log` is retained
as historical evidence of the earlier performance failure after the
End-return/victory stage: rolling p95 was `3,197,958 ns` at tick 6488, above
the `2,000,000 ns` target, with average `738,791 ns`. The newer `run-debug308`
full lifecycle run supersedes that result with rolling p95 `1,272,625 ns`; no
M0–M4 claim is upgraded by either controlled run.

## Codex recovery checkpoint — 2026-08-10 current turn

Root cause addressed: the emergency damage cue already contained a fair
 attacker direction, but the controller ignored it and scanned only near the
 old heading; directionless environmental damage also had no legal escape
 branch. A follow-up failure showed that a short vanilla knockback left the
 body airborne, so the bounded directionless separation was incorrectly
 gated on `onGround`. Changed files are
`src/main/java/dev/mcai/companion/skills/core/EmergencySurvivalController.java`,
`src/test/java/dev/mcai/companion/skills/core/EmergencySurvivalControllerTest.java`,
`docs/progress/GOAL_STATE.json`, and this checkpoint. The latest retained
failure was `run-debug291`, where the End portal search remained under
repeated directionless crystal damage; `run-debug308` now passes the full
integrated lifecycle and p95 gate after the airborne separation fix. The
focused combat, follow, package, and Python audits now pass. Next work is a
fresh live-model Actor/Observer black-box attempt only when a valid credential
is available; until then, continue current-source physical component coverage
and keep the formal M0–M4 statuses `NOT_RUN` rather than upgrading controlled
tests.

## Codex recovery checkpoint — 2026-08-11 current turn

Root cause addressed in the current farming regression: the crop maintenance
atom could take a long direct pickup input through a corridor whose first-person
map had not yet proved every body/support voxel. On the compact irrigated field,
that let the headless player enter the one-block water source and issue a
vanilla jump; Forge's `canTrample` is independent of sneak, so the landing could
turn a just-replanted farmland block into dirt. A second compatibility failure
on Forge 65.0.0 came from an older semantic sample publishing an authorized
crop's hidden support as AIR, which made the final substrate approach time out.

Changed production files:

- `src/main/java/dev/mcai/companion/skills/core/MoveToSkill.java`
  (existing farming sneak/airborne safety path retained);
- `src/main/java/dev/mcai/companion/skills/farming/MaintainObservedCropFieldSkill.java`
  (farming movement uses sneak and parent checkpoints retain child diagnostics);
- `src/main/java/dev/mcai/companion/skills/farming/HarvestAndReplantStepSkill.java`
  (complete non-liquid direct-corridor evidence, bounded authorized AIR-support
  compatibility branch, water/field diagnostics and safe route checks);
- `src/main/java/dev/mcai/companion/navigation/PerceptionNavMapper.java`
  (observed liquid is never upgraded to SOLID by edge-straddling body contact).

Focused real Forge evidence after the changes:

- Forge 65.1.0: compact wheat, carrot, potato, beetroot and offset-water
  expanded wheat each passed 1/1 in fresh isolated GameTest servers; the strict
  wheat rerun that previously failed now passed.
- Forge 65.0.0: compact wheat, carrot and beetroot each passed 1/1 in fresh
  isolated servers. The first 65.0.0 run retained the real support-visibility
  timeout; the compatibility branch fixed it without relaxing liquid rejection.
- `./gradlew test --no-daemon`: PASS; `./gradlew build --no-daemon`: PASS,
  including compatibility checker and `verifyReleaseJar`.
- `python3 -m unittest discover -s e2e -p 'test_*.py' -v`: 44/44 PASS.
  `pytest` was not available on this host, so no pytest result is claimed.
- Current installable artifact is
  `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`, SHA-256
  `1c8ea9740091e3385f53c61adf9e7b3ffcd4e44d15edb5389e65320d313cba7d`,
  17,136,675 bytes. The worktree remains `DIRTY_NO_COMMIT`; artifact status is
  `NON_RELEASE`.

The selector wildcard was also tested once and failed because five GameTests
shared one server/body lifecycle concurrently; that is an orchestration failure,
not a farming result. All functional farming evidence above uses one exact
selector per fresh server. No provider token was read or sent during this turn.

Last failed formal gates remain unchanged: no current valid provider
configuration on this Darwin host, no Linux/Xvfb Actor/Observer client worker,
and no real model/client/Hardcore M0–M4 archive. Those gates remain `NOT_RUN`;
these no-model physical tests do not establish autonomous survival, PVP, or the
two-hour random-seed completion target. Next step is to preserve this artifact
and continue isolated physical regressions, then rerun the real preflight and
Actor/Observer flow only with an externally supplied valid configuration.

The follow-up physical regressions also completed on fresh Forge 65.1.0 servers:
`real_player_chat_to_immediate_bound_follow`,
`real_emergency_zombie_skeleton_horde`, and `real_parkour_course` each passed
1/1. Logs are `/tmp/mcai-followup-real_player_chat_to_immediate_bound_follow.log`,
`/tmp/mcai-followup-real_emergency_zombie_skeleton_horde.log`, and
`/tmp/mcai-followup-real_parkour_course.log`; the isolated run directories are
`/tmp/mcai-followup-real_player_chat_to_immediate_bound_follow-IRnsRN`,
`/tmp/mcai-followup-real_emergency_zombie_skeleton_horde-kBvx8J`, and
`/tmp/mcai-followup-real_parkour_course-moAoKz` (the middle directory name is
the fresh directory printed by the runner). These are still no-model physical
component checks, not evidence for model-controlled chat, PVP, clients, or
Hardcore/random-seed M0–M4 gates.

The requested no-player-server coverage was rerun with the exact registered
selectors on Forge 65.1.0: `zero_human_dedicated_server_chunk_and_respawn`
passed 1/1 in 1.759 s (run directory
`/tmp/mcai-final-zero_human_dedicated_server_chunk_and_respawn-mJwYkb`), and
`auto_presence_on_human_login` passed 1/1 in 605.0 ms (run directory
`/tmp/mcai-final-auto_presence_on_human_login-oksmvs`). The earlier attempted
`real_zero_human...` selector was not a product failure; it matched no test and
exited before execution. These remain no-model lifecycle evidence.

The GameTest selector guard was corrected so the no-model
`real_offline_critical_golden_apple` test is not classified as live-model;
`build.gradle` now excludes the explicit `offline_critical_golden_apple`
selector from that guard. After the fix, fresh Forge 65.1.0 runs passed
`real_offline_critical_golden_apple` 1/1 in 689.9 ms,
`real_emergency_enderman_defense` 1/1 in 1.551 s, and
`real_emergency_slime_defense` 1/1 in 799.1 ms. Full `./gradlew build
--no-daemon` passed, the product hash remained
`1c8ea9740091e3385f53c61adf9e7b3ffcd4e44d15edb5389e65320d313cba7d`, and the
Python unittest audit passed 45/45 after adding the selector regression. No
provider credential was read or sent.

The M0 physical slice was rerun on both required Forge endpoints with fresh
servers. `headless_player_lifecycle_state_and_fair_action` passed 1/1 on
Forge 65.0.0 in 24.70 s and on Forge 65.1.0 in 25.34 s. `real_water_clutch`
passed 1/1 on Forge 65.0.0 in 736.4 ms and on Forge 65.1.0 in 788.2 ms.
The lifecycle logs include ordinary portal return, item collection, hostile
reaction and body-state checks; the water logs include real
PREPARING_WATER/DEPLOYING_WATER/BRACING_FALL transitions. These are controlled
no-model M0 component checks, not real-client or formal M0–M4 completion.

The current source was then exercised against every published Forge 65.x patch
with the exact selector `mcai_companion:zero_human_dedicated_server_chunk_and_respawn`.
All eleven isolated runs passed 1/1: 65.0.0 (`/tmp/mcai-patch-65.0.0.log`,
`/tmp/mcai-patch-65.0.0-tTwBKF`), 65.0.1 (`/tmp/mcai-patch-65.0.1.log`,
`/tmp/mcai-patch-65.0.1-sn1Dj1`), 65.0.2 (`/tmp/mcai-patch-65.0.2.log`,
`/tmp/mcai-patch-65.0.2-2GpAYX`), 65.0.3 (`/tmp/mcai-patch-65.0.3.log`,
`/tmp/mcai-patch-65.0.3-rNyxd1`), 65.0.4 (`/tmp/mcai-patch-65.0.4.log`,
`/tmp/mcai-patch-65.0.4-vz0y4B`), 65.0.5 (`/tmp/mcai-patch-65.0.5.log`,
`/tmp/mcai-patch-65.0.5-BintWA`), 65.0.6 (`/tmp/mcai-patch-65.0.6.log`,
`/tmp/mcai-patch-65.0.6-spsspA`), 65.0.7 (`/tmp/mcai-patch-65.0.7.log`,
`/tmp/mcai-patch-65.0.7-qYH9PC`), 65.0.8 (`/tmp/mcai-patch-65.0.8.log`,
`/tmp/mcai-patch-65.0.8-UJgpFF`), 65.0.9 (`/tmp/mcai-patch-65.0.9.log`,
`/tmp/mcai-patch-65.0.9-XoAZ3i`), and 65.1.0
(`/tmp/mcai-patch-65.1.0.log`, `/tmp/mcai-patch-65.1.0-67BAPx`). This is a
complete current-source lifecycle patch smoke, not the required full
chat/movement/menu/save matrix or a formal M0–M4 result.
## 2026-08-11 continuation: delayed first-human anchor and talk-only SAFE_IDLE guard

- 生产缺口：专用服务器无人在线启动时，AI 会在无真人锚点下经过 40 tick admission grace 进入 `ACTIVE`；首个真人稍后登录时，原路径直接返回 `already_active`，因此 AI 可能留在世界/保存出生点而不是玩家旁边。
- 已改 `CompanionWorldData`、`AiPlayerManager`：SavedData 的 `goal_progress.body_spawn_anchored` 保存放置来源；无真人放置标记为未锚定。首个真人登录只在目标、技能和应急生存均空闲时通过正常断开/`PlayerList.placeNewPlayer`/`SafeCompanionSpawnLocator` 重建一次；繁忙时记住登录玩家并在服务端 tick 重试。没有 gameplay teleport、方块改写或物品生成。
- 已增 `LiveModelChatGameTests.delayedHumanLoginAfterZeroHumanActive`、GameTest instance JSON 和 `CompanionWorldDataTest`：Forge 65.1.1 新鲜目录先无真人运行到 AI ACTIVE，再登录 `TestHuman`，核验稳定 UUID、目标修订号、背包、TAB `[AI]`、同维度安全距离和最终两名玩家；1/1 通过，约 2.444 秒，日志 `/tmp/mcai-delayed-login5-xC0Ak2/logs/latest.log`。
- 生产缺口：模型对 PLAYER_CHAT/MCP/Hardcore 目标返回 `SAFE_IDLE` 时不能把“未执行任务”静默终止。`BrainOrchestrator` 现在抑制未验收 speech，记录 `model_safe_idle_rejected_for_active_goal`/`evaluation_safe_idle_rejected`，普通聊天走有限重规划后等待玩家新指令，恢复类内部目标保留原安全停机语义；`MinecraftBrainEventSink` 对普通目标每个 revision 只广播一次“模型误判为暂停，当前还没有执行动作”。新增 Brain/EventSink 测试通过。
- 定向验证：JDK25 `BrainOrchestratorTest`、`CompanionWorldDataTest`、`PendingPlayerSpawnTest`、`SwitchableModelGatewayTest`、`MinecraftBrainEventSinkSourceContractTest` 通过；完整 `check verifyReleaseJar e2eClientJar e2eOracleJar` 16 actionable tasks、Python E2E 49/49、兼容性检查和 `git diff --check` 通过。当前 JAR `build/libs/mcai_companion-0.1.5-dev-mc26.2.jar`，17,150,533 bytes，SHA-256 `63a9f24c626af96dd6bb17e32dd199c6a13eea85328cc9308b3e28bf564ecec3`。
- 证据边界：上述是当前 Forge ServerPlayer/无模型物理和协议边界证据；真实配置模型、真实 Actor/Observer 客户端、渲染截图、Hardcore 隐藏种子、24/100 小时 soak 与 M0--M4 正式门禁仍 `NOT_RUN`。源码仍 `DIRTY_NO_COMMIT`，工件仍 `NON_RELEASE`。本机无 Linux/Xvfb，也没有可授权的有效模型凭据；不会把旧 MiMo 401 前后的受控 GameTest当正式验收。
