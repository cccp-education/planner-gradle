<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — ভোক্তা গাইড

> Planning Expert Gradle প্লাগইন — প্রাকৃতিক ভাষার অভিপ্রায়কে LLM-এর মাধ্যমে একটি কাঠামোগত সম্পাদন পরিকল্পনায় (EPICs → User Stories → Tasks) বিভেদিত করে।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.1` · **গোষ্ঠী**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.planner`
- **বিল্ড**: `./gradlew :planner-plugin:build` · **পরীক্ষা**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`planner-gradle` প্রাকৃতিক ভাষায় প্রকাশিত একটি উচ্চ-স্তরের অভিপ্রায় গ্রহণ করে এবং এটিকে একটি LLM
(LangChain4j + Ollama দ্বারা পরিবেশিত DeepSeek-v4-pro) এর মাধ্যমে একটি কাঠামোগত সম্পাদন
পরিকল্পনায় —— EPICs → User Stories → Gradle tasks বিভেদিত করে। আউটপুট হলো কাঠামোগত stdout
এবং `build/planning/` এর অধীনে একটি JSON পরিকল্পনা আর্টিফ্যাক্ট।

CCCP Education বহু-প্লাগইন ইকোসিস্টেমের অংশ:

```
ব্যবহারকারী অভিপ্রায় → [planner-gradle] → LLM (Ollama) → কাঠামোগত পরিকল্পনা (EPICs/US/Tasks)
```

এটি প্রসঙ্গ-চ্যানেল মডেলিংয়ের জন্য একমাত্র সত্যের উৎস হিসেবে N0 `codebase-contracts`
(`ContextChannel`、`ChannelBudget`、`CompositeContext`、`CompositeContextConfig`) গ্রহণ করে।

## দ্রুত শুরু

### 1. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. একটি পরিকল্পনা তৈরি করুন

```bash
./gradlew generatePlan \
  --intention="প্রশিক্ষণ পাইপলাইনে একটি PDF রপ্তানি কাজ যোগ করুন"
```

বিদ্যমান স্পেকস থেকে ঐচ্ছিক RAG প্রসঙ্গ:

```bash
./gradlew generatePlan \
  --intention="quiz benchmark CLI পুনর্গঠন করুন" \
  -PspecsDir=specs/
```

### 3. LLM এন্ডপয়েন্ট অধ্যারোহণ করুন

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="deepseek-v4-pro:cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## উপলব্ধ কাজ

| কাজ | গোষ্ঠী | বিবরণ |
|-----|--------|--------|
| `generatePlan` | generate | প্রাকৃতিক ভাষার অভিপ্রায়কে একটি কাঠামোগত সম্পাদন পরিকল্পনায় (EPICs → User Stories → Tasks) বিভেদিত করে। RAG প্রসঙ্গের জন্য ঐচ্ছিক `-PspecsDir=/path/to/specs`। |

> `vibecode` কাজটি planner থেকে সরানো হয়েছে (split-brain সমাধান)।
> এটি শুধুমাত্র `codebase-gradle` (N1)-এ বাস করে: `./gradlew :codebase-plugin:vibecode --intention="..."`।

## এক্সটেনশন DSL

```gradle
planner {
    ollamaModel    = "deepseek-v4-pro:cloud"   // ডিফল্ট
    ollamaBaseUrl  = "http://localhost:11434"  // ডিফল্ট
    intention      = "Your default intention"  // ঐচ্ছিক, -Pintention দ্বারা অধ্যারোহণযোগ্য
    specsDir       = layout.projectDirectory.dir("specs")  // ঐচ্ছিক RAG উৎস
}
```

সমস্ত এক্সটেনশন বৈশিষ্ট্য প্রতি-আহ্বান Gradle বৈশিষ্ট্যের মাধ্যমে অধ্যারোহণযোগ্য:
`-Pintention`、`-PspecsDir`、`-PollamaModel`、`-PollamaBaseUrl`।

## পূর্বশর্ত

- **Java** 24+ (Kotlin 2.3.20 টুলচেইন)
- **Gradle** 9.5+ (টুলচেইন স্বয়ংক্রিয়-প্রোভিশনিংয়ের জন্য foojay-resolver-convention 1.0.0)
- **Ollama** স্থানীয়ভাবে (বা দূরবর্তী) চলমান, `deepseek-v4-pro:cloud` পরিবেশন করছে
- পোর্ট `11434–11436` বিশ্বব্যাপী নিষিদ্ধ; `11437–11465` এ রোটেশন করুন।
  অনুমোদিত মডেল: `gpt-oss:120b-cloud`、`gemma4:31b-cloud`।

## বিল্ড ও পরীক্ষা

```bash
./gradlew :planner-plugin:build              # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew :planner-plugin:build -x test      # শুধু কম্পাইল
./gradlew :planner-plugin:test               # JUnit5 একক পরীক্ষা
./gradlew :planner-plugin:publishToMavenLocal # স্থানীয় প্রকাশন
```

## CI ব্যবহার (ম্যানুয়াল ডিসপ্যাচ)

`decompose.yml` ওয়ার্কফ্লো GitHub Actions UI থেকে একটি পরিকল্পনা তৈরি করার জন্য একটি
`workflow_dispatch` ট্রিগার উন্মোচন করে:

- ইনপুট: `intention` (আবশ্যক)、`feature_request_id` (ঐচ্ছিক)
- Ollama ক্লাউড থেকে `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` টানে
- উৎপন্ন পরিকল্পনা `features/plans/` এর অধীনে কমিট করে (যখন feature request id দেওয়া হয়)
- `build/planning/*.json` আর্টিফ্যাক্ট আপলোড করে

## সমস্যার সমাধান

| লক্ষণ | সমাধান |
|-------|--------|
| `Connection refused localhost:11434` | Ollama শুরু করুন: `ollama serve`; মডেল টানুন: `ollama pull deepseek-v4-pro:cloud` |
| LLM বিকৃত JSON ফেরত দেয় | পুনঃচেষ্টা করুন (`OllamaBridge` এ অন্তর্নিহিত 3× রিট্রাই); `IntentionPlanner` এ টোকেন বাজেট যাচাই করুন |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| পোর্ট `11434` নিষিদ্ধ | `11437–11465` এ একটি পোর্ট ব্যবহার করুন; `-PollamaBaseUrl=http://localhost:11437` পাস করুন |

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_