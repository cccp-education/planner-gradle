package planning

import com.fasterxml.jackson.annotation.JsonProperty

data class LLMResponse(
    @field:JsonProperty("title")
    val title: String,
    @field:JsonProperty("epics")
    val epics: List<LLMEpic>,
    @field:JsonProperty("totalPoints")
    val totalPoints: Int,
    @field:JsonProperty("estimatedSessions")
    val estimatedSessions: String
)

data class LLMEpic(
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("description")
    val description: String,
    @field:JsonProperty("points")
    val points: Int,
    @field:JsonProperty("userStories")
    val userStories: List<LLMUserStory>
)

data class LLMUserStory(
    @field:JsonProperty("description")
    val description: String,
    @field:JsonProperty("tasks")
    val tasks: List<LLMTask>
)

data class LLMTask(
    @field:JsonProperty("description")
    val description: String,
    @field:JsonProperty("gradleTask")
    val gradleTask: String,
    @field:JsonProperty("toolType")
    val toolType: TaskType? = null,
    @field:JsonProperty("target")
    val target: String? = null
)

fun LLMResponse.toPlan(): Plan = Plan(
    title = title,
    epics = epics.map { it.toEpic() },
    totalPoints = totalPoints,
    estimatedSessions = estimatedSessions
)

private fun LLMEpic.toEpic(): Epic = Epic(
    name = name,
    description = description,
    points = points,
    userStories = userStories.map { it.toUserStory() }
)

private fun LLMUserStory.toUserStory(): UserStory = UserStory(
    description = description,
    tasks = tasks.map { it.toTask() }
)

internal fun LLMTask.toTask(): Task = Task(
    description = description,
    gradleTask = gradleTask,
    toolType = toolType ?: TaskType.GRADLE,
    target = target ?: ""
)
