package planning.steps

import contracts.agent.Plan
import planning.budget.RelevanceContext

/**
 * Shared scenario state ("World") for the planner Cucumber step classes.
 *
 * Cucumber instantiates each step-definition class independently, so fields
 * declared on a step class are not visible to steps defined on another class.
 * To let `PlannerVibecodingSteps` (PLN-VIBE-5), `PlannerVerifySteps`
 * (PLN-VERIFY-4) and `PlannerBudgetSteps` (PLN-BUDGET-3) share the same
 * scenario-scoped state — the raw LLM JSON fixture, the parsed plan, the
 * built prompt, the parsing exception, the formatted stdout, the budget
 * intention/contexts/result and the budget logs — all classes read and
 * write through this object.
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

    var budgetIntention: String = ""
    var budgetEager: String = ""
    var budgetRag: String = ""
    var budgetGraphify: String = ""
    var budgetDocs: String = ""
    var budgetedContext: RelevanceContext? = null
    val budgetLogs: MutableList<String> = mutableListOf()

    fun reset() {
        rawJson = ""
        parsedPlan = null
        parsingException = null
        prompt = ""
        stdout = ""
        budgetIntention = ""
        budgetEager = ""
        budgetRag = ""
        budgetGraphify = ""
        budgetDocs = ""
        budgetedContext = null
        budgetLogs.clear()
    }
}