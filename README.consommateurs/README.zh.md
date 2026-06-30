<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — 消费者指南

> Planning Expert Gradle 插件 — 通过 LLM 将自然语言意图分解为结构化执行计划（EPICs → User Stories → Tasks）。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **版本**：`0.0.1` · **组**：`education.cccp` · **插件 ID**：`education.cccp.planner`
- **构建**：`./gradlew :planner-plugin:build` · **测试**：`./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能介绍

`planner-gradle` 接收以自然语言表达的高层意图，通过 LLM（LangChain4j + 由 Ollama 提供的
DeepSeek-v4-pro）将其分解为结构化执行计划 —— EPICs → User Stories → Gradle tasks。输出为结构化
stdout 以及位于 `build/planning/` 下的 JSON 计划工件。

CCCP Education 多插件生态系统的一部分：

```
用户意图 → [planner-gradle] → LLM (Ollama) → 结构化计划 (EPICs/US/Tasks)
```

它消费 N0 `codebase-contracts`（`ContextChannel`、`ChannelBudget`、`CompositeContext`、
`CompositeContextConfig`）作为上下文通道建模的唯一真相来源。

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. 生成计划

```bash
./gradlew generatePlan \
  --intention="为培训流水线添加一个 PDF 导出任务"
```

可选的来自现有规格的 RAG 上下文：

```bash
./gradlew generatePlan \
  --intention="重构 quiz benchmark CLI" \
  -PspecsDir=specs/
```

### 3. 覆盖 LLM 端点

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="gpt-oss:120b-cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## 可用任务

| 任务 | 组 | 描述 |
|------|----|------|
| `generatePlan` | generate | 将自然语言意图分解为结构化执行计划（EPICs → User Stories → Tasks）。可选 `-PspecsDir=/path/to/specs` 用于 RAG 上下文。 |

> `vibecode` 任务已从 planner 中移除（split-brain 解决方案）。
> 它仅存在于 `codebase-gradle`（N1）中：`./gradlew :codebase-plugin:vibecode --intention="..."`。

## 扩展 DSL

```gradle
planner {
    ollamaModel    = "gpt-oss:120b-cloud"   // 默认
    ollamaBaseUrl  = "http://localhost:11434"  // 默认
    intention      = "Your default intention"  // 可选，可通过 -Pintention 覆盖
    specsDir       = layout.projectDirectory.dir("specs")  // 可选 RAG 源
}
```

所有扩展属性均可通过 Gradle 属性在每次调用时覆盖：
`-Pintention`、`-PspecsDir`、`-PollamaModel`、`-PollamaBaseUrl`。

## 前置条件

- **Java** 24+（Kotlin 2.3.20 工具链）
- **Gradle** 9.5+（foojay-resolver-convention 1.0.0 用于工具链自动配置）
- **Ollama** 本地（或远程）运行，提供 `gpt-oss:120b-cloud`
- 端口 `11434–11436` 全局禁止；在 `11437–11465` 间轮换。
  授权模型：`gpt-oss:120b-cloud`、`gemma4:31b-cloud`。

## 构建与测试

```bash
./gradlew :planner-plugin:build              # 完整构建（编译 + 测试）
./gradlew :planner-plugin:build -x test      # 仅编译
./gradlew :planner-plugin:test               # JUnit5 单元测试
./gradlew :planner-plugin:publishToMavenLocal # 本地发布
```

## CI 使用（手动触发）

`decompose.yml` 工作流暴露了 `workflow_dispatch` 触发器，可从 GitHub Actions UI 生成计划：

- 输入：`intention`（必需）、`feature_request_id`（可选）
- 从 Ollama 云端拉取 `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`
- 将生成的计划提交至 `features/plans/`（当提供 feature request id 时）
- 上传 `build/planning/*.json` 工件

## 故障排除

| 症状 | 修复 |
|------|------|
| `Connection refused localhost:11434` | 启动 Ollama：`ollama serve`；拉取模型：`ollama pull gpt-oss:120b-cloud` |
| LLM 返回格式错误的 JSON | 重试（`OllamaBridge` 内置 3× 重试）；检查 `IntentionPlanner` 中的 token 预算 |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| 端口 `11434` 被禁止 | 使用 `11437–11465` 中的端口；传入 `-PollamaBaseUrl=http://localhost:11437` |

## 许可证

Apache License 2.0 —— 参见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_