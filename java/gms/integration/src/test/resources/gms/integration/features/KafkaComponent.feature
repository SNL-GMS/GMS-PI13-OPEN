@integration @system @component @kafka @end2end @soh @smoke

Feature: Testing Kafka is available and configured properly

Background:
  Given The environment is started with
  | ZOOKEEPER     |
  | KAFKA_ONE     |
  Given Kafka is up and running
  And all the topics have been created

Scenario Outline: Verify that different topics are available for sending and receiving of messages
      When I am able to send a "<message-type>" in "<file-path>" to the "<topic-name>" topic
      Then I am able to receive the same "<message-type>" in "<file-path>" from the "<topic-name>" topic

      Examples:
        |              message-type |                             file-path |  topic-name |
        |       RawStationDataFrame |   gms/integration/data/LBTB-RSDF.json |    soh.rsdf |
        | AcquiredStationSohExtract | gms/integration/data/soh_extract.json | soh.extract |