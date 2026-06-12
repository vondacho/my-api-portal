package io.obya.api.onboarding.appl.usecase.processing.aas;

import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import java.io.IOException;
import java.net.URI;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.PROCESSING_FAILED;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;
import static io.obya.api.onboarding.appl.usecase.processing.reader.URIReader.readerFor;

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

    abstract Try<State> setInfo(State state, M model);

    abstract Try<State> setMetadata(State state, M model);

    Try<State> setModel(State state, M model) {
        return Try.success(state.model(model));
    }

    Try<State> setBody(State state, ParsingResult<M> parsingResult) {
        return Try.success(state.body(() -> parsingResult.content));
    }
}
