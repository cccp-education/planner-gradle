package planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskTypeTest {

    @Test
    fun `TaskType enum has three variants GRADLE EDIT_FILE EXEC_SHELL`() {
        val values = TaskType.values().map { it.name }.toSet()
        assertEquals(setOf("GRADLE", "EDIT_FILE", "EXEC_SHELL"), values)
    }

    @Test
    fun `Task default toolType is GRADLE and default target is blank`() {
        val task = Task(description = "Run tests", gradleTask = "./gradlew test")
        assertEquals(TaskType.GRADLE, task.toolType)
        assertEquals("", task.target)
    }

    @Test
    fun `Task accepts toolType EDIT_FILE with target`() {
        val task = Task(
            description = "Edit build file",
            gradleTask = "",
            toolType = TaskType.EDIT_FILE,
            target = "build.gradle.kts"
        )
        assertEquals(TaskType.EDIT_FILE, task.toolType)
        assertEquals("build.gradle.kts", task.target)
    }

    @Test
    fun `Task accepts toolType EXEC_SHELL with target`() {
        val task = Task(
            description = "Run shell command",
            gradleTask = "",
            toolType = TaskType.EXEC_SHELL,
            target = "git status"
        )
        assertEquals(TaskType.EXEC_SHELL, task.toolType)
        assertEquals("git status", task.target)
    }

    @Test
    fun `Task rejects blank description`() {
        assertFailsWith<IllegalArgumentException> {
            Task(description = "", gradleTask = "./gradlew test")
        }
    }

    @Test
    fun `Task rejects blank description even for non-GRADLE toolType`() {
        assertFailsWith<IllegalArgumentException> {
            Task(
                description = "   ",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = "foo.kt"
            )
        }
    }

    @Test
    fun `Task with toolType GRADLE requires non-blank gradleTask`() {
        assertFailsWith<IllegalArgumentException> {
            Task(description = "Run tests", gradleTask = "")
        }
    }

    @Test
    fun `Task with toolType EDIT_FILE requires non-blank target`() {
        assertFailsWith<IllegalArgumentException> {
            Task(
                description = "Edit file",
                gradleTask = "",
                toolType = TaskType.EDIT_FILE,
                target = ""
            )
        }
    }

    @Test
    fun `Task with toolType EXEC_SHELL requires non-blank target`() {
        assertFailsWith<IllegalArgumentException> {
            Task(
                description = "Run shell",
                gradleTask = "",
                toolType = TaskType.EXEC_SHELL,
                target = "  "
            )
        }
    }
}