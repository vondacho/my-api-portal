package io.obya.api.onboarding.appl.usecase.processing.oai;

import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class OverlayV11Parser extends OverlayParser {

    public OverlayV11Parser(URIReader[] readers, BiFunction<State, List<Exception>, Map<String, Object>> mapper) {
        super(readers, mapper);
    }
}
