Feature: API specification onboarding
  As the API portal
  I want to register and upgrade API specifications through the RegistrationService
  So that only valid and scored specifications enter the registry

  Background:
    Given the registry holds no prior specification

  Scenario: A valid and scored OpenAPI candidate is registered
    When the candidate "e2e/openapi_v30_petstore_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification id is "mock-id"
    And the specification version is "v1"
    And the contract version is "OPENAPI_V30"
    And a scorecard is assigned with global score of 74
    And the specification status is "REGISTERED"
    And the specification body contains "x-score"
    And the registry contains specification "mock-id"
    And the registry contains specification "petstore" for product "platform" at version "v1"

  Scenario: A valid and scored AsyncAPI candidate is registered
    When the candidate "e2e/asyncapi_v30_notification_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification id is "mock-id"
    And the specification version is "v1"
    And the contract version is "ASYNCAPI_V30"
    And a scorecard is assigned with global score of 74
    And the registry contains specification "mock-id"
    And the registry contains specification "notification" for product "platform" at version "v1"

  Scenario: A candidate missing required info is not onboarded
    When the candidate "e2e/openapi_v30_missing_required_info.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MISSING_DATA" is reported

  Scenario: A candidate having a malformed version is not onboarded
    When the candidate "e2e/openapi_v30_malformed_version.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MALFORMED_VERSION" is reported

  Scenario: A candidate missing required metadata is not onboarded
    When the candidate "e2e/openapi_v30_missing_required_metadata.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MISSING_DATA" is reported

  Scenario: A candidate having a malformed revision is not onboarded
    When the candidate "e2e/openapi_v30_malformed_revision.yaml" is submitted
    Then the onboarding fails
    And a violation with code "MALFORMED_REVISION" is reported

  Scenario: A candidate missing non-mandatory info is onboarded
    When the candidate "e2e/openapi_v30_missing_non_required_info.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And no violation reported

  Scenario: A candidate missing non-mandatory metadata is onboarded
    When the candidate "e2e/openapi_v30_missing_non_required_metadata.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And no violation reported

  Scenario: A valid candidate scored under the minimal threshold is not onboarded
    When the candidate "e2e/openapi_v30_under_minimal_scoring_threshold.yaml" is submitted
    Then the onboarding fails
    And a violation with code "INSUFFICIENT_SCORING" is reported

  Scenario: A valid candidate is not registered when the scorer is not available
    Given the scorer cannot score specifications
    When the candidate "e2e/openapi_v30_petstore_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "VALID"
    And no specification id is assigned
    And no scorecard is assigned
    And a warning with code "DEPENDENCY_NOT_AVAILABLE" is reported

  Scenario: A valid candidate is not registered when the registry is not available
    Given the registry cannot register specifications
    When the candidate "e2e/openapi_v30_petstore_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "SCORED"
    And no specification id is assigned
    And a warning with code "DEPENDENCY_NOT_AVAILABLE" is reported

  Scenario: An onboarded candidate gets a revision
    When the candidate "e2e/openapi_v30_petstore_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification id is "mock-id"
    And the revision is "1.0.0"
    And the registry contains specification "petstore" for product "platform" at version "v1" with revision "1.0.0"

  Scenario: An update on a registered specification creates a new revision
    Given the registry holds specification "petstore" for product "platform" at version "v1" with revision "1.0.0"
    When the candidate "e2e/openapi_v30_petstore_v1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification id is "mock-id"
    And the specification version is "v1"
    And the revision is "1.0.1"
    And the registry contains specification "mock-id"
    And the registry contains specification "petstore" for product "platform" at version "v1" with revision "1.0.1"

  Scenario: A candidate having a next revision is onboarded
    Given the registry holds specification "petstore" for product "platform" at version "v1" with revision "1.0.0"
    When the candidate "e2e/openapi_v30_petstore_v1_1_0_1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification version is "v1"
    And the revision is "1.0.1"
    And the registry contains specification "petstore" for product "platform" at version "v1" with revision "1.0.1"

  Scenario: A candidate having a previous revision is onboarded
    Given the registry holds specification "petstore" for product "platform" at version "v1" with revision "1.0.1"
    When the candidate "e2e/openapi_v30_petstore_v1_1_0_0.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification version is "v1"
    And the revision is "1.0.2"
    And the registry contains specification "petstore" for product "platform" at version "v1" with revision "1.0.2"
    And a warning with code "REVISION_AUTO_INCREMENTED" is reported

  Scenario: A candidate having a disaligned revision is onboarded
    When the candidate "e2e/openapi_v30_petstore_v1_0_0_1.yaml" is submitted
    Then the onboarding succeeds
    And the specification status is "REGISTERED"
    And the specification version is "v1"
    And the revision is "1.0.0"
    And the registry contains specification "petstore" for product "platform" at version "v1" with revision "1.0.0"
    And a warning with code "REVISION_NOT_ALIGNED" is reported

