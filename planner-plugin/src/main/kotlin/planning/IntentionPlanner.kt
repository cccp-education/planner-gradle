package planning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import contracts.agent.Plan
import dev.langchain4j.data.message.UserMessage
import org.gradle.api.logging.Logger
import org.slf4j.LoggerFactory as Slf4jLoggerFactory

object IntentionPlanner {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private const val MAX_ATTEMPTS = 3

    fun plan(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        logger: Logger,
        ollamaModel: String = "gemma4:31b-cloud",
        ollamaBaseUrl: String = "http://localhost:11437"
    ): Plan {
        val model = OllamaBridge.chatModel(ollamaModel = ollamaModel, ollamaBaseUrl = ollamaBaseUrl)
        val prompt = buildPrompt(intention, context, specContents)

        return callLlm(model, prompt) { msg -> logger.lifecycle(msg) }
    }

    fun plan(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        eagerContext: String,
        ragContext: String,
        graphifyContext: String,
        docsContext: String,
        logger: Logger,
        ollamaModel: String = "gemma4:31b-cloud",
        ollamaBaseUrl: String = "http://localhost:11437"
    ): Plan {
        val model = OllamaBridge.chatModel(ollamaModel = ollamaModel, ollamaBaseUrl = ollamaBaseUrl)
        val prompt = buildPrompt(intention, context, specContents, eagerContext, ragContext, graphifyContext, docsContext)

        return callLlm(model, prompt) { msg -> logger.lifecycle(msg) }
    }

    fun plan(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        eagerContext: String,
        ragContext: String,
        graphifyContext: String,
        docsContext: String,
        ollamaModel: String = "gemma4:31b-cloud",
        ollamaBaseUrl: String = "http://localhost:11437"
    ): Plan {
        val log = Slf4jLoggerFactory.getLogger(IntentionPlanner::class.java)
        val model = OllamaBridge.chatModel(ollamaModel = ollamaModel, ollamaBaseUrl = ollamaBaseUrl)
        val prompt = buildPrompt(intention, context, specContents, eagerContext, ragContext, graphifyContext, docsContext)

        return callLlm(model, prompt) { msg -> log.info(msg) }
    }

    private fun callLlm(model: dev.langchain4j.model.chat.ChatModel, prompt: String, log: (String) -> Unit): Plan {
        var lastError: Exception? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val response = model.chat(UserMessage.from(prompt))
                val raw = response.aiMessage().text()
                log("[LLM] Attempt $attempt/$MAX_ATTEMPTS — ${raw.length} chars received")

                val parsed = mapper.readValue<LLMResponse>(raw)
                log("[LLM] Parsed: ${parsed.epics.size} EPICs, ${parsed.totalPoints} points")
                return parsed.toPlan()
            } catch (e: Exception) {
                lastError = e
                log("[ERROR] Parsing attempt $attempt failed: ${e.message}")
                if (attempt < MAX_ATTEMPTS) {
                    log("[RETRY] Will retry — attempt ${attempt + 1}/$MAX_ATTEMPTS")
                }
            }
        }
        throw IllegalStateException(
            "[FATAL] Failed to parse LLM response after $MAX_ATTEMPTS attempts",
            lastError
        )
    }

    internal fun buildPrompt(intention: String, context: PlanningContext, specContents: List<SpecReader.SpecContent>): String {
        val specsSection = SpecReader.toPromptContext(specContents)
        return buildPromptInternal(intention, specsSection)
    }

    internal fun buildPrompt(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        eagerContext: String,
        ragContext: String,
        graphifyContext: String,
        docsContext: String
    ): String {
        val specsSection = SpecReader.toPromptContext(specContents)
        val eagerBlock = if (eagerContext.isNotBlank()) "\n\nEAGER CONTEXT (governance, EPIC status):\n$eagerContext" else ""
        val ragBlock = if (ragContext.isNotBlank()) "\n\nSEMANTIC CONTEXT (RAG pgvector — codebase chunks):\n$ragContext" else ""
        val graphifyBlock = if (graphifyContext.isNotBlank()) "\n\nGRAPH CONTEXT (structural relations):\n$graphifyContext" else ""
        val docsBlock = if (docsContext.isNotBlank()) "\n\nDOCUMENT CONTEXT (codex corpus — AFNOR, REAC, manuals):\n$docsContext" else ""
        return buildPromptInternal(intention, specsSection, eagerBlock + ragBlock + graphifyBlock + docsBlock)
    }

    private fun buildPromptInternal(intention: String, specsSection: String, extraContext: String = ""): String {
        val specsBlock = if (specsSection.isNotEmpty()) {
            """
            |
            |Existing specifications (use as context):
            |$specsSection
            """.trimMargin()
        } else ""

        val toolCatalog = ToolCatalog.toPromptSection()

        return """
            |You are a Planning Expert. Your role is to decompose a high-level intention
            |into a structured execution plan for a Gradle plugin project.
            |
            |Intention: $intention
            |
            |$specsBlock
            |$extraContext
            |$toolCatalog
            |Output a valid JSON object with this exact structure:
            |{
            |  "title": "<intention summary>",
            |  "epics": [
            |    {
            |      "name": "<EPIC-ID>",
            |      "description": "<epic description>",
            |      "points": <story points, integer>,
            |      "userStories": [
            |        {
            |          "description": "<user story description>",
            |          "tasks": [
            |            {
            |              "description": "<task description>",
            |              "gradleTask": "./gradlew <task>",
            |              "toolType": "GRADLE",
            |              "target": ""
            |            }
            |          ]
            |        }
            |      ]
            |    }
            |  ],
            |  "totalPoints": <sum of all epic points>,
            |  "estimatedSessions": "<range like '3-5'>"
            |}
            |
            |Rules:
            |- EPIC names use a short prefix derived from the intention (e.g., PLN, TEST, CAP) followed by a dash and index starting at 0
            |- Decompose logically: 1-4 EPICs, each with 1-4 user stories, each with 1-3 tasks
            |- gradleTask values must be realistic Gradle invocations like "./gradlew test", "./gradlew build"
            |- When `toolType` is omitted, the consumer applies the default GRADLE
            |- Use governance/RAG/document context to avoid redundant EPICs
            |- If an EPIC is already TERMINE, do NOT re-plan it — reference it as dependency
            |- Output ONLY the JSON object, no markdown fences, no explanations
            |- The JSON must be valid and parseable by Jackson
            """.trimMargin()
    }
}
