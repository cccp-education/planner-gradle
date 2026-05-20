package planning.vibecoding

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import codebase.koog.AugmentedState
import codebase.koog.KoogAugmentedContextGraph
import codebase.koog.Plan
import codebase.koog.Task
import codebase.koog.ToolRegistry
import codebase.koog.VibecodingState
import kotlinx.coroutines.runBlocking

class VibecodingGraph(
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    val augmentedGraph: KoogAugmentedContextGraph = KoogAugmentedContextGraph()
) {

    val graph: AIAgentGraphStrategy<VibecodingState, VibecodingState> = strategy<VibecodingState, VibecodingState>(
        name = "vibecoding",
        toolSelectionStrategy = ToolSelectionStrategy.NONE
    ) {
        val buildContext by node<VibecodingState, VibecodingState> { state ->
            buildContextNode(state)
        }

        val classify by node<VibecodingState, VibecodingState> { state ->
            classifyNode(state)
        }

        val plan by node<VibecodingState, VibecodingState> { state ->
            planNode(state)
        }

        val executeTools by node<VibecodingState, VibecodingState> { state ->
            executeToolsNode(state)
        }

        val sendToolResult by node<VibecodingState, VibecodingState> { state ->
            if (state.dryRun) state.finish() else state.copy(finished = true)
        }

        edge(nodeStart forwardTo buildContext onCondition { _ -> true } transformed { it })
        edge(buildContext forwardTo classify onCondition { _ -> true } transformed { it })
        edge(classify forwardTo plan onCondition { _ -> true } transformed { it })
        edge(plan forwardTo executeTools onCondition { _ -> true } transformed { it })
        edge(executeTools forwardTo sendToolResult onCondition { _ -> true } transformed { it })
        edge(sendToolResult forwardTo nodeFinish onCondition { _ -> true } transformed { it })
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }

    fun buildSystemPrompt(state: VibecodingState): String = """
        |You are a Vibecoding Agent operating in a Gradle workspace.
        |
        |Intention: ${state.intention}
        |Plan: ${state.planJson.take(2000)}
        |WorkspaceRoot: ${state.workspaceRoot}
        |DryRun: ${state.dryRun}
        |Iteration: ${state.iteration} / ${state.maxActions}
        |
        |Use available tools to execute the plan step by step.
        """.trimMargin()

    fun execute(initialState: VibecodingState): VibecodingState {
        var state = initialState

        if (state.plan == null) {
            state = buildContextNode(state)
            if (state.error != null) return state

            state = classifyNode(state)
            if (state.error != null) return state

            state = planNode(state)
            if (state.error != null) return state
        }

        state = executeToolsNode(state)

        if (state.dryRun) {
            return state.finish()
        }

        return state
    }

    private fun buildContextNode(state: VibecodingState): VibecodingState {
        val augmentedState = AugmentedState(
            intention = state.intention,
            workspaceRoot = state.workspaceRoot
        )
        return try {
            val result = augmentedGraph.execute(augmentedState)
            if (result.error != null) {
                state.copy(planJson = result.planJson, plan = result.plan, classification = result.classification)
            } else {
                state
            }
        } catch (e: Exception) {
            state
        }
    }

    private fun classifyNode(state: VibecodingState): VibecodingState {
        val augmentedState = AugmentedState(
            intention = state.intention,
            workspaceRoot = state.workspaceRoot
        )
        val result = augmentedGraph.execute(augmentedState)
        return state.copy(
            planJson = result.planJson,
            plan = result.plan,
            classification = result.classification
        )
    }

    private fun planNode(state: VibecodingState): VibecodingState {
        val augmentedState = AugmentedState(
            intention = state.intention,
            workspaceRoot = state.workspaceRoot
        )
        val result = augmentedGraph.execute(augmentedState)
        return state.copy(
            planJson = result.planJson,
            plan = result.plan,
            classification = result.classification
        )
    }

    private fun executeToolsNode(state: VibecodingState): VibecodingState {
        val plan = state.plan ?: return state.copy(finished = true)
        val allTasks = parsePlanTasks(plan)
        if (allTasks.isEmpty()) return state.copy(finished = true)

        val executedResults = mutableListOf<String>()

        for (task in allTasks) {
            val (toolName, args) = parseToolCall(task.gradleTask)
            if (toolName.isEmpty()) {
                executedResults.add("ERROR: empty tool name in task '${task.description}'")
                continue
            }
            try {
                val result = toolRegistry.execute(
                    toolName = toolName,
                    arguments = args,
                    workspaceRoot = state.workspaceRoot,
                    dryRun = state.dryRun
                )
                executedResults.add(result.take(500))
            } catch (e: SecurityException) {
                executedResults.add("ERROR: Security block — ${e.message}")
            } catch (e: codebase.koog.tools.ToolkitIsMissingException) {
                executedResults.add("ERROR: Unknown tool '$toolName' — ${e.message}")
            } catch (e: Exception) {
                executedResults.add("ERROR: $toolName failed — ${e.message}")
            }
        }

        return state.copy(
            executedTasks = executedResults,
            currentTaskDescription = "Executed ${executedResults.size}/${allTasks.size} tasks"
        ).nextIteration().finish()
    }

    companion object {
        fun parsePlanTasks(plan: Plan): List<Task> {
            return plan.epics
                .flatMap { it.userStories }
                .flatMap { it.tasks }
        }

        fun parseToolCall(gradleTask: String): Pair<String, Map<String, String>> {
            val tokens = gradleTask.trim().split(" ", limit = 2)
            val toolName = tokens.first().lowercase()
            val rawArgs = if (tokens.size > 1) tokens[1] else ""

            return when (toolName) {
                "read_file" -> {
                    val path = rawArgs.trim()
                    Pair("read_file", mapOf("path" to path))
                }
                "write_file" -> {
                    val parts = rawArgs.trim().split(" ", limit = 2)
                    val path = parts.first()
                    val content = if (parts.size > 1) parts[1] else ""
                    Pair("write_file", mapOf("path" to path, "content" to content))
                }
                "edit_file" -> {
                    val parts = rawArgs.trim().split(" ", limit = 3)
                    val path = parts.first()
                    val oldStr = if (parts.size > 1) parts[1] else ""
                    val newStr = if (parts.size > 2) parts[2] else ""
                    Pair("edit_file", mapOf("path" to path, "oldString" to oldStr, "newString" to newStr))
                }
                "list_directory" -> {
                    val path = rawArgs.trim().ifEmpty { "." }
                    Pair("list_directory", mapOf("path" to path))
                }
                "exit" -> {
                    Pair("exit", emptyMap())
                }
                "exec_shell" -> {
                    val command = rawArgs.trim()
                    Pair("exec_shell", mapOf("command" to command))
                }
                "exec_gradle" -> {
                    val task = rawArgs.trim()
                    Pair("exec_gradle", mapOf("task" to task))
                }
                else -> Pair(toolName, mapOf("raw" to rawArgs))
            }
        }
    }
}
