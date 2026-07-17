package io.obya.api.onboarding.adapter.in.web.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.obya.api.onboarding.domain.model.SpecificationId;

import java.io.IOException;

public class SpecificationIdDeserializer extends StdDeserializer<SpecificationId> {

    protected SpecificationIdDeserializer() {
        super(SpecificationId.class);
    }

    @Override
    public SpecificationId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.has("id")) {
            return new SpecificationId(node.get("id").asText());
        } else {
            throw new IOException("Invalid SpecificationId format");
        }
    }
}
