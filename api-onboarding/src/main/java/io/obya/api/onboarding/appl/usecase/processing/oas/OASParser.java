package io.obya.api.onboarding.appl.usecase.processing.oas;

import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.common.util.Try;
import io.openapiparser.*;
import io.openapiprocessor.jackson.JacksonConverter;
import io.openapiprocessor.jackson.JacksonJsonWriter;
import io.openapiprocessor.jsonschema.reader.StringReader;
import io.openapiprocessor.jsonschema.schema.*;
import org.semver4j.Semver;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.*;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.*;
import static io.obya.api.onboarding.appl.usecase.processing.reader.URIReader.readerFor;
import static io.obya.api.onboarding.domain.model.Metadata.*;

abstract class OASParser<M> implements Processor<State> {

    private final URIReader[] readers;

    public OASParser(URIReader...readers) {
        this.readers = readers;
    }

    @Override
    public Try<State> process(Try<State> state) {
        return state
            .filter(st -> nonNull(st::source), MISSING_DATA.failure( "state.source"), true)
            .flatMap(st -> {
                try {
                    final ParsingResult<M> parsing = parse(st.source());
                    return Try.success(st)
                        .flatMap(s -> setModel(s, parsing.model))
                        .flatMap(s -> setInfo(s, parsing.model))
                        .flatMap(s -> setMetadata(s, parsing.model))
                        .flatMap(s -> setBody(s, parsing.result));

                } catch (IllegalArgumentException | IOException e) {
                    return Try.failure(PROCESSING_FAILED.failure(st.source(), e).get());
                } catch (ParserException e) {
                    return Try.failure(PROCESSING_FAILED.failure(st.source(), e.getCause().getCause()).get());
                }
            });
    }

    record ParsingResult<M>(M model, OpenApiResult result) {}

    ParsingResult<M> parse(URI source) throws IllegalArgumentException, IOException, ParserException {
        final String content = readerFor(source, readers).allInOne(source);
        final DocumentLoader loader = new DocumentLoader(new StringReader(content), new JacksonConverter());
        final DocumentStore documents = new DocumentStore();
        final OpenApiResult result = new OpenApiParser(documents, loader).parse(source);
        return new ParsingResult<>(initModel(result), result);
    }

    protected abstract M initModel(OpenApiResult result);

    protected Try<State> setModel(State state, M model) {
        return Try.success(state.model(model));
    }

    protected Try<State> setInfo(State state, M model) {
        Semver version = semver(nonNull(getVersion(model), MISSING_DATA.failure("info.version")),
                VERSION_NOT_COMPLIANT.failure("info.version"));

        final Try<Info> info = Try.success(new Info(
                        getTitle(model),
                        getDescription(model),
                        version)
                )
                .filter(i -> nonEmpty(i::title), MISSING_DATA.failure("info.title"))
                .filter(i -> nonEmpty(i::description), MISSING_DATA.failure("info.description"));

        return info.hasExceptions() ?
                info.flatMap(_ -> Try.failure(PARSING_FAILED.failure(state.source(), "state.info").get())) :
                info.map(state::info);
    }

    protected abstract String getTitle(M model);
    protected abstract String getDescription(M model);
    protected abstract String getVersion(M model);

    protected Try<State> setMetadata(State state, M model) {
        Semver componentVersion = getComponentVersion(model) == null ? null :
                semver(getComponentVersion(model), VERSION_NOT_COMPLIANT.failure(META_COMPONENT_VERSION_KEY));

        final Try<Metadata> metadata = Try.success(new Metadata(
                        getName(model),
                        getBundleName(model),
                        getProductName(model),
                        getComponentName(model),
                        componentVersion
                ))
                .filter(m -> nonEmpty(m::apiName), MISSING_DATA.failure(META_API_NAME_KEY))
                .filter(m -> nonEmpty(m::bundleName), MISSING_DATA.failure(META_BUNDLE_NAME_KEY))
                .filter(m -> nonEmpty(m::productName), MISSING_DATA.failure(META_PRODUCT_NAME_KEY));

        return metadata.hasExceptions() ?
                metadata.flatMap(_ -> Try.failure(PARSING_FAILED.failure(state.source(), "state.metadata").get())) :
                metadata.map(state::metadata);
    }

    protected abstract String getName(M model);
    protected abstract String getBundleName(M model);
    protected abstract String getProductName(M model);
    protected abstract String getComponentName(M model);
    protected abstract String getComponentVersion(M model);

    protected Try<State> setBody(State state, OpenApiResult parsingResult) {
        return Try.success(state.body(() -> {
            try {
                return toJson(parsingResult);
            } catch (IOException e) {
                return e.getMessage();
            }
        }));
    }

    protected static String toJson(OpenApiResult parsingResult) throws IOException {
        StringWriter stringWriter = new StringWriter();
        parsingResult.write(new JacksonJsonWriter(stringWriter));
        return stringWriter.toString();
    }
}
