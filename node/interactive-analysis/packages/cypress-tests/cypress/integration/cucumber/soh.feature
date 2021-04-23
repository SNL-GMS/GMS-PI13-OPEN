@soh
Feature: State of Health
  Tests the SOH displays, including Station Statistics, Overview, Lag and Missing. 
  Tests acknowledgement, selection, and that the displays open and are
  ready to load data.

@non-destructive @smoke @overview
  Scenario: Non-Destructive group filter tests for SOH Overview
    Given the UI is opened to the SOH Overview Display
    Then All_1 station group exists
    Given we can filter SOH station groups
    Then All_1 is filtered out
    Given we can filter the SOH Overview Display
    Then acknowledged good is filtered out
    Given we can filter SOH station groups they can be unfiltered
    Then All_1 is unfiltered

@non-destructive @smoke @overview
  Scenario: Non-Destructive status filter tests for SOH Overview
    Given we can filter the SOH Overview Display
    Then acknowledged good is filtered out

@non-destructive @smoke @statistics
  Scenario: Non-Destructive status filter tests for Station Statistics
    Given the UI is opened to the Station Statistics Display
    Given the user can filter the SOH display
    Then good selection should disappear
    
@non-destructive @smoke @statistics
  Scenario: Non-Destructive group filter tests for Station Statistics
    Given the UI is opened to the Station Statistics Display
    Given the user can filter out all but one group
    Then the number of cells in the acknowledged bin should decrease

@non-destructive @smoke @environment
  Scenario: Non-Destructive tests for SOH Environment
    Given The SOH Environment Display is opened and ready to load data
    Then the user can view the SOH Environment table

@non-destructive @smoke @lag
  Scenario: Non-Destructive tests for SOH Lag
    Given The SOH Lag Display is opened and ready to load data
    Then the user can view the SOH Lag chart

@non-destructive @smoke @missing
  Scenario: Non-Destructive tests for SOH Missing
    Given The SOH Missing Display is opened and ready to load data
    Then the user can view the SOH Missing chart

  
@destructive @overview
  Scenario: Destructive tests for SOH Overview
    Given The SOH Overview Display is opened and loads data
    Then the user can acknowledge in the overview

@destructive @statistics
  Scenario: Destructive tests for Station Statistics
    Given The Station Statistics Display is opened and loads data
    Then the user can acknowledge SOH in the station statistics display

@destructive @environment
  Scenario: Destructive tests for SOH Environment
    Given The SOH Environment Display is opened and ready to load data
    Then the user can view the SOH Environment table
    Then the user can quiet using the SOH Environment display
    Then the user can quiet with a comment using the SOH Environment display

@destructive @lag
  Scenario: Destructive tests for SOH Lag
    Given The SOH Lag Display is opened and ready to load data
    Then the user can cancel all quiet indicators using SOH Lag display
    Then the user can quiet using the SOH Lag display
    Then the user can cancel a channel monitor that is quieted using the SOH Lag display
    Then the user can quiet with a comment using the SOH Lag display
    Then the user can cancel quieting with a comment using the SOH Lag display
    Then the user can cancel a channel monitor that is quieted using the SOH Lag display


@destructive @missing
  Scenario: Destructive tests for SOH Missing
    Given The SOH Missing Display is opened and ready to load data
    Then the user can cancel all quiet indicators using SOH Missing display
    Then the user can quiet using the SOH Missing display
    Then the user can cancel a channel monitor that is quieted using the SOH Missing display
    Then the user can quiet with a comment using the SOH Missing display
    Then the user can cancel quieting with a comment using the SOH Missing display
    Then the user can cancel a channel monitor that is quieted using the SOH Missing display

