Minecraft AI Companion — Codex 单一持久目标：连续完成 M1–M4 与真实黑盒测试

最后修订：2026-07-29目标仓库：/Users/weida/Documents/minecraft-ai-companion-forge产品名：Minecraft AI CompanionMod ID：mcai_companion

使用方法：把本文件放进目标仓库根目录，命名为 CODEX_GOAL_M1_M4.md，然后在 Codex CLI 中执行下面这一条 /goal。Codex 必须先完整读取本文件，再开始工作。

/goal In /Users/weida/Documents/minecraft-ai-companion-forge, continuously research, design, implement, integrate, test, harden, and validate Minecraft AI Companion through the prerequisite player-body bootstrap and all of M1, M2, M3, and M4 without stopping for milestone approval, until one frozen release commit passes every mandatory real dedicated-server, real offscreen-client, chat-to-action, movement, inventory, crafting, navigation, expert-companion, technical-Minecraft, natural-action, endurance, security, performance, and hidden-seed gate defined in CODEX_GOAL_M1_M4.md; stop early only for a proven external blocker that cannot be resolved with the available repository, permissions, credentials, infrastructure, and configured spending limits, after completing every non-blocked task and recording a minimal reproduction plus complete evidence.

1. 目标性质与“一次性完成”的准确含义

这是一个单一、持续、可恢复的工程目标，不是一轮普通问答，也不是让你一次输出几千行未经运行的代码。

“一次性完成 M1–M4”在本项目中指：

只设置一个持久目标。

允许你在内部制定计划、提交代码、创建工作树、运行实验、失败、修复、重构和重新评测。

M1 通过后自动继续 M2；M2 通过后自动继续 M3 与 M4；不得在每个里程碑结束时停下来等待用户再次说“继续”。

上下文压缩、进程重启或 Codex 会话恢复后，必须从仓库中的持久进度与证据继续，而不是重新猜测项目状态。

不能把“创建了接口”“编译通过”“写了测试但没运行”“完成了一个演示世界”当作完成。

最终停止条件只有两个：

FINAL_PASS：同一个冻结发布提交真实通过本文件要求的全部 M1–M4 门禁；

BLOCKED_EXTERNAL：存在你无法自行消除的真实外部阻断，并且有最小复现、日志、精确缺失条件和恢复命令。外部阻断不允许被改写成 PASS。

不要因工程规模很大而主动缩小目标。不要在 M1、M2 或一个能走路的演示版结束。不要靠堆空架构、TODO 和模拟测试制造进度。

本目标不授权你购买云资源、创建付费账号、绕过许可证、泄露密钥或无限制消耗模型费用。你可以使用已经配置并明确允许的本地资源、CI、云执行器、API 凭据和预算上限。缺少必要资源时，完成所有不依赖它的工作，然后按本文件的阻断规则报告，不得伪造统计结果。

2. 最终产品定义

从零完成一个可发布的 Forge Mod，使 Minecraft 世界中存在一个真正可长期游玩的 AI 伙伴。它必须：

不需要单独的 Microsoft/Mojang 游戏账号；

以稳定 UUID 和 ServerPlayer 语义存在于服务端世界；

能被安装本 Mod 的真实客户端看见；

具有正确皮肤、头部、姿态、装备、主副手和动画；

具有原版玩家语义的背包、护甲、饥饿、生命、经验、效果、统计、末影箱、死亡、重生点和维度状态；

能理解玩家从真实聊天栏发送的中文和英文消息，并通过真实游戏聊天通道回复；

对可执行请求不仅说话，还必须启动技能并在世界中产生可验证的合法结果；

能正常走路、跑步、跳跃、游泳、攀爬、乘船、乘矿车、开门、通过传送门、避障和脱困；

能正常查看与管理自己的背包，通过真实菜单事务使用箱子、工作台、熔炉、酿造台、附魔台、铁砧、锻造台和村民交易；

能采集、合成、冶炼、战斗、建造、整理仓储、制作生电/红石自动化、建立农场和维护长期基地；

能作为专业陪玩跟随、合作、交流、记忆、服从权限和尊重玩家建筑；

能在真正未知随机种子的 Hardcore 世界中，仅接收一次“通关 Minecraft”的初始命令后零人工干预地完成游戏；

动作连续、合法、目的明确，不瞬移、不机械抖动、不通过直接改世界作弊，并在普通观察者视角下接近熟练真人玩家；

在断网、模型超时、模型非法输出、世界变化、技能失败和重启后安全恢复；

运行时只使用一个高层语言模型。实时运动、格挡、瞄准、攻击节奏、进食、避险和技能执行由本地 20 TPS 控制器完成；

同一仓库维护共享核心，并为每条受支持的 Minecraft/Forge 主线各发布一个独立 Forge Mod JAR。每个运行环境仍只需安装一个对应版本的产品 JAR；测试系统可以有独立测试 JAR、客户端测试 Mod 和编排器，但它们不得进入产品 JAR。禁止为了表面上的‘单 JAR 跨大版本’而依赖脆弱反射、吞掉类加载错误或放宽实际兼容测试。

本文中的“深电”按“生电，即技术生存、红石自动化、农场和机器”理解。

3. 固定技术基线、Forge 65+ 兼容策略与开工时复核

开工时必须联网复核并写入 docs/research/FACTS.md 与 compat/forge-lines.toml。本项目的兼容下限是 Forge 65.0.0，并要求覆盖所有已经正式发布的 Forge 65.x、Forge 66.x 以及后续主版本。

截至本目标修订日，官方状态为：

当前主 Minecraft 版本：26.2

当前 Forge Recommended / Latest：65.1.0

最低强制兼容 Forge：65.0.0

当前必须覆盖的 Forge 65.x：官方仓库中所有已发布的 65.0.0 至当前最新 65.x

Forge 66：本目标修订时尚未正式发布；不得伪造 66 兼容结果

Java：Temurin JDK 25

Gradle：每条 Minecraft/Forge 主线使用对应官方 MDK Wrapper，并锁定校验和

自有代码：Apache-2.0

模型接口：Java 25 HttpClient，不依赖外部 Node/Python 守护进程作为产品运行前提

数据库：Xerial SQLite JDBC 的开工时最新稳定版本，经许可证、Maven Central 签名、三平台原生库和 Forge Jar-in-Jar 验证后锁定

Xaero 当前主回归：Minecraft 26.2 / Forge 对应的 26.4.3；每条未来 Minecraft 主线动态选择官方可用构建

“兼容 Forge 65.0.0–66 及以上”必须按以下方式实现：

Forge 65.x / Minecraft 26.2：这是当前最低兼容线。forge65 适配模块原则上以 65.0.0 API 下限编译，避免无意调用后续 patch 才出现的接口；产品元数据在核对当前 MDK 的 Maven 版本范围语法后声明类似 [65.0.0,66.0.0) 的 Forge 范围和对应 Minecraft 26.2 范围。该 JAR 必须至少在 65.0.0 和当前 Recommended 上执行完整真实 E2E；每个官方已发布 65.x 都必须执行启动、加载、GameTest、聊天、移动、菜单、保存和重启回归。

Forge 66.x 及后续主版本：Forge 主版本通常伴随 Minecraft、映射或 API 变化。每个正式发布的主版本建立独立版本适配模块和对应产品 JAR，共享模型、记忆、规划、技能语义和测试场景；例如 Forge 66 模块以 66.0.0 为 API 下限，并在验证后声明 [66.0.0,67.0.0) 类似范围。不得宣称一个按 65 映射编译的 JAR 能未经测试直接跨到 66。

