package planning.budget

import contracts.context.ContextChannel

object BudgetWiring {

    fun resolveBudgetedContexts(
        intention: String,
        eagerCtx: String,
        ragCtx: String,
        graphifyCtx: String,
        docsCtx: String,
        totalTokens: Int = 8000,
        log: (String) -> Unit = {}
    ): RelevanceContext {
        val hasMultiChannel = eagerCtx.isNotBlank() || ragCtx.isNotBlank()
            || graphifyCtx.isNotBlank() || docsCtx.isNotBlank()

        if (!hasMultiChannel) {
            return RelevanceContext(eagerCtx, ragCtx, graphifyCtx, docsCtx)
        }

        val budgeted = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eagerCtx,
            ragCtx = ragCtx,
            graphifyCtx = graphifyCtx,
            docsCtx = docsCtx,
            totalTokens = totalTokens
        )

        log(
            "[planner] Multi-canal activ\u00e9 : " +
                "E=${ContextChannel.estimateTokens(budgeted.eager)} " +
                "R=${ContextChannel.estimateTokens(budgeted.rag)} " +
                "G=${ContextChannel.estimateTokens(budgeted.graphify)} " +
                "D=${ContextChannel.estimateTokens(budgeted.docs)} tokens"
        )

        return budgeted
    }
}