<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Guide Consommateur

> Plugin Gradle Planning Expert — décompose une intention en langage naturel en un plan d'exécution structuré (EPICs → User Stories → Tasks) via LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.1` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.planner`
- **Build** : `./gradlew :planner-plugin:build` · **Tests** : `./gradlew :planner-plugin:test` (JUnit5)

🌐 Langues : [English](README.md) | **Français**

---

## Ce que ça fait

`planner-gradle` prend une intention de haut niveau exprimée en langage naturel et la
décompose en un plan d'exécution structuré — EPICs → User Stories → tâches Gradle — via
un LLM (LangChain4j + DeepSeek-v4-pro servi par Ollama). La sortie est un stdout
structuré plus un artefact JSON de plan sous `build/planning/`.

Fait partie de l'écosystème multi-plugin CCCP Education :

```
intention utilisateur → [planner-gradle] → LLM (Ollama) → plan structuré (EPICs/US/Tâches)
```

Il consomme le contrat N0 `codebase-contracts` (`ContextChannel`, `ChannelBudget`,
`CompositeContext`, `CompositeContextConfig`) comme source unique de vérité pour la
modélisation des canaux de contexte.

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. Générer un plan

```bash
./gradlew generatePlan \
  --intention="Ajouter une tâche d'export PDF au pipeline de formation"
```

Contexte RAG optionnel depuis des specs existantes :

```bash
./gradlew generatePlan \
  --intention="Refactorer le CLI du quiz benchmark" \
  -PspecsDir=specs/
```

### 3. Surcharger le point de terminaison LLM

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="deepseek-v4-pro:cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## Tâches disponibles

| Tâche | Groupe | Description |
|-------|--------|-------------|
| `generatePlan` | generate | Décompose une intention en langage naturel en un plan d'exécution structuré (EPICs → User Stories → Tasks). Option `-PspecsDir=/path/to/specs` pour le contexte RAG. |

> La tâche `vibecode` a été supprimée de planner (résolution split-brain).
> Elle vit uniquement dans `codebase-gradle` (N1) : `./gradlew :codebase-plugin:vibecode --intention="..."`.

## DSL d'extension

```gradle
planner {
    ollamaModel    = "deepseek-v4-pro:cloud"   // défaut
    ollamaBaseUrl  = "http://localhost:11434"  // défaut
    intention      = "Votre intention par défaut"  // optionnel, surchargeable via -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // source RAG optionnelle
}
```

Toutes les propriétés d'extension sont surchargeables à l'appel via les propriétés Gradle :
`-Pintention`, `-PspecsDir`, `-PollamaModel`, `-PollamaBaseUrl`.

## Prérequis

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 pour l'auto-provisioning du toolchain)
- **Ollama** en local (ou distant), servant `deepseek-v4-pro:cloud`
- Les ports `11434–11436` sont interdits globalement ; rotation sur `11437–11465`.
  Modèles autorisés : `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Build & tests

```bash
./gradlew :planner-plugin:build              # build complet (compile + tests)
./gradlew :planner-plugin:build -x test      # compilation seule
./gradlew :planner-plugin:test               # tests unitaires JUnit5
./gradlew :planner-plugin:publishToMavenLocal # publication locale
```

## Utilisation CI (déclenchement manuel)

Le workflow `decompose.yml` expose un trigger `workflow_dispatch` pour générer un plan
depuis l'UI GitHub Actions :

- Entrées : `intention` (requis), `feature_request_id` (optionnel)
- Pull `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` depuis Ollama cloud
- Committe le plan généré sous `features/plans/` (quand un id de feature request est fourni)
- Upload l'artefact `build/planning/*.json`

## Dépannage

| Symptôme | Correctif |
|----------|-----------|
| `Connection refused localhost:11434` | Démarrer Ollama : `ollama serve` ; puller le modèle : `ollama pull deepseek-v4-pro:cloud` |
| Le LLM renvoie du JSON malformé | Retry (retry 3× intégré dans `OllamaBridge`) ; vérifier le budget de tokens dans `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| Port `11434` interdit | Utiliser un port dans `11437–11465` ; passer `-PollamaBaseUrl=http://localhost:11437` |

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Fait partie de l'écosystème CCCP Education — `groupId: education.cccp`._