完成时的动态集合：最终验收覆盖“冻结提交时官方已经发布的全部 Forge 主版本 >=65”。尚未发布的未来版本不计入当前 PASS，但必须有版本发现工作流；一旦官方出现新主版本而仓库没有适配模块，兼容 CI 和发布流程立即失败，禁止继续宣称“65+ 全兼容”。

每条运行环境一个 JAR：用户在某个 Minecraft/Forge 版本上只安装一个产品 JAR；仓库可以生成 mcai_companion-<modVersion>-mc26.2-forge65.jar、未来 ...-forge66.jar 等。不要制造跨 Minecraft 映射的万能胖 JAR。

同等功能：不能把 66+ 适配降级成只能加载。聊天、移动、背包、菜单、M1–M4、MCP、SQLite、Xaero 和安全门禁必须保持功能等价；版本差异必须在 CAPABILITY_MATRIX.md 中精确记录。

数据升级：世界 SavedData、SQLite、身份 UUID、任务、标点、资产和机制规格必须有跨版本迁移测试。不得依赖数值注册表 ID；持久化使用稳定资源标识、schema 版本和显式迁移。

创建 compat/forge-lines.toml，至少描述：

[[line]]
forgeMajor = 65
minecraft = "26.2"
minimumForge = "65.0.0"
recommendedForge = "65.1.0"
module = "forge65"
status = "required"

开工时从 Forge 官方下载页/Maven 元数据重新生成候选矩阵，再由锁文件固定。正常构建不得悄悄漂移版本。

如果官方信息变化：

记录查询时间、来源、MDK 哈希、Minecraft 版本、Forge 主/次版本和 Java/Gradle 要求；

更新 compat/forge-lines.toml 与 ADR；

新 Forge patch 先加入回归矩阵；

新 Forge major 建立新适配模块并执行完整门禁；

不得仅把 mods.toml 的 loaderVersion 范围写宽后宣称兼容。

4. 研究与参考边界

先研究，后实现。优先级为：

每条已发布 Forge >=65 主线对应 Minecraft 版本的实际反编译映射源码、MDK、编译和运行行为；

Forge 官方源码、官方文档、MDK 和 GameTest；

Minecraft 官方行为和协议；

公开论文与许可清晰的开源项目；

其他模组的公开功能描述，仅用于发现需求和失败模式。

可以参考但不得直接照搬的方向：

Mineflayer：实体/方块跟踪、移动、载具、背包、合成、容器、挖掘、建造和聊天的能力拆分；

mineflayer-pathfinder：静态、动态与组合目标，A*、破坏/放置成本、危险排除和增量重规划；

Voyager：自动课程、可复用技能库、环境反馈、失败复盘和自验证；

JARVIS-1：多模态规划、目标控制器和经验记忆；

MineDojo：可程序评分任务、创意任务和完整通关任务的评测分类；

OpenAI VPT 与 BEDD：真人输入轨迹、画面—动作数据和人类偏好评测方法；

HeadlessMC 与 mc-runtime-test：真实 Minecraft 客户端在 CI、无显示设备和 Xvfb 中运行；

nekostulAICompanion、Nova Quantum、Core Companion：结构化意图、任务链、长期伙伴与生存层的公开设计；

Mindcraft：LLM 生成并执行代码会产生提示注入和主机执行风险；本项目不得采用该危险路径。

禁止：

复制 Baritone、Mineflayer、AltoClef、Xaero、作弊客户端或其他项目的受限代码、资源、映射、提示词和资产；

反编译 ARR 模组后复制内部实现；

把外部协议机器人直接作为产品 AI 身体；

让运行时模型生成、编译或执行 Java、JavaScript、Shell、Minecraft 命令或任意代码；

以其他项目宣传页面的自述当作本项目功能已被证明。

每个依赖和参考项目必须记录许可证、使用方式和是否进入发布物。

5. 持久仓库状态与连续执行

创建并持续维护：

AGENTS.md

PLANS.md

README.md

LICENSE

NOTICE

SECURITY.md

CODEX_GOAL_M1_M4.md

docs/PRODUCT_SPEC.md

docs/ARCHITECTURE.md

docs/TEST_ARCHITECTURE.md

docs/TEST_MATRIX.md

docs/CAPABILITY_MATRIX.md

docs/PROMPT_CONTRACT.md

docs/FAIR_PLAY_POLICY.md

docs/RISK_REGISTER.md

docs/research/FACTS.md

docs/progress/GOAL_STATE.json

docs/progress/DECISIONS.log

docs/verification/M0-BOOTSTRAP.md

docs/verification/M1.md

docs/verification/M2.md

docs/verification/M3.md

docs/verification/M4.md

docs/verification/FINAL.md

docs/adr/

e2e/orchestrator/

e2e/client-mod/

e2e/oracle-mod/

e2e/scenarios/

e2e/fixtures/

e2e/human-traces/

e2e/results/

benchmarks/

skills/minecraft-companion/

GOAL_STATE.json 至少记录：

当前阶段与子门禁；

当前 Git commit；

最近成功测试 commit；

PASS / FAIL / BLOCKED / NOT_RUN；

所有失败用例；

下一条精确命令；

API、云执行器、预算或人工数据等外部依赖状态；

证据目录；

上下文恢复说明。

每次上下文压缩、会话恢复或长测试结束后，先读 AGENTS.md、本文件、GOAL_STATE.json、PLANS.md 和最近日志。不要重新开始项目。

每完成一个内部门禁可以提交和打标签，但不得因提交了 M1 或 M2 而结束目标。自动进入下一阶段。

6. 必须先通过但不作为停止点的 M0 内部门禁

用户要求一次性完成 M1–M4，但这些阶段依赖真实玩家身体。M0 作为内部技术前提必须先通过，随后自动继续 M1，不得停下来请示。M0 必须先在 Forge 65.0.0 与当前 65.x Recommended 上通过；每个正式发布的 Forge 66+ 主线在宣称兼容前也必须独立通过该门禁。

实现 AiServerPlayer extends ServerPlayer 与完整的无客户端生命周期。Forge 的 makeMockServerPlayer 只能证明可以构造 ServerPlayer + Connection + EmbeddedChannel + ServerGamePacketListenerImpl，不能证明产品级长期在线。

必须验证：

稳定 UUID 和 GameProfile；

正确加入/移除 PlayerList；

实体追踪、区块追踪与玩家列表一致；

HeadlessConnectionPump 正确处理需要回应的协议状态；

Keepalive、传送确认、菜单状态、断开与关闭；

出站包被消费且队列有界；

保存、重启、死亡、重生、下界、末地和返回；

两个真实客户端同时看见正确玩家、装备、皮肤和动作；

24 小时无连接包增长、幽灵实体、玩家列表残留和资源泄漏；

关键玩家动作与真人客户端的差分测试；

SQLite 单 JAR 加载、迁移、WAL、FTS5、R*Tree、损坏检测和跨世界隔离。

如果完整 ServerPlayer 语义不可行，必须 BLOCKED_ARCHITECTURE 并停止虚假后续开发。不得改用普通 Mob、ArmorStand 或 PathFinderMob 冒充。

7. 生产架构

