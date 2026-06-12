package io.obya.api.onboarding.appl.usecase.processing.oas;

import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;
import io.openapiparser.*;
import io.openapiprocessor.jackson.JacksonConverter;
import io.openapiprocessor.jackson.JacksonJsonWriter;
import io.openapiprocessor.jsonschema.reader.StringReader;
import io.openapiprocessor.jsonschema.schema.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.PROCESSING_FAILED;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;
import static io.obya.api.onboarding.appl.usecase.processing.reader.URIReader.readerFor;

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

    abstract M initModel(OpenApiResult result);

    abstract Try<State> setInfo(State state, M model);

    abstract Try<State> setMetadata(State state, M model);

    Try<State> setModel(State state, M model) {
        return Try.success(state.model(model));
    }

    Try<State> setBody(State state, OpenApiResult parsingResult) {
        return Try.success(state.body(() -> {
            try {
                return toJson(parsingResult);
            } catch (IOException e) {
                return e.getMessage();
            }
        }));
    }

    String toJson(OpenApiResult parsingResult) throws IOException {
        StringWriter stringWriter = new StringWriter();
        parsingResult.write(new JacksonJsonWriter(stringWriter));
        return stringWriter.toString();
    }
}
