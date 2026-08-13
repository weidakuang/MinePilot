# MinePilot — Minecraft AI Companion

MinePilot 是本项目的公开仓库名；生产 Mod ID 保持为 `mcai_companion`。

开始使用： [使用教程](docs/USAGE.md) · [项目规范](docs/PROJECT_CHARTER.md) ·
[贡献与提交检查](CONTRIBUTING.md) · [安全策略](SECURITY.md)

面向 Minecraft Java 26.2 / Forge 65.x 的可见、无第二账号 AI 玩家 Mod。

> **当前状态：`0.1.9-dev-mc26.2` 工程验证版。** 真实玩家身体、单模型运行链、公平感知、记忆、配置界面、皮肤同步及一批生存技能已经实现并通过单元/受控 GameTest 门禁；但本仓库还没有通过 24 小时稳定性、未知随机种子零干预通关或 M0–M4 正式验收，因此不能称为最终版、两小时速通产品或“专业游戏陪玩”。

完整的逐项证据与未通过项目见 [实施状态](docs/IMPLEMENTATION_STATUS.md)；
针对 Dwinovo/minecraft-numen 的源码、API 与公开缺陷审阅见
[Numen 参考审阅](docs/NUMEN_REVIEW.md)。

## 已实现的运行基础

- 以原版 `ServerPlayer` 作为唯一权威身体，不需要微软/Mojang账号或第二个 Minecraft 客户端。
- 使用世界内持久 UUID，接入原版 `PlayerList`、玩家存档、背包、主副手、护甲、末影箱、生命、饥饿、经验、维度与死亡状态。
- 无客户端连接泵处理 KeepAlive、传送确认、玩家加载确认和出站包释放。
- 挖掘、放置、使用、攻击、装备、合成和菜单事务走原版玩家路径；技能层不直接生成物品、不传送玩家，也不直接改世界结果。
- Java 25 单模型网关支持 Responses 与 OpenAI-compatible Chat Completions、结构化输出降级、single-flight、超时/取消、revision 失效和错误脱敏。
- 模型只作高层决策；移动、格挡、进食、撤退和紧急落地等低延迟动作在服务端 Tick 上运行。
- 20 TPS 身体/危险状态与分频第一人称语义采样；方块和实体目标带视线、距离、遮挡、样本序号及世界/目标 revision 约束。
- SQLite WAL 记忆包含事件、任务 checkpoint、FTS5 名称检索、R*Tree 空间检索、标点与已验证传送门边。
- Xaero 26.4.2/26.4.3 结构化共享标点解析；只消费明确共享坐标，不 OCR、不读取雷达或洞穴图，也不传送。
- 回环 MCP 提供 `observe`、`set_goal`、`goal_status`、`say`、`cancel_goal`、`add_waypoint`、`get_screenshot` 和 `get_audit_summary`，并执行 Bearer、Host、Origin 与请求体限制。

## 已接入的技能范围

当前生产注册表已包含下列能力。这里的“已接入”表示实现了受约束的原子/组合技能，不等于已经通过任意自然世界中的完整高层任务。

- 移动与探索：移动、转向、跟随、滚动走廊规划、已观察目标探索、环境勘察与标点记忆/回忆。
- 公平交互：破坏方块、使用方块/物品、攻击/交互实体、拾取可见掉落。
- 物品与菜单：装备、丢弃、2×2/3×3 配方合成，以及基于已观察槽位的箱子、熔炉、切石机和村民交易事务；`smelt_menu_batch` 可在一次高层决策中通过原版菜单投入原料/燃料、等待真实烹饪并取出指定产物。
- 生存劳动：可见资源簇采集、成熟作物收获与补种、动态庇护所的现场规划和本地连续批量施工、睡床。
- 交通与地形动作：船、矿车、搭桥、垫高、保守跑酷、落地水下降和紧急落地控制。
- 战斗：本地近战、盾牌/撤退节奏、弓弩等远程攻击、由连续可见位置推断的移动目标提前量、受控烈焰人掉落获取。
- 维度与进度：建造并点燃黑曜石下界门、正常进入/返回传送门、末影之眼轨迹记录、已观察末地门激活、受控末影龙战斗和返回主世界。