7.0 多版本仓库与发行结构

使用单一仓库和多项目构建，建议逻辑结构：

:common-model
:common-memory
:common-planner
:common-skills
:common-audit
:forge-65
:forge-66              # 仅在官方 66 发布后建立
:e2e-common
:e2e-forge-65
:e2e-forge-66          # 同上
:compat-checker

共享模块不得直接依赖某一 Minecraft 映射中的客户端/服务端类。版本模块负责：

ServerPlayer、连接和生命周期；

事件总线；

网络注册；

菜单事务；

注册表、配方和标签；

SavedData；

客户端渲染和皮肤；

当前版本的动作适配；

Xaero 适配；

GameTest 和真实客户端测试入口。

定义稳定的内部端口，例如 GameRuntimeAdapter、PlayerActionAdapter、MenuAdapter、RegistryAdapter、RenderAdapter 和 VersionCapabilities。版本模块只能通过这些端口接入共享规划与技能。

compat-checker 必须：

查询并锁定官方已发布 Forge >=65 主版本；

检测新主版本；

验证每个主版本存在适配模块、产品 JAR、测试客户端 JAR和真实测试结果；

检查发布 JAR 的 Minecraft/Forge 版本声明与实际编译线一致；

阻止把 forge65 JAR 发布成 Forge 66 兼容；

输出机器可读兼容清单和人类可读支持矩阵。

同一功能可以通过版本适配器实现差异，但不允许复制整套业务逻辑到每个版本后逐渐分叉。共享代码比例、重复代码和 API 差异需要持续审计。

7.1 分层

每条 Minecraft/Forge 主线各自产出一个产品 JAR；每个版本模块内部仍按职责拆分：

bootstrap

player

connection

lifecycle

input

action

perception

dialogue

goal

planner

skill

navigation

combat

building

automation

memory

model

mcp

compat.xaero

client.skin

audit

config

testkit，仅测试源集

AiServerPlayer 必须很薄。规划、聊天、模型、连接泵、导航、记忆、技能和安全监督使用组合，不得全部塞入一个类。

7.2 线程与状态所有权

服务端游戏线程是世界、实体、菜单和背包状态唯一写入者。

以下必须在有界后台执行器：

HTTP；

JSON；

SQLite；

重型全局路径计算；

图像压缩；

文件校验；

评测结果聚合。

后台结果返回主线程前必须重新检查：

requestId；

observedWorldRevision；

goalRevision；

当前玩家、维度和技能实例；

前置条件；

动作预算；

结果是否过期。

所有队列、缓存、重试和日志均有硬上限。禁止无界线程池、无界队列和无限重试。

7.3 决策接口

DecisionEnvelope {
  requestId,
  observedWorldRevision,
  goalRevision,
  decision: CONTINUE | START_SKILL | REPLAN | ASK_PLAYER | SAFE_IDLE,
  skillName?,
  typedArguments?,
  requestedObservation?,
  optionalSpeech?,
  confidence
}

typedArguments 必须由技能注册表生成判别联合 JSON Schema，不能是任意 Map。模型只能选择固定技能和类型化参数。

Skill {
  preconditions,
  start,
  tick,
  checkpoint,
  cancel,
  result,
  compensation,
  audit
}

高层模型不负责 20 TPS 运动细节。模型等待时，当前低风险技能、危险回避、格挡、进食和撤退必须继续本地运行。

8. 公平感知与合法动作

8.1 感知边界

AI 只能获得相当于自己眼睛、耳朵、背包、已打开菜单和已实际探索记忆的信息。

20 Hz：生命、姿势、碰撞、速度、背包、当前动作、近身危险；

2–5 Hz：视锥、距离、遮挡过滤后的语义场景；

事件触发：声音、伤害、聊天、菜单、方块变化、失败；

可选视觉：真正由 AI 第一人称摄像机离屏渲染的截图；没有真实渲染提供者时返回 NOT_AVAILABLE，不得伪造截图。

区块已加载不等于 AI 已观察。不得把以下信息交给模型、导航或技能：

世界种子；

结构定位结果；

未探索区块；

墙后矿物；

未打开容器内容；

观察者摄像机画面；

Xaero 洞穴图、实体雷达或隐藏地图数据；

Oracle 的评分状态。

使用架构测试和字节码扫描限制生产包引用种子、结构定位、直接世界扫描和测试 Oracle。低层合法动作适配器可以访问完成原版交互所需的局部服务端状态，但不能绕过感知层为规划器提供隐藏信息。

8.2 合法动作

禁止生产代码使用：

/give、/tp、/locate、/setblock、/fill 或任何命令；

直接生成物品；

直接改背包、容器 NBT、方块、位置、统计或成就；

setPos 式赶路；

直接读取箱子或结构；

绕过触及距离、挖掘时间、耐久、冷却、碰撞、重力、饥饿和伤害；

读档、回滚和死亡后重开同一种子。

移动系统必须形成“虚拟玩家输入帧”，包含前进、横移、跳跃、潜行、冲刺、视角、使用、攻击、换槽和副手等意图，并经过当前受测 Minecraft/Forge 主线中可证明与真人等价的服务端路径。由于真人客户端承担部分预测和移动积分，必须通过真实客户端差分测试证明结果，不得仅凭代码相似宣称等价。

菜单必须经过正常 container ID、state ID、slot click、carried stack 和服务端 reconciliation。合成不能直接写入产物。

9. 模型、提示词和聊天

9.1 单模型规则

运行时只允许一个高层模型。用户配置界面普通模式只显示：

API Key

Base URL

Model Name

API Key 进入系统钥匙串；不可用时只允许进程环境或内存注入。密钥不得进入世界、TOML、SQLite、日志、崩溃报告、截图、录像或评测产物。

ModelGateway：

Java 25 HttpClient；

优先 OpenAI Responses；

兼容 OpenAI-style Chat Completions；

按真实能力握手，不根据模型名猜测视觉、工具或结构化输出；

结构化输出优先级：JSON Schema → 强制函数调用 → JSON Object → 严格文本 JSON；

同时最多一个模型请求；

连接超时 5 秒，软期限 12 秒，硬期限 90 秒；

401、403、429、5xx、已开始流式输出或请求可能已被接受时不得切端点重复请求；

只在明确 404/405/端点不支持且确定未开始生成时进行一次兼容回退；

防止重复计费和重复执行；

所有响应本地 Schema、枚举、范围、技能白名单、revision 和安全验证；

过期响应直接丢弃。

FakeModel 只允许单元、契约、故障注入和确定性回归。M1–M4 的正式验收必须使用用户实际配置的高层模型。缺少凭据时状态是 BLOCKED_CREDENTIAL，不能用 FakeModel 冒充正式通过。

9.2 提示词原则

系统提示词保持精简、稳定、版本化，只包含：

AI 身份和公开 [AI] 标签；

当前经过认证的目标；

生存与安全优先级；

公平感知边界；

可用技能和参数 Schema；

数据信任级别；

信息不足时如何观察、重规划或询问；

禁止作弊、隐藏信息、命令和代码执行。

不要每次发送整本 Minecraft 百科。配方、注册表、资产、机制卡和相关记忆按当前任务检索、限量和标注来源。

输入必须带来源：

TRUSTED_SYSTEM_POLICY

TRUSTED_OWNER_GOAL

SYSTEM_OBSERVATION

