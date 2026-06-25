package io.obya.api.onboarding.appl.usecase.processing.graphql;

import io.obya.api.onboarding.appl.usecase.processing.Processor;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import static io.obya.api.onboarding.domain.model.Violation.Code.MISSING_DATA;
import static io.obya.api.onboarding.appl.usecase.processing.Validator.nonNull;

public class GraphQLParser implements Processor<State> {

    @Override
    public Try<State> process(Try<State> state) {
        return state
            .filter(s -> nonNull(s::source), MISSING_DATA.failure( "state.source"), true)
            .filter(s -> nonNull(s::contract), MISSING_DATA.failure( "state.contract"), true);
    }
}
