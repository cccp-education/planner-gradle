<!-- master source — other languages are translations of this file -->
# planner-gradle — Consumer Guide

> Planning Expert Gradle plugin — decomposes a natural language intention into a structured execution plan (EPICs → User Stories → Tasks) via LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Version**: `0.0.1` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.planner`
- **Build**: `./gradlew :planner-plugin:build` · **Tests**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: **EN** | [中文](README.consommateurs/README.zh.md) | [हिन्दी](README.consommateurs/README.hi.md) | [Español](README.consommateurs/README.es.md) | [Français](README.consommateurs/README.fr.md) | [العربية](README.consommateurs/README.ar.md) | [বাংলা](README.consommateurs/README.bn.md) | [Português](README.consommateurs/README.pt.md) | [Русский](README.consommateurs/README.ru.md) | [اردو](README.consommateurs/README.ur.md)

---

## What it does

`planner-gradle` takes a high-level intention expressed in natural language and decomposes
it into a structured execution plan — EPICs → User Stories → Gradle tasks — via an LLM
(LangChain4j + DeepSeek-v4-pro served by Ollama). Output is structured stdout plus a JSON
plan artifact under `build/planning/`.

Part of the CCCP Education multi-plugin ecosystem:

```
user intent → [planner-gradle] → LLM (Ollama) → structured plan (EPICs/US/Tasks)
```

It consumes the N0 `codebase-contracts` (`ContextChannel`, `ChannelBudget`,
`CompositeContext`, `CompositeContextConfig`) as the single source of truth for
context-channel modelling.

## Quick Start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. Generate a plan

```bash
./gradlew generatePlan \
  --intention="Add a PDF export task to the training pipeline"
```

Optional RAG context from existing specs:

```bash
./gradlew generatePlan \
  --intention="Refactor the quiz benchmark CLI" \
  -PspecsDir=specs/
```

### 3. Override the LLM endpoint

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="deepseek-v4-pro:cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `generatePlan` | generate | Decomposes a natural language intention into a structured execution plan (EPICs → User Stories → Tasks). Optional `-PspecsDir=/path/to/specs` for RAG context. |

> The `vibecode` task has been removed from planner (split-brain resolution).
> It lives only in `codebase-gradle` (N1): `./gradlew :codebase-plugin:vibecode --intention="..."`.

## Extension DSL

```gradle
planner {
    ollamaModel    = "deepseek-v4-pro:cloud"   // default
    ollamaBaseUrl  = "http://localhost:11434"  // default
    intention      = "Your default intention"  // optional, overridable by -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // optional RAG source
}
```

All extension properties are overridable per-invocation via Gradle properties:
`-Pintention`, `-PspecsDir`, `-PollamaModel`, `-PollamaBaseUrl`.

## Prerequisites

- **Java** 24+ (Kotlin 2.3.20 toolchain)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 for toolchain auto-provisioning)
- **Ollama** running locally (or remote), serving `deepseek-v4-pro:cloud`
- Ports `11434–11436` are forbidden globally; rotate over `11437–11465`.
  Authorized models: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Build & test

```bash
./gradlew :planner-plugin:build              # full build (compiles + tests)
./gradlew :planner-plugin:build -x test      # compile only
./gradlew :planner-plugin:test               # JUnit5 unit tests
./gradlew :planner-plugin:publishToMavenLocal # local publish
```

## CI usage (manual dispatch)

The `decompose.yml` workflow exposes a `workflow_dispatch` trigger to generate a plan
from the GitHub Actions UI:

- Inputs: `intention` (required), `feature_request_id` (optional)
- Pulls `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` from Ollama cloud
- Commits the generated plan under `features/plans/` (when a feature request id is given)
- Uploads the `build/planning/*.json` artifact

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Connection refused localhost:11434` | Start Ollama: `ollama serve`; pull the model: `ollama pull deepseek-v4-pro:cloud` |
| LLM returns malformed JSON | Retry (built-in 3× retry in `OllamaBridge`); check token budget in `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| Port `11434` forbidden | Use a port in `11437–11465`; pass `-PollamaBaseUrl=http://localhost:11437` |

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._