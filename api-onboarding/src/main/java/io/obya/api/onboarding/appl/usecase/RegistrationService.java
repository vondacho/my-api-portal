package io.obya.api.onboarding.appl.usecase;

import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.workflow.Flow;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.common.util.Try;

import java.net.URI;
import java.util.List;

public class RegistrationService {

    private final Flow flow;

    private final Registry registry;

    public RegistrationService(Flow flow, Registry registry) {
        this.flow = flow;
        this.registry = registry;
    }

    public Try<State> submit(URI candidate) {
        final Try<State> state = flow.process(Try.success(new State().source(candidate)));
        return state
                .map(s -> specificationOf(s, state.getExceptions()))
                .flatMap(registry::register)
                .flatMap(id -> state.map(s -> s.id(id).status(Status.REGISTERED)))
                .recoverWithOther(_ -> state);
    }

    public Specification specificationOf(State state, List<Exception> failures) {
        return new Specification(
                state.info(),
                state.contract(),
                state.metadata(),
                state.score(),
                state.body(),
                Violation.from(failures));
    }

}
