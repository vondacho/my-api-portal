package io.obya.api.onboarding.appl.parsing;

import io.openapiparser.OpenApiParser;
import io.openapiparser.model.v30.OpenApi;
import io.openapiprocessor.interfaces.Converter;
import io.openapiprocessor.interfaces.Reader;
import io.openapiprocessor.jsonschema.reader.UriReader;
import io.openapiprocessor.jsonschema.schema.DocumentLoader;
import io.openapiprocessor.jsonschema.schema.DocumentStore;
import io.openapiprocessor.snakeyaml.SnakeYamlConverter;

import java.nio.file.Path;

public class OpenApiProcessor {

    public OpenApi parse(Path specification) {
        Reader reader = new UriReader();
        Converter converter = new SnakeYamlConverter();
        DocumentLoader loader = new DocumentLoader(reader, converter);

        DocumentStore documents = new DocumentStore();
        OpenApiParser parser = new OpenApiParser(documents, loader);

        var openApiResult = parser.parse(specification.toString());
        return openApiResult.getModel(OpenApi.class);
    }

}
