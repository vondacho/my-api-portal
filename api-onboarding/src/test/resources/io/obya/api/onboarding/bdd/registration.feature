Feature: API specification onboarding
  As the API portal
  I want to register and upgrade API specifications through the RegistrationService
  So that only valid, scored specifications enter the registry

  Background:
    Given the registry holds no prior specification

  Scenario: A valid OpenAPI specification is registered
    When the candidate "e2e/openapi_v30_petstore.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the contract version is "OPENAPI_V30"
    And the registered API name is "petstore"

  Scenario: A valid AsyncAPI specification is registered
    When the candidate "e2e/asyncapi_v30_notification.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the contract version is "ASYNCAPI_V30"
    And the registered API name is "notification"

  Scenario: A specification without custom metadata cannot be onboarded
    When the candidate "e2e/openapi_v30_no_metadata.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MISSING_DATA" is reported

  Scenario: A valid specification is not registered when the registry rejects it
    Given the registry cannot register specifications
    When the candidate "e2e/openapi_v30_petstore.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "SCORED"
    And no specification id is assigned

  Scenario: Re-submitting the same version of an existing specification auto-increments the patch
    Given the registry already contains specification "petstore" for product "platform" at version "1.0.0"
    When the specification "mock-id" is upgraded with candidate "e2e/openapi_v30_petstore.yaml"
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification version is "1.0.1"
    And a violation with code "VERSION_AUTO_INCREMENTED" is reported
