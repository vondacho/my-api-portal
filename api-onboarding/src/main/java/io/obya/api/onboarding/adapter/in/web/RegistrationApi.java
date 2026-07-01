package io.obya.api.onboarding.adapter.in.web;

import io.obya.api.onboarding.adapter.in.web.model.Candidate;
import io.obya.api.onboarding.adapter.in.web.model.CandidateProcessed;
import io.obya.api.onboarding.domain.model.Implementation;
import io.obya.api.onboarding.domain.model.ScoreSummary;
import io.obya.api.onboarding.domain.model.Scorecard;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@OpenAPIDefinition(
        info = @Info(
                title = "API Onboarding - Registration API",
                description = "Minimal API surface for submitting candidates specification.",
                contact = @Contact(name = "API Onboarding Team"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT"),
                extensions = { @Extension(properties = {
                        @ExtensionProperty(name = "api-name", value = "registration"),
                        @ExtensionProperty(name = "product-name", value = "api"),
                        @ExtensionProperty(name = "bundle-name", value = "api-portal") })} )
)
@Tag(name = "registrations", description = "Operations defined for api-onboarding")

@RequestMapping("/registrations")
public interface RegistrationApi {

    @Operation(
            operationId = "submitCandidate",
            summary = "Submit a Candidate",
            description = "Accepts a Candidate payload describing a specification source to be processed.",
            tags = {"registrations"}
    )
    @PostMapping
    ResponseEntity<CandidateProcessed> submit(
            @RequestBody Candidate candidate);

    @Operation(
            operationId = "scoreSpecification",
            summary = "Record a quality score for a specification",
            description = "Attaches a Scorecard to an existing specification resource.",
            tags = {"registrations"}
    )
    @PutMapping("/{id}/score")
    void score(
            @Parameter(description = "ID of the specification being scored", required = true, schema = @Schema(type = "string"))
            @PathVariable SpecificationId id,
            @RequestBody Scorecard scorecard);

    @Operation(
            operationId = "implementSpecification",
            summary = "Attach an implementation to a specification",
            description = "Associates a component Implementation with an existing specification resource.",
            tags = {"registrations"}
    )
    @PutMapping("/{id}/component")
    void implement(
            @Parameter(description = "ID of the specification being implemented", required = true, schema = @Schema(type = "string"))
            @PathVariable SpecificationId id,
            @RequestBody Implementation implementation);

    @Operation(
            operationId = "overlaySpecification",
            summary = "Apply an overlay to a specification",
            description = "Accepts a Candidate payload describing an overlay source to be applied to an existing specification resource.",
            tags = {"registrations"}
    )
    @PutMapping("/{id}/overlay")
    void overlay(
            @Parameter(description = "ID of the specification receiving the overlay", required = true, schema = @Schema(type = "string"))
            @PathVariable SpecificationId id,
            @RequestBody Candidate overlay);

    @Operation(
            operationId = "getSpecification",
            summary = "Get a specification resource",
            description = "Returns the current state of a registered specification.",
            tags = {"registrations-extended"}
    )
    @GetMapping("/{id}")
    CandidateProcessed get(
            @Parameter(description = "ID of the specification to retrieve", required = true, schema = @Schema(type = "string"))
            @PathVariable SpecificationId id);

    @Operation(
            operationId = "getSpecificationScore",
            summary = "Get the score summary for a specification",
            description = "Returns the current score summary for a registered specification.",
            tags = {"registrations"}
    )
    @GetMapping("/{id}/score")
    ScoreSummary getScore(
            @Parameter(description = "ID of the specification whose score is requested", required = true, schema = @Schema(type = "string"))
            @PathVariable SpecificationId id);
}
