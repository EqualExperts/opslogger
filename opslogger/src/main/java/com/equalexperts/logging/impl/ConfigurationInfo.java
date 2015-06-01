package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigurationInfo {
    public static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);
    public static final Supplier<Map<String, String>> EMPTY_CORRELATION_ID_SUPPLIER = Collections::emptyMap;

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

    public <T extends Enum<T> & LogMessage> Destination<T> configureDestination() throws IOException {
        StackTraceProcessor stackTraceProcessor = this.configureStackTraceProcessor();
        if (logfilePath.isPresent()) {
            if (!Files.isSymbolicLink(logfilePath.get().getParent())) {
                Files.createDirectories(logfilePath.get().getParent());
            }
            FileChannelProvider provider = new FileChannelProvider(logfilePath.get());
            ActiveRotationRegistry registry = ActiveRotationRegistry.getSingletonInstance();
            return registry.add(new PathDestination<>(provider, stackTraceProcessor, registry));
        }
        return new OutputStreamDestination<>(loggerOutput.orElse(System.out), stackTraceProcessor);
    }

    public Consumer<Throwable> configureErrorHandler() {
        return errorHandler.orElse(DEFAULT_ERROR_HANDLER);
    }

    public Supplier<Map<String, String>> configureCorrelationIdSupplier() {
        return correlationIdSupplier.orElse(EMPTY_CORRELATION_ID_SUPPLIER);
    }

    private StackTraceProcessor configureStackTraceProcessor() throws IOException {
        Optional<Path> storagePath = this.determineStackTraceProcessorPath();
        if (storagePath.isPresent()) {
            if (!Files.isSymbolicLink(storagePath.get())) {
                Files.createDirectories(storagePath.get());
            }
            return new FilesystemStackTraceProcessor(storagePath.get(), new ThrowableFingerprintCalculator());
        }
        return new SimpleStackTraceProcessor();
    }

    private Optional<Path> determineStackTraceProcessorPath() {
        if (storeStackTracesInFilesystem.isPresent()) {
            //storing stack traces in the filesystem has been explicitly configured

            if (!storeStackTracesInFilesystem.get()) {
                return Optional.empty(); //explicitly disabled
            }

            if (!stackTraceStoragePath.isPresent() && !logfilePath.isPresent()) {
                throw new IllegalStateException("Cannot store stack traces in the filesystem without providing a path");
            }

            if (stackTraceStoragePath.isPresent()) {
                //use the explicitly provided location
                return stackTraceStoragePath;
            }
        }

        //No explicit path provided. Store stack traces in the same directory as the log file, if one is specified.
        return logfilePath.map(Path::getParent);
    }
}
