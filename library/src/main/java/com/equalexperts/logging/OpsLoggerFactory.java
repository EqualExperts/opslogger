package com.equalexperts.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OpsLoggerFactory {
    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);
    static final Supplier<String[]> EMPTY_CORRELATION_ID_SUPPLIER = () -> null;

    private Optional<PrintStream> loggerOutput = Optional.empty();
    private Optional<Path> logfilePath = Optional.empty();

    private Optional<Boolean> storeStackTracesInFilesystem = Optional.empty();
    private Optional<Path> stackTraceStoragePath = Optional.empty();
    private Optional<Consumer<Throwable>> errorHandler = Optional.empty();
    private Optional<Supplier<String[]>> correlationIdSupplier = Optional.empty();

    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateParametersForSetDestination(printStream);
        loggerOutput = Optional.of(printStream);
        logfilePath = Optional.empty();
        return this;
    }

    public OpsLoggerFactory setPath(Path path) {
        validateParametersForSetPath(path);
        logfilePath = Optional.of(path);
        loggerOutput = Optional.empty();
        return this;
    }

    public OpsLoggerFactory setStoreStackTracesInFilesystem(boolean store) {
        storeStackTracesInFilesystem = Optional.of(store);
        if (!store) {
            stackTraceStoragePath = Optional.empty();
        }
        return this;
    }

    public OpsLoggerFactory setStackTraceStoragePath(Path directory) {
        validateParametersForSetStackTraceStoragePath(directory);
        setStoreStackTracesInFilesystem(true);
        stackTraceStoragePath = Optional.of(directory);
        return this;
    }

    public OpsLoggerFactory setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = Optional.ofNullable(handler);
        return this;
    }

    public OpsLoggerFactory setCorrelationIdSupplier(Supplier<String[]> supplier) {
        this.correlationIdSupplier = Optional.ofNullable(supplier);
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        BasicOpsLogger.Destination<T> destination = configureBasicDestination();
        Supplier<String[]> correlationIdSupplier = this.correlationIdSupplier.orElse(EMPTY_CORRELATION_ID_SUPPLIER);
        Consumer<Throwable> errorHandler = this.errorHandler.orElse(DEFAULT_ERROR_HANDLER);
        return new BasicOpsLogger<>(Clock.systemUTC(), correlationIdSupplier, destination, errorHandler);
    }

    private <T extends Enum<T> & LogMessage> BasicOpsLogger.Destination<T> configureBasicDestination() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        if (this.logfilePath.isPresent()) {
            if (!Files.isSymbolicLink(logfilePath.get().getParent())) {
                Files.createDirectories(logfilePath.get().getParent());
            }
            RefreshableFileChannelProvider fileChannelProvider = new RefreshableFileChannelProvider(logfilePath.get(), Duration.of(100, ChronoUnit.MILLIS));
            return new BasicPathDestination<>(new ReentrantLock(), fileChannelProvider, stackTraceProcessor);
        }
        return new OutputStreamDestination<>(loggerOutput.orElse(System.out), stackTraceProcessor);
    }

    private StackTraceProcessor configureStackTraceProcessor() throws IOException {
        Optional<Path> storagePath = determineStackTraceProcessorPath();
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

    private void validateParametersForSetDestination(PrintStream destination) {
        Objects.requireNonNull(destination, "Destination must not be null");
    }

    private void validateParametersForSetStackTraceStoragePath(Path directory) {
        Objects.requireNonNull(directory, "path must not be null");
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("path must be a directory");
        }
    }

    private void validateParametersForSetPath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must not be a directory");
        }
    }
}