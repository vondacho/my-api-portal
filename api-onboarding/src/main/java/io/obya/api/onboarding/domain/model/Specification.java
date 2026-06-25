package io.obya.api.onboarding.domain.model;

import java.util.List;

public record Specification(
        Info info,
        Contract contract,
        Metadata metadata,
        Scorecard score,
        String body,
        List<Violation> violations,
        SpecificationId id) {
}
