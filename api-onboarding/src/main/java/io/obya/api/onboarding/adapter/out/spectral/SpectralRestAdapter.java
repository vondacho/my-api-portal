package io.obya.api.onboarding.adapter.out.spectral;

import io.obya.api.onboarding.appl.out.ScorerDelegate;
import io.obya.api.onboarding.domain.model.Contract;
import io.obya.api.onboarding.domain.model.Score;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.common.util.Try;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static io.obya.api.onboarding.domain.model.Scorecard.Dimension.FC;

public class SpectralRestAdapter implements ScorerDelegate {

    @Override
    public Try<Scorecard> score(URI source, Contract contract) {
        return Try.success(new Scorecard(
                new Score(74),
                Map.of(FC, new Score(99))));
    }

    @Override
    public Try<Scorecard> score(String source, Contract contract) {
        return Try.success(new Scorecard(
                new Score(55),
                Map.of(FC, new Score(99))));
    }
}
