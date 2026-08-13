# 隐藏随机种子 Hardcore 评测

`scripts/run-hidden-seed-evaluations.py` 是独立于 GameTest 的进程级评测器。
它为每次尝试复制一份干净的专用服务端模板，使用系统安全随机数生成
64 位种子，并强制写入：

- `hardcore=true`
- `difficulty=hard`
- `gamemode=survival`
- `force-gamemode=true`
- `allow-flight=false`
- `enable-command-block=false`
- `enable-rcon=false`
- `generate-structures=true`

评测器先执行一次模型能力预检；预检成功后只发送一次
`mcai evaluation start completion` 或 `mcai evaluation start foundation`。
前者安装固定目标“通关 Minecraft”；后者安装 M1 固定目标
“建立安全据点并生存到第二天”。之后聊天、MCP 写操作、标点和模型配置
均被持久锁定。终态由服务端写入
`world/data/mcai_companion/evaluation-result.json`，不依赖模型自报。

## 准备检查

服务端模板必须是未启动过的干净目录：

- 已安装当前发布 JAR、Minecraft 26.2及Forge 65.x；
- 已由用户本人接受 EULA，`eula.txt` 包含 `eula=true`；
- 不包含 `world/`；
- 模型 Base URL、Model Name 和系统钥匙串凭据已经配置；
- 不包含会改世界生成、规则、战利品或玩家能力的额外 Mod/数据包。

评测器不会替用户接受 EULA，也不会把 API Key 写入参数、结果或世界。

先只生成并审计两个用例：

```bash
python3 scripts/run-hidden-seed-evaluations.py \
  --template-dir /absolute/clean-forge-server \
  --output-dir /absolute/evaluation-suite \
  --route completion \
  --cases 2 \
  --prepare-only
```

实际运行时，把专用服务端命令放在 `--` 后面：

```bash
python3 scripts/run-hidden-seed-evaluations.py \
  --template-dir /absolute/clean-forge-server \
  --output-dir /absolute/evaluation-suite \
  --route completion \
  --cases 100 \
  -- java -Xms2G -Xmx4G -jar forge-server.jar nogui
```

`summary.json` 只含种子承诺、终态证据、游戏 Tick 和墙钟耗时；
`private-seeds.json` 权限为 `0600`，包含复现所需原始种子。正式盲测应让
评测器与 Minecraft 服务端运行在隔离的 OS 用户或容器中，使服务端进程
无法读取私有清单；测试结束后再向审计者公开清单和盐值。

## 结果边界

两条路线共同要求：

- 真正 Hardcore 与标准生存规则；
- AI 本体处于 Survival，所有真人连接均为 Spectator；
- 未污染、未死亡；
- 结束 Tick 不早于开始 Tick。

`completion` 还要求击杀归因属于 AI 本体，并收到原版
`isEndConquered` 返回证据。`foundation` 要求服务端逐项核验木材、
食物余量、石制工具、铁镐/桶/盾工具组，AI 亲自打开过且仍存在的工作台、
熔炉和箱子，成功的原版入箱事务与仍在箱内的物资，动态生成并完成的封闭
有门有光庇护所，以及跨入下一主世界日；模型的“完成”文本不能代替这些
证据。

`summary.json` 会分别统计不超过 72,000 Tick（一小时）、144,000 Tick
（两小时）和 432,000 Tick（六小时）的完成次数。M1 正式矩阵使用
`--route foundation --cases 100`；通关矩阵使用 `completion`。
`--prepare-only`、受控 GameTest、
单个成功样本或未执行用例都不会计作自然随机种子通关证据。当前仓库尚未
执行 100/200/1,000 个隐藏种子，因此不能据此宣称已达到计划成功率。

## 分片聚合

大量用例应在相互隔离的评测主机上分片运行。只把每个分片的公开
`summary.json` 交给聚合器；不要把 `private-seeds.json` 或原始种子复制到
聚合主机：

```bash
python3 scripts/aggregate-hidden-seed-summaries.py \
  --route completion --minimum-cases 1000 --json \
  /audit/shard-01/summary.json /audit/shard-02/summary.json
```

聚合器会重新核对每个终态的 Hardcore 锁定、污染/死亡状态、末影龙与返回
传送门证据，重新计算小时计数和比例，拒绝重复 `caseId`/种子承诺、原始
种子字段、准备但未执行的用例、缺少精确 product SHA-256/release commit
绑定的 summary 以及计数被篡改的 summary；所有分片还必须绑定同一个工件
和 commit。`completion` 且
最少 1,000 个用例时，只有两小时完成数达到 `ceil(95%)` 且六小时完成数
达到 `ceil(99%)` 才会返回 `PASS`；这仍需在干净 release commit 上由
`aggregateHiddenSeeds` 正式门禁接收。缺少摘要时该门禁保持 `NOT_RUN`。

运行器会从模板 `mods/` 中唯一的 `mcai_companion-*.jar` 自动计算 product
SHA-256，并从 `MCAI_SOURCE_COMMIT`/当前 Git HEAD 取得 40 位 commit；也可用
`--product-sha256` 和 `--source-commit` 显式绑定。无法得到两者时，摘要仍可
用于调试，但聚合器会拒绝其正式统计用途。

同一聚合器也服务于 `e2eM1` 与 `e2eM2`：分别通过环境变量
`MCAI_M1_FOUNDATION_SUMMARIES`（至少 100 个 foundation 用例，1 小时
`ceil(95%)`）和 `MCAI_M2_COMPLETION_SUMMARIES`（至少 200 个 completion
用例，6 小时 `ceil(90%)`）接入。三条门禁都要求真实执行的公开摘要；没有
对应摘要时只记录 `NOT_RUN`。

在正式 release checkout 上，聚合命令还应带上期望工件绑定（Gradle 产出的
精确产品 JAR SHA-256），或由门禁环境设置
`MCAI_EXPECTED_PRODUCT_SHA256`；门禁会同时把摘要中的 40 位 source commit
与当前干净 commit 比较。没有精确 JAR 绑定时，即使统计数字满足阈值也不会
提升为正式 PASS。
