package planning

import contracts.agent.Plan
import contracts.agent.TaskType

object StdoutFormatter {

    fun format(plan: Plan): String = buildString {
        appendLine("[PLAN] title=\"${plan.title}\" totalPoints=${plan.totalPoints} estimatedSessions=${plan.estimatedSessions}")
        for (epic in plan.epics) {
            appendLine("[EPIC] name=\"${epic.name}\" description=\"${epic.description}\" points=${epic.points}")
            for (us in epic.userStories) {
                appendLine("  [US] description=\"${us.description}\"")
                for (task in us.tasks) {
                    appendLine("    [TASK] description=\"${task.description}\" ${formatTaskTail(task)}")
                }
            }
        }
    }

    private fun formatTaskTail(task: contracts.agent.GradleTask): String {
        val base = when (task.toolType) {
            TaskType.GRADLE -> "toolType=GRADLE gradleTask=${task.gradleTask}"
            TaskType.EDIT_FILE -> "toolType=EDIT_FILE target=${task.target}"
            TaskType.EXEC_SHELL -> "toolType=EXEC_SHELL target=${task.target}"
        }
        val expectedSuffix = if (task.expectedOutput != "BUILD SUCCESSFUL") " expectedOutput=${task.expectedOutput}" else ""
        return base + expectedSuffix
    }
}