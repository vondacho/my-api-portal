package io.obya.api.onboarding.appl.usecase.config;

import io.obya.api.onboarding.appl.out.ScorerDelegate;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.appl.usecase.RegistrationService;
import io.obya.api.onboarding.domain.model.Violation;
import io.obya.api.onboarding.appl.usecase.processing.*;
import io.obya.api.onboarding.appl.usecase.processing.aas.AASV20Parser;
import io.obya.api.onboarding.appl.usecase.processing.aas.AASV26Parser;
import io.obya.api.onboarding.appl.usecase.processing.aas.AASV30Parser;
import io.obya.api.onboarding.appl.usecase.processing.oai.OverlayV10Parser;
import io.obya.api.onboarding.appl.usecase.processing.oas.OASV30Parser;
import io.obya.api.onboarding.appl.usecase.processing.oas.OASV31Parser;
import io.obya.api.onboarding.appl.usecase.processing.oas.OASV32Parser;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIFileReader;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIHttpReader;
import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.domain.model.Contract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Map;

@Configuration
public class RegistrationConfig {

    private final URIReader[] readers = { new URIFileReader(), new URIHttpReader() };

    @Bean
    public RegistrationService registrationService(Registry registry, ScorerDelegate remoteScorer) {
        return new RegistrationService(
            receptionist(),
            parser(),
            scorer(remoteScorer),
            scoreOverlayer(),
            versionEnforcer(registry),
            registry);
    }

    public Receptionist receptionist() {
        return new Receptionist(readers);
    }

    public Scorer scorer(ScorerDelegate scorer) {
        return new Scorer(scorer);
    }

    public Parser parser() {
        return new Parser(Map.of(
                Contract.Version.OPENAPI_V30, () -> new OASV30Parser(readers),
                Contract.Version.OPENAPI_V31, () -> new OASV31Parser(readers),
                Contract.Version.OPENAPI_V32, () -> new OASV32Parser(readers),
                Contract.Version.ASYNCAPI_V20, () -> new AASV20Parser(readers),
                Contract.Version.ASYNCAPI_V26, () -> new AASV26Parser(readers),
                Contract.Version.ASYNCAPI_V30, () -> new AASV30Parser(readers)));
    }

    public Revisor versionEnforcer(Registry registry) {
        return new Revisor(registry);
    }

    public Overlayer scoreOverlayer() {
        return new Overlayer(URI.create("file:///Users/olivier/Labor/github/my-api-portal/api-onboarding/src/main/resources/overlays/overlay_scores.yaml"),
                new OverlayV10Parser(readers, (state, _) -> Map.of(
                    "score", state.score()
                )));
    }

    public Overlayer violationOverlayer() {
        return new Overlayer(URI.create("file:///Users/olivier/Labor/github/my-api-portal/api-onboarding/src/main/resources/overlays/overlay_violations.yaml"),
                new OverlayV10Parser(readers, (_, exceptions) -> Map.of(
                "violations", Violation.from(exceptions)
                )));
    }

    public Overlayer metricOverlayer() {
        return new Overlayer(URI.create("file:///Users/olivier/Labor/github/my-api-portal/api-onboarding/src/main/resources/overlays/overlay_metrics.yaml"),
                new OverlayV10Parser(readers, (_,_) -> Map.of()));
    }

    public Overlayer rbacAbacOverlayer() {
        return new Overlayer(URI.create("file:///Users/olivier/Labor/github/my-api-portal/api-onboarding/src/main/resources/overlays/overlay_rbac_abac.yaml"),
                new OverlayV10Parser(readers, (_,_) -> Map.of()));
    }

    public Overlayer samplesOverlayer() {
        return new Overlayer(URI.create("file:///Users/olivier/Labor/github/my-api-portal/api-onboarding/src/main/resources/overlays/overlay_samples.yaml"),
                new OverlayV10Parser(readers, (_,_) -> Map.of()));
    }
}