## 自动化验证现状

2026-07-29 当前代码的一致性回归与定向物理审计：

- 完整 `./gradlew test`：684 项，0 failure、0 error、2 skipped。两个
  skipped 是需要显式安全注入凭据的真实供应商测试；默认测试不访问真实
  API Key。
- `real_water_clutch` 此前清理后连续 3/3 通过：约 5 格主动下降与约 12 格
  紧急下坠都经过原版重力/碰撞，实际放水、产生空桶、增加使用统计并无伤
  落地。它以 Hardcore 风险策略执行，但不是随机 Hardcore 世界统计。
- `real_parkour_course` 在本轮发现两格缺口着陆刹车缺陷后，修复版连续
  2/2 通过：连续三次一格缺口、两格
  缺口、90° 转向、一格上升及第二次转向跳跃均由原版输入/物理完成，
  没有填充缺口或受到伤害。修复使用空中反向输入提前制动，没有传送、
  改位置或放宽落地断言。
- `real_portal_cast_and_light` 已连续三次通过，并在清理诊断代码后再次
  通过：技能用真实桶事务、流体更新和临时模具逐格浇出黑曜石框架，清理
  模具后以打火石激活原版下界门。
- `natural_recipe_unlock_after_log_pickup` 通过，验证 Headless 玩家自然
  拾取木头后取得原版配方解锁。
- `verified_shelter_evidence` 与 `verified_foundation_evidence` 通过，
  验证庇护所几何/门/光照，以及工作台、熔炉、箱子、入箱事务和箱内
  物资均由服务端持久证据复验，设施损坏或箱子清空会撤销完成状态。
- M1 路线现在与紧急控制器共用安全食物目录；熟鲑鱼、南瓜派、蜂蜜等
  不会再被错误忽略，河豚、腐肉和毒马铃薯不会计入安全储备。庇护所经
  服务端复验完成后，路线会停止重复索取结构块、门和光源；若庇护所
  后续损坏，需求会自动恢复。
- `real_furnace_batch` 通过，验证原版熔炉菜单中的原料/燃料转移、
  实际烹饪 Tick、燃料消耗和指定产物取出，不直接读写方块实体产物。
- 最长的 `headless_player_lifecycle_state_and_fair_action` 在本轮改动后
  通过，串行覆盖 Headless 登录/重登、菜单、移动、挖掘、交通、
  搭桥/垫高、跑酷、下界往返、受控烈焰棒、末影之眼、末地门、末地路线、
  受控龙战、`Free the End` 与返回流程。此前发布基线的完整 Forge
  服务端门禁曾在 Forge65.0.0 上约40.85秒内通过当时全部19个 required
  test；当前版本已经注册22个测试，尚未把22项作为一个批次重新跑完。
- 旧文档曾记录过 MiMo 的六项线上模型夹具和基础生存启动夹具；这些是
  **历史、受控运行记录，不是当前源码的正式证据**。当前源码/工件发生过
  多轮动作因果、权限、首登生命周期和安全修复，且最近的供应商能力探测返回
  401；因此它们不能用来宣称当前版本会聊天到动作、自然生存或通关。
  当前正式 live-model Actor/Observer 门禁以 `NOT_RUN` 记录，直到同一冻结
  工件在具备授权模型和真实客户端的隔离环境中重新通过
  `model_response_received → decision_revision_accepted → skill_started →
  low_level_actions_issued` 因果链。无模型 GameTest 只保留为物理/协议下限。
