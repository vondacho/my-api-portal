package io.obya.api.onboarding.adapter.in.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.semver4j.Semver;

import java.io.IOException;

public class SemverSerializer extends StdSerializer<Semver> {

    public SemverSerializer() {
        super(Semver.class);
    }

    @Override
    public void serialize(Semver value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getVersion());
    }
}
