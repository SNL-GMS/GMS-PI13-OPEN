Feature: UI App general features
  Smoke tests to ensure that the app in general works.
  Checks to make sure you can log in and that the page
  displays basic info about the user.

  @app @smoke-test
  Scenario: Smoke test GMS UI.
    Given we can load the GMS login page
    Then I see "GMS SOH Monitoring" in the title

  @app @smoke
  Scenario: GMS login.
    Given we can load the GMS login page
    Given I can login as "cypress-user"
    Then "cypress-user" is displayed as my username
