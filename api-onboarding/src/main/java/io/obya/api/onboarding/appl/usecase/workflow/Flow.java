package io.obya.api.onboarding.appl.usecase.workflow;

import io.obya.api.onboarding.appl.usecase.processing.Processor;

public interface Flow {

    @SafeVarargs
    static Processor<State> compositeProcessor(Processor<State>... processors) {
        return initialState -> {
            var state = initialState;
            for (Processor<State> processor : processors) {
                if (state.isFailure()) {
                    break;
                }
                state = processor.process(state);
            }
            return state;
        };
    }
}
