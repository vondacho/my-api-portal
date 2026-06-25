package io.obya.api.onboarding.appl.usecase.workflow;

import io.obya.api.onboarding.domain.model.Status;
import io.obya.api.onboarding.domain.model.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.net.URI;
import java.util.function.Supplier;

@Data
@Accessors(chain = true, fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class State {
    SpecificationId id;
    URI source;
    Info info;
    Contract contract;
    Metadata metadata;
    Status status;
    Scorecard score;
    Object model;
    Supplier<String> body;
}
