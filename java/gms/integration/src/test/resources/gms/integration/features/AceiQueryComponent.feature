@integration @component @soh @soh-acei
Feature: We want to verify that retrieval of historical soh acei objects using station name,
  soh type and time range is working correctly.

Background:
  Given The environment is started with
    | POSTGRES_SERVICE          |
    | CONFIG_LOADER             |
    | PROCESSING_CONFIG_SERVICE |
    | OSD_SERVICE               |
    | ETCD                      |
  Given The postgres service is healthy

Scenario: Verify that you can query acei boolean and analog with station name, acei soh type, and time range in the database succeeds.
  When I connect to the database via the OSD interface
  Given Referenced acei analog and boolean objects are stored in the osd
  Then Querying for the referenced acei objects using station, time range and soh type returns acei list
