package io.obya.api.onboarding.appl.usecase.processing.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class URLReader implements URIReader {

    @Override
    public boolean canRead(URI uri) {
        return uri.getScheme().equals("http");
    }

    @Override
    public String firstLineOnly(URI uri) throws IOException {
        validateScheme(uri);
        try (var reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    @Override
    public String allInOne(URI uri) throws IOException {
        validateScheme(uri);
        try (var reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {
            return reader.readAllAsString();
        }
    }

    private void validateScheme(URI uri) {
        if (!canRead(uri)) {
            throw new IllegalArgumentException("Not a valid URI, scheme must be http: " + uri);
        }
    }
}
