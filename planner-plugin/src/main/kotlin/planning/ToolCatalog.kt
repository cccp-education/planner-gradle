package planning

import contracts.agent.TaskType

/**
 * Tool catalogue — source unique de vérité côté planner pour les 7 outils
 * vibecoding. Aligné sur `vibecoding-contracts.ToolRegistry` (workspace-bom N0).
 *
 * Le planner ne dépend pas directement de `vibecoding-contracts` (non publié
 * proprement) : les 7 noms + descriptions sont alignés manuellement ici pour
 * éviter la drift (single-borough, zéro dépendance externe fragile).
 *
 * Consommé par `IntentionPlanner.buildPromptInternal` pour exposer le
 * catalogue au LLM.
 */
object ToolCatalog {

    private val tools: List<Pair<String, String>> = listOf(
        "read_file" to "Read the contents of a file at the given path",
        "write_file" to "Write content to a file at the given path",
        "edit_file" to "Edit a file by replacing oldString with newString at the given path",
        "list_directory" to "List the contents of a directory at the given path",
        "exit" to "Exit the vibecoding loop",
        "exec_shell" to "Execute a shell command via bash -c (DANGEROUS commands blocked)",
        "exec_gradle" to "Execute a Gradle task via ./gradlew"
    )

    fun toolNames(): List<String> = tools.map { it.first }

    fun description(name: String): String =
        tools.firstOrNull { it.first == name }?.second
            ?: throw IllegalArgumentException("Unknown tool: $name")

    /**
     * Renders the tool catalogue as a prompt section for the planner LLM,
     * documenting the three `TaskType` variants and the mapping between
     * tool name and task fields.
     */
    fun toPromptSection(): String = buildString {
        appendLine("Vibecoding tool catalogue (the plan may drive any of these):")
        for ((name, desc) in tools) {
            appendLine("- $name      — $desc")
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