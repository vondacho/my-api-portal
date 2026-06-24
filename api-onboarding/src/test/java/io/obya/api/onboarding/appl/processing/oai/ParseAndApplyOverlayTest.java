package io.obya.api.onboarding.appl.processing.oai;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.ibm.oas.overlay.OverlayProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseAndApplyOverlayTest {

    @Test
    void should_parse_overlay() throws IOException {
        final String overlay = Files.readString(Path.of("src/test/resources", "oai", "overlay.yaml"));
        assertThat(overlay).contains(
                "x-metrics-sli: '200ms'",
                "x-metrics-slo: '200ms'",
                "x-violations:",
                "x-score: 100");

        final String body = Files.readString(Path.of("src/test/resources", "oai", "openapi.yaml"));
        assertThat(OverlayProcessor.processOverlay(body, overlay)).contains(
                "x-metrics-sli: 200ms",
                "x-metrics-slo: 200ms",
                "x-violations:",
                "x-score: 100");
    }

    @Test
    void should_instantiate_then_apply_templated_overlay() throws IOException {
        final String template = Files.readString(Path.of("src/test/resources", "oai", "overlay_template.yaml"));
        assertThat(template).contains("x-score: {{global}}");

        Mustache mustachTemplate = new DefaultMustacheFactory().compile(new StringReader(template), "test", "{{", "}}");
        StringWriter writer = new StringWriter();
        mustachTemplate.execute(writer, Map.of("global", "79.0"));

        final String body = Files.readString(Path.of("src/test/resources", "oai", "openapi.yaml"));
        assertThat(OverlayProcessor.processOverlay(body, writer.toString())).contains("x-score: 79.0");
    }
}
