package planning

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD — EPIC PLN-LIFECYCLE US-2 : functional tests for the opt-in `-Pagent.lifecycle=true`
 * flag wired in `DecomposeIntentionTask`. The lifecycle header `[LIFECYCLE]` must be emitted
 * only when the flag is set, and never by default (backward compat / économie d'encre).
 *
 * Since EPIC PLN-LLM-HUB (S-072), the LLM is resolved via `LlmBuildService` (codebase N1)
 * on the default Ollama port — these tests run against the real Ollama instance if available,
 * and degrade gracefully if not. The lifecycle block runs before the LLM call, so the
 * assertion on stdout is valid either way.
 */
class DecomposeIntentionLifecycleTest {

    @Test
    @Tag("integration")
    fun `generatePlan with agent lifecycle true emits LIFECYCLE header before LLM call`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test lifecycle on",
                "-PeagerContext=eager governance data",
                "-Pagent.lifecycle=true",
                "-PollamaBaseUrl=http://localhost:1"
            )
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(
            output.contains("[LIFECYCLE]"),
            "Lifecycle header should be emitted when -Pagent.lifecycle=true: $output"
        )
    }

    @Test
    @Tag("integration")
    fun `generatePlan without agent lifecycle does not emit LIFECYCLE header`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test lifecycle off",
                "-PeagerContext=eager governance data",
                "-PollamaBaseUrl=http://localhost:1"
            )
            .withPluginClasspath()
            .build()

        val output = result.output
        assertFalse(
            output.contains("[LIFECYCLE]"),
            "Lifecycle header should NOT be emitted by default (backward compat): $output"
        )
    }

    private fun createTestProject(): File {
        val projectDir = File(System.getProperty("java.io.tmpdir"), "test-lifecycle-${System.nanoTime()}")
        projectDir.mkdirs()

        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-lifecycle"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("education.cccp.planner")
            }
        """.trimIndent())

        return projectDir
    }
}