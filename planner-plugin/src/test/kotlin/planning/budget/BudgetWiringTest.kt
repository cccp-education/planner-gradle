package planning.budget

import contracts.context.ContextChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetWiringTest {

    private val intention = "planifier formation afnor qualite"
    private val eager = "afnor formation qualite ".repeat(200)
    private val rag = "gastronomie italienne recette ".repeat(200)
    private val graphify = "graphe connaissance noeud ".repeat(200)
    private val docs = "documentation referentiel reac ".repeat(200)

    @Test
    fun `returns a RelevanceContext with the four channels budgeted`() {
        val result = BudgetWiring.resolveBudgetedContexts(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs
        )
        assertTrue(result.eager.length <= eager.length, "EAGER should be truncated to budget")
        assertTrue(result.rag.length <= rag.length, "RAG should be truncated to budget")
        assertTrue(result.graphify.length <= graphify.length, "GRAPHIFY should be truncated to budget")
        assertTrue(result.docs.length <= docs.length, "DOCS should be truncated to budget")
    }

    @Test
    fun `returns blank context when all channels are blank`() {
        val result = BudgetWiring.resolveBudgetedContexts(
            intention = intention,
            eagerCtx = "",
            ragCtx = "",
            graphifyCtx = "",
            docsCtx = ""
        )
        assertEquals("", result.eager)
        assertEquals("", result.rag)
        assertEquals("", result.graphify)
        assertEquals("", result.docs)
    }

    @Test
    fun `logs multi-canal token counts when at least one channel is non-blank`() {
        val logs = mutableListOf<String>()
        BudgetWiring.resolveBudgetedContexts(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            log = { logs.add(it) }
        )
        assertEquals(1, logs.size, "should log exactly one budget line")
        val line = logs.first()
        assertTrue(line.contains("Multi-canal"), "log should mention multi-canal: $line")
        assertTrue(line.contains("E="), "log should include EAGER tokens: $line")
        assertTrue(line.contains("R="), "log should include RAG tokens: $line")
        assertTrue(line.contains("G="), "log should include GRAPHIFY tokens: $line")
        assertTrue(line.contains("D="), "log should include DOCS tokens: $line")
        assertTrue(line.contains("tokens"), "log should mention tokens: $line")
    }

    @Test
    fun `does not log when no multi-channel is active`() {
        val logs = mutableListOf<String>()
        BudgetWiring.resolveBudgetedContexts(
            intention = intention,
            eagerCtx = "",
            ragCtx = "",
            graphifyCtx = "",
            docsCtx = "",
            log = { logs.add(it) }
        )
        assertTrue(logs.isEmpty(), "should not log budget when no multi-channel: $logs")
    }

    @Test
    fun `log token counts reflect the estimated tokens of the budgeted content`() {
        val logs = mutableListOf<String>()
        val result = BudgetWiring.resolveBudgetedContexts(
            intention = intention,
            eagerCtx = eager,
            ragCtx = rag,
            graphifyCtx = graphify,
            docsCtx = docs,
            log = { logs.add(it) }
        )
        val line = logs.first()
        val eagerTokens = ContextChannel.estimateTokens(result.eager)
        val ragTokens = ContextChannel.estimateTokens(result.rag)
        val graphifyTokens = ContextChannel.estimateTokens(result.graphify)
        val docsTokens = ContextChannel.estimateTokens(result.docs)
        assertTrue(line.contains("E=$eagerTokens"), "log should include EAGER token count: $line")
        assertTrue(line.contains("R=$ragTokens"), "log should include RAG token count: $line")
        assertTrue(line.contains("G=$graphifyTokens"), "log should include GRAPHIFY token count: $line")
        assertTrue(line.contains("D=$docsTokens"), "log should include DOCS token count: $line")
        assertFalse(result.eager.isEmpty(), "EAGER should not be empty when channel is provided")
    }
}