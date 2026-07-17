package io.obya.api.onboarding.domain.model;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static io.obya.api.onboarding.domain.model.Violation.Code.MALFORMED_VERSION;

public record Version(int major) {

    public static final Version V1 =  new Version(1);
    public static final Version V2 = new Version(2);

    private static final String pattern = "^v[0-9][1-9]*$";

    public static final Predicate<String> matcher = Pattern.compile(pattern).asMatchPredicate();

    public static Version from(String s) {
        return from(s, MALFORMED_VERSION.failure(s, pattern));
    }

    public static Version from(Revision revision) {
        return new Version(revision.semver().getMajor());
    }

    public static <E extends Exception> Version from(String s, Supplier<E> e) throws E {
        if (matcher.test(s)) return new Version(Integer.parseInt(s.substring(1)));
        throw e.get();
    }

    public String format() {
        return "v" + major;
    }
}
