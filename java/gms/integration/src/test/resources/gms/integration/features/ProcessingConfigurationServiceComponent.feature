@integration @component @smoke @soh @osd @frameworks-configuration-service
Feature: Verify that the framework Processing Configuration service is up and ready and configuration can be retrieved
  This is to validate that the service can be deployed is functional

  Background:
    Given The environment is started with
    | PROCESSING_CONFIG_SERVICE |
    | POSTGRES_SERVICE          |
    | ETCD                      |
    
  Scenario: Verify the Framework Processing Configuration Service can store and retrieve data
    Given The Framework Processing Configuration Service is alive
    When Processing Configuration has been loaded
    And I request a configuration with the Configuration Name as the request body
    Then I should retrieve the correct configuration