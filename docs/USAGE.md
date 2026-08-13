# MinePilot 使用教程

本文面向第一次安装或测试 MinePilot 的玩家、服务器管理员和开发者。当前教程对应
`0.1.9-dev-mc26.2`：Minecraft Java 26.2、Forge 65.x、Java 25。它不是
“两小时通关保证书”；当前 M0–M4 正式门禁仍在进行中。

## 1. 安装

### 客户端/内置服务器

1. 安装 Java 25 和 Minecraft Java 26.2。
2. 为该实例安装 Forge 65.0.0–65.x；不要把 Forge 安装器本身放进 `mods`。
3. 从 `build/libs/` 只复制一个 `mcai_companion-*.jar` 到 `mods/`。
4. 客户端和集成服务器必须使用同一 JAR。未安装 Mod 的第三方服务器不能生成无账号
   的 AI `ServerPlayer`。
5. 启动世界并确认暂停菜单中出现“AI 陪玩”。首次打开会显示配置向导。

### 专用服务器

安装同一个 JAR，先确认 Mojang EULA 和服务器规则，再启动 `runServer` 或你的正常
专服。AI 可以在没有真人在线时保持世界和自身生命周期；首个真人登录时，服务端会在
安全位置完成一次性初始锚定，不是游戏中的任意传送。使用 `/mcai embodiment status`
可查看 `[AI]` 身体、维度、UUID 和目标状态。

## 2. 第一次配置

按 `Esc → AI 陪玩`，依次设置：

- **Agent 名称**：3–16 个字符，仅英文字母、数字、下划线，且不能与玩家/服务器
  名称重复。
- **配色与皮肤**：导入本地不超过 1 MiB 的现代 64×64 PNG，选择经典或纤细手臂。
  文件损坏、尺寸错误或同步失败会稳定回退到 Steve/Alex。
- **温度**：`0.0–1.0`，直接作为模型采样温度；低值更稳定，高值更发散。它不是
  物理世界的“智能度”开关。
- **Base URL / Model Name**：填写供应商实际支持的 HTTPS API 地址与模型名。不要
  根据名称猜测 Responses、函数调用或视觉能力；保存验证会进行能力握手。
- **API Key**：只填入设置框或通过安全注入，不要发到聊天、提交、截图或 issue。
- **系统偏好**：写角色风格、语言和协作偏好；它不能覆盖公平、安全、权限和极限规则。

点击“保存并验证”。按钮会固定在表单底部，成功后第一次普通聊天会引导你完成一次
低风险交互。单人世界不需要 `@mcai`；多人可用 Agent 名称明确指定对象。

## 3. 跨平台保存密钥

推荐优先级是进程 Secret 文件 > 系统密钥环/DPAPI > 进程环境变量。密钥恢复失败时，
AI 身体仍可以出现并显示 `[AI]`，但不会调用模型或假装已经完成任务。

### macOS

设置页会使用当前用户 Keychain。若运行在无交互服务账号，使用 Secret 文件：

```bash
export MCAI_API_KEY_FILE="/run/secrets/mcai-api-key"
```

### Windows / Windows Server

设置页使用当前 Windows 用户作用域 DPAPI。服务重启必须继续使用同一个服务账号；更
推荐把密钥放进受 ACL 保护的 Secret 文件并设置 `MCAI_API_KEY_FILE`。

### Linux 桌面与 Debian 专服

Linux 桌面可使用 Secret Service（通常由 `secret-tool` 提供）。无桌面 Debian/Ubuntu
请使用 systemd 或容器 Secret：

```ini
[Service]
LoadCredential=mcai-api-key:/etc/mcai-companion/api-key
```

Mod 会读取 `$CREDENTIALS_DIRECTORY/mcai-api-key`。`MCAI_API_KEY` 仅作为进程环境回退，
不应写入 systemd unit、Gradle 属性或仓库。若平台没有安全存储，界面会明确显示
`saved_verified_process_restart_required`，而不是偷偷创建明文配置文件。

## 4. 和 AI 说话与下达任务

单人世界中可以像和队友说话一样直接输入：

```text
帮我收割小麦，放进家里的箱子，然后补种。
跟我走，保持十格以内，遇到怪物先举盾撤退。
去森林砍树，背包满了就回到主仓库。
```

