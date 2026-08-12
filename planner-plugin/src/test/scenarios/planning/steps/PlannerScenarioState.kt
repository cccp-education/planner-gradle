package planning.steps

import contracts.agent.Plan
import dev.langchain4j.model.chat.ChatModel
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import codebase.koog.llm.service.LlmBuildService
import planning.budget.RelevanceContext

/**
 * Shared scenario state ("World") for the planner Cucumber step classes.
 *
 * Cucumber instantiates each step-definition class independently, so fields
 * declared on a step class are not visible to steps defined on another class.
 * To let `PlannerVibecodingSteps` (PLN-VIBE-5), `PlannerVerifySteps`
 * (PLN-VERIFY-4), `PlannerBudgetSteps` (PLN-BUDGET-3) and
 * `PlannerLlmHubSteps` (PLN-LLM-HUB-3) share the same scenario-scoped
 * state — the raw LLM JSON fixture, the parsed plan, the built prompt,
 * the parsing exception, the formatted stdout, the budget
 * intention/contexts/result, the budget logs, the LLM-hub project/service/
 * provider/model and the fake ChatModel — all classes read and write
 * through this object.
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

    var hubProject: Project? = null
    var hubServiceProvider: Provider<LlmBuildService>? = null
    var hubResolvedModel: ChatModel? = null
    var hubAiProvider: String? = null
    var hubFakeModel: FakeChatModel? = null
    var hubFakeInvokeCount: Int = 0

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
        hubProject = null
        hubServiceProvider = null
        hubResolvedModel = null
        hubAiProvider = null
        hubFakeModel = null
        hubFakeInvokeCount = 0
    }

    fun freshProject(properties: Map<String, Any?> = emptyMap()): Project {
        val p = ProjectBuilder.builder().build()
        properties.forEach { (k, v) -> p.extensions.extraProperties.set(k, v) }
        return p
    }
}