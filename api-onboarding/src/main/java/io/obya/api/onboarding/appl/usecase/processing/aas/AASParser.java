package io.obya.api.onboarding.appl.usecase.processing.aas;

import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.common.util.Try;
import org.semver4j.Semver;

import java.io.IOException;
import java.net.URI;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.*;
import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.VERSION_NOT_COMPLIANT;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.*;
import static io.obya.api.onboarding.appl.usecase.processing.reader.URIReader.readerFor;
import static io.obya.api.onboarding.domain.model.Metadata.*;
import static io.obya.api.onboarding.domain.model.Metadata.META_PRODUCT_NAME_KEY;

abstract class AASParser<M> implements Processor<State> {

    private final URIReader[] readers;

    public AASParser(URIReader...readers) {
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
                        .flatMap(s -> setBody(s, parsing));

                } catch (IllegalArgumentException | IOException e) {
                    return Try.failure(PROCESSING_FAILED.failure(st.source(), e).get());
                }
            });
    }

    record ParsingResult<M>(M model, String content) {}

    ParsingResult<M> parse(URI source) throws IllegalArgumentException, IOException {
        final String content = readerFor(source, readers).allInOne(source);
        return new ParsingResult<>(initModel(content), content);
    }

    abstract M initModel(String result) throws IOException;

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

    protected Try<State> setBody(State state, ParsingResult<M> parsingResult) {
        return Try.success(state.body(() -> parsingResult.content));
    }
}
