package planning.steps

import codebase.koog.llm.LlmProvider
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.logging.Logging
import planning.IntentionPlanner
import planning.PlanningContext
import planning.llm.LlmProviderChatModelAdapter
import planning.llm.PlanningLlmService.aiProvider
import planning.llm.PlanningLlmService.registerLlmBuildService
import planning.llm.PlanningLlmService.resolveModel
import planning.steps.PlannerScenarioState.freshProject
import planning.steps.PlannerScenarioState.hubAiProvider
import planning.steps.PlannerScenarioState.hubFakeInvokeCount
import planning.steps.PlannerScenarioState.hubFakeModel
import planning.steps.PlannerScenarioState.hubProject
import planning.steps.PlannerScenarioState.hubResolvedModel
import planning.steps.PlannerScenarioState.hubServiceProvider
import planning.steps.PlannerScenarioState.parsedPlan

/**
 * Cucumber step definitions for the planner LLM-hub feature
 * (PLN-LLM-HUB-3 — `planner_llm_hub.feature`).
 *
 * Pure BDD — no production code is modified by this baby-step. The LLM-hub
 * domain layer (`PlanningLlmService.registerLlmBuildService` +
 * `PlanningLlmService.resolveModel` + `LlmProviderChatModelAdapter` +
 * `IntentionPlanner.plan` with injected `ChatModel`) is exercised with
 * in-memory fixtures (ProjectBuilder + a FakeChatModel), mirroring the
 * unit tests of PLN-LLM-HUB-1/2 (S-072/073) and the BDD pattern of
 * `PlannerVerifySteps` (PLN-VERIFY-4, S-067) / `PlannerBudgetSteps`
 * (PLN-BUDGET-3, S-071).
 *
 * Scenarios cover the service registration (non-null provider +
 * resolvable LlmProvider), the `ai.provider` property defaulting/
 * reading/trimming/lowercasing, the `resolveModel` routing (pool via
 * LlmBuildService when no mock URL, OllamaChatModel when `-Pollama.baseUrl`
 * is set), and the `IntentionPlanner.plan` contract accepting an injected
 * `ChatModel` (backward-compatible 4-string + Logger call signature).
 *
 * Shared `Given`/`When`/`Then` steps are delegated to
 * [PlannerVibecodingSteps] — both step classes read and write through the
 * shared [PlannerScenarioState] "World" object.
 */
class PlannerLlmHubSteps : En {

    init {

        // -------------------------------------------------------------------------
        // Given — fresh test project + properties
        // -------------------------------------------------------------------------

        Given("a fresh test project") {
            hubProject = freshProject()
        }

        Given("a fresh test project without any ai-provider property") {
            hubProject = freshProject()
        }

        Given("a fresh test project with the ai-provider property set to {string}") { value: String ->
            hubProject = freshProject(mapOf("ai.provider" to value))
        }

        Given("a fresh test project with no mock Ollama URL") {
            hubProject = freshProject()
        }

        Given("a fresh test project with the ollama.baseUrl property set to {string}") { url: String ->
            hubProject = freshProject(mapOf("ollama.baseUrl" to url))
        }

        Given("a fake ChatModel that returns a valid plan JSON") {
            val json = """
                {"title":"LLM Hub Plan","epics":[{"name":"H-0","description":"hub epic",
                "points":1,"userStories":[{"description":"us","tasks":[
                {"description":"task","gradleTask":"./gradlew test","toolType":"GRADLE",
                "target":"","expectedOutput":"BUILD SUCCESSFUL","maxRetries":3,
                "verifyHook":null}]}]}],"totalPoints":1,"estimatedSessions":"1"}
            """.trimIndent()
            hubFakeModel = FakeChatModel(json)
            hubFakeInvokeCount = 0
        }

        // -------------------------------------------------------------------------
        // When — service registration + model resolution + plan
        // -------------------------------------------------------------------------

        When("the LLM build service is registered") {
            val p = requireNotNull(hubProject)
            hubServiceProvider = p.registerLlmBuildService()
        }

        When("the aiProvider property is read") {
            val p = requireNotNull(hubProject)
            hubAiProvider = p.aiProvider
        }

        When("resolveModel is called for the {string} provider") { provider: String ->
            val p = requireNotNull(hubProject)
            val sp = hubServiceProvider ?: p.registerLlmBuildService().also { hubServiceProvider = it }
            hubResolvedModel = p.resolveModel(provider, sp)
        }

        When("the planner decomposes the intention using the injected model") {
            val fake = requireNotNull(hubFakeModel)
            val logger = Logging.getLogger("planner-llm-hub-cucumber")
            parsedPlan = IntentionPlanner.plan(
                intention = "test llm hub integration",
                context = PlanningContext(intention = "test llm hub integration"),
                specContents = emptyList(),
                model = fake,
                logger = logger
            )
            hubFakeInvokeCount = fake.invokeCount
        }

        // -------------------------------------------------------------------------
        // Then — assertions
        // -------------------------------------------------------------------------

        Then("the service provider should be non-null") {
            assertThat(hubServiceProvider).isNotNull
        }

        Then("the service should expose a resolvable LlmProvider") {
            val sp = requireNotNull(hubServiceProvider)
            assertThat(sp.get().provider()).isNotNull
        }

        Then("it should equal {string}") { expected: String ->
            assertThat(hubAiProvider).isEqualTo(expected)
        }

        Then("the resolved model should be a ChatModel") {
            assertThat(hubResolvedModel).isInstanceOf(ChatModel::class.java)
        }

        Then("the model should wrap a codebase LlmProvider") {
            assertThat(hubResolvedModel).isInstanceOf(LlmProviderChatModelAdapter::class.java)
        }

        Then("the model should not wrap a codebase LlmProvider") {
            assertThat(hubResolvedModel).isNotInstanceOf(LlmProviderChatModelAdapter::class.java)
        }

        Then("the plan title should be {string}") { title: String ->
            assertThat(parsedPlan?.title).isEqualTo(title)
        }

        Then("the plan should contain {int} EPIC") { count: Int ->
            assertThat(parsedPlan?.epics).hasSize(count)
        }

        Then("the fake model should have been invoked once") {
            assertThat(hubFakeInvokeCount).isEqualTo(1)
        }
    }
}