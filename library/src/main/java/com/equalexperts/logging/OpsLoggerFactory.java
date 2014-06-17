package com.equalexperts.logging;

import com.equalexperts.util.Clock;
import com.equalexperts.util.Consumer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class OpsLoggerFactory {
    private static final boolean ENABLE_AUTO_FLUSH = true;

    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = new Consumer<Throwable>() {
        public void accept(Throwable error) {
            error.printStackTrace(System.err);
        }
    };

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
        PrintStream output = configurePrintStream();
        return new BasicOpsLogger<>(output, Clock.systemUTC(), stackTraceProcessor, errorHandler != null ? errorHandler : DEFAULT_ERROR_HANDLER);
    }

    private PrintStream configurePrintStream() throws IOException {
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
        Path storagePath = determineStackTraceProcessorPath();
        if (storagePath != null) {
            Files.createDirectories(storagePath);
            return new FilesystemStackTraceProcessor(storagePath, new ThrowableFingerprintCalculator());
        }
        return new SimpleStackTraceProcessor();
    }

    private Path determineStackTraceProcessorPath() {
        if (storeStackTracesInFilesystem != null) {
            //storing stack traces in the filesystem has been explicitly configured

            if (!storeStackTracesInFilesystem) {
                return null; //explicitly disabled
            }

            if (stackTraceStoragePath == null && logfilePath == null) {
                throw new IllegalStateException("Cannot store stack traces in the filesystem without providing a path");
            }

            if (stackTraceStoragePath != null) {
                //use the explicitly provided location
                return stackTraceStoragePath;
            }
        }

        //No explicit path provided. Store stack traces in the same directory as the log file, if one is specified.
        return (logfilePath != null) ? logfilePath.getParent() : null;
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
