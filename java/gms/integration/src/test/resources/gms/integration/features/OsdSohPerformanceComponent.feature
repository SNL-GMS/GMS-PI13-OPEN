@integration @component @soh @kafka @osd-consumers @ignore
Feature: We want to verify that the OSD Kafka consumers can consume messages at an acceptable rate
  given bursts of data, irregularly metered data flow. In the future, we may be interested in what
  happens should a consumer lose the ability to connect to postgres.

  Background:
    Given The environment is started with
    | OSD_STATION_SOH_KAFKA_CONSUMER |
    | POSTGRES_SERVICE               |
    | ZOOKEEPER                      |
    | KAFKA_ONE                      |
    | OSD_SERVICE                    |
    | PROCESSING_CONFIG_SERVICE      |
    | CONFIG_LOADER                  |
    | OSD_RSDF_KAFKA_CONSUMER        |
    | ETCD                           |
    | ACEI_MERGE_PROCESSOR           |
    Given Zookeeper is up and running
    Given Kafka is up and running
    And all the topics have been created
    Given The postgres service is healthy
    Given The "OSD_RSDF_KAFKA_CONSUMER" service is healthy
    Given The "OSD_STATION_SOH_KAFKA_CONSUMER" service is healthy
    Given The "ACEI_MERGE_PROCESSOR" service is healthy

  Scenario Outline: Verify that you can consume x batches of messages containing y messages on
  topic z at an acceptable rate and the consumers do not fail.
    When I connect to the database via the OSD interface
    And I send <batchCount> batches of <messages> messages of type <type> every <interval> on the <topic> topic using <relativeFilePath> to create messages to be stored in <osd_table_type>
    Then I can verify <batchCount> batches of <messages> messages were received on <topic> and stored in <osd_table_type>, and none of the services crashed during the transmission

    Examples:
      | batchCount | messages   | type                           | interval         | topic             | relativeFilePath                                              | osd_table_type           |
      | 10         | 30         | "RAW_STATION_DATA_FRAME_ID"    | "PT1.0S"         | "soh.rsdf"        | "gms/integration/data/LBTB-RSDF.json"                         | "RAW_STATION_DATA_FRAME" |
 #     | 10         | 15         | "STATION_SOH_ID"               | "PT1.0S"         | "soh.station-soh" | "gms/integration/data/BOSAStationSoh.json"                              | "STATION_SOH"            |
      | 1          | 4800       | "ACEI_ID"                      | "PT1.0S"         | "soh.acei"        | "gms/integration/data/soh_acei_boolean.json"                  | "ACEI_BOOLEAN"           |
      | 1          | 600        | "ACEI_ID"                      | "PT1.0S"         | "soh.acei"        | "gms/integration/data/soh_acei_analog.json"                   | "ACEI_ANALOG"            |
