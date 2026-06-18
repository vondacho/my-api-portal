package io.obya.api.onboarding.adapter.out.config;

import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.semver4j.Semver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.RESOURCE_NOT_FOUND;

@EnableConfigurationProperties(RegistryProperties.class)
@Configuration
public class RegistryConfig {

    @ConditionalOnProperty(name = "registry.adapter", havingValue = "dummy")
    @Bean
    public Registry dummyRegistry() {
        return new Registry() {
            @Override
            public Try<SpecificationId> register(Specification specification) {
                return Try.success(new SpecificationId("one-unique-id"));
            }

            @Override
            public Try<Specification> specificationAt(SpecificationId id, String... attributes) {
                return new Try.Failure<>(List.of(RESOURCE_NOT_FOUND.failure( "Specification", id).get()));
            }

            @Override
            public Try<Specification> specificationAt(String name, String product, Semver version, String... attributes) {
                return new Try.Failure<>(List.of(RESOURCE_NOT_FOUND.failure( "Specification",
                        "[%s-%s-%s]".formatted(name, product, version)).get()));
            }
        };
    }
}
