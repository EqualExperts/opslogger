package com.equalexperts.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OpsLoggerFactory {
    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);
    static final Supplier<Map<String, String>> EMPTY_CORRELATION_ID_SUPPLIER = Collections::emptyMap;

    private Optional<PrintStream> loggerOutput = Optional.empty();
    private Optional<Path> logfilePath = Optional.empty();

    private boolean async = false;
    private Optional<Boolean> storeStackTracesInFilesystem = Optional.empty();
    private Optional<Path> stackTraceStoragePath = Optional.empty();
    private Optional<Consumer<Throwable>> errorHandler = Optional.empty();
    private Optional<Supplier<Map<String,String>>> correlationIdSupplier = Optional.empty();

    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateParametersForSetDestination(printStream);
        loggerOutput = Optional.of(printStream);
        logfilePath = Optional.empty();
        return this;
    }

    public OpsLoggerFactory setPath(Path path) {
        validateParametersForSetPath(path);
        logfilePath = Optional.of(path).map(Path::toAbsolutePath);
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

    public OpsLoggerFactory setCorrelationIdSupplier(Supplier<Map<String,String>> supplier) {
        this.correlationIdSupplier = Optional.ofNullable(supplier);
        return this;
    }

    public OpsLoggerFactory setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        Supplier<Map<String,String>> correlationIdSupplier = this.correlationIdSupplier.orElse(EMPTY_CORRELATION_ID_SUPPLIER);
        Consumer<Throwable> errorHandler = this.errorHandler.orElse(DEFAULT_ERROR_HANDLER);
        if (async) {
            AsyncOpsLogger.Destination<T> destination = configureAsyncDestination();
            return new AsyncOpsLogger<>(Clock.systemUTC(), correlationIdSupplier, destination, errorHandler, new LinkedTransferQueue<>(), new AsyncExecutor(Executors.defaultThreadFactory()));
        }
        BasicOpsLogger.Destination<T> destination = configureBasicDestination();
        return new BasicOpsLogger<>(Clock.systemUTC(), correlationIdSupplier, destination, errorHandler);
    }

    private <T extends Enum<T> & LogMessage> AsyncOpsLogger.Destination<T> configureAsyncDestination() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        if (logfilePath.isPresent()) {
            if (!Files.isSymbolicLink(logfilePath.get().getParent())) {
                Files.createDirectories(logfilePath.get().getParent());
            }
            FileChannelProvider provider = new FileChannelProvider(logfilePath.get());
            return new AsyncPathDestination<>(provider, stackTraceProcessor);
        }
        return new OutputStreamDestination<>(loggerOutput.orElse(System.out), stackTraceProcessor);
    }


    private <T extends Enum<T> & LogMessage> BasicOpsLogger.Destination<T> configureBasicDestination() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        if (this.logfilePath.isPresent()) {
            if (!Files.isSymbolicLink(logfilePath.get().getParent())) {
                Files.createDirectories(logfilePath.get().getParent());
            }
            FileChannelProvider fileChannelProvider = new FileChannelProvider(logfilePath.get());
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