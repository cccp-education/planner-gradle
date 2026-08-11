package planning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import contracts.agent.TaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLMTaskVerifyTest {

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `LLMTask parses expectedOutput when provided`() {
        val json = """{"description":"Generate SPG","gradleTask":"./gradlew generateSPG","expectedOutput":"SPG generated"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals("SPG generated", task.expectedOutput)
    }

    @Test
    fun `LLMTask parses maxRetries when provided`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test","maxRetries":5}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(5, task.maxRetries)
    }

    @Test
    fun `LLMTask parses verifyHook when provided`() {
        val json = """{"description":"Build","gradleTask":"./gradlew build","verifyHook":"scripts/check-artifacts.sh"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals("scripts/check-artifacts.sh", task.verifyHook)
    }

    @Test
    fun `toTask applies expectedOutput default BUILD SUCCESSFUL when omitted`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals("BUILD SUCCESSFUL", task.expectedOutput)
    }

    @Test
    fun `toTask applies maxRetries default 3 when omitted`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals(3, task.maxRetries)
    }

    @Test
    fun `toTask applies verifyHook default null when omitted`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test"}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertNull(task.verifyHook)
    }

    @Test
    fun `toTask preserves all three verify metadata when provided together`() {
        val json = """
            {"description":"Generate slides","gradleTask":"./gradlew generateSlides",
             "expectedOutput":"Slides generated","maxRetries":2,"verifyHook":"scripts/verify-decks.sh"}
        """.trimIndent()
        val llmTask = mapper.readValue<LLMTask>(json)
        val task = llmTask.toTask()
        assertEquals("Generate slides", task.description)
        assertEquals("./gradlew generateSlides", task.gradleTask)
        assertEquals(TaskType.GRADLE, task.toolType)
        assertEquals("Slides generated", task.expectedOutput)
        assertEquals(2, task.maxRetries)
        assertEquals("scripts/verify-decks.sh", task.verifyHook)
    }

    @Test
    fun `toTask rejects blank expectedOutput via GradleTask invariants`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test","expectedOutput":""}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        try {
            llmTask.toTask()
            error("Expected IllegalArgumentException for blank expectedOutput")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("expectedOutput"))
        }
    }

    @Test
    fun `toTask rejects maxRetries below 1 via GradleTask invariants`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test","maxRetries":0}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        try {
            llmTask.toTask()
            error("Expected IllegalArgumentException for maxRetries=0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("maxRetries"))
        }
    }

    @Test
    fun `toTask rejects maxRetries above 10 via GradleTask invariants`() {
        val json = """{"description":"Run tests","gradleTask":"./gradlew test","maxRetries":11}"""
        val llmTask = mapper.readValue<LLMTask>(json)
        try {
            llmTask.toTask()
            error("Expected IllegalArgumentException for maxRetries=11")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("maxRetries"))
        }
    }
}