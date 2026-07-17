package io.obya.api.onboarding.adapter.out.strapi;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import io.obya.api.onboarding.domain.model.Version;
import io.obya.api.onboarding.domain.model.Violation;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
public class ResilientStrapiRegistryRestAdapter extends StrapiRegistryRestAdapter {

    public ResilientStrapiRegistryRestAdapter(SpecificationApi specificationApi) {
       super(specificationApi);
    }

    @CircuitBreaker(name = "strapi", fallbackMethod = "registerFallback")
    @Bulkhead(name = "strapi")
    @Retry(name = "strapi")
    @Override
    public Try<SpecificationId> register(Specification specification) {
        return super.register(specification);
    }

    Try<SpecificationId> registerFallback(HttpServerErrorException e) {
        if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)
            return Try.failure(Violation.Code.DEPENDENCY_NOT_AVAILABLE.failure("registry", e).get());
        return Try.failure(Violation.Code.DEPENDENCY_INTERNAL_ERROR.failure("registry", e).get());
    }

    Try<SpecificationId> registerFallback(HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
            return Try.failure(Violation.Code.DEPENDENCY_RESPONSE_NOT_READABLE.failure("registry", e).get());
        return Try.failure(Violation.Code.DEPENDENCY_BAD_REQUEST.failure("registry", e).get());
    }

    @CircuitBreaker(name = "strapi", fallbackMethod = "atFallback")
    @Bulkhead(name = "strapi")
    @Retry(name = "strapi")
    @Override
    public Try<Specification> at(SpecificationId id, String... attributes) {
        return super.at(id, attributes);
    }

    Try<Specification> atFallback(HttpServerErrorException e) {
        if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)
            return Try.failure(Violation.Code.DEPENDENCY_NOT_AVAILABLE.failure("registry", e).get());
        return Try.failure(Violation.Code.DEPENDENCY_INTERNAL_ERROR.failure("registry", e).get());
    }

    @CircuitBreaker(name = "strapi", fallbackMethod = "latestAtFallback")
    @Bulkhead(name = "strapi")
    @Retry(name = "strapi")
    @Override
    public Try<Specification> latestAt(String name, String productName, Version version, String... attributes) {
        try {
            return super.latestAt(name, productName, version, attributes);
        } catch (Exception e) {
            return Try.failure(Violation.Code.DEPENDENCY_INTERNAL_ERROR.failure("registry", e).get());
        }
    }

    Try<Specification> latestAtFallback(HttpServerErrorException e) {
        if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)
            return Try.failure(Violation.Code.DEPENDENCY_NOT_AVAILABLE.failure("registry", e).get());
        return Try.failure(Violation.Code.DEPENDENCY_INTERNAL_ERROR.failure("registry", e).get());
    }
}
