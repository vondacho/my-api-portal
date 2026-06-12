package io.obya.api.onboarding.adapter.in.web.exception;

import org.springframework.http.HttpStatusCode;

import java.net.URI;

public interface ProblemRegistry {

   record ProblemSample(URI type, String title, String detail, HttpStatusCode code) {}

   ProblemSample badRequest();

   ProblemSample requestIsNotValid();

   ProblemSample dependencyUnavailable();
}
