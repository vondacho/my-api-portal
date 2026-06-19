package io.obya.api.onboarding.adapter.out.spectral.config;

import io.obya.api.onboarding.adapter.out.spectral.SpectralRestAdapter;
import io.obya.api.onboarding.appl.out.ScorerDelegate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpectralClientConfig {

    @ConditionalOnProperty(name = "scorer.adapter", havingValue = "spectral")
    @Bean
    public ScorerDelegate spectralScorer() {
        return new SpectralRestAdapter();
    }
}
