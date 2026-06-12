package io.obya.api.onboarding.appl.usecase.workflow;

import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.common.util.Try;

import java.util.List;

public class Flow {

    private final List<Processor<State>> processors;

    public Flow(List<Processor<State>> processors) {
        this.processors = processors;
    }

    public Try<State> process(Try<State> initialState) {
        var state = initialState;
        for (Processor<State> processor : processors) {
            if (state.isFailure()) {
                break;
            }
            state = processor.process(state);
        }
        return state;
    }
}
