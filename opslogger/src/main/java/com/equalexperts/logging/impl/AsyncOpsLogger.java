package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

/**
 * Asynchronous OpsLogger which puts the record to be logged in a transferQueue and
 * returns immediately which allows for better performance at the expense of not
 * necessarily having everything logged if the JVM shuts down unexpectedly.
 * A background thread is responsible for emptying the transferQueue.
 */

public class AsyncOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {

    private static final DiagnosticContextSupplier NO_LOCAL_DIAGNOSTIC_CONTEXT_SUPPLIER = null;

    static final int MAX_BATCH_SIZE = 100;
    private final Future<?> processingThread;
    private final LinkedTransferQueue<Optional<LogicalLogRecord<T>>> transferQueue;
    private final Clock clock;
    private final DiagnosticContextSupplier diagnosticContextSupplier;
    private final Destination<T> destination;
    private final Consumer<Throwable> errorHandler;
    private final boolean closeable;

    public AsyncOpsLogger(Clock clock, DiagnosticContextSupplier diagnosticContextSupplier, Destination<T> destination, Consumer<Throwable> errorHandler, LinkedTransferQueue<Optional<LogicalLogRecord<T>>> transferQueue, AsyncExecutor executor) {
        this.clock = clock;
        this.diagnosticContextSupplier = diagnosticContextSupplier;
        this.destination = destination;
        this.errorHandler = errorHandler;
        this.transferQueue = transferQueue;
        processingThread = executor.execute(this::process);
        this.closeable = true;
    }

    private AsyncOpsLogger(Clock clock, DiagnosticContextSupplier diagnosticContextSupplier, Destination<T> destination, Consumer<Throwable> errorHandler, LinkedTransferQueue<Optional<LogicalLogRecord<T>>> transferQueue, Future<?> processingThread, boolean closeable) {
        this.clock = clock;
        this.diagnosticContextSupplier = diagnosticContextSupplier;
        this.destination = destination;
        this.errorHandler = errorHandler;
        this.transferQueue = transferQueue;
        this.processingThread = processingThread;
        this.closeable = closeable;
    }

    @Override
    public void log(T message, Object... details) {
        log(message, NO_LOCAL_DIAGNOSTIC_CONTEXT_SUPPLIER, details);
    }

    @Override
    public void log(T message, DiagnosticContextSupplier localContextSupplier, Object... details) {
        try {

            DiagnosticContext diagnosticContext = new DiagnosticContext(diagnosticContextSupplier, localContextSupplier);
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), diagnosticContext, message, Optional.empty(), details);
            transferQueue.put(Optional.of(record));
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
            DiagnosticContext diagnosticContext = new DiagnosticContext(diagnosticContextSupplier, localContextSupplier);
            LogicalLogRecord<T> record = new LogicalLogRecord<>(clock.instant(), diagnosticContext, message, Optional.of(cause), details);
            transferQueue.put(Optional.of(record));
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
    }

    public AsyncOpsLogger<T> with(DiagnosticContextSupplier localContextSupplier) {
        return new AsyncOpsLogger<>(clock, localContextSupplier, destination, errorHandler, transferQueue, processingThread, false);
    }

    @Override
    public void close() throws Exception {
        if (closeable) {
            try {
                transferQueue.put(Optional.empty()); //an empty optional is the shutdown signal
                processingThread.get();
            } finally {
                destination.close();
            }
        }
    }

    private void process() {
        /*
            An empty optional on the queue is the shutdown signal
         */
        boolean run = true;
        do {
            try {
                List<Optional<LogicalLogRecord<T>>> messages = waitForNextBatch();
                List<LogicalLogRecord<T>> logRecords = messages.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList());

                if (logRecords.size() < messages.size()) {
                    run = false; //shutdown signal detected
                }
                processBatch(logRecords);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        } while (run);
    }

    private void processBatch(List<LogicalLogRecord<T>> batch) throws Exception {
        if (batch.isEmpty()) {
            return;
        }
        destination.beginBatch();
        for (LogicalLogRecord<T> record : batch) {
            try {
                destination.publish(record);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        }
        destination.endBatch();
    }

    private List<Optional<LogicalLogRecord<T>>> waitForNextBatch() throws InterruptedException {
        List<Optional<LogicalLogRecord<T>>> result = new ArrayList<>();
        result.add(transferQueue.take()); //a blocking operation
        transferQueue.drainTo(result, MAX_BATCH_SIZE - 1);
        return result;
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

    public Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }

    public LinkedTransferQueue<Optional<LogicalLogRecord<T>>> getTransferQueue() {
        return transferQueue;
    }
}
