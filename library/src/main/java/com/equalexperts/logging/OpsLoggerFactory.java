package com.equalexperts.logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static java.nio.file.StandardOpenOption.*;

public class OpsLoggerFactory {
    private static final boolean ENABLE_AUTO_FLUSH = true;

    private PrintStream loggerOutput = System.out;

    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateDestination(printStream);
        loggerOutput = printStream;
        return this;
    }

    public OpsLoggerFactory setPath(Path path) throws IOException {
        validatePath(path);
        OutputStream outputStream = Files.newOutputStream(path, CREATE, APPEND);
        loggerOutput = new PrintStream(outputStream, ENABLE_AUTO_FLUSH);
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() {
        return new BasicOpsLogger<>(loggerOutput, Clock.systemUTC(), new SimpleStackTraceProcessor());
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
