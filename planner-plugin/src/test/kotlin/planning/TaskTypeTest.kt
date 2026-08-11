package planning

import contracts.agent.GradleTask
import contracts.agent.TaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskTypeTest {

    @Test
    fun `TaskType enum has three variants GRADLE EDIT_FILE EXEC_SHELL`() {
        val values = TaskType.values().map { it.name }.toSet()
        assertEquals(setOf("GRADLE", "EDIT_FILE", "EXEC_SHELL"), values)
    }

    @Test
    fun `GradleTask default toolType is GRADLE and default target is blank`() {
        val task = GradleTask(description = "Run tests", gradleTask = "./gradlew test")
        assertEquals(TaskType.GRADLE, task.toolType)
        assertEquals("", task.target)
    }

    @Test
    fun `GradleTask accepts toolType EDIT_FILE with target`() {
        val task = GradleTask(
            description = "Edit build file",
            gradleTask = "",
            toolType = TaskType.EDIT_FILE,
            target = "build.gradle.kts"
        )
        assertEquals(TaskType.EDIT_FILE, task.toolType)
        assertEquals("build.gradle.kts", task.target)
    }

    @Test
    fun `GradleTask accepts toolType EXEC_SHELL with target`() {
        val task = GradleTask(
            description = "Run shell command",
            gradleTask = "",
            toolType = TaskType.EXEC_SHELL,
            target = "git status"
        )
        assertEquals(TaskType.EXEC_SHELL, task.toolType)
        assertEquals("git status", task.target)
    }

    @Test
    fun `GradleTask rejects blank description`() {
        assertFailsWith<IllegalArgumentException> {
            GradleTask(description = "", gradleTask = "./gradlew test")
        }
    }

    @Test
    fun `GradleTask rejects blank description even for non-GRADLE toolType`() {
        assertFailsWith<IllegalArgumentException> {
            GradleTask(
                description = "   ",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = "foo.kt"
            )
        }
    }

    @Test
    fun `GradleTask with toolType GRADLE requires non-blank gradleTask`() {
        assertFailsWith<IllegalArgumentException> {
            GradleTask(description = "Run tests", gradleTask = "")
        }
    }

    @Test
    fun `GradleTask with toolType EDIT_FILE requires non-blank target`() {
        assertFailsWith<IllegalArgumentException> {
            GradleTask(
                description = "Edit file",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = ""
            )
        }
    }

    @Test
    fun `GradleTask with toolType EXEC_SHELL requires non-blank target`() {
        assertFailsWith<IllegalArgumentException> {
            GradleTask(
                description = "Run shell",
                gradleTask = "",
                toolType = TaskType.EXEC_SHELL,
                target = "  "
            )
        }
    }
}