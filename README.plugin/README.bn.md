<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — প্লাগইন অভ্যন্তরীণ

> `planner-plugin` Gradle প্লাগইনের জন্য ডেভেলপার ও অবদানকারী গাইড।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.1` · **গোষ্ঠী**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.planner`
- **টুলচেইন**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **বিল্ড**: `./gradlew :planner-plugin:build -x test` · **পরীক্ষা**: `./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## মডিউল বিন্যাস

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # প্লাগইন প্রবেশ বিন্দু — generatePlan কার্য নিবন্ধন করে
        ├── PlannerExtension.kt        # এক্সটেনশন DSL (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # generatePlan কার্য বাস্তবায়ন
        ├── IntentionPlanner.kt        # প্রম্পট নির্মাণ + LLM অর্কেস্ট্রেশন + যৌথ প্রসঙ্গ
        ├── OllamaBridge.kt            # LangChain4j ChatModel র্যাপার + 3× রিট্রাই
        ├── LLMResponse.kt             # LLM অপরিশোধিত প্রতিক্রিয়া পার্সিং
        ├── Plan.kt                    # Plan / EPIC / UserStory / GradleTask ডেটা ক্লাস
        ├── PlanningContext.kt         # প্ল্যানিং প্রসঙ্গ ভ্যালু অবজেক্ট
        ├── SpecReader.kt              # বিদ্যমান স্পেক্সে RAG (টোকেন বাজেট 2000)
        ├── Metadata.kt                # ফরম্যাট পিভট — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # কাঠামোগত stdout আউটপুট
```

## N0 চুক্তি (workspace-bom MEMPHIS থেকে)

| চুক্তি | আর্টিফ্যাক্ট | প্রদান করে |
|--------|--------------|------------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner শুধমাত্র `codebase-contracts` গ্রহণ করে (প্রসঙ্গ চ্যানেলের জন্য একমাত্র সত্যের উৎস)।
> এটি `codebase-plugin` (N1) এর উপর নির্ভর করে **না** — split-brain সমাধানের সময় `vibecode` কার্যটি
> planner থেকে সরানো হয়েছিল (session 049)।

## মূল লাইব্রেরিসমূহ

- **langchain4j** 1.14.1 — LLM প্রদানকারী (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — এজেন্টিক গ্রাফের জন্য Kotlin DSL (অর্কেস্ট্রেশন)
- **kotlinx-serialization-json** 1.7.3 — কাঠামোগত JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — LLM প্রতিক্রিয়া পার্সিং
- **gradle-plugin-publish** 2.1.0 — Gradle Plugin Portal প্রকাশন

## Ollama ইনস্ট্যন্স (বৈশ্বিক সীমাবদ্ধতা)

পোর্ট `11434–11436` নিষিদ্ধ। `11437–11465` এ রোটেশন (29 পোর্ট)।
অনুমোদিত মডেল: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।
`PlanningPlugin` এ ডিফল্ট মডেল: `deepseek-v4-pro:cloud`।

## পরীক্ষা ম্যাট্রিক্স

| কাজ | পরিধি | মন্তব্য |
|-----|-------|--------|
| `:planner-plugin:test` | JUnit5 একক পরীক্ষা | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, পূর্ণ ব্যতিক্রম লগিং |

পরীক্ষা ক্লাস (`src/test/kotlin/planning/` এর অধীনে 8 ফাইল):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

কোনো Cucumber স্যুট নেই — planner শুদ্ধ JUnit5 ব্যবহার করে (কোনো `testFast`/`testAll`/`testEpics` বিভাজন নেই)।
এই বিল্ডে কোনো Kover কভারেজ গেট কনফিগার করা নেই।

## JVM টিউনিং

পরীক্ষাগুলি `-XX:+EnableDynamicAgentLoading` সহ চলে। ভারী LLM ইন্টিগ্রেশন রানের জন্য:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## বিল্ড কমান্ড

```bash
./gradlew :planner-plugin:build                       # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew :planner-plugin:build -x test               # শুধু কম্পাইল
./gradlew :planner-plugin:test                        # JUnit5 একক পরীক্ষা
./gradlew :planner-plugin:publishToMavenLocal         # স্থানীয় প্রকাশন
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI পাইপলাইন

`.github/workflows/` দুটি ওয়ার্কফ্লো নির্ধারণ করে:

1. **test.yml** — `main`/`master` এ push/PR এর সময় `./gradlew :planner-plugin:build`
   (JDK 24 Temurin, 15 মিনিট timeout, `gradle/actions/setup-gradle@v4`)।
2. **decompose.yml** — ম্যানুয়াল ট্রিগার `workflow_dispatch`: Ollama ইনস্টল করে, device key
   `OLLAMA_DEVICE_KEY_A` সেট করে, `qwen3.5:397b-cloud` + `deepseek-v4-pro:cloud` টানে,
   `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...` চালায়, প্ল্যান কে
   `features/plans/` এর অধীনে কমিট করে, `build/planning/*.json` আর্টিফ্যাক্ট আপলোড করে।

## প্রকাশন (NMCP)

Maven Central এ প্রকাশন `com.gradleup.nmcp` ব্যবহার করে (`settings.gradle.kts` এ কনফিগার করা,
প্রকাশন প্রকার `AUTOMATIC`)। `build.gradle.kts` ঘোষণা করে:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- `withType<MavenPublication>` এ POM: নাম, বিবরণ, Apache 2.0 লাইসেন্স,
  ডেভেলপার `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, not legacy Sonatype staging)
- `signing { useGpgCmd() }` — `CI=true` বা `-SNAPSHOT` সংস্করণ না হলে স্বাক্ষর করে
- `java { withJavadocJar(); withSourcesJar() }`

Gradle Plugin Portal প্রকাশন `com.gradleup.plugin-publish` 2.1.0 ব্যবহার করে
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`)।

সমস্ত `implementation` নির্ভরতা রিলিজ (কোনো `-SNAPSHOT` নেই); `codebase-contracts:0.0.1`
আগেই Maven Central এ প্রকাশিত।

## EPIC স্থিতি

সমস্ত EPICs `0.0.1` এ বন্ধ (দেখুন `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1।

## অবদান

1. বিল্ড কম্পাইল হয়: `./gradlew :planner-plugin:build -x test`
2. একক পরীক্ষা সবুজ: `./gradlew :planner-plugin:test`
3. DAG সীমানা সম্মান করুন: planner হল N2 — N3 `runner-gradle` দ্বারা আমদানিযোগ্য, কখনও
   N3 আমদানি করে না। কখনও `codebase-plugin` (N1) এর উপর নির্ভর করবেন না; `codebase-contracts` (N0) ব্যবহার করুন।
4. যেকোনো সোর্স পরিবর্তনের পরে: `./gradlew :planner-plugin:publishToMavenLocal` (নিয়ম 2)।
5. 4-ক্রিয়া শ্রেণীবিন্যাস (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) এবং
   `Metadata.kt` ফরম্যাট পিভট (EPIC K-2) অনুসরণ করুন।

## আর্কিটেকচার ডকস

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, সেশন, প্রশাসন
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — নিরপেক্ষ নিয়ম (5 নিয়ম)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — সেশন প্রারম্ভ চেকলিস্ট
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — ওয়ার্কস্পেস অন্টোলজি (4 ক্রিয়া)

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_