- 本轮新增的定向门禁分别验证：实时测试服40 Tick不会突发快进；零真人
  在线时 AI 之外的邻近区块仍有实体/方块 Tick，且死亡后可按原版重生；
  普通玩家聊天触发的突袭僵尸防御会在12 Tick内产生物理反应并真正击杀；
  危急生命会持续吃完金苹果而不因饥饿条满而取消。突袭战斗的最终修订版
  当前仅完成一次通过，不能表述为稳定成功率。
- Minecraft26.2兼容矩阵此前在Forge65.0.0、用户报告的65.0.8和65.0.9
  上分别通过当时的完整GameTest；该结果是发布基线，不是当前22项测试的
  最新整批结果。发行元数据接受
  `[65.0.0,66.0.0)`，并有自动化测试防止再次退化为单补丁精确锁定。

这些结果证明受控夹具中的动作语义和组合链可运行，不证明自然随机种子
零干预通关、动态满血末影龙、两小时成功率或专业陪玩验收。

## 构建

要求：

- Temurin/OpenJDK 25
- Minecraft Java 26.2
- Forge **65.0.0（含）至66.0.0（不含）**
- macOS、Linux 或 Windows
- 网络可访问 Forge/Maven 依赖仓库

Forge 是启动器实例的加载器版本，不能把 Forge 安装器当作普通 Mod
丢进 `mods` 目录。此前发布基线已分别在Forge65.0.0、65.0.8和65.0.9上
通过当时的完整GameTest；当前新增22项整批尚未重跑。元数据接受全部
65.x补丁版本。错误页中的`minecraft`和
`forge`是同一个`mcai_companion`的两项依赖，不是两个额外模组。
Minecraft26.1.x/Forge64.x需要单独的旧版构建，不能靠放宽元数据安全
跨越Minecraft版本。

```bash
export JAVA_HOME=/path/to/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
```

可安装的单一 JAR：

```text
build/libs/mcai_companion-0.1.9-dev-mc26.2.jar
```

构建脚本会在写入前把 `build/libs/mcai_companion-*.jar` 的旧版本移动到可恢复的
`build/archive-libs`，并把不可安装的 slim 审计中间产物放到
`build/audit-libs/mcai_companion-0.1.9-dev-mc26.2-slim.jar`。因此把
`build/libs` 目录内容复制到 `mods` 时只会得到一个 `mcai_companion` 模组；
不要把 `build/audit-libs` 复制进去。JAR 通过 Forge Jar-in-Jar 内含
SQLite JDBC；Xaero 适配使用结构化共享标点协议，不会把 Xaero 或第二个模组
打包进来。

本地验证：

```bash
./gradlew test
./gradlew runGameTestServer
```

`runGameTestServer` 是 Forge 提供的专用无窗口游戏测试服，不需要启动器、
客户端画面或鼠标点击；失败数会成为进程退出码。项目在其中创建真实
`ServerPlayer`、通过 Forge 的玩家聊天事件发送任务，并断言原版世界、
背包、实体、物理和统计结果。已经安全配置凭据且确实要消耗真实模型
Token时，可额外运行：

```bash
./gradlew runGameTestServer \
  -Plive_model_test=true \
  -Prealtime_gametest=true
```

`realtime_gametest` 会让专用测试服按真实20 TPS推进，并由独立时钟
契约检查40 Tick确实耗时约2秒。Forge的构建测试服通常会尽快推进Tick，
若省略此参数，外部模型等待期间可能经过数万游戏Tick，使掉落物消失或
作物生长，不能作为长延迟模型工作流的有效验收。

这能自动验证聊天→真实模型→生产规划器→游戏动作。ESC菜单布局、按钮
触感、两个客户端看到的皮肤/动画等客户端视觉问题仍需要人工验收，不能
由无渲染服务端伪装成已验证。

标准开发专用服还需由你自行确认 Mojang EULA 后运行：

```bash
./gradlew runServer
```

## Agent 设置中心

