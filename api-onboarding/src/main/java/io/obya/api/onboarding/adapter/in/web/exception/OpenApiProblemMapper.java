package io.obya.api.onboarding.adapter.in.web.exception;

import com.atlassian.oai.validator.report.ValidationReport;

import java.util.HashMap;
import java.util.Map;

public class OpenApiProblemMapper extends ProblemMapper<ValidationReport> {

    public OpenApiProblemMapper(ProblemRegistry problemRegistry) {
        super(problemRegistry);
    }

     @Override
     protected ProblemRegistry.ProblemSample defaultSample(ValidationReport report) {
        return problemRegistry.requestIsNotValid();
     }

    @Override
    public Map<String, Object> standardErrors(ValidationReport report) {
        var errors = new HashMap<String, Object>();
        var items = report.getMessages();
        errors.put("errors", items.stream()
                .map(m -> Map.of("level", m.getLevel(), "detail", m.getMessage()))
                .toList());
        return errors;
    }
}
