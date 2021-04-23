@integration @component @interactive @soh @smoke @ignore
Feature: Verify that all the Interactive Analysis services are up and ready
  This is to validate that the services can be deployed are functional

  Scenario: Verify that Interactive Analysis UI is alive
    Given The interactive analysis UI responds
    When I curl the UI
    Then I get expected UI result

  Scenario: Verify that Interactive Analysis Gateway is alive
    Given The gateway responds
    When I curl the gateway url
    Then I get expected gateway result