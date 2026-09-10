package planning

import org.gradle.api.Plugin
import org.gradle.api.Project
import planning.llm.PlanningLlmService.aiProvider
import planning.llm.PlanningLlmService.registerLlmBuildService
import planning.llm.PlanningLlmService.registerPlannerPort

class PlanningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("planner", PlannerExtension::class.java)
        ext.aiProvider.convention("ollama")

        val llmServiceProvider = project.registerLlmBuildService()

        // EPIC SVO-3 (D5): expose the PlannerPort to the build so codebase
        // tasks (autonomousSession, SVO-4) can resolve the N2 planner by the
        // canonical shared-services name — zero planning.* import on N1.
        project.registerPlannerPort()

        project.tasks.register(
            "generatePlan",
            DecomposeIntentionTask::class.java
        ) { task ->
            task.group = "generate"
            task.description = "Decomposes a natural language intention into a structured execution plan (EPICs → User Stories → Tasks). Optional: -PspecsDir=/path/to/specs for RAG context."
            task.intention.set(project.providers.gradleProperty("intention").orElse(ext.intention))
            val specsDirProp = project.providers.gradleProperty("specsDir")
            if (specsDirProp.isPresent) {
                task.specsDir.set(project.layout.projectDirectory.dir(specsDirProp.get()))
            } else {
                task.specsDir.set(ext.specsDir)
            }
            task.aiProvider.set(project.providers.gradleProperty("ai.provider").orElse(ext.aiProvider))
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }

        // NOTE: vibecode task supprimee de planner (split-brain resolution).
        // La tache vibecode est dans codebase-gradle (N1) uniquement.
        // Appel cross-projet: ./gradlew :codebase-plugin:vibecode --intention="..."
        //
        // EPIC PLN-LLM-HUB — planner consomme codebase (N1) comme socle LLM unifié
        // via LlmBuildService (Gradle BuildService DI). OllamaBridge standalone supprimé.
    }
}