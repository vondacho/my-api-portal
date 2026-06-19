package io.obya.api.onboarding.appl.usecase;

import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.processing.*;
import io.obya.api.onboarding.appl.usecase.workflow.Flow;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;

import java.net.URI;
import java.util.List;

import static io.obya.api.onboarding.appl.usecase.model.Status.SCORED;

public class RegistrationService {

    private final Receptionist receptionist;

    private final Parser parser;

    private final Scorer scorer;

    private final Overlayer overlayer;

    private final VersionEnforcer versionEnforcer;

    private final Registry registry;

    public RegistrationService(Receptionist receptionist, Parser parser, Scorer scorer, Overlayer overlayer, VersionEnforcer versionEnforcer, Registry registry) {
        this.receptionist = receptionist;
        this.parser = parser;
        this.scorer = scorer;
        this.overlayer = overlayer;
        this.versionEnforcer = versionEnforcer;
        this.registry = registry;
    }

    public Try<State> submit(URI candidate) {
        final Try<State> scored = Flow.compositeProcessor(
                receptionist,
                parser,
                versionEnforcer,
                scorer,
                overlayer).process(Try.success(new State().source(candidate)));

        if (scored.map(st -> st.status() != SCORED).getValue().orElse(true)) {
            return scored;
        }
        final Try<SpecificationId> registered = scored
                .map(st -> specificationOf(st, scored.getExceptions()))
                .flatMap(registry::register);

        if (registered.isFailure()) {
            return registered.recoverWithOther(_ -> scored);
        }
        return registered.flatMap(id ->
                scored.map(st -> st.id(id).status(Status.REGISTERED)));
    }

    public Specification specificationOf(State state, List<Exception> failures) {
        return new Specification(
                state.info(),
                state.contract(),
                state.metadata(),
                state.score(),
                state.body().get(),
                Violation.from(failures),
                state.id());
    }
}
