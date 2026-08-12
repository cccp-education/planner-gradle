package planning.llm

import codebase.koog.llm.LlmProvider
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LlmProviderChatModelAdapterTest {

    @Test
    fun `chat String should delegate to LlmProvider and return text`() {
        val provider = FakeLlmProvider("Hello back")
        val adapter = LlmProviderChatModelAdapter(provider)

        val text = adapter.chat("Hello")

        assertThat(text).isEqualTo("Hello back")
    }

    @Test
    fun `chat messages should concatenate system then user text into single prompt`() {
        val captured = mutableListOf<String>()
        val provider = FakeLlmProvider { prompt ->
            captured.add(prompt)
            "ok"
        }
        val adapter = LlmProviderChatModelAdapter(provider)

        adapter.chat(
            SystemMessage.from("You are a Planning Expert."),
            UserMessage.from("Decompose this intention."),
        )

        assertThat(captured).hasSize(1)
        assertThat(captured.single()).contains("You are a Planning Expert.")
        assertThat(captured.single()).contains("Decompose this intention.")
        assertThat(captured.single()).contains("You are a Planning Expert.\nDecompose this intention.")
    }

    @Test
    fun `chat messages should preserve order and join all messages text`() {
        val captured = mutableListOf<String>()
        val provider = FakeLlmProvider { prompt ->
            captured.add(prompt)
            "ok"
        }
        val adapter = LlmProviderChatModelAdapter(provider)

        adapter.chat(
            SystemMessage.from("system part"),
            UserMessage.from("user part"),
        )

        assertThat(captured.single()).isEqualTo("system part\nuser part")
    }

    @Test
    fun `chat ChatRequest should delegate via doChat and return ChatResponse with AiMessage`() {
        val provider = FakeLlmProvider("plan content")
        val adapter = LlmProviderChatModelAdapter(provider)

        val request = ChatRequest.builder()
            .messages(SystemMessage.from("sys"), UserMessage.from("usr"))
            .build()
        val response = adapter.chat(request)

        assertThat(response.aiMessage().text()).isEqualTo("plan content")
    }

    @Test
    fun `provider exception should propagate as RuntimeException`() {
        val provider = FakeLlmProvider { throw IllegalStateException("boom") }
        val adapter = LlmProviderChatModelAdapter(provider)

        assertThrows<IllegalStateException> {
            adapter.chat("anything")
        }
    }

    private class FakeLlmProvider(private val responseOrThrow: (String) -> String) : LlmProvider {
        constructor(response: String) : this({ _ -> response })

        override suspend fun call(prompt: String): String = responseOrThrow(prompt)
    }
}