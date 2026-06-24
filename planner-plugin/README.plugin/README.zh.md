<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — 插件内部

> `planner-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **版本**：`0.0.1` · **组**：`education.cccp` · **插件 ID**：`education.cccp.planner`
- **工具链**：Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **构建**：`./gradlew :planner-plugin:build -x test` · **测试**：`./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 模块布局

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # 插件入口点 — 注册 generatePlan 任务
        ├── PlannerExtension.kt        # 扩展 DSL (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # generatePlan 任务实现
        ├── IntentionPlanner.kt        # 提示构建 + LLM 编排 + 复合上下文
        ├── OllamaBridge.kt            # LangChain4j ChatModel 包装 + 3× 重试
        ├── LLMResponse.kt             # LLM 原始响应解析
        ├── Plan.kt                    # Plan / EPIC / UserStory / GradleTask 数据类
        ├── PlanningContext.kt         # 规划上下文值对象
        ├── SpecReader.kt              # 对现有规格做 RAG (token 预算 2000)
        ├── Metadata.kt                # 格式枢纽 — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # 结构化 stdout 输出
```

## N0 契约（来自 workspace-bom MEMPHIS）

| 契约 | 工件 | 提供 |
|------|------|------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner 仅消费 `codebase-contracts`（上下文通道的唯一真相来源）。它**不**依赖
> `codebase-plugin` (N1) —— `vibecode` 任务在 split-brain 解决期间已从 planner 中移除（session 049）。

## 关键库

- **langchain4j** 1.14.1 — LLM 提供者 (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — 用于代理图的 Kotlin DSL（编排）
- **kotlinx-serialization-json** 1.7.3 — 结构化 JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — LLM 响应解析
- **gradle-plugin-publish** 2.1.0 — Gradle Plugin Portal 发布

## Ollama 实例（全局约束）

端口 `11434–11436` 被禁止。在 `11437–11465` 间轮换（29 个端口）。
授权模型：`gpt-oss:120b-cloud`, `gemma4:31b-cloud`。
`PlanningPlugin` 中的默认模型：`deepseek-v4-pro:cloud`。

## 测试矩阵

| 任务 | 范围 | 说明 |
|------|------|------|
| `:planner-plugin:test` | JUnit5 单元测试 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, 完整异常日志 |

测试类（8 个文件位于 `src/test/kotlin/planning/`）：

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

无 Cucumber 套件 —— planner 使用纯 JUnit5（无 `testFast`/`testAll`/`testEpics` 拆分）。
此构建未配置 Kover 覆盖率门禁。

## JVM 调优

测试以 `-XX:+EnableDynamicAgentLoading` 运行。对于重型 LLM 集成运行：

```bash
export GRADLE_OPTS="-Xmx2g"
```

## 构建命令

```bash
./gradlew :planner-plugin:build                       # 完整构建（编译 + 测试）
./gradlew :planner-plugin:build -x test               # 仅编译
./gradlew :planner-plugin:test                        # JUnit5 单元测试
./gradlew :planner-plugin:publishToMavenLocal         # 本地发布
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI 流水线

`.github/workflows/` 定义了两个工作流：

1. **test.yml** — 在向 `main`/`master` 的 push/PR 上运行 `./gradlew :planner-plugin:build`
   (JDK 24 Temurin，15 分钟超时，`gradle/actions/setup-gradle@v4`)。
2. **decompose.yml** — `workflow_dispatch` 手动触发：安装 Ollama，设置 device key
   `OLLAMA_DEVICE_KEY_A`，拉取 `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud`，
   运行 `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`，将计划提交至
   `features/plans/`，上传 `build/planning/*.json` 工件。

## 发布 (NMCP)

发布至 Maven Central 使用 `com.gradleup.nmcp`（配置在 `settings.gradle.kts`，
发布类型 `AUTOMATIC`）。`build.gradle.kts` 声明：

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM on `withType<MavenPublication>`：名称、描述、Apache 2.0 许可证、
  开发者 `cccp-education` (`cccp.edu@gmail.com`)、SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, not legacy Sonatype staging)
- `signing { useGpgCmd() }` — 除非 `CI=true` 环境变量或 `-SNAPSHOT` 版本否则签名
- `java { withJavadocJar(); withSourcesJar() }`

Gradle Plugin Portal 发布使用 `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`)。

所有 `implementation` 依赖均为发布版本（无 `-SNAPSHOT`）；`codebase-contracts:0.0.1`
已发布在 Maven Central。

## EPIC 状态

所有 EPIC 已在 `0.0.1` 中关闭（见 `.agents/INDEX.adoc`）：
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1。

## 贡献

1. 构建可编译：`./gradlew :planner-plugin:build -x test`
2. 单元测试通过：`./gradlew :planner-plugin:test`
3. 遵守 DAG 边界：planner 是 N2 —— 可被 N3 `runner-gradle` 导入，绝不
   导入 N3。绝不依赖 `codebase-plugin` (N1)；使用 `codebase-contracts` (N0)。
4. 任何源代码更改后：`./gradlew :planner-plugin:publishToMavenLocal`（规则 2）。
5. 遵循 4-动词分类法 (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) 和
   `Metadata.kt` 格式枢纽 (EPIC K-2)。

## 架构文档

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, 路线图, 会话, 治理
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — 绝对规则（5 条规则）
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — 会话开始检查清单
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — 工作区本体（4 个动词）

## 许可证

Apache License 2.0 —— 参见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_