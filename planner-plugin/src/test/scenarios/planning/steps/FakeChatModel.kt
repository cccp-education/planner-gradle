package planning.steps

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse

/**
 * Fake [ChatModel] used by `planner_llm_hub.feature` Cucumber scenarios to
 * exercise `IntentionPlanner.plan` without a network LLM. Returns a fixed
 * JSON string and records the number of invocations.
 */
class FakeChatModel(private val response: String) : ChatModel {

    var invokeCount: Int = 0
        private set

    override fun doChat(request: ChatRequest): ChatResponse {
        invokeCount++
        return ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
    }

    private fun ChatMessage.text(): String = when (this) {
        is UserMessage -> if (hasSingleText()) singleText() else contents().joinToString { it.toString() }
        is AiMessage -> text()
        else -> toString()
    }
}