package io.obya.api.onboarding.adapter.out.strapi;

import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import io.obya.api.onboarding.adapter.out.strapi.model.SpecificationPostSpecifications200Response;
import io.obya.api.onboarding.adapter.out.strapi.model.SpecificationPostSpecificationsRequest;
import io.obya.api.onboarding.adapter.out.strapi.model.SpecificationPostSpecificationsRequestData;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.ScoreSummary;
import io.obya.api.onboarding.domain.model.Specification;
import io.obya.api.onboarding.domain.model.SpecificationId;
import io.obya.common.util.Try;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.*;

public class StrapiRegistryRestAdapter implements Registry {

    private final SpecificationApi specificationApi;

    public StrapiRegistryRestAdapter(SpecificationApi specificationApi) {
        this.specificationApi = specificationApi;
    }

    @Override
    public Try<SpecificationId> register(Specification specification) {
        SpecificationPostSpecificationsRequest request = new SpecificationPostSpecificationsRequest().data(
                new SpecificationPostSpecificationsRequestData()
                        .specId(obfuscate(
                                "[%s-%s-%s]".formatted(
                                    specification.metadata().apiName(),
                                    specification.metadata().productName(),
                                    specification.info().version())))
                        .name(specification.metadata().apiName())
                        .version(specification.info().version())
                        .productName(specification.metadata().productName())
                        .bundleName(specification.metadata().bundleName())
                        .contract(SpecificationPostSpecificationsRequestData.ContractEnum.valueOf(
                                specification.contract().version().name()))
                        .body(specification.body().get())
                        .score(ScoreSummary.from(specification.score()))
                        .violations(specification.violations())
        );

        try {
            ResponseEntity<SpecificationPostSpecifications200Response> response =
                    specificationApi.specificationPostSpecifications(request);

            if (response.getStatusCode().is5xxServerError()) {
                throw new HttpServerErrorException(response.getStatusCode());
            }
            if (response.getStatusCode().is4xxClientError()) {
                throw new HttpClientErrorException(response.getStatusCode());
            }
            return Try.success(new SpecificationId(response.getBody().getData().getDocumentId()));

        } catch (ResourceAccessException e) {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (RestClientException e) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause().getMessage());
        }
    }

    private static String obfuscate(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }
}
