package com.equalexperts.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
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
        validateDestination(printStream);
        loggerOutput = Optional.of(printStream);
        logfilePath = Optional.empty();
        return this;
    }

    public OpsLoggerFactory setPath(Path path) {
        validateLogfilePath(path);
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
        validateStackTraceStoragePath(directory);
        setStoreStackTracesInFilesystem(true);
        stackTraceStoragePath = Optional.of(directory);
        return this;
    }

    public OpsLoggerFactory setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = Optional.of(handler);
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        PrintStream output = configureOutput();
        return new BasicOpsLogger<>(output, Clock.systemUTC(), stackTraceProcessor, errorHandler.orElse(DEFAULT_ERROR_HANDLER));
    }

    private PrintStream configureOutput() throws IOException {
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
        if (!storeStackTracesInFilesystem.isPresent() && logfilePath.isPresent()) {
            //default behaviour
            return logfilePath.map(Path::getParent);
        }

        if ((storeStackTracesInFilesystem.isPresent()) && storeStackTracesInFilesystem.get()) {
            //storing stack traces in the filesystem has been explicitly enabled

            if (!stackTraceStoragePath.isPresent() && !logfilePath.isPresent()) {
                throw new IllegalStateException("Cannot store stack traces in the filesystem without providing a path");
            }

            if (stackTraceStoragePath.isPresent()) {
                //use the explicitly provided location
                return stackTraceStoragePath;
            }

            //store stack traces in the same directory as the log file
            return logfilePath.map(Path::getParent);
        }
        return Optional.empty();
    }

    private void validateDestination(PrintStream printStream) {
        if (printStream == null) {
            throw new IllegalArgumentException("Destination must not be null");
        }
    }

    private void validateStackTraceStoragePath(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("path must be a directory");
        }
    }

    private void validateLogfilePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must not be a directory");
        }
    }
}