加入世界后按 `Esc`，点击与“回到游戏”同列的“AI 陪玩”；也可以从
Mods 列表进入。设置表单在较高 GUI 缩放或较小窗口下可滚动，底部
“保存并验证”和“返回”固定在表单外，不会再被多行输入框遮挡。
设置中心全部使用 Minecraft 原版按钮、文本框、列表、滑块和提示文本，
提供：

- 服务端权威校验的 Agent 名称：3–16位，只允许英文字母、数字和下划线；
- Agent 配色，以及 `0.0–1.0` 的真实模型采样温度滑块；
- 本地 64×64 PNG 皮肤拖放入口和经典/纤细手臂选择；
- API Key、Base URL、Model Name；
- 不超过4,096字符的 Agent 系统偏好；
- 第一次进入时的四步新手引导和验证后的首次普通聊天交互；单人世界
  无需 `@mcai`，多人世界可用名称明确指定 Agent。

专用服务器上的普通非 OP 玩家若要发起游戏任务，需要管理员在生成的
`config/mcai-companion.toml` 的 `chat.allowedSenders` 中填写该玩家 UUID；
该白名单只授予游戏任务入口，不授予 `/mcai` 管理或评测命令。留空时单人
所有者和服务器管理员仍可使用，其他玩家的聊天仍可被观察但不会安装目标。

温度滑块会作为真实的采样 `temperature` 随每次 Responses 或 Chat
Completions 请求发送；系统偏好仍不能覆盖公平游玩、安全、权限或极限
评测规则。当前
`0.1.9-dev-mc26.2` 仍只有一个活动 Agent 身份；真正多 Agent 必须为每个实例
提供独立 UUID、身体、凭据槽、记忆、目标和控制循环，不能用共享单例
伪装成多 Agent，因此仍属于 M3 未完成项。

“Save & verify”会在服务端应用配置并执行能力握手。远程 Base URL 必须为 HTTPS；HTTP 只允许回环测试地址。远程多人连接还要求加密传输和管理员权限。

API Key 不写入 TOML、世界、SQLite、日志、崩溃报告或截图。持久存储按
服务端实际运行平台选择：

