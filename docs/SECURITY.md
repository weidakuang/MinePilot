# Security and trust boundaries

## Secrets

API Key 不得进入：

- Forge TOML；
- 世界 SavedData；
- SQLite；
- 日志、异常消息、崩溃报告；
- MCP 返回值；
- 截图。

请求时 `SecretSource` 提供一次性 `char[]`，HTTP Header 构造后立即清零
数组。平台安全存储和无头服务器注入采用以下边界：

- macOS Keychain 写入通过 `/usr/bin/security -w <password>` 的短生命周期
  非交互调用。`security(1)` 不支持从管道为 `-w` 提供密码；父进程不记录
  命令行、日志或输出，并在启动子进程后立即清空命令数组；密钥仍不会进入
  世界、配置、数据库、崩溃报告或截图；
- Windows 通过固定、非交互 PowerShell 调用当前用户作用域 DPAPI；
  PowerShell argv 只含固定脚本，密钥经 stdin 传入，实例配置目录只落
  DPAPI 密文。同一 Windows 服务账号才能在重启后解密；
- Linux 桌面通过 `secret-tool` 连接 Secret Service，密钥写入 stdin；
- 无桌面 Linux/容器优先读取 `MCAI_API_KEY_FILE`，并原生识别 systemd
  `$CREDENTIALS_DIRECTORY/mcai-api-key`。这些外部 Secret 由管理员或
  编排器管理，Mod 不改写、不删除；
- `MCAI_API_KEY` 是兼容性回退。系统安全存储和外部注入均不可用时只
  退回可擦除的进程内存，不创建自有明文密钥文件。

若同时存在外部注入和旧的平台安全存储，显式的
`MCAI_API_KEY_FILE`、systemd Secret 或 `MCAI_API_KEY` 优先；这允许服务端
在不删除旧密钥环项目的情况下安全轮换凭据。未提供注入时才读取平台安全
存储。

设置界面优先请求重启安全保存：若平台安全存储不可用，配置事务仍可在本次
进程完成验证，但内部 storage 会标记为
`process_only_secure_store_unavailable`，客户端显示
`saved_verified_process_restart_required`，并要求下次启动前通过
`MCAI_API_KEY_FILE`、systemd Secret 或 `MCAI_API_KEY` 注入。它不会把进程内
凭据误报为已持久化，也不会创建新的明文密钥文件；若凭据本身无效，仍会失败
关闭并不安装新的模型端点。

所有平台 helper 都有固定的非秘密参数、输出上限和15秒超时；macOS Keychain
写入的短生命周期密码参数不写日志且不被父进程保留；外部密钥文件有
32 KiB 上限、严格 UTF-8和既有凭据字符校验。

能力探测必须由显式 setup 操作发起。极限评测开始会原子确认“网关已验证且没有 probe 正在运行”，随后冻结模型配置；评测期间不能重新探测或替换网关。

Esc/Mods 的 Agent 设置中心显示名称、配色、0.0–1.0采样温度、皮肤入口、
API Key、Base URL、Model Name 和 Agent 系统偏好。API Key 使用密码
控件，不预填、不回传，也不写入 Forge 配置；留空表示保留当前凭据。
名称、配色、温度、引导完成状态和系统偏好属于非秘密世界配置。界面
明确要求不得把密钥粘贴进系统偏好。
设置写入使用有长度上限的 PLAY_TO_SERVER 消息，并要求：

- 当前连接绑定、两分钟过期、一次性 256-bit 随机会话令牌；
- 集成服务器内存连接或已启用 Minecraft 加密的网络连接；离线模式明文
  连接拒绝传输密钥；
- 单人世界所有者或专用服务器管理员权限；
- 远程端点必须为 HTTPS，HTTP 仅允许数值回环地址或 localhost；
- 极限评测冻结、在途探测和并发配置更新时 fail-closed；
- 服务端响应只含固定状态码和“凭据是否可用”，不含密钥、前缀或长度。

Agent 名称由服务端按 `^[A-Za-z0-9_]{3,16}$` 验证，并与在线玩家及
该世界已经见过的玩家名称进行不区分大小写的冲突检查。活动身体不能
原地改写 `GameProfile` 名称，必须先正常移除，避免客户端玩家列表和
皮肤缓存出现两个身份。

温度会经过客户端、网络消息和世界数据三层 `0.0–1.0` 有限数校验，
并作为供应商采样 `temperature` 随每个 Responses/Chat 请求发送。用户
系统偏好被放在公平、安全和技能白名单规则之后，并明确不能覆盖这些
规则；底层执行器仍然只接受已注册技能。

密钥字节在编码/解码后清零，服务端严格 UTF-8 解码成临时 `char[]`，
再交给既有 `ApiKeyManager`。Base URL 与 Model Name 经同一
`EndpointValidator` 验证后才写入 common TOML；验证成功的配置即时生效
并重新探测，不需要重启。

## MCP

- 只能绑定 JVM 回环地址；
- Bearer 至少 32 个 URL-safe 字符；
- 校验远端地址、Host 与 Origin；
- 最大请求体 256 KiB；
- JSON-RPC 错误不回显内部异常；
- 极限评测开始后所有写工具 fail-closed，评测前导入过外部标点的世界也不能作为新鲜随机种子。

## Prompt injection

以下内容永远先作为不可信游戏数据：

- 普通聊天；
- 书、告示牌、物品名；
- Xaero 标签；
- 模组 GUI 文本；
- 服务器 MOTD。

只有明确授权玩家的 `@mcai ...`、有权限命令或带 Bearer 的 MCP 写调用能修改目标。Xaero 标签只进入带 provenance 的标点字段，不能成为模型系统指令。

## Reporting

请不要在 issue 中粘贴真实 API Key、完整世界数据库或包含私人聊天的日志。报告时提供受控 failure code、Minecraft/Forge/Mod 版本和最小复现步骤。
