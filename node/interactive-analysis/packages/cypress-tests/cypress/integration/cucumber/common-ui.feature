@common-ui
Feature: Common UI 
  Focus on the common-ui displays

  @non-destructive @smoke @system-message
  Scenario: Non-Destructive data check for System Message
    Given the UI is opened to the System Messages Display
    Then a system message exists
    Then auto scroll can be turned off and New Message button appears
    Then the New Message button can be clicked, scrolling to the bottom and enabling auto scroll
    Then table previous button can be pressed, disabling auto scroll and New Message button appears
    Then system messages can be cleared