package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.api.onboarding.appl.out.ScorerDelegate;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.common.util.Try;

import java.util.List;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.*;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;

public class Scorer implements Processor<State> {

    private final ScorerDelegate delegate;

    public Scorer(ScorerDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Try<State> process(Try<State> state) {
        return state
            .filter(st -> nonNull(st::source), MISSING_DATA.failure( "state.source"), true)
            .filter(st -> nonNull(st::contract), MISSING_DATA.failure( "state.contract"), true)
            .flatMap(st -> {
                final Try<Scorecard> scored = delegate.score(st.source(), st.contract());
                if (scored.isFailure()) {
                    return degradeGracefully(st);
                }
                return scored
                        .map(sc -> st.score(sc).status(Status.SCORED))
                        .filter(s -> !s.score().isTooLow(), s -> INSUFFICIENT_SCORING.failure(s.score()).get(), true);
            });
    }

    private Try<State> degradeGracefully(State state) {
        return new Try.Partial<>(
                state.score(Scorecard.undefined()),
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("scorer", "unavailable").get()));
    }
}
