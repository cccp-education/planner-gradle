package planning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import contracts.agent.AgentState
import contracts.agent.Plan
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import org.gradle.api.logging.Logger
import org.slf4j.LoggerFactory as Slf4jLoggerFactory
import planning.lifecycle.LifecycleAdapter

object IntentionPlanner {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private const val MAX_ATTEMPTS = 3

    fun plan(
        intention: String,
        context: PlanningContext,
        specContents: List<SpecReader.SpecContent>,
        model: ChatModel,
        logger: Logger
    ): Plan {
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
        model: ChatModel,
        logger: Logger
    ): Plan {
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
        model: ChatModel
    ): Plan {
        val log = Slf4jLoggerFactory.getLogger(IntentionPlanner::class.java)
        val prompt = buildPrompt(intention, context, specContents, eagerContext, ragContext, graphifyContext, docsContext)
        return callLlm(model, prompt) { msg -> log.info(msg) }
    }

    /**
     * EPIC PLN-LIFECYCLE US-2 — lifecycle-aware surcharge (additive).
     *
     * Consumes an `AgentState.ContextReady` (N0) and returns an `AgentState.Planned` (N0).
     * The compositeContext from the state is forwarded as the `eagerContext` channel so the
     * existing multi-canal prompt path is reused. The resulting `Plan` is wrapped via
     * `LifecycleAdapter.toPlanned`, preserving `intention`, `compositeContext`, `afnorCorpus`,
     * and `epics`. If the adapter returns `null` (factory failure), a fallback `AgentState.Planned`
     * is built directly so the planner never throws on the lifecycle path.
     *
     * Backward compat: the 4-String surcharges above are unchanged. This API is opt-in.
     */
    fun plan(
        state: AgentState.ContextReady,
        specContents: List<SpecReader.SpecContent>,
        model: ChatModel,
        logger: Logger
    ): AgentState.Planned {
        val context = PlanningContext(intention = state.intention)
        val plan = plan(
            intention = state.intention,
            context = context,
            specContents = specContents,
            eagerContext = state.compositeContext,
            ragContext = "",
            graphifyContext = "",
            docsContext = state.afnorCorpus,
            model = model,
            logger = logger
        )
        val planned = LifecycleAdapter.toPlanned(
            plan = plan,
            intention = state.intention,
            compositeContext = state.compositeContext,
            afnorCorpus = state.afnorCorpus
        )
        return planned ?: AgentState.Planned(
            intention = state.intention,
            compositeContext = state.compositeContext,
            afnorCorpus = state.afnorCorpus,
            epics = plan.epics
        )
    }

    private fun callLlm(model: ChatModel, prompt: String, log: (String) -> Unit): Plan {
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
            |              "target": "",
            |              "expectedOutput": "<expected success signal>",
            |              "maxRetries": 3,
            |              "verifyHook": null
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
            |- `expectedOutput` is REQUIRED: the concrete success signal the task must produce (e.g. "BUILD SUCCESSFUL", "SPG generated", "Tests passed"). Defaults to "BUILD SUCCESSFUL" when omitted
            |- `maxRetries` is optional, defaults to 3 (range 1..10). Set higher for flaky/network tasks
            |- `verifyHook` is optional and rare: a shell script path run after success to validate artifacts (e.g. "scripts/check-artifacts.sh"). Omit when no post-verify is needed
            |- Use governance/RAG/document context to avoid redundant EPICs
            |- If an EPIC is already TERMINE, do NOT re-plan it — reference it as dependency
            |- Output ONLY the JSON object, no markdown fences, no explanations
            |- The JSON must be valid and parseable by Jackson
            """.trimMargin()
    }
}
