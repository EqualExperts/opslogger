package com.equalexperts.logging;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** OpsLogger which writes each entry directly to the Destination */

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final Clock clock;
    private final Consumer<Throwable> errorHandler;
    private final Destination<T> destination;
    private final Lock lock;
    private final Supplier<Map<String,String>> correlationIdSupplier;

    BasicOpsLogger(Clock clock, Supplier<Map<String, String>> correlationIdSupplier, Destination<T> destination, Lock lock, Consumer<Throwable> errorHandler) {
        this.clock = clock;
        this.correlationIdSupplier = correlationIdSupplier;
        this.destination = destination;
        this.lock = lock;
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
            publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        try {
            LogicalLogRecord<T> record = constructLogRecord(message, Optional.of(cause), details);
            publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    private LogicalLogRecord<T> constructLogRecord(T message, Optional<Throwable> o, Object... details) {
        return new LogicalLogRecord<>(clock.instant(), correlationIdSupplier.get(), message, o, details);
    }

    private void publish(LogicalLogRecord<T> record) throws Exception {
        lock.lock();
        try {
            destination.publish(record);
        } finally {
            lock.unlock();
        }
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

    Supplier<Map<String, String>> getCorrelationIdSupplier() {
        return correlationIdSupplier;
    }

    Lock getLock() {
        return lock;
    }

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}