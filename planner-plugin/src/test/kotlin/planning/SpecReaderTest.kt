package planning

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpecReaderTest {
    @Test
    fun `read empty directory returns empty`() {
        val tmp = Files.createTempDirectory("specReaderTest")
        val result = SpecReader.read(tmp)
        assertEquals(0, result.size)
    }

    @Test
    fun `read directory with adoc files`() {
        val tmp = Files.createTempDirectory("specReaderTest")
        Files.writeString(tmp.resolve("alpha.adoc"), "content A")
        Files.writeString(tmp.resolve("beta.adoc"), "content B")
        Files.writeString(tmp.resolve("notes.txt"), "should be ignored")

        val result = SpecReader.read(tmp)
        assertEquals(2, result.size)
        assertEquals("alpha.adoc", result[0].fileName)
        assertEquals("content A", result[0].text)
        assertEquals("beta.adoc", result[1].fileName)
        assertEquals("content B", result[1].text)
    }

    @Test
    fun `read non-existent directory returns empty`() {
        val result = SpecReader.read(Path.of("/nonexistent/specs/xyz"))
        assertEquals(0, result.size)
    }

    @Test
    fun `toPromptContext with empty list returns empty string`() {
        val result = SpecReader.toPromptContext(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `toPromptContext includes header per file`() {
        val specs =
            listOf(
                SpecReader.SpecContent("a.adoc", "hello"),
                SpecReader.SpecContent("b.adoc", "world"),
            )
        val context = SpecReader.toPromptContext(specs)
        assertTrue(context.contains("--- a.adoc ---"))
        assertTrue(context.contains("hello"))
        assertTrue(context.contains("--- b.adoc ---"))
        assertTrue(context.contains("world"))
    }

    @Test
    fun `toPromptContext truncates at token budget`() {
        val specs =
            listOf(
                SpecReader.SpecContent("big.adoc", "x".repeat(10_000)),
            )
        val context = SpecReader.toPromptContext(specs, maxChars = 200)
        assertEquals(200, context.length)
    }
}
