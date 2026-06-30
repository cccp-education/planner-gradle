<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — داخليات الإضافة

> دليل المطوّر والمساهم لإضافة Gradle `planner-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.1` · **المجموعة**: `education.cccp` · **مُعرِّف الإضافة**: `education.cccp.planner`
- **سلسلة الأدوات**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **البناء**: `./gradlew :planner-plugin:build -x test` · **الاختبارات**: `./gradlew :planner-plugin:test`

🌐 اللغات: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## تخطيط الوحدة

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # نقطة دخول الإضافة — تسجّل مهمة generatePlan
        ├── PlannerExtension.kt        # DSL الامتداد (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # تنفيذ مهمة generatePlan
        ├── IntentionPlanner.kt        # بناء الموجّه + تنسيق LLM + سياق مركّب
        ├── OllamaBridge.kt            # غلاف LangChain4j ChatModel + إعادة محاولة 3×
        ├── LLMResponse.kt             # تحليل الاستجابة الخام لـ LLM
        ├── Plan.kt                    # أصنف بيانات Plan / EPIC / UserStory / GradleTask
        ├── PlanningContext.kt         # كائن قيمة سياق التخطيط
        ├── SpecReader.kt              # RAG على المواصفات الموجودة (ميزانية الرموز 2000)
        ├── Metadata.kt                # صيغة محورية — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # إخراج stdout مهيكل
```

## عقود N0 (من workspace-bom MEMPHIS)

| العقد | القطعة | يوفّر |
|-------|--------|-------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> يستهلك planner فقط `codebase-contracts` (المصدر الوحيد للحقيقة لقنوات السياق). لا يعتمد
> **على** `codebase-plugin` (N1) — تمت إزالة مهمة `vibecode` من planner أثناء حل split-brain
> (session 049).

## المكتبات الرئيسية

- **langchain4j** 1.14.1 — مزوّدو LLM (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — DSL Kotlin للرسوم البيانية الوكيلة (التنسيق)
- **kotlinx-serialization-json** 1.7.3 — I/O JSON مهيكل
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — تحليل استجابات LLM
- **gradle-plugin-publish** 2.1.0 — النشر على Gradle Plugin Portal

## نسخ Ollama (القيد العالمي)

المنافذ `11434–11436` محظورة. التناوب على `11437–11465` (29 منفذًا).
النماذج المصرّح بها: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
النموذج الافتراضي في `PlanningPlugin`: `gpt-oss:120b-cloud`.

## مصفوفة الاختبارات

| المهمة | النطاق | الملاحظات |
|--------|--------|-----------|
| `:planner-plugin:test` | اختبارات وحدة JUnit5 | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, تسجيل كامل للاستثناءات |

أصناف الاختبار (8 ملفات تحت `src/test/kotlin/planning/`):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

لا توجد مجموعات Cucumber — planner يستخدم JUnit5 نقيًا (لا تقسيم `testFast`/`testAll`/`testEpics`).
لا يوجد بوابة تغطية Kover مكوّنة في هذا البناء.

## ضبط JVM

تعمل الاختبارات مع `-XX:+EnableDynamicAgentLoading`. لعمليات تكامل LLM الثقيلة:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## أوامر البناء

```bash
./gradlew :planner-plugin:build                       # بناء كامل (ترجمة + اختبارات)
./gradlew :planner-plugin:build -x test               # ترجمة فقط
./gradlew :planner-plugin:test                        # اختبارات وحدة JUnit5
./gradlew :planner-plugin:publishToMavenLocal         # نشر محلي
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## خط الأنابيب CI

يُعرِّف `.github/workflows/` مساري عمل:

1. **test.yml** — `./gradlew :planner-plugin:build` عند push/PR إلى `main`/`master`
   (JDK 24 Temurin, مهلة 15 دقيقة, `gradle/actions/setup-gradle@v4`).
2. **decompose.yml** — مُشغِّل يدوي `workflow_dispatch`: يُثبّت Ollama، يضبط مفتاح الجهاز
   `OLLAMA_DEVICE_KEY_A`، يسحب `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud`,
   يُشغّل `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...`، يلتزم الخطة
   تحت `features/plans/`، يرفع قطعة `build/planning/*.json`.

## النشر (NMCP)

النشر إلى Maven Central يستخدم `com.gradleup.nmcp` (مكوّن في `settings.gradle.kts`،
نوع النشر `AUTOMATIC`). يُعرِّف `build.gradle.kts`:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- POM على `withType<MavenPublication>`: اسم، وصف، رخصة Apache 2.0،
  مطوّر `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (بوابة مركزية، لا staging القديم لـ Sonatype)
- `signing { useGpgCmd() }` — يوقّع إلا إذا كان `CI=true` أو نسخة `-SNAPSHOT`
- `java { withJavadocJar(); withSourcesJar() }`

نشر Gradle Plugin Portal يستخدم `com.gradleup.plugin-publish` 2.1.0
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`).

جميع تبعيات `implementation` إصدارات نهائية (لا `-SNAPSHOT`)؛ `codebase-contracts:0.0.1`
منشور بالفعل على Maven Central.

## حالة EPICs

جميع EPICs مغلقة في `0.0.1` (راجع `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1.

## المساهمة

1. يترجم البناء: `./gradlew :planner-plugin:build -x test`
2. اختبارات الوحدة خضراء: `./gradlew :planner-plugin:test`
3. احترام حدود DAG: planner هو N2 — قابل للاستيراد من N3 `runner-gradle`، لا يستورد
   أبدًا N3. لا تعتمد أبدًا على `codebase-plugin` (N1)؛ استخدم `codebase-contracts` (N0).
4. بعد أي تغيير في المصدر: `./gradlew :planner-plugin:publishToMavenLocal` (القاعدة 2).
5. اتبع تصنيف الأفعال الأربعة (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) و
   صيغة المحور `Metadata.kt` (EPIC K-2).

## وثائق البنية

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, الجلسات, الحوكمة
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — القواعد المطلقة (5 قواعد)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — قائمة افتتاح الجلسة
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — أنطولوجيا workspace (4 أفعال)

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._