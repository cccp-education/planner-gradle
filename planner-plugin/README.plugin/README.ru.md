<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Внутреннее устройство плагина

> Руководство разработчика и контрибьютора для Gradle-плагина `planner-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.1` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.planner`
- **Инструментальная цепочка**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **Сборка**: `./gradlew :planner-plugin:build -x test` · **Тесты**: `./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Структура модуля

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # Точка входа плагина — регистрирует задачу generatePlan
        ├── PlannerExtension.kt        # DSL расширения (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # Реализация задачи generatePlan
        ├── IntentionPlanner.kt        # Построение промпта + оркестрация LLM + составной контекст
        ├── OllamaBridge.kt            # Обёртка LangChain4j ChatModel + повтор 3×
        ├── LLMResponse.kt             # Разбор сырого ответа LLM
        ├── Plan.kt                    # Классы данных Plan / EPIC / UserStory / GradleTask
        ├── PlanningContext.kt         # Value object контекста планирования
        ├── SpecReader.kt              # RAG по существующим спецификациям (бюджет токенов 2000)
        ├── Metadata.kt                # Формат-пивот — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # Структурированный вывод stdout
```

## Контракты N0 (из workspace-bom MEMPHIS)

| Контракт | Артефакт | Предоставляет |
|----------|----------|---------------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner потребляет только `codebase-contracts` (единственный источник истины для
> контекстных каналов). Он **не** зависит от `codebase-plugin` (N1) — задача
> `vibecode` была удалена из planner во время решения split-brain (session 049).

## Ключевые библиотеки

- **langchain4j** 1.14.1 — провайдеры LLM (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — Kotlin DSL для агентных графов (оркестрация)
- **kotlinx-serialization-json** 1.7.3 — структурированный JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — разбор ответов LLM
- **gradle-plugin-publish** 2.1.0 — публикация в Gradle Plugin Portal

## Инстансы Ollama (глобальное ограничение)

Порты `11434–11436` запрещены. Ротация по `11437–11465` (29 портов).
Авторизованные модели: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Модель по умолчанию в `PlanningPlugin`: `gpt-oss:120b-cloud`.

## Матрица тестов

| Задача | Область | Примечания |
|--------|---------|------------|
| `:planner-plugin:test` | Модульные тесты JUnit5 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, полное логирование исключений |

Тестовые классы (8 файлов в `src/test/kotlin/planning/`):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

Без Cucumber-наборов — planner использует чистый JUnit5 (без разделения `testFast`/`testAll`/`testEpics`).
Шлюз покрытия Kover в этой сборке не настроен.

## Настройка JVM

Тесты запускаются с `-XX:+EnableDynamicAgentLoading`. Для тяжёлых интеграционных прогонов LLM:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## Команды сборки

```bash
./gradlew :planner-plugin:build                       # полная сборка (компиляция + тесты)
./gradlew :planner-plugin:build -x test               # только компиляция
./gradlew :planner-plugin:test                        # модульные тесты JUnit5
./gradlew :planner-plugin:publishToMavenLocal         # локальная публикация
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI-конвейер

`.github/workflows/` определяет два рабочих процесса:

1. **test.yml** — `./gradlew :planner-plugin:build` при push/PR в `main`/`master`
   (JDK 24 Temurin, таймаут 15 мин, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — ручной триггер `workflow_dispatch`: устанавливает Ollama, настраивает
   ключ устройства `OLLAMA_DEVICE_KEY_A`, загружает `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   выполняет `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`, фиксирует план
   в `features/plans/`, загружает артефакт `build/planning/*.json`.

## Публикация (NMCP)

Публикация в Maven Central использует `com.gradleup.nmcp` (настроено в `settings.gradle.kts`,
тип публикации `AUTOMATIC`). `build.gradle.kts` объявляет:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM на `withType<MavenPublication>`: имя, описание, лицензия Apache 2.0,
  разработчик `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, не устаревший staging Sonatype)
- `signing { useGpgCmd() }` — подписывает, если только не `CI=true` или версия `-SNAPSHOT`
- `java { withJavadocJar(); withSourcesJar() }`

Публикация в Gradle Plugin Portal использует `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

Все зависимости `implementation` являются релизами (без `-SNAPSHOT`); `codebase-contracts:0.0.1`
уже опубликован в Maven Central.

## Статус EPIC

Все EPIC закрыты в `0.0.1` (см. `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## Участие

1. Сборка компилируется: `./gradlew :planner-plugin:build -x test`
2. Модульные тесты зелёные: `./gradlew :planner-plugin:test`
3. Соблюдать границу DAG: planner — это N2 — импортируем N3 `runner-gradle`, никогда
   не импортирует N3. Никогда не зависеть от `codebase-plugin` (N1); использовать
   `codebase-contracts` (N0).
4. После любого изменения исходного кода: `./gradlew :planner-plugin:publishToMavenLocal` (правило 2).
5. Следовать таксономии 4 глаголов (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) и
   формату-пивоту `Metadata.kt` (EPIC K-2).

## Документы по архитектуре

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, сессии, управление
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — Абсолютные правила (5 правил)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — Чек-лист начала сессии
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — Онтология рабочего пространства (4 глагола)

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._