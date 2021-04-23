@integration @osd @soh-status-change-event
Feature: Verify that soh status change events are being stored correctly

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
    When Soh Status Change is stored in the osd
    Then Querying for soh status change by station returns the soh status change


  Scenario: Verify that that storing a new soh status change with same station as an existing
  soh status change replaces that station
  When I connect to the database via the OSD interface
  When Soh Status Change is stored in the osd
  And New soh status change with same station as previous soh status change is stored in osd
  Then New soh status change is retrieved when queried by station



