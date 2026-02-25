# src/test/resources/features/parallel_isolation.feature

Feature: Parallel Isolation Verification

  Scenario: Thread A navigates to Google
    Given I open "https://www.google.com"
    Then the page title should contain "Google"

  Scenario: Thread B navigates to GitHub
    Given I open "https://www.github.com"
    Then the page title should contain "GitHub"

  Scenario: Thread C navigates to Wikipedia
    Given I open "https://www.wikipedia.org"
    Then the page title should contain "Wikipedia"