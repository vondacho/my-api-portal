package io.obya.api.onboarding.appl.usecase.processing.playground.aas;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseAndValidateAsyncAPITest {
    @Test
    void validate() throws IOException {
        var objectMapper = new ObjectMapper(new YAMLFactory())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

        AsyncAPI model = objectMapper.readValue(
                    Files.readString(Path.of("src/test/resources", "playground", "api", "aas", "anyof-asyncapi.yml")),
                    AsyncAPI.class);

        assertThat(model.getInfo().getDescription()).isEqualTo("Booking API");
        assertThat(model.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(model.getInfo().getExtensionFields()).containsExactlyInAnyOrderEntriesOf(Map.of("x-product-slug", "personal-training", "x-api-slug", "booking"));
    }
}
