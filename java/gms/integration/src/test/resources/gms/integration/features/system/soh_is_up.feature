@integration @system @end2end @soh @smoke @soh-is-up
Feature: Verify that all the SoH services are up and ready
  This is to validate that the services can be deployed are all functional

  Scenario: Verify all the SoH services are alive
    Given The environment is started with
      | OSD_STATION_SOH_KAFKA_CONSUMER      |
      | POSTGRES_SERVICE                    |
      | ZOOKEEPER                           |
      | KAFKA_ONE                           |
      | CONFIG_LOADER                       |
      | PROCESSING_CONFIG_SERVICE           |
      | OSD_RSDF_KAFKA_CONSUMER             |
      | OSD_SERVICE                         |
      | SOH_CONTROL                         |
      | CONNMAN                             |
      | DATAMAN                             |
      | INTERACTIVE_ANALYSIS_UI             |
      | INTERACTIVE_ANALYSIS_CONFIG_SERVICE |
      | INTERACTIVE_ANALYSIS_API_GATEWAY    |
      | ETCD                                |
    # The is healthy check performs a created, running and healthy check
    Given The "POSTGRES_SERVICE" service is healthy
    And The "CONNMAN" service is healthy
    And The "DATAMAN" service is healthy
    And The "OSD_STATION_SOH_KAFKA_CONSUMER" service is healthy
    And The "OSD_SERVICE" service is healthy
    And The "PROCESSING_CONFIG_SERVICE" service is healthy
    And The "ETCD" service is healthy
    And The Soh Control Service is alive
    And The "OSD_RSDF_KAFKA_CONSUMER" service is healthy
    And The "INTERACTIVE_ANALYSIS_UI" service is healthy
    And The "INTERACTIVE_ANALYSIS_API_GATEWAY" service is healthy
    Then The SoH system is deployed





