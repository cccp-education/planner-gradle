<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Internes du Plugin

> Guide développeur & contributeur pour le plugin Gradle `planner-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.1` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.planner`
- **Toolchain** : Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **Build** : `./gradlew :planner-plugin:build -x test` · **Tests** : `./gradlew :planner-plugin:test`

🌐 Langues : [English](README.md) | **Français**

---

## Disposition du module

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # Point d'entrée du plugin — enregistre la tâche generatePlan
        ├── PlannerExtension.kt        # DSL d'extension (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # Implémentation de la tâche generatePlan
        ├── IntentionPlanner.kt        # Construction du prompt + orchestration LLM + contexte composite
        ├── OllamaBridge.kt            # Wrapper LangChain4j ChatModel + retry 3×
        ├── LLMResponse.kt             # Parsing de la réponse LLM brute
        ├── Plan.kt                    # Data classes Plan / EPIC / UserStory / GradleTask
        ├── PlanningContext.kt         # Value object du contexte de planification
        ├── SpecReader.kt              # RAG sur les specs existantes (budget tokens 2000)
        ├── Metadata.kt                # Format pivot — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # Sortie stdout structurée
```

## Contrats N0 (depuis workspace-bom MEMPHIS)

| Contrat | Artefact | Fournit |
|---------|----------|---------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner consomme uniquement `codebase-contracts` (source unique de vérité pour les
> canaux de contexte). Il ne dépend **pas** de `codebase-plugin` (N1) — la tâche
> `vibecode` a été supprimée de planner lors de la résolution du split-brain (session 049).

## Bibliothèques clés

- **langchain4j** 1.14.1 — fournisseurs LLM (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — DSL Kotlin pour graphes agentiques (orchestration)
- **kotlinx-serialization-json** 1.7.3 — I/O JSON structuré
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — parsing des réponses LLM
- **gradle-plugin-publish** 2.1.0 — publication sur le Gradle Plugin Portal

## Instances Ollama (contrainte globale)

Les ports `11434–11436` sont interdits. Rotation sur `11437–11465` (29 ports).
Modèles autorisés : `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Modèle par défaut dans `PlanningPlugin` : `gpt-oss:120b-cloud`.

## Matrice de tests

| Tâche | Périmètre | Notes |
|-------|-----------|-------|
| `:planner-plugin:test` | Tests unitaires JUnit5 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, logging complet des exceptions |

Classes de test (8 fichiers sous `src/test/kotlin/planning/`) :

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

Aucune suite Cucumber — planner utilise du JUnit5 pur (pas de split `testFast`/`testAll`/`testEpics`).
Aucun gate de couverture Kover configuré dans ce build.

## Réglage JVM

Les tests s'exécutent avec `-XX:+EnableDynamicAgentLoading`. Pour les runs d'intégration
LLM lourds :

```bash
export GRADLE_OPTS="-Xmx2g"
```

## Commandes de build

```bash
./gradlew :planner-plugin:build                       # build complet (compile + tests)
./gradlew :planner-plugin:build -x test               # compilation seule
./gradlew :planner-plugin:test                        # tests unitaires JUnit5
./gradlew :planner-plugin:publishToMavenLocal         # publication locale
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## Pipeline CI

`.github/workflows/` définit deux workflows :

1. **test.yml** — `./gradlew :planner-plugin:build` sur push/PR vers `main`/`master`
   (JDK 24 Temurin, timeout 15 min, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — trigger manuel `workflow_dispatch` : installe Ollama, configure
   la clé device `OLLAMA_DEVICE_KEY_A`, pull `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   exécute `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`, committe le
   plan sous `features/plans/`, upload l'artefact `build/planning/*.json`.

## Publication (NMCP)

La publication vers Maven Central utilise `com.gradleup.nmcp` (configuré dans
`settings.gradle.kts`, type de publication `AUTOMATIC`). Le `build.gradle.kts` déclare :

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM sur `withType<MavenPublication>` : nom, description, licence Apache 2.0,
  développeur `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (portail central, pas l'ancien staging Sonatype)
- `signing { useGpgCmd() }` — signe sauf si `CI=true` ou version `-SNAPSHOT`
- `java { withJavadocJar(); withSourcesJar() }`

La publication sur le Gradle Plugin Portal utilise `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

Toutes les dépendances `implementation` sont des releases (pas de `-SNAPSHOT`) ;
`codebase-contracts:0.0.1` est déjà publié sur Maven Central.

## Statut des EPICs

Tous les EPICs clôturés en `0.0.1` (voir `.agents/INDEX.adoc`) :
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## Contribuer

1. Le build compile : `./gradlew :planner-plugin:build -x test`
2. Tests unitaires verts : `./gradlew :planner-plugin:test`
3. Respecter la frontière DAG : planner est N2 — importable par N3 `runner-gradle`,
   n'importe jamais N3. Ne jamais dépendre de `codebase-plugin` (N1) ; utiliser
   `codebase-contracts` (N0).
4. Après toute modification de code source : `./gradlew :planner-plugin:publishToMavenLocal` (règle 2).
5. Suivre la taxonomie 4 verbes (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) et le
   format pivot `Metadata.kt` (EPIC K-2).

## Docs d'architecture

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, sessions, gouvernance
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — Règles absolues (5 règles)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — Checklist d'ouverture de session
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — Ontologie du workspace (4 verbes)

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Fait partie de l'écosystème CCCP Education — `groupId: education.cccp`._