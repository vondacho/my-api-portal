package io.obya.api.onboarding.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.appl.usecase.RegistrationService;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Contract;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.semver4j.Semver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cucumber glue exercising {@link RegistrationService} directly (no HTTP layer).  The
 * real processing pipeline (reception, parsing, scoring, overlaying) runs against the
 * spec fixtures while the {@link Registry} out-port is replaced by a Mockito mock so the
 * scenarios stay self-contained.
 */
@CucumberContextConfiguration
@SpringBootTest
public class RegistrationServiceSteps {

    @Autowired
    RegistrationService registrationService;

    @MockitoBean
    Registry registry;

    private Try<State> result;

    @Before
    public void resetRegistry() {
        // Default: no prior version, registration always succeeds.
        when(registry.register(any())).thenReturn(Try.success(new SpecificationId("generated-id")));
        result = null;
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("the registry holds no prior specification")
    public void theRegistryHoldsNoPriorSpecification() {
        when(registry.infoAt(any(SpecificationId.class))).thenReturn(new Try.Failure<>(List.of()));
        when(registry.infoAt(any(String.class), any(String.class), any(Semver.class)))
                .thenReturn(new Try.Failure<>(List.of()));
    }

    @Given("the registry cannot register specifications")
    public void theRegistryCannotRegisterSpecifications() {
        when(registry.register(any())).thenReturn(new Try.Failure<>(List.of()));
    }

    @Given("the registry already contains specification {string} for product {string} at version {string}")
    public void theRegistryAlreadyContains(String apiName, String product, String version) {
        Specification existing = new Specification(
                new Info(apiName, "An existing specification.", Semver.parse(version)),
                Contract.from(Contract.Version.OPENAPI_V30),
                new Metadata(apiName, apiName, product, null, null),
                Scorecard.undefined(),
                "",
                List.of(),
                new SpecificationId("mock-id"));
        when(registry.infoAt(any(SpecificationId.class))).thenReturn(Try.success(existing));
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("the candidate {string} is submitted")
    public void theCandidateIsSubmitted(String resource) throws Exception {
        result = registrationService.submit(uriOf(resource));
    }

    @When("the specification {string} is upgraded with candidate {string}")
    public void theSpecificationIsUpgradedWith(String id, String resource) throws Exception {
        result = registrationService.upgrade(new SpecificationId(id), uriOf(resource));
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("the onboarding succeeds")
    public void theOnboardingSucceeds() {
        assertThat(result.isFailure())
                .as("onboarding result should not be a failure")
                .isFalse();
    }

    @Then("the onboarding fails")
    public void theOnboardingFails() {
        assertThat(result.isFailure())
                .as("onboarding result should be a failure")
                .isTrue();
    }

    @Then("the specification status is {string}")
    public void theSpecificationStatusIs(String status) {
        assertThat(result.getOrThrow().status()).isEqualTo(Status.valueOf(status));
    }

    @Then("the specification status is not {string}")
    public void theSpecificationStatusIsNot(String status) {
        assertThat(result.getOrThrow().status()).isNotEqualTo(Status.valueOf(status));
    }

    @Then("the specification version is {string}")
    public void theSpecificationVersionIs(String version) {
        assertThat(result.getOrThrow().info().version().getVersion()).isEqualTo(version);
    }

    @Then("the registered API name is {string}")
    public void theRegisteredApiNameIs(String apiName) {
        assertThat(result.getOrThrow().metadata().apiName()).isEqualTo(apiName);
    }

    @Then("the contract version is {string}")
    public void theContractVersionIs(String contract) {
        assertThat(result.getOrThrow().contract().version())
                .isEqualTo(Contract.Version.valueOf(contract));
    }

    @Then("no specification id is assigned")
    public void noSpecificationIdIsAssigned() {
        assertThat(result.getOrThrow().id()).isNull();
    }

    @Then("a violation with code {string} is reported")
    public void aViolationWithCodeIsReported(String code) {
        assertThat(Violation.from(result.getExceptions()))
                .extracting(Violation::code)
                .contains(Violation.Code.valueOf(code));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private URI uriOf(String classpathResource) throws Exception {
        return getClass().getClassLoader().getResource(classpathResource).toURI();
    }

}
