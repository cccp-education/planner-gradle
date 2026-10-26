package planning

import contracts.agent.TaskType
import contracts.vibecoding.registry.ToolRegistry

/**
 * Tool catalogue — planner's projection of the N0 vibecoding registry.
 *
 * PLN-REVIEW-2 — the seven tools (and their descriptions) are **read from**
 * `vibecoding-contracts.ToolRegistry` (Central 0.0.1), never copied locally:
 * one vocabulary, one owner. The planner only adds the prompt rendering and
 * the `TaskType` mapping guidance.
 *
 * Consumed by `IntentionPlanner.buildPromptInternal` to expose the catalogue
 * to the LLM.
 */
object ToolCatalog {
    private val registry = ToolRegistry()

    fun toolNames(): List<String> = registry.toolNames()

    fun description(name: String): String = registry.get(name).description

    /**
     * Renders the tool catalogue as a prompt section for the planner LLM,
     * documenting the three `TaskType` variants and the mapping between
     * tool name and task fields.
     */
    fun toPromptSection(): String = buildString {
        appendLine("Vibecoding tool catalogue (the plan may drive any of these):")
        for (name in registry.toolNames()) {
            appendLine("- $name      — ${registry.get(name).description}")
        }
        appendLine()
        appendLine("Each task carries a `toolType` among GRADLE (default), EDIT_FILE, EXEC_SHELL:")
        appendLine("- GRADLE     → set `gradleTask` to a realistic invocation like \"./gradlew test\".")
        appendLine("               Cross-borough examples: \":slider:generateSlides\", \":capsule:extractSpeakerNotes\",")
        appendLine("               \":bakery:publishSite\", \":plantuml:generateDiagram\".")
        appendLine("- EDIT_FILE  → set `target` to the file path; leave `gradleTask` blank.")
        appendLine("- EXEC_SHELL → set `target` to the shell command; leave `gradleTask` blank.")
    }
}
