package planning.steps

import codebase.koog.llm.LlmProvider
import codebase.koog.llm.adapter.LlmProviderChatModelAdapter
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import planning.llm.PlanningLlmService.registerLlmBuildService
import planning.llm.PlanningLlmService.resolveModel
import planning.steps.PlannerScenarioState.freshProject
import planning.steps.PlannerScenarioState.hubResolvedModel
import planning.steps.PlannerScenarioState.hubServiceProvider
import java.io.File

/**
 * Cucumber steps for the LLM-ADAPTER-EXTRACT EPIC (US-5).
 *
 * Validates that planner consumes the N1 adapter from codebase
 * (codebase.koog.llm.adapter.LlmProviderChatModelAdapter) and that the
 * local planning.llm.LlmProviderChatModelAdapter copy has been removed.
 *
 * All steps use the "adapter extract" prefix to avoid glue collisions
 * with PlannerLlmHubSteps (pattern S-088).
 */
class PlannerAdapterExtractSteps : En {

    init {
        Given("the planner source tree is scanned for LlmProviderChatModelAdapter") {
            // No-op — the file existence check happens in the Then step
        }

        Then("the codebase.koog.llm.adapter.LlmProviderChatModelAdapter import should be present") {
            val sourceRoot = File("src/main/kotlin")
            val serviceFile = sourceRoot
                .resolve("planning")
                .resolve("llm")
                .resolve("PlanningLlmService.kt")
            assertThat(serviceFile.exists()).isTrue()
            assertThat(serviceFile.readText())
                .contains("import codebase.koog.llm.adapter.LlmProviderChatModelAdapter")
        }

        And("the planning.llm package should not declare a LlmProviderChatModelAdapter class") {
            val sourceRoot = File("src/main/kotlin")
            val planningLlmDir = sourceRoot.resolve("planning").resolve("llm")
            if (planningLlmDir.exists()) {
                val adapterFiles = planningLlmDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { it.readText().contains("class LlmProviderChatModelAdapter") }
                    .toList()
                assertThat(adapterFiles).isEmpty()
            }
        }

        Given("adapter extract registers the LLM hub on a fresh project") {
            val project = freshProject()
            hubServiceProvider = project.registerLlmBuildService()
        }

        When("adapter extract resolves a model without a mock URL") {
            val project = freshProject()
            hubResolvedModel = project.resolveModel("ollama", hubServiceProvider!!)
        }

        When("adapter extract resolves a model with a mock URL") {
            val project = freshProject(
                mapOf(
                    "ollama.baseUrl" to "http://localhost:1",
                    "ollama.modelName" to "smollm:135m",
                )
            )
            hubResolvedModel = project.resolveModel("ollama", hubServiceProvider!!)
        }

        Then("adapter extract should get a ChatModel instance") {
            assertThat(hubResolvedModel).isInstanceOf(ChatModel::class.java)
        }

        Then("adapter extract should get a codebase LlmProviderChatModelAdapter instance") {
            assertThat(hubResolvedModel).isInstanceOf(LlmProviderChatModelAdapter::class.java)
        }

        Then("adapter extract should not get a codebase LlmProviderChatModelAdapter instance") {
            assertThat(hubResolvedModel).isNotInstanceOf(LlmProviderChatModelAdapter::class.java)
        }

        Then("adapter extract should still get a ChatModel instance") {
            assertThat(hubResolvedModel).isInstanceOf(ChatModel::class.java)
        }

        Given("a fake codebase LlmProvider that returns a canned plan JSON") {
            PlannerScenarioState.hubFakeModel = null
            PlannerScenarioState.hubFakeInvokeCount = 0
        }

        When("the planner calls the adapter with a decompose intention prompt") {
            val provider = LlmProvider { _ ->
                PlannerScenarioState.hubFakeInvokeCount++
                """{"title":"Mock Plan","epics":[],"totalPoints":0,"estimatedSessions":0}"""
            }
            val adapter = LlmProviderChatModelAdapter(provider)
            val request = ChatRequest.builder()
                .messages(UserMessage.from("Decompose intention: build a planner plugin"))
                .build()
            val response = adapter.doChat(request)
            PlannerScenarioState.rawJson = response.aiMessage().text()
        }

        Then("the fake provider should have been invoked once") {
            assertThat(PlannerScenarioState.hubFakeInvokeCount).isEqualTo(1)
        }

        Then("the adapter response should wrap the canned plan JSON") {
            assertThat(PlannerScenarioState.rawJson).contains("Mock Plan")
        }
    }
}