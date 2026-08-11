@contracts
Feature: Planner aligned on N0 contracts (PLN-CONTRACTS-5)

  The planner consumes the N0 shared kernel from workspace-bom
  (agent-contracts: Plan, Epic, UserStory, GradleTask, TaskType) instead
  of re-declaring its own types. The tool catalogue exposed in the prompt
  is aligned on the seven vibecoding tools from vibecoding-contracts
  (read_file, write_file, edit_file, list_directory, exit, exec_shell,
  exec_gradle). The bridge to codebase preserves toolType and target
  end-to-end (no silent drop).

  These scenarios exercise the pure domain layer (ToolCatalog,
  LLMTask.toTask(), IntentionPlanner.buildPrompt) with in-memory fakes —
  no network, no LLM key.

  Scenario: The tool catalogue exposes the seven vibecoding tools from registry
    Given an intention "vibecode a slider deck"
    When the planner builds its prompt
    Then the prompt should list the tool "read_file"
    And the prompt should list the tool "write_file"
    And the prompt should list the tool "edit_file"
    And the prompt should list the tool "list_directory"
    And the prompt should list the tool "exit"
    And the prompt should list the tool "exec_shell"
    And the prompt should list the tool "exec_gradle"

  Scenario: A plan emitted by planner is N0-typed (contracts.agent.Plan)
    Given a raw LLM JSON response with one user story mixing three tool types
    When the response is parsed into a Plan
    Then the plan should contain 3 tasks
    And the first task should have toolType "GRADLE" and gradleTask "./gradlew test"
    And the second task should have toolType "EDIT_FILE" and target "build.gradle.kts"
    And the third task should have toolType "EXEC_SHELL" and target "git status"

  Scenario: The toolType and target are preserved end-to-end without drop
    Given a raw LLM JSON response with one user story mixing three tool types
    When the response is parsed into a Plan
    Then the plan should contain 3 tasks
    And the first task should have toolType "GRADLE" and gradleTask "./gradlew test"
    And the second task should have toolType "EDIT_FILE" and target "build.gradle.kts"
    And the third task should have toolType "EXEC_SHELL" and target "git status"

  Scenario: The tool catalogue documents the three TaskType variants
    Given an intention "vibecode a capsule deck"
    When the planner builds its prompt
    Then the prompt should document toolType "GRADLE"
    And the prompt should document toolType "EDIT_FILE"
    And the prompt should document toolType "EXEC_SHELL"