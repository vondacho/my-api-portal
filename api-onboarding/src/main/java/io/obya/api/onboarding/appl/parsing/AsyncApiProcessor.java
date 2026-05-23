package io.obya.api.onboarding.appl.parsing;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsyncApiProcessor {

    public AsyncAPI parse(Path specification) {
        try {
            var objectMapper = new ObjectMapper(new YAMLFactory())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

            return objectMapper.readValue(
                    Files.readString(specification),
                    AsyncAPI.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
