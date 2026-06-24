# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`api-onboarding` is a Spring Boot 3 service (Java 25, Maven) that validates, scores, and registers API specifications into a Strapi-backed catalog. It is one module of the `myApiPortal` monorepo (siblings: `api-portal`, `api-registry`).

## Commands

```bash
# Build (skipping tests)
mvn package -DskipTests

# Run all tests (unit + BDD + e2e)
mvn test

# Run a single test class
mvn test -Dtest=RegistrationE2ETest

# Run a single test method
mvn test -Dtest=RegistrationE2ETest#submit_validOpenApiV30_registersSpecAndReturns201

# Run the application
mvn spring-boot:run
```

## Architecture

### Hexagonal (ports & adapters)

The codebase is a clean hexagonal architecture under `io.obya.api.onboarding`:

| Layer | Package | Responsibility |
|---|---|---|
| **Domain** | `domain.model` | Pure value objects: `Specification`, `Contract`, `Scorecard`, `Info`, `Metadata`, `Score` |
| **Application** | `appl.usecase` | `RegistrationService` orchestrates the pipeline; `appl.out` defines ports (`Registry`, `ScorerDelegate`) |
| **Inbound adapter** | `adapter.in.web` | REST controller, OpenAPI-validated requests, exception→RFC 9457 Problem mapping |
| **Outbound adapters** | `adapter.out.*` | Strapi (registry), Spectral (linter), Jentic; selected via `registry.adapter` / `scorer.adapter` properties |

### Processing pipeline

`RegistrationService.submit(URI)` drives a sequential `Flow.compositeProcessor` chain that stops on first `Failure`:

```
Receptionist → Parser → VersionEnforcer → Scorer → Overlayer
```

- **Receptionist** — reads the first line of the source URI to detect `Contract.Type` and `Contract.Version`
- **Parser** — dispatches to a version-specific parser (`OASV30Parser`, `AASV30Parser`, `GraphQLParser`, `WSDLParser`, …) that populates `State.info`, `State.metadata`, `State.model`
- **VersionEnforcer** — checks the registry for an existing spec with the same `apiName`/`productName`; auto-patches the version if it collides and records a `VERSION_AUTO_INCREMENTED` violation
- **Scorer** — calls `ScorerDelegate.score()`; on delegate failure degrades to `Partial` with `DEPENDENCY_NOT_AVAILABLE`; rejects specs below the configured threshold with `INSUFFICIENT_SCORING`
- **Overlayer** — applies an OAS Overlay document (Mustache-templated YAML) to the spec body in-memory

Each processor receives and returns `Try<State>` and never throws; failures are accumulated as `Violation.Failure` exceptions inside the monad.

### `Try<T>` monad

`io.obya.common.util.Try` is a custom sealed interface with three states:

- `Success<T>` — value present, no exceptions
- `Partial<T>` — value present **and** accumulated exceptions (pipeline continues but violations are recorded)
- `Failure<T>` — no value (pipeline stops)

Use `filter(..., strict=true)` to transition to `Failure`; `filter(..., strict=false)` (default) keeps the value and demotes to `Partial`.

### Contract detection

`Contract.Type` and `Contract.Version` are identified by regex against the first line of the file:

- OpenAPI: `openapi: 3.0.3 / 3.1.0 / 3.2.0`
- AsyncAPI: `asyncapi: 2.0.0 / 2.6.0 / 3.0.0`
- GraphQL schema, WSDL, OAS Overlay 1.0/1.1

### Outbound adapter selection

Adapters are Spring beans guarded by `@ConditionalOnProperty`:

| Property | Value | Bean |
|---|---|---|
| `registry.adapter` | `strapi` (default) | `ResilientStrapiRegistryRestAdapter` (circuit-breaker + retry via Resilience4j) |
| `registry.adapter` | `dummy` | in-memory no-op (used in tests via `application-e2e.yaml`) |
| `scorer.adapter` | `dummy` (default) | returns a fixed `Scorecard(74, FC=99)` |

The Strapi client is generated at build time by `openapi-generator-maven-plugin` from `src/main/resources/openapi/strapi_specification_v4.json` using the `spring-http-interface` library. Generated code lands in `io.obya.api.onboarding.adapter.out.strapi.{api,model}`.

### Overlays

Five OAS Overlay YAML files under `src/main/resources/overlays/` are applied post-scoring to enrich specs:
- `overlay_scores.yaml` — injects the numeric score
- `overlay_violations.yaml` — injects violation list
- `overlay_metrics.yaml`, `overlay_rbac_abac.yaml`, `overlay_samples.yaml`

The `OverlayProcessor` (from `com.ibm.oas.overlay`) evaluates JSONPath `actions[].target` expressions from the overlay against the spec body. The `OverlayV10Parser` supplies a Mustache context map built from `State` and accumulated exceptions.

## Testing strategy

| Test type | Class(es) | Notes |
|---|---|---|
| Unit | `TryTest`, `ParseAndValidate*Test` | Plain JUnit 5, no Spring context |
| BDD / functional | `RunRegistrationCucumberTest` + `RegistrationServiceSteps` | Cucumber-JVM against `RegistrationService` directly; `registry.adapter=dummy`, `scorer.adapter=dummy` |
| E2E (full Spring) | `RegistrationE2ETest`, `StrapiRegistryE2ETest`, `RegistrationRestAssuredE2ETest` | `@SpringBootTest(RANDOM_PORT)`; `Registry` mocked via `@MockitoBean`; real HTTP via `TestRestTemplate` / RestAssured |

The `application-e2e.yaml` test profile sets both adapters to `dummy` so no external services are needed.

BDD feature file: `src/test/resources/io/obya/api/onboarding/bdd/registration.feature`

## Key configuration

`src/main/resources/application.yaml` notable defaults:
- `registry.adapter=strapi`, `strapi.base-url=http://localhost:1337/api`
- `scorer.adapter=dummy`
- Resilience4j circuit-breaker on the `strapi` instance (failure threshold 50 %, 10-call window)
- Spring MVC Problem Details enabled (`spring.mvc.problemdetails.enabled=true`)

The `RegistrationConfig` bean wires hard-coded absolute `file://` paths for overlay documents — these must be updated if the repo is checked out at a different path.
