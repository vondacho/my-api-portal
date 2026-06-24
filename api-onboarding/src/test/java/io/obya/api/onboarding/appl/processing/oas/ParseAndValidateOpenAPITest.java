package io.obya.api.onboarding.appl.processing.oas;

import io.openapiparser.*;
import io.openapiparser.model.v30.OpenApi;
import io.openapiprocessor.interfaces.Converter;
import io.openapiprocessor.interfaces.Reader;
import io.openapiprocessor.jackson.JacksonConverter;
import io.openapiprocessor.jsonschema.reader.UriReader;
import io.openapiprocessor.jsonschema.schema.*;
import io.openapiprocessor.jsonschema.validator.Validator;
import io.openapiprocessor.jsonschema.validator.ValidatorSettings;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * https://github.com/openapi-processor/openapi-parser
 */
public class ParseAndValidateOpenAPITest {

    @Test
    void parseAndValidate () throws URISyntaxException {
        // 1. create a document loader.
        // It loads a document by uri and converts it to a Map<String, Object>
        // object tree that represents the OpenAPI document. The parser
        // operates on that Object tree which makes it independent of the
        // object mapper (e.g. jackson, snakeyaml etc.).
        // Both (Reader and Converter) have a very simple interface which makes
        // it easy to implement your own.
        Reader reader = new UriReader();
        Converter converter = new JacksonConverter();
        DocumentLoader loader = new DocumentLoader(reader, converter);

        // 2. create a parser.
        DocumentStore documents = new DocumentStore();
        OpenApiParser parser = new OpenApiParser(documents, loader);

        // 3. parse the OpenAPI from resource or url.
        // here it loads an OpenAPI document from a resource file, but URI works too.
        OpenApiResult openApiResult = parser.parse("/oas/openapi.yaml");
        System.out.println(documents.getDocuments());
        System.out.println(openApiResult.bundle());

        // 4. get the API specModel from the result to navigate the OpenAPI document.
        // OpenAPI 3.1.x with specModel.v31.OpenAPI import
        OpenApi model = openApiResult.getModel(OpenApi.class);

        // 5. navigate the specModel
        assertThat(model.getInfo().getExtensions().get("x-product-slug")).isEqualTo("personal-training");
        assertThat(model.getInfo().getExtensions().get("x-api-slug")).isEqualTo("booking");
        assertThat(Objects.requireNonNull(Objects.requireNonNull(model.getPaths().getPathItem("/sessions")).getOperations().get("get").getDescription())).isEqualTo("Gets all sessions planned in an interval of dates.");
        assertThat(Objects.requireNonNull(Objects.requireNonNull(model.getPaths().getPathItem("/sessions")).getOperations().get("get").getTags())).containsExactly("session");

        // 6. create Validator to validate the OpenAPI schema.
        SchemaStore store = new SchemaStore(loader);
        ValidatorSettings settings = new ValidatorSettings();
        Validator validator = new Validator(settings);

        // 7. validate the OpenAPI schema.
        boolean valid = openApiResult.validate(validator, store);
        assertThat(valid).as("Specification is valid:%s%n".formatted(valid)).isTrue();

        // 8. print validation errors
        Collection<ValidationError> errors = openApiResult.getValidationErrors();
        ValidationErrorTextBuilder builder = new ValidationErrorTextBuilder();

        for (ValidationError error : errors) {
            System.out.println(builder.getText(error));
        }

        // 9. parse an overlay
        var overlayParser = new OverlayParser(new DocumentStore(), loader);
        var overlayResult = overlayParser.parse("/oas/overlay.yaml");

        var shemaStore = new SchemaStore(loader);
        valid = overlayResult.validate(validator, shemaStore);
        assertThat(valid).as("Overlay is valid:%s%n".formatted(valid)).isTrue();

        for (ValidationError error : overlayResult.getValidationErrors()) {
            System.out.println(new ValidationErrorTextBuilder().getText(error));
        }

        // 10. apply an overlay
        var overlaid = openApiResult.apply(overlayResult);

        // 11. navigate the specModel
        Scope scope = Scope.empty();
        var overlaidModel = new OpenApi(
                new Context(scope, new ReferenceRegistry()),
                new Bucket(scope, overlaid));

        assertThat(overlaidModel.getInfo().getExtensions().get("x-metrics-sli")).isEqualTo("200ms");
        assertThat(overlaidModel.getInfo().getExtensions().get("x-metrics-slo")).isEqualTo("200ms");
    }
}
