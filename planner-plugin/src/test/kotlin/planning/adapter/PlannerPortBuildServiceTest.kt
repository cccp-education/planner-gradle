package planning.adapter

import codebase.koog.plannerport.PlannerPort
import codebase.koog.planning.PlanState
import contracts.context.CompositeContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PlannerPortBuildServiceTest {

    private fun stub(name: String) = PlannerPort { intention, _ ->
        PlanState(intention = intention, error = name)
    }

    @Test
    fun `service exposes the port registered in the registry`() {
        val port = stub("registered")
        PlannerPortRegistry.register(port)
        val service = testService()
        assertSame(port, service.port())
    }

    @Test
    fun `canonical service name is stable for cross-borough resolution`() {
        assertEquals(
            "plannerPortService",
            PlannerPortBuildService.SERVICE_NAME,
            "codebase resolves the port by this exact name — must never change silently",
        )
    }

    @Test
    fun `delegates to the port contract`() {
        PlannerPortRegistry.register(stub("delegate"))
        val service = testService()
        val state = service.port().plan(
            "intention",
            CompositeContext(
                eagerSection = "e", ragSection = "r",
                graphifySection = "g", docsSection = "d",
                config = contracts.context.CompositeContextConfig(),
            )
        )
        assertEquals("intention", state.intention)
        assertEquals("delegate", state.error)
    }
}

private fun testService() = ConcretePlannerPortBuildService()