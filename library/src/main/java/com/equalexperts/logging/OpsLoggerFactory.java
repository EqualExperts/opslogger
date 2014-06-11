package com.equalexperts.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.*;

public class OpsLoggerFactory {
    private static final boolean ENABLE_AUTO_FLUSH = true;
    static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);

    private PrintStream loggerOutput = null;
    private Path loggerPath = null;

    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateDestination(printStream);
        loggerOutput = printStream;
        loggerPath = null;
        return this;
    }

    public OpsLoggerFactory setPath(Path path) throws IOException {
        validatePath(path);
        loggerPath = path;
        loggerOutput = null;
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        PrintStream output = System.out;
        StackTraceProcessor stackTraceProcessor = null;
        if (loggerOutput != null) {
            output = loggerOutput;
        }
        if (loggerPath != null) {
            ensureParentDirectoriesExist(loggerPath);
            OutputStream outputStream = Files.newOutputStream(loggerPath, CREATE, APPEND);
            output = new PrintStream(outputStream, ENABLE_AUTO_FLUSH);
            stackTraceProcessor = new FilesystemStackTraceProcessor(loggerPath.getParent(), new ThrowableFingerprintCalculator());
        }
        if (stackTraceProcessor == null) {
            stackTraceProcessor = new SimpleStackTraceProcessor();
        }
        return new BasicOpsLogger<>(output, Clock.systemUTC(), stackTraceProcessor, DEFAULT_ERROR_HANDLER);
    }

    private void ensureParentDirectoriesExist(Path path) throws IOException {
        Files.createDirectories(path.getParent());
    }

    private void validateDestination(PrintStream printStream) {
        if (printStream == null) {
            throw new IllegalArgumentException("Destination must not be null");
        }
    }

    private void validatePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must not be a directory");
        }
    }
}
