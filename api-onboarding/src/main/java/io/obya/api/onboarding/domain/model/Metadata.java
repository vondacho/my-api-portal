package io.obya.api.onboarding.domain.model;

public record Metadata(
        String name,
        Revision revision,
        String bundleName,
        String productName,
        String componentName,
        Revision componentRevision) {

    public static final String META_API_NAME_KEY = "x-api-name";
    public static final String META_API_REVISION_KEY = "x-api-revision";
    public static final String META_BUNDLE_NAME_KEY = "x-bundle-name";
    public static final String META_PRODUCT_NAME_KEY = "x-product-name";
    public static final String META_COMPONENT_NAME_KEY = "x-component-name";
    public static final String META_COMPONENT_REVISION_KEY = "x-component-revision";

    public Metadata withComponent(String componentName, Revision componentRevision) {
        return new Metadata(name, revision, bundleName, productName, componentName, componentRevision);
    }
}
