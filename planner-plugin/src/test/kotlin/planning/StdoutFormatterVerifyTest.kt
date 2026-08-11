package planning

import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.TaskType
import contracts.agent.UserStory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StdoutFormatterVerifyTest {

    private fun planWith(vararg tasks: GradleTask): Plan = Plan(
        title = "verify plan",
        epics = listOf(
            Epic(
                name = "V-0",
                description = "Verify metadata",
                points = tasks.size,
                userStories = listOf(
                    UserStory(description = "setup", tasks = tasks.toList())
                )
            )
        ),
        totalPoints = tasks.size,
        estimatedSessions = "1"
    )

    @Test
    fun `format does not emit expectedOutput when default BUILD SUCCESSFUL`() {
        val plan = planWith(GradleTask(description = "Run tests", gradleTask = "./gradlew test"))
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertFalse(
            line.contains("expectedOutput"),
            "default expectedOutput should not be printed"
        )
    }

    @Test
    fun `format emits expectedOutput when custom`() {
        val plan = planWith(
            GradleTask(
                description = "Generate SPG",
                gradleTask = "./gradlew generateSPG",
                expectedOutput = "SPG generated"
            )
        )
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertTrue(line.contains("expectedOutput=SPG generated"), "custom expectedOutput should be printed: $line")
    }

    @Test
    fun `format emits expectedOutput for EDIT_FILE task when custom`() {
        val plan = planWith(
            GradleTask(
                description = "Edit config",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = "build.gradle.kts",
                expectedOutput = "File edited"
            )
        )
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertTrue(line.contains("expectedOutput=File edited"), "expectedOutput should follow toolType/target: $line")
    }

    @Test
    fun `format emits full line for custom expectedOutput on GRADLE task`() {
        val plan = planWith(
            GradleTask(
                description = "Run tests",
                gradleTask = "./gradlew test",
                expectedOutput = "Tests passed"
            )
        )
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertEquals(
            "    [TASK] description=\"Run tests\" toolType=GRADLE gradleTask=./gradlew test expectedOutput=Tests passed",
            line
        )
    }
}