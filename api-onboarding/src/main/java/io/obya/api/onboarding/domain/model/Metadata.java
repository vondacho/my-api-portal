package io.obya.api.onboarding.domain.model;

public record Metadata(
        String apiName,
        String bundleName,
        String productName,
        String componentName,
        String componentVersion) {
}
