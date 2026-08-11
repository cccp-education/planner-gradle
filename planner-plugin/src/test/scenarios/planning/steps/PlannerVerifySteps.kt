package planning.steps

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.UserStory
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import planning.IntentionPlanner
import planning.LLMResponse
import planning.PlanningContext
import planning.StdoutFormatter
import planning.toPlan
import planning.steps.PlannerScenarioState.parsedPlan
import planning.steps.PlannerScenarioState.prompt
import planning.steps.PlannerScenarioState.rawJson
import planning.steps.PlannerScenarioState.stdout

/**
 * Cucumber step definitions specific to the planner verify-metadata feature
 * (PLN-VERIFY-4 — `planner_verify.feature`).
 *
 * Pure BDD — no production code is modified by this baby-step. The domain
 * layer (`LLMTask.toTask()` defaults + invariants, `IntentionPlanner.buildPrompt`
 * JSON schema + rules, `StdoutFormatter` custom/default emission) is exercised
 * with in-memory JSON fixtures, mirroring the unit tests of PLN-VERIFY-2
 * (S-065) and the BDD pattern of `PlannerVibecodingSteps` (PLN-VIBE-5, S-059).
 *
 * Scenarios cover the three verify metadata (`expectedOutput`/`maxRetries`/
 * `verifyHook`) carried end-to-end by the planner, the backward-compatible
 * defaults when the LLM omits them, the prompt JSON schema + rules, and the
 * `StdoutFormatter` economy-of-ink behaviour (custom printed, default silent).
 *
 * Shared `Given`/`When`/`Then` steps (raw JSON parsing, prompt building, plan
 * task count) are delegated to [PlannerVibecodingSteps] — both step classes
 * read and write through the shared [PlannerScenarioState] "World" object so
 * steps defined on one class are visible to scenarios whose other steps live
 * on the other class. Step texts that already exist in
 * [PlannerVibecodingSteps] are NOT re-declared here (Cucumber rejects
 * duplicate step definitions across glue classes).
 */
class PlannerVerifySteps : En {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    init {

        // -------------------------------------------------------------------------
        // Given — verify-specific raw LLM JSON fixtures
        // -------------------------------------------------------------------------

        Given("a raw LLM JSON response with one task carrying all three verify metadata") {
            rawJson = """
                {
                  "title": "Verify plan",
                  "epics": [
                    {
                      "name": "V-0",
                      "description": "Generate SPG",
                      "points": 2,
                      "userStories": [
                        {
                          "description": "Generate SPG",
                          "tasks": [
                            {"description": "Generate SPG", "gradleTask": "./gradlew generateSPG",
                             "expectedOutput": "SPG generated", "maxRetries": 5,
                             "verifyHook": "scripts/check-spg.sh"}
                          ]
                        }
                      ]
                    }
                  ],
                  "totalPoints": 2,
                  "estimatedSessions": "1"
                }
            """.trimIndent()
        }

        Given("a raw LLM JSON response with one legacy task without verify metadata") {
            rawJson = """
                {
                  "title": "Legacy plan",
                  "epics": [
                    {
                      "name": "L-0",
                      "description": "Legacy",
                      "points": 1,
                      "userStories": [
                        {
                          "description": "Run tests",
                          "tasks": [
                            {"description": "Run tests", "gradleTask": "./gradlew test"}
                          ]
                        }
                      ]
                    }
                  ],
                  "totalPoints": 1,
                  "estimatedSessions": "1"
                }
            """.trimIndent()
        }

        Given("a plan with one task carrying custom expectedOutput {string}") { expectedOutput: String ->
            val task = GradleTask(
                description = "Generate slides",
                gradleTask = "./gradlew generateSlides",
                expectedOutput = expectedOutput
            )
            parsedPlan = Plan(
                title = "stdout plan",
                epics = listOf(
                    Epic(
                        name = "S-0",
                        description = "Slides",
                        points = 1,
                        userStories = listOf(
                            UserStory(description = "generate", tasks = listOf(task))
                        )
                    )
                ),
                totalPoints = 1,
                estimatedSessions = "1"
            )
        }

        // -------------------------------------------------------------------------
        // When — verify-specific actions
        // -------------------------------------------------------------------------

        When("the plan is formatted to stdout") {
            assertThat(parsedPlan).isNotNull()
            stdout = StdoutFormatter.format(parsedPlan!!)
        }

        // -------------------------------------------------------------------------
        // Then — verify metadata carried by the plan
        // -------------------------------------------------------------------------

        Then("the task should have expectedOutput {string}") { expectedOutput: String ->
            assertThat(firstTask().expectedOutput).isEqualTo(expectedOutput)
        }

        Then("the task should have maxRetries {int}") { maxRetries: Int ->
            assertThat(firstTask().maxRetries).isEqualTo(maxRetries)
        }

        Then("the task should have verifyHook {string}") { verifyHook: String ->
            assertThat(firstTask().verifyHook).isEqualTo(verifyHook)
        }

        Then("the task should have a null verifyHook") {
            assertThat(firstTask().verifyHook).isNull()
        }

        // -------------------------------------------------------------------------
        // Then — prompt schema + rules
        // -------------------------------------------------------------------------

        Then("the prompt should contain the field {string}") { field: String ->
            assertThat(prompt).contains(field)
        }

        Then("the prompt should state that expectedOutput is required") {
            val lower = prompt.lowercase()
            assertThat(lower).contains("expectedoutput")
            assertThat(lower).contains("required")
        }

        Then("the prompt should state that maxRetries defaults to {int}") { default: Int ->
            val lower = prompt.lowercase()
            assertThat(lower).contains("default")
            assertThat(prompt).contains(default.toString())
        }

        Then("the prompt should state that maxRetries range is {int} to {int}") { min: Int, max: Int ->
            assertThat(prompt).contains("$min..$max")
        }

        Then("the prompt should state that verifyHook is optional and rare") {
            val lower = prompt.lowercase()
            assertThat(lower).contains("verifyhook")
            assertThat(lower).contains("rare")
        }

        // -------------------------------------------------------------------------
        // Then — stdout formatter economy of ink
        // -------------------------------------------------------------------------

        Then("the stdout line should contain {string}") { fragment: String ->
            assertThat(stdout).contains(fragment)
        }

        Then("the stdout line should not contain {string}") { fragment: String ->
            assertThat(stdout).doesNotContain(fragment)
        }
    }

    private fun firstTask(): GradleTask =
        parsedPlan!!.epics.flatMap { it.userStories }.flatMap { it.tasks }.first()
}