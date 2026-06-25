package io.obya.api.onboarding.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.appl.out.ScorerDelegate;
import io.obya.api.onboarding.appl.usecase.RegistrationService;
import io.obya.api.onboarding.domain.model.*;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;
import org.mockito.ArgumentCaptor;
import org.semver4j.Semver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.obya.api.onboarding.domain.model.Violation.Code.DEPENDENCY_NOT_AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cucumber glue exercising {@link RegistrationService} directly (no HTTP layer).  The
 * real processing pipeline (reception, parsing, scoring, overlaying) runs against the
 * spec fixtures while the {@link Registry} and {@link ScorerDelegate} out-ports are
 * replaced by Mockito mocks so the scenarios stay self-contained and deterministic.
 */
@CucumberContextConfiguration
@SpringBootTest
public class RegistrationServiceSteps {

    private static final String REGISTERED_ID = "mock-id";

    @Autowired
    RegistrationService registrationService;

    @MockitoBean
    Registry registry;

    @MockitoBean
    ScorerDelegate scorer;

    private Try<State> result;

    @Before
    public void resetMocks() {
        // Default: registration always succeeds and yields a stable document id.
        when(registry.register(any(Specification.class))).thenReturn(Try.success(new SpecificationId(REGISTERED_ID)));
        // Default: scoring succeeds with a good grade.  The fixture flagged as
        // "under_minimal_scoring_threshold" is scored below the acceptable threshold so the
        // corresponding scenario needs no extra Given.
        when(scorer.score(any(URI.class), any(Contract.class))).thenAnswer(invocation -> {
            URI source = invocation.getArgument(0);
            return Try.success(scorecardFor(source.toString()));
        });
        when(scorer.score(anyString(), any(Contract.class))).thenAnswer(invocation -> {
            String source = invocation.getArgument(0);
            return Try.success(scorecardFor(source));
        });
        result = null;
    }

    private static Scorecard scorecardFor(String source) {
        int global = source.contains("under_minimal_scoring_threshold") ? 10 : 74;
        return new Scorecard(new Score(global), Map.of(Scorecard.Dimension.FC, new Score(99)));
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("the registry holds specification {string} for product {string} at version {string} with revision {string}")
    public void theRegistryHoldsSpecificationFor(String apiName, String productName, String version, String revision) {
        final Specification specification = new Specification(
                new Info("any", "any", Version.from(version)),
                Contract.from(Contract.Version.OPENAPI_V30),
                new Metadata(
                        apiName,
                        Revision.from(revision),
                        "any",
                        productName, null, null),
                Scorecard.undefined(),
                "any",
                List.of(),
                new SpecificationId(REGISTERED_ID));

        when(registry.at(apiName, productName, Version.from(version)))
                .thenReturn(Try.success(specification));
        when(registry.at(apiName, productName, Version.from(version), Revision.from(revision)))
                .thenReturn(Try.success(specification));
    }

    @Given("the registry holds no prior specification")
    public void theRegistryHoldsNoPriorSpecification() {
        when(registry.at(anyString(), anyString(), any(Version.class))).thenReturn(new Try.Failure<>(List.of()));
    }

    @Given("the registry cannot register specifications")
    public void theRegistryCannotRegisterSpecifications() {
        when(registry.register(any(Specification.class))).thenReturn(new Try.Failure<>(
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("registry", "unavailable").get())));
    }

