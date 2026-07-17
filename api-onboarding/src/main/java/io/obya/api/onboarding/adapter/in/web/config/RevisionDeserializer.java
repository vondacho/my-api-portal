package io.obya.api.onboarding.adapter.in.web.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.obya.api.onboarding.domain.model.Revision;

import java.io.IOException;

public class RevisionDeserializer extends StdDeserializer<Revision> {

    protected RevisionDeserializer() {
        super(Revision.class);
    }

    @Override
    public Revision deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return Revision.from(p.getText());
    }
}
