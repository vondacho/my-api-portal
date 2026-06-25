package io.obya.api.onboarding.domain.model;

import io.obya.api.onboarding.appl.usecase.processing.Validator;
import org.semver4j.Semver;

import java.util.function.Supplier;

import static io.obya.api.onboarding.domain.model.Violation.Code.MALFORMED_REVISION;

public record Revision(Semver semver) {

    public static Revision from(String s) {
        return from(s, (MALFORMED_REVISION.failure(s, "semver")));
    }

    public static <E extends Exception> Revision from(String s, Supplier<E> e) throws E {
        return new Revision(Validator.semver(s, e));
    }

    public static Revision from(Version version) {
        return new Revision(Semver.builder().withMajor(version.major()).build());
    }

    public boolean matches(Version version) {
        return semver.getMajor() == version.major();
    }

    public Revision next() {
        return new Revision(semver.nextPatch());
    }

    public boolean after(Revision other) {
        return this.semver.isGreaterThan(other.semver);
    }
}
