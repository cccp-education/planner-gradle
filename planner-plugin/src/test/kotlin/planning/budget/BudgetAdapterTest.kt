package planning.budget

import contracts.agent.ChannelScore
import contracts.agent.RelevanceBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetAdapterTest {

    private val intention = "planifier formation afnor qualite"
    private val eager = repeated("afnor formation qualite")
    private val rag = repeated("gastronomie italienne recette")
    private val graphify = repeated("graphe connaissance noeud")
    private val docs = repeated("documentation referentiel reac")

    private fun repeated(words: String, times: Int = 2000): String =
        List(times) { words }.joinToString(" ")

    @Test
    fun `maps the four planner channels to EAGER RAG GRAPHIFY DOCS keys and forwards original content`() {
        var captured: Map<String, String>? = null
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            compute = { _, channels, _ ->
                captured = channels
                RelevanceBudget(channels = emptyList())
            }
        )
        val map = requireNotNull(captured)
        assertEquals(setOf("EAGER", "RAG", "GRAPHIFY", "DOCS"), map.keys)
        assertEquals(eager, map["EAGER"])
        assertEquals(rag, map["RAG"])
        assertEquals(graphify, map["GRAPHIFY"])
        assertEquals(docs, map["DOCS"])
        assertEquals(eager, result.eager)
        assertEquals(rag, result.rag)
        assertEquals(graphify, result.graphify)
        assertEquals(docs, result.docs)
    }

    @Test
    fun `passes the intention and default totalTokens 8000 to compute`() {
        var capturedIntention: String? = null
        var capturedTotalTokens: Int? = null
        BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            compute = { i, _, t ->
                capturedIntention = i
                capturedTotalTokens = t
                RelevanceBudget(channels = emptyList())
            }
        )
        assertEquals(intention, capturedIntention)
        assertEquals(8000, capturedTotalTokens)
    }

    @Test
    fun `forwards a custom totalTokens to compute`() {
        var capturedTotalTokens: Int? = null
        BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            totalTokens = 500,
            compute = { _, _, t ->
                capturedTotalTokens = t
                RelevanceBudget(channels = emptyList())
            }
        )
        assertEquals(500, capturedTotalTokens)
    }

    @Test
    fun `maps computed channel scores back to the four planner channels`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            compute = { _, _, _ ->
                RelevanceBudget(
                    channels = listOf(
                        ChannelScore("EAGER", "eager-truncated", 0.9),
                        ChannelScore("RAG", "rag-truncated", 0.5),
                        ChannelScore("GRAPHIFY", "graphify-truncated", 0.3),
                        ChannelScore("DOCS", "docs-truncated", 0.1)
                    )
                )
            }
        )
        assertEquals("eager-truncated", result.eager)
        assertEquals("rag-truncated", result.rag)
        assertEquals("graphify-truncated", result.graphify)
        assertEquals("docs-truncated", result.docs)
    }

    @Test
    fun `allocates a larger budget to the channel most relevant to the intention`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs
        )
        assertTrue(result.eager.length > result.rag.length, "relevant EAGER channel should receive more tokens")
        assertTrue(result.eager.length > result.graphify.length, "relevant EAGER channel should receive more tokens than GRAPHIFY")
        assertTrue(result.eager.length > result.docs.length, "relevant EAGER channel should receive more tokens than DOCS")
    }

    @Test
    fun `truncates content exceeding the allocated budget`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs
        )
        assertTrue(result.eager.length < eager.length, "EAGER content should be truncated to the budget")
        assertTrue(result.rag.length < rag.length, "RAG content should be truncated to the budget")
    }

    @Test
    fun `returns original content when compute returns an empty channel list`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            compute = { _, _, _ -> RelevanceBudget(channels = emptyList()) }
        )
        assertEquals(eager, result.eager)
        assertEquals(rag, result.rag)
        assertEquals(graphify, result.graphify)
        assertEquals(docs, result.docs)
    }

    @Test
    fun `returns original content when compute throws`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            compute = { _, _, _ -> throw IllegalStateException("embedding unavailable") }
        )
        assertEquals(eager, result.eager)
        assertEquals(rag, result.rag)
        assertEquals(graphify, result.graphify)
        assertEquals(docs, result.docs)
    }

    @Test
    fun `preserves blank channels as blank when compute returns channel scores`() {
        val result = BudgetAdapter.applyRelevanceBudget(
            intention = intention,
            eagerCtx = eager,
            ragCtx = "",
            graphifyCtx = graphify,
            docsCtx = "",
            compute = { _, _, _ ->
                RelevanceBudget(
                    channels = listOf(
                        ChannelScore("EAGER", "eager-truncated", 0.9),
                        ChannelScore("RAG", "", 0.5),
                        ChannelScore("GRAPHIFY", "graphify-truncated", 0.3),
                        ChannelScore("DOCS", "", 0.1)
                    )
                )
            }
        )
        assertEquals("eager-truncated", result.eager)
        assertEquals("", result.rag)
        assertEquals("graphify-truncated", result.graphify)
        assertEquals("", result.docs)
    }
}
