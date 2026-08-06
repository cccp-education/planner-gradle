package planning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LLMTaskVibecodingTest {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val fullJson = """
        {
          "title": "Vibecoding plan",
          "epics": [
            {
              "name": "V-0",
              "description": "Bootstrap",
              "points": 3,
              "userStories": [
                {
                  "description": "Setup",
                  "tasks": [
                    {"description": "Run tests", "gradleTask": "./gradlew test"},
                    {"description": "Edit build", "gradleTask": "", "toolType": "EDIT_FILE", "target": "build.gradle.kts"},
                    {"description": "Git status", "gradleTask": "", "toolType": "EXEC_SHELL", "target": "git status"}
                  ]
                }
              ]
            }
          ],
          "totalPoints": 3,
          "estimatedSessions": "1-2"
        }
    """.trimIndent()

    @Test
    fun `LLMTask parses legacy JSON without toolType or target`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(TaskType.GRADLE, task.toolType)
        assertEquals("", task.target)
        assertEquals("./gradlew test", task.gradleTask)
    }

    @Test
    fun `LLMTask parses toolType GRADLE explicitly`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew check","toolType":"GRADLE"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(TaskType.GRADLE, task.toolType)
    }

    @Test
    fun `LLMTask parses toolType EDIT_FILE with target`() {
        val json = """{"description":"Edit config","gradleTask":"","toolType":"EDIT_FILE","target":"settings.gradle.kts"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(TaskType.EDIT_FILE, task.toolType)
        assertEquals("settings.gradle.kts", task.target)
    }

    @Test
    fun `LLMTask parses toolType EXEC_SHELL with target`() {
        val json = """{"description":"List files","gradleTask":"","toolType":"EXEC_SHELL","target":"ls -la"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(TaskType.EXEC_SHELL, task.toolType)
        assertEquals("ls -la", task.target)
    }

    @Test
    fun `toPlan propagates toolType and target through full hierarchy`() {
        val response = mapper.readValue<LLMResponse>(fullJson)
        val plan = response.toPlan()

        val tasks = plan.epics[0].userStories[0].tasks
        assertEquals(3, tasks.size)

        val gradleTask = tasks[0]
        assertEquals(TaskType.GRADLE, gradleTask.toolType)
        assertEquals("./gradlew test", gradleTask.gradleTask)
        assertEquals("", gradleTask.target)

        val editTask = tasks[1]
        assertEquals(TaskType.EDIT_FILE, editTask.toolType)
        assertEquals("build.gradle.kts", editTask.target)
        assertEquals("", editTask.gradleTask)

        val shellTask = tasks[2]
        assertEquals(TaskType.EXEC_SHELL, shellTask.toolType)
        assertEquals("git status", shellTask.target)
    }

    @Test
    fun `toTask rejects EDIT_FILE without target via Task invariants`() {
        val json = """{"description":"Edit file","gradleTask":"","toolType":"EDIT_FILE"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        assertFailsWith<IllegalArgumentException> {
            llmTask.toTask()
        }
    }
}