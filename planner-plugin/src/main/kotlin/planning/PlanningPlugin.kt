package planning

import org.gradle.api.Plugin
import org.gradle.api.Project
import planning.vibecoding.VibecodingTask

class PlanningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("planner", PlannerExtension::class.java)
        ext.ollamaModel.convention("deepseek-v4-pro:cloud")
        ext.ollamaBaseUrl.convention("http://localhost:11434")
        ext.formationsDir.convention(project.layout.projectDirectory.dir("data/formations"))

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
            task.ollamaModel.set(project.providers.gradleProperty("ollamaModel").orElse(ext.ollamaModel))
            task.ollamaBaseUrl.set(project.providers.gradleProperty("ollamaBaseUrl").orElse(ext.ollamaBaseUrl))
        }

        project.tasks.register("generateSPG", GenerateSPGTask::class.java) { task ->
            task.group = "generate"
            task.description = "Génère le Scénario Pédagogique Global (SPG) via LLM + contexte composite + convention over configuration formationsDir + metadata.json"
            task.outputDir.set(project.layout.buildDirectory.dir("spg"))
            task.ollamaModel.set(project.providers.gradleProperty("ollamaModel").orElse(ext.ollamaModel))
            task.ollamaBaseUrl.set(project.providers.gradleProperty("ollamaBaseUrl").orElse(ext.ollamaBaseUrl))
            task.formationsDir.set(ext.formationsDir)
            val contextFileProp = project.providers.gradleProperty("workspaceContextFile")
            if (contextFileProp.isPresent) {
                task.workspaceContextFile.set(project.layout.projectDirectory.file(contextFileProp.get()))
            }
        }

        project.tasks.register(
            "vibecode",
            VibecodingTask::class.java
        ) { task ->
            task.group = "generate"
            task.description = "Vibecoding agent — koog autonomous loop (context → plan → execute). Dry-run, maxActions 10. Audit trail JSONL."
            task.workspaceRoot.set(project.rootDir)
            task.intention.set(project.providers.gradleProperty("intention").orElse(""))
            task.dryRun.set(project.providers.gradleProperty("dryRun").map { it.toBoolean() }.orElse(false))
            task.maxActions.set(project.providers.gradleProperty("maxActions").map { it.toInt() }.orElse(10))
        }
    }
}
