package io.obya.api.onboarding.appl.usecase.processing;

import org.semver4j.Semver;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Validator {

    static <T> boolean nonNull(T value) {
        return value != null;
    }

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
        return !(value == null || value.isEmpty());
    }

    static boolean nonEmpty(Supplier<String> value) {
        return nonEmpty(value.get());
    }

    static <E extends Exception> String nonEmpty(String value, Supplier<E> e) throws E {
        return Optional.ofNullable(value).filter(s -> !s.isBlank()).orElseThrow(e);
    }

    static <E extends Exception> String nonEmpty(Supplier<String> value, Supplier<E> e) throws E {
        return nonEmpty(value.get(), e);
    }

    static <E extends Exception> Semver semver(String value, Supplier<E> e) throws E {
        return Optional.ofNullable(Semver.coerce(value)).orElseThrow(e);
    }

    static <T> Predicate<T> requiredOf(T value) {
        return _ -> value != null;
    }

    static <T> Predicate<T> requiredOf(Supplier<T> getter) {
        return _ -> getter.get() != null;
    }

    static <U> Predicate<U> nonEmptyOf(String value) {
        return _ -> nonEmpty(value);
    }

    static <U> Predicate<U> nonEmptyOf(Supplier<String> getter) {
        return nonEmptyOf(getter.get());
    }
}
