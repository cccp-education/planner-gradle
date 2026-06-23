<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Guía del Consumidor

> Plugin Gradle Planning Expert — descompone una intención en lenguaje natural en un plan de ejecución estructurado (EPICs → User Stories → Tasks) mediante un LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.1` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.planner`
- **Build**: `./gradlew :planner-plugin:build` · **Tests**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`planner-gradle` toma una intención de alto nivel expresada en lenguaje natural y la
descompone en un plan de ejecución estructurado — EPICs → User Stories → tareas Gradle —
mediante un LLM (LangChain4j + DeepSeek-v4-pro servido por Ollama). La salida es stdout
estructurado más un artefacto JSON de plan bajo `build/planning/`.

Parte del ecosistema multi-plugin de CCCP Education:

```
intención del usuario → [planner-gradle] → LLM (Ollama) → plan estructurado (EPICs/US/Tareas)
```

Consume el contrato N0 `codebase-contracts` (`ContextChannel`, `ChannelBudget`,
`CompositeContext`, `CompositeContextConfig`) como única fuente de verdad para el
modelado de canales de contexto.

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. Generar un plan

```bash
./gradlew generatePlan \
  --intention="Añadir una tarea de exportación PDF al pipeline de formación"
```

Contexto RAG opcional desde especificaciones existentes:

```bash
./gradlew generatePlan \
  --intention="Refactorizar el CLI del quiz benchmark" \
  -PspecsDir=specs/
```

### 3. Sobrescribir el endpoint del LLM

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="deepseek-v4-pro:cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## Tareas disponibles

| Tarea | Grupo | Descripción |
|-------|-------|-------------|
| `generatePlan` | generate | Descompone una intención en lenguaje natural en un plan de ejecución estructurado (EPICs → User Stories → Tasks). Opción `-PspecsDir=/path/to/specs` para contexto RAG. |

> La tarea `vibecode` se ha eliminado de planner (resolución split-brain).
> Vive solo en `codebase-gradle` (N1): `./gradlew :codebase-plugin:vibecode --intention="..."`.

## DSL de extensión

```gradle
planner {
    ollamaModel    = "deepseek-v4-pro:cloud"   // por defecto
    ollamaBaseUrl  = "http://localhost:11434"  // por defecto
    intention      = "Your default intention"  // opcional, reemplazable por -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // fuente RAG opcional
}
```

Todas las propiedades de extensión son reemplazables por invocación mediante propiedades de Gradle:
`-Pintention`, `-PspecsDir`, `-PollamaModel`, `-PollamaBaseUrl`.

## Requisitos previos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 para auto-provisioning del toolchain)
- **Ollama** ejecutándose localmente (o remoto), sirviendo `deepseek-v4-pro:cloud`
- Los puertos `11434–11436` están prohibidos globalmente; rotar sobre `11437–11465`.
  Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Build y tests

```bash
./gradlew :planner-plugin:build              # build completo (compila + tests)
./gradlew :planner-plugin:build -x test      # solo compilar
./gradlew :planner-plugin:test               # tests unitarios JUnit5
./gradlew :planner-plugin:publishToMavenLocal # publicación local
```

## Uso en CI (dispatch manual)

El workflow `decompose.yml` expone un trigger `workflow_dispatch` para generar un plan
desde la UI de GitHub Actions:

- Entradas: `intention` (obligatorio), `feature_request_id` (opcional)
- Descarga `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` desde Ollama cloud
- Hace commit del plan generado bajo `features/plans/` (cuando se da un feature request id)
- Sube el artefacto `build/planning/*.json`

## Resolución de problemas

| Síntoma | Solución |
|---------|----------|
| `Connection refused localhost:11434` | Iniciar Ollama: `ollama serve`; descargar el modelo: `ollama pull deepseek-v4-pro:cloud` |
| El LLM devuelve JSON malformado | Reintentar (reintento 3× integrado en `OllamaBridge`); verificar el presupuesto de tokens en `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| Puerto `11434` prohibido | Usar un puerto en `11437–11465`; pasar `-PollamaBaseUrl=http://localhost:11437` |

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._