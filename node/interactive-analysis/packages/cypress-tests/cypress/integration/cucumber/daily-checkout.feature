@smoke @daily @system
Feature: Daily System Checkouts
  Checks to see if the UI front-end and UI back-end are alive,
  ready, and healthy. Verify Integration and Test Daily Checkout Steps run.

  Background:
    Given The ui is alive
    Given The ui is ready
    Then The user can login

  @app @smoke-test
  Scenario: Verify Display 8 Group exists on the UI
    Given the UI is opened to the SOH Overview Display
    Given the "All_1|All_2|EurAsia|OthCont|Wrapped|A_To_H|I_To_Z|CD1.1|IMS_Sta|NON_IMS" station group exists
    Then this test should work just fine!

  Scenario Outline: Verify that the <groupname> has <total> stations configured
    Given the UI is opened to the SOH Overview Display
    Given the "<groupname>" station group exists
    Then "<groupname>" display has <total> stations configured for overview Display

  Examples:
  | groupname | total |
  | All_1 | 301 |
  | All_2 | 301 |
  | EurAsia | 121 |
  | OthCont | 190 |
  | Wrapped | 179 |
  | CD1.1 | 132 |
  | A_To_H | 112 |
  | I_To_Z | 199 |
  | IMS_Sta | 225 |
  | NON_IMS | 85 |




