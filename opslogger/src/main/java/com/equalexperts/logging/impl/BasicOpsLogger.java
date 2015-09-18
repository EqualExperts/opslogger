package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/** OpsLogger which writes each entry directly to the Destination */

public class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {

    private static final DiagnosticContextSupplier NO_LOCAL_DIAGNOSTIC_CONTEXT_SUPPLIER = null;

    private final Clock clock;
    private final Consumer<Throwable> errorHandler;
    private final Destination<T> destination;
    private final Lock lock;
    private final DiagnosticContextSupplier diagnosticContextSupplier;
    private final boolean closeable;

    public BasicOpsLogger(Clock clock, DiagnosticContextSupplier diagnosticContextSupplier, Destination<T> destination, Lock lock, Consumer<Throwable> errorHandler) {
        this(clock, diagnosticContextSupplier, destination, lock, errorHandler, true);
    }

    private BasicOpsLogger(Clock clock, DiagnosticContextSupplier diagnosticContextSupplier, Destination<T> destination, Lock lock, Consumer<Throwable> errorHandler, boolean closeable) {
        this.clock = clock;
        this.diagnosticContextSupplier = diagnosticContextSupplier;
        this.destination = destination;
        this.lock = lock;
        this.errorHandler = errorHandler;
        this.closeable = closeable;
    }

    @Override
    public void close() throws Exception {
        if (closeable) {
            destination.close();
        }
    }

    @Override
    public void log(T message, Object... details) {
        log(message, NO_LOCAL_DIAGNOSTIC_CONTEXT_SUPPLIER, details);
    }

    @Override
    public void log(T message, DiagnosticContextSupplier localContextSupplier, Object... details) {
        try {
            LogicalLogRecord<T> record = constructLogRecord(message, localContextSupplier, Optional.empty(), details);
            publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        log(message, NO_LOCAL_DIAGNOSTIC_CONTEXT_SUPPLIER, cause, details);
    }

    @Override
    public void log(T message, DiagnosticContextSupplier localContextSupplier, Throwable cause, Object... details) {
        try {
            LogicalLogRecord<T> record = constructLogRecord(message, localContextSupplier, Optional.of(cause), details);
            publish(record);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    public BasicOpsLogger<T> with(DiagnosticContextSupplier override) {
        return new BasicOpsLogger<>(clock, override, destination, lock, errorHandler, false);
    }

    private LogicalLogRecord<T> constructLogRecord(T message, DiagnosticContextSupplier localContextSupplier, Optional<Throwable> o, Object... details) {
        return new LogicalLogRecord<>(clock.instant(), new DiagnosticContext(diagnosticContextSupplier, localContextSupplier), message, o, details);
    }

    private void publish(LogicalLogRecord<T> record) throws Exception {
        lock.lock();
        try {
            destination.beginBatch();
            try {
                destination.publish(record);
            } finally {
                destination.endBatch();
            }
        } finally {
            lock.unlock();
        }
    }

    public Clock getClock() {
        return clock;
    }

    public Destination<T> getDestination() {
        return destination;
    }

    public DiagnosticContextSupplier getDiagnosticContextSupplier() {
        return diagnosticContextSupplier;
    }

    public Lock getLock() {
        return lock;
    }

    public Consumer<Throwable> getErrorHandler() { return errorHandler; }
}