package planning

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import java.time.Duration

object OllamaBridge {

    private const val DEFAULT_BASE_URL = "http://localhost:11434"
    private const val DEFAULT_MODEL_NAME = "qwen3.5:397b-cloud"

    private val baseUrl: String
        get() = System.getenv("OLLAMA_BASE_URL") ?: DEFAULT_BASE_URL

    private val modelName: String
        get() = System.getenv("OLLAMA_MODEL") ?: DEFAULT_MODEL_NAME

    fun chatModel(): ChatModel = OllamaChatModel.builder()
        .baseUrl(baseUrl)
        .modelName(modelName)
        .timeout(Duration.ofMinutes(5))
        .build()
}
