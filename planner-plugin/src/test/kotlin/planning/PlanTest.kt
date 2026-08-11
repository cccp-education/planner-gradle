package planning

import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.UserStory
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanTest {

    @Test
    fun `stdout formatter produces structured output`() {
        val plan = Plan(
            title = "test plugin",
            epics = listOf(
                Epic(
                    name = "TST-0",
                    description = "Bootstrap",
                    points = 3,
                    userStories = listOf(
                        UserStory(
                            description = "Setup project",
                            tasks = listOf(GradleTask("Create files", "./gradlew init"))
                        )
                    )
                )
            ),
            totalPoints = 3,
            estimatedSessions = "1-2"
        )

        val output = StdoutFormatter.format(plan)
        val lines = output.trim().lines()

        assertEquals("[PLAN] title=\"test plugin\" totalPoints=3 estimatedSessions=1-2", lines[0])
        assertEquals("[EPIC] name=\"TST-0\" description=\"Bootstrap\" points=3", lines[1])
        assertEquals("  [US] description=\"Setup project\"", lines[2])
        assertEquals("    [TASK] description=\"Create files\" toolType=GRADLE gradleTask=./gradlew init", lines[3])
    }
}