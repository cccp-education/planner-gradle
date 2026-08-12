package planning.steps

import contracts.agent.ChannelScore
import contracts.agent.RelevanceBudget
import contracts.context.ContextChannel
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import planning.budget.BudgetAdapter
import planning.budget.BudgetWiring
import planning.budget.RelevanceContext
import planning.steps.PlannerScenarioState.budgetDocs
import planning.steps.PlannerScenarioState.budgetEager
import planning.steps.PlannerScenarioState.budgetGraphify
import planning.steps.PlannerScenarioState.budgetIntention
import planning.steps.PlannerScenarioState.budgetLogs
import planning.steps.PlannerScenarioState.budgetRag
import planning.steps.PlannerScenarioState.budgetedContext

/**
 * Cucumber step definitions for the planner relevance-budget feature
 * (PLN-BUDGET-3 — `planner_budget.feature`).
 *
 * Pure BDD — no production code is modified by this baby-step. The budget
 * domain layer (`BudgetAdapter.applyRelevanceBudget` pure adapter +
 * `BudgetWiring.resolveBudgetedContexts` wiring + economy-of-ink guard)
 * is exercised with in-memory fixtures, mirroring the unit tests of
 * PLN-BUDGET-1/2 (S-069/070) and the BDD pattern of `PlannerVerifySteps`
 * (PLN-VERIFY-4, S-067).
 *
 * Scenarios cover the dynamic budget allocation (EAGER > RAG/GRAPHIFY/DOCS
 * when the intention matches EAGER), the fallback to original content when
 * `compute` returns an empty channel list (non-regression), the four-channel
 * preservation (budgeted content passed to LLM), and the total-tokens
 * constraint (sum of estimated tokens <= budget + tolerance).
 *
 * Shared `Given`/`When`/`Then` steps (intention, prompt building, plan task
 * count) are delegated to [PlannerVibecodingSteps] — both step classes read
 * and write through the shared [PlannerScenarioState] "World" object.
 */
class PlannerBudgetSteps : En {

    init {

        // -------------------------------------------------------------------------
        // Given — budget intention + multi-channel fixtures
        // -------------------------------------------------------------------------

        Given("a budget intention {string}") { intention: String ->
            budgetIntention = intention
        }

        Given("four multi-channel contexts where the EAGER channel matches the intention") {
            budgetEager = "afnor formation qualite ".repeat(200)
            budgetRag = "gastronomie italienne recette ".repeat(200)
            budgetGraphify = "graphe connaissance noeud ".repeat(200)
            budgetDocs = "documentation referentiel reac ".repeat(200)
        }

        Given("four multi-channel contexts with distinct content") {
            budgetEager = "eager governance data alpha ".repeat(200)
            budgetRag = "rag semantic chunks beta ".repeat(200)
            budgetGraphify = "graph relations gamma ".repeat(200)
            budgetDocs = "codex corpus docs delta ".repeat(200)
        }

        // -------------------------------------------------------------------------
        // When — budget resolution
        // -------------------------------------------------------------------------

        When("the budget is resolved for the four channels") {
            budgetedContext = BudgetWiring.resolveBudgetedContexts(
                intention = budgetIntention,
                eagerCtx = budgetEager,
                ragCtx = budgetRag,
                graphifyCtx = budgetGraphify,
                docsCtx = budgetDocs,
                log = { msg -> budgetLogs.add(msg) }
            )
        }

        When("the budget is resolved with a compute that returns no channels") {
            budgetedContext = BudgetAdapter.applyRelevanceBudget(
                intention = budgetIntention,
                eagerCtx = budgetEager,
                ragCtx = budgetRag,
                graphifyCtx = budgetGraphify,
                docsCtx = budgetDocs,
                compute = { _, _, _ -> RelevanceBudget(channels = emptyList()) }
            )
        }

        When("the budget is resolved for the four channels with total tokens {int}") { totalTokens: Int ->
            budgetedContext = BudgetWiring.resolveBudgetedContexts(
                intention = budgetIntention,
                eagerCtx = budgetEager,
                ragCtx = budgetRag,
                graphifyCtx = budgetGraphify,
                docsCtx = budgetDocs,
                totalTokens = totalTokens,
                log = { msg -> budgetLogs.add(msg) }
            )
        }

        // -------------------------------------------------------------------------
        // Then — dynamic allocation
        // -------------------------------------------------------------------------

        Then("the EAGER channel should receive more tokens than the RAG channel") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ContextChannel.estimateTokens(ctx.eager))
                .isGreaterThan(ContextChannel.estimateTokens(ctx.rag))
        }

        Then("the EAGER channel should receive more tokens than the GRAPHIFY channel") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ContextChannel.estimateTokens(ctx.eager))
                .isGreaterThan(ContextChannel.estimateTokens(ctx.graphify))
        }

        Then("the EAGER channel should receive more tokens than the DOCS channel") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ContextChannel.estimateTokens(ctx.eager))
                .isGreaterThan(ContextChannel.estimateTokens(ctx.docs))
        }

        // -------------------------------------------------------------------------
        // Then — fallback to original content
        // -------------------------------------------------------------------------

        Then("the budgeted EAGER channel should equal the original EAGER content") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ctx.eager).isEqualTo(budgetEager)
        }

        Then("the budgeted RAG channel should equal the original RAG content") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ctx.rag).isEqualTo(budgetRag)
        }

        Then("the budgeted GRAPHIFY channel should equal the original GRAPHIFY content") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ctx.graphify).isEqualTo(budgetGraphify)
        }

        Then("the budgeted DOCS channel should equal the original DOCS content") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ctx.docs).isEqualTo(budgetDocs)
        }

        // -------------------------------------------------------------------------
        // Then — four channels preserved
        // -------------------------------------------------------------------------

        Then("the budgeted context should contain {int} non-blank channels") { count: Int ->
            val ctx = requireNotNull(budgetedContext)
            val nonBlank = listOf(ctx.eager, ctx.rag, ctx.graphify, ctx.docs)
                .count { it.isNotBlank() }
            assertThat(nonBlank).isEqualTo(count)
        }

        Then("each budgeted channel should be shorter than or equal to its original") {
            val ctx = requireNotNull(budgetedContext)
            assertThat(ctx.eager.length).isLessThanOrEqualTo(budgetEager.length)
            assertThat(ctx.rag.length).isLessThanOrEqualTo(budgetRag.length)
            assertThat(ctx.graphify.length).isLessThanOrEqualTo(budgetGraphify.length)
            assertThat(ctx.docs.length).isLessThanOrEqualTo(budgetDocs.length)
        }

        // -------------------------------------------------------------------------
        // Then — total tokens constraint
        // -------------------------------------------------------------------------

        Then("the sum of estimated tokens across all channels should not exceed {int} plus a tolerance of {int}") { budget: Int, tolerance: Int ->
            val ctx = requireNotNull(budgetedContext)
            val total = ContextChannel.estimateTokens(ctx.eager) +
                ContextChannel.estimateTokens(ctx.rag) +
                ContextChannel.estimateTokens(ctx.graphify) +
                ContextChannel.estimateTokens(ctx.docs)
            assertThat(total).isLessThanOrEqualTo(budget + tolerance)
        }
    }
}