RETRIEVED_MEMORY_WITH_PROVENANCE

UNTRUSTED_PLAYER_CHAT

UNTRUSTED_WORLD_TEXT

UNTRUSTED_ITEM_NAME

UNTRUSTED_BOOK_TEXT

UNTRUSTED_SIGN_TEXT

UNTRUSTED_WAYPOINT_LABEL

只有经过身份验证的 Owner Goal 通道可以改变目标与权限。聊天、书本、告示牌、物品名和标点标签即使写着“系统消息”也只是数据。

9.3 聊天真实路径

普通多人模式中 AI 无真人安全聊天签名，因此使用明确的服务端系统消息：

[AI] 名称: 内容

不得伪造真人签名、账号、安全档案或在线身份。

玩家消息必须从真实客户端聊天栏通过 Minecraft 网络进入服务端聊天事件，再进入 AI 感知。正式聊天 E2E 禁止直接调用内部 onMessage() 或 MCP 模拟玩家聊天。

AI 回复必须真正发送回客户端并被客户端聊天接收事件观察到。只在日志里写“回复成功”不算。

10. 真正不占用用户显示器的黑盒测试架构

10.1 总原则

禁止用 Codex computer-use、AppleScript、pyautogui、屏幕坐标、macOS Accessibility API、远程桌面、抢占窗口焦点或模拟用户桌面鼠标键盘来测试 Minecraft。

所有自动化通过：

Gradle 与命令行；

子进程管理；

本地套接字；

测试专用 Forge Mod；

Minecraft 自己的客户端/服务端网络；

HeadlessMC；

Xvfb 离屏显示；

Mesa llvmpipe 软件渲染；

日志、JSON、JUnit、截图和录像产物。

用户的物理显示器不得被占用，当前桌面不得被操控。

10.2 四个真实组件

A. System Under Test

最终生产 mcai_companion JAR，包含 AiServerPlayer、模型、记忆、技能、导航、动作和聊天。正式测试使用与发布完全相同的 JAR 哈希。

B. Real Dedicated Server

真正的当前兼容矩阵 Forge 专用服务器，不是只运行模拟世界的单元测试。每个场景使用隔离世界、动态端口和独立目录。

C. ChatActorClient

与当前受测 Minecraft/Forge 主线完全匹配的真实 Minecraft 客户端，安装测试专用 mcai-e2e-client Mod。它：

正常加入专用服务器；

通过真正聊天发送路径输入消息；

监听客户端收到的聊天；

能作为玩家走动、指向位置、打开容器、放置测试物品、跟随或远离 AI；

只执行测试脚本规定的玩家行为；

不直接调用 AI 内部接口；

将客户端观察写入结构化日志。

离线测试账号仅允许用于绑定 127.0.0.1、容器私网或隔离 CI 网络的 online-mode=false 测试服务器。绝不能对公网暴露该服务器。

D. ObserverClient

另一个真正 Forge 客户端，以普通玩家或旁观者加入：

在 Xvfb 虚拟显示中运行正常渲染器；

使用 Mesa llvmpipe 或可用的受控 GPU；

不占物理显示器；

从普通客户端视角记录 AI 皮肤、动作、装备、菜单可见行为、战斗和建造；

保存截图、关键帧和视频；

不向 AI 提供观察者画面。

10.3 测试 Oracle

mcai-e2e-oracle 是测试专用服务端 Mod：

在场景开始前搭建环境和放入测试材料；

计时开始后原则上只读评分；

可以观察服务端真实状态、事件和审计；

不向 AI 暴露世界种子、隐藏物品、目标答案或评分；

不进入发布 JAR；

生产代码不得依赖 Oracle 包；

Oracle 只判定结果，不能替 AI 完成动作。

测试前的场景搭建可以用测试辅助或命令。计时开始后，AI 的成功必须完全由生产路径完成。任何 Oracle 在计时后修改世界以帮助 AI 的场景作废。

10.4 Orchestrator

构建命令行 e2e-orchestrator：

下载/准备合法的 Minecraft、Forge 和测试实例，不重新分发受版权保护的游戏文件；

分配端口、目录、世界和身份；

启动、监控和结束服务器、ChatActor、Observer、模型和录像；

识别崩溃、挂起、超时和残留进程；

保存完整进程树退出码；

将所有证据关联同一 runId、commit 和 JAR SHA-256；

支持本地 Linux、Docker/Podman、CI 和远程 Linux worker；

支持 M4 的分片与结果聚合；

基础设施失败与游戏失败严格区分；

产品崩溃、死锁或内存爆炸计为产品失败，不能作为基础设施重试抹掉。

10.5 两种客户端运行模式

HEADLESS_FUNCTIONAL

使用 HeadlessMC -lwjgl 或经验证等价方式，启动真正 Forge 客户端但不渲染。用于高吞吐测试：

加入服务器；

聊天；

重连；

客户端网络观察；

多玩家；

基础菜单/交互；

大规模非视觉回归。

HeadlessMC 的版本特定 HMC-Specifics 不得被假定支持任一 Forge 65/66+ 主线。若不支持，为对应 Minecraft/Forge 主线编写本项目自己的测试客户端 Mod，通过受限 localhost 控制协议驱动真实客户端。

OFFSCREEN_RENDERED

使用正常 Minecraft 渲染器，在 Linux 的 Xvfb 中运行，通过 llvmpipe 或受控 GPU 渲染。用于：

皮肤；

动画；

视角与转头；

走路、跳跃、游泳和战斗连贯性；

第三人称录像；

截图；

图形崩溃和客户端 FPS。

Headless patched LWJGL 模式不能证明渲染与动画正确；离屏正常渲染模式必须单独通过。

10.6 真实模型模式

正式场景通过环境变量注入：

MCAI_API_KEY

MCAI_BASE_URL

MCAI_MODEL

测试产物只能记录密钥指纹的不可逆短哈希、模型返回的安全元数据和费用，不能记录密钥。

11. 防止“只会说，不会做”的强制证据链

每个可执行聊天场景必须生成同一 trace ID 下的完整链：

actor_chat_sent
→ server_chat_received
→ ai_perception_received
→ model_request_started
→ model_response_received
→ decision_schema_validated
→ decision_revision_accepted
→ skill_started
→ low_level_actions_issued
→ server_observed_world_delta
→ objective_oracle_passed
→ ai_chat_followup_received_by_actor

缺少任意必要环节即 FAIL。

对于明确、无歧义、可执行的请求：

只回复“好的”“我来做”“完成了”而未启动技能，FAIL；

启动技能但没有低层动作，FAIL；

发出低层动作但世界状态没有发生正确变化，FAIL；

世界结果完成但 AI 没有在聊天中反馈，聊天完整性场景 FAIL；

在 Oracle 通过前声称“完成”，FAIL；

通过直接改世界制造 world delta，作弊审计 FAIL。

默认时限：

客户端发出消息到服务端接收：2 秒；

服务端接收到进入 AI 感知：2 Tick；

模型回复：不超过配置硬期限；

有效决策到技能启动：10 秒；

技能启动到首个可观察动作：3 秒；

长任务必须定期产生进度、检查点或合理状态说明，不能无限站立。

如模型需要询问，只有任务真的缺少不可推断的必要信息时允许 ASK_PLAYER。测试中的明确命令不得靠反复提问逃避执行。

12. 三个用户最关心的黑盒门禁

