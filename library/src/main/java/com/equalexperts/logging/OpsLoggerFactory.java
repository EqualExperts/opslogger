package com.equalexperts.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Clock;

public class OpsLoggerFactory {
    private static final boolean ENABLE_APPEND = true;
    private static final boolean ENABLE_AUTO_FLUSH = true;

    private PrintStream loggerOutput = System.out;

    public OpsLoggerFactory loggingTo(PrintStream printStream) {
        loggerOutput = printStream;
        return this;
    }

    public OpsLoggerFactory loggingTo(File file) throws FileNotFoundException {
        TestFriendlyFileOutputStream fileOutputStream = new TestFriendlyFileOutputStream(file, ENABLE_APPEND);
        loggerOutput = new TestFriendlyPrintStream(fileOutputStream, ENABLE_AUTO_FLUSH);
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build(@SuppressWarnings("UnusedParameters") Class<T> messageType) {
        return new BasicOpsLogger<>(loggerOutput, Clock.systemUTC());
    }
}
