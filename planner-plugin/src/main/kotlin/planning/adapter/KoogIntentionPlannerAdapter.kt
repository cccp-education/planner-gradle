package planning.adapter

import codebase.koog.plannerport.PlannerPort
import codebase.koog.planning.PlanState
import contracts.context.CompositeContext
import dev.langchain4j.model.chat.ChatModel
import org.slf4j.LoggerFactory
import planning.IntentionPlanner
import planning.PlanningContext

/**
 * N2 adapter implementing the codebase N1 [PlannerPort] (EPIC SVO-3, D3/D5).
 *
 * Transposes the historical `codebase.rag.PlannerIntegration.plan` (removed
 * by SVO-1) into the planner borough: the inversion point now lives on the
 * right side of the dependency arrow (planner → codebase, flèche unique).
 *
 * The adapter owns its failures: any [IntentionPlanner] error is caught and
 * returned as a failed [PlanState] — the calling graph owns the degraded
 * fallback (`PlannerPortFailed`), per the port contract.
 *
 * Testability: constructor injection of a [ChatModel] (FakeChatModel in
 * tests, [LlmProviderChatModelAdapter] over the codebase LLM pool in
 * production wiring).
 */
class KoogIntentionPlannerAdapter(
    private val model: ChatModel,
) : PlannerPort {

    private val log = LoggerFactory.getLogger(KoogIntentionPlannerAdapter::class.java)

    override fun plan(intention: String, compositeContext: CompositeContext): PlanState {
        log.info(
            "KoogIntentionPlannerAdapter: planning intention '{}' (EAGER={}B, RAG={}B, Graphify={}B, Docs={}B)",
            intention.take(80),
            compositeContext.eagerSection.length,
            compositeContext.ragSection.length,
            compositeContext.graphifySection.length,
            compositeContext.docsSection.length,
        )
        return try {
            val ctx = PlanningContext(intention = intention)
            val plan = IntentionPlanner.plan(
                intention = intention,
                context = ctx,
                specContents = emptyList(),
                eagerContext = compositeContext.eagerSection,
                ragContext = compositeContext.ragSection,
                graphifyContext = compositeContext.graphifySection,
                docsContext = compositeContext.docsSection,
                model = model,
            )
            log.info(
                "KoogIntentionPlannerAdapter: plan OK — {} EPICs, {} pts, {} sessions",
                plan.epics.size, plan.totalPoints, plan.estimatedSessions,
            )
            PlanState(
                intention = intention,
                compositeContext = compositeContext,
                classification = if (intention.length > 80) "complexe" else "simple",
                planJson = "",
                plan = plan,
                error = null,
            )
        } catch (e: Exception) {
            log.error("KoogIntentionPlannerAdapter: plan failed — {}", e.message)
            PlanState(
                intention = intention,
                compositeContext = compositeContext,
                error = "IntentionPlanner failed: ${e.message}",
            )
        }
    }
}