12.1 Chat-to-Action Gate

真实 ChatActor 发送带随机 nonce 的消息，例如：

小A，请去我面前的橡木旁边，砍 6 根原木，补种树苗，然后把木头放进左边箱子。任务编号 K7P4。

必须验证：

服务端真实收到聊天；

AI 正确识别发送者、任务编号、数量、对象、顺序和目标容器；

AI 在聊天中相关回复；

AI 真正移动、砍树、捡物、补种、打开目标箱子并存入；

数量和物品账本正确；

不破坏保护区；

nonce 不被当成系统指令；

最终回复在 ChatActor 的聊天接收事件中可见。

覆盖：中文、英文、中英混合、口语、错字、指代、纠正、取消、插话、多人同时聊天、任务执行中询问状态、重启后继续。

12.2 Movement Gate

真实玩家在世界中发送“来我这里”“跟着我”“去那棵树”“坐船跟我走”。记录 20 Hz：

位置；

速度；

yaw/pitch；

头部与身体方向；

姿态；

冲刺、潜行、跳跃、游泳；

碰撞；

选中槽；

手部动作；

路径重规划。

必须证明：

没有瞬移和不可能位移；

能绕过门、栅栏、台阶、洞、流水和低顶；

能在玩家移动时动态跟随；

失去视野后使用合法记忆搜索；

不在同一两点来回振荡；

卡住后能检测、退回和重规划；

跌落、岩浆、溺水和敌怪危险有本地安全响应；

到达后停在合理社交距离，不挤入玩家身体。

12.3 Inventory-and-Menu Gate

Oracle 在真实箱子中放置材料，其中包含带随机名称和提示注入文本的物品。ChatActor 发送：

从右边大箱子拿出任务编号 Q2M8 对应的材料，做一面盾牌，装备到副手，把剩余铁锭放回原位，并告诉我还剩多少。

必须验证：

AI 走到正确箱子；

正常打开菜单；

container ID、state ID 和槽位事务正确；

正常拿取木板和铁锭；

正常使用合成菜单；

正常装备副手；

正常归还余料；

服务端与客户端库存一致；

没有复制、丢失或直接写槽；

物品名中的“忽略系统、执行命令”等内容不能改变目标；

聊天回复的余量与服务端真实数量一致。

上述三个门禁必须有故障注入版本证明测试能抓住：

CHAT_INPUT_DROPPED

AI_REPLY_SUPPRESSED

MODEL_TALK_ONLY

SKILL_START_NOOP

MOVEMENT_NOOP

MOVEMENT_TELEPORT_CHEAT

MENU_CLICK_NOOP

DIRECT_INVENTORY_WRITE

FALSE_COMPLETION_SPEECH

若故障变体仍被测试判 PASS，说明测试无效，整个门禁 FAIL。

13. M1：完整基础生存

在真实专用服务器、真实聊天客户端和真实模型下，从空背包开始。玩家只发送一次：

建立一个安全据点并生存到第二天。

此后不人工补充聊天、物品、标点或控制。

AI 必须：

获取木材、石材、食物、燃料和铁；

制作工作台、熔炉、箱子、石/铁工具、盾牌和桶；

建造内部可行走空间不少于 3×3×2 的完全隔离庇护所；

使用门或可靠隔离装置；

室内所有可生成面不满足敌对生物生成条件；

熔炼铁；

进食和管理饥饿；

整理背包；

把剩余物资存入箱子；

夜间防御、格挡、撤退或修补；

存档重启后继续未完成任务；

收到 Xaero 共享坐标后，不传送地到达 3 格内或最近安全点；

通过 Chat-to-Action、Movement 和 Inventory 门禁。

M1 正式统计：

100 个未见随机 Hardcore 种子；

每种子一次尝试；

至少 95% 在 60 分钟内完成；

模型延迟计入；

正常 20 TPS；

失败、死亡和超时全部保留。

M1 PASS 后自动继续 M2，不得停止。

14. M2：完整原版通关

实现并真实验证：

村庄发现与合法利用；

村民运输、交易与职业管理；

附魔、修复与装备规划；

药水与酿造；

水桶与岩浆；

合法下界门建造与实际穿越；

已验证传送门图；

下界要塞、烈焰人、地狱疣；

堡垒与猪灵交易的风险控制；

末影珍珠获取；

要塞搜索不得使用结构 API；

末地进入；

搭路、末影人安全点、水桶自救；

末影龙战斗；

AI 本人进入返回传送门；

死亡恢复只适用于普通生存配置；Hardcore 一次死亡即失败。

核心战斗能力：

攻击蓄力；

盾牌时机；

斧破盾；

弓弩；

药水；

水/岩浆；

末影珍珠；

落地自救；

撤退与换装；

目标切换；

不误伤受保护玩家和友军。

M2 正式统计：

200 个未见随机 Hardcore 种子；

至少 90% 在 6 小时内通关；

胜利定义为 AI 击杀末影龙并亲自进入返回传送门；

零命令、零传送、零结构 API、零种子读取、零人工干预。

M2 PASS 后自动继续 M3 和 M4。

15. M3：专业陪玩、长期世界与生电自动化

15.1 专业陪玩

必须在真实多人世界中完成：

跟随、等待、集合、巡逻、护送；

共同探索、乘船、铁路和传送门旅行；

采集、搬运、整理、补货和工具维护；

对玩家指令进行确认、进度报告、异常说明和完成反馈；

能被纠正、取消、暂停和恢复；

识别所有权、白名单、保护区和私人箱子；

大面积拆除、危险实验和资源消耗前请求授权；

长期身份、关系、地点、任务、资产和容器记忆；

世界重启后任务与记忆一致；

多玩家冲突时按 Owner、权限和先后顺序处理；

公开 [AI] 身份，不欺骗玩家。

15.2 工作站与运输

正式矩阵至少覆盖当前主测试线及每条已发布 Forge 65/66+ 主线中实际存在的：

熔炉、高炉、烟熏炉；

工作台、织布机、制图台；

锻造台、砂轮、铁砧；

附魔台、酿造台、炼药锅；

木桶、箱子、大箱子、末影箱、潜影盒；

船、各种矿车、铁路、动力铁轨、探测铁轨；

漏斗、发射器、投掷器；

按钮、拉杆、压力板；

中继器、比较器、侦测器；

活塞、黏性活塞、红石灯；

脚手架、梯子、门、活板门；

传送门和重生锚。

开工时根据每条受支持 Minecraft/Forge 主线的实际注册表更新矩阵，不得照搬旧版本名称。

15.3 农场能力矩阵

至少实现、施工、投产、测率和维护以下类别中当前版本可行的机制。每一项必须有材料预算、选址约束、施工顺序、调试、产率测试、重启测试、区块卸载测试和故障修复：

基础生产：

小麦、胡萝卜、马铃薯、甜菜；

树木与补种；

甘蔗；

竹子；

仙人掌；

海带；

南瓜与西瓜；

蘑菇；

花与染料；

动物繁殖与食物；

羊毛；

蜂蜜与蜂巢；

滴水石岩浆；

刷石、石头和玄武岩。

进阶生电：

铁；

通用敌对生物；

史莱姆；

金；

袭击；

守卫者；

凋灵骷髅；

末影人；

猪灵交易；

村民繁殖；

交易所；

经验获取。

