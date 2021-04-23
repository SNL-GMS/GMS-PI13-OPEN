Feature: Health Checks
  Checks to see if the UI front-end and UI back-end are alive,
  ready, and healthy. Also checks to ensure that Cypress has 
  built and is configured to work correctly.
  
  @non-destructive
  Scenario: Health Checks for Gateway
    Given The gateway is alive
    Given The gateway is ready
    Then The gateway is healthy
  
  @non-destructive
  Scenario: Health Check for UI
    Given The ui is alive
    Given The ui is ready
    Given The ui is healthy
    Then The user can login

  @app @smoke-test
  Scenario: Working with Webpack & cucumber is working with cypress and ts.
    Given webpack is configured
    Then this test should work just fine!