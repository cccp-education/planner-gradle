package planning.lifecycle

import arrow.core.Either
import contracts.agent.AgentState
import contracts.agent.Plan
import planning.PlanningContext

object LifecycleAdapter {
    fun toContextReady(
        context: PlanningContext,
        eagerContext: String = "",
        ragContext: String = "",
        graphifyContext: String = "",
        docsContext: String = "",
        factory: (String, String, String) -> AgentState.ContextReady = { intention, composite, afnor ->
            AgentState.ContextReady(
                intention = intention,
                compositeContext = composite,
                afnorCorpus = afnor,
            )
        },
    ): AgentState.ContextReady? {
        val composite = buildCompositeContext(eagerContext, ragContext, graphifyContext, docsContext)
        return Either
            .catch {
                factory(context.intention, composite, docsContext)
            }.fold({ null }, { it })
    }

    fun toPlanned(
        plan: Plan,
        intention: String,
        compositeContext: String = "",
        afnorCorpus: String = "",
        classification: String = "",
        modelChoice: String = "",
        factory: (
            String,
            String,
            String,
            String,
            String,
            String,
            List<contracts.agent.Epic>,
        ) -> AgentState.Planned = { i, c, a, cls, m, _, epics ->
            AgentState.Planned(
                intention = i,
                compositeContext = c,
                afnorCorpus = a,
                classification = cls,
                modelChoice = m,
                epics = epics,
            )
        },
    ): AgentState.Planned? =
        Either
            .catch {
                factory(intention, compositeContext, afnorCorpus, classification, modelChoice, plan.title, plan.epics)
            }.fold({ null }, { it })

    private fun buildCompositeContext(
        eager: String,
        rag: String,
        graphify: String,
        docs: String,
    ): String =
        buildString {
            if (eager.isNotBlank()) append(eager).append('\n')
            if (rag.isNotBlank()) append(rag).append('\n')
            if (graphify.isNotBlank()) append(graphify).append('\n')
            if (docs.isNotBlank()) append(docs).append('\n')
        }.trimEnd()
}
