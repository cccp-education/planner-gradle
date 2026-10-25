package planning

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * PLN-REVIEW-1 — the `generatePlan` task exposes a durable output directory,
 * `build/planner` by default and `-PplanOutputDir` when overridden.
 *
 * These are wiring tests: they never invoke the LLM (only the helper task that
 * prints the configured directory is run).
 */
class PlanOutputWiringTest {
    @Test
    fun `the plan output directory defaults to build planner`() {
        val projectDir = createProject()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printPlanOutput")
                .build()

        assertTrue(result.output.contains("PLAN_OUTPUT="))
        assertTrue(result.output.contains("build/planner"))
    }

    @Test
    fun `the plan output directory honors the planOutputDir property`() {
        val projectDir = createProject()

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printPlanOutput", "-PplanOutputDir=custom-plan")
                .build()

        assertTrue(result.output.contains("PLAN_OUTPUT="))
        assertTrue(result.output.contains("custom-plan"))
    }

    private fun createProject(): File {
        val projectDir = File(System.getProperty("java.io.tmpdir"), "plan-output-${System.nanoTime()}")
        projectDir.mkdirs()

        projectDir.resolve("settings.gradle").writeText(
            """
            pluginManagement { repositories { mavenLocal(); gradlePluginPortal() } }
            rootProject.name = "plan-output"
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle").writeText(
            """
            plugins { id("education.cccp.planner") }

            tasks.register("printPlanOutput") {
                doLast {
                    def task = tasks.getByName("generatePlan")
                    println("PLAN_OUTPUT=" + task.planOutputDir.get().asFile.absolutePath)
                }
            }
            """.trimIndent(),
        )

        return projectDir
    }
}
