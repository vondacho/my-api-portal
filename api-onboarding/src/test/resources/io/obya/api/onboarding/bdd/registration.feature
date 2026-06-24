Feature: API specification onboarding
  As the API portal
  I want to register and upgrade API specifications through the RegistrationService
  So that only valid and scored specifications enter the registry

  Background:
    Given the registry holds no prior specification

  Scenario: A valid and scored OpenAPI specification is registered
    When the candidate "e2e/openapi_v30_petstore_v1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification id is "mock-id"
    And the contract version is "OPENAPI_V30"
    And a scorecard is assigned with global score of 74
    And the specification body contains "x-score: 74"
    And the specification status is "REGISTERED"
    And the registry contains specification "mock-id"
    And the registry contains specification "petstore" for product "platform" at version "1.0.0"

  Scenario: A valid and scored AsyncAPI specification is registered
    When the candidate "e2e/asyncapi_v30_notification_v1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification id is "mock-id"
    And the contract version is "ASYNCAPI_V30"
    And a scorecard is assigned with global score of 74
    And the registry contains specification "mock-id"
    And the registry contains specification "notification" for product "platform" at version "1.0.0"

  Scenario: A specification missing required info is not onboarded
    When the candidate "e2e/openapi_v30_missing_required_info.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MISSING_DATA" is reported

  Scenario: A specification missing required metadata is not onboarded
    When the candidate "e2e/openapi_v30_missing_required_metadata.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MISSING_DATA" is reported

  Scenario: A specification missing non-mandatory info is onboarded
    When the candidate "e2e/openapi_v30_missing_non_required_info.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And no violation reported

  Scenario: A specification missing non-mandatory metadata is onboarded
    When the candidate "e2e/openapi_v30_missing_non_required_metadata.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And no violation reported

  Scenario: A valid specification scored under the minimal threshold is not onboarded
    When the candidate "e2e/openapi_v30_under_minimal_scoring_threshold.yaml" is submitted
    Then the onboarding fails
    And a violation with code "INSUFFICIENT_SCORING" is reported

  Scenario: A valid specification is not registered when the scorer is not available
    Given the scorer cannot score specifications
    When the candidate "e2e/openapi_v30_petstore_v1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "VALID"
    And no specification id is assigned
    And no scorecard is assigned
    And a warning with code "DEPENDENCY_NOT_AVAILABLE" is reported

  Scenario: A valid specification is not registered when the registry is not available
    Given the registry cannot register specifications
    When the candidate "e2e/openapi_v30_petstore_v1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "SCORED"
    And no specification id is assigned
    And a warning with code "DEPENDENCY_NOT_AVAILABLE" is reported

  Scenario: A valid and scored OpenAPI specification upgrades a registered specification
    Given the registry contains specification "petstore" for product "platform" with latest version "1.0.0"
    When the candidate "e2e/openapi_v30_petstore_v1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification id is "mock-id"
    And the specification version is "1.0.1"
    And a scorecard is assigned with global score of 74
    And a warning with code "VERSION_AUTO_INCREMENTED" is reported
    And the registry contains specification "mock-id"
    And the registry contains specification "petstore" for product "platform" at version "1.0.1"
