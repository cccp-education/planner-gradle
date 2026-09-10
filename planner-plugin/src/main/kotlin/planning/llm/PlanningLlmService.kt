@file:Suppress("MemberVisibilityCanBePrivate")

package planning.llm

import codebase.koog.llm.adapter.LlmProviderChatModelAdapter
import codebase.koog.llm.service.LlmBuildService
import dev.langchain4j.model.chat.ChatModel
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceSpec
import planning.adapter.KoogIntentionPlannerAdapter
import planning.adapter.PlannerPortBuildService
import planning.adapter.PlannerPortRegistry
import java.time.Duration

/**
 * Central LLM socle for the Planner Gradle plugin.
 *
 * EPIC PLN-LLM-HUB (decision S-072): planner consumes codebase (N1) as the
 * unified LLM socle, mirroring the slider SLD-8 / capsule CAP-ARCH-1 pattern.
 * Provider resolution delegates to [LlmBuildService] (Gradle-managed DI)
 * via [LlmProviderChatModelAdapter].
 *
 * Responsibilities:
 * - [registerLlmBuildService] — [LlmBuildService] registration (Gradle DI)
 * - [resolveModel] — bridges codebase's [codebase.koog.llm.LlmProvider] to
 *   langchain4j [ChatModel]
 * - [aiProvider] — reads `-Pai.provider`, defaulting to `"ollama"`
 *
 * ## Mock-LLM fallback (test compat)
 *
 * When `-Pollama.baseUrl` is set (typically to a test mock HTTP server),
 * resolution returns a langchain4j `OllamaChatModel` pointed at that URL —
 * the GradleTestKit Cucumber scenarios inject a mock via `-Pollama.baseUrl`,
 * no pool required.
 *
 * In production (no `-Pollama.baseUrl`), the codebase [LlmBuildService] is
 * used: [codebase.koog.llm.service.LlmServiceResolver] resolves the provider
 * from the pool, wrapped in a [LlmProviderChatModelAdapter].
 */
object PlanningLlmService {

    const val PROP_AI_PROVIDER = "ai.provider"
    const val PROVIDER_OLLAMA = "ollama"

    /** Reads `-Pai.provider`, defaulting to `"ollama"` when absent or blank. */
    val Project.aiProvider: String
        get() = (findProperty(PROP_AI_PROVIDER) as? String
            ?: PROVIDER_OLLAMA).lowercase().trim()

    /**
     * Registers the [LlmBuildService] (Gradle-managed DI) and returns its
     * [Provider]. The service exposes a codebase [codebase.koog.llm.LlmProvider]
     * resolved by [codebase.koog.llm.service.LlmServiceResolver].
     *
     * Call once per build; inject the returned [Provider] into tasks via
     * `@ServiceReference`.
     */
    fun Project.registerLlmBuildService(): Provider<LlmBuildService> =
        gradle.sharedServices.registerIfAbsent(
            "plannerLlmService", LlmBuildService::class.java
        ) { spec: BuildServiceSpec<LlmBuildService.Params> ->
            spec.parameters.model.convention(project.aiProvider)
            spec.maxParallelUsages.set(1)
        }

    /**
     * Registers the N1 [codebase.koog.plannerport.PlannerPort] for this build
     * (EPIC SVO-3, D5): the [KoogIntentionPlannerAdapter] is constructed
     * over the LLM resolved by [registerLlmBuildService] and registered in
     * [planning.adapter.PlannerPortRegistry]; the shared-service
     * [planning.adapter.PlannerPortBuildService] (name
     * [planning.adapter.PlannerPortBuildService.SERVICE_NAME]) exposes it to
     * any consumer borough (codebase resolves by that canonical name —
     * zero `planning.*` import on the N1 side).
     *
     * Call once per build, after [registerLlmBuildService].
     */
    fun Project.registerPlannerPort(): Provider<PlannerPortBuildService> =
        gradle.sharedServices.registerIfAbsent(
            PlannerPortBuildService.SERVICE_NAME, PlannerPortBuildService::class.java
        ) { spec: BuildServiceSpec<PlannerPortBuildService.EmptyParams> ->
            spec.parameters // marker only — the port lives in PlannerPortRegistry
        }.also { _ ->
            val model = resolveModel(
                provider = aiProvider,
                serviceProvider = registerLlmBuildService(),
            )
            PlannerPortRegistry.register(KoogIntentionPlannerAdapter(model))
        }

    /**
     * Resolves the langchain4j [ChatModel] for the given [provider] by
     * delegating to the codebase [LlmBuildService] and wrapping the returned
     * [codebase.koog.llm.LlmProvider] in a [LlmProviderChatModelAdapter].
     *
     * ## Mock-LLM fallback (test compat)
     *
     * When `-Pollama.baseUrl` is set (typically to a test mock HTTP server),
     * resolution returns an `OllamaChatModel` honoring that property. This
     * keeps the GradleTestKit Cucumber scenarios working without an LLM pool.
     *
     * In production (no `-Pollama.baseUrl`), the codebase [LlmBuildService]
     * is used: the provider is resolved from the pool and wrapped in a
     * [LlmProviderChatModelAdapter].
     */
    fun Project.resolveModel(
        provider: String,
        serviceProvider: Provider<LlmBuildService>,
    ): ChatModel {
        val mockOllamaUrl = findProperty("ollama.baseUrl") as? String
        if (provider == PROVIDER_OLLAMA && mockOllamaUrl != null) {
            return createOllamaChatModel(mockOllamaUrl)
        }
        val llmProvider = serviceProvider.get().provider()
        return LlmProviderChatModelAdapter(llmProvider)
    }

    /**
     * Builds a langchain4j `OllamaChatModel` honoring the `ollama.*` Gradle
     * properties (`baseUrl`, `modelName`, `temperature`, `timeout`). Used by
     * the mock-LLM fallback in [resolveModel].
     */
    fun Project.createOllamaChatModel(baseUrl: String): ChatModel =
        dev.langchain4j.model.ollama.OllamaChatModel.builder().apply {
            this.baseUrl(baseUrl)
            modelName(findProperty("ollama.modelName")?.toString() ?: "gemma4:31b-cloud")
            temperature(findProperty("ollama.temperature")?.toString()?.toDoubleOrNull() ?: 0.8)
            timeout(Duration.ofSeconds(findProperty("ollama.timeout")?.toString()?.toLongOrNull() ?: 300L))
            logRequests(true)
            logResponses(true)
        }.build()
}