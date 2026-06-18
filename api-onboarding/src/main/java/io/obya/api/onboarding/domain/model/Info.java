package io.obya.api.onboarding.domain.model;

import org.semver4j.Semver;

public record Info(
     String title,
     String description,
     Semver version) {

    public Info nextPatch() {
        return new Info(title, description, version.nextPatch());
    }
}
