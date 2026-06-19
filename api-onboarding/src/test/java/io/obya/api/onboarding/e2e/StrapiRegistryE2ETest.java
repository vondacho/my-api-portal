package io.obya.api.onboarding.e2e;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.adapter.in.web.model.CandidateProcessed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * End-to-end tests that exercise the full onboarding pipeline including the
 * Strapi registry adapter.  Strapi is replaced by a WireMock server so the
 * tests remain self-contained while still validating the HTTP contract with
 * the registry.
 */
@Disabled
@SpringBootTest(webEnvironment = RANDOM_PORT)
class StrapiRegistryE2ETest {

    @RegisterExtension
    static WireMockExtension strapiMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureStrapiUrl(DynamicPropertyRegistry registry) {
        registry.add("strapi.base-url", () -> strapiMock.baseUrl() + "/api");
        registry.add("registry.adapter", () -> "strapi");
    }

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void resetStubs() {
        strapiMock.resetAll();
    }

    // -------------------------------------------------------------------------
    // Submit – Strapi available
    // -------------------------------------------------------------------------

    @Test
    void submit_validSpec_strapiRegisters_returns201() throws Exception {
        String documentId = "test-doc-id-abc";
        strapiMock.stubFor(post(urlEqualTo("/api/specifications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(strapiCreateResponse(documentId))));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_0_0.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CandidateProcessed body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(Status.REGISTERED);
        assertThat(body.id()).isEqualTo(documentId);

        // Verify the body sent to Strapi contains the expected fields
        strapiMock.verify(postRequestedFor(urlEqualTo("/api/specifications"))
                .withRequestBody(matchingJsonPath("$.data.name", equalTo("petstore")))
                .withRequestBody(matchingJsonPath("$.data.productName", equalTo("platform")))
                .withRequestBody(matchingJsonPath("$.data.contract", equalTo("OPENAPI_V30"))));
    }

    @Test
    void submit_validAsyncApiSpec_strapiRegisters_returns201() throws Exception {
        String documentId = "async-doc-xyz";
        strapiMock.stubFor(post(urlEqualTo("/api/specifications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(strapiCreateResponse(documentId))));

        URI specUri = specFileUri("e2e/asyncapi_v30_notification_v1_0_0.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo(documentId);

        strapiMock.verify(postRequestedFor(urlEqualTo("/api/specifications"))
                .withRequestBody(matchingJsonPath("$.data.contract", equalTo("ASYNCAPI_V30"))));
    }

    // -------------------------------------------------------------------------
    // Submit – Strapi unavailable (resilience)
    // -------------------------------------------------------------------------

    @Test
    void submit_strapiUnavailable_returnsValidWithoutRegistration() throws Exception {
        strapiMock.stubFor(post(urlEqualTo("/api/specifications"))
                .willReturn(aResponse().withStatus(503)));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_0_0.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        // Pipeline succeeded but Strapi was unavailable → VALID/SCORED, not REGISTERED
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CandidateProcessed body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isNotEqualTo(Status.REGISTERED);
        assertThat(body.id()).isNull();
        // At least one violation must report the registry unavailability
        assertThat(body.violations())
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void submit_strapiReturns500_returnsValidWithoutRegistration() throws Exception {
        strapiMock.stubFor(post(urlEqualTo("/api/specifications"))
                .willReturn(aResponse().withStatus(500)));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_0_0.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations", new Candidate(specUri), CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isNotEqualTo(Status.REGISTERED);
    }

    // -------------------------------------------------------------------------
    // Upgrade – Strapi available
    // -------------------------------------------------------------------------

    @Test
    void upgrade_withExistingSpec_strapiUpdates_returns201() throws Exception {
        String documentId = "existing-doc-id";
        String existingVersion = "1.0.0";

        // GET /api/specifications/{id} – returns existing spec
        strapiMock.stubFor(get(urlEqualTo("/api/specifications/" + documentId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(strapiGetResponse(documentId, existingVersion))));

        // PUT /api/specifications/{id} – accepts the upgraded spec
        strapiMock.stubFor(put(urlEqualTo("/api/specifications/" + documentId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(strapiUpdateResponse(documentId))));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_0_0.yaml");
        ResponseEntity<CandidateProcessed> response = rest.postForEntity(
                "/registrations/" + documentId,
                new Candidate(specUri),
                CandidateProcessed.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CandidateProcessed body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(Status.REGISTERED);

        strapiMock.verify(getRequestedFor(urlEqualTo("/api/specifications/" + documentId)));
        strapiMock.verify(putRequestedFor(urlEqualTo("/api/specifications/" + documentId)));
    }

    @Test
    void upgrade_withUnknownId_strapiReturns404_returnsError() throws Exception {
        String unknownId = "ghost-id-000";
        strapiMock.stubFor(get(urlEqualTo("/api/specifications/" + unknownId))
                .willReturn(aResponse().withStatus(404)));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_0_0.yaml");
        ResponseEntity<String> response = rest.postForEntity(
                "/registrations/" + unknownId,
                new Candidate(specUri),
                String.class);

        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Strapi response builders
    // -------------------------------------------------------------------------

    private static String strapiCreateResponse(String documentId) {
        return """
                {
                  "data": {
                    "documentId": "%s",
                    "name": "petstore",
                    "version": "1.0.0",
                    "productName": "platform",
                    "bundleName": "petstore",
                    "contract": "OPENAPI_V30",
                    "body": ""
                  }
                }
                """.formatted(documentId);
    }

    private static String strapiGetResponse(String documentId, String version) {
        return """
                {
                  "data": {
                    "documentId": "%s",
                    "name": "petstore",
                    "version": "%s",
                    "productName": "platform",
                    "bundleName": "petstore",
                    "contract": "OPENAPI_V30",
                    "body": ""
                  }
                }
                """.formatted(documentId, version);
    }

    private static String strapiUpdateResponse(String documentId) {
        return strapiCreateResponse(documentId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private URI specFileUri(String classpathResource) throws Exception {
        return getClass().getClassLoader().getResource(classpathResource).toURI();
    }
}
