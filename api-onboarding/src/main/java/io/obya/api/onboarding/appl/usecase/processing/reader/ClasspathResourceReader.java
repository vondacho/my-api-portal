package io.obya.api.onboarding.appl.usecase.processing.reader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class ClasspathResourceReader implements URIReader {

    private final URIFileReader filereader = new URIFileReader();

    @Override
    public boolean canRead(URI uri) {
        return uri.getScheme().equals("classpath");
    }

    @Override
    public String firstLineOnly(URI uri) throws IOException {
        try {
            return filereader.firstLineOnly(toClasspath(uri));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String allInOne(URI uri) throws IOException {
        try {
            return filereader.allInOne(toClasspath(uri));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private URI toClasspath(URI uri) throws URISyntaxException {
        return Objects.requireNonNull(
                getClass().getResource(uri.getPath()), "Resource not found in classpath: " + uri)
                .toURI();
    }
}
