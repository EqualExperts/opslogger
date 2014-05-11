package com.equalexperts.logging;

import java.io.PrintStream;
import java.time.Clock;

public class OpsLoggerFactory {
    private PrintStream loggerOutput = System.out;

    public OpsLoggerFactory loggingTo(PrintStream printStream) {
        loggerOutput = printStream;
        return this;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build(@SuppressWarnings("UnusedParameters") Class<T> messageType) {
        return new BasicOpsLogger<>(loggerOutput, Clock.systemUTC());
    }
}
