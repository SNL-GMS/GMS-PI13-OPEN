@integration @osd @frameworks-osd-service
Feature: Verify that the framework OSD service is up and ready
  This is to validate that the OSD service, when deployed, is functional

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

  Scenario: Verify postgres db can be connected to via JPA
    When I connect to the database via the OSD interface
    And I attempt to get all Channels
    Then I get a non-empty list of channels

  Scenario: Verify the Framework OSD Service is alive
    Given The "OSD_SERVICE" service is healthy
    Then The Frameworks OSD Service works

