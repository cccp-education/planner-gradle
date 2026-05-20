package planning.vibecoding

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VibecodingTaskIntegrationTest {

    @Test
    fun `vibecode task is registered by planning plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode")
        assertNotNull(task)
        assertTrue(task is VibecodingTask)
    }

    @Test
    fun `vibecode task has generate group`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode")
        assertEquals("generate", task.group)
    }

    @Test
    fun `vibecode task executes in dryRun with write_file plan`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("Create a test file")
        task.dryRun.set(true)
        task.maxActions.set(3)

        task.executeVibecoding()

        val auditFile = File(project.buildDir, "build/vibecoding/audit.jsonl")
        assertTrue(auditFile.exists(), "Audit file should exist after vibecode execution")
        val auditContent = auditFile.readText()
        assertTrue(auditContent.contains("session_complete"), "Audit should contain session_complete entry")
    }

    @Test
    fun `vibecode task creates audit dir on execution`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("test audit")
        task.dryRun.set(true)
        task.maxActions.set(2)

        task.executeVibecoding()

        val auditDir = File(project.buildDir, "build/vibecoding")
        assertTrue(auditDir.exists(), "Audit directory should be created")
        assertTrue(auditDir.isDirectory)
    }

    @Test
    fun `vibecode task writes session audit with error field`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("test error field")
        task.dryRun.set(true)
        task.maxActions.set(1)

        task.executeVibecoding()

        val auditFile = File(project.buildDir, "build/vibecoding/audit.jsonl")
        assertTrue(auditFile.exists())
        val content = auditFile.readText()
        assertTrue(content.contains("\"error\""), "Audit should contain error field (null or value)")
        assertTrue(content.contains("\"finished\""), "Audit should contain finished field")
        assertTrue(content.contains("\"intention\""), "Audit should contain intention field")
        assertTrue(content.contains("\"dryRun\""), "Audit should contain dryRun field")
    }

    @Test
    fun `vibecode with intention writes it in audit`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("write a hello world kotlin file")
        task.dryRun.set(true)
        task.maxActions.set(2)

        task.executeVibecoding()

        val auditFile = File(project.buildDir, "build/vibecoding/audit.jsonl")
        assertTrue(auditFile.readText().contains("write a hello world kotlin file"))
    }

    @Test
    fun `vibecode with dryRun false still produces audit`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("test real mode")
        task.dryRun.set(false)
        task.maxActions.set(2)

        task.executeVibecoding()

        val auditFile = File(project.buildDir, "build/vibecoding/audit.jsonl")
        assertTrue(auditFile.exists(), "Audit should exist even without dryRun")
        assertTrue(auditFile.readText().length > 0)
    }

    @Test
    fun `vibecode with zero maxActions finishes immediately`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        task.workspaceRoot.set(project.buildDir)
        task.intention.set("should finish immediately")
        task.dryRun.set(true)
        task.maxActions.set(0)

        task.executeVibecoding()

        val auditFile = File(project.buildDir, "build/vibecoding/audit.jsonl")
        assertTrue(auditFile.exists())
    }

    @Test
    fun `vibecode task is registered with correct interface type`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.planner")

        val task = project.tasks.getByName("vibecode") as VibecodingTask
        assertEquals("vibecode", task.name)
        assertEquals("Vibecoding agent — koog autonomous loop (context → plan → execute). Dry-run, maxActions 10. Audit trail JSONL.", task.description)
    }

    private fun assertEquals(expected: String?, actual: String?) {
        assertTrue(expected == actual, "Expected '$expected' but was '$actual'")
    }
}
