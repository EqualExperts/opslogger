package com.equalexperts.logging;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final Clock clock;
    private final Consumer<Throwable> errorHandler;
    private final Destination<T> destination;
    private final Supplier<String[]> correlationIdSupplier;

    BasicOpsLogger(Clock clock, Supplier<String[]> correlationIdSupplier, Destination<T> destination, Consumer<Throwable> errorHandler) {
        this.clock = clock;
        this.correlationIdSupplier = correlationIdSupplier;
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
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), correlationIdSupplier.get(), message, Optional.empty(), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        try {
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), correlationIdSupplier.get(), message, Optional.of(cause), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    static interface Destination<T extends Enum<T> & LogMessage> extends Closeable {
        void publish(LogicalLogRecord<T> record) throws Exception;
    }

    Clock getClock() {
        return clock;
    }

    Destination<T> getDestination() {
        return destination;
    }

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}