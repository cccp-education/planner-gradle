package planning

import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.TaskType
import contracts.agent.UserStory
import kotlin.test.Test
import kotlin.test.assertEquals

class StdoutFormatterVibecodingTest {

    private fun planWith(vararg tasks: GradleTask): Plan = Plan(
        title = "vibecode plan",
        epics = listOf(
            Epic(
                name = "V-0",
                description = "Bootstrap",
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
    fun `format emits toolType for GRADLE task`() {
        val plan = planWith(GradleTask(description = "Run tests", gradleTask = "./gradlew test"))
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertEquals(
            "    [TASK] description=\"Run tests\" toolType=GRADLE gradleTask=./gradlew test",
            line
        )
    }

    @Test
    fun `format emits toolType and target for EDIT_FILE task`() {
        val plan = planWith(
            GradleTask(
                description = "Edit build",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = "build.gradle.kts"
            )
        )
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertEquals(
            "    [TASK] description=\"Edit build\" toolType=EDIT_FILE target=build.gradle.kts",
            line
        )
    }

    @Test
    fun `format emits toolType and target for EXEC_SHELL task`() {
        val plan = planWith(
            GradleTask(
                description = "List files",
                gradleTask = "",
                toolType = TaskType.EXEC_SHELL,
                target = "ls -la"
            )
        )
        val line = StdoutFormatter.format(plan).trim().lines().last()
        assertEquals(
            "    [TASK] description=\"List files\" toolType=EXEC_SHELL target=ls -la",
            line
        )
    }

    @Test
    fun `format emits mixed tasks in a single plan`() {
        val plan = planWith(
            GradleTask(description = "Run tests", gradleTask = "./gradlew test"),
            GradleTask(description = "Edit config", gradleTask = "", toolType = TaskType.EDIT_FILE, target = "settings.gradle.kts"),
            GradleTask(description = "Git status", gradleTask = "", toolType = TaskType.EXEC_SHELL, target = "git status")
        )
        val lines = StdoutFormatter.format(plan).trim().lines()
        assertEquals(
            "    [TASK] description=\"Run tests\" toolType=GRADLE gradleTask=./gradlew test",
            lines[3]
        )
        assertEquals(
            "    [TASK] description=\"Edit config\" toolType=EDIT_FILE target=settings.gradle.kts",
            lines[4]
        )
        assertEquals(
            "    [TASK] description=\"Git status\" toolType=EXEC_SHELL target=git status",
            lines[5]
        )
    }
}