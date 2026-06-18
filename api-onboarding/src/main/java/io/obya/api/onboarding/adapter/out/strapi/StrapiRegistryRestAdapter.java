package io.obya.api.onboarding.adapter.out.strapi;

import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import io.obya.api.onboarding.adapter.out.strapi.model.*;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.*;
import io.obya.common.util.Try;
import org.semver4j.Semver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.obya.api.onboarding.appl.usecase.model.Violation.Code.RESOURCE_NOT_FOUND;
import static java.util.Objects.nonNull;

public class StrapiRegistryRestAdapter implements Registry {

    private final SpecificationApi specificationApi;

    public StrapiRegistryRestAdapter(SpecificationApi specificationApi) {
        this.specificationApi = specificationApi;
    }

    @Override
    public Try<SpecificationId> register(Specification specification) {
        return nonNull(specification.id()) ? update(specification) : create(specification);
    }

    private Try<SpecificationId> create(Specification specification) {
        SpecificationPostSpecificationsRequest request = new SpecificationPostSpecificationsRequest().data(
                new SpecificationPostSpecificationsRequestData()
                        .specId(obfuscate(
                                "[%s-%s-%s]".formatted(
                                        specification.metadata().apiName(),
                                        specification.metadata().productName(),
                                        specification.info().version())))
                        .name(specification.metadata().apiName())
                        .version(specification.info().version().getVersion())
                        .productName(specification.metadata().productName())
                        .bundleName(specification.metadata().bundleName())
                        .contract(SpecificationPostSpecificationsRequestData.ContractEnum.valueOf(
                                specification.contract().version().name()))
                        .body(specification.body())
                        .score(ScoreSummary.from(specification.score()))
                        .violations(specification.violations())
        );
        return execute(() -> specificationApi.specificationPostSpecifications(request),
                body -> new SpecificationId(body.getData().getDocumentId()));
    }

    private Try<SpecificationId> update(Specification specification) {
        SpecificationPutSpecificationsByIdRequest request = new SpecificationPutSpecificationsByIdRequest().data(
                new SpecificationPutSpecificationsByIdRequestData()
                        .specId(obfuscate(
                                "[%s-%s-%s]".formatted(
                                        specification.metadata().apiName(),
                                        specification.metadata().productName(),
                                        specification.info().version())))
                        .name(specification.metadata().apiName())
                        .version(specification.info().version().getVersion())
                        .productName(specification.metadata().productName())
                        .bundleName(specification.metadata().bundleName())
                        .contract(SpecificationPutSpecificationsByIdRequestData.ContractEnum.valueOf(
                                specification.contract().version().name()))
                        .body(specification.body())
                        .score(ScoreSummary.from(specification.score()))
                        .violations(specification.violations())
        );
        return execute(() -> specificationApi.specificationPutSpecificationsById(specification.id().id(), request),
                body -> new SpecificationId(body.getData().getDocumentId()));
    }

    @Override
    public Try<Specification> specificationAt(SpecificationId id, String...attributes) {
        return execute(() -> specificationApi.specificationGetSpecificationsById(id.id()),
                body -> new Specification(
                        new Info(
                                "title",
                                "description",
                                Semver.parse(body.getData().getVersion())),
                        Contract.from(Contract.Version.valueOf(body.getData().getContract().name())),
                        new Metadata(
                                body.getData().getName(),
                                body.getData().getBundleName(),
                                body.getData().getProductName(),
                                Metadata.META_COMPONENT_NAME_KEY,
                                null),
                        Scorecard.undefined(), //TODO
                        body.getData().getBody().toString(),
                        Collections.emptyList(), //TODO
                        id)
                );
    }

    @Override
    public Try<Specification> specificationAt(String name, String product, Semver version, String... attributes) {
        return new Try.Failure<>(List.of(RESOURCE_NOT_FOUND.failure( "Specification",
                "[%s-%s-%s]".formatted(name, product, version)).get()));
    }

    private static <T,R> Try<R> execute(Supplier<ResponseEntity<T>> request, Function<T, R> packaging) {
        try {
            ResponseEntity<T> response = request.get();

            if (response.getStatusCode().is5xxServerError()) {
                throw new HttpServerErrorException(response.getStatusCode());
            }
            if (response.getStatusCode().is4xxClientError()) {
                throw new HttpClientErrorException(response.getStatusCode());
            }
            return Try.success(packaging.apply(response.getBody()));

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
