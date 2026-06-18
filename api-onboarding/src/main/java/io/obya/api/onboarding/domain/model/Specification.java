package io.obya.api.onboarding.domain.model;

import io.obya.api.onboarding.appl.usecase.model.Violation;

import java.util.List;
import java.util.function.Supplier;

public record Specification(
        Info info,
        Contract contract,
        Metadata metadata,
        Scorecard score,
        String body,
        List<Violation> violations,
        SpecificationId id) {
}
