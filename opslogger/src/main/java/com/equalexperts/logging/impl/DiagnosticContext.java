package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

public class DiagnosticContext {

    private final Map<String, String> context;

    public DiagnosticContext(DiagnosticContextSupplier supplier) {
        if (supplier == null) {
            context = Collections.emptyMap();
        } else {
            Map<String, String> rawContext = supplier.getMessageContext();
            if (rawContext == null) {
                context = Collections.emptyMap();
            } else {
                context = Collections.unmodifiableMap(new LinkedHashMap<>(rawContext));
            }
        }
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void printContextInformation(StringBuilder result) {
        String contextInformation = context.entrySet().stream()
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
