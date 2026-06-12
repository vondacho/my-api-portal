package io.obya.api.onboarding.adapter.in.web.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProblemMapper<CAUSE> {

   protected final ProblemRegistry problemRegistry;

   public ProblemMapper(ProblemRegistry problemRegistry) {
      this.problemRegistry = problemRegistry;
   }

   public ProblemDetail toProblem(CAUSE cause, WebRequest request) {
        return toProblem(cause, toInstance(cause, request));
    }

    public ProblemDetail toProblem(CAUSE cause, URI instance) {
        var pd = fromSample(defaultSample(cause));
        pd.setInstance(instance);
        pd.setProperties(standardErrors(cause));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    protected ProblemRegistry.ProblemSample defaultSample(CAUSE cause) {
       return problemRegistry.badRequest();
    }

    protected ProblemDetail fromSample(ProblemRegistry.ProblemSample problemSample) {
        var pd = ProblemDetail.forStatus(problemSample.code());
        pd.setDetail(problemSample.detail());
        pd.setType(problemSample.type());
        pd.setTitle(problemSample.title());
        return pd;
    }

    public URI toInstance(CAUSE cause, WebRequest request) {
        return URI.create(request.getDescription(false));
    }

    public Map<String, Object> standardErrors(CAUSE cause) {
        var errors = new HashMap<String, Object>();
        errors.put("errors", List.of(Map.of("detail", cause.toString())));
        return errors;
    }
}
