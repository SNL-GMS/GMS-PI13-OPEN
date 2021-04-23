@integration @component @connman @system @soh @smoke
Feature: Verify that the Connman service is up and ready
  This is to validate that the service can be deployed is functional

  Background:
    Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | CONFIG_LOADER             |
    | POSTGRES_SERVICE          |
    | OSD_SERVICE               |
    | CONNMAN                   |
    | ETCD                      |

  Scenario: Verify that Connman service is ready for SoH
    Given Configuration for "CONNMAN" component test is loaded
    Given The "CONNMAN" service has been restarted
    Given The "CONNMAN" service is healthy
    Then connman responds to connection request correctly for station "TST"