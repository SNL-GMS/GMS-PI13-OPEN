@integration @component @dataman @soh
Feature: DataMan Application

  Tests the DataMan application consumes Cd11Frames from a data injector,
  processes them, and writes messages to the RSDF Kafka topic. The messages from the
  injector are Cd11Frame objects. For each, an RSDF object is written to the output topic as JSON.

Background:
  # Dataman is healthy and json files are mounted/readable
  Given the environment should restart between scenarios: "false"
  Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | CONFIG_LOADER             |
    | POSTGRES_SERVICE          |
    | OSD_SERVICE               |
    | ZOOKEEPER                 |
    | KAFKA_ONE                 |
    | DATAMAN                   |
    | ETCD                      |
    | OSD_SERVICE               |
  Given Configuration for "DATAMAN" component test is loaded
  Given The "DATAMAN" service has been restarted
  Given The "DATAMAN" service is healthy

#   DataMan is receiving the correct frames and publishing RSDFs
  Scenario: DataMan is receiving and publishing data
    Given appropriate json files are mounted and readable from the "gms/integration/requests/dataacquisition/dataman/" directory
    And an input "LBTB-RSDF.json" frame resource file contains JSON versions of "RawStationDataFrame" objects
    And the "RawStationDataFrame" object is converted to a "Cd11DataFrame" object
    And a Cd11FrameFactory for station "LBTB"
    And an unused dataman connection
    And the dataman socket is connected and sends a list of channel subframes
    Then within a period of 120 seconds expected "RawStationDataFrame" message is readable from the kafka topic "soh.rsdf"

   Scenario: DataMan is receiving and responding to ACKNACK frames
     Given a Cd11FrameFactory for station "LBTB"
     And an unused dataman connection
     When the dataman socket is connected and sends a ACKNACK request for frame set "STA12345678901234567"
     Then an ACKNACK response is received for frame set "STA12345678901234567"

   Scenario: DataMan is receiving and responding to Alert frames
     Given a Cd11FrameFactory for station "LBTB"
     And an unused dataman connection
     When the dataman socket is connected and sends a Alert request
     Then the dataman socket was disconnected
     And the dataman socket was sent a "Alert" message

    Scenario: DataMan is receiving Option Frame Requests
      Given a Cd11FrameFactory for station "LBTB"
      And an unused dataman connection
      When the dataman socket is connected and sends an Option request
      Then the dataman socket was sent a "Option Request" message
      And an OPTION response is received from dataman


   Scenario: DataMan create gap list and reset it using AcknackFrame
     Given a Cd11FrameFactory for station "LBTB"
     And an unused dataman connection
     When a CommandResponseFrame with sequence of 5 is sent to Dataman for station "LBTB"
     And a CommandResponseFrame with sequence of 55 is sent to Dataman for station "LBTB"
     And a gap list between 5 and 55 is reported from Dataman
     And sending an ACKNAK for frame set "STA12345678901234567" with highestSeqNum of 4 and no gaps
     Then dataman's gap list is reset for frame set "STA12345678901234567"

     Scenario: DataMan create gap list and reset it using CustomResetFrame
       Given a Cd11FrameFactory for station "LBTB"
       And an unused dataman connection
       When a CommandResponseFrame with sequence of 5 is sent to Dataman for station "LBTB"
       And a CommandResponseFrame with sequence of 55 is sent to Dataman for station "LBTB"
       And a gap list between 5 and 55 is reported from Dataman
       And a CustomResetFrame is sent
       Then the dataman socket was disconnected
       And dataman's gap list is reset for frame set "TEST:0"

     Scenario: DataMan is NOT crashed by unknown or unsupported CD11 data frames
       Given a Cd11FrameFactory for station "LBTB"
       And an unused dataman connection
       When an ACKNACK frame for frame set "STA12345678901234567" is sent to dataman after sending a MalformedFrame
       Then an ACKNACK response is received for frame set "STA12345678901234567"

 # Scenario: DataMan is NOT crashed by malformed data being sent
  #  Given a Cd11FrameFactory for station "LBTB"
  #  And an unused dataman connection
   # When an ACKNACK frame for frame set "STA12345678901234567" is sent to dataman after sending malformed data
   # Then an ACKNACK response is received for frame set "STA12345678901234567"

  Scenario Outline: DataMan is receiving NO-OP frames
    Given a Cd11FrameFactory for station "LBTB"
    And an unused dataman connection
    When the dataman socket is connected and sends a <frame_type>
    Then the dataman socket was sent a <frame_type> message
    And an ACKNACK response is received for frame set "TEST:0"


       Examples:
         |  frame_type             |
         |  "CD ONE ENCAPSULATION" |
         |  "COMMAND REQUEST"      |
         |  "CONNECTION REQUEST"   |
         |  "CONNECTION RESPONSE"  |
         |  "OPTION RESPONSE"      |
