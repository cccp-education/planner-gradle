<!-- master source — other languages are translations of this file -->
# planner-gradle — Plugin Internals

> Developer & contributor guide for the `planner-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Version**: `0.0.1` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.planner`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **Build**: `./gradlew :planner-plugin:build -x test` · **Tests**: `./gradlew :planner-plugin:test`

🌐 Languages: **EN** | [中文](README.plugin/README.zh.md) | [हिन्दी](README.plugin/README.hi.md) | [Español](README.plugin/README.es.md) | [Français](README.plugin/README.fr.md) | [العربية](README.plugin/README.ar.md) | [বাংলা](README.plugin/README.bn.md) | [Português](README.plugin/README.pt.md) | [Русский](README.plugin/README.ru.md) | [اردو](README.plugin/README.ur.md)

---

## Module layout

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # Plugin entry point — registers generatePlan task
        ├── PlannerExtension.kt        # Extension DSL (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # generatePlan task implementation
        ├── IntentionPlanner.kt        # Prompt building + LLM orchestration + composite context
        ├── OllamaBridge.kt            # LangChain4j ChatModel wrapper + 3× retry
        ├── LLMResponse.kt             # LLM raw response parsing
        ├── Plan.kt                    # Plan / EPIC / UserStory / GradleTask data classes
        ├── PlanningContext.kt         # Planning context value object
        ├── SpecReader.kt              # RAG on existing specs (token budget 2000)
        ├── Metadata.kt                # Format pivot — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # Structured stdout output
```

## N0 contracts (from workspace-bom MEMPHIS)

| Contract | Artifact | Provides |
|----------|----------|----------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner consumes only `codebase-contracts` (the single source of truth for context
> channels). It does **not** depend on `codebase-plugin` (N1) — the `vibecode` task was
> removed from planner during split-brain resolution (session 049).

## Key libraries

- **langchain4j** 1.14.1 — LLM providers (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — Kotlin DSL for agentic graphs (orchestration)
- **kotlinx-serialization-json** 1.7.3 — structured JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — LLM response parsing
- **gradle-plugin-publish** 2.1.0 — Gradle Plugin Portal publishing

## Ollama instances (global constraint)

Ports `11434–11436` are forbidden. Rotate over `11437–11465` (29 ports).
Authorized models: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Default model in `PlanningPlugin`: `gpt-oss:120b-cloud`.

## Test matrix

| Task | Scope | Notes |
|------|-------|-------|
| `:planner-plugin:test` | JUnit5 unit tests | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, full exception logging |

Test classes (8 files under `src/test/kotlin/planning/`):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

No Cucumber suites — planner uses pure JUnit5 (no `testFast`/`testAll`/`testEpics` split).
No Kover coverage gate is configured in this build.

## JVM tuning

Tests run with `-XX:+EnableDynamicAgentLoading`. For heavy LLM integration runs:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## Build commands

```bash
./gradlew :planner-plugin:build                       # full build (compiles + tests)
./gradlew :planner-plugin:build -x test               # compile only
./gradlew :planner-plugin:test                        # JUnit5 unit tests
./gradlew :planner-plugin:publishToMavenLocal         # local publish
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI pipeline

`.github/workflows/` defines two workflows:

1. **test.yml** — `./gradlew :planner-plugin:build` on push/PR to `main`/`master`
   (JDK 24 Temurin, 15 min timeout, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — `workflow_dispatch` manual trigger: installs Ollama, sets up
   `OLLAMA_DEVICE_KEY_A` device key, pulls `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   runs `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`, commits the
   plan under `features/plans/`, uploads `build/planning/*.json` artifact.

## Publication (NMCP)

Publication to Maven Central uses `com.gradleup.nmcp` (configured in `settings.gradle.kts`,
publishing type `AUTOMATIC`). The `build.gradle.kts` declares:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM on `withType<MavenPublication>`: name, description, Apache 2.0 license,
  developer `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, not legacy Sonatype staging)
- `signing { useGpgCmd() }` — signs unless `CI=true` env or `-SNAPSHOT` version
- `java { withJavadocJar(); withSourcesJar() }`

Gradle Plugin Portal publication uses `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

All `implementation` dependencies are releases (no `-SNAPSHOT`); `codebase-contracts:0.0.1`
is already published on Maven Central.

## EPIC status

All EPICs closed in `0.0.1` (see `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## Contributing

1. Build compiles: `./gradlew :planner-plugin:build -x test`
2. Unit tests green: `./gradlew :planner-plugin:test`
3. Respect the DAG boundary: planner is N2 — importable by N3 `runner-gradle`, never
   imports N3. Never depend on `codebase-plugin` (N1); use `codebase-contracts` (N0).
4. After any source change: `./gradlew :planner-plugin:publishToMavenLocal` (rule 2).
5. Follow the 4-verb taxonomy (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) and the
   `Metadata.kt` format pivot (EPIC K-2).

## Architecture docs

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, sessions, governance
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — Absolute rules (5 rules)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — Session opening checklist
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — Workspace ontology (4 verbs)

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._