<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Internos do Plugin

> Guia de programador e contribuidor para o plugin Gradle `planner-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.1` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.planner`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **Build**: `./gradlew :planner-plugin:build -x test` · **Testes**: `./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposição do módulo

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # Ponto de entrada do plugin — regista a tarefa generatePlan
        ├── PlannerExtension.kt        # DSL de extensão (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # Implementação da tarefa generatePlan
        ├── IntentionPlanner.kt        # Construção do prompt + orquestração LLM + contexto composto
        ├── OllamaBridge.kt            # Wrapper LangChain4j ChatModel + repetição 3×
        ├── LLMResponse.kt             # Parsing da resposta LLM em bruto
        ├── Plan.kt                    # Data classes Plan / EPIC / UserStory / GradleTask
        ├── PlanningContext.kt         # Value object do contexto de planificação
        ├── SpecReader.kt              # RAG sobre specs existentes (orçamento tokens 2000)
        ├── Metadata.kt                # Formato pivô — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # Saída stdout estruturada
```

## Contratos N0 (do workspace-bom MEMPHIS)

| Contrato | Artefacto | Fornece |
|----------|----------|---------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner consome apenas `codebase-contracts` (única fonte de verdade para os
> canais de contexto). **Não** depende de `codebase-plugin` (N1) — a tarefa
> `vibecode` foi removida do planner durante a resolução do split-brain (session 049).

## Bibliotecas-chave

- **langchain4j** 1.14.1 — fornecedores LLM (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — DSL Kotlin para grafos agénticos (orquestração)
- **kotlinx-serialization-json** 1.7.3 — I/O JSON estruturado
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — parsing de respostas LLM
- **gradle-plugin-publish** 2.1.0 — publicação no Gradle Plugin Portal

## Instâncias Ollama (restrição global)

Os portos `11434–11436` são proibidos. Rodar sobre `11437–11465` (29 portos).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Modelo padrão em `PlanningPlugin`: `gpt-oss:120b-cloud`.

## Matriz de testes

| Tarefa | Âmbito | Notas |
|--------|--------|-------|
| `:planner-plugin:test` | Testes unitários JUnit5 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, logging completo de exceções |

Classes de teste (8 ficheiros sob `src/test/kotlin/planning/`):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

Sem suites Cucumber — planner usa JUnit5 puro (sem divisão `testFast`/`testAll`/`testEpics`).
Nenhum gate de cobertura Kover configurado neste build.

## Afinação JVM

Os testes correm com `-XX:+EnableDynamicAgentLoading`. Para runs de integração LLM pesados:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## Comandos de build

```bash
./gradlew :planner-plugin:build                       # build completo (compila + testes)
./gradlew :planner-plugin:build -x test               # apenas compilar
./gradlew :planner-plugin:test                        # testes unitários JUnit5
./gradlew :planner-plugin:publishToMavenLocal         # publicação local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## Pipeline CI

`.github/workflows/` define dois workflows:

1. **test.yml** — `./gradlew :planner-plugin:build` em push/PR para `main`/`master`
   (JDK 24 Temurin, timeout 15 min, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — trigger manual `workflow_dispatch`: instala Ollama, configura a
   chave device `OLLAMA_DEVICE_KEY_A`, descarrega `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   executa `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`, faz commit do
   plano sob `features/plans/`, carrega o artefacto `build/planning/*.json`.

## Publicação (NMCP)

A publicação para Maven Central usa `com.gradleup.nmcp` (configurado em `settings.gradle.kts`,
tipo de publicação `AUTOMATIC`). O `build.gradle.kts` declara:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM em `withType<MavenPublication>`: nome, descrição, licença Apache 2.0,
  programador `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (portal central, não o staging legado do Sonatype)
- `signing { useGpgCmd() }` — assina exceto se `CI=true` ou versão `-SNAPSHOT`
- `java { withJavadocJar(); withSourcesJar() }`

A publicação no Gradle Plugin Portal usa `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

Todas as dependências `implementation` são releases (sem `-SNAPSHOT`); `codebase-contracts:0.0.1`
já está publicado no Maven Central.

## Estado dos EPICs

Todos os EPICs fechados em `0.0.1` (ver `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## Contribuir

1. O build compila: `./gradlew :planner-plugin:build -x test`
2. Testes unitários verdes: `./gradlew :planner-plugin:test`
3. Respeitar a fronteira DAG: planner é N2 — importável por N3 `runner-gradle`,
   nunca importa N3. Nunca depender de `codebase-plugin` (N1); usar
   `codebase-contracts` (N0).
4. Após qualquer alteração de código-fonte: `./gradlew :planner-plugin:publishToMavenLocal` (regra 2).
5. Seguir a taxonomia de 4 verbos (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) e o
   formato pivô `Metadata.kt` (EPIC K-2).

## Documentos de arquitetura

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, sessões, governança
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — Regras absolutas (5 regras)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — Checklist de início de sessão
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — Ontologia do workspace (4 verbos)

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._