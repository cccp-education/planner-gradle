package planning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.UserStory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLN-REVIEW-1 — the plan and its pivot metadata are persisted without
 * re-invoking the LLM. The stdout contract remains the primary output.
 */
class PlanPersistenceTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `write creates plan json metadata json and the asciidoc contract`() {
        val plan = samplePlan()
        val ascii = StdoutFormatter.format(plan)

        val planFile = PlanPersistence.write(plan, tempDir, "ollama", "0.0.2", ascii)

        assertTrue(planFile.exists())
        assertTrue(tempDir.resolve("metadata.json").exists())
        assertTrue(tempDir.resolve("plan.adoc").exists())
        assertEquals("plan.json", planFile.name)
    }

    @Test
    fun `plan json round-trips through the reader`() {
        val plan = samplePlan()

        val planFile = PlanPersistence.write(plan, tempDir, "ollama", "0.0.2", null)

        assertEquals(plan, PlanPersistence.read(planFile))
    }

    @Test
    fun `plan json carries the full hierarchy`() {
        val plan = samplePlan()

        val json = PlanPersistence.write(plan, tempDir, "ollama", "0.0.2", null).readText()

        assertTrue(json.contains("\"title\""))
        assertTrue(json.contains("\"EPIC-0\""))
        assertTrue(json.contains("first user story"))
        assertTrue(json.contains("./gradlew test"))
        assertTrue(json.contains("\"totalPoints\""))
    }

    @Test
    fun `metadata carries the pivot contract`() {
        PlanPersistence.write(samplePlan(), tempDir, "nemotron", "0.0.2", null)

        val metadata = jacksonObjectMapper().readTree(tempDir.resolve("metadata.json"))

        assertEquals("planner", metadata["source"].asText())
        assertEquals("Plan", metadata["type"].asText())
        assertEquals(1, metadata["sessions"].asInt())
        assertEquals("nemotron", metadata["model"].asText())
        assertEquals("0.0.2", metadata["version"].asText())
        assertEquals(listOf("agent-contracts", "codebase"), metadata["dependencies"].map { it.asText() })
    }

    @Test
    fun `generated at is an ISO instant`() {
        PlanPersistence.write(samplePlan(), tempDir, "ollama", "0.0.2", null)

        val metadata = jacksonObjectMapper().readTree(tempDir.resolve("metadata.json"))

        Instant.parse(metadata["generatedAt"].asText())
    }

    @Test
    fun `ascii doc is the stdout contract`() {
        val plan = samplePlan()
        val ascii = StdoutFormatter.format(plan)

        PlanPersistence.write(plan, tempDir, "ollama", "0.0.2", ascii)

        assertEquals(ascii, tempDir.resolve("plan.adoc").readText())
    }

    @Test
    fun `write creates a nested output directory`() {
        val nested = tempDir.resolve("nested/planner")

        PlanPersistence.write(samplePlan(), nested, "ollama", "0.0.2", null)

        assertTrue(nested.resolve("plan.json").exists())
        assertTrue(nested.resolve("metadata.json").exists())
    }

    @Test
    fun `absent ascii doc does not create plan adoc`() {
        PlanPersistence.write(samplePlan(), tempDir, "ollama", "0.0.2", null)

        assertFalse(tempDir.resolve("plan.adoc").exists())
    }

    private fun samplePlan(): Plan =
        Plan(
            title = "Add tests",
            epics =
                listOf(
                    Epic(
                        name = "EPIC-0",
                        description = "quality",
                        points = 5,
                        userStories =
                            listOf(
                                UserStory(
                                    description = "first user story",
                                    tasks = listOf(GradleTask(description = "run tests", gradleTask = "./gradlew test")),
                                ),
                            ),
                    ),
                ),
            totalPoints = 5,
            estimatedSessions = "1-2",
        )
}
