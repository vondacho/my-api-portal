package io.obya.api.onboarding.appl.usecase.processing.reader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class URIFileReader implements URIReader {

    @Override
    public boolean canRead(URI uri) {
        return uri.getScheme().equals("file");
    }

    @Override
    public String firstLineOnly(URI uri) throws IOException {
        validateScheme(uri);
        try(var br = Files.newBufferedReader(Path.of(uri.getPath()))) {
            return br.readLine();
        }
    }

    @Override
    public String allInOne(URI uri) throws IOException {
        validateScheme(uri);
        try(var br = Files.newBufferedReader(Path.of(uri.getPath()))) {
            return br.readAllAsString();
        }
    }

    private void validateScheme(URI uri) {
        if (!canRead(uri)) {
            throw new IllegalArgumentException("Not a valid URI, scheme must be file: " + uri);
        }
    }
}
