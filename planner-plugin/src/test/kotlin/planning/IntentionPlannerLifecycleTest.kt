package planning

import contracts.agent.AgentPhase
import contracts.agent.AgentState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD — EPIC PLN-LIFECYCLE US-2 : unit tests for the lifecycle-aware surcharge
 * `IntentionPlanner.plan(state: AgentState.ContextReady, ...): AgentState.Planned`.
 *
 * The surcharge is additive — the existing 4-String + Logger contract is preserved.
 * It delegates to the multi-canal `plan()` to get a `Plan`, then wraps it via
 * `LifecycleAdapter.toPlanned` into an `AgentState.Planned` (N0).
 */
class IntentionPlannerLifecycleTest {

    private val logger: Logger = Logging.getLogger("test")

    @Test
    fun `plan with ContextReady returns Planned with epics parsed from LLM`() {
        val fakeModel = FakeChatModel(
            """{"title":"Lifecycle plan","epics":[{"name":"LF-0","description":"desc","points":3,
            "userStories":[{"description":"us","tasks":[{"description":"task",
            "gradleTask":"./gradlew build","toolType":"GRADLE","target":"",
            "expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,"verifyHook":null}]}]}],
            "totalPoints":3,"estimatedSessions":"2"}"""
        )
        val state = AgentState.ContextReady(
            intention = "planifier formation afnor",
            compositeContext = "eager + rag + graphify merged",
            afnorCorpus = "afnor corpus content"
        )

        val planned = IntentionPlanner.plan(
            state = state,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertEquals(1, planned.epics.size)
        assertEquals("LF-0", planned.epics.first().name)
    }

    @Test
    fun `plan with ContextReady preserves intention in Planned`() {
        val fakeModel = FakeChatModel(validJson())
        val state = AgentState.ContextReady(
            intention = "intention lifecycle",
            compositeContext = "composite",
            afnorCorpus = "afnor"
        )

        val planned = IntentionPlanner.plan(
            state = state,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertEquals("intention lifecycle", planned.intention)
    }

    @Test
    fun `plan with ContextReady preserves compositeContext and afnorCorpus in Planned`() {
        val fakeModel = FakeChatModel(validJson())
        val state = AgentState.ContextReady(
            intention = "test preserve",
            compositeContext = "merged composite context",
            afnorCorpus = "afnor reac corpus"
        )

        val planned = IntentionPlanner.plan(
            state = state,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertEquals("merged composite context", planned.compositeContext)
        assertEquals("afnor reac corpus", planned.afnorCorpus)
    }

    @Test
    fun `plan with ContextReady sets phase to EXECUTE`() {
        val fakeModel = FakeChatModel(validJson())
        val state = AgentState.ContextReady(
            intention = "test phase",
            compositeContext = "",
            afnorCorpus = ""
        )

        val planned = IntentionPlanner.plan(
            state = state,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertEquals(AgentPhase.EXECUTE, planned.phase)
    }

    @Test
    fun `plan with ContextReady invokes ChatModel and prompt contains compositeContext`() {
        val fakeModel = FakeChatModel(validJson())
        val state = AgentState.ContextReady(
            intention = "test prompt delegation",
            compositeContext = "delegated composite block",
            afnorCorpus = "afnor docs"
        )

        IntentionPlanner.plan(
            state = state,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertTrue(fakeModel.invoked, "ChatModel.chat() should be called")
        assertTrue(
            fakeModel.lastPrompt!!.contains("delegated composite block"),
            "Prompt should contain the compositeContext from ContextReady"
        )
    }

    private fun validJson(): String =
        """{"title":"T","epics":[{"name":"E-0","description":"d","points":1,
        "userStories":[{"description":"us","tasks":[{"description":"task",
        "gradleTask":"./gradlew test","toolType":"GRADLE","target":"",
        "expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,"verifyHook":null}]}]}],
        "totalPoints":1,"estimatedSessions":"1"}"""

    private class FakeChatModel(private val response: String) : ChatModel {
        var invoked = false
        var lastPrompt: String? = null

        override fun doChat(request: ChatRequest): ChatResponse {
            invoked = true
            lastPrompt = request.messages().joinToString("\n") { it.text() ?: "" }
            return ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
        }

        private fun dev.langchain4j.data.message.ChatMessage.text(): String = when (this) {
            is UserMessage ->
                if (hasSingleText()) singleText() else contents().joinToString { it.toString() }
            is AiMessage -> text()
            else -> toString()
        }
    }
}