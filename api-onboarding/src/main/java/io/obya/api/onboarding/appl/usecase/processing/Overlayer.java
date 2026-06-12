package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.api.onboarding.appl.usecase.processing.oai.OverlayParser;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import java.net.URI;

public class Overlayer implements Processor<State> {

    private final URI uri;
    private final OverlayParser strategy;

    public Overlayer(URI uri, OverlayParser strategy) {
       this.uri = uri;
       this.strategy = strategy;
    }

    @Override
    public Try<State> process(Try<State> state) {
       return state.flatMap(st -> strategy.process(Try.success(st.source(uri))));
    }
}