如果使用 Xiaomi MiMo Token Plan，请确认凭据仍在有效期内、Base URL 与订阅页面一致，
并遵守供应商的使用范围：官方说明 Token Plan 仅用于编程工具场景，禁止把它用于明显的
非编程自动化脚本或自定义后端；本 Mod 的真实模型门禁不会用被拒绝的 Token Plan
冒充可用陪玩。请改用供应商明确授权的 API/凭据后再运行真实模型测试。详见
[MiMo API integration FAQ](https://mimo.mi.com/docs/en-US/quick-start/faq/api-integration)
和 [Token Plan usage terms](https://mimo.mi.com/docs/tokenplan/subscription)。

- macOS：用户 Keychain；
- Windows / Windows Server：当前 Windows 用户作用域 DPAPI，实例
  `config/mcai-companion/credentials/` 中只保存 DPAPI 密文；服务重启后
  必须继续使用同一个 Windows 服务账号；
- Linux 桌面：安装了 `secret-tool` 且登录会话提供 Secret Service 时，
  使用系统密钥环；
- Debian/Ubuntu 等无桌面的专用服务器：使用 `MCAI_API_KEY_FILE`、
  systemd `LoadCredential=mcai-api-key:...`，或容器只读 Secret；Mod 会
  自动读取 `$CREDENTIALS_DIRECTORY/mcai-api-key`；
- `MCAI_API_KEY` 仍可作为进程环境回退，但更推荐只读 Secret 文件，
  避免密钥进入服务环境转储。

明确提供的 `MCAI_API_KEY_FILE`、systemd Secret 或 `MCAI_API_KEY` 会优先于
旧的 Keychain/DPAPI/Secret Service 项目，用于无交互轮换凭据；未提供注入时
才恢复平台安全存储。这样服务器不会继续使用旧密钥遮蔽管理员刚轮换的密钥。

设置界面的“保存并验证”会优先写入重启安全的 Keychain、DPAPI 或 Secret
Service。若当前平台没有可用安全存储，验证仍可在本次进程完成，但状态会明确显示
`saved_verified_process_restart_required`；这不是重启安全保存。此时请在下次启动前使用上面的
`MCAI_API_KEY_FILE`/systemd Secret 注入（或 `MCAI_API_KEY`）恢复凭据，避免再次在界面输入。

```bash
export MCAI_API_KEY_FILE='/run/secrets/mcai-api-key'
```

systemd 专用服示例（密钥源文件应仅允许管理员读取）：

```ini
[Service]
LoadCredential=mcai-api-key:/etc/mcai-companion/api-key
```

保存按钮在没有可用系统安全存储时只保留当前进程副本，并在服务端内部记录
`process_only_secure_store_unavailable`、向界面显示
`saved_verified_process_restart_required`；它不会为了“记住密钥”而偷偷创建明文文件。

常用开发命令：

```text
/mcai embodiment spawn
/mcai embodiment status
/mcai embodiment remove
/mcai goal status
/mcai model status
/mcai model probe
@mcai <高层目标>
```

极限评测入口：

```text
/mcai evaluation start
```

评测入口会固定“通关 Minecraft”目标并锁定后续聊天目标、MCP 写工具、新标点和模型配置。它实现的是审计约束；在隐藏随机种子统计完成前，不能把该命令解释成已具备可靠自动通关能力。

## 自定义皮肤

可在 Agent 设置中心打开皮肤页，把一个不超过 1 MiB、带透明通道的
现代 64×64 PNG 拖到窗口。也可以手动放到：

```text
config/mcai-companion/skin.png
```

然后按手臂模型执行：

```text
/mcai skin reload classic
/mcai skin reload slim
/mcai skin status
/mcai skin clear
```

服务端按 SHA-256 缓存并分块同步到安装了本 Mod 的客户端。缺失、损坏、尺寸错误或摘要不匹配时，会依据 AI UUID 稳定回退到史蒂夫/艾利克斯及对应手臂模型。协议、缓存和客户端解析已有自动化测试；两个真实客户端同时观察全部动画的人工门禁尚未完成。

## Codex MCP

在 `mcai-companion.toml` 中启用：

```toml
[mcp]
enabled = true
port = 25766
```

给 Minecraft 与 Codex 进程配置同一个高熵本机令牌：

```bash
export MCAI_MCP_TOKEN='至少32位URL安全随机字符'
.agents/skills/minecraft-companion/scripts/configure-mcp.sh
```

MCP 只绑定回环地址。`get_screenshot` 当前会明确报告视觉捕获不可用，因为尚无经过认证和脱敏验证的客户端捕获通道；语义感知不会假装看过截图。

## 尚未通过的发布门禁

- 24 小时 Headless 稳定性与 100 小时长期记忆/性能审计。
- 一个初始命令、此后零干预的自然随机种子 Hardcore 基础生存和完整通关。
- 自然地形中的堡垒搜寻、烈焰棒/珍珠路线、要塞与末地门房搜寻，以及会飞行、俯冲、栖息的动态满血末影龙。
- 100/200/1,000 个隐藏种子统计，因此没有“两小时 ≥95%、六小时 ≥99%”证据。
- M3 的完整建筑、全部原版工作站、红石装置、农场、PVE/PVP 和长期陪玩场景矩阵。
- M4 速通优化及盲测指标；M5 的 Create、MTR、农夫乐事等已验证专用适配器。
- 已认证、脱敏的第一人称截图通道和视觉模型能力测试。

在这些门禁完成前，请把本仓库视为功能广泛的工程验证版，不要用于宣称最终交付或专业陪玩服务。

## 许可证

本项目自有代码采用 [Apache-2.0](LICENSE)。第三方依赖说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
