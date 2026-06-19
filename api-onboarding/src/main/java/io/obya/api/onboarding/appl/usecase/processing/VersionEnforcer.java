package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.Info;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.common.util.Try;
import org.semver4j.Semver;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.VERSION_AUTO_INCREMENTED;

public class VersionEnforcer implements Processor<State> {

    private final Registry registry;

    public VersionEnforcer(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Try<State> process(Try<State> state) {
        Try<Semver> latestVersion = state.flatMap(s ->
                latestVersion(s.metadata().apiName(), s.metadata().productName(), s.info().version()));

        if (latestVersion.isFailure()) {
            return latestVersion.recoverWithOther(_ -> state);
        }
        return latestVersion.flatMap(v -> enforcer(v).process(state));
    }

    private Processor<State> enforcer(Semver latestVersion) {
        return state -> state
            .filter(
            st -> st.info().version().isGreaterThan(latestVersion),
            st -> VERSION_AUTO_INCREMENTED.failure(st.info().version(), st.info().version().nextPatch()).get(), true)
            .recoverWith(_ -> state.map(st -> st.info(st.info().nextPatch())));
    }

    private Try<Semver> latestVersion(String name, String product, Semver version) {
        return findByMetadata(name, product, version)
                .map(Specification::info)
                .map(Info::version);
    }

    private Try<Specification> findByMetadata(String name, String product, Semver version) {
        return registry.infoAt(name, product, version);
    }
}
