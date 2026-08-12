@budget
Feature: Planner applies a dynamic relevance budget to multi-channel context (PLN-BUDGET-3)

  The planner decomposes an intention into a plan after collecting four
  context channels (EAGER, RAG, GRAPHIFY, DOCS). Instead of truncating each
  channel with fixed proportions (40/30/20/10), the planner delegates to
  `BudgetWiring.resolveBudgetedContexts` which calls
  `BudgetAdapter.applyRelevanceBudget` — a pure adapter mapping the four
  planner channels to `agent-contracts.RelevanceBudget.compute`. The
  budget weights each channel by its similarity to the intention
  (naive word overlap when no embeddings), so the channel most relevant
  to the intention receives a larger share of the 8000-token budget.

  When `RelevanceBudget.compute` returns an empty channel list or throws,
  the adapter falls back to the original untruncated content — the LLM
  receives the same context it would have today (backward compat). When
  all channels are blank, `BudgetWiring` skips the budget call entirely
  (economy of ink).

  These scenarios exercise the pure budget domain
  (`BudgetAdapter.applyRelevanceBudget`, `BudgetWiring.resolveBudgetedContexts`)
  with in-memory fixtures — no network, no LLM key, no codebase dependency.

  Scenario: The dynamic budget allocates more tokens to the channel matching the intention
    Given a budget intention "planifier formation afnor qualite"
    And four multi-channel contexts where the EAGER channel matches the intention
    When the budget is resolved for the four channels
    Then the EAGER channel should receive more tokens than the RAG channel
    And the EAGER channel should receive more tokens than the GRAPHIFY channel
    And the EAGER channel should receive more tokens than the DOCS channel

  Scenario: The budget falls back to original content when compute returns an empty channel list
    Given a budget intention "planifier formation afnor"
    And four multi-channel contexts with distinct content
    When the budget is resolved with a compute that returns no channels
    Then the budgeted EAGER channel should equal the original EAGER content
    And the budgeted RAG channel should equal the original RAG content
    And the budgeted GRAPHIFY channel should equal the original GRAPHIFY content
    And the budgeted DOCS channel should equal the original DOCS content

  Scenario: The four channels are preserved and passed budgeted to the LLM
    Given a budget intention "planifier formation afnor qualite"
    And four multi-channel contexts with distinct content
    When the budget is resolved for the four channels
    Then the budgeted context should contain 4 non-blank channels
    And each budgeted channel should be shorter than or equal to its original

  Scenario: The total tokens allocated across channels respects the budget
    Given a budget intention "planifier formation afnor qualite"
    And four multi-channel contexts where the EAGER channel matches the intention
    When the budget is resolved for the four channels with total tokens 8000
    Then the sum of estimated tokens across all channels should not exceed 8000 plus a tolerance of 1000