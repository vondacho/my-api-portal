package io.obya.api.onboarding.at;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static io.obya.api.onboarding.domain.model.Violation.Code.DEPENDENCY_NOT_AVAILABLE;
import static java.util.Objects.nonNull;
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

    private static final String REGISTERED_ID = "spec-123";

    @Autowired
    RegistrationService registrationService;

    @MockitoBean
    Supplier<LocalDateTime> auditTimestampProvider;

    @MockitoBean
    Registry registry;

    @MockitoBean
    ScorerDelegate scorer;

    private Try<State> result;

    @Before
    public void resetMocks() {
        // Default: registration always succeeds and yields a stable document id.
        when(registry.register(any(Specification.class))).thenAnswer(invocation -> {
            Specification specification = invocation.getArgument(0);
            return Try.success(nonNull(specification.id()) ? specification.id() : new SpecificationId(REGISTERED_ID));
        });
        // Default: scoring succeeds with a good grade.  The fixture flagged as
        // "under_minimal_scoring_threshold" is scored below the acceptable threshold so the
        // corresponding scenario needs no extra Given.
        when(scorer.score(any(URI.class), any(Contract.class))).thenAnswer(invocation -> {
            URI source = invocation.getArgument(0);
            return Try.success(scorecardFor(source.toString()));
        });
        when(scorer.score(anyString(), any(Contract.class))).thenReturn(Try.success(scorecardFor("test")));
        result = null;
    }

    private static Scorecard scorecardFor(String source) {
        int global = source.contains("under_minimal_scoring_threshold") ? 10 : 74;
        return new Scorecard(new Score(global), Map.of(Scorecard.Dimension.FC, new Score(99)));
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("the registry holds {specId} named {string} in {string} at {version} {revision}")
    public void theRegistryHoldsSpecificationFor(SpecificationId id, String apiName, String productName, Version version, Revision revision) throws Exception {
        final Specification specification = new Specification(
                new Info("test", "test", version),
                Contract.from(Contract.Version.OPENAPI_V30),
                new Metadata(
                        apiName,
                        revision,
                        "test",
                        productName, null, null),
                Scorecard.undefined(),
                bodyOf(uriOf("e2e/openapi_v30_petstore_v1.yaml")),
                List.of(),
                id);

        when(registry.at(id)).thenReturn(Try.success(specification));
        when(registry.at(apiName, productName, version)).thenReturn(Try.success(specification));
        when(registry.at(apiName, productName, version, revision)).thenReturn(Try.success(specification));
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

    @When("the candidate {string} is submitted on {localDate}")
    public void theCandidateIsSubmittedOn(String resource, LocalDate when) throws Exception {
        when(auditTimestampProvider.get()).thenReturn(when.atStartOfDay());
        result = registrationService.submit(uriOf(resource));
    }

    @When("scoring {specId} on {localDate}")
    public void scoringSpecificationOn(SpecificationId id, LocalDate when) {
        when(auditTimestampProvider.get()).thenReturn(when.atStartOfDay());
        result = registrationService.score(id);
    }

    @When("applying overlay {string} to {specId} on {localDate}")
    public void applyingOverlayToSpecificationOn(String resource, SpecificationId id, LocalDate when) throws Exception {
        when(auditTimestampProvider.get()).thenReturn(when.atStartOfDay());
        result = registrationService.overlay(id, uriOf(resource));
    }

    @When("associating {specId} with component {string} {revision} on {localDate}")
    public void associatingComponentWithSpecificationOn(SpecificationId id, String componentName, Revision componentRevision, LocalDate when) {
        when(auditTimestampProvider.get()).thenReturn(when.atStartOfDay());
        result = registrationService.implement(id, new Implementation(componentName, componentRevision));
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

    @Then("the scoring succeeds")
    public void theScoringSucceeds() {
        assertThat(result.isFailure())
                .as("scoring result should not be a failure")
                .isFalse();
    }

    @Then("the implementation succeeds")
    public void theImplementationSucceeds() {
        assertThat(result.isFailure())
                .as("implementing result should not be a failure")
                .isFalse();
    }

    @Then("the overlaying succeeds")
    public void theOverlayingSucceeds() {
        assertThat(result.isFailure())
                .as("overlaying result should not be a failure")
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

    @Then("the specification status is {status}")
    public void theSpecificationStatusIs(Status status) {
        assertThat(result.getOrThrow().status()).isEqualTo(status);
    }

    @Then("the specification id is {specId}")
    public void theSpecificationIdIs(SpecificationId id) {
        assertThat(result.getOrThrow().id()).isNotNull();
        assertThat(result.getOrThrow().id()).isEqualTo(id);
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

    @Then("the specification version is {version}")
    public void theSpecificationVersionIs(Version version) {
        assertThat(result.getOrThrow().info().version()).isEqualTo(version);
    }

    @Then("the specification revision is {revision}")
    public void theRevisionIs(Revision revision) {
        assertThat(result.getOrThrow().metadata().apiRevision()).isEqualTo(revision);
    }

    @And("the audit contains {string} on {localDate}")
    public void theAuditContainsReasonOn(String reason, LocalDate when) {
        //throw new PendingException();
    }

    @Then("the contract is {contract}")
    public void theContractVersionIs(Contract.Version contract) {
        assertThat(result.getOrThrow().contract().version()).isEqualTo(contract);
    }

    @Then("the specification body contains {string}")
    public void theSpecificationBodyContains(String fragment) {
        assertThat(result.getOrThrow().body().get()).contains(fragment);
    }

    @Then("the specification body matches {string}")
    public void theSpecificationBodyMatches(String regex) {
        assertThat(result.getOrThrow().body().get()).matches(Pattern.compile(regex));
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

    @Then("registered {specId} named {string} in {string} at {version} {revision}")
    public void theRegistryContainsSpecificationFor(SpecificationId id, String apiName, String productName, Version version, Revision revision) {
        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(registry).register(captor.capture());
        Specification persisted = captor.getValue();
        if (id.id().equals(REGISTERED_ID)) {
            assertThat(persisted.id()).isNull();
        } else {
            assertThat(persisted.id()).isEqualTo(id);
        }
        assertThat(persisted.info().version()).isEqualTo(version);
        assertThat(persisted.metadata().apiName()).isEqualTo(apiName);
        assertThat(persisted.metadata().productName()).isEqualTo(productName);
        assertThat(persisted.metadata().apiRevision()).isEqualTo(revision);
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

    @Then("a violation {violation} is reported")
    public void aViolationWithCodeIsReported(Violation.Code code) {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MAJOR))
                .extracting(Violation::code)
                .contains(code);
    }

    @Then("no warning reported")
    public void noWarningReported() {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MINOR))
                .isEmpty();
    }

    @Then("a warning {violation} is reported")
    public void aWarningWithCodeIsReported(Violation.Code code) {
        assertThat(Violation.from(result.getExceptions())
                .stream().filter(v -> v.severity() == Violation.Severity.MINOR))
                .extracting(Violation::code)
                .contains(code);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private URI uriOf(String classpathResource) throws Exception {
        return Objects.requireNonNull(getClass().getClassLoader().getResource(classpathResource)).toURI();
    }

    private String bodyOf(URI uri) throws Exception {
        return Files.readString(Paths.get(uri));
    }

}
