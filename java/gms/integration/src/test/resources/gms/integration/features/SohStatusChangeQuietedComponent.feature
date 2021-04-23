@integration @osd @soh-quieted-soh-status-change
    
Feature: Verify that quieted soh status change events are being stored correctly

  Background:
    Given The environment is started with
      | POSTGRES_SERVICE          |
      | CONFIG_LOADER             |
      | PROCESSING_CONFIG_SERVICE |
      | OSD_SERVICE               |
      | ETCD                      |
    Given The postgres service is healthy
    When I query the database schema objects
    Then I should get the expected list of schemas

  Scenario: Verify that soh status change is stored correctly to the osd
    When I connect to the database via the OSD interface
    When Quieted soh status change is stored in the osd
    Then Querying by quieted until time after a given instant returns the correct quieted soh status changes


Scenario: Verify that soh status change is updated correctly in the osd
    When I connect to the database via the OSD interface
    When Quieted soh status change is stored in the osd
    And Quieted soh status change is updated and stored in the osd
    Then Querying by quieted until time after a given instant returns the correct quieted soh status changes
