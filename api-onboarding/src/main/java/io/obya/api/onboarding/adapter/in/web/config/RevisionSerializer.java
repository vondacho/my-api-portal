package io.obya.api.onboarding.adapter.in.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.obya.api.onboarding.domain.model.Revision;

import java.io.IOException;

public class RevisionSerializer extends StdSerializer<Revision> {

    public RevisionSerializer() {
        super(Revision.class);
    }

    @Override
    public void serialize(Revision value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.semver().getVersion());
    }
}
