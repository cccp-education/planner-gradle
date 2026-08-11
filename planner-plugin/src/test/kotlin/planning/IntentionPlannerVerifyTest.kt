package planning

import kotlin.test.Test
import kotlin.test.assertTrue

class IntentionPlannerVerifyTest {

    private val context = PlanningContext(intention = "verify a feature")

    private fun prompt(): String =
        IntentionPlanner.buildPrompt("verify a feature", context, emptyList())

    @Test
    fun `buildPrompt exposes expectedOutput in the task JSON schema`() {
        val p = prompt()
        assertTrue(p.contains("\"expectedOutput\""), "JSON schema should include expectedOutput")
    }

    @Test
    fun `buildPrompt exposes maxRetries in the task JSON schema`() {
        val p = prompt()
        assertTrue(p.contains("\"maxRetries\""), "JSON schema should include maxRetries")
    }

    @Test
    fun `buildPrompt exposes verifyHook in the task JSON schema`() {
        val p = prompt()
        assertTrue(p.contains("\"verifyHook\""), "JSON schema should include verifyHook")
    }

    @Test
    fun `buildPrompt documents the expectedOutput default`() {
        val p = prompt()
        assertTrue(
            p.contains("BUILD SUCCESSFUL"),
            "prompt should state the default expectedOutput"
        )
    }

    @Test
    fun `buildPrompt documents the maxRetries default`() {
        val p = prompt()
        assertTrue(
            p.contains("default") && p.contains("3"),
            "prompt should state the default maxRetries is 3"
        )
    }

    @Test
    fun `buildPrompt documents that expectedOutput is required`() {
        val p = prompt()
        assertTrue(
            p.contains("expectedOutput", ignoreCase = true) &&
                p.contains("required", ignoreCase = true),
            "prompt should state that expectedOutput is required"
        )
    }

    @Test
    fun `buildPrompt documents that verifyHook is optional and rare`() {
        val p = prompt()
        assertTrue(
            p.contains("verifyHook", ignoreCase = true) &&
                (p.contains("rare", ignoreCase = true) || p.contains("optional", ignoreCase = true)),
            "prompt should state that verifyHook is optional/rare"
        )
    }
}