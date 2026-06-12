package io.obya.api.onboarding.appl.out;

import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;

public interface Registry {
    Try<SpecificationId> register(Specification specification);
}
