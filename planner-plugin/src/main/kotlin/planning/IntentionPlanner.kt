package planning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.langchain4j.data.message.UserMessage
import org.gradle.api.logging.Logger

object IntentionPlanner {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private const val MAX_ATTEMPTS = 3

    fun plan(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        logger: Logger,
        ollamaModel: String = "deepseek-v4-pro:cloud",
        ollamaBaseUrl: String = "http://localhost:11434"
    ): Plan {
        val model = OllamaBridge.chatModel(ollamaModel = ollamaModel, ollamaBaseUrl = ollamaBaseUrl)
        val prompt = buildPrompt(intention, context, specContents)

        var lastError: Exception? = null

        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val response = model.chat(UserMessage.from(prompt))
                val raw = response.aiMessage().text()
                logger.lifecycle("[LLM] Attempt $attempt/$MAX_ATTEMPTS — ${raw.length} chars received")

                val parsed = mapper.readValue<LLMResponse>(raw)
                logger.lifecycle("[LLM] Parsed: ${parsed.epics.size} EPICs, ${parsed.totalPoints} points")
                return parsed.toPlan()
            } catch (e: Exception) {
                lastError = e
                logger.error("[ERROR] Parsing attempt $attempt failed: ${e.message}")
                if (attempt < MAX_ATTEMPTS) {
                    logger.warn("[RETRY] Will retry — attempt ${attempt + 1}/$MAX_ATTEMPTS")
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
        val specsBlock = if (specsSection.isNotEmpty()) {
            """
            |
            |Existing specifications (use as context):
            |$specsSection
            """.trimMargin()
        } else ""

        return """
            |You are a Planning Expert. Your role is to decompose a high-level intention
            |into a structured execution plan for a Gradle plugin project.
            |
            |Intention: $intention
            |
            |$specsBlock
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
            |              "gradleTask": "./gradlew <task>"
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
            |- Output ONLY the JSON object, no markdown fences, no explanations
            |- The JSON must be valid and parseable by Jackson
            """.trimMargin()
    }
}
