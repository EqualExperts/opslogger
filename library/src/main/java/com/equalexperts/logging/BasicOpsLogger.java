package com.equalexperts.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Clock;
import java.util.function.Consumer;

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final PrintStream output;
    private final Clock clock;
    private final StackTraceProcessor stackTraceProcessor;
    private final Consumer<Throwable> errorHandler;

    BasicOpsLogger(PrintStream output, Clock clock, StackTraceProcessor stackTraceProcessor, Consumer<Throwable> errorHandler) {
        this.output = output;
        this.clock = clock;
        this.stackTraceProcessor = stackTraceProcessor;
        this.errorHandler = errorHandler;
    }

    @Override
    public void close() throws IOException {
        if (!streamIsSpecial()) {
            output.close();
        }
    }

    @Override
    public void log(T message, Object... details) {
        LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), message, null, details);
        publish(record);
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), message, cause, details);
        publish(record);
    }

    private void publish(LogicalLogRecord<T> record) {
        try {
            output.println(record.format(stackTraceProcessor));
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    private boolean streamIsSpecial() {
        return (output == System.out) || (output == System.err);
    }

    PrintStream getOutput() {
        return output;
    }

    Clock getClock() {
        return clock;
    }

    StackTraceProcessor getStackTraceProcessor() {
        return stackTraceProcessor;
    }

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}