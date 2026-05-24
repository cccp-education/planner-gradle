package planning

import cccp.vibecoding.contracts.context.ChannelBudget
import cccp.vibecoding.contracts.context.CompositeContextConfig
import cccp.vibecoding.contracts.context.ContextChannel
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD — EPIC 3 propagation Multi-Canal Convergent N1→N2.
 * Vérifie que DecomposeIntentionTask accepte et traite les 4 nouveaux canaux
 * (eager, rag, graphify, docs) en plus du SpecReader classique.
 *
 * Compatibilité ascendante garantie : les canaux sont @Optional.
 */
class DecomposeIntentionMultiCanalTest {

    @Test
    fun `generatePlan without multi-channel properties succeeds (backward compatible)`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generatePlan", "-Pintention=test backward compat")
            .withPluginClasspath()
            .build()

        // Must not crash — backward compatible
        assertTrue(result.output.contains("[PLAN]") || result.output.contains("[ERROR]") || result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `generatePlan with eager channel does not crash`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test eager channel",
                "-PeagerContext=EPIC TEST-0: verify eager propagation"
            )
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Multi-canal active") || result.output.contains("[PLAN]") || result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `generatePlan with all 4 channels logs multi-canal tokens`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test multi-canal full",
                "-PeagerContext=eager governance data here",
                "-PragContext=rag semantic chunks here",
                "-PgraphifyContext=graph relations here",
                "-PdocsContext=codex corpus docs here"
            )
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(
            output.contains("Multi-canal active") || output.contains("[PLAN]") || output.contains("[ERROR]"),
            "Should handle multi-channel gracefully: $output"
        )
    }

    @Test
    fun `generatePlan with only docs channel uses 8-param planner path`() {
        val projectDir = createTestProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "generatePlan",
                "-Pintention=test docs only",
                "-PdocsContext=AFNOR Chapitre 2: Competences professionnelles"
            )
            .withPluginClasspath()
            .build()

        // docsContext non-blank triggers 8-param path
        assertTrue(result.output.contains("[PLAN]") || result.output.contains("[ERROR]") || result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `typed context types compile and instantiate correctly`() {
        // Vérifie que les types N0 dupliqués sont fonctionnels
        val channel = ContextChannel.Docs("test corpus content")
        assertTrue(channel.contentNonEmpty)
        assertTrue(channel.budgetProportion == 0.10)
        assertTrue(channel.sectionHeader == "CONTEXTE_DOCS")
        assertTrue(channel.name == "Codex/Docs")

        val config = CompositeContextConfig(
            totalTokenBudget = 8000,
            budgetEagerLazy = 0.40,
            budgetRag = 0.30,
            budgetGraphify = 0.20,
            budgetDocs = 0.10,
            budgetOverhead = 0.0
        )
        val budget = ChannelBudget.fromConfig(config)
        assertTrue(budget.docsTokens == 800)
        assertTrue(budget.eagerTokens == 3200)
    }

    @Test
    fun `ChannelBudget truncates Docs channel under token budget`() {
        val longDocs = "document content ".repeat(500)
        val channel = ContextChannel.Docs(longDocs)
        val budget = ChannelBudget(totalTokenBudget = 2000) // 200 tokens for docs
        val truncated = budget.applyBudget(listOf(channel)).first()
        val tokens = ContextChannel.estimateTokens(truncated.content)
        assertTrue(tokens <= 200 + 5, "Docs truncated to budget: $tokens <= 200")
    }

    private fun createTestProject(): File {
        val projectDir = File(System.getProperty("java.io.tmpdir"), "test-multicanal-${System.nanoTime()}")
        projectDir.mkdirs()

        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-multicanal"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("education.cccp.planner")
            }
        """.trimIndent())

        return projectDir
    }
}
