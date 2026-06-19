package io.obya.api.onboarding.adapter.out.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scorer")
public record ScorerProperties(@NotBlank String adapter) {}
