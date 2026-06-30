<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — प्लगइन आंतरिक

> `planner-plugin` Gradle प्लगइन के लिए डेवलपर व योगदानकर्ता गाइड।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.1` · **समूह**: `education.cccp` · **प्लगइन ID**: `education.cccp.planner`
- **टूलचेन**: Java 24 · Kotlin 2.3.20 · Gradle 9.5 (foojay-resolver-convention 1.0.0)
- **बिल्ड**: `./gradlew :planner-plugin:build -x test` · **परीक्षण**: `./gradlew :planner-plugin:test`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## मॉड्यूल अभिन्यास

```
planner-plugin/
└── src/main/kotlin/
    └── planning/
        ├── PlanningPlugin.kt          # प्लगइन प्रवेश बिंदु — generatePlan कार्य पंजीकृत करता है
        ├── PlannerExtension.kt        # एक्सटेंशन DSL (ollamaModel, ollamaBaseUrl, intention, specsDir)
        ├── DecomposeIntentionTask.kt  # generatePlan कार्य कार्यान्वयन
        ├── IntentionPlanner.kt        # प्रॉम्प्ट निर्माण + LLM ऑर्केस्ट्रेशन + समग्र प्रसঙ्ग
        ├── OllamaBridge.kt            # LangChain4j ChatModel रैपर + 3× रिट्राय
        ├── LLMResponse.kt             # LLM अपरिष्कृत प्रतिक्रिया पार्सिंग
        ├── Plan.kt                    # Plan / EPIC / UserStory / GradleTask डेटा क्लासेस
        ├── PlanningContext.kt         # प्लानिंग प्रसङ्क वैल्यू ऑब्जेक्ट
        ├── SpecReader.kt              # मौजूदा स्पेक्स पर RAG (टोकन बजट 2000)
        ├── Metadata.kt                # फॉर्मैट पिवट — metadata.json (EPIC K-2)
        └── StdoutFormatter.kt         # संरचित stdout आउटपुट
