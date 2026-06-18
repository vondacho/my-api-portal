package io.obya.api.onboarding.appl.out;

import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.semver4j.Semver;

public interface Registry {

    Try<SpecificationId> register(Specification specification);

    Try<Specification> specificationAt(SpecificationId id, String...attributes);

    Try<Specification> specificationAt(String name, String product, Semver version, String...attributes);

    default Try<Specification> infoAt(SpecificationId id) {
        return specificationAt(id, "info", "contract", "metadata");
    }

    default Try<Specification> infoAt(String name, String product, Semver version) {
        return specificationAt(name, product, version, "info", "contract", "metadata");
    }
}
