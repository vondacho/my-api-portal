package io.obya.api.onboarding.e2e;

import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.*;
import io.obya.common.util.Try;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * End-to-end tests covering the full onboarding pipeline from HTTP ingress through
 * parsing, scoring, and registration.  This is the RestAssured counterpart of
 * {@link RegistrationE2ETest}: the Registry port is replaced by a Mockito mock so the
 * tests run without any external infrastructure (no Strapi, no dummy profile), and the
 * HTTP contract is asserted with RestAssured's fluent given/when/then DSL.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RegistrationRestAssuredE2ETest {

    @MockitoBean
    Registry registry;

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/";
        // No prior version found → first submission
        when(registry.at(anyString(), anyString(), any(Version.class))).thenReturn(new Try.Failure<>(List.of()));
        // Happy path: registration always succeeds
        when(registry.register(any())).thenReturn(Try.success(new SpecificationId("mock-id")));
    }

    // -------------------------------------------------------------------------
    // Submit – happy paths
    // -------------------------------------------------------------------------

    @Test
    void submit_validOpenApiV30_registersSpecAndReturns201() throws Exception {
        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1.yaml");

        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(specUri))
        .when()
                .post("/registrations")
        .then()
                .statusCode(201)
                .body("status", equalTo("REGISTERED"))
                .body("id", equalTo("mock-id"))
                .body("contract", equalTo("OPENAPI_V30"))
                .body("info", notNullValue())
                .body("info.title", equalTo("Petstore API"))
                .body("info.version.major", equalTo(1))
                .body("metadata", notNullValue())
                .body("metadata.apiName", equalTo("petstore"))
                .body("metadata.productName", equalTo("platform"))
                .body("source", equalTo(specUri.toString()))
                .body("scorecard", notNullValue());
    }

    @Test
    void submit_validAsyncApiV30_registersSpecAndReturns201() throws Exception {
        URI specUri = specFileUri("e2e/asyncapi_v30_notification_v1.yaml");

        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(specUri))
        .when()
                .post("/registrations")
        .then()
                .statusCode(201)
                .body("status", equalTo("REGISTERED"))
                .body("contract", equalTo("ASYNCAPI_V30"))
                .body("info.title", equalTo("Notification API"))
                .body("metadata.apiName", equalTo("notification"));
    }

    // -------------------------------------------------------------------------
    // Submit – validation / partial results
    // -------------------------------------------------------------------------

    @Test
    void submit_specMissingCustomMetadata_returnsProblemDetail() throws Exception {
        URI specUri = specFileUri("e2e/openapi_v30_no_metadata.yaml");

        // Without x-api-name / x-product-name the pipeline fails and the controller
        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(specUri))
        .when()
                .post("/registrations")
        .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("https://problems-registry.smartbear.com/bad-request"))
                .body("title", equalTo("Bad Request"))
                .body("status", equalTo(400))
                .body("errors", notNullValue());
    }

    @Test
    void submit_registryFails_returnsValidWithoutId() throws Exception {
        when(registry.register(any())).thenReturn(new Try.Failure<>(List.of()));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1.yaml");

        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(specUri))
        .when()
                .post("/registrations")
        .then()
                .statusCode(200)
                .body("status", not(equalTo("REGISTERED")))
                .body("id", nullValue());
    }

    // -------------------------------------------------------------------------
    // Submit – error cases
    // -------------------------------------------------------------------------

    @Test
    void submit_nonExistentSourceUri_returnsError() {
        URI missing = URI.create("file:///non/existent/spec.yaml");

        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(missing))
        .when()
                .post("/registrations")
        .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("https://problems-registry.smartbear.com/bad-request"))
                .body("title", equalTo("Bad Request"))
                .body("status", equalTo(400))
                .body("errors", notNullValue());
    }

    @Test
    void submit_missingSourceField_returns4xx() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/registrations")
                .then()
        .statusCode(422)
                .contentType("application/problem+json")
                .body("type", equalTo("https://problems-registry.smartbear.com/validation-error"))
                .body("title", equalTo("Validation Error"))
                .body("status", equalTo(422))
                .body("errors", notNullValue());
    }

    // -------------------------------------------------------------------------
    // Upgrade – happy path
    // -------------------------------------------------------------------------

    @Test
    void upgrade_withExistingSpec_autoIncrementsVersionAndReturns201() throws Exception {
        Specification existing = new Specification(
                new Info("Petstore API", "A sample API.", Version.from("v1")),
                Contract.from(Contract.Version.OPENAPI_V30),
                new Metadata("petstore",
                        Revision.from("1.0.0"),
                        "petstore", "platform",
                        null, null),
                Scorecard.undefined(),
                "",
                List.of(),
                new SpecificationId("mock-id"));

        when(registry.at(anyString(), anyString(), any(Version.class))).thenReturn(Try.success(existing));
        when(registry.register(any())).thenReturn(Try.success(new SpecificationId("upgrade-id")));

        URI specUri = specFileUri("e2e/openapi_v30_petstore_v1_1_0_0.yaml");

        given()
                .contentType(ContentType.JSON)
                .body(new Candidate(specUri))
        .when()
                .post("/registrations")
        .then()
                .statusCode(201)
                .body("id", equalTo("upgrade-id"))
                .body("status", equalTo("REGISTERED"))
                .body("contract", equalTo("OPENAPI_V30"))
                .body("info.version.major", equalTo(1))
                .body("metadata.apiRevision.semver", equalTo("1.0.1"))
                .body("violations.code", hasItem("REVISION_AUTO_INCREMENTED"));
    }


    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private URI specFileUri(String classpathResource) throws Exception {
        return getClass().getClassLoader().getResource(classpathResource).toURI();
    }
}
