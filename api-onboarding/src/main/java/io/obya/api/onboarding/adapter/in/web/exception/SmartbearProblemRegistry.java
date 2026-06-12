package io.obya.api.onboarding.adapter.in.web.exception;

import java.net.URI;

import static org.springframework.http.HttpStatus.*;

public class SmartbearProblemRegistry implements ProblemRegistry {

   @Override
   public ProblemSample badRequest() {
      return new ProblemSample(
              URI.create("https://problems-registry.smartbear.com/bad-request"),
              "Bad Request",
              "The request is invalid or malformed.",
              BAD_REQUEST
      );
   }

   @Override
   public ProblemSample requestIsNotValid() {
      return new ProblemSample(
         URI.create("https://problems-registry.smartbear.com/validation-error"),
         "Validation Error",
         "The request is not valid.",
              UNPROCESSABLE_ENTITY
      );
   }

   @Override
   public ProblemSample dependencyUnavailable() {
      return new ProblemSample(
              URI.create("https://problems-registry.smartbear.com/dependency-unavailable"),
              "Dependency Unavailable",
              "The operation could not be completed because a required external service timed out.",
              SERVICE_UNAVAILABLE
      );
   }
}
