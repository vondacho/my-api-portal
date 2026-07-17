package io.obya.api.onboarding.appl.usecase.processing;

import org.semver4j.Semver;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Validator {

    static <T> boolean nonNull(Supplier<T> value) {
        return value.get() != null;
    }

    static <T, E extends Exception> T nonNull(T value, Supplier<E> e) throws E {
        return Optional.ofNullable(value).orElseThrow(e);
    }

    static <T, E extends Exception> T nonNull(Supplier<T> value, Supplier<E> e) throws E {
        return Optional.ofNullable(value.get()).orElseThrow(e);
    }

    static boolean nonEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static boolean nonEmpty(Supplier<String> value) {
        return nonEmpty(value.get());
    }

    static <E extends Exception> Semver semver(String value, Supplier<E> e) throws E {
        return Optional.ofNullable(Semver.coerce(value)).orElseThrow(e);
    }
}
