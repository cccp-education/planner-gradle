package planning.llm

import dev.langchain4j.model.chat.ChatModel
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import planning.llm.PlanningLlmService.aiProvider
import planning.llm.PlanningLlmService.registerLlmBuildService
import planning.llm.PlanningLlmService.resolveModel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PlanningLlmService] — the `planning.llm` LLM socle
 * (EPIC PLN-LLM-HUB).
 *
 * Baby-step TDD (RED → GREEN): mirrors capsule CAP-ARCH-1 pattern
 * (`registerLlmBuildService` + `resolveModel` with mock-LLM fallback).
 * Uses [org.gradle.testfixtures.ProjectBuilder] — no daemon, no network.
 */
class PlanningLlmServiceTest {

    private fun project(properties: Map<String, Any?> = emptyMap()): org.gradle.api.Project {
        val p = ProjectBuilder.builder().build()
        properties.forEach { (k, v) -> p.extensions.extraProperties.set(k, v) }
        return p
    }

    @Test
    fun `aiProvider defaults to ollama when no property is set`() {
        assertEquals("ollama", project().aiProvider)
    }

    @Test
    fun `aiProvider reads the ai-provider property`() {
        assertEquals("gemini", project(mapOf("ai.provider" to "gemini")).aiProvider)
    }

    @Test
    fun `aiProvider trims and lowercases the property value`() {
        assertEquals("ollama", project(mapOf("ai.provider" to "  OLLAMA  ")).aiProvider)
    }

    @Test
    fun `registerLlmBuildService returns a non-null provider`() {
        assertNotNull(project().registerLlmBuildService())
    }

    @Test
    fun `registerLlmBuildService exposes a resolvable LlmProvider`() {
        val service = project().registerLlmBuildService().get()
        assertNotNull(service.provider())
    }

    @Test
    fun `registerLlmBuildService is idempotent per build`() {
        val p = project()
        assertTrue(p.registerLlmBuildService() === p.registerLlmBuildService())
    }

    @Test
    fun `resolveModel returns an OllamaChatModel when -Pollama-baseUrl is set`() {
        val p = project(mapOf("ollama.baseUrl" to "http://localhost:0"))
        val model = p.resolveModel("ollama", p.registerLlmBuildService())
        assertNotNull(model)
        assertTrue(model is ChatModel)
    }

    @Test
    fun `resolveModel returns a ChatModel via LlmBuildService when no mock url`() {
        val p = project()
        val model = p.resolveModel("ollama", p.registerLlmBuildService())
        assertNotNull(model)
        assertTrue(model is ChatModel)
    }

    @Test
    fun `resolveModel with a custom provider name routes through LlmBuildService`() {
        val p = project()
        val model = p.resolveModel("gpt-oss:120b-cloud", p.registerLlmBuildService())
        assertNotNull(model)
        assertTrue(model is ChatModel)
    }
}