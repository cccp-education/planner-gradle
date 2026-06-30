<!-- translated from README.md rev 0.0.1 -->
# planner-gradle — उपभोक्ता गाइड

> Planning Expert Gradle प्लगइन — LLM के माध्यम से प्राकृतिक भाषा के इरादे को संरचित निष्पादन योजना (EPICs → User Stories → Tasks) में विघटित करता है।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/planner-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/planner-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.planner.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.planner)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/planner-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/planner-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/planner-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.1` · **समूह**: `education.cccp` · **प्लगइन ID**: `education.cccp.planner`
- **बिल्ड**: `./gradlew :planner-plugin:build` · **परीक्षण**: `./gradlew :planner-plugin:test` (JUnit5)

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`planner-gradle` प्राकृतिक भाषा में व्यक्त उच्च-स्तरीय इरादे को लेता है और उसे LLM
(LangChain4j + Ollama द्वारा सेवित DeepSeek-v4-pro) के माध्यम से एक संरचित निष्पादन योजना
—— EPICs → User Stories → Gradle tasks में विघटित करता है। आउटपुट संरचित stdout तथा
`build/planning/` के अंतर्गत एक JSON योजना आर्टिफैक्ट है।

CCCP Education बहु-प्लगइन पारिस्थितिकी तंत्र का भाग:

```
उपयोगकर्ता इरादा → [planner-gradle] → LLM (Ollama) → संरचित योजना (EPICs/US/Tasks)
```

यह संदर्भ-चैनल मॉडलिंग के लिए एकमात्र सत्य स्रोत के रूप में N0 `codebase-contracts`
(`ContextChannel`、`ChannelBudget`、`CompositeContext`、`CompositeContextConfig`) का उपभोग करता है।

## त्वरित प्रारंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.planner") version "0.0.1"
}
```

### 2. योजना उत्पन्न करें

```bash
./gradlew generatePlan \
  --intention="प्रशिक्षण पाइपलाइन में एक PDF निर्यात कार्य जोड़ें"
```

मौजूदा स्पेक्स से वैकल्पिक RAG संदर्भ:

```bash
./gradlew generatePlan \
  --intention="quiz benchmark CLI को पुनर्गठित करें" \
  -PspecsDir=specs/
```

### 3. LLM एंडपॉइंट अध्यारोहित करें

```bash
./gradlew generatePlan \
  --intention="..." \
  --ollamaModel="gpt-oss:120b-cloud" \
  --ollamaBaseUrl="http://localhost:11434"
```

## उपलब्ध कार्य

| कार्य | समूह | विवरण |
|-------|------|--------|
| `generatePlan` | generate | प्राकृतिक भाषा के इरादे को संरचित निष्पादन योजना (EPICs → User Stories → Tasks) में विघटित करता है। RAG संदर्भ के लिए वैकल्पिक `-PspecsDir=/path/to/specs`। |

> `vibecode` कार्य को planner से हटा दिया गया है (split-brain समाधान)।
> यह केवल `codebase-gradle` (N1) में रहता है: `./gradlew :codebase-plugin:vibecode --intention="..."`।

## एक्सटेंशन DSL

```gradle
planner {
    ollamaModel    = "gpt-oss:120b-cloud"   // डिफ़ॉल्ट
    ollamaBaseUrl  = "http://localhost:11434"  // डिफ़ॉल्ट
    intention      = "Your default intention"  // वैकल्पिक, -Pintention द्वारा अध्यारोहण योग्य
    specsDir       = layout.projectDirectory.dir("specs")  // वैकल्पिक RAG स्रोत
}
```

सभी एक्सटेंशन गुण प्रति-आह्वान Gradle गुणों के माध्यम से अध्यारोहण योग्य हैं:
`-Pintention`、`-PspecsDir`、`-PollamaModel`、`-PollamaBaseUrl`।

## पूर्वापेक्षाएँ

- **Java** 24+ (Kotlin 2.3.20 टूलचेन)
- **Gradle** 9.5+ (टूलचेन ऑटो-प्रोविजनिंग के लिए foojay-resolver-convention 1.0.0)
- **Ollama** स्थानीय रूप से (या दूरस्थ) चल रहा है, `gpt-oss:120b-cloud` सेवित करता है
- पोर्ट `11434–11436` वैश्विक रूप से निषिद्ध हैं; `11437–11465` पर रोटेशन करें।
  अधिकृत मॉडल: `gpt-oss:120b-cloud`、`gemma4:31b-cloud`।

## बिल्ड व परीक्षण

```bash
./gradlew :planner-plugin:build              # पूर्ण बिल्ड (कम्पाइल + परीक्षण)
./gradlew :planner-plugin:build -x test      # केवल कम्पाइल
./gradlew :planner-plugin:test               # JUnit5 इकाई परीक्षण
./gradlew :planner-plugin:publishToMavenLocal # स्थानीय प्रकाशन
```

## CI उपयोग (मैन्युअल डिस्पैच)

`decompose.yml` वर्कफ़्लो GitHub Actions UI से योजना उत्पन्न करने के लिए एक `workflow_dispatch`
ट्रिगर उजागर करता है:

- इनपुट: `intention` (अनिवार्य)、`feature_request_id` (वैकल्पिक)
- Ollama क्लाउड से `qwen3.5:397b-cloud` + `gpt-oss:120b-cloud` खींचता है
- उत्पन्न योजना को `features/plans/` के अंतर्गत कमिट करता है (जब feature request id दिया गया हो)
- `build/planning/*.json` आर्टिफैक्ट अपलोड करता है

## समस्या निवारण

| लक्षण | समाधान |
|-------|--------|
| `Connection refused localhost:11434` | Ollama प्रारंभ करें: `ollama serve`; मॉडल खींचें: `ollama pull gpt-oss:120b-cloud` |
| LLM विकृत JSON लौटाता है | पुनः प्रयास करें (`OllamaBridge` में अंतर्निहित 3× रिट्राय); `IntentionPlanner` में टोकन बजट जाँचें |
| `Java heap space` | `export GRADLE_OPTS="-Xmx2g"` |
| पोर्ट `11434` निषिद्ध | `11437–11465` में एक पोर्ट उपयोग करें; `-PollamaBaseUrl=http://localhost:11437` पास करें |

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का भाग — `groupId: education.cccp`।_