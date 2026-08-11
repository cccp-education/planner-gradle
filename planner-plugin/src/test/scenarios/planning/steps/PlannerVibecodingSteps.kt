package planning.steps

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import planning.IntentionPlanner
import planning.LLMResponse
import planning.PlanningContext
import planning.Plan
import planning.Task
import planning.TaskType
import planning.toPlan

/**
 * Cucumber step definitions for the planner vibecoding-aware feature
 * (PLN-VIBE-5 — `planner_vibecoding.feature`).
 *
 * Pure BDD — no production code is modified by this baby-step. The domain
 * layer (`Task` invariants, `LLMTask.toTask()`, `StdoutFormatter`,
 * `IntentionPlanner.buildPrompt()`) is exercised with in-memory JSON
 * fixtures, mirroring the unit tests of PLN-VIBE-1/2/3/4 and the BDD
 * pattern of `DeckPipelineKoogSteps` (slider SLD-8.3d).
 *
 * Scenarios cover the multi-tool plan parsing (GRADLE/EDIT_FILE/EXEC_SHELL),
 * the legacy default `GRADLE` fallback (non-regression of PlannerIntegration),
 * the `Task` invariants (EDIT_FILE requires a target), and the planner
 * prompt exposing the vibecoding tool catalogue + cross-borough tasks +
 * the "do not re-plan an EPIC TERMINE" rule.
 */
class PlannerVibecodingSteps : En {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private var rawJson: String = ""
    private var parsedPlan: Plan? = null
    private var parsingException: Throwable? = null
    private var prompt: String = ""

    init {

        // -------------------------------------------------------------------------
        // Given — raw LLM JSON fixtures
        // -------------------------------------------------------------------------

        Given("a raw LLM JSON response with one user story mixing three tool types") {
            rawJson = """
                {
                  "title": "Vibecoding plan",
                  "epics": [
                    {
                      "name": "V-0",
                      "description": "Bootstrap",
                      "points": 3,
                      "userStories": [
                        {
                          "description": "Setup",
                          "tasks": [
                            {"description": "Run tests", "gradleTask": "./gradlew test"},
                            {"description": "Edit build", "gradleTask": "", "toolType": "EDIT_FILE", "target": "build.gradle.kts"},
                            {"description": "Git status", "gradleTask": "", "toolType": "EXEC_SHELL", "target": "git status"}
                          ]
                        }
                      ]
                    }
                  ],
                  "totalPoints": 3,
                  "estimatedSessions": "1-2"
                }
            """.trimIndent()
        }

        Given("a raw LLM JSON response with a single legacy task without toolType") {
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
                            {"description": "Run tests", "gradleTask": "./gradlew check"}
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

        Given("a raw LLM JSON response with an EDIT_FILE task missing its target") {
            rawJson = """
                {
                  "title": "Bad plan",
                  "epics": [
                    {
                      "name": "B-0",
                      "description": "Bad",
                      "points": 1,
                      "userStories": [
                        {
                          "description": "Edit",
                          "tasks": [
                            {"description": "Edit file", "gradleTask": "", "toolType": "EDIT_FILE"}
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

        Given("an intention {string}") { intention: String ->
            val context = PlanningContext(intention = intention)
            prompt = IntentionPlanner.buildPrompt(intention, context, emptyList())
        }

        // -------------------------------------------------------------------------
        // When
        // -------------------------------------------------------------------------

        When("the response is parsed into a Plan") {
            val response = mapper.readValue<LLMResponse>(rawJson)
            parsedPlan = response.toPlan()
        }

        When("the response parsing is attempted") {
            try {
                val response = mapper.readValue<LLMResponse>(rawJson)
                response.toPlan()
            } catch (e: Throwable) {
                parsingException = e
            }
        }

        When("the planner builds its prompt") {
            // prompt already built in the Given step
            assertThat(prompt).isNotBlank()
        }

        // -------------------------------------------------------------------------
        // Then — multi-tool plan
        // -------------------------------------------------------------------------

        Then("the plan should contain {int} task(s)") { count: Int ->
            val tasks = parsedPlan!!.epics.flatMap { it.userStories }.flatMap { it.tasks }
            assertThat(tasks).hasSize(count)
        }

        Then("the first task should have toolType {string} and gradleTask {string}") { toolType: String, gradleTask: String ->
            val task = firstTask()
            assertThat(task.toolType).isEqualTo(TaskType.valueOf(toolType))
            assertThat(task.gradleTask).isEqualTo(gradleTask)
        }

        Then("the second task should have toolType {string} and target {string}") { toolType: String, target: String ->
            val task = taskAt(1)
            assertThat(task.toolType).isEqualTo(TaskType.valueOf(toolType))
            assertThat(task.target).isEqualTo(target)
        }

        Then("the third task should have toolType {string} and target {string}") { toolType: String, target: String ->
            val task = taskAt(2)
            assertThat(task.toolType).isEqualTo(TaskType.valueOf(toolType))
            assertThat(task.target).isEqualTo(target)
        }

        // -------------------------------------------------------------------------
        // Then — legacy default
        // -------------------------------------------------------------------------

        Then("the task should have toolType {string}") { toolType: String ->
            val task = firstTask()
            assertThat(task.toolType).isEqualTo(TaskType.valueOf(toolType))
        }

        Then("the task should have a blank target") {
            assertThat(firstTask().target).isBlank()
        }

        // -------------------------------------------------------------------------
        // Then — invariants
        // -------------------------------------------------------------------------

        Then("the parsing should fail with a message containing {string}") { fragment: String ->
            assertThat(parsingException)
                .withFailMessage("Expected parsing to fail but no exception was thrown")
                .isNotNull()
            assertThat(parsingException!!.message ?: "").contains(fragment)
        }

        // -------------------------------------------------------------------------
        // Then — prompt
        // -------------------------------------------------------------------------

        Then("the prompt should list the tool {string}") { tool: String ->
            assertThat(prompt).contains(tool)
        }

        Then("the prompt should document toolType {string}") { toolType: String ->
            assertThat(prompt).contains(toolType)
        }

        Then("the prompt should cite the cross-borough task {string}") { crossBorough: String ->
            assertThat(prompt).contains(crossBorough)
        }

        Then("the prompt should instruct not to re-plan an EPIC already TERMINE") {
            assertThat(prompt).contains("TERMINE")
        }
    }

    private fun firstTask(): Task = taskAt(0)

    private fun taskAt(index: Int): Task {
        val tasks = parsedPlan!!.epics.flatMap { it.userStories }.flatMap { it.tasks }
        return tasks[index]
    }
}