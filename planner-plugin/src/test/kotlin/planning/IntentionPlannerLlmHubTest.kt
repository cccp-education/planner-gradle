package planning

import contracts.agent.Plan
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
 * TDD — EPIC PLN-LLM-HUB US-2 : tests unitaires pour `IntentionPlanner.plan()`
 * avec injection du `ChatModel` (au lieu de construire `OllamaBridge.chatModel()` interne).
 *
 * Vérifie que le contrat d'appel est préservé (4 String + Logger → Plan)
 * tout en acceptant un ChatModel injectable pour testabilité.
 */
class IntentionPlannerLlmHubTest {

    private val context = PlanningContext(intention = "test llm hub")
    private val logger: Logger = Logging.getLogger("test")

    @Test
    fun `plan with single-canal accepts injected ChatModel and returns parsed Plan`() {
        val fakeModel = FakeChatModel(
            """{"title":"Test","epics":[{"name":"T-0","description":"desc","points":1,
            "userStories":[{"description":"us","tasks":[{"description":"task",
            "gradleTask":"./gradlew test","toolType":"GRADLE","target":"",
            "expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,"verifyHook":null}]}]}],
            "totalPoints":1,"estimatedSessions":"1"}"""
        )

        val plan = IntentionPlanner.plan(
            intention = "test",
            context = context,
            specContents = emptyList(),
            model = fakeModel,
            logger = logger
        )

        assertEquals("Test", plan.title)
        assertEquals(1, plan.totalPoints)
        assertTrue(fakeModel.invoked, "ChatModel.chat() should be called")
    }

    @Test
    fun `plan with multi-canal accepts injected ChatModel and preserves 4-string contract`() {
        val fakeModel = FakeChatModel(
            """{"title":"Multi","epics":[{"name":"M-0","description":"multi","points":2,
            "userStories":[{"description":"us","tasks":[{"description":"task",
            "gradleTask":"./gradlew build","toolType":"GRADLE","target":"",
            "expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,"verifyHook":null}]}]}],
            "totalPoints":2,"estimatedSessions":"2"}"""
        )

        val plan = IntentionPlanner.plan(
            intention = "test multi",
            context = context,
            specContents = emptyList(),
            eagerContext = "eager data",
            ragContext = "rag data",
            graphifyContext = "graph data",
            docsContext = "docs data",
            model = fakeModel,
            logger = logger
        )

        assertEquals("Multi", plan.title)
        assertEquals(2, plan.totalPoints)
        assertTrue(fakeModel.invoked, "ChatModel.chat() should be called")
        assertTrue(fakeModel.lastPrompt!!.contains("EAGER CONTEXT"))
        assertTrue(fakeModel.lastPrompt!!.contains("SEMANTIC CONTEXT"))
    }

    @Test
    fun `plan retries 3 times on parse failure then throws`() {
        val fakeModel = FakeChatModel("not valid json")

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            IntentionPlanner.plan(
                intention = "test retry",
                context = context,
                specContents = emptyList(),
                model = fakeModel,
                logger = logger
            )
        }

        assertEquals(3, fakeModel.invokeCount, "Should retry 3 times")
    }

    private class FakeChatModel(private val response: String) : ChatModel {
        var invoked = false
        var invokeCount = 0
        var lastPrompt: String? = null

        override fun doChat(request: ChatRequest): ChatResponse {
            invoked = true
            invokeCount++
            lastPrompt = request.messages().joinToString("\n") { it.text() ?: "" }
            return ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
        }

        private fun dev.langchain4j.data.message.ChatMessage.text(): String = when (this) {
            is dev.langchain4j.data.message.UserMessage ->
                if (hasSingleText()) singleText() else contents().joinToString { it.toString() }
            is dev.langchain4j.data.message.SystemMessage -> text()
            is AiMessage -> text()
            else -> toString()
        }
    }
}