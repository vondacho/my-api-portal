package io.obya.api.onboarding.appl.usecase.processing.aas;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.common.util.Try;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.PROCESSING_FAILED;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonEmpty;

public class AASV30Parser extends AASParser<AsyncAPI> {

    public AASV30Parser(URIReader... readers) {
        super(readers);
    }

    @Override
    protected AsyncAPI initModel(String content) throws JsonProcessingException {
        var objectMapper = new ObjectMapper(new YAMLFactory())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper.readValue(content, AsyncAPI.class);
    }

    @Override
    Try<State> setInfo(State state, AsyncAPI model) {
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
    Try<State> setMetadata(State state, AsyncAPI model) {
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

    private static String getExtension(AsyncAPI model, String key) {
        return (String) model.getInfo().getExtensionFields().get(key);
    }
}
