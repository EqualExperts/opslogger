package com.equalexperts.logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final Clock clock;
    private final Consumer<Throwable> errorHandler;
    private final Destination<T> destination;

    BasicOpsLogger(PrintStream output, Clock clock, StackTraceProcessor stackTraceProcessor, Consumer<Throwable> errorHandler) {
        this(clock, new BasicOutputStreamDestination<>(output, stackTraceProcessor), errorHandler);
    }

    BasicOpsLogger(Clock clock, Destination<T> destination, Consumer<Throwable> errorHandler) {
        this.clock = clock;
        this.destination = destination;
        this.errorHandler = errorHandler;
    }

    @Override
    public void close() throws IOException {
        destination.close();
    }

    @Override
    public void log(T message, Object... details) {
        try {
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), message, Optional.empty(), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        try {
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), message, Optional.of(cause), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    protected static interface Destination<T extends Enum<T> & LogMessage> extends Closeable {
        void publish(LogicalLogRecord<T> record) throws Exception;

        @Override
        void close() throws IOException;
    }

    Clock getClock() {
        return clock;
    }

    @Deprecated
    BasicOutputStreamDestination<T> getBasicOutputStreamDestination() {
        return (BasicOutputStreamDestination<T>) getDestination();
    }

    Destination<T> getDestination() {
        return destination;
    }

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}