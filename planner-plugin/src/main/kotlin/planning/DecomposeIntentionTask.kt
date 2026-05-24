package planning

import education.cccp.contracts.context.ChannelBudget
import education.cccp.contracts.context.CompositeContext
import education.cccp.contracts.context.CompositeContextConfig
import education.cccp.contracts.context.ContextChannel
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * EPIC 3 — Multi-Canal Convergent : task câblée pour recevoir les 4 canaux de contexte
 * (EAGER, RAG, Graphify, Docs) en plus du SpecReader classique.
 * Compatible ascendante : si les canaux sont absents, fallback sur IntentionPlanner 4-param.
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
    abstract val ollamaModel: Property<String>

    @get:Input
    abstract val ollamaBaseUrl: Property<String>

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

        // EPIC 3 : si au moins un canal multi-canal est fourni, utiliser l'overload 8-param
        val hasMultiChannel = eagerCtx.isNotBlank() || ragCtx.isNotBlank()
            || graphifyCtx.isNotBlank() || docsCtx.isNotBlank()

        val plan = if (hasMultiChannel) {
            IntentionPlanner.plan(
                intention = intent,
                context = context,
                specContents = specContents,
                eagerContext = eagerCtx,
                ragContext = ragCtx,
                graphifyContext = graphifyCtx,
                docsContext = docsCtx,
                logger = logger,
                ollamaModel = ollamaModel.get(),
                ollamaBaseUrl = ollamaBaseUrl.get()
            )
        } else {
            IntentionPlanner.plan(
                intention = intent,
                context = context,
                specContents = specContents,
                logger = logger,
                ollamaModel = ollamaModel.get(),
                ollamaBaseUrl = ollamaBaseUrl.get()
            )
        }

        // ── EPIC 3 : écriture typed CompositeContext (optionnel) ──
        if (hasMultiChannel) {
            try {
                val config = CompositeContextConfig(
                    totalTokenBudget = 8000,
                    budgetEagerLazy = 0.40,
                    budgetRag = 0.30,
                    budgetGraphify = 0.20,
                    budgetDocs = 0.10,
                    budgetOverhead = 0.0
                )
                val budget = ChannelBudget.fromConfig(config)
                val channels = budget.applyBudget(
                    listOf(
                        ContextChannel.Eager(eagerCtx),
                        ContextChannel.Rag(ragCtx),
                        ContextChannel.Graphify(graphifyCtx),
                        ContextChannel.Docs(docsCtx),
                        ContextChannel.Resource("")
                    )
                )
                val typedCtx = CompositeContext(
                    eagerSection = channels[0].content,
                    ragSection = channels[1].content,
                    graphifySection = channels[2].content,
                    docsSection = channels[3].content,
                    config = config
                )
                logger.lifecycle(
                    "[planner] Multi-canal activé : E={} R={} G={} D={} tokens",
                    ContextChannel.estimateTokens(typedCtx.eagerSection),
                    ContextChannel.estimateTokens(typedCtx.ragSection),
                    ContextChannel.estimateTokens(typedCtx.graphifySection),
                    ContextChannel.estimateTokens(typedCtx.docsSection)
                )
            } catch (e: Exception) {
                logger.warn("[planner] Erreur assemblage typed CompositeContext (non-bloquant) : {}", e.message)
            }
        }

        val output = StdoutFormatter.format(plan)
        println(output)
    }
}
