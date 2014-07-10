package com.equalexperts.logging;

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
    public void close() throws Exception {
        destination.close();
    }

    @Override
    public void log(T message, Object... details) {
        try {
            LogicalLogRecord<T> record = constructLogRecord(message, Optional.empty(), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        try {
            LogicalLogRecord<T> record = constructLogRecord(message, Optional.of(cause), details);
            destination.publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    private LogicalLogRecord<T> constructLogRecord(T message, Optional<Throwable> o, Object... details) {
        return new LogicalLogRecord<>(clock.instant(), correlationIdSupplier.get(), message, o, details);
    }

    static interface Destination<T extends Enum<T> & LogMessage> extends AutoCloseable {
        void publish(LogicalLogRecord<T> record) throws Exception;
    }

    Clock getClock() {
        return clock;
    }

    Destination<T> getDestination() {
        return destination;
    }

    Supplier<String[]> getCorrelationIdSupplier() {
        return correlationIdSupplier;
    }

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}