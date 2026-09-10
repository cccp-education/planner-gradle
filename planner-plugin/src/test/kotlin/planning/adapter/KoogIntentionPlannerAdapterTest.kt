package planning.adapter

import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KoogIntentionPlannerAdapterTest {

    private fun sampleContext(): CompositeContext = CompositeContext(
        eagerSection = "EAGER rules",
        ragSection = "RAG docs",
        graphifySection = "GRAPH relations",
        docsSection = "DOCS manuals",
        config = CompositeContextConfig()
    )

    private fun planJson(title: String = "Test"): String = """
        {"title":"$title","epics":[{"name":"T-0","description":"desc","points":1,
        "userStories":[{"description":"us","tasks":[{"description":"task",
        "gradleTask":"./gradlew test","toolType":"GRADLE","target":"",
        "expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,"verifyHook":null}]}]}],
        "totalPoints":1,"estimatedSessions":"1"}
    """.trimIndent()

    @Test
    fun `adapter implements PlannerPort and returns a PlanState carrying the plan`() {
        val adapter = KoogIntentionPlannerAdapter(FakeChatModel(planJson()))
        val state = adapter.plan("fix the bug", sampleContext())
        assertEquals("fix the bug", state.intention)
        assertNotNull(state.plan, "adapter must produce a parsed Plan")
        assertEquals("Test", state.plan?.title)
        assertNull(state.error)
    }

    @Test
    fun `adapter forwards the four composite channels to the planner`() {
        var capturedPrompt: String? = null
        val model = RecordingChatModel { prompt -> capturedPrompt = prompt }
        val adapter = KoogIntentionPlannerAdapter(model)
        adapter.plan("intention x", sampleContext())
        val prompt = capturedPrompt.orEmpty()
        assertTrue(prompt.contains("EAGER rules"), "eager channel must reach the prompt")
        assertTrue(prompt.contains("RAG docs"), "rag channel must reach the prompt")
        assertTrue(prompt.contains("GRAPH relations"), "graphify channel must reach the prompt")
        assertTrue(prompt.contains("DOCS manuals"), "docs channel must reach the prompt")
    }

    @Test
    fun `adapter classification is complex for long intentions and simple otherwise`() {
        val adapter = KoogIntentionPlannerAdapter(FakeChatModel(planJson()))
        val short = adapter.plan("short", sampleContext())
        val long = adapter.plan("a".repeat(81), sampleContext())
        assertEquals("simple", short.classification)
        assertEquals("complexe", long.classification)
    }

    @Test
    fun `adapter preserves the composite context in the returned state`() {
        val adapter = KoogIntentionPlannerAdapter(FakeChatModel(planJson()))
        val context = sampleContext()
        val state = adapter.plan("fix", context)
        assertEquals(context, state.compositeContext)
    }

    @Test
    fun `planner failure is caught and returned as a failed PlanState — never throws`() {
        val adapter = KoogIntentionPlannerAdapter(FakeChatModel("not valid json"))
        val state = adapter.plan("fix the bug", sampleContext())
        assertNotNull(state.error, "parse failure must surface as PlanState.error")
        assertTrue(state.error.orEmpty().contains("IntentionPlanner failed"))
        assertNull(state.plan)
    }

    @Test
    fun `adapter keeps the failed intention and context on error state`() {
        val adapter = KoogIntentionPlannerAdapter(FakeChatModel("not valid json"))
        val state = adapter.plan("doomed intention", sampleContext())
        assertEquals("doomed intention", state.intention)
        assertEquals(sampleContext(), state.compositeContext)
    }
}

class FakeChatModel(private val response: String) : ChatModel {
    override fun doChat(request: ChatRequest): ChatResponse =
        ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
}

class RecordingChatModel(private val onChat: (String) -> Unit) : ChatModel {
    override fun doChat(request: ChatRequest): ChatResponse {
        onChat(request.messages().filterIsInstance<dev.langchain4j.data.message.UserMessage>()
            .firstOrNull()?.singleText().orEmpty())
        return ChatResponse.builder().aiMessage(AiMessage.from("{}")).build()
    }
}