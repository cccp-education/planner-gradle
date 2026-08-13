@adapter-extract
Feature: Planner consumes the N1 LlmProviderChatModelAdapter from codebase
  As a cross-borough maintainer, I want planner to consume the shared
  LlmProviderChatModelAdapter extracted into codebase (N1) rather than
  keeping a local duplicate copy. The ChatModel contract is preserved,
  the local planning.llm.LlmProviderChatModelAdapter file is removed,
  and the adapter forwards calls to the codebase LlmProvider.

  Scenario: The local planning.llm.LlmProviderChatModelAdapter file no longer exists
    Given the planner source tree is scanned for LlmProviderChatModelAdapter
    Then the codebase.koog.llm.adapter.LlmProviderChatModelAdapter import should be present
    And the planning.llm package should not declare a LlmProviderChatModelAdapter class

  Scenario: Planner resolves a ChatModel that is a codebase LlmProviderChatModelAdapter
    Given adapter extract registers the LLM hub on a fresh project
    When adapter extract resolves a model without a mock URL
    Then adapter extract should get a ChatModel instance
    And adapter extract should get a codebase LlmProviderChatModelAdapter instance

  Scenario: The N1 adapter forwards the prompt to the codebase LlmProvider
    Given a fake codebase LlmProvider that returns a canned plan JSON
    When the planner calls the adapter with a decompose intention prompt
    Then the fake provider should have been invoked once
    And the adapter response should wrap the canned plan JSON

  Scenario: Planner backward compatibility is preserved after extraction
    Given adapter extract registers the LLM hub on a fresh project
    When adapter extract resolves a model with a mock URL
    Then adapter extract should not get a codebase LlmProviderChatModelAdapter instance
    And adapter extract should still get a ChatModel instance