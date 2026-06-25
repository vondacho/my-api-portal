package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.api.onboarding.domain.model.Status;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Contract;
import io.obya.common.util.Try;

import java.util.Map;
import java.util.function.Supplier;

import static io.obya.api.onboarding.domain.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;

public class Parser implements Processor<State> {

    private final Map<Contract.Version, Supplier<Processor<State>>> strategies;

    public Parser(Map<Contract.Version, Supplier<Processor<State>>> strategies) {
        this.strategies = strategies;
    }

    @Override
    public Try<State> process(Try<State> state) {
        return state
            .filter(st -> nonNull(st::contract), MISSING_DATA.failure( "state.contract"), true)
            .flatMap(st -> strategies.get(st.contract().version()).get().process(state)
                .map(s -> s.status(Status.VALID)));
    }
}
