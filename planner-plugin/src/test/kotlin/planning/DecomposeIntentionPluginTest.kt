package planning

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecomposeIntentionPluginTest {

    @Test
    fun `plugin registers generatePlan task`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("tasks", "--group", "generate")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("generatePlan"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `generatePlan task belongs to generate group`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Generate tasks"))
    }

    @Test
    @Tag("integration")
    fun `generatePlan with valid intention outputs structured format`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generatePlan", "-Pintention=add unit tests", "-PollamaBaseUrl=http://localhost:1")
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(
            output.contains("[PLAN]") ||
            output.contains("[ERROR]") ||
            output.contains("[FATAL]")
        )
    }

    @Test
    @Tag("integration")
    fun `generatePlan with missing intention does not crash`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generatePlan", "-PollamaBaseUrl=http://localhost:1")
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(
            output.contains("[PLAN]") ||
            output.contains("[ERROR]") ||
            output.contains("[FATAL]") ||
            output.contains("BUILD SUCCESSFUL"),
            "Should not crash even without explicit intention"
        )
    }

    private fun createTestProject(): File {
        val projectDir = File(System.getProperty("java.io.tmpdir"), "test-project-${System.nanoTime()}")
        projectDir.mkdirs()

        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("education.cccp.planner")
            }
        """.trimIndent())

        return projectDir
    }
}
