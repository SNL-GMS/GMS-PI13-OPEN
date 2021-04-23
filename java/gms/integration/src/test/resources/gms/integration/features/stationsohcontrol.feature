@feature @F-07.03.09 @soh-control @integration @soh
Feature: Monitor Station SOH Application

  Tests the Monitor Station SOH application consumes messages from a Kafka
  topic, processes them, and writes messages to another Kafka topic. The messages from the
  input topic are JSON versions of AcquiredStationSohExtract objects. For each, a list of
  StationSoh objects are written to the output topic as JSON.

  Background:
    Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | CONFIG_LOADER             |
    | POSTGRES_SERVICE          |
    | ZOOKEEPER                 |
    | OSD_SERVICE               |
    | SOH_CONTROL               |
    | KAFKA_ONE                 |
    | ETCD                      |
  Scenario: SOH is Monitored
    Given The Soh Control Service is alive
    And an input "ten_soh_extracts.json" resource contains JSON versions of "AcquiredStationSohExtract" objects
    And the "AcquiredStationSohExtract" objects are written to the kafka topic "soh.extract"
    And within a period of 120 seconds "StationSoh" messages are read from the kafka topic "soh.station-soh"
    And within a period of 120 seconds "CapabilitySohRollup" messages are read from the kafka topic "soh.capability-rollup"
    # These next 2 steps simply detect whether data formats have changed since the resources were generated.
    And an input "station_soh.json" resource contains JSON versions of "StationSoh" objects
    And an input "capability_soh_rollup.json" resource contains JSON versions of "CapabilitySohRollup" objects
    Then the formats have not changed

