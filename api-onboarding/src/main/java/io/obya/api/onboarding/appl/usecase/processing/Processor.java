package io.obya.api.onboarding.appl.usecase.processing;

import io.obya.common.util.Try;

public interface Processor<STATE> {
    Try<STATE> process(Try<STATE> in);
}
