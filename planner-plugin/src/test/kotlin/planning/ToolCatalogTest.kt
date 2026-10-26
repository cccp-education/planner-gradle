package planning

import contracts.agent.TaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolCatalogTest {

    @Test
    fun `ToolCatalog exposes the seven vibecoding tools`() {
        val names = ToolCatalog.toolNames()
        assertEquals(7, names.size)
        assertTrue("read_file" in names)
        assertTrue("write_file" in names)
        assertTrue("edit_file" in names)
        assertTrue("list_directory" in names)
        assertTrue("exit" in names)
        assertTrue("exec_shell" in names)
        assertTrue("exec_gradle" in names)
    }

    @Test
    fun `ToolCatalog provides a description for each tool`() {
        for (name in ToolCatalog.toolNames()) {
            val desc = ToolCatalog.description(name)
            assertTrue(desc.isNotBlank(), "description for '$name' must not be blank")
        }
    }

    @Test
    fun `ToolCatalog renders a prompt section listing all seven tools`() {
        val section = ToolCatalog.toPromptSection()
        assertTrue(section.contains("read_file"), "prompt section should list read_file")
        assertTrue(section.contains("write_file"), "prompt section should list write_file")
        assertTrue(section.contains("edit_file"), "prompt section should list edit_file")
        assertTrue(section.contains("list_directory"), "prompt section should list list_directory")
        assertTrue(section.contains("exit"), "prompt section should list exit")
        assertTrue(section.contains("exec_shell"), "prompt section should list exec_shell")
        assertTrue(section.contains("exec_gradle"), "prompt section should list exec_gradle")
    }

    @Test
    fun `ToolCatalog prompt section mentions all three TaskType variants`() {
        val section = ToolCatalog.toPromptSection()
        assertTrue(section.contains(TaskType.GRADLE.name), "section should mention GRADLE")
        assertTrue(section.contains(TaskType.EDIT_FILE.name), "section should mention EDIT_FILE")
        assertTrue(section.contains(TaskType.EXEC_SHELL.name), "section should mention EXEC_SHELL")
    }

    @Test
    fun `ToolCatalog toolNames matches the seven tools from vibecoding-contracts registry`() {
        val expected = listOf(
            "read_file", "write_file", "edit_file", "list_directory",
            "exit", "exec_shell", "exec_gradle"
        )
        assertEquals(expected, ToolCatalog.toolNames())
    }
}