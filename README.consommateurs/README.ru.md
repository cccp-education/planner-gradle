<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — Руководство потребителя

> Gradle-плагин Planning Expert — декомпозирует намерение на естественном языке в структурированный план выполнения (EPICs → User Stories → Tasks) через LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.1` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.planner`
- **Сборка**: `./gradlew :planner-plugin:build` · **Тесты**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`planner-gradle` принимает намерение высокого уровня, выраженное на естественном языке, и
декомпозирует его в структурированный план выполнения — EPICs → User Stories → задачи Gradle —
через LLM (LangChain4j + DeepSeek-v4-pro, обслуживаемый Ollama). Вывод — структурированный
stdout плюс артефакт плана в формате JSON в `build/planning/`.

Часть мульти-плагинной экосистемы CCCP Education:

```
намерение пользователя → [planner-gradle] → LLM (Ollama) → структурированный план (EPICs/US/Задачи)
```

Потребляет контракт N0 `codebase-contracts` (`ContextChannel`, `ChannelBudget`,
`CompositeContext`, `CompositeContextConfig`) как единый источник истины для
моделирования контекстных каналов.

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. Сгенерировать план

```bash
./gradlew generatePlan \
  --intention="Добавить задачу экспорта PDF в конвейер обучения"
```

Опциональный RAG-контекст из существующих спецификаций:

```bash
./gradlew generatePlan \
  --intention="Рефакторинг CLI quiz benchmark" \
  -PspecsDir=specs/
```

### 3. Переопределить endpoint LLM

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="gpt-oss:120b-cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## Доступные задачи

| Задача | Группа | Описание |
|--------|--------|----------|
| `generatePlan` | generate | Декомпозирует намерение на естественном языке в структурированный план выполнения (EPICs → User Stories → Tasks). Опциональный `-PspecsDir=/path/to/specs` для RAG-контекста. |

> Задача `vibecode` была удалена из planner (решение split-brain).
> Она существует только в `codebase-gradle` (N1): `./gradlew :codebase-plugin:vibecode --intention="..."`.

## DSL расширения

```gradle
planner {
    ollamaModel    = "gpt-oss:120b-cloud"   // по умолчанию
    ollamaBaseUrl  = "http://localhost:11434"  // по умолчанию
    intention      = "Your default intention"  // опционально, переопределяется через -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // опциональный RAG-источник
}
```

Все свойства расширения переопределяются при каждом вызове через свойства Gradle:
`-Pintention`, `-PspecsDir`, `-PollamaModel`, `-PollamaBaseUrl`.

## Предварительные требования

- **Java** 24+ (инструментальная цепочка Kotlin 2.3.20)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 для авто-настройки toolchain)
- **Ollama** работает локально (или удалённо), обслуживает `gpt-oss:120b-cloud`
- Порты `11434–11436` запрещены глобально; ротация в `11437–11465`.
  Авторизованные модели: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.

## Сборка и тесты

```bash
./gradlew :planner-plugin:build              # полная сборка (компиляция + тесты)
./gradlew :planner-plugin:build -x test      # только компиляция
./gradlew :planner-plugin:test               # модульные тесты JUnit5
./gradlew :planner-plugin:publishToMavenLocal # локальная публикация
```

## Использование в CI (ручной запуск)

Рабочий процесс `decompose.yml` предоставляет триггер `workflow_dispatch` для генерации плана
из интерфейса GitHub Actions:

- Входные данные: `intention` (обязательно), `feature_request_id` (опционально)
- Загружает `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` из Ollama cloud
- Фиксирует сгенерированный план в `features/plans/` (когда указан feature request id)
- Загружает артефакт `build/planning/*.json`

## Устранение неисправностей

| Симптом | Решение |
|---------|---------|
| `Connection refused localhost:11434` | Запустить Ollama: `ollama serve`; загрузить модель: `ollama pull gpt-oss:120b-cloud` |
| LLM возвращает некорректный JSON | Повторить (встроенная 3× повтор в `OllamaBridge`); проверить бюджет токенов в `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| Порт `11434` запрещён | Использовать порт из `11437–11465`; передать `-PollamaBaseUrl=http://localhost:11437` |

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._