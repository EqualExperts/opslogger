package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncOpsLoggerFactory extends AbstractOpsLoggerFactory {

    public AsyncOpsLoggerFactory(ConfigurationInfo configurationInfo) {
        super(configurationInfo);
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        Supplier<Map<String,String>> correlationIdSupplier = configureCorrelationIdSupplier();
        Consumer<Throwable> errorHandler = configureErrorHandler();
        Destination<T> destination = configureDestination();
        return new AsyncOpsLogger<>(Clock.systemUTC(), correlationIdSupplier, destination, errorHandler, new LinkedTransferQueue<>(), new AsyncExecutor(Executors.defaultThreadFactory()));
    }
}
