@integration @component @soh @capability-soh-rollup
Feature: We want to verify that storage and retrieval of capability soh rollups is working correctly.

Background:
    Given The environment is started with
    | POSTGRES_SERVICE          |
    | CONFIG_LOADER             |
    | PROCESSING_CONFIG_SERVICE |
    | OSD_SERVICE               |
    | ETCD                      |
    Given The postgres service is healthy

    
Scenario: Storing a capability rollup soh with correct information that refers to station sohs and
station group in the database succeeds.
    When I connect to the database via the OSD interface
    Given Referenced station sohs are stored in the osd
    And Capability soh rollup with appropriate values in field stored in osd
    Then Querying for capability soh rollup using given station group returns the correct capability soh rollup

Scenario: Capability Rollup with station not in OSD is not stored.
    When I connect to the database via the OSD interface
    Given Referenced station sohs are stored in the osd
    And Attempted storage of valid capability soh rollup and capability soh rollup with non-existent "STATION"
    Then Querying for capability soh rollup using given station group returns the correct capability soh rollup

Scenario: Capability Rollup with station group not in OSD is not stored.
    When I connect to the database via the OSD interface
    Given Referenced station sohs are stored in the osd
    And Attempted storage of valid capability soh rollup and capability soh rollup with non-existent "STATIONGROUP"
    Then Querying for capability soh rollup using given station group returns the correct capability soh rollup
