package io.obya.api.onboarding.appl.usecase.processing.reader;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

public interface URIReader {

    boolean canRead(URI uri);

    String firstLineOnly(URI uri) throws IOException;

    String allInOne(URI uri) throws IOException;

    static URIReader readerFor(URI uri, URIReader...readers) {
        return Arrays.stream(readers)
                .filter(reader -> reader.canRead(uri))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No registered reader found for source: " + uri));
    }
}
