package io.obya.api.onboarding.adapter.in.web;

import io.obya.api.onboarding.adapter.in.web.model.CandidateProcessed;
import io.obya.api.onboarding.appl.usecase.OnBoardingException;
import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.appl.usecase.RegistrationService;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.api.onboarding.domain.model.*;
import io.obya.common.util.Try;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@RestController
public class RegistrationRestController implements RegistrationApi {

    private final RegistrationService registrationService;

    public RegistrationRestController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public ResponseEntity<CandidateProcessed> submit(Candidate candidate) {
        Try<State> state =  registrationService.submit(candidate.source());
        return state.map(s -> ResponseEntity
            .status(s.status() == Status.REGISTERED ? CREATED : OK)
            .body(CandidateProcessed.from(s, Violation.from(state.getExceptions()))))
                .getOrThrow(() -> new OnBoardingException(Violation.from(state.getExceptions())));
    }

    @Override
    public ResponseEntity<CandidateProcessed> upgrade(SpecificationId id, Candidate candidate) {
        Try<State> state =  registrationService.upgrade(id, candidate.source());
        return state.map(s -> ResponseEntity
                        .status(s.status() == Status.REGISTERED ? CREATED : OK)
                        .body(CandidateProcessed.from(s, Violation.from(state.getExceptions()))))
                .getOrThrow(() -> new OnBoardingException(Violation.from(state.getExceptions())));
    }

    @Override
    public void score(SpecificationId id, Scorecard scorecard) {

    }

    @Override
    public void implement(SpecificationId id, Implementation implementation) {

    }

    @Override
    public void overlay(SpecificationId id, Candidate overlay) {

    }

    @Override
    public CandidateProcessed get(SpecificationId id) {
        return null;
    }

    @Override
    public ScoreSummary getScore(SpecificationId id) {
        return null;
    }
}
