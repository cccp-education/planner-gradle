package planning.steps

import contracts.agent.Plan

/**
 * Shared scenario state ("World") for the planner Cucumber step classes.
 *
 * Cucumber instantiates each step-definition class independently, so fields
 * declared on a step class are not visible to steps defined on another class.
 * To let `PlannerVibecodingSteps` (PLN-VIBE-5) and `PlannerVerifySteps`
 * (PLN-VERIFY-4) share the same scenario-scoped state — the raw LLM JSON
 * fixture, the parsed plan, the built prompt, the parsing exception and the
 * formatted stdout — both classes read and write through this object.
 *
 * The state is reset by Cucumber before each scenario via the
 * `@Before`-annotated hook in [PlannerScenarioStateReset] — Cucumber
 * instantiates fresh step objects per scenario, but this object is a
 * process-wide singleton, hence the explicit reset.
 */
object PlannerScenarioState {

    var rawJson: String = ""
    var parsedPlan: Plan? = null
    var parsingException: Throwable? = null
    var prompt: String = ""
    var stdout: String = ""

    fun reset() {
        rawJson = ""
        parsedPlan = null
        parsingException = null
        prompt = ""
        stdout = ""
    }
}