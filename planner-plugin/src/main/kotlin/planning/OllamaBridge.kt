package planning

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import java.time.Duration

object OllamaBridge {

    private const val DEFAULT_BASE_URL = "http://localhost:11434"
    private const val DEFAULT_MODEL_NAME = "qwen3.5:397b-cloud"

    fun chatModel(
        ollamaModel: String = DEFAULT_MODEL_NAME,
        ollamaBaseUrl: String = DEFAULT_BASE_URL
    ): ChatModel {
        val resolvedBaseUrl = System.getenv("OLLAMA_BASE_URL") ?: ollamaBaseUrl
        val resolvedModel = System.getenv("OLLAMA_MODEL") ?: ollamaModel
        return OllamaChatModel.builder()
            .baseUrl(resolvedBaseUrl)
            .modelName(resolvedModel)
            .timeout(Duration.ofMinutes(5))
            .build()
    }
}