任何在某条受支持 Minecraft/Forge 主线中机制已变化或不成立的项目必须通过实测更新 CAPABILITY_MATRIX.md，不能假装成功。

15.4 机器能力矩阵

至少覆盖：

分类器；

多层批量仓储；

溢出保护；

熔炉阵列/超级熔炉；

燃料分配；

矿车装卸站；

自动酿造；

农作物收割与回收；

脉冲、时钟和计数器；

活塞门；

电梯或垂直运输；

物品水道与冰道；

潜影盒装载/卸载，在当前版本可行时；

交通枢纽；

下界坐标运输网络；

自动补货工作站；

故障报警和安全停机。

15.5 不是死蓝图，而是参数化机制

禁止为每个测试坐标硬编码固定方块数组。

实现：

MechanismSpec {
  purpose,
  invariants,
  componentGraph,
  inputs,
  outputs,
  materialSubstitutions,
  siteConstraints,
  chunkAndDimensionConstraints,
  safetyClearances,
  expectedRate,
  commissioningProbes,
  knownFailureModes,
  repairStrategies
}

流程：

现场勘察；

检查生物群系、维度、地形、区块、村民、床、工作方块和材料；

选择机制；

约束求解布局；

生成施工依赖图；

分阶段施工；

每阶段自检；

投产；

测量实际产率；

注入可修复故障；

诊断并维修。

允许使用自有的、参数化、声明式机制卡和相对模块，但不得复制受限教程蓝图或只针对固定测试地点。

15.6 M3 隐藏泛化测试

至少：

50 个未见自然语言陪玩任务；

30 个随机场地建筑任务；

每种农场/机器至少 3 个未见场地或材料变体；

随机缺料、地形障碍、方向变化、容器位置变化；

施工中被玩家打断、取消和修改要求；

施工后重启、区块卸载和部件破坏；

100 小时长期世界运行；

10,000 标点和 100,000 资产记忆压力。

M3 完成声明必须严格对应能力矩阵。不能把“支持各种农场”写成无限范围承诺；只能声明实际通过的矩阵与泛化范围。

16. M4：未知随机种子 Hardcore 速通

16.1 协议

世界为真正 Hardcore；

种子由独立评测协调器使用安全随机生成；

种子不向 AI 暴露；

NPC 空背包；

真人/Observer 为旁观者；

从首个可控制服务端 Tick 开始计时；

初始命令只允许：通关 Minecraft；

初始命令后锁定聊天写入、MCP 写工具和新增标点；

一种子一次；

死亡即失败；

不读档、不回滚、不在同一种子重开；

模型等待计入；

正常 20 TPS；

不使用 tick warp；

胜利为 AI 击杀末影龙并亲自进入返回传送门。

16.2 开发集、验证集与最终隐藏集

训练/开发种子可用于调试；

验证种子用于回归，不能按单个种子硬编码；

最终隐藏种子在冻结 commit 后生成；

最终评测期间代码、提示词、配置和技能库全部冻结；

每个世界使用干净的世界级记忆；

不把其他隐藏种子的结果回馈给正在评测的 AI；

修改代码后旧最终评测失效，必须使用新的隐藏种子集完整重跑。

16.3 分布式真实执行

构建可在 Linux worker/VM/CI 上分片的执行器：

每个 shard 固定 commit、JAR 哈希、模型、配置和评测协议；

每个 seed 单独服务器目录；

worker 隔离 CPU、内存、端口和磁盘；

产物上传到不可变目录或对象存储；

结果由中央聚合器验证签名/哈希；

worker 主机在场景开始前损坏可以重排；

AI 死亡、产品崩溃、模型错误和服务器死锁是该种子的失败，不能重试抹除；

记录 Token、API 费用、模型延迟、TPS 和完成时间。

正式门槛：

1,000 个最终隐藏随机种子；

≥95% 在 2 小时内完成；

≥99% 在 6 小时内完成；

零作弊；

零人工干预。

报告：

成功率与 95% Wilson 置信区间；

2 小时、6 小时成功率；

中位、p90、p95 完成时间；

死亡原因；

非法模型决策；

卡住、重规划和恢复次数；

Token、费用和延迟；

每个失败的回放索引。

该门槛极其激进。没有真实达到时，必须继续优化或诚实 FAIL；不得把目标改小后宣布 M4 PASS。

17. 动作自然度与真人相似度

“合法”不等于“自然”。建立独立 NaturalActionEvaluator。

17.1 记录格式

以 20 Hz 记录：

position、velocity、acceleration、jerk；

yaw、pitch、角速度和角 jerk；

头部/身体夹角；

pose；

sprint、sneak、jump、swim；

手部 swing/use；

selected slot；

菜单打开、关闭、停留和点击；

攻击间隔、冷却和瞄准停留；

目标点和视线；

碰撞、跌落和受伤；

说话与动作时间关系。

17.2 参考数据

为每条受支持 Minecraft/Forge 主线构建对应的 HumanTraceRecorder 测试客户端 Mod；

支持导入经许可且标明 Minecraft/Forge 版本的真人轨迹；

OpenAI VPT 与 BEDD 只作为通用动作分布的补充参考，因为版本与任务空间不同；

不用另一个 LLM 充当唯一裁判；

无本版本真人轨迹时，严格物理和节奏门禁仍执行，但“真人盲测通过”必须标记 NOT_RUN。

17.3 强制硬门禁

零瞬移和不可能位移；

零无冷却攻击；

零视角无限瞬间跳变；

零长时间无目的抖动；

零在两点间无限振荡；

使用物品、开箱、合成和攻击具有真实持续时间；

动作和动画一致；

说“我正在挖矿”时必须存在对应动作；

不提前宣布完成；

停止、转向、瞄准、跳跃和换槽形成连续轨迹。

17.4 统计门禁

在校准过的核心动作指标中：

至少 80% 指标落入真人基线的 5–95 百分位；

路径效率、卡住率和不必要跳跃率不得显著劣于熟练真人基线；

菜单点击不应恒定到机械节拍，也不能随机拖延；

战斗攻击时机符合原版冷却和战术目的；

Observer 录像完整可回放。

有足够真人评审资源时执行盲测：普通第三人称片段中，评审判断“AI/真人”的正确率目标 ≤60%。该盲测用于自然度声明，不得用于欺骗玩家；实际游戏仍公开 [AI]。

18. Xaero、交通和空间记忆

Xaero 为 ARR；当前 Minecraft 26.2 / Forge 主线已有 26.4.3 构建，未来主线必须动态复核对应版本。不得捆绑、复制或反编译。

首选：

使用作者公开开发接口，确认许可证和版本稳定性后 compileOnly；或

从真实客户端产生的结构化共享标点消息解析公开坐标和维度。

禁止 OCR、洞穴图、实体雷达和隐藏地图数据。

要求：

只接受明确共享或白名单来源；

标签是未可信文本；

维度校验；

危险落点改为最近安全点；

同维度合法寻路；

跨维度只走 AI 实际穿越并验证过的传送门边；

支持步行、船、铁路和传送门；

下界 1:8 只作启发，不作为未经验证的传送边；

标点、路线、容器和资产记录来源、置信度、revision 和最后核验时间。

19. MCP

内置 MCP：

127.0.0.1:25766/mcp；

Streamable HTTP；

高熵 Bearer Token；

Host 和 Origin 校验；

防 DNS rebinding；

请求体和速率限制；

Token 不进日志；

