package io.obya.api.onboarding.adapter.out.strapi;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import io.obya.api.onboarding.appl.usecase.model.Violation;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@ConditionalOnProperty(name = "registry.adapter", havingValue = "strapi")
@Component(value = "strapi")
@Slf4j
public class ResilientStrapiRegistryRestAdapter extends StrapiRegistryRestAdapter {

    public ResilientStrapiRegistryRestAdapter(SpecificationApi specificationApi) {
       super(specificationApi);
    }

    @CircuitBreaker(name = "strapi", fallbackMethod = "fallback")
    @Bulkhead(name = "strapi")
    @Retry(name = "strapi")
    @Override
    public Try<SpecificationId> register(Specification specification) {
        return super.register(specification);
    }

    Try<SpecificationId> fallback(HttpServerErrorException e) {
        if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)
            return Try.failure(Violation.Code.DEPENDENCY_NOT_AVAILABLE.failure(e).get());
        return Try.failure(Violation.Code.DEPENDENCY_INTERNAL_ERROR.failure(e).get());
    }

    Try<SpecificationId> fallback(HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
            return Try.failure(Violation.Code.DEPENDENCY_RESPONSE_NOT_READABLE.failure(e).get());
        return Try.failure(Violation.Code.DEPENDENCY_BAD_REQUEST.failure(e).get());
    }
}
