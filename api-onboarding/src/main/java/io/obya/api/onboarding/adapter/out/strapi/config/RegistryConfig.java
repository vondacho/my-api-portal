package io.obya.api.onboarding.adapter.out.strapi.config;

import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(RegistryProperties.class)
@Configuration
public class RegistryConfig {

    @ConditionalOnProperty(name = "registry.adapter", havingValue = "dummy")
    @Bean
    public Registry dummyRegistry() {
        return _ -> Try.of(() -> new SpecificationId("one unique id"));
    }
}
