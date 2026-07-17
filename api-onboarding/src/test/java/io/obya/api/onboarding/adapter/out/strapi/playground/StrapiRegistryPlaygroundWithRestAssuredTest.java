package io.obya.api.onboarding.adapter.out.strapi.playground;

import io.obya.api.onboarding.domain.model.Revision;
import io.obya.api.onboarding.domain.model.Version;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.obya.api.onboarding.appl.usecase.UsecaseExamples.Sources.bodyOf;
import static io.obya.api.onboarding.appl.usecase.UsecaseExamples.Sources.validCandidateUri;
import static io.obya.api.onboarding.domain.model.DomainExamples.Specifications.id123;
import static io.obya.api.onboarding.domain.model.DomainExamples.Specifications.specificationOf;
import static io.restassured.RestAssured.given;

@Disabled("E2E playground for investigation and troubleshooting")
class StrapiRegistryPlaygroundWithRestAssuredTest {

    @BeforeEach
    void setUp() {
        RestAssured.port = 1337;
        RestAssured.basePath = "/api";
    }

    @Test
    void should_list_specification_resources() {
        given()
                .when()
                .get("/specifications")
                .peek()
                .then()
                .statusCode(200);
    }

    @Test
    void should_get_specification_resource_detail() {
        given()
                .when()
                .get("/specifications/123")
                .peek()
                .then()
                .statusCode(200);
    }

    @Test
    void should_register_valid_openapi_specification() throws Exception {
        given()
                .when()
                .body(specificationOf(
                        id123.get(), "petstore", "platform",
                        Version.V1, Revision.V100,
                        bodyOf(validCandidateUri.get())))
                .post("/specifications")
                .peek()
                .then()
                .statusCode(201);
    }
}
