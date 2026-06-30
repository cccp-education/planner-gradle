<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — صارفین کا گائیڈ

> Planning Expert Gradle پلگ اِن — قدرتی زبان میں اظہارِ ارادے کو LLM کے ذریعے ایک ساختاری انجمن پلان (EPICs → User Stories → Tasks) میں تفکیک کرتا ہے۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.1` · **گروپ**: `education.cccp` · **پلگ اِن آئی ڈی**: `education.cccp.planner`
- **بلڈ**: `./gradlew :planner-plugin:build` · **ٹیسٹ**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 زبانیں: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`planner-gradle` قدرتی زبان میں اظہارِ شدہ اعلیٰ سطح کے ارادے کو لیتا ہے اور اسے LLM
(LangChain4j + Ollama کے ذریعے فراہم کردہ DeepSeek-v4-pro) کے ذریعے ایک ساختاری انجمن پلان
—— EPICs → User Stories → Gradle tasks میں تفکیک کرتا ہے۔ آؤٹ پٹ ساختاری stdout اور
`build/planning/` کے تحت ایک JSON پلان آرٹی فیکٹ ہے۔

CCCP Education کثیر-پلگ اِن ماحولیاتی نظام کا حصہ:

```
صارف کا ارادہ → [planner-gradle] → LLM (Ollama) → ساختاری پلان (EPICs/US/Tasks)
```

یہ سیاق و سباق-چینل ماڈلنگ کے لیے واحد حق کے منبع کے طور پر N0 `codebase-contracts`
(`ContextChannel`、`ChannelBudget`、`CompositeContext`、`CompositeContextConfig`) استعمال کرتا ہے۔

## فوری آغاز

### 1. پلگ اِن لگائیں

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. پلان پیدا کریں

```bash
./gradlew generatePlan \
  --intention="تربینی پائپ لائن میں PDF برآمد کا کام شامل کریں"
```

موجودہ اسپیکس سے اختیاری RAG سیاق:

```bash
./gradlew generatePlan \
  --intention="quiz benchmark CLI کی تشکیلِ نو" \
  -PspecsDir=specs/
```

### 3. LLM اینڈ پوائنٹ اوورائیڈ کریں

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="gpt-oss:120b-cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## دستیاب کام

| کام | گروپ | تفصیل |
|-----|------|--------|
| `generatePlan` | generate | قدرتی زبان کے ارادے کو ساختاری انجمن پلان (EPICs → User Stories → Tasks) میں تفکیک کرتا ہے۔ RAG سیاق کے لیے اختیاری `-PspecsDir=/path/to/specs`۔ |

> `vibecode` کام کو planner سے ہٹا دیا گیا ہے (split-brain حل)۔
> یہ صرف `codebase-gradle` (N1) میں رہتا ہے: `./gradlew :codebase-plugin:vibecode --intention="..."`۔

## توسیع DSL

```gradle
planner {
    ollamaModel    = "gpt-oss:120b-cloud"   // طے شدہ
    ollamaBaseUrl  = "http://localhost:11434"  // طے شدہ
    intention      = "Your default intention"  // اختیاری، -Pintention سے اوورائیڈ قابل
    specsDir       = layout.projectDirectory.dir("specs")  // اختیاری RAG منبع
}
```

تمام توسیعی خصوصیات ہر استدعا پر Gradle خصوصیات کے ذریعے اوورائیڈ قابل ہیں:
`-Pintention`、`-PspecsDir`、`-PollamaModel`、`-PollamaBaseUrl`۔

## پیشگی شرائط

- **Java** 24+ (Kotlin 2.3.20 ٹول چین)
- **Gradle** 9.5+ (ٹول چین آٹو-پروویژننگ کے لیے foojay-resolver-convention 1.0.0)
- **Ollama** مقامی طور پر (یا بعید) چل رہا ہے، `gpt-oss:120b-cloud` پیش کر رہا ہے
- پورٹس `11434–11436` عالمی سطح پر ممنوع ہیں؛ `11437–11465` پر روٹیشن کریں۔
  مجاز ماڈلز: `gpt-oss:120b-cloud`、`gemma4:31b-cloud`۔

## بلڈ اور ٹیسٹ

```bash
./gradlew :planner-plugin:build              # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew :planner-plugin:build -x test      # صرف کمپائل
./gradlew :planner-plugin:test               # JUnit5 یونٹ ٹیسٹ
./gradlew :planner-plugin:publishToMavenLocal # مقامی اشاعت
```

## CI استعمال (مینول ڈسپیچ)

`decompose.yml` ورک فلو GitHub Actions UI سے پلان پیدا کرنے کے لیے `workflow_dispatch`
ٹرگر ظاہر کرتا ہے:

- ان پٹس: `intention` (لازمی)、`feature_request_id` (اختیاری)
- Ollama کلاؤڈ سے `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` کھینچتا ہے
- پیدا کردہ پلان کو `features/plans/` کے تحت کمٹ کرتا ہے (جب feature request id دیا گیا ہو)
- `build/planning/*.json` آرٹی فیکٹ اپ لوڈ کرتا ہے

## مسئلہ حل

| علامت | حل |
|-------|-----|
| `Connection refused localhost:11434` | Ollama شروع کریں: `ollama serve`; ماڈل کھینچیں: `ollama pull gpt-oss:120b-cloud` |
| LLM مسخ شدہ JSON لوٹاتا ہے | دوبارہ کوشش کریں (`OllamaBridge` میں اندرونی 3× ری-ٹرائی); `IntentionPlanner` میں ٹوکن بجٹ چیک کریں |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| پورٹ `11434` ممنوع | `11437–11465` میں ایک پورٹ استعمال کریں; `-PollamaBaseUrl=http://localhost:11437` پاس کریں |

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔_