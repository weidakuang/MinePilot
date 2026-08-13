# Contributing to MinePilot

感谢贡献。MinePilot 是一个需要同时满足游戏公平、模型安全、服务端稳定性和可审计
证据的工程；“看起来能跑”不等于可以合并或发布。

## 开始前

1. 先读 [项目规范](docs/PROJECT_CHARTER.md)、[AGENTS.md](AGENTS.md) 和
   [目标文件](CODEX_GOAL_M1_M4.md)；目标文件是 M0–M4 和版本声明的唯一来源。
2. 搜索现有实现、ADR、测试和 `docs/progress/GOAL_STATE.json`，优先修复已有路径，
   不要平行创建同义类、重复技能或第二套配置。
3. 确认改动属于当前 Forge/Minecraft 主版本线。不同主版本必须使用适配器或独立
   工件，不能把版本范围写成“所有 Forge”。

## 分支、提交与描述

- 分支使用 `codex/`、`feature/`、`fix/` 或 `docs/` 前缀；不要直接重写 `main`。
- 一个提交只解决一个可描述的问题。提交前必须说明：原因、行为变化、测试命令、
  是否影响兼容性/安全性和证据状态。
- 禁止重复提交、空提交、纯格式噪声、复制旧代码改名、无调用点的占位类和“为了让
  测试变绿”而放宽断言。若没有用户可观察的行为、测试、文档或安全收益，不要提交。
- 推荐 Conventional Commit subject，使用英文、现在时、50–80 字符：

  ```text
  feat(skills): add verified portal return action
  fix(runtime): recover the authoritative body after initial anchor
  test(live): record the real model causal chain
  docs: explain Debian credential injection
  ```

- 正文用规范化描述，不写“改了一下”“应该可以”“大概修复”。若证据不足，明确写
  `NOT_RUN`、`FAIL` 或 `BLOCKED_*`。

## 每次提交前的强制检查

先只暂存本次改动，再运行：

```bash
git add <本次文件>
./scripts/preflight-before-commit.sh
git diff --cached --stat
git diff --cached --check
```

脚本会拒绝：空暂存区、重复 patch、生成的 `build/run/logs/e2e/results`、常见密钥
模式、生产新增 TODO/`UnsupportedOperationException` 以及空白错误。它还会在生产代码
有改动但没有相应测试/文档时给出警告；警告必须在提交说明中解释，不可静默忽略。

脚本不是 CI 的替代品。至少按改动类型运行：

| 改动 | 最低验证 |
| --- | --- |
| 纯文档 | `git diff --cached --check`、链接/命令人工复核 |
| Java 逻辑 | `./gradlew test` 或针对性 `--tests` |
| 技能/物理/菜单 | 对应 Forge GameTest；不得只改断言 |
| 模型/聊天/权限 | 契约测试 + 授权真实模型/真实客户端切片；没有凭据就标 `BLOCKED_CREDENTIAL` |
| 版本/构建 | `./gradlew clean build`，核对生成 JAR 和依赖 |
| 安全/凭据 | 脱敏日志、密钥路径和错误分支测试 |

正式 M0–M4 证据还必须绑定源提交、JAR SHA-256、MC/Forge/Java、模型、种子策略和
退出码。单元测试、受控 GameTest、旧版本记录和模型回复本身都不能升级正式状态。

## 代码行为要求

- 生产代码只能通过原版 `ServerPlayer`、菜单、物理、冷却、耐久、视线和世界规则
  产生结果；禁止直接改背包/方块、传送、结构定位、隐藏扫描或作弊命令。
- 模型输出必须经过结构化校验、世界/目标 revision 校验和权限检查。模型说“我做了”
  不得直接变成成功状态，必须观察到真实 `skill_started` 和结果。
- 所有低延迟生存反应在服务端 Tick 运行；模型等待、断网、429、非法 JSON 或旧响应
  不能让身体停在危险中。
- API Key、玩家身份、IP、私有坐标和截图中的界面信息不得写入日志、测试产物或 Git。
- 新技能必须包含前置条件、tick 行为、检查点、取消条件和失败状态；新记忆必须有
  来源、世界/维度、revision 和最后核验时间。
- 测试夹具、Oracle、客户端脚本和突变工具不得进入生产 JAR；必须有构建契约防止泄漏。

## Pull Request 检查表

提交 PR 前，在描述中回答：

- [ ] 这项改动解决了什么具体问题？是否复用了已有实现？
- [ ] 是否改变了 Minecraft/Forge 兼容范围、权限、密钥或公平边界？
- [ ] 是否新增/更新了有意义的测试、文档或审计证据？
- [ ] 是否运行了与改动匹配的命令，并记录真实结果而非推测？
- [ ] 是否确认没有密钥、运行目录、构建工件、重复文件或无调用点代码？
- [ ] 若证据未达到正式门禁，是否保留为 `NOT_RUN`/`BLOCKED_*`？

所有原创贡献按仓库的 Apache-2.0 发布。第三方代码、资产或许可证不清楚时不要复制，
先在 issue/PR 中说明来源并补充 `THIRD_PARTY_NOTICES.md`。
