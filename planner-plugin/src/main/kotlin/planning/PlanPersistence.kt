package planning

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import contracts.agent.Plan
import java.io.File
import java.time.Instant

/**
 * PLN-REVIEW-1 — persists the generated plan and its taxonomy metadata.
 *
 * The stdout contract `[PLAN]/[EPIC]/[US]/[TASK]` remains the primary output;
 * this writer is **additive** and makes the plan consumable by other tools
 * (codebase, dashboard, economy-of-ink re-entry) without re-invoking the LLM.
 *
 * Pure file I/O: no Gradle, no LLM, no network.
 */
object PlanPersistence {
    const val PLAN_JSON = "plan.json"
    const val METADATA_JSON = "metadata.json"
    const val PLAN_ASCIIDOC = "plan.adoc"

    private val mapper =
        jacksonObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)

    /**
     * Writes [plan] to [outputDir] as `plan.json`, plus the pivot `metadata.json`
     * and, when [asciiDoc] is supplied, the human-readable `plan.adoc` (the same
     * text as the stdout contract).
     */
    fun write(
        plan: Plan,
        outputDir: File,
        model: String,
        version: String,
        asciiDoc: String? = null,
    ): File {
        outputDir.mkdirs()
        val planFile = File(outputDir, PLAN_JSON)
        planFile.writeText(mapper.writeValueAsString(plan))

        Metadata.writeTo(
            outputDir,
            Metadata(
                source = "planner",
                type = "Plan",
                sessions = 1,
                generatedAt = Instant.now().toString(),
                model = model,
                version = version,
                dependencies = listOf("agent-contracts", "codebase"),
            ),
        )

        if (!asciiDoc.isNullOrBlank()) {
            File(outputDir, PLAN_ASCIIDOC).writeText(asciiDoc)
        }
        return planFile
    }

    /** Reads a previously persisted `plan.json` (round-trip for consumers/tests). */
    fun read(file: File): Plan = mapper.readValue(file, Plan::class.java)
}
