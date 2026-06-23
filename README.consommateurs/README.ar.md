<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — دليل المستهلك

> إضافة Gradle لتخطيط الخبراء — تُفكّك نية مُعبَّر عنها بلغة طبيعية إلى خطة تنفيذ مهيكلة (EPICs → User Stories → Tasks) عبر LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.1` · **المجموعة**: `education.cccp` · **مُعرِّف الإضافة**: `education.cccp.planner`
- **البناء**: `./gradlew :planner-plugin:build` · **الاختبارات**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 اللغات: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا يفعل

يأخذ `planner-gradle` نية عالية المستوى مُعبَّر عنها بلغة طبيعية ويُفكّكها إلى خطة تنفيذ
مهيكلة — EPICs → User Stories → مهام Gradle — عبر LLM (LangChain4j + DeepSeek-v4-pro
مُقدَّم من Ollama). المخرجات هي stdout مهيكل بالإضافة إلى قطعة JSON للخطة تحت `build/planning/`.

جزء من منظومة CCCP Education متعددة الإضافات:

```
نية المستخدم → [planner-gradle] → LLM (Ollama) → خطة مهيكلة (EPICs/US/مهام)
```

يستهلك عقد N0 `codebase-contracts` (`ContextChannel`، `ChannelBudget`،
`CompositeContext`، `CompositeContextConfig`) كمصدر وحيد للحقيقة لنمذجة قنوات السياق.

## البداية السريعة

### 1. تطبيق الإضافة

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. توليد خطة

```bash
./gradlew generatePlan \
  --intention="إضافة مهمة تصدير PDF إلى خطوط تدريب الفريق"
```

سياق RAG اختياري من مواصفات موجودة:

```bash
./gradlew generatePlan \
  --intention="إعادة هيكلة واجهة سطر أوامر quiz benchmark" \
  -PspecsDir=specs/
```

### 3. تجاوز نقطة نهاية LLM

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="deepseek-v4-pro:cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|--------|----------|-------|
| `generatePlan` | generate | تُفكّك نية بلغة طبيعية إلى خطة تنفيذ مهيكلة (EPICs → User Stories → Tasks). خيار `-PspecsDir=/path/to/specs` لسياق RAG. |

> تمت إزالة مهمة `vibecode` من planner (حل split-brain).
> تعيش فقط في `codebase-gradle` (N1): `./gradlew :codebase-plugin:vibecode --intention="..."`.

## DSL الامتداد

```gradle
planner {
    ollamaModel    = "deepseek-v4-pro:cloud"   // افتراضي
    ollamaBaseUrl  = "http://localhost:11434"  // افتراضي
    intention      = "Your default intention"  // اختياري، قابل للتجاوز عبر -Pintention
    specsDir       = layout.projectDirectory.dir("specs")  // مصدر RAG اختياري
}
```

جميع خصائص الامتداد قابلة للتجاوز لكل استدعاء عبر خصائص Gradle:
`-Pintention`، `-PspecsDir`، `-PollamaModel`، `-PollamaBaseUrl`.

## المتطلبات المسبقة

- **Java** 24+ (سلسلة أدوات Kotlin 2.3.20)
- **Gradle** 9.5+ (foojay-resolver-convention 1.0.0 للتزوير التلقائي لسلسلة الأدوات)
- **Ollama** يعمل محليًا (أو عن بُعد)، يُقدّم `deepseek-v4-pro:cloud`
- المنافذ `11434–11436` محظورة عالميًا؛ التناوب على `11437–11465`.
  النماذج المصرّح بها: `gpt-oss:120b-cloud`، `gemma4:31b-cloud`.

## البناء والاختبار

```bash
./gradlew :planner-plugin:build              # بناء كامل (ترجمة + اختبارات)
./gradlew :planner-plugin:build -x test      # ترجمة فقط
./gradlew :planner-plugin:test               # اختبارات وحدة JUnit5
./gradlew :planner-plugin:publishToMavenLocal # نشر محلي
```

## الاستخدام في CI (إرسال يدوي)

يكشف workflow `decompose.yml` عن مُشغِّل `workflow_dispatch` لتوليد خطة من واجهة GitHub Actions:

- المدخلات: `intention` (مطلوب)، `feature_request_id` (اختياري)
- يسحب `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` من سحابة Ollama
- يلتزم الخطة المُولَّدة تحت `features/plans/` (عند تقديم feature request id)
- يرفع قطعة `build/planning/*.json`

## استكشاف الأخطاء وإصلاحها

| العَرَض | الإصلاح |
|---------|---------|
| `Connection refused localhost:11434` | ابدأ Ollama: `ollama serve`؛ اسحب النموذج: `ollama pull deepseek-v4-pro:cloud` |
| LLM يُعيد JSON تالفًا | أعد المحاولة (إعادة محاولة 3× مدمجة في `OllamaBridge`)؛ تحقق من ميزانية الرموز في `IntentionPlanner` |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| المنفذ `11434` محظور | استخدم منفذًا في `11437–11465`؛ مرّر `-PollamaBaseUrl=http://localhost:11437` |

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._