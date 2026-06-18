package io.obya.api.onboarding.appl.usecase;

import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.processing.*;
import io.obya.api.onboarding.appl.usecase.workflow.Flow;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Metadata;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.semver4j.Semver;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.*;

public class RegistrationService {

    private final Receptionist receptionist;

    private final Parser parser;

    private final Scorer scorer;

    private final Overlayer overlayer;

    private final Registry registry;

    public RegistrationService(Receptionist receptionist, Parser parser, Scorer scorer, Overlayer overlayer, Registry registry) {
        this.receptionist = receptionist;
        this.parser = parser;
        this.scorer = scorer;
        this.overlayer = overlayer;
        this.registry = registry;
    }

    public Try<State> submit(URI candidate) {
        final Try<State> state = Flow.compositeProcessor(
                receptionist,
                parser,
                scorer,
                st -> st
                        .flatMap(s -> currentVersion(s.metadata().apiName(), s.metadata().productName(), s.info().version())
                        .flatMap(currentVersion -> enforceUpgrade(currentVersion).process(st))
                        .recoverWith(_ -> st)),  // no prior version → first submission, skip enforcement
                overlayer).process(Try.success(new State().source(candidate)));

        return state
                .map(st -> specificationOf(st, state.getExceptions()))
                .flatMap(registry::register)
                .flatMap(id -> state.map(st -> st.id(id).status(Status.REGISTERED)))
                .recoverWithOther(_ -> state);
    }

    public Try<State> upgrade(SpecificationId id, URI candidate) {
        return currentVersion(id).flatMap(currentVersion -> {

            final Try<State> state = Flow.compositeProcessor(
                    receptionist,
                    parser,
                    scorer,
                    st -> enforceUpgrade(currentVersion).process(st),
                    overlayer).process(Try.success(new State().source(candidate)));

            return state
                    .map(s -> specificationOf(s, state.getExceptions()))
                    .flatMap(registry::register)
                    .flatMap(upgradeId -> state.map(s -> s.id(upgradeId).status(Status.REGISTERED)))
                    .recoverWithOther(_ -> state);
        });
    }

    private Try<Specification> findById(SpecificationId id) {
        return registry.infoAt(id);
    }

    private Try<Specification> findByMetadata(String name, String product, Semver version) {
        return registry.infoAt(name, product, version);
    }

    private Try<Semver> currentVersion(SpecificationId id) {
        return findById(id)
                .map(Specification::info)
                .map(Info::version);
    }

    private Try<Semver> currentVersion(String name, String product, Semver version) {
        return findByMetadata(name, product, version)
                .map(Specification::info)
                .map(Info::version);
    }

    private Processor<State> enforceUpgrade(Semver currentVersion) {
        return state -> state
                .filter(
                st -> st.info().version().isGreaterThan(currentVersion),
                st -> VERSION_AUTO_INCREMENTED.failure(st.info().version(), st.info().version().nextPatch()).get(), true)
                .recoverWith(_ -> state.map(st -> st.info(st.info().nextPatch())));
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
