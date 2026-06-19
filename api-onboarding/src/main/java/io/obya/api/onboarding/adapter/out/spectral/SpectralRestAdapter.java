package io.obya.api.onboarding.adapter.out.spectral;

import io.obya.api.onboarding.appl.out.ScorerDelegate;
import io.obya.api.onboarding.domain.model.Contract;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.common.util.Try;

import java.net.URI;
import java.util.List;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.DEPENDENCY_NOT_AVAILABLE;

public class SpectralRestAdapter implements ScorerDelegate {

    @Override
    public Try<Scorecard> score(URI source, Contract contract) {
        return new Try.Failure<>(
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("scorer", "unavailable").get()));
    }

    @Override
    public Try<Scorecard> score(String source, Contract contract) {
        return new Try.Failure<>(
                List.of(DEPENDENCY_NOT_AVAILABLE.failure("scorer", "unavailable").get()));
    }
}
