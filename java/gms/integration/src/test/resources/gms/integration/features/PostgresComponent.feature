@integration @system @component @osd @db @end2end @soh @smoke
Feature: Verify that postgres db is up and ready for SoH
  This is to validate that the services can be deployed are are all functional

Background:
  Given The environment is started with
  | POSTGRES_SERVICE |
  Given The postgres service is healthy

  Scenario: Verify postgres db has SoH schema prepared
    When I query the database schema objects
