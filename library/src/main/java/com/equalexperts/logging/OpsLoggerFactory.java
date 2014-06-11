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

    private PrintStream loggerOutput = null;
    private Path logfilePath = null;

    private Boolean storeStackTracesInFilesystem = null;
    private Path stackTraceStoragePath = null;
    private Consumer<Throwable> errorHandler = null;

    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateDestination(printStream);
        loggerOutput = printStream;
        logfilePath = null;
        return this;
    }

    public OpsLoggerFactory setPath(Path path) {
        validateLogfilePath(path);
        logfilePath = path;
        loggerOutput = null;
        return this;
    }

    public OpsLoggerFactory setStoreStackTracesInFilesystem(boolean store) {
        storeStackTracesInFilesystem = store;
        if (!store) {
            stackTraceStoragePath = null;
        }
        return this;
    }

    public OpsLoggerFactory setStackTraceStoragePath(Path directory) {
        validateStackTraceStoragePath(directory);
        setStoreStackTracesInFilesystem(true);
        stackTraceStoragePath = directory;
        return this;
    }

    public OpsLoggerFactory setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = handler;
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        StackTraceProcessor stackTraceProcessor = configureStackTraceProcessor();
        PrintStream output = configureOutput();
        Consumer<Throwable> errorHandler = (this.errorHandler != null) ? this.errorHandler : DEFAULT_ERROR_HANDLER;
        return new BasicOpsLogger<>(output, Clock.systemUTC(), stackTraceProcessor, errorHandler);
    }

    private PrintStream configureOutput() throws IOException {
        if (loggerOutput != null) {
            return loggerOutput;
        }
        if (logfilePath != null) {
            Files.createDirectories(logfilePath.getParent());
            OutputStream outputStream = Files.newOutputStream(logfilePath, CREATE, APPEND);
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
        if (storeStackTracesInFilesystem == null && logfilePath != null) {
            //default behaviour
            return Optional.of(logfilePath.getParent());
        }

        if ((storeStackTracesInFilesystem != null) && storeStackTracesInFilesystem) {
            //storing stack traces in the filesystem has been explicitly enabled

            if ((stackTraceStoragePath == null) && (logfilePath == null)) {
                throw new IllegalStateException("Cannot store stack traces in the filesystem without providing a path");
            }

            if (stackTraceStoragePath != null) {
                //use the explicitly provided location
                return Optional.of(stackTraceStoragePath);
            }

            //store stack traces in the same directory as the log file
            return Optional.of(logfilePath.getParent());
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