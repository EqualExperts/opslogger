package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BasicOpsLoggerFactory {

    private final InfrastructureFactory infrastructureFactory;

    public BasicOpsLoggerFactory(InfrastructureFactory infrastructureFactory) {
        this.infrastructureFactory = infrastructureFactory;
    }

    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        Supplier<Map<String,String>> correlationIdSupplier = infrastructureFactory.configureCorrelationIdSupplier();
        Consumer<Throwable> errorHandler = infrastructureFactory.configureErrorHandler();
        Destination<T> destination = infrastructureFactory.configureDestination();
        return new BasicOpsLogger<>(Clock.systemUTC(), correlationIdSupplier, destination, new ReentrantLock(), errorHandler);
    }
}
