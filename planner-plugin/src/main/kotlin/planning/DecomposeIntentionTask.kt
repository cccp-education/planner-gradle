package planning

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "LLM output is probabilistic — never cache")
abstract class DecomposeIntentionTask : DefaultTask() {

    @get:Input
    abstract val intention: Property<String>

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val specsDir: DirectoryProperty

    @get:Input
    abstract val ollamaModel: Property<String>

    @get:Input
    abstract val ollamaBaseUrl: Property<String>

    @TaskAction
    fun decompose() {
        val intent = intention.get()
        val specContents = if (specsDir.isPresent) {
            SpecReader.read(specsDir.get().asFile.toPath())
        } else {
            emptyList()
        }
        val context = PlanningContext(intention = intent)
        val plan = IntentionPlanner.plan(
            intention = intent,
            context = context,
            specContents = specContents,
            logger = logger,
            ollamaModel = ollamaModel.get(),
            ollamaBaseUrl = ollamaBaseUrl.get()
        )
        val output = StdoutFormatter.format(plan)
        println(output)
    }
}
