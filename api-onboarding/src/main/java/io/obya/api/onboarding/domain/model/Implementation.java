package io.obya.api.onboarding.domain.model;

import org.semver4j.Semver;

public record Implementation(String componentName, Semver componentVersion) {
}
