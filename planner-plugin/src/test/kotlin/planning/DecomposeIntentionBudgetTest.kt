package planning

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecomposeIntentionBudgetTest {

    @Test
    @Tag("integration")
    fun `generatePlan with multi-channel logs budget token counts before LLM call`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test budget wiring",
                "-PeagerContext=eager governance data here",
                "-PragContext=rag semantic chunks here",
                "-PgraphifyContext=graph relations here",
                "-PdocsContext=codex corpus docs here",
                "-PollamaBaseUrl=http://localhost:1"
            )
            .withPluginClasspath()
            .buildAndFail()

        val output = result.output
        assertTrue(
            output.contains("Multi-canal activ"),
            "Budget log should appear BEFORE LLM call even when Ollama is unreachable: $output"
        )
    }

    @Test
    @Tag("integration")
    fun `generatePlan without multi-channel does not log budget`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test no budget log",
                "-PollamaBaseUrl=http://localhost:1"
            )
            .withPluginClasspath()
            .buildAndFail()

        val output = result.output
        assertFalse(
            output.contains("Multi-canal activ"),
            "Budget log should be skipped when no multi-channel (economie d'encre): $output"
        )
    }

    private fun createTestProject(): File {
        val projectDir = File(System.getProperty("java.io.tmpdir"), "test-budget-${System.nanoTime()}")
        projectDir.mkdirs()

        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-budget"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("education.cccp.planner")
            }
        """.trimIndent())

        return projectDir
    }
}