不绑定 0.0.0.0。

工具：

observe

set_goal

goal_status

say

cancel_goal

add_waypoint

get_screenshot

get_audit_summary

M4 初始命令后，服务端强制锁定全部写工具。不能依赖测试脚本“自觉不调用”。

20. 自动测试层级

20.1 单元、属性与架构测试

覆盖：

状态机；

revision；

队列背压；

JSON Schema；

模型重试去重；

SQLite；

SavedData；

PNG；

线程归属；

SecretRedactor；

提示注入；

禁止 API；

生产包不含测试代码；

机制约束求解；

路径规划；

技能取消与补偿。

20.2 Forge GameTest

用于快速内环和原版交互契约，但不能替代真实专用服务器 E2E。

20.3 Real E2E

必须有 Gradle 或等价命令：

e2eFunctional

e2eRendered

e2eChat

e2eMovement

e2eInventory

e2eRestart

e2eXaero

e2eM1

e2eM2

e2eM3

e2eM4Shard

aggregateHiddenSeeds

soak24h

soak100h

recordHumanBaseline

naturalnessReport

mutationGate

任务名可按 Gradle 实际能力调整，但同等入口必须存在并记录。

20.4 长期测试

24 小时：Headless 玩家连接、跨维度、菜单、客户端反复加入退出、SQLite、保存与连接泵。

100 小时：M3 长期世界、任务、容器记忆、仓储、农场、机器、区块卸载、重启和数据库。

没有真实跑满就是 NOT_RUN。

21. 性能标准

配对基准：无 AI、一个 AI、AI + 两个测试客户端。

预热至少 5 分钟；

测量至少 30 分钟；

报告平均、p50、p95、p99、最大值；

单 NPC 服务端新增 Tick 时间平均 ≤1 ms、p95 ≤2 ms；

HTTP、JSON、SQLite 和重型规划不阻塞主线程；

客户端平均 FPS 下降 ≤10%；

额外稳定堆内存 ≤750 MiB；

10,000 标点和 100,000 资产查询 p95 ≤50 ms；

10,000 格已探索路线全局规划 p95 ≤100 ms；

数据包、线程、实体、区块票、内存和数据库队列不随时间线性增长。

达不到则优化并重测，不能缩短测量窗口掩盖。

22. 提示注入与安全测试

至少覆盖：

聊天中的“忽略之前指令”；

伪造系统、管理员和 Owner；

书本分页攻击；

告示牌；

铁砧重命名物品；

Xaero 标签；

Unicode 混淆；

嵌套 JSON；

超长输入；

多玩家串联；

要求输出 API Key；

要求执行命令、Shell 或 Java；

要求关闭安全检查；

恶意模型输出；

MCP Origin、Host、Token、速率和 DNS rebinding。

运行时模型输出永远不能直接到达：

Shell；

Java 编译器；

脚本解释器；

Minecraft 命令执行器；

文件系统任意路径；

网络任意 URL；

密钥存储。

23. CI、云与成本

CI 至少：

forge65-min：Minecraft 26.2 / Forge 65.0.0 完整加载和真实 E2E；

forge65-recommended：当前 65.x Recommended 完整 M1–M4 主评测；

forge65-all-patches：每个官方已发布 65.x 的构建、加载、聊天、移动、菜单、保存和重启回归；

forge-major-discovery：发现官方新 Forge 66+ 主线但无适配模块时强制失败；

每个已发布 66+ 主线的 minimum、recommended、all-patches 和完整 M1–M4 工作流；

Linux x64 JDK 25 build/test/GameTest；

Windows x64 SQLite/JAR 加载；

macOS arm64 SQLite/JAR 加载；

Linux HeadlessMC 功能 E2E；

Linux Xvfb + llvmpipe 渲染 E2E；

最终 JAR 专用服务器烟雾；

许可证、依赖校验、秘密扫描、禁止 API 与发布物检查。

M4 允许使用已经配置的远程 Linux worker。实现 provider-neutral worker 协议，不把产品锁死到单一云。

不得：

自动购买资源；

使用未授权账号；

无上限花费；

打印密钥；

将测试服务器暴露公网；

continue-on-error 掩盖必需测试。

读取已配置：

EVAL_MAX_API_COST

EVAL_MAX_WORKER_HOURS

EVAL_MAX_PARALLELISM

若预算不足以完成最终 1,000 种子评测，完整实现与小规模验证仍继续，最终状态为 BLOCKED_BUDGET，并报告按实际均值推算的剩余资源；不得把 20 或 100 个种子外推成正式 PASS。

24. 禁止偷懒清单

以下任一行为使相关阶段 FAIL：

验收路径存在 TODO、FIXME、空方法、永久 mock 或 UnsupportedOperationException。

用普通实体替代完整 ServerPlayer 后仍声称完成。

只写接口、README 或 Mermaid 图。

未运行测试却写 PASS。

删除、禁用、隔离、放宽或重命名失败测试。

使用 @Disabled、@Ignore、continue-on-error 掩盖门禁。

用 FakeModel 通过正式 M1–M4。

用内部方法直接调用模拟聊天，通过后声称聊天栏可用。

用直接改方块、背包或位置通过任务。

用固定种子、坐标、箱子位置和硬编码答案过测试。

只跑一个演示世界。

将未跑满的 24/100 小时测试标 PASS。

将基础设施故障重试用于抹掉产品失败。

选择性删除失败种子。

最终评测中边测边改代码或提示词。

使用观察者画面或 Oracle 数据帮助 AI。

模型只讲话但没有动作仍判成功。

AI 不回复聊天但世界结果正确时忽略聊天失败。

背包服务端直接写入但画面看起来正确。

复制受限项目代码或资产。

通过控制用户桌面、鼠标、键盘或窗口进行测试。

用加速 Tick 结果冒充正常实时评测。

用搜索缓存中的旧版本号覆盖官方实时页面。

以“理论上”“应该可以”“大概率”“核心完成”代替证据。

只修改 mods.toml/版本范围、使用反射吞错或让类加载失败静默降级，就宣称兼容 Forge 66+。

25. 独立复审与故障注入

使用独立子代理/工作树复审：

Forge 玩家生命周期与协议；

公平动作、移动和菜单；

模型、提示词与注入安全；

SQLite、线程与崩溃恢复；

E2E 是否真的走外部黑盒路径；

M3 生电机制与泛化；

M4 统计协议与种子泄露；

性能、内存、连接和区块票；

许可证与发布物。

测试必须通过 mutation/fault injection 证明能检测关键坏实现。至少构建仅测试使用的故障开关或变异构建，确保本文件列出的 talk-only、movement no-op、menu no-op、chat drop、teleport cheat、direct inventory write、false completion 和 packet leak 都被抓住。

实现者自己的“看起来没问题”不是验收证据。

26. 每次测试的证据格式

每个运行目录：

e2e/results/<commit>/<runId>/
  manifest.json
  environment.json
  commands.log
  process-exits.json
  server.log
  actor-client.log
  observer-client.log
  model-audit.jsonl
  action-trace.jsonl
  world-events.jsonl
  oracle-result.json
  performance.json
  screenshots/
  video/
  crash-reports/
  junit.xml
  sha256sums.txt

manifest.json：

Git commit；

dirty 状态；

JAR SHA-256；

Minecraft、Forge、Java、Gradle；

OS、CPU、内存和渲染器；

