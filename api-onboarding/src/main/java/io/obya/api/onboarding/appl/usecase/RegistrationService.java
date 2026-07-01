package io.obya.api.onboarding.appl.usecase;

import io.obya.api.onboarding.domain.model.*;
import io.obya.api.onboarding.appl.usecase.processing.*;
import io.obya.api.onboarding.appl.usecase.workflow.Flow;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.common.util.Try;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static io.obya.api.onboarding.domain.model.Status.SCORED;

public class RegistrationService {

    private final Receptionist receptionist;

    private final Parser parser;

    private final Scorer scorer;

    private final Overlayer scoreOverlayer;

    private final Overlayer componentOverlayer;

    private final Revisor revisor;

    private final Registry registry;

    private final Supplier<LocalDateTime> currentDateTimeProvider;

    public RegistrationService(
            Receptionist receptionist,
            Parser parser,
            Scorer scorer,
            Overlayer scoreOverlayer,
            Overlayer componentOverlayer,
            Revisor revisor,
            Registry registry,
            Supplier<LocalDateTime> currentDateTimeProvider) {

        this.receptionist = receptionist;
        this.parser = parser;
        this.scorer = scorer;
        this.scoreOverlayer = scoreOverlayer;
        this.componentOverlayer = componentOverlayer;
        this.revisor = revisor;
        this.registry = registry;
        this.currentDateTimeProvider = currentDateTimeProvider;
    }

    public Try<State> submit(URI candidate) {
        final Try<State> scored = Flow.compositeProcessor(
                receptionist,
                parser,
                revisor,
                scorer,
                scoreOverlayer).process(Try.success(new State().source(candidate)));

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

    public Try<State> overlay(SpecificationId specificationId, URI overlay) {
        final Try<State> overlaid = registry.at(specificationId)
                .flatMap(s -> Flow.compositeProcessor(revisor, Overlayer.defaultFrom(overlay))
                    .process(Try.success(stateOf(s))));

        return overlaid
                .map(st -> st.id(null))
                .map(st -> specificationOf(st, List.of()))
                .flatMap(registry::register)
                .flatMap(id -> overlaid.map(st -> st.id(id).status(Status.REGISTERED)));
    }

    public Try<State> score(SpecificationId specificationId) {
        final Try<State> scored = registry.at(specificationId)
                .flatMap(s -> Flow.compositeProcessor(scorer, scoreOverlayer)
                        .process(Try.success(stateOf(s))));

        final Try<SpecificationId> registered = scored
                .map(st -> specificationOf(st, scored.getExceptions()))
                .flatMap(registry::register);

        if (registered.isFailure()) {
            return registered.recoverWithOther(_ -> scored);
        }
        return registered.flatMap(id ->
                scored.map(st -> st.id(id).status(Status.REGISTERED)));
    }

    public Try<State> implement(SpecificationId specificationId, Implementation component) {
        final Try<State> implemented = registry.at(specificationId)
                .map(this::stateOf)
                .map(st -> st.metadata(st.metadata().withComponent(
                        component.componentName(), component.componentRevision())))
                .flatMap(st -> componentOverlayer.process(Try.success(st)));

        return implemented
                .map(st -> specificationOf(st, List.of()))
                .flatMap(registry::register)
                .flatMap(id -> implemented.map(st -> st.id(id).status(Status.REGISTERED)));
    }

    private Specification specificationOf(State state, List<Exception> failures) {
        return new Specification(
                state.info(),
                state.contract(),
                state.metadata(),
                state.score(),
                state.body().get(),
                Violation.from(failures),
                state.id());
    }

    private State stateOf(Specification specification) {
        return new State()
                .id(specification.id())
                .info(specification.info())
                .contract(specification.contract())
                .metadata(specification.metadata())
                .body(specification::body);
    }
}
