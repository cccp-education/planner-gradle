package planning.lifecycle

import contracts.agent.AgentPhase
import contracts.agent.AgentState
import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.UserStory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import planning.PlanningContext

class LifecycleAdapterTest {

    private val intention = "planifier formation afnor qualite"
    private val eager = "EAGER governance content"
    private val rag = "RAG semantic content"
    private val graphify = "GRAPHIFY structural content"
    private val docs = "DOCS afnor reac corpus"

    @Test
    fun `toContextReady maps PlanningContext intention to AgentState ContextReady intention`() {
        val context = PlanningContext(intention = intention)
        val state = LifecycleAdapter.toContextReady(context)
        assertNotNull(state)
        assertEquals(intention, state.intention)
    }

    @Test
    fun `toContextReady concatenates the four channels into compositeContext`() {
        val context = PlanningContext(intention = intention)
        val state = LifecycleAdapter.toContextReady(
            context = context,
            eagerContext = eager,
            ragContext = rag,
            graphifyContext = graphify,
            docsContext = docs
        )
        assertNotNull(state)
        val composite = state.compositeContext
        assertTrue(composite.contains(eager), "compositeContext should contain EAGER content")
        assertTrue(composite.contains(rag), "compositeContext should contain RAG content")
        assertTrue(composite.contains(graphify), "compositeContext should contain GRAPHIFY content")
        assertTrue(composite.contains(docs), "compositeContext should contain DOCS content")
    }

    @Test
    fun `toContextReady maps docsContext to afnorCorpus`() {
        val context = PlanningContext(intention = intention)
        val state = LifecycleAdapter.toContextReady(
            context = context,
            eagerContext = eager,
            ragContext = rag,
            graphifyContext = graphify,
            docsContext = docs
        )
        assertNotNull(state)
        assertEquals(docs, state.afnorCorpus)
    }

    @Test
    fun `toContextReady sets phase to CLASSIFY`() {
        val context = PlanningContext(intention = intention)
        val state = LifecycleAdapter.toContextReady(context)
        assertNotNull(state)
        assertEquals(AgentPhase.CLASSIFY, state.phase)
    }

    @Test
    fun `toContextReady returns null when factory throws`() {
        val context = PlanningContext(intention = intention)
        val state = LifecycleAdapter.toContextReady(
            context = context,
            factory = { _, _, _ -> throw IllegalStateException("agent state unavailable") }
        )
        assertNull(state)
    }

    @Test
    fun `toPlanned maps Plan epics to AgentState Planned epics`() {
        val plan = samplePlan()
        val state = LifecycleAdapter.toPlanned(plan = plan, intention = intention)
        assertNotNull(state)
        assertEquals(plan.epics, state.epics)
    }

    @Test
    fun `toPlanned preserves intention compositeContext and sets phase to EXECUTE`() {
        val plan = samplePlan()
        val state = LifecycleAdapter.toPlanned(
            plan = plan,
            intention = intention,
            compositeContext = "composite",
            afnorCorpus = "afnor"
        )
        assertNotNull(state)
        assertEquals(intention, state.intention)
        assertEquals("composite", state.compositeContext)
        assertEquals("afnor", state.afnorCorpus)
        assertEquals(AgentPhase.EXECUTE, state.phase)
    }

    @Test
    fun `toPlanned returns null when factory throws`() {
        val plan = samplePlan()
        val state = LifecycleAdapter.toPlanned(
            plan = plan,
            intention = intention,
            factory = { _, _, _, _, _, _, _ -> throw IllegalStateException("agent state unavailable") }
        )
        assertNull(state)
    }

    private fun samplePlan(): Plan = Plan(
        title = "sample plan",
        epics = listOf(
            Epic(
                name = "EPIC-0",
                description = "sample epic",
                points = 2,
                userStories = listOf(
                    UserStory(
                        description = "sample us",
                        tasks = listOf(
                            GradleTask(
                                description = "sample task",
                                gradleTask = "./gradlew build"
                            )
                        )
                    )
                )
            )
        ),
        totalPoints = 2,
        estimatedSessions = "1"
    )
}