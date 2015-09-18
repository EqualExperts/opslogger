package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class DiagnosticContext {

    private final Map<String, String> mergedContext;

    public DiagnosticContext(DiagnosticContextSupplier supplier) {
        if (supplier == null) {
            mergedContext = Collections.emptyMap();
        } else {
            Map<String, String> rawContext = supplier.getMessageContext();
            if (rawContext == null) {
                mergedContext = Collections.emptyMap();
            } else {
                mergedContext = Collections.unmodifiableMap(new LinkedHashMap<>(rawContext));
            }
        }
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
