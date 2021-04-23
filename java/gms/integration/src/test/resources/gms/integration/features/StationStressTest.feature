@integration @system @component @end2end @stress @soh @smoke @ignore
Feature: Verify that the system can handle a given number of stations for a given time
This feature validates that the back-end services, for a given number of stations and a period of time,
correctly compute the latency and handle messages with no lag in any of the kafka topics.

Background:
  Given The environment is started with
    | OSD_STATION_SOH_KAFKA_CONSUMER      |
    | POSTGRES_SERVICE                    |
    | ZOOKEEPER                           |
    | KAFKA_ONE                           |
    | CONFIG_LOADER                       |
    | OSD_SERVICE                         |
    | PROCESSING_CONFIG_SERVICE           |
    | OSD_RSDF_KAFKA_CONSUMER             |
    | CONNMAN                             |
    | DATAMAN                             |
    | ETCD                                |
  Given Zookeeper is up and running
  Given Kafka is up and running
  And all the topics have been created
  Given The postgres service is healthy
  And Postgres db has bootstrap data loaded
  Given The Framework OSD Service container is created
  Given The connman service is healthy
  Given The dataman service is healthy
  Given The RSDF stream processor is healthy
  Given The Soh Control Service is alive
  And appropriate stations and station groups are stored in and retrievable from the OSD

Scenario Outline: Verify that you can run the system for x stations for y minutes, the system does not crash
  When I run <num-stations> stations for <duration> minutes
  Then I can verify that there is less than <num-messages> lag in the <kafka-topic-name> topic
  And None of the services crashed during the transmission

  Examples:
    | num-stations | duration | num-messages | kafka-topic-name |
    |            1 |       15 |          100 |         soh.rsdf |
    |            1 |       15 |          100 |      soh.extract |
    |            1 |       15 |          100 |  soh.station-soh |
