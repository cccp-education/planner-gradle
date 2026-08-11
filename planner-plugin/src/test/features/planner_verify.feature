@verify
Feature: Planner emits vibecoding verification metadata (PLN-VERIFY-4)

  The planner decomposes an intention into a plan whose tasks carry
  verification metadata consumed by the codebase vibecoding engine:
  `expectedOutput` (the concrete success signal a task must produce),
  `maxRetries` (budget the StepVerifier honours before giving up), and
  `verifyHook` (an optional shell script run after success to validate
  artifacts). When the LLM omits them, backward-compatible defaults are
  applied (`"BUILD SUCCESSFUL"`, `3`, `null`) so the codebase bridge
  `VibecodingGraph.extractCurrentStep` produces an identical
  `VibecodingStep` to today — zero regression on PlannerIntegration.

  These scenarios exercise the pure planner domain (LLMTask.toTask()
  defaults, IntentionPlanner.buildPrompt JSON schema + rules,
  StdoutFormatter) with in-memory JSON fixtures — no network, no LLM
  key, no codebase cross-borough dependency.

  Scenario: A plan parsed from LLM carries custom expectedOutput, maxRetries and verifyHook
    Given a raw LLM JSON response with one task carrying all three verify metadata
    When the response is parsed into a Plan
    Then the plan should contain 1 task
    And the task should have expectedOutput "SPG generated"
    And the task should have maxRetries 5
    And the task should have verifyHook "scripts/check-spg.sh"

  Scenario: Defaults are preserved when the LLM omits verify metadata
    Given a raw LLM JSON response with one legacy task without verify metadata
    When the response is parsed into a Plan
    Then the plan should contain 1 task
    And the task should have expectedOutput "BUILD SUCCESSFUL"
    And the task should have maxRetries 3
    And the task should have a null verifyHook

  Scenario: The planner prompt exposes the three verify metadata fields in the task JSON schema
    Given an intention "generate a SPG"
    When the planner builds its prompt
    Then the prompt should contain the field "expectedOutput"
    And the prompt should contain the field "maxRetries"
    And the prompt should contain the field "verifyHook"

  Scenario: The planner prompt states that expectedOutput is required and maxRetries defaults to 3 in range 1..10
    Given an intention "generate a SPG"
    When the planner builds its prompt
    Then the prompt should state that expectedOutput is required
    And the prompt should state that maxRetries defaults to 3
    And the prompt should state that maxRetries range is 1 to 10
    And the prompt should state that verifyHook is optional and rare

  Scenario: StdoutFormatter emits custom expectedOutput and stays silent on default
    Given a plan with one task carrying custom expectedOutput "Slides generated"
    When the plan is formatted to stdout
    Then the stdout line should contain "expectedOutput=Slides generated"
    And the stdout line should not contain "expectedOutput=BUILD SUCCESSFUL"