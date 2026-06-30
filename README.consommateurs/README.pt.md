<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Guia do Consumidor

> Plugin Gradle Planning Expert — decompõe uma intenção em linguagem natural num plano de execução estruturado (EPICs → User Stories → Tasks) via LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.1` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.planner`
- **Build**: `./gradlew :planner-plugin:build` · **Testes**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

`planner-gradle` recebe uma intenção de alto nível expressa em linguagem natural e
decompõe-a num plano de execução estruturado — EPICs → User Stories → tarefas Gradle —
via um LLM (LangChain4j + DeepSeek-v4-pro servido pelo Ollama). A saída é stdout
estruturado mais um artefacto JSON de plano sob `build/planning/`.

Parte do ecossistema multi-plugin da CCCP Education:

```
intenção do utilizador → [planner-gradle] → LLM (Ollama) → plano estruturado (EPICs/US/Tarefas)
```

Consome o contrato N0 `codebase-contracts` (`ContextChannel`, `ChannelBudget`,
`CompositeContext`, `CompositeContextConfig`) como única fonte de verdade para a
modelação de canais de contexto.

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. Gerar um plano

```bash
./gradlew generatePlan \
  --intention="Adicionar uma tarefa de exportação PDF ao pipeline de formação"
```

Contexto RAG opcional a partir de specs existentes:

```bash
./gradlew generatePlan \
  --intention="Refatorizar o CLI do quiz benchmark" \
  -PspecsDir=specs/
```

### 3. Sobrescrever o endpoint do LLM

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="gpt-oss:120b-cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|--------|-------|-----------|
| `generatePlan` | generate | Decompõe uma intenção em linguagem natural num plano de execução estruturado (EPICs → User Stories → Tasks). Opção `-PspecsDir=/path/to/specs` para contexto RAG. |

> A tarefa `vibecode` foi removida do planner (resolução split-brain).
> Vive apenas em `codebase-gradle` (N1): `./gradlew :codebase-plugin:vibecode --intention="..."`.

## DSL de extensão

```gradle
planner {
    ollamaModel    = "gpt-oss:120b-cloud"   // padrão
    ollamaBaseUrl  = "http://localhost:11434"  // padrão
    intention      = "Your default intention"  // opcional, sobrescrevível por -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // fonte RAG opcional
}
```

Todas as propriedades de extensão são sobrescrevíveis por invocação via propriedades do Gradle:
`-Pintention`, `-PspecsDir`, `-PollamaModel`, `-PollamaBaseUrl`.

## Pré-requisitos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 para auto-provisioning do toolchain)
- **Ollama** a correr localmente (ou remoto), servindo `gpt-oss:120b-cloud`
- Os portos `11434–11436` são proibidos globalmente; rodar sobre `11437–11465`.
  Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Build e testes

```bash
./gradlew :planner-plugin:build              # build completo (compila + testes)
./gradlew :planner-plugin:build -x test      # apenas compilar
./gradlew :planner-plugin:test               # testes unitários JUnit5
./gradlew :planner-plugin:publishToMavenLocal # publicação local
```

## Uso em CI (dispatch manual)

O workflow `decompose.yml` expõe um trigger `workflow_dispatch` para gerar um plano
a partir da UI do GitHub Actions:

- Entradas: `intention` (obrigatório), `feature_request_id` (opcional)
- Descarrega `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` da nuvem Ollama
- Faz commit do plano gerado sob `features/plans/` (quando é dado um feature request id)
- Carrega o artefacto `build/planning/*.json`

## Resolução de problemas

| Sintoma | Solução |
|---------|---------|
| `Connection refused localhost:11434` | Iniciar Ollama: `ollama serve`; descarregar o modelo: `ollama pull gpt-oss:120b-cloud` |
| LLM devolve JSON malformado | Repetir (repetição 3× integrada em `OllamaBridge`); verificar o orçamento de tokens em `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| Porto `11434` proibido | Usar um porto em `11437–11465`; passar `-PollamaBaseUrl=http://localhost:11437` |

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._