多人世界中可用名称或 `@mcai` 明确指定：

```text
@MinePilot 去坐标 x=120 y=68 z=-40，到了先报告，不要传送。
```

AI 的正常执行顺序应当在审计中表现为：聊天收到 → 目标确认 → 模型决策 →
`skill_started` → 原版低层动作 → 世界/背包结果。只有聊天回复而没有技能和结果时，
它仍是失败，不要把“好的，我这就来”当作完成。

没有 API Key、网络断开、401/429、超时或非法输出时，AI 会保留身体，执行当前已授权
的低风险动作，必要时举盾、进食、撤退或安全待机；它不能凭空继续编造观察结果。

## 5. 标点、皮肤、MCP 与命令

- Xaero：只共享结构化标点和维度。AI 会正常步行、乘船、铁路或通过已验证传送门
  图前往，不读取洞穴图/雷达，也不调用传送。
- MCP：默认仅绑定 `127.0.0.1:25766/mcp`，需要 Bearer Token、Host/Origin 校验。
  可用工具为 `observe`、`set_goal`、`goal_status`、`say`、`cancel_goal`、
  `add_waypoint`、`get_screenshot`、`get_audit_summary`。
- 皮肤：客户端按 AI UUID 渲染纹理，保留主副手、护甲、盾、进食、潜行、游泳和睡眠
  动画；未设置时回退 Steve/Alex。

常用管理命令（需要权限）：

```text
/mcai embodiment spawn
/mcai embodiment status
/mcai embodiment remove
/mcai goal status
/mcai model status
/mcai model probe
```

专服普通玩家若要发起游戏任务，管理员需把 UUID 放进
`config/mcai-companion.toml` 的 `chat.allowedSenders`。这不是 OP，也不会开放作弊
命令。极限评测的 `/mcai evaluation start` 会锁定聊天、MCP 写工具、新标点和配置。

## 6. 运行测试

开发者在仓库根目录运行：

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew test
./gradlew runGameTestServer
```

这两个命令验证单元、契约、物理和受控 GameTest，不等同于正式随机种子成绩。已确认
供应商授权并希望消耗真实模型额度时，才显式注入凭据并运行：

```bash
MCAI_API_KEY_FILE=/run/secrets/mcai-api-key \
  ./gradlew runGameTestServer \
  -Plive_model_test=true -Prealtime_gametest=true
```

实时参数让测试服按真实 20 TPS 推进；省略它可能导致模型等待期间游戏快进。正式
Actor/Observer、24 小时稳定、100/200/1000 随机种子门禁必须在独立环境重新运行，
不能用本地无窗口夹具替代。

## 7. 排错

| 现象 | 先检查 |
| --- | --- |
| 只有两个 Forge/Minecraft 条目或提示版本不匹配 | 这是同一个 Mod 的依赖显示；确认 Minecraft 26.2、Forge 65.x 和 Java 25，mods 中只放一个 `mcai_companion` JAR |
| 进入世界后没有 AI 身体 | 看 `/mcai embodiment status`、服务端日志和 UUID；没有 Key 也应有身体，模型不可用只会停止说话/动作 |
| AI 说话但不动 | 检查审计是否有 `decision_revision_accepted`、`skill_started`、`low_level_actions_issued`；只有 ACK 没有技能就是缺陷，不是成功 |
| 重新进入世界又要求 Key | 确认当前运行账号有 Keychain/DPAPI/Secret Service 权限，或改用 `MCAI_API_KEY_FILE`；不要把密钥写进 TOML |
| 远程模型 401/429 | 检查供应商授权、URL、模型名和额度；不要反复重试导致重复扣费 |
| UI 文字重叠或不能滚动 | 在较大窗口/较低 GUI 缩放复现，并记录版本、分辨率和截图；服务端模型能力不应被 UI 问题冒充为已验证 |
| Xaero 标点无法到达 | 确认标点是明确共享、维度正确且落点可站立；跨维度必须存在已验证传送门边 |

提交日志或截图前请删除 API Key、Authorization、玩家 IP、世界路径和私人坐标。
更完整的项目边界、证据规则和贡献规范见 [PROJECT_CHARTER.md](PROJECT_CHARTER.md)
与 [CONTRIBUTING.md](../CONTRIBUTING.md)。
