package planning

import codebase.koog.llm.service.LlmBuildService
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.services.ServiceReference
import org.gradle.work.DisableCachingByDefault
import planning.budget.BudgetWiring
import planning.llm.PlanningLlmService.aiProvider
import planning.llm.PlanningLlmService.resolveModel

/**
 * EPIC 3 — Multi-Canal Convergent : task câblée pour recevoir les 4 canaux de contexte
 * (EAGER, RAG, Graphify, Docs) en plus du SpecReader classique.
 * Compatible ascendante : si les canaux sont absents, fallback sur IntentionPlanner 4-param.
 *
 * EPIC PLN-LLM-HUB — le ChatModel est résolu via [LlmBuildService] (codebase N1
 * hub) injecté par Gradle DI, au lieu du legacy [OllamaBridge] standalone.
 */
@DisableCachingByDefault(because = "LLM output is probabilistic — never cache")
abstract class DecomposeIntentionTask : DefaultTask() {

    @get:Input
    abstract val intention: Property<String>

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val specsDir: DirectoryProperty

    @get:Input
    abstract val aiProvider: Property<String>

    @get:ServiceReference
    abstract val llmService: Property<LlmBuildService>

    // ── EPIC 3 : canaux multi-canal (optionnels pour compatibilité ascendante) ──

    @get:Optional
    @get:Input
    abstract val eagerContext: Property<String>

    @get:Optional
    @get:Input
    abstract val ragContext: Property<String>

    @get:Optional
    @get:Input
    abstract val graphifyContext: Property<String>

    @get:Optional
    @get:Input
    abstract val docsContext: Property<String>

    @TaskAction
    fun decompose() {
        val intent = intention.get()
        val specContents = if (specsDir.isPresent) {
            SpecReader.read(specsDir.get().asFile.toPath())
        } else {
            emptyList()
        }
        val context = PlanningContext(intention = intent)

        val eagerCtx = eagerContext.orNull ?: ""
        val ragCtx = ragContext.orNull ?: ""
        val graphifyCtx = graphifyContext.orNull ?: ""
        val docsCtx = docsContext.orNull ?: ""

        val hasMultiChannel = eagerCtx.isNotBlank() || ragCtx.isNotBlank()
            || graphifyCtx.isNotBlank() || docsCtx.isNotBlank()

        val budgeted = BudgetWiring.resolveBudgetedContexts(
            intention = intent,
            eagerCtx = eagerCtx,
            ragCtx = ragCtx,
            graphifyCtx = graphifyCtx,
            docsCtx = docsCtx,
            log = { msg -> logger.lifecycle(msg) }
        )

        val provider = aiProvider.get()
        val serviceProvider: Provider<LlmBuildService> = llmService
        val model = project.resolveModel(provider, serviceProvider)

        val plan = if (hasMultiChannel) {
            IntentionPlanner.plan(
                intention = intent,
                context = context,
                specContents = specContents,
                eagerContext = budgeted.eager,
                ragContext = budgeted.rag,
                graphifyContext = budgeted.graphify,
                docsContext = budgeted.docs,
                model = model,
                logger = logger
            )
        } else {
            IntentionPlanner.plan(
                intention = intent,
                context = context,
                specContents = specContents,
                model = model,
                logger = logger
            )
        }

        val output = StdoutFormatter.format(plan)
        println(output)
    }
}