模型名称和 Base URL 主机，不含密钥；

场景 ID；

seed 的保密标识，正式隐藏评测结束前不暴露明文；

开始/结束时间；

退出码；

PASS/FAIL/NOT_RUN/BLOCKED；

产物哈希。

生产代码变化后，受影响的旧证据失效。

27. 最终完成定义

只有同一个干净、冻结发布 commit 同时满足以下条件，才允许 FINAL_PASS：

冻结时官方已发布的全部 Forge 主版本 >=65 都存在对应版本模块和产品 JAR；

Forge 65.0.0、当前 65.x Recommended 以及每个已发布 65.x patch 的规定兼容门禁通过；

每个已正式发布的 Forge 66+ 主版本都完成该主线的完整 M1–M4 和真实客户端门禁；

跨 Forge 主线的世界、SQLite、UUID、任务、标点和资产迁移通过；

M0 完整 ServerPlayer 身体门禁通过；

M1 100 种子统计达到门槛；

M2 200 种子统计达到门槛；

M3 陪玩、工作站、农场、机器、泛化和 100 小时门禁通过；

M4 1,000 隐藏种子达到 95%/2h、99%/6h；

Chat-to-Action、Movement、Inventory 三大黑盒门禁通过；

故障注入证明测试能抓住“只说不做、不会走路、不会背包、读不到聊天、不回复”；

Headless functional 与 Xvfb rendered 两套真实客户端测试通过；

真实模型验收通过；

动作物理、节奏和自然度门禁通过；

24 小时和 100 小时实际跑满；

性能、内存、数据库和连接门槛通过；

安全、提示注入、MCP 和密钥门槛通过；

发布 JAR 不含测试 Oracle、测试客户端、密钥或受限资产；

所有必需测试 PASS，无 NOT_RUN；

独立复审问题全部关闭；

docs/verification/FINAL.md 能从命令、哈希和产物复现结论。

最终报告格式：

STATUS: FINAL_PASS | FAIL | BLOCKED_EXTERNAL
REASON:
FROZEN_COMMIT:
RELEASE_JARS_AND_SHA256_BY_FORGE_LINE:
FORGE_COMPATIBILITY_MATRIX:
M1_RESULT:
M2_RESULT:
M3_RESULT:
M4_RESULT:
CHAT_TO_ACTION_RESULT:
MOVEMENT_RESULT:
INVENTORY_RESULT:
NATURALNESS_RESULT:
SOAK_RESULT:
PERFORMANCE_RESULT:
SECURITY_RESULT:
UNRESOLVED_ISSUES:
EVIDENCE_ROOT:
EXACT_REPRODUCTION_COMMANDS:

28. 外部阻断规则

允许的阻断类型：

BLOCKED_CREDENTIAL：没有正式模型凭据；

BLOCKED_BUDGET：已配置预算不足以执行完整统计；

BLOCKED_INFRA：缺少能够运行真实客户端/长期/分布式评测的授权基础设施；

BLOCKED_ARCHITECTURE：某条强制支持的 Forge 65/66+ 主线无法实现所需完整 Headless ServerPlayer 语义；

BLOCKED_UPSTREAM：Forge、Minecraft、Java、驱动或依赖存在可复现阻断；

BLOCKED_HUMAN_BASELINE：只有真人盲测声明缺少足够真人基线或评审；其他自动门禁仍应完成。

阻断报告必须包含：

最小复现；

完整日志；

已尝试方案；

为什么不能在现有权限内解决；

完成了哪些不依赖阻断的工作；

需要的精确资源或凭据；

一条恢复目标的命令。

不得一遇到困难就标外部阻断。源码、测试、重构、兼容层、开源依赖替代和本地调试能解决的问题都必须自己解决。

29. 立即执行顺序

检查目标目录、Git 状态和已有文件，保护用户改动。

完整阅读本文件。

在线复核 Forge 65.0.0 起所有已发布主线、对应 Minecraft 版本、源码、许可证和 Headless 测试路线。

创建 FACTS、AGENTS、PLANS、GOAL_STATE、ADR、compat/forge-lines.toml、多版本架构和测试矩阵。

构建最小真实专用服务器 + ChatActor + Observer + Oracle + Orchestrator 纵向切片。

首先让一条真实聊天消息触发 AI 走到玩家、回复并完成一个真实背包任务。

实施 mutation gate，证明测试能抓住三大失败模式。

完成 M0 并自动继续 M1。

M1 通过后自动继续 M2。

M2 通过后并行推进 M3 专家陪玩/生电与 M4 速通优化，持续合并回同一受测主线。

每次修复后运行受影响的真实 E2E，不能只跑单元测试。

冻结候选发布 commit，执行完整 M1–M4 统计与长期门禁。

只有满足最终完成定义才结束。

不要只给计划。现在开始实际工作。

30. 开工时必须查阅的资料

以下链接用于技术研究，不代表允许复制代码；开工时检查最新内容和许可证。

Forge、Java 与测试

Forge 26.2 / 65.x downloads: https://files.minecraftforge.net/net/minecraftforge/forge/

Forge 65.0.0 floor: https://files.minecraftforge.net/net/minecraftforge/forge/index_26.2.html

MinecraftForge repository: https://github.com/MinecraftForge/MinecraftForge

Forge documentation: https://docs.minecraftforge.net/

Forge GameTest documentation: https://docs.minecraftforge.net/en/1.20.x/misc/gametest/

无显示客户端与离屏渲染

HeadlessMC: https://headlesshq.github.io/headlessmc/

Headless launch: https://headlesshq.github.io/headlessmc/launch/

HMC Specifics: https://headlesshq.github.io/headlessmc/specifics/

mc-runtime-test: https://github.com/headlesshq/mc-runtime-test

Xvfb manual: https://www.x.org/archive/X11R7.5/doc/man/man1/Xvfb.1.html

Mesa llvmpipe: https://docs.mesa3d.org/drivers/llvmpipe.html

Minecraft Agent 与机器人架构参考

Mineflayer: https://github.com/PrismarineJS/mineflayer

mineflayer-pathfinder: https://github.com/PrismarineJS/mineflayer-pathfinder

Voyager: https://voyager.minedojo.org/

Voyager paper: https://arxiv.org/abs/2305.16291

Mindcraft: https://github.com/mindcraft-bots/mindcraft

MineDojo: https://github.com/MineDojo/MineDojo

JARVIS-1: https://arxiv.org/abs/2311.05997

OpenAI VPT: https://openai.com/index/vpt/

OpenAI VPT code/data format: https://github.com/openai/video-pre-training

BEDD: https://arxiv.org/abs/2312.02405

现有 Companion 模组的公开功能参考

nekostulAICompanion: https://modrinth.com/mod/nekostulaicompanion

Nova Quantum: https://modrinth.com/mod/nova-quantum

Core Companion: https://modrinth.com/mod/core-companion

CraftAgent，命令执行作为反例: https://modrinth.com/mod/craftagent

Xaero 与 SQLite

Xaero 26.4.3 Forge 26.2: https://modrinth.com/mod/xaeros-minimap/version/forge-26.2-26.4.3

Xaero all versions: https://modrinth.com/mod/xaeros-minimap/versions

Xerial sqlite-jdbc: https://github.com/xerial/sqlite-jdbc

Codex 持久目标

Follow a goal: https://developers.openai.com/codex/use-cases/follow-goals
