@vibecoding
Feature: Planner vibecoding-aware multi-tool decomposition (PLN-VIBE-5)

  The planner decomposes a high-level intention into a structured plan
  whose tasks may drive any of three tool types: GRADLE (default), EDIT_FILE,
  or EXEC_SHELL. The LLM response is parsed by `LLMTask.toTask()` which
  applies the GRADLE default when `toolType` is omitted, preserving the
  legacy contract of `PlannerIntegration` (codebase).

  These scenarios exercise the pure domain layer (Task invariants,
  LLMTask.toTask(), StdoutFormatter, IntentionPlanner.buildPrompt) with
  in-memory fakes — no network, no LLM key.

  Scenario: A multi-tool plan parses tasks with GRADLE, EDIT_FILE and EXEC_SHELL
    Given a raw LLM JSON response with one user story mixing three tool types
    When the response is parsed into a Plan
    Then the plan should contain 3 tasks
    And the first task should have toolType "GRADLE" and gradleTask "./gradlew test"
    And the second task should have toolType "EDIT_FILE" and target "build.gradle.kts"
    And the third task should have toolType "EXEC_SHELL" and target "git status"

  Scenario: A legacy LLM response without toolType defaults to GRADLE
    Given a raw LLM JSON response with a single legacy task without toolType
    When the response is parsed into a Plan
    Then the plan should contain 1 task
    And the task should have toolType "GRADLE"
    And the task should have a blank target

  Scenario: Task invariants reject an EDIT_FILE task without a target
    Given a raw LLM JSON response with an EDIT_FILE task missing its target
    When the response parsing is attempted
    Then the parsing should fail with a message containing "target"

  Scenario: The planner prompt exposes the vibecoding tool catalogue and cross-borough tasks
    Given an intention "vibecode a slider deck"
    When the planner builds its prompt
    Then the prompt should list the tool "edit_file"
    And the prompt should list the tool "exec_shell"
    And the prompt should document toolType "GRADLE"
    And the prompt should cite the cross-borough task ":slider:"
    And the prompt should instruct not to re-plan an EPIC already TERMINE