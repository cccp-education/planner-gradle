package planning

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * TDD — EPIC 3 : tests unitaires pour le buildPrompt multi-canal d'IntentionPlanner.
 * Vérifie que les 4 canaux (EAGER, RAG, Graphify, Docs) sont injectés dans le prompt.
 */
class IntentionPlannerMultiCanalTest {

    private val context = PlanningContext(intention = "test multi-canal")

    @Test
    fun `buildPrompt 8-param includes eager context`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList(),
            eagerContext = "EPIC TEST-0: verify eager",
            ragContext = "",
            graphifyContext = "",
            docsContext = ""
        )
        assertTrue(prompt.contains("EAGER CONTEXT"))
        assertTrue(prompt.contains("EPIC TEST-0: verify eager"))
        assertTrue(prompt.contains("governance, EPIC status"))
    }

    @Test
    fun `buildPrompt 8-param includes rag context`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList(),
            eagerContext = "",
            ragContext = "vector chunk: class IntentionPlanner",
            graphifyContext = "",
            docsContext = ""
        )
        assertTrue(prompt.contains("SEMANTIC CONTEXT"))
        assertTrue(prompt.contains("vector chunk: class IntentionPlanner"))
        assertTrue(prompt.contains("RAG pgvector"))
    }

    @Test
    fun `buildPrompt 8-param includes graphify context`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList(),
            eagerContext = "",
            ragContext = "",
            graphifyContext = "nodes: 42, edges: 87",
            docsContext = ""
        )
        assertTrue(prompt.contains("GRAPH CONTEXT"))
        assertTrue(prompt.contains("nodes: 42, edges: 87"))
        assertTrue(prompt.contains("structural relations"))
    }

    @Test
    fun `buildPrompt 8-param includes docs context`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList(),
            eagerContext = "",
            ragContext = "",
            graphifyContext = "",
            docsContext = "AFNOR Referentiel Chapitre 2"
        )
        assertTrue(prompt.contains("DOCUMENT CONTEXT"))
        assertTrue(prompt.contains("AFNOR Referentiel Chapitre 2"))
        assertTrue(prompt.contains("codex corpus"))
    }

    @Test
    fun `buildPrompt 8-param includes all 4 contexts`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "plan epic N",
            context = context,
            specContents = emptyList(),
            eagerContext = "GOVERNANCE",
            ragContext = "RAG_RESULT",
            graphifyContext = "GRAPH_DATA",
            docsContext = "DOCS_DATA"
        )
        assertTrue(prompt.contains("EAGER CONTEXT"))
        assertTrue(prompt.contains("SEMANTIC CONTEXT"))
        assertTrue(prompt.contains("GRAPH CONTEXT"))
        assertTrue(prompt.contains("DOCUMENT CONTEXT"))
        assertTrue(prompt.contains("GOVERNANCE"))
        assertTrue(prompt.contains("RAG_RESULT"))
        assertTrue(prompt.contains("GRAPH_DATA"))
        assertTrue(prompt.contains("DOCS_DATA"))
    }

    @Test
    fun `buildPrompt 8-param empty contexts add no headers`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList(),
            eagerContext = "",
            ragContext = "",
            graphifyContext = "",
            docsContext = ""
        )
        assertTrue(!prompt.contains("EAGER CONTEXT"))
        assertTrue(!prompt.contains("SEMANTIC CONTEXT"))
        assertTrue(!prompt.contains("GRAPH CONTEXT"))
        assertTrue(!prompt.contains("DOCUMENT CONTEXT"))
        // Must still have the JSON contract
        assertTrue(prompt.contains("\"title\""))
        assertTrue(prompt.contains("\"epics\""))
    }

    @Test
    fun `buildPrompt 4-param backward compatible`() {
        val prompt = IntentionPlanner.buildPrompt(
            intention = "test",
            context = context,
            specContents = emptyList()
        )
        assertTrue(prompt.contains("Intention: test"))
        assertTrue(prompt.contains("\"title\""))
        assertTrue(!prompt.contains("EAGER CONTEXT"))
        assertTrue(!prompt.contains("SEMANTIC CONTEXT"))
    }
}
