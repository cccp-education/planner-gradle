<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — پلگ اِن کی اندرونی ساخت

> Gradle پلگ اِن `planner-plugin` کے لیے ڈویلپر اور معاون کا گائیڈ۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.1` · **گروپ**: `education.cccp` · **پلگ اِن آئی ڈی**: `education.cccp.planner`
- **ٹول چین**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **بلڈ**: `./gradlew :planner-plugin:build -x test` · **ٹیسٹ**: `./gradlew :planner-plugin:test`

🌐 زبانیں: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## ماڈیول ترتیب

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # پلگ اِن کا داخلہ بِند — generatePlan کام رجسٹر کرتا ہے
        ├── PlannerExtension.kt        # توسیع DSL (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # generatePlan کام کا نفاذ
        ├── IntentionPlanner.kt        # پرامپٹ سازی + LLM آركیسٹریشن + مجموعی سیاق
        ├── OllamaBridge.kt            # LangChain4j ChatModel ریپر + 3× ری-ٹرائی
        ├── LLMResponse.kt             # LLM خام ردعمل پارسنگ
        ├── Plan.kt                    # Plan / EPIC / UserStory / GradleTask ڈیٹا کلاسز
        ├── PlanningContext.kt         # پلاننگ سیاق ویلیو آبجیکٹ
        ├── SpecReader.kt              # موجودہ اسپیکس پر RAG (ٹوکن بجٹ 2000)
        ├── Metadata.kt                # فارمیٹ پِیوٹ — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # ساختاری stdout آؤٹ پٹ
```

## N0 معاہدے (workspace-bom MEMPHIS سے)

| معاہدہ | آرٹی فیکٹ | فراہم کرتا ہے |
|--------|-----------|---------------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner صرف `codebase-contracts` استعمال کرتا ہے (سیاق چینلز کے لیے واحد حق کا منبع)۔
> یہ `codebase-plugin` (N1) پر منحصر **نہیں** — `vibecode` کام کو split-brain حل کے دوران
> planner سے ہٹا دیا گیا (session 049)۔

## کلیدی لائبریریاں

- **langchain4j** 1.14.1 — LLM فراہم کنندگان (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — ایجنٹک گرافس کے لیے Kotlin DSL (آرکیسٹریشن)
- **kotlinx-serialization-json** 1.7.3 — ساختاری JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — LLM ردعمل پارسنگ
- **gradle-plugin-publish** 2.1.0 — Gradle Plugin Portal اشاعت

## Ollama اِنسٹینسز (عالمی پابندی)

پورٹس `11434–11436` ممنوع ہیں۔ `11437–11465` پر روٹیشن (29 پورٹس)۔
مجاز ماڈلز: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`۔
`PlanningPlugin` میں طے شدہ ماڈل: `gpt-oss:120b-cloud`۔

## ٹیسٹ میٹرکس

| کام | دائرہ | نوٹس |
|-----|-------|------|
| `:planner-plugin:test` | JUnit5 یونٹ ٹیسٹ | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, مکمل استثناء لاگنگ |

ٹیسٹ کلاسز (`src/test/kotlin/planning/` کے تحت 8 فائلز):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

کوئی Cucumber سوٹس نہیں — planner خالص JUnit5 استعمال کرتا ہے (کوئی `testFast`/`testAll`/`testEpics` تقسیم نہیں)۔
اس بلڈ میں کوئی Kover کوریج گیٹ کنفیگر نہیں ہے۔

## JVM ٹیوننگ

ٹیسٹ `-XX:+EnableDynamicAgentLoading` کے ساتھ چلتے ہیں۔ بھاری LLM انٹیگریشن رنز کے لیے:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## بلڈ کمانڈز

```bash
./gradlew :planner-plugin:build                       # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew :planner-plugin:build -x test               # صرف کمپائل
./gradlew :planner-plugin:test                        # JUnit5 یونٹ ٹیسٹ
./gradlew :planner-plugin:publishToMavenLocal         # مقامی اشاعت
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI پائپ لائن

`.github/workflows/` دو ورک فلووز متعین کرتا ہے:

1. **test.yml** — `main`/`master` پر push/PR پر `./gradlew :planner-plugin:build`
   (JDK 24 Temurin, 15 منٹ timeout, `gradle/actions/setup-gradle@v4`)۔
2. **decompose.yml** — مینول ٹرگر `workflow_dispatch`: Ollama انسٹال کرتا ہے، ڈیوائس کلید
   `OLLAMA_DEVICE_KEY_A` سیٹ کرتا ہے، `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` کھینچتا ہے،
   `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...` چلاتا ہے، پلان کو
   `features/plans/` کے تحت کمٹ کرتا ہے، `build/planning/*.json` آرٹی فیکٹ اپ لوڈ کرتا ہے۔

## اشاعت (NMCP)

Maven Central پر اشاعت `com.gradleup.nmcp` استعمال کرتی ہے (`settings.gradle.kts` میں کنفیگر،
اشاعت کی قسم `AUTOMATIC`)۔ `build.gradle.kts` اعلان کرتا ہے:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- `withType<MavenPublication>` پر POM: نام، تفصیل، Apache 2.0 لائسنس،
  ڈویلپر `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, not legacy Sonatype staging)
- `signing { useGpgCmd() }` — `CI=true` یا `-SNAPSHOT` ورژن نہ ہونے پر دستخط کرتا ہے
- `java { withJavadocJar(); withSourcesJar() }`

Gradle Plugin Portal اشاعت `com.gradleup.plugin-publish` 2.1.0 استعمال کرتی ہے
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`)۔

تمام `implementation` انحصارات ریلیزز ہیں (کوئی `-SNAPSHOT` نہیں)؛ `codebase-contracts:0.0.1`
پہلے ہی Maven Central پر اشاعت شد ہے۔

## EPIC حالت

تمام EPICs `0.0.1` میں بند (دیکھیں `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1۔

## معاونیت

1. بلڈ کمپائل ہو: `./gradlew :planner-plugin:build -x test`
2. یونٹ ٹیسٹ سبز: `./gradlew :planner-plugin:test`
3. DAG سرحد کی تعمیل کریں: planner N2 ہے — N3 `runner-gradle` کے ذریعے درآمد قابل، کبھی
   N3 درآمد نہیں کرتا۔ کبھی `codebase-plugin` (N1) پر منحصر نہ ہوں; `codebase-contracts` (N0) استعمال کریں۔
4. کسی بھی سورس تبدیلی کے بعد: `./gradlew :planner-plugin:publishToMavenLocal` (قاعدہ 2)۔
5. 4-فعل 分类 (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) اور
   `Metadata.kt` فارمیٹ پِیوٹ (EPIC K-2) پر عمل کریں۔

## فن تعمیر دستاویزات

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, جلسات, حکمرانی
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — مطلق قواعد (5 قواعد)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — جلسہ آغاز چیک لسٹ
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — ورک سپیس اونٹولوجی (4 افعال)

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔_