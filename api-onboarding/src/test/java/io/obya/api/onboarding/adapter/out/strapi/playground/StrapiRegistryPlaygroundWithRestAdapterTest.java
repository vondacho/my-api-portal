package io.obya.api.onboarding.adapter.out.strapi.playground;

import io.obya.api.onboarding.adapter.out.strapi.StrapiRegistryRestAdapter;
import io.obya.api.onboarding.domain.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClientException;

import static io.obya.api.onboarding.appl.usecase.UsecaseExamples.Sources.bodyOf;
import static io.obya.api.onboarding.appl.usecase.UsecaseExamples.Sources.validCandidateUri;
import static io.obya.api.onboarding.domain.model.DomainExamples.Specifications.id123;
import static io.obya.api.onboarding.domain.model.DomainExamples.Specifications.specificationOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@Disabled("E2E playground for investigation and troubleshooting")
@SpringBootTest(webEnvironment = NONE)
class StrapiRegistryPlaygroundWithRestAdapterTest {

    @DynamicPropertySource
    static void configureStrapiUrl(DynamicPropertyRegistry registry) {
        registry.add("strapi.base-url", () -> "http://localhost:1337/api");
        registry.add("registry.adapter", () -> "strapi");
    }

    @Autowired
    private StrapiRegistryRestAdapter adapter;

    @Test
    void should_list_specification_resources() {
        assertThat(adapter.all()).isNotNull();
    }

    @Test
    void should_get_specification_resource_detail() {
        try {
            assertThat(adapter.at(id123.get())).isNotNull();
        } catch (RestClientException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void should_register_valid_openapi_specification() throws Exception {
        try {
            Specification specification = specificationOf(
                    id123.get(), "petstore", "platform",
                    Version.V1, Revision.V100,
                    bodyOf(validCandidateUri.get()));

            assertThat(adapter.register(specification).isSuccess()).isTrue();
        } catch (RestClientException e) {
            fail(e.getMessage());
        }
    }
}
