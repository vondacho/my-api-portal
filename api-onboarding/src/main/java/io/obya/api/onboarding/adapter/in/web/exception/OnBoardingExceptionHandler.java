package io.obya.api.onboarding.adapter.in.web.exception;

import io.obya.api.onboarding.appl.usecase.OnBoardingException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class OnBoardingExceptionHandler {

    private final OnBoardingProblemMapper onBoarding = new OnBoardingProblemMapper(new SmartbearProblemRegistry());

    @ExceptionHandler(OnBoardingException.class)
    public ResponseEntity<ProblemDetail> handle(OnBoardingException ex, WebRequest request) {
        final ProblemDetail pd = onBoarding.toProblem(ex.getFailures(), request);
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }
}
