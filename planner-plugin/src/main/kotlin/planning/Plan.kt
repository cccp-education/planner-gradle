package planning

import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val title: String,
    val epics: List<Epic>,
    val totalPoints: Int,
    val estimatedSessions: String
)

@Serializable
data class Epic(
    val name: String,
    val description: String,
    val points: Int,
    val userStories: List<UserStory>
)

@Serializable
data class UserStory(
    val description: String,
    val tasks: List<Task>
)

/**
 * Type of tool a [Task] drives. The planner emits multi-tool plans:
 * [GRADLE] invokes a Gradle task via `./gradlew`, while [EDIT_FILE] and
 * [EXEC_SHELL] delegate to the codebase vibecoding hub (ToolRegistry).
 *
 * Default is [GRADLE] to preserve the legacy contract of `PlannerIntegration`
 * (codebase) which only consumes `gradleTask`.
 */
@Serializable
enum class TaskType {
    GRADLE,
    EDIT_FILE,
    EXEC_SHELL
}

@Serializable
data class Task(
    val description: String,
    val gradleTask: String,
    val toolType: TaskType = TaskType.GRADLE,
    val target: String = ""
) {
    init {
        require(description.isNotBlank()) { "Task.description must not be blank" }
        require(!(toolType == TaskType.GRADLE && gradleTask.isBlank())) {
            "Task.gradleTask must not be blank when toolType is GRADLE"
        }
        require(!(toolType != TaskType.GRADLE && target.isBlank())) {
            "Task.target must not be blank when toolType is $toolType"
        }
    }
}
