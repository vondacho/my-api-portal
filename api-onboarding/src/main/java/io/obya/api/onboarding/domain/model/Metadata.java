package io.obya.api.onboarding.domain.model;

import org.semver4j.Semver;

public record Metadata(
        String apiName,
        String bundleName,
        String productName,
        String componentName,
        Semver componentVersion) {

    public static final String META_API_NAME_KEY = "x-api-name";
    public static final String META_BUNDLE_NAME_KEY = "x-bundle-name";
    public static final String META_PRODUCT_NAME_KEY = "x-product-name";
    public static final String META_COMPONENT_NAME_KEY = "x-component-name";
    public static final String META_COMPONENT_VERSION_KEY = "x-component-version";
}
