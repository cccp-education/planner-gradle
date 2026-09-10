package planning.adapter

import codebase.koog.plannerport.PlannerPort
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * JVM-wide registry for the [PlannerPort] wired by the planner plugin
 * (EPIC SVO-3, D5).
 *
 * The Gradle [PlannerPortBuildService] cannot carry an arbitrary Kotlin
 * object through its serializable parameters — so the wired port lives in
 * this plain registry and the service simply forwards to it. Registration
 * happens once per build in `PlanningPlugin` (via `registerPlannerPort`),
 * resolution happens in the consumer borough by shared-services name.
 */
object PlannerPortRegistry {

    @Volatile
    private var port: PlannerPort? = null

    fun register(candidate: PlannerPort) {
        port = candidate
    }

    fun resolve(): PlannerPort? = port
}

/**
 * Gradle [BuildService] exposing the wired [PlannerPort] to any borough in
 * the build (EPIC SVO-3, D5 — mirror of codebase's `LlmBuildService`).
 *
 * Cross-borough contract: the consumer project applies both plugins
 * (`education.cccp.codebase` + `education.cccp.planner`) and resolves this
 * service by [SERVICE_NAME] via `gradle.sharedServices` — zero `planning.*`
 * import in codebase production code (cycle N1↔N2 stays dead, S-256).
 */
abstract class PlannerPortBuildService : BuildService<PlannerPortBuildService.EmptyParams> {

    /** Marker parameters — the port lives in [PlannerPortRegistry]. */
    interface EmptyParams : BuildServiceParameters

    fun port(): PlannerPort = PlannerPortRegistry.resolve()
        ?: codebase.koog.plannerport.PlannerPort.NoOp

    companion object {
        /**
         * Canonical shared-services name under which the service is
         * registered. codebase-side resolution looks this exact name up —
         * changing it is a breaking cross-borough contract change.
         */
        const val SERVICE_NAME = "plannerPortService"
    }
}

/** Concrete subclass for plain instantiation in unit tests (no Gradle). */
class ConcretePlannerPortBuildService : PlannerPortBuildService() {
    override fun getParameters(): EmptyParams =
        object : EmptyParams {}
}