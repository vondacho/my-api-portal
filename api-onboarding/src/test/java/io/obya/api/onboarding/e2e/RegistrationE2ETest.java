package io.obya.api.onboarding.e2e;

import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.adapter.in.web.model.CandidateProcessed;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.domain.model.Contract;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * End-to-end tests covering the full onboarding pipeline from HTTP ingress through
 * parsing, scoring, and registration.  The Registry port is replaced by a Mockito
 * mock so the tests run without any external infrastructure (no Strapi, no dummy
 * profile).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RegistrationE2ETest {

    @MockitoBean
    Registry registry;

    @Autowired
    TestRestTemplate rest;

    @BeforeEach
    void setUpRegistry() {
        // No prior version found → first submission
        when(registry.infoAt(any(SpecificationId.class))).thenReturn(new Try.Failure<>(List.of()));
        when(registry.infoAt(any(String.class), any(String.class), any(Semver.class))).thenReturn(new Try.Failure<>(List.of()));
        // Happy path: registration always succeeds
        when(registry.register(any())).thenReturn(Try.success(new SpecificationId("mock-id")));
    }

    // -------------------------------------------------------------------------
    // Submit – happy paths
    // -------------------------------------------------------------------------

    @Test
    void submit_validOpenApiV30_registersSpecAndReturns201() throws Exception {
        URI specUri = specFileUri("e2e/openapi_v30_petstore.yaml");

        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertCandidateRegistered(specUri, response.getBody(), Semver.parse("1.0.0"));
    }

    @Test
    void submit_validAsyncApiV30_registersSpecAndReturns201() throws Exception {
        URI specUri = specFileUri("e2e/asyncapi_v30_notification.yaml");

        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CandidateProcessed body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(Status.REGISTERED);
        assertThat(body.contract()).isEqualTo(Contract.Version.ASYNCAPI_V30);
        assertThat(body.info().title()).isEqualTo("Notification API");
        assertThat(body.metadata().apiName()).isEqualTo("notification");
    }

    // -------------------------------------------------------------------------
    // Submit – validation / partial results
    // -------------------------------------------------------------------------

    @Test
    void submit_specMissingCustomMetadata_returnsProblemDetail() throws Exception {
        URI specUri = specFileUri("e2e/openapi_v30_no_metadata.yaml");

        ResponseEntity<ProblemDetail> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), ProblemDetail.class);

        // Without x-api-name / x-product-name the pipeline fails and the controller
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        assertBadRequestProblem(response.getBody());
    }

    @Test
    void submit_registryFails_returnsValidWithoutId() throws Exception {
        when(registry.register(any())).thenReturn(new Try.Failure<>(List.of()));

        URI specUri = specFileUri("e2e/openapi_v30_petstore.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertCandidateScored(specUri, response.getBody());
    }

    // -------------------------------------------------------------------------
    // Submit – error cases
    // -------------------------------------------------------------------------

    @Test
    void submit_nonExistentSourceUri_returnsError() {
        URI missing = URI.create("file:///non/existent/spec.yaml");

        ResponseEntity<ProblemDetail> response = rest.postForEntity(
                "/registrations", new Candidate(missing), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        assertBadRequestProblem(response.getBody());
    }

    @Test
    void submit_missingSourceField_returns4xx() {
        org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(
                "{}", org.springframework.http.HttpHeaders.EMPTY);

        ResponseEntity<ProblemDetail> response = rest.postForEntity("/registrations", request, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        assertUnprocessableEntityProblem(response.getBody());
    }

    // -------------------------------------------------------------------------
    // Upgrade – happy path
    // -------------------------------------------------------------------------

    @Test
    void upgrade_withExistingSpec_autoIncrementsVersionAndReturns201() throws Exception {
        Specification existing = new Specification(
                new Info("Petstore API", "A sample API.", Semver.parse("1.0.0")),
                Contract.from(Contract.Version.OPENAPI_V30),
                new Metadata("petstore", "petstore", "platform", null, null),
                Scorecard.undefined(),
                "",
                List.of(),
                new SpecificationId("mock-id"));

        // Registry returns the existing spec when the upgrade looks it up by id
        when(registry.infoAt(any(SpecificationId.class))).thenReturn(Try.success(existing));

        URI specUri = specFileUri("e2e/openapi_v30_petstore.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations/mock-id/upgrades", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertCandidateRegistered(specUri, response.getBody(), Semver.parse("1.0.1"));

        // Spec version 1.0.0 == existing 1.0.0 → auto-patch to 1.0.1, warning attached
        assertThat(response.getBody().violations())
                .isNotNull()
                .isNotEmpty()
                .anyMatch(v -> v.code() == Violation.Code.VERSION_AUTO_INCREMENTED);
    }

    @Test
    void upgrade_withUnknownId_returnsError() throws Exception {
        // specificationAt already returns Failure by default → currentVersion(id) fails
        URI specUri = specFileUri("e2e/openapi_v30_petstore.yaml");

        ResponseEntity<ProblemDetail> response = rest.postForEntity(
                "/registrations/unknown-id/upgrades",
                new Candidate(specUri),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        assertBadRequestProblem(response.getBody());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private URI specFileUri(String classpathResource) throws Exception {
        return getClass().getClassLoader().getResource(classpathResource).toURI();
    }

    private void assertCandidateRegistered(URI specUri, CandidateProcessed body, Semver expectedVersion) {
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(Status.REGISTERED);
        assertThat(body.id()).isEqualTo("mock-id");
        assertThat(body.contract()).isEqualTo(Contract.Version.OPENAPI_V30);
        assertThat(body.info()).isNotNull();
        assertThat(body.info().title()).isEqualTo("Petstore API");
        assertThat(body.info().version()).isEqualTo(expectedVersion);
        assertThat(body.metadata()).isNotNull();
        assertThat(body.metadata().apiName()).isEqualTo("petstore");
        assertThat(body.metadata().productName()).isEqualTo("platform");
        assertThat(body.source()).isEqualTo(specUri);
        assertThat(body.scorecard()).isNotNull();
    }

    private void assertCandidateScored(URI specUri, CandidateProcessed body) {
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(Status.SCORED);
        assertThat(body.id()).isNull();
        assertThat(body.contract()).isEqualTo(Contract.Version.OPENAPI_V30);
        assertThat(body.info()).isNotNull();
        assertThat(body.info().title()).isEqualTo("Petstore API");
        assertThat(body.info().version().getVersion()).isEqualTo("1.0.0");
        assertThat(body.metadata()).isNotNull();
        assertThat(body.metadata().apiName()).isEqualTo("petstore");
        assertThat(body.metadata().productName()).isEqualTo("platform");
        assertThat(body.source()).isEqualTo(specUri);
        assertThat(body.scorecard()).isNotNull();
    }

    private void assertBadRequestProblem(ProblemDetail problem) {
        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getType()).isEqualTo(URI.create("https://problems-registry.smartbear.com/bad-request"));
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getProperties()).containsKey("errors");
    }

    private void assertUnprocessableEntityProblem(ProblemDetail problem) {
        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getType()).isEqualTo(URI.create("https://problems-registry.smartbear.com/validation-error"));
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getProperties()).containsKey("errors");
    }
}
