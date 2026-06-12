package io.obya.api.onboarding.adapter.out.spectral.config;

import io.obya.api.onboarding.adapter.out.spectral.SpectralRestAdapter;
import io.obya.api.onboarding.appl.out.ScorerDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpectralClientConfig {

    @Bean
    public ScorerDelegate spectralLinter() {
        return new SpectralRestAdapter();
    }
}
