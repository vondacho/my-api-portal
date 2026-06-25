package io.obya.api.onboarding.appl.usecase;

import io.obya.api.onboarding.domain.model.Violation;
import lombok.Getter;

import java.util.List;

public class OnBoardingException extends RuntimeException {

    @Getter
    private final List<Violation> failures;

    public OnBoardingException(List<Violation> failures) {
        this.failures = failures;
    }
}
