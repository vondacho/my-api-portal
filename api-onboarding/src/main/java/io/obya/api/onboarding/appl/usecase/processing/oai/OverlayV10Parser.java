package io.obya.api.onboarding.appl.usecase.processing.oai;

import io.obya.api.onboarding.appl.usecase.processing.reader.URIReader;
import io.obya.api.onboarding.appl.usecase.workflow.State;
import io.obya.common.util.Try;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class OverlayV10Parser extends OverlayParser {

    public OverlayV10Parser(URIReader[] readers, BiFunction<State, List<Exception>, Map<String, Object>> mapper) {
        super(readers, mapper);
    }
}
