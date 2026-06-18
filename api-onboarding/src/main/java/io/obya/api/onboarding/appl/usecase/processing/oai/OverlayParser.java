package io.obya.api.onboarding.appl.usecase.processing.oai;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.ibm.oas.overlay.OverlayProcessor;
import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.*;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;
import static io.obya.api.onboarding.appl.usecase.processing.reader.URIReader.readerFor;

public class OverlayParser implements Processor<State> {

    private final URIReader[] readers;

    private final MustacheFactory mf;

    private final BiFunction<State, List<Exception>, Map<String, Object>> mapper;

    public OverlayParser(URIReader[] readers, BiFunction<State, List<Exception>, Map<String, Object>> mapper) {
        this.readers = readers;
        this.mf = new DefaultMustacheFactory();
        this.mapper = mapper;
    }

    @Override
    public Try<State> process(Try<State> state) {
        return state
            .filter(st -> nonNull(st::source), MISSING_DATA.failure( "state.source"), true)
            .filter(st -> nonNull(st::body), MISSING_DATA.failure( "state.body"), true)
            .flatMap(st -> {
                try {
                    final String result = processOverlay(st.source(), st.body().get(), mapper.apply(st, state.getExceptions()));
                    return Try.success(st.body(() -> result));

                } catch (IOException e) {
                    return Try.failure(OVERLAYING_FAILED.failure(st.source(), e).get());
                }
            });
    }

    String processOverlay(URI overlay, String body, Map<String, Object> data) throws IOException {
        Mustache template = mf.compile(new StringReader(readerFor(overlay, readers).allInOne(overlay)), overlay.toString());
        return OverlayProcessor.processOverlay(body, instantiateOverlay(template, data));
    }

    String instantiateOverlay(Mustache overlay, Map<String, Object> data) {
        StringWriter writer = new StringWriter();
        overlay.execute(writer, data);
        return writer.toString();
    }
}
