package planning.budget

import contracts.agent.ChannelScore
import contracts.agent.RelevanceBudget

data class RelevanceContext(
    val eager: String,
    val rag: String,
    val graphify: String,
    val docs: String
)

object BudgetAdapter {
    fun applyRelevanceBudget(
        intention: String,
        eagerCtx: String,
        ragCtx: String,
        graphifyCtx: String,
        docsCtx: String,
        totalTokens: Int = 8000,
        compute: (String, Map<String, String>, Int) -> RelevanceBudget = { i, c, t ->
            RelevanceBudget.compute(i, c, t)
        }
    ): RelevanceContext {
        val channels = mapOf(
            "EAGER" to eagerCtx,
            "RAG" to ragCtx,
            "GRAPHIFY" to graphifyCtx,
            "DOCS" to docsCtx
        )
        return try {
            val budget = compute(intention, channels, totalTokens)
            if (budget.channels.isEmpty()) {
                RelevanceContext(eagerCtx, ragCtx, graphifyCtx, docsCtx)
            } else {
                val byName = budget.channels.associateBy { it.name }
                RelevanceContext(
                    eager = byName["EAGER"]?.content ?: eagerCtx,
                    rag = byName["RAG"]?.content ?: ragCtx,
                    graphify = byName["GRAPHIFY"]?.content ?: graphifyCtx,
                    docs = byName["DOCS"]?.content ?: docsCtx
                )
            }
        } catch (e: Exception) {
            RelevanceContext(eagerCtx, ragCtx, graphifyCtx, docsCtx)
        }
    }
}
