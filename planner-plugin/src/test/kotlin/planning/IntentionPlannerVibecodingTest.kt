package planning

import kotlin.test.Test
import kotlin.test.assertTrue

class IntentionPlannerVibecodingTest {

    private val context = PlanningContext(intention = "vibecode a feature")

    private fun prompt(): String =
        IntentionPlanner.buildPrompt("vibecode a feature", context, emptyList())

    @Test
    fun `buildPrompt exposes the vibecoding tool catalogue`() {
        val p = prompt()
        assertTrue(p.contains("read_file"), "catalogue should list read_file")
        assertTrue(p.contains("write_file"), "catalogue should list write_file")
        assertTrue(p.contains("edit_file"), "catalogue should list edit_file")
        assertTrue(p.contains("list_directory"), "catalogue should list list_directory")
        assertTrue(p.contains("exec_shell"), "catalogue should list exec_shell")
        assertTrue(p.contains("exec_gradle"), "catalogue should list exec_gradle")
    }

    @Test
    fun `buildPrompt documents the three TaskType variants`() {
        val p = prompt()
        assertTrue(p.contains("GRADLE"), "prompt should mention GRADLE toolType")
        assertTrue(p.contains("EDIT_FILE"), "prompt should mention EDIT_FILE toolType")
        assertTrue(p.contains("EXEC_SHELL"), "prompt should mention EXEC_SHELL toolType")
    }

    @Test
    fun `buildPrompt shows the task JSON schema with toolType and target`() {
        val p = prompt()
        assertTrue(p.contains("\"toolType\""), "JSON schema should include toolType")
        assertTrue(p.contains("\"target\""), "JSON schema should include target")
    }

    @Test
    fun `buildPrompt cites cross-borough gradle tasks`() {
        val p = prompt()
        assertTrue(p.contains(":slider:"), "prompt should mention :slider: cross-borough tasks")
        assertTrue(p.contains(":capsule:"), "prompt should mention :capsule: cross-borough tasks")
        assertTrue(p.contains(":bakery:"), "prompt should mention :bakery: cross-borough tasks")
        assertTrue(p.contains(":plantuml:"), "prompt should mention :plantuml: cross-borough tasks")
    }

    @Test
    fun `buildPrompt enforces non-regression on toolType default`() {
        val p = prompt()
        assertTrue(
            p.contains("default") && p.contains("GRADLE"),
            "prompt should state GRADLE is the default toolType"
        )
    }
}