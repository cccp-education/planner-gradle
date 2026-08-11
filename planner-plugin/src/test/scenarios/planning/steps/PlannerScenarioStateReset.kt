package planning.steps

import io.cucumber.java.Before

/**
 * Cucumber `@Before` hook resetting the shared [PlannerScenarioState] before
 * each scenario.
 *
 * Cucumber instantiates fresh step-definition objects per scenario, but the
 * shared state is held in a Kotlin `object` singleton (see
 * [PlannerScenarioState]) whose mutable properties would otherwise leak from
 * one scenario to the next. This hook guarantees a clean world per scenario.
 *
 * The hook lives on an object so it is registered exactly once regardless of
 * how many step-definition classes the glue instantiates. Cucumber scans the
 * glue path for `@Before`-annotated methods, so placing it on a top-level
 * object suffices as long as the class is on the glue path.
 */
object PlannerScenarioStateReset {

    @Before
    @JvmStatic
    fun resetState() {
        PlannerScenarioState.reset()
    }
}