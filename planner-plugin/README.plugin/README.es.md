<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Internos del Plugin

> Guía de desarrollador y colaborador para el plugin Gradle `planner-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.1` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.planner`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **Build**: `./gradlew :planner-plugin:build -x test` · **Tests**: `./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposición del módulo

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # Punto de entrada del plugin — registra la tarea generatePlan
        ├── PlannerExtension.kt        # DSL de extensión (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # Implementación de la tarea generatePlan
        ├── IntentionPlanner.kt        # Construcción del prompt + orquestación LLM + contexto compuesto
        ├── OllamaBridge.kt            # Wrapper LangChain4j ChatModel + reintento 3×
        ├── LLMResponse.kt             # Parsing de la respuesta LLM cruda
        ├── Plan.kt                    # Data classes Plan / EPIC / UserStory / GradleTask
        ├── PlanningContext.kt         # Value object del contexto de planificación
        ├── SpecReader.kt              # RAG sobre specs existentes (presupuesto tokens 2000)
        ├── Metadata.kt                # Formato pivote — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # Salida stdout estructurada
```

## Contratos N0 (desde workspace-bom MEMPHIS)

| Contrato | Artefacto | Proporciona |
|----------|----------|-------------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner consume únicamente `codebase-contracts` (única fuente de verdad para los
> canales de contexto). **No** depende de `codebase-plugin` (N1) — la tarea
> `vibecode` se eliminó de planner durante la resolución del split-brain (session 049).

## Bibliotecas clave

- **langchain4j** 1.14.1 — proveedores LLM (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — DSL Kotlin para grafos agénticos (orquestación)
- **kotlinx-serialization-json** 1.7.3 — I/O JSON estructurado
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — parsing de respuestas LLM
- **gradle-plugin-publish** 2.1.0 — publicación en el Gradle Plugin Portal

## Instancias Ollama (restricción global)

Los puertos `11434–11436` están prohibidos. Rotar sobre `11437–11465` (29 puertos).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Modelo por defecto en `PlanningPlugin`: `gpt-oss:120b-cloud`.

## Matriz de tests

| Tarea | Ámbito | Notas |
|-------|--------|-------|
| `:planner-plugin:test` | Tests unitarios JUnit5 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, logging completo de excepciones |

Clases de test (8 archivos bajo `src/test/kotlin/planning/`):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

Sin suites Cucumber — planner usa JUnit5 puro (sin división `testFast`/`testAll`/`testEpics`).
No hay gate de cobertura Kover configurado en este build.

## Ajuste JVM

Los tests se ejecutan con `-XX:+EnableDynamicAgentLoading`. Para runs de integración LLM pesados:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## Comandos de build

```bash
./gradlew :planner-plugin:build                       # build completo (compila + tests)
./gradlew :planner-plugin:build -x test               # solo compilar
./gradlew :planner-plugin:test                        # tests unitarios JUnit5
./gradlew :planner-plugin:publishToMavenLocal         # publicación local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## Pipeline CI

`.github/workflows/` define dos workflows:

1. **test.yml** — `./gradlew :planner-plugin:build` en push/PR a `main`/`master`
   (JDK 24 Temurin, timeout 15 min, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — trigger manual `workflow_dispatch`: instala Ollama, configura la
   clave device `OLLAMA_DEVICE_KEY_A`, descarga `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   ejecuta `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`, hace commit del
   plan bajo `features/plans/`, sube el artefacto `build/planning/*.json`.

## Publicación (NMCP)

La publicación a Maven Central usa `com.gradleup.nmcp` (configurado en `settings.gradle.kts`,
tipo de publicación `AUTOMATIC`). El `build.gradle.kts` declara:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM en `withType<MavenPublication>`: nombre, descripción, licencia Apache 2.0,
  desarrollador `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (portal central, no el staging legacy de Sonatype)
- `signing { useGpgCmd() }` — firma salvo `CI=true` o versión `-SNAPSHOT`
- `java { withJavadocJar(); withSourcesJar() }`

La publicación en el Gradle Plugin Portal usa `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

Todas las dependencias `implementation` son releases (sin `-SNAPSHOT`); `codebase-contracts:0.0.1`
ya está publicado en Maven Central.

## Estado de los EPICs

Todos los EPICs cerrados en `0.0.1` (ver `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## Contribuir

1. El build compila: `./gradlew :planner-plugin:build -x test`
2. Tests unitarios en verde: `./gradlew :planner-plugin:test`
3. Respetar la frontera DAG: planner es N2 — importable por N3 `runner-gradle`,
   nunca importa N3. Nunca depender de `codebase-plugin` (N1); usar
   `codebase-contracts` (N0).
4. Tras cualquier cambio en el código fuente: `./gradlew :planner-plugin:publishToMavenLocal` (regla 2).
5. Seguir la taxonomía de 4 verbos (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) y el
   formato pivote `Metadata.kt` (EPIC K-2).

## Docs de arquitectura

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, sesiones, gobernanza
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — Reglas absolutas (5 reglas)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — Checklist de inicio de sesión
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — Ontología del workspace (4 verbos)

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._