```

## N0 अनुबंध (workspace-bom MEMPHIS से)

| अनुबंध | आर्टिफैक्ट | प्रदान करता है |
|--------|-----------|---------------|
| `codebase-contracts` | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext, CompositeContextConfig |

> Planner केवल `codebase-contracts` का उपभोग करता है (प्रसङ्क चैनलों के लिए एकमात्र सत्य स्रोत)।
> यह `codebase-plugin` (N1) पर निर्भर **नहीं** करता — `vibecode` कार्य split-brain समाधान के दौरान
> planner से हटा दिया गया (session 049)।

## प्रमुख लाइब्रेरियाँ

- **langchain4j** 1.14.1 — LLM प्रदाता (`langchain4j`, `langchain4j-ollama`)
- **koog-agents** 0.8.0 — एजेंटिक ग्राफ़ के लिए Kotlin DSL (ऑर्केस्ट्रेशन)
- **kotlinx-serialization-json** 1.7.3 — संरचित JSON I/O
- **Jackson** 2.18.2 (`jackson-databind`, `jackson-module-kotlin`) — LLM प्रतिक्रिया पार्सिंग
- **gradle-plugin-publish** 2.1.0 — Gradle Plugin Portal प्रकाशन

## Ollama इंस्टैंस (वैश्विक बाध्यता)

पोर्ट `11434–11436` निषिद्ध हैं। `11437–11465` पर रोटेशन (29 पोर्ट)।
अधिकृत मॉडल: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।
`PlanningPlugin` में डिफ़ॉल्ट मॉडल: `gpt-oss:120b-cloud`।

## परीक्षण आव्यूह

| कार्य | क्षेत्र | टिप्पणियाँ |
|-------|--------|-----------|
| `:planner-plugin:test` | JUnit5 इकाई परीक्षण | `useJUnitPlatform()`, `-XX:+EnableDynamicAgentLoading`, पूर्ण अपवाद लॉगिंग |

परीक्षण क्लासेस (`src/test/kotlin/planning/` के अंतर्गत 8 फ़ाइलें):

- `IntentionPlannerTest`, `IntentionPlannerMultiCanalTest`
- `LLMResponseTest`, `PlanTest`
- `OllamaBridgeTest`, `SpecReaderTest`
- `DecomposeIntentionPluginTest`, `DecomposeIntentionMultiCanalTest`

कोई Cucumber सूट नहीं — planner शुद्ध JUnit5 का उपयोग करता है (कोई `testFast`/`testAll`/`testEpics` विभाजन नहीं)।
इस बिल्ड में कोई Kover कवरेज गेट विन्यस्त नहीं है।

## JVM ट्यूनिंग

परीक्षण `-XX:+EnableDynamicAgentLoading` के साथ चलते हैं। भारी LLM एकीकरण रन के लिए:

```bash
export GRADLE_OPTS="-Xmx2g"
```

## बिल्ड कमांड

```bash
./gradlew :planner-plugin:build                       # पूर्ण बिल्ड (कम्पाइल + परीक्षण)
./gradlew :planner-plugin:build -x test               # केवल कम्पाइल
./gradlew :planner-plugin:test                        # JUnit5 इकाई परीक्षण
./gradlew :planner-plugin:publishToMavenLocal         # स्थानीय प्रकाशन
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
```

## CI पाइपलाइन

`.github/workflows/` दो वर्कफ़्लो परिभाषित करता है:

1. **test.yml** — `main`/`master` पर push/PR पर `./gradlew :planner-plugin:build`
   (JDK 24 Temurin, 15 मिनट timeout, `gradle/actions/setup-gradle@v4`)।
2. **decompose.yml** — मैन्युअल ट्रिगर `workflow_dispatch`: Ollama इंस्टॉल करता है, device key
   `OLLAMA_DEVICE_KEY_A` सेट करता है, `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` खींचता है,
   `./gradlew generatePlan -Pintention=... -Pfeature.request.id=...` चलाता है, प्लान को
   `features/plans/` के अंतर्गत कमिट करता है, `build/planning/*.json` आर्टिफैक्ट अपलोड करता है।

## प्रकाशन (NMCP)

Maven Central पर प्रकाशन `com.gradleup.nmcp` का उपयोग करता है (`settings.gradle.kts` में विन्यस्त,
प्रकाशन प्रकार `AUTOMATIC`)। `build.gradle.kts` घोषित करता है:

- `group = "education.cccp"`, `version = libs.plugins.planner.get().version` (`0.0.1`)
- `withType<MavenPublication>` पर POM: नाम, विवरण, Apache 2.0 लाइसेंस,
  डेवलपर `cccp-education` (`cccp.edu@gmail.com`), SCM →
  `github.com/cheroliv/planner-gradle`
- `repositories { mavenCentral() }` (central portal, not legacy Sonatype staging)
- `signing { useGpgCmd() }` — `CI=true` या `-SNAPSHOT` संस्करण न होने पर हस्ताक्षर करता है
- `java { withJavadocJar(); withSourcesJar() }`

Gradle Plugin Portal प्रकाशन `com.gradleup.plugin-publish` 2.1.0 का उपयोग करता है
(`gradlePlugin { website, vcsUrl, plugins.create("planner") }`)।

सभी `implementation` निर्भरताएँ रिलीज़ हैं (कोई `-SNAPSHOT` नहीं); `codebase-contracts:0.0.1`
पहले से Maven Central पर प्रकाशित है।

## EPIC स्थिति

सभी EPICs `0.0.1` में बंद (देखें `.agents/INDEX.adoc`):
PLN-0 → PLN-8, EPIC K (K-0 → K-5), EPIC ABC-B, Publication Maven Central 0.0.1।

## योगदान

1. बिल्ड कम्पाइल हो: `./gradlew :planner-plugin:build -x test`
2. इकाई परीक्षण हरे: `./gradlew :planner-plugin:test`
3. DAG सीमा का पालन करें: planner N2 है — N3 `runner-gradle` द्वारा आयात योग्य, कभी
   N3 आयात नहीं। `codebase-plugin` (N1) पर कभी निर्भर नहीं; `codebase-contracts` (N0) उपयोग करें।
4. किसी भी स्रोत परिवर्तन के बाद: `./gradlew :planner-plugin:publishToMavenLocal` (नियम 2)।
5. 4-क्रिया वर्गीकरण (GENERER/COLLECTER/TRANSFORMER/DÉPLOYER) और
   `Metadata.kt` फॉर्मैट पिवट (EPIC K-2) का पालन करें।

## आर्किटेक्चर दस्तावेज़

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap, सत्र, शासन
- [.agents/AGENT.adoc](../.agents/AGENT.adoc) — निरपेक्ष नियम (5 नियम)
- [.agents/SESSION_CHECKLIST.adoc](../.agents/SESSION_CHECKLIST.adoc) — सत्र प्रारंभ चेकलिस्ट
- [TAXONOMIE_WORKSPACE.adoc](../../../../TAXONOMIE_WORKSPACE.adoc) — वर्कस्पेस आंटोलॉजी (4 क्रियाएँ)

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का भाग — `groupId: education.cccp`।_