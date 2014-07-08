package com.equalexperts.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.*;

public class OpsLoggerFactory {
    private static final boolean ENABLE_AUTO_FLUSH = true;
    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);

    private Optional<PrintStream> loggerOutput = Optional.empty();
    private Optional<Path> logfilePath = Optional.empty();

    private Optional<Boolean> storeStackTracesInFilesystem = Optional.empty();
    private Optional<Path> stackTraceStoragePath = Optional.empty();
    private Optional<Consumer<Throwable>> errorHandler = Optional.empty();

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

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        PrintStream output = configurePrintStream();
        BasicOpsLogger.Destination<T> destination = new BasicOutputStreamDestination<>(output, stackTraceProcessor);
        return new BasicOpsLogger<>(Clock.systemUTC(), destination, errorHandler.orElse(DEFAULT_ERROR_HANDLER));
    }

    private PrintStream configurePrintStream() throws IOException {
        if (loggerOutput.isPresent()) {
            return loggerOutput.get();
        }
        if (logfilePath.isPresent()) {
            Files.createDirectories(logfilePath.get().getParent());
            OutputStream outputStream = Files.newOutputStream(logfilePath.get(), CREATE, APPEND);
            return new PrintStream(outputStream, ENABLE_AUTO_FLUSH);
        }
        return System.out;
    }

    private StackTraceProcessor configureStackTraceProcessor() throws IOException {
        Optional<Path> storagePath = determineStackTraceProcessorPath();
        if (storagePath.isPresent()) {
            Files.createDirectories(storagePath.get());
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