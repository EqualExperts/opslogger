package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Constructs the various non-trivial dependencies that OpsLogger implementations need.
 */
public class InfrastructureFactory {
    public static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = (error) -> error.printStackTrace(System.err);
    public static final DiagnosticContextSupplier EMPTY_CONTEXT_SUPPLIER = Collections::emptyMap;

    private final Optional<Path> logfilePath;
    private final Optional<PrintStream> loggerOutput;
    private final Optional<Boolean> storeStackTracesInFilesystem;
    private final Optional<Path> stackTraceStoragePath;
    private final Optional<DiagnosticContextSupplier> correlationIdSupplier;
    private final Optional<Consumer<Throwable>> errorHandler;

    public InfrastructureFactory(Optional<Path> logfilePath, Optional<PrintStream> loggerOutput, Optional<Boolean> storeStackTracesInFilesystem, Optional<Path> stackTraceStoragePath, Optional<DiagnosticContextSupplier> correlationIdSupplier, Optional<Consumer<Throwable>> errorHandler) {
        this.logfilePath = logfilePath;
        this.loggerOutput = loggerOutput;
        this.storeStackTracesInFilesystem = storeStackTracesInFilesystem;
        this.stackTraceStoragePath = stackTraceStoragePath;
        this.correlationIdSupplier = correlationIdSupplier;
        this.errorHandler = errorHandler;
    }

    public <T extends Enum<T> & LogMessage> Destination<T> configureDestination() throws UncheckedIOException {
        try {
            StackTraceProcessor stackTraceProcessor = this.configureStackTraceProcessor();
            if (logfilePath.isPresent()) {
                if (!Files.isSymbolicLink(logfilePath.get().getParent())) {
                    Files.createDirectories(logfilePath.get().getParent());
                }
                FileChannelProvider provider = new FileChannelProvider(logfilePath.get());
                ActiveRotationRegistry registry = ActiveRotationRegistry.getSingletonInstance();
                return registry.add(new PathDestination<>(provider, stackTraceProcessor, registry));
            }
            return new OutputStreamDestination<>(loggerOutput.orElse(System.out), stackTraceProcessor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Consumer<Throwable> configureErrorHandler() {
        return errorHandler.orElse(DEFAULT_ERROR_HANDLER);
    }

    public DiagnosticContextSupplier configureContextSupplier() {
        return correlationIdSupplier.orElse(EMPTY_CONTEXT_SUPPLIER);
    }

    private StackTraceProcessor configureStackTraceProcessor() throws IOException {
        Optional<Path> storagePath = this.determineStackTraceProcessorPath();
        if (storagePath.isPresent()) {
            if (!Files.isSymbolicLink(storagePath.get())) {
                Files.createDirectories(storagePath.get());
            }
            return new FilesystemStackTraceProcessor(storagePath.get(), new ThrowableFingerprintCalculator());
        }
        return new SimpleStackTraceProcessor();
    }

    private Optional<Path> determineStackTraceProcessorPath() {
        if (storeStackTracesInFilesystem.isPresent()) {
            //storing stack traces in the filesystem has been explicitly configured

            if (!storeStackTracesInFilesystem.get()) {
                return Optional.empty(); //explicitly disabled
            }

            if (stackTraceStoragePath.isPresent()) {
                //use the explicitly provided location when one is set
                return stackTraceStoragePath;
            }

            if (!logfilePath.isPresent()) {
                throw new IllegalStateException("Cannot store stack traces in the filesystem without providing a path");
            }
        }

        //No explicit path provided. Store stack traces in the same directory as the log file, if one is specified.
        return logfilePath.map(Path::getParent);
    }

    //region test hooks: allow tests to determine the values passed into the constructor
    public Optional<Path> getLogfilePath() {
        return logfilePath;
    }

    public Optional<PrintStream> getLoggerOutput() {
        return loggerOutput;
    }

    public Optional<Boolean> getStoreStackTracesInFilesystem() {
        return storeStackTracesInFilesystem;
    }

    public Optional<Path> getStackTraceStoragePath() {
        return stackTraceStoragePath;
    }

    public Optional<DiagnosticContextSupplier> getContextSupplier() {
        return correlationIdSupplier;
    }

    public Optional<Consumer<Throwable>> getErrorHandler() {
        return errorHandler;
    }
    //endregion
}
