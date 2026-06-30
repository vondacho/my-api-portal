package io.obya.api.onboarding.adapter.out.strapi;

import io.obya.api.onboarding.adapter.out.strapi.api.SpecificationApi;
import io.obya.api.onboarding.adapter.out.strapi.model.*;
import io.obya.api.onboarding.appl.out.Registry;
import io.obya.api.onboarding.domain.model.*;
import io.obya.common.util.Try;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

public class StrapiRegistryRestAdapter implements Registry {

    private static final String NO_POPULATED_RELATION = null;
    private static final String NO_QUERY = null;
    private static final List<String> NO_PROJECTION = List.of();
    private static final Map<String, Object> NO_FILTER = Map.of();
    private static final String PUBLISHED_DOCUMENTS = "published";
    private static final Function<String, String> DESC_OF = attr -> attr + ":desc";
    private static final Function<String, String> ASC_OF = attr -> attr + ":asc";

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
                        .version(specification.info().version().format())
                        .productName(specification.metadata().productName())
                        .bundleName(specification.metadata().bundleName())
                        .contract(SpecificationPostSpecificationsRequestData.ContractEnum.valueOf(
                                specification.contract().version().name()))
                        .body(specification.body())
                        .score(ScoreSummary.from(specification.score()))
                        .violations(specification.violations())
        );
        return execute(() -> specificationApi.specificationPostSpecifications(
                        NO_PROJECTION,
                        NO_POPULATED_RELATION,
                        PUBLISHED_DOCUMENTS,
                        null,
                        null,
                        request),
                body -> Try.success(new SpecificationId(body.getData().getDocumentId())));
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
                        .version(specification.info().version().format())
                        .productName(specification.metadata().productName())
                        .bundleName(specification.metadata().bundleName())
                        .contract(SpecificationPutSpecificationsByIdRequestData.ContractEnum.valueOf(
                                specification.contract().version().name()))
                        .body(specification.body())
                        .score(ScoreSummary.from(specification.score()))
                        .violations(specification.violations())
        );
        return execute(() -> specificationApi.specificationPutSpecificationsById(
                specification.id().id(),
                        NO_PROJECTION,
                        NO_POPULATED_RELATION,
                        PUBLISHED_DOCUMENTS,
                        null,
                        null,
                        request),
                body -> Try.success(new SpecificationId(body.getData().getDocumentId())));
    }

    @Override
    public Try<Specification> at(SpecificationId id, String... attributes) {
        return execute(() -> specificationApi.specificationGetSpecificationsById(
                id.id(),
                Arrays.stream(attributes).toList(),
                NO_POPULATED_RELATION,
                NO_FILTER,
                List.of(DESC_OF.apply("revision")),
                PUBLISHED_DOCUMENTS, null, null
                ),
                body -> Try.ofOptional(() -> ofNullable(
                        body.getData()).map(it ->
                                new Specification(
                                        new Info("empty", "empty", Version.from(it.getVersion())),
                                        Contract.from(Contract.Version.valueOf(it.getContract().getValue())),
                                        new Metadata(
                                                it.getName(),
                                                Revision.from(it.getRevision()),
                                                it.getBundleName(),
                                                it.getProductName(),
                                                it.getComponentName(),
                                                Revision.from(it.getComponentRevision())),
                                        Scorecard.undefined(),
                                        "",
                                        List.of(),
                                        new SpecificationId(it.getDocumentId())))
                        ));
    }

    @Override
    public Try<Specification> latestAt(String name, String productName, Version version, String... attributes) {
        var filters = new HashMap<String, Object>(Map.of("name", name, "productName", productName, "version", version.toString()));
        return execute(() -> specificationApi.specificationGetSpecifications(
                Arrays.stream(attributes).toList(),
                filters,
                NO_QUERY,
                List.of(DESC_OF.apply("revision")),
                NO_POPULATED_RELATION,
                PUBLISHED_DOCUMENTS, null, null),
                body -> Try.ofOptional(() ->
                        body.getData().stream().map(it ->
                        new Specification(
                                new Info("empty", "empty", Version.from(it.getVersion())),
                                Contract.from(Contract.Version.valueOf(it.getContract().getValue())),
                                new Metadata(
                                        it.getName(),
                                        Revision.from(it.getRevision()),
                                        it.getBundleName(),
                                        it.getProductName(),
                                        it.getComponentName(),
                                        Revision.from(it.getComponentRevision())),
                                Scorecard.undefined(),
                                "",
                                List.of(),
                                new SpecificationId(it.getDocumentId())))
                        .findFirst()));
    }

    @Override
    public Try<Specification> revisionAt(String name, String productName, Version version, Revision revision, String... attributes) {
        return execute(() -> specificationApi.specificationGetSpecifications(
                        Arrays.stream(attributes).toList(),
                        Map.of("name", name,
                                "productName", productName,
                                "version", version.toString(),
                                "revision", revision.semver().toString()),
                        NO_QUERY,
                        List.of(DESC_OF.apply("revision")),
                        NO_POPULATED_RELATION,
                        PUBLISHED_DOCUMENTS, null, null),
                body -> Try.ofOptional(() ->
                        body.getData().stream().map(it ->
                                        new Specification(
                                                new Info("empty", "empty", Version.from(it.getVersion())),
                                                Contract.from(Contract.Version.valueOf(it.getContract().getValue())),
                                                new Metadata(
                                                        it.getName(),
                                                        Revision.from(it.getRevision()),
                                                        it.getBundleName(),
                                                        it.getProductName(),
                                                        it.getComponentName(),
                                                        Revision.from(it.getComponentRevision())),
                                                Scorecard.undefined(),
                                                "",
                                                List.of(),
                                                new SpecificationId(it.getDocumentId())))
                                .findFirst()));
    }

    private static <T,R> Try<R> execute(Supplier<ResponseEntity<T>> request, Function<T, Try<R>> packaging) {
        try {
            ResponseEntity<T> response = request.get();

            if (response.getStatusCode().is5xxServerError()) {
                throw new HttpServerErrorException(response.getStatusCode());
            }
            if (response.getStatusCode().is4xxClientError()) {
                throw new HttpClientErrorException(response.getStatusCode());
            }
            return packaging.apply(response.getBody());

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
