@ignore @Feature @F-01.01.27 @cd11-rsdf-processor @integration @soh
Feature: Cd11 Rsdf Processor Kafka Streams Application

  Tests that the Cd11 Rsdf Processor Streams application consumes messages from a Kafka
  topic, processes them, and writes messages to two Kafka topics. The messages from the
  input topic are JSON versions of RSDF objects. For each object, a list of ACEI objects and
  a single Extract object is written to the two output topics as JSON.

#  deploymentCtxt().withServices(GmsServiceType.ETCD,
#  GmsServiceType.POSTGRES_SERVICE,
#  GmsServiceType.PROCESSING_CONFIG_SERVICE,
#  GmsServiceType.CONFIG_LOADER,
#  GmsServiceType.ZOOKEEPER,
#  GmsServiceType.KAFKA_ONE,
#  GmsServiceType.RSDF_STREAMS_PROCESSOR
#  ).  start();
  Background:
    Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | CONFIG_LOADER             |
    | ZOOKEEPER                 |
    | KAFKA_ONE                 |
    | RSDF_STREAMS_PROCESSOR    |
    | POSTGRES_SERVICE          |
    | ETCD                      |
    | OSD_SERVICE               |
    Given Configuration for "RSDF_STREAMS_PROCESSOR" component test is loaded
    Given The "RSDF_STREAMS_PROCESSOR" service is healthy
    And appropriate json files are mounted and readable from the "gms/integration/requests/dataacquisition/cd11/rsdf/processor/" directory

  Scenario:
    Given an input "soh-rsdf-raw.json" RSDF resource file contains JSON versions of "RawStationDataFrame" objects
    And the "RawStationDataFrame" object is written to the kafka topic "soh.rsdf"
    Then within a period of 120 seconds expected "AcquiredChannelEnvironmentIssue" and "AcquiredStationSohExtract" messages are readable from the kafka topics "soh.acei" and "soh.extract" respectively
