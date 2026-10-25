package planning

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class PlannerExtension {
    abstract val aiProvider: Property<String>
    abstract val intention: Property<String>
    abstract val specsDir: DirectoryProperty

    /** PLN-REVIEW-1 — output directory for plan.json/metadata.json/plan.adoc. */
    abstract val planOutputDir: DirectoryProperty
}