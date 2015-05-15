package com.equalexperts.logging.impl;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigurationInfo {
    private final Optional<Path> logfilePath;
    private final Optional<PrintStream> loggerOutput;
    private final Optional<Boolean> storeStackTracesInFilesystem;
    private final Optional<Path> stackTraceStoragePath;
    private final Optional<Supplier<Map<String, String>>> correlationIdSupplier;
    private final Optional<Consumer<Throwable>> errorHandler;

    public ConfigurationInfo(Optional<Path> logfilePath, Optional<PrintStream> loggerOutput, Optional<Boolean> storeStackTracesInFilesystem, Optional<Path> stackTraceStoragePath, Optional<Supplier<Map<String, String>>> correlationIdSupplier, Optional<Consumer<Throwable>> errorHandler) {
        this.logfilePath = logfilePath;
        this.loggerOutput = loggerOutput;
        this.storeStackTracesInFilesystem = storeStackTracesInFilesystem;
        this.stackTraceStoragePath = stackTraceStoragePath;
        this.correlationIdSupplier = correlationIdSupplier;
        this.errorHandler = errorHandler;
    }

    public Optional<Path> getLogfilePath() {
        return logfilePath;
    }

    public Optional<PrintStream> getLoggerOutput() {
        return loggerOutput;
    }

    public Optional<Boolean> getStoreStackTracesInFilesystem() {
        return storeStackTracesInFilesystem;
    }

    public Optional<Path> getStackTraceStoragePath() {
        return stackTraceStoragePath;
    }

    public Optional<Supplier<Map<String, String>>> getCorrelationIdSupplier() {
        return correlationIdSupplier;
    }

    public Optional<Consumer<Throwable>> getErrorHandler() {
        return errorHandler;
    }
}
