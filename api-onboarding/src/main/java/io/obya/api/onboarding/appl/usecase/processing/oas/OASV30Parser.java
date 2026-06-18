package io.obya.api.onboarding.appl.usecase.processing.oas;

import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.openapiparser.OpenApiResult;
import io.openapiparser.model.v30.OpenApi;
import static io.obya.api.onboarding.domain.model.Metadata.*;

public class OASV30Parser extends OASParser<OpenApi> {

    public OASV30Parser(URIReader... readers) {
        super(readers);
    }

    @Override
    protected OpenApi initModel(OpenApiResult result) {
        return result.getModel(OpenApi.class);
    }

    @Override
    protected String getTitle(OpenApi openApi) {
        return openApi.getInfo().getTitle();
    }

    @Override
    protected String getDescription(OpenApi openApi) {
        return openApi.getInfo().getDescription();
    }

    @Override
    protected String getVersion(OpenApi openApi) {
        return openApi.getInfo().getVersion();
    }

    @Override
    protected String getName(OpenApi openApi) {
        return (String) openApi.getInfo().getExtensions().get(META_API_NAME_KEY);
    }

    @Override
    protected String getBundleName(OpenApi openApi) {
        return (String) openApi.getInfo().getExtensions().get(META_BUNDLE_NAME_KEY);
    }

    @Override
    protected String getProductName(OpenApi openApi) {
        return (String) openApi.getInfo().getExtensions().get(META_PRODUCT_NAME_KEY);
    }

    @Override
    protected String getComponentName(OpenApi openApi) {
        return (String) openApi.getInfo().getExtensions().get(META_COMPONENT_NAME_KEY);
    }

    @Override
    protected String getComponentVersion(OpenApi openApi) {
        return (String) openApi.getInfo().getExtensions().get(META_COMPONENT_VERSION_KEY);
    }
}