    @Given("the scorer cannot score specifications")
    public void theScorerCannotScoreSpecifications() {
        when(scorer.score(any(URI.class), any(Contract.class))).thenReturn(new Try.Failure<>(
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("scorer", "unavailable").get())));
        when(scorer.score(anyString(), any(Contract.class))).thenReturn(new Try.Failure<>(
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("scorer", "unavailable").get())));
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("the candidate {string} is submitted")
    public void theCandidateIsSubmitted(String resource) throws Exception {
        result = registrationService.submit(uriOf(resource));
    }

    // -------------------------------------------------------------------------
    // Then – outcome
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

    // -------------------------------------------------------------------------
    // Then – specification state
    // -------------------------------------------------------------------------

    @Then("the specification status is {string}")
    public void theSpecificationStatusIs(String status) {
        assertThat(result.getOrThrow().status()).isEqualTo(Status.valueOf(status));
    }

    @Then("the specification id is {string}")
    public void theSpecificationIdIs(String id) {
        assertThat(result.getOrThrow().id()).isNotNull();
        assertThat(result.getOrThrow().id().id()).isEqualTo(id);
    }

    @Then("a specification id is assigned")
    public void aSpecificationIdIsAssigned() {
        assertThat(result.getOrThrow().id()).isNotNull();
        assertThat(result.getOrThrow().id().id()).isNotBlank();
    }

    @Then("no specification id is assigned")
    public void noSpecificationIdIsAssigned() {
        assertThat(result.getOrThrow().id()).isNull();
    }

    @Then("the specification version is {string}")
    public void theSpecificationVersionIs(String version) {
        assertThat(result.getOrThrow().info().version()).isEqualTo(Version.from(version));
    }

    @Then("the revision is {string}")
    public void theRevisionIs(String revision) {
        assertThat(result.getOrThrow().metadata().apiRevision()).isEqualTo(Revision.from(revision));
    }

    @Then("the contract version is {string}")
    public void theContractVersionIs(String contract) {
        assertThat(result.getOrThrow().contract().version())
                .isEqualTo(Contract.Version.valueOf(contract));
    }

    @Then("the specification body contains {string}")
    public void theSpecificationBodyContains(String fragment) {
        assertThat(result.getOrThrow().body().get()).contains(fragment);
    }

    @Then("a scorecard is assigned with global score of {int}")
    public void aScorecardIsAssignedWithGlobalScore(Integer score) {
        Scorecard scorecard = result.getOrThrow().score();
        assertThat(scorecard).isNotNull();
        assertThat(scorecard.isUndefined())
                .as("a scorecard should be assigned")
                .isFalse();
        assertThat(scorecard.global().evaluation()).isEqualTo(score);
    }

    @Then("no scorecard is assigned")
    public void noScorecardIsAssigned() {
        Scorecard scorecard = result.getOrThrow().score();
        assertThat(scorecard == null || scorecard.isUndefined())
                .as("no scorecard should be assigned")
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Then – registry interaction
    // -------------------------------------------------------------------------

    @Then("the registry contains specification {string}")
    public void theRegistryContainsSpecification(String id) {
        verify(registry).register(any(Specification.class));
        assertThat(result.getOrThrow().id()).isEqualTo(new SpecificationId(id));
    }

    @Then("the registry contains specification {string} for product {string} at version {string}")
    public void theRegistryContainsSpecificationFor(String apiName, String productName, String version) {
        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(registry).register(captor.capture());
        Specification persisted = captor.getValue();
        assertThat(persisted.metadata().apiName()).isEqualTo(apiName);
        assertThat(persisted.metadata().productName()).isEqualTo(productName);
        assertThat(persisted.info().version()).isEqualTo(Version.from(version));
    }

    @Then("the registry contains specification {string} for product {string} at version {string} with revision {string}")
    public void theRegistryContainsSpecificationFor(String apiName, String productName, String version, String revision) {
        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(registry).register(captor.capture());
        Specification persisted = captor.getValue();
        assertThat(persisted.metadata().apiName()).isEqualTo(apiName);
        assertThat(persisted.metadata().productName()).isEqualTo(productName);
        assertThat(persisted.info().version()).isEqualTo(Version.from(version));
        assertThat(persisted.metadata().apiRevision()).isEqualTo(Revision.from(revision));
    }

    // -------------------------------------------------------------------------
    // Then – violations
    // -------------------------------------------------------------------------

    @Then("no violation reported")
    public void noViolationReported() {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MAJOR))
                .isEmpty();
    }

    @Then("a violation with code {string} is reported")
    public void aViolationWithCodeIsReported(String code) {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MAJOR))
                .extracting(Violation::code)
                .contains(Violation.Code.valueOf(code));
    }

    @Then("no warning reported")
    public void noWarningReported() {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MINOR))
                .isEmpty();
    }

    @Then("a warning with code {string} is reported")
    public void aWarningWithCodeIsReported(String code) {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MINOR))
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
