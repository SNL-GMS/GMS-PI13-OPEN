@data-burst @component @integration @soh
Feature: Verify that the system can handle data bursts and continues to run

  Background:
    Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | POSTGRES_SERVICE          |
    | POSTGRES_EXPORTER         |
    | RSDF_STREAMS_PROCESSOR    |
    | CONFIG_LOADER             |
    | ZOOKEEPER                 |
    | OSD_SERVICE               |
    | KAFKA_ONE                 |
    | ETCD                      |
    Given Zookeeper is up and running
    Given Kafka is up and running
    And all the topics have been created
    Given The postgres service is healthy
    Given The Framework OSD Service container is created
    Given The RSDF streams processor is healthy

  Scenario Outline: Verify that you can run the system sending x messages on topic y at interval z,
  and the system does not crash. The acceptable thresholds for lag and latency are a function of the
  average lag and latency recorded when the first batch is processed and the lag tolerance percentage
  and latency tolerance percentage. The first batch is only used to establish a baseline; it is not
  factored into the final averages. Batch configurations are delimited by colons. Each batch
  configuration is delimited by commas and has the format batch size, batch count, interval.

    When I send <batchConfiguration> of <messageType> messages on the <sourceTopic> topic, receiving on the <destinationTopic> topic, using <relativeFilePath> to create messages with <lagTolerancePercentage> and <latencyTolerancePercentage>
    Then I can verify, using a specific <batchConfiguration>, that all messages were received on the <destinationTopic>, and none of the services crashed during the transmission

    Examples:
      | batchConfiguration                                                   | messageType                 | sourceTopic | destinationTopic | lagTolerancePercentage | latencyTolerancePercentage | relativeFilePath                      |
      | "100,5,PT1.0S:500,5,PT0.05S:100,5,PT0.25S:500,1,PT2.0S:250,2,PT0.5S" | "RAW_STATION_DATA_FRAME_ID" | "soh.rsdf"  | "soh.extract"    | 300.0                  | 100.0                      | "gms/integration/data/LBTB-RSDF.json" |
