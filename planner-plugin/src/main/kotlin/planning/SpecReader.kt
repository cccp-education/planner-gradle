package planning

import java.nio.file.Path

object SpecReader {
    data class SpecContent(
        val fileName: String,
        val text: String,
    )

    private const val TOKEN_BUDGET = 2_000
    private const val CHARS_PER_TOKEN = 4 // coarse estimate for Latin/English
    private val MAX_CONTEXT_CHARS = TOKEN_BUDGET * CHARS_PER_TOKEN

    fun read(directory: Path): List<SpecContent> {
        val dir = directory.toFile()
        if (!dir.isDirectory) return emptyList()

        return dir
            .listFiles { f -> f.isFile && f.name.endsWith(".adoc") }
            ?.sortedBy { it.name }
            ?.map { SpecContent(it.name, it.readText()) }
            ?: emptyList()
    }

    fun toPromptContext(
        specs: List<SpecContent>,
        maxChars: Int = MAX_CONTEXT_CHARS,
    ): String {
        if (specs.isEmpty()) return ""
        val sb = StringBuilder()
        for (spec in specs) {
            sb.append("--- ${spec.fileName} ---\n")
            sb.appendLine(spec.text)
            if (sb.length >= maxChars) break
        }
        return sb.toString().take(maxChars)
    }
}
