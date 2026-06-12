package io.obya.api.onboarding.adapter.in.web.exception;

import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class OpenApiExceptionHandler {

    private final OpenApiProblemMapper problemMapper = new OpenApiProblemMapper(new SmartbearProblemRegistry());

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handle(InvalidRequestException ex, WebRequest request) {
        final ProblemDetail pd = problemMapper.toProblem(ex.getValidationReport(), request);
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }
}
