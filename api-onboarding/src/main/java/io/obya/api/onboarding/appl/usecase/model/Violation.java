package io.obya.api.onboarding.appl.usecase.model;

import java.util.List;
import java.util.function.Supplier;

public record Violation(String detail, Severity severity) implements Comparable<Violation> {

    public enum Severity {
        MAJOR, MINOR
    }

    public enum Code {
        PROCESSING_FAILED("The given uri [%s] cannot be processed [%s].", Severity.MAJOR),
        MISSING_DATA("A required data is missing or empty [%s].", Severity.MAJOR),
        INSUFFICIENT_SCORING("The score is too low [%s]", Severity.MAJOR),
        LINTING_RULE_VIOLATED("A linting rule must be respected [%s].", Severity.MAJOR),
        LINTING_RULE_TOLERATED("A linting rule should be respected [%s].", Severity.MINOR),
        DEPENDENCY_INTERNAL_ERROR("The dependency [%s] encountered an error [%s].", Severity.MINOR),
        DEPENDENCY_NOT_AVAILABLE("The dependency [%s] is not available [%s].", Severity.MINOR),
        DEPENDENCY_BAD_REQUEST("The dependency [%s] rejected a bad request [%s].", Severity.MAJOR),
        DEPENDENCY_RESPONSE_NOT_READABLE("The response of the dependency [%s] is not readable [%s].", Severity.MAJOR);

        public final String detail;
        public final Severity severity;

        Code(String detail, Severity severity) {
            this.detail = detail;
            this.severity = severity;
        }

        public Supplier<Failure> failure(Object... args) {
            return () -> new Failure(this, args);
        }
    }

    public static class Failure extends RuntimeException {
        public final Code code;
        Failure(Code code, Object... args) {
            super(String.format(code.detail, args));
            this.code = code;
        }
    }

    public static Violation from(Exception failure) {
        return failure instanceof Failure ?
            new Violation(failure.getMessage(), ((Failure) failure).code.severity) :
            new Violation(failure.getMessage(), Severity.MAJOR);
    }

    public static List<Violation> from(List<Exception> failures) {
        return failures.stream().map(Violation::from).toList();
    }

    public int compareTo(Violation other) {
        return this.severity.compareTo(other.severity);
    }
}