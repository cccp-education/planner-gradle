package planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntentionPlannerTest {
    private val context = PlanningContext(intention = "test")

    @Test
    fun `buildPrompt includes intention`() {
        val prompt =
            IntentionPlanner.buildPrompt(
                "create a test framework",
                PlanningContext(intention = "create a test framework"),
                emptyList(),
            )
        assertTrue(prompt.contains("Intention: create a test framework"))
    }

    @Test
    fun `buildPrompt includes JSON contract structure`() {
        val prompt = IntentionPlanner.buildPrompt("test", context, emptyList())
        assertTrue(prompt.contains("\"title\""))
        assertTrue(prompt.contains("\"epics\""))
        assertTrue(prompt.contains("\"totalPoints\""))
        assertTrue(prompt.contains("\"estimatedSessions\""))
        assertTrue(prompt.contains("\"userStories\""))
        assertTrue(prompt.contains("\"tasks\""))
    }

    @Test
    fun `buildPrompt includes EPIC naming rules`() {
        val prompt = IntentionPlanner.buildPrompt("test", context, emptyList())
        assertTrue(prompt.contains("EPIC names use a short prefix"))
    }

    @Test
    fun `buildPrompt includes output-only-JSON rule`() {
        val prompt = IntentionPlanner.buildPrompt("test", context, emptyList())
        assertTrue(prompt.contains("Output ONLY the JSON object"))
    }

    @Test
    fun `buildPrompt without specs has no specs section`() {
        val prompt = IntentionPlanner.buildPrompt("test", context, emptyList())
        assertEquals(-1, prompt.indexOf("Existing specifications"))
    }

    @Test
    fun `buildPrompt with specs includes spec content`() {
        val specs =
            listOf(
                SpecReader.SpecContent("rules.adoc", "= Rules\n\nUse PREFIX-00 naming"),
                SpecReader.SpecContent("api.adoc", "= API\n\nPOST /plan"),
            )
        val prompt = IntentionPlanner.buildPrompt("add feature", context, specs)

        assertTrue(prompt.contains("Existing specifications"), "Should contain specs header")
        assertTrue(prompt.contains("rules.adoc"))
        assertTrue(prompt.contains("Use PREFIX-00 naming"))
        assertTrue(prompt.contains("api.adoc"))
        assertTrue(prompt.contains("POST /plan"))
    }

    @Test
    fun `buildPrompt with specs appears before JSON contract`() {
        val specs =
            listOf(
                SpecReader.SpecContent("guide.adoc", "Important guidelines here"),
            )
        val prompt = IntentionPlanner.buildPrompt("do X", context, specs)

        val specsIndex = prompt.indexOf("Existing specifications")
        val jsonIndex = prompt.indexOf("\"title\"")
        val intentionIndex = prompt.indexOf("Intention:")

        assertTrue(specsIndex > intentionIndex, "Specs should appear after intention")
        assertTrue(jsonIndex > specsIndex, "JSON contract should appear after specs")
    }
}
