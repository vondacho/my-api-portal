package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.api.onboarding.appl.usecase.processing.oai.OverlayParser;
import io.obya.api.onboarding.appl.usecase.processing.oai.OverlayV10Parser;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIFileReader;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIHttpReader;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import java.net.URI;
import java.util.Map;

public class Overlayer implements Processor<State> {

    private final URI uri;
    private final OverlayParser strategy;

    public Overlayer(URI uri, OverlayParser strategy) {
       this.uri = uri;
       this.strategy = strategy;
    }

    public static Overlayer defaultFrom(URI overlay) {
        URIReader[] readers = { new URIFileReader(), new URIHttpReader() };
        return new Overlayer(overlay, new OverlayV10Parser(readers, (_, _) -> Map.of()));
    }

    @Override
    public Try<State> process(Try<State> state) {
       return state.flatMap(st -> {
           final URI originalUri = st.source();
           return strategy.process(Try.success(st.source(uri))).map(s -> s.source(originalUri));
       });
    }
}
