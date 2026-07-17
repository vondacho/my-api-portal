package io.obya.api.onboarding.adapter.out.strapi.config;

import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@EnableConfigurationProperties(StrapiProperties.class)
@Configuration
public class StrapiClientConfig {

    @ConditionalOnProperty(name = "registry.adapter", havingValue = "strapi")
    @Bean
    SpecificationApi strapiApiClient(RestClient.Builder builder, StrapiProperties props) {
        RestClient restClient = builder
                .baseUrl("http://localhost:1337/api")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(SpecificationApi.class);
    }
}
