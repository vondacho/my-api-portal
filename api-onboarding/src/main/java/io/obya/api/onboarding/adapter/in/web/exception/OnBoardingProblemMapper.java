package io.obya.api.onboarding.adapter.in.web.exception;

import io.obya.api.onboarding.domain.model.Violation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnBoardingProblemMapper extends ProblemMapper<List<Violation>> {

    public OnBoardingProblemMapper(ProblemRegistry problemRegistry) {
        super(problemRegistry);
    }

    @Override
    protected ProblemRegistry.ProblemSample defaultSample(List<Violation> failures) {
       return super.defaultSample(failures);
    }

    @Override
    public Map<String, Object> standardErrors(List<Violation> failures) {
        var errors = new HashMap<String, Object>();
        errors.put("errors", failures.stream()
                .map(v -> Map.of("severity", v.severity(), "detail", v.detail()))
                .toList());
        return errors;
    }
}
