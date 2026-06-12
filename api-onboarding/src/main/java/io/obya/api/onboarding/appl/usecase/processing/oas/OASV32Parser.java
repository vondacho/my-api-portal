package io.obya.api.onboarding.appl.usecase.processing.oas;

import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.common.util.Try;
import io.openapiparser.OpenApiResult;
import io.openapiparser.model.v32.OpenApi;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.PROCESSING_FAILED;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonEmpty;

public class OASV32Parser extends OASParser<OpenApi> {

    public OASV32Parser(URIReader... readers) {
        super(readers);
    }

    @Override
    OpenApi initModel(OpenApiResult result) {
        return result.getModel(OpenApi.class);
    }

    @Override
    Try<State> setInfo(State state, OpenApi model) {
        var info = Try.success(new Info(
                        model.getInfo().getTitle(),
                        model.getInfo().getDescription(),
                        model.getInfo().getVersion())
                )
                .filter(i -> nonEmpty(i::title), MISSING_DATA.failure("info.title"))
                .filter(i -> nonEmpty(i::description), MISSING_DATA.failure("info.description"))
                .filter(i -> nonEmpty(i::version), MISSING_DATA.failure("info.version"));

        return info.hasExceptions() ?
                info.flatMap(_ -> Try.failure(PROCESSING_FAILED.failure(state.source(), "state.info").get())) :
                info.map(state::info);
    }

    @Override
    Try<State> setMetadata(State state, OpenApi model) {
        var metadata = Try.success(new Metadata(
                        getExtension(model, "x-api-slug"),
                        getExtension(model, "x-api-bundle"),
                        getExtension(model, "x-product-slug"),
                        getExtension(model, "x-component"),
                        getExtension(model, "x-component-version")
                ))
                .filter(m -> nonEmpty(m::apiName), MISSING_DATA.failure("x-api-slug"))
                .filter(m -> nonEmpty(m::bundleName), MISSING_DATA.failure("x-api-bundle"))
                .filter(m -> nonEmpty(m::productName), MISSING_DATA.failure("x-product-slug"));

        return metadata.hasExceptions() ?
                metadata.flatMap(_ -> Try.failure(PROCESSING_FAILED.failure(state.source(), "state.metadata").get())) :
                metadata.map(state::metadata);
    }

    private static String getExtension(OpenApi model, String key) {
        return (String) model.getInfo().getExtensions().get(key);
    }
}
