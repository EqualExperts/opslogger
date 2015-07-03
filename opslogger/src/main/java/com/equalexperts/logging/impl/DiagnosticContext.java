package com.equalexperts.logging.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class DiagnosticContext {

    private final Map<String, String> mergedContext;

    @SafeVarargs
    public DiagnosticContext(Map<String, String>... contexts) {
        if (contexts == null || contexts.length == 0) {
            throw new IllegalArgumentException("Must provide at least one context");
        }
        Map<String,String> mergedContext = new LinkedHashMap<>();
        Stream.of(contexts).filter(Objects::nonNull).forEach(mergedContext::putAll);
        this.mergedContext = Collections.unmodifiableMap(mergedContext);
    }

    public Map<String, String> getMergedContext() {
        return mergedContext;
    }

    public void printContextInformation(StringBuilder result) {
        String contextInformation = mergedContext.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> !e.getValue().isEmpty())
                .map(es -> es.getKey() + "=" + es.getValue())
                .collect(joining(";"));
        result.append(contextInformation);
        if (!contextInformation.isEmpty()) {
            result.append(",");
        }
    }
}
