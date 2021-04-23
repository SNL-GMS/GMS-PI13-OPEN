@integration @component @soh @soh-storage
Feature: We want to verify that the OSD can store objects in Postgres at an acceptable rate.

  Background:
    Given The environment is started with
    | OSD_SERVICE                    |
    | POSTGRES_SERVICE               |
    | CONFIG_LOADER                  |
    | PROCESSING_CONFIG_SERVICE      |
    | ETCD                           |
    Given The postgres service is healthy

  Scenario Outline: Verify that you can store data in the DB at an acceptable rate
    When I connect to the database via the OSD interface
    And I create <num_messages> messages of type <type> using <relativeFilePath> to create messages to be stored in <osd_table_type> in specified duration on average
    Then I can verify <num_messages> messages were stored in duration <duration> in table <osd_table_type> and none of the services crashed during the test

    Examples:
      | num_messages | type                        | relativeFilePath                             | osd_table_type           | duration |
      | 30           | "RAW_STATION_DATA_FRAME_ID" | "gms/integration/data/LBTB-RSDF.json"        | "RAW_STATION_DATA_FRAME" | "PT1.0S" |
      | 15           | "STATION_SOH_ID"            | "gms/integration/data/BOSAStationSoh.json"   | "STATION_SOH"            | "PT1.0S" |
      | 480          | "ACEI_ID"                   | "gms/integration/data/soh_acei_boolean.json" | "ACEI_BOOLEAN"           | "PT0.1S" |
      | 600          | "ACEI_ID"                   | "gms/integration/data/soh_acei_analog.json"  | "ACEI_ANALOG"            | "PT1.0S" |
