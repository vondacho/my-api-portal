package io.obya.api.onboarding.adapter.in.web;

import io.obya.api.onboarding.adapter.in.web.model.CandidateProcessed;
import io.obya.api.onboarding.appl.usecase.OnBoardingException;
import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.appl.usecase.RegistrationService;
import io.obya.api.onboarding.appl.usecase.model.Status;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@RestController
@RequestMapping("/registrations")
public class RegistrationRestController {

    private final RegistrationService registrationService;

    public RegistrationRestController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<CandidateProcessed> submit(@RequestBody Candidate candidate) {
        Try<State> state =  registrationService.submit(candidate.source());

        return state.map(s -> ResponseEntity
            .status(s.status() == Status.REGISTERED ? CREATED : OK)
            .body(CandidateProcessed.from(s, Violation.from(state.getExceptions()))))
                .getOrThrow(() -> new OnBoardingException(Violation.from(state.getExceptions())));
    }

}
