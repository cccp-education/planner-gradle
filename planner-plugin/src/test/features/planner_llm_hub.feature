@llm-hub
Feature: Planner consumes codebase (N1) as the unified LLM socle (PLN-LLM-HUB-3)

  The planner delegates LLM provider resolution to codebase's
  `LlmBuildService` (Gradle BuildService DI) instead of building a
  standalone `OllamaChatModel` via the legacy `OllamaBridge`. The
  `PlanningLlmService` registers the service once per build and
  resolves the langchain4j `ChatModel` by wrapping the codebase
  `LlmProvider` in a `LlmProviderChatModelAdapter`. When a mock
  Ollama URL is set via `-Pollama.baseUrl` (Cucumber/GradleTestKit
  scenarios), resolution falls back to a plain `OllamaChatModel`
  pointed at the mock — no pool required.

  The `IntentionPlanner.plan()` contract accepts the `ChatModel`
  as an injectable parameter rather than constructing one internally,
  preserving the backward-compatible 4-string + Logger call signature
  consumed by `PlannerIntegration` (codebase).

  These scenarios exercise the pure LLM-hub domain
  (`PlanningLlmService.registerLlmBuildService`,
  `PlanningLlmService.resolveModel`,
  `LlmProviderChatModelAdapter`,
  `IntentionPlanner.plan` with injected `ChatModel`)
  with in-memory fixtures — no network, no real LLM key.

  Scenario: PlanningLlmService registers a non-null LlmBuildService
    Given a fresh test project
    When the LLM build service is registered
    Then the service provider should be non-null
    And the service should expose a resolvable LlmProvider

  Scenario: The ai-provider property defaults to ollama when absent
    Given a fresh test project without any ai-provider property
    When the aiProvider property is read
    Then it should equal "ollama"

  Scenario: The ai-provider property is read, trimmed and lowercased
    Given a fresh test project with the ai-provider property set to "  GEMINI  "
    When the aiProvider property is read
    Then it should equal "gemini"

  Scenario: resolveModel returns a ChatModel backed by LlmBuildService when no mock URL is set
    Given a fresh test project with no mock Ollama URL
    When resolveModel is called for the "ollama" provider
    Then the resolved model should be a ChatModel
    And the model should wrap a codebase LlmProvider

  Scenario: resolveModel returns an OllamaChatModel when a mock URL is set
    Given a fresh test project with the ollama.baseUrl property set to "http://localhost:0"
    When resolveModel is called for the "ollama" provider
    Then the resolved model should be a ChatModel
    And the model should not wrap a codebase LlmProvider

  Scenario: IntentionPlanner.plan accepts an injected ChatModel and returns a parsed Plan
    Given a fake ChatModel that returns a valid plan JSON
    When the planner decomposes the intention using the injected model
    Then the plan title should be "LLM Hub Plan"
    And the plan should contain 1 EPIC
    And the fake model should have been invoked once