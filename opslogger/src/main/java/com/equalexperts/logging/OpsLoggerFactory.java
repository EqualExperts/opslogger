package com.equalexperts.logging;

import com.equalexperts.logging.impl.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OpsLoggerFactory {

    private Optional<PrintStream> loggerOutput = Optional.empty();
    private Optional<Path> logfilePath = Optional.empty();

    private boolean async = false;
    private Optional<Boolean> storeStackTracesInFilesystem = Optional.empty();
    private Optional<Path> stackTraceStoragePath = Optional.empty();
    private Optional<Consumer<Throwable>> errorHandler = Optional.empty();
    private Optional<Supplier<Map<String,String>>> correlationIdSupplier = Optional.empty();

    private AsyncOpsLoggerFactory asyncOpsLoggerFactory = new AsyncOpsLoggerFactory();
    private BasicOpsLoggerFactory basicOpsLoggerFactory = new BasicOpsLoggerFactory();

    /**
     * The destination for the log strings. A typical value is System.out.
     * @param printStream destination
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setDestination(PrintStream printStream) {
        validateParametersForSetDestination(printStream);
        loggerOutput = Optional.of(printStream);
        logfilePath = Optional.empty();
        return this;
    }

    /**
     * The path of the file to print the log strings to.  Is closed and reopened frequently to allow outside log rotation to work.
     * The path is used as-is.
     * @param path path for log file
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setPath(Path path) {
        validateParametersForSetPath(path);
        logfilePath = Optional.of(path).map(Path::toAbsolutePath);
        loggerOutput = Optional.empty();
        return this;
    }

    /**
     * <p>Should stack traces be placed in individual files or printed along with the log statements?</p>
     * <p>If called with true, each unique stack trace will placed in its own file. (see setStackTraceStoragePath).
     * If called with false, stack traces will be printed to main log file.</p>
     * @param store true for separate files, false for inlined in log file
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setStoreStackTracesInFilesystem(boolean store) {
        storeStackTracesInFilesystem = Optional.of(store);
        if (!store) {
            stackTraceStoragePath = Optional.empty();
        }
        return this;
    }

    /**
     * <p>Path to directory to contain individual stack trace files.</p>
     * <p>
     * Must be called with a valid path corresponding to a directory, where stack traces will be stored
     * (see setStoreStackTracesInFilesystem).  If the directory does not exist, it will be created.</p>
     * @param directory valid path for target directory
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setStackTraceStoragePath(Path directory) {
        validateParametersForSetStackTraceStoragePath(directory);
        setStoreStackTracesInFilesystem(true);
        stackTraceStoragePath = Optional.of(directory);
        return this;
    }

    /**
     * <p>Handler for when exceptions occur when logging.</p>
     * <p>
     * If any exception is thrown "inside" this logger, it will caught and be passed on to this error handler,
     * which then is responsible for any further error handling.  The log message causing the error, will not
     * be processed further.</p>
     * @param handler Consumer of Throwables handleling any exception encountered.
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = Optional.ofNullable(handler);
        return this;
    }

    /**
     * <p>Set the supplier of the map to print for each log entry.</p>
     * <p>The correlation id map is printed out as part of the message logged:</p>
     * <p>Example code: (where <code>setCorrelationIdSupplier(()-&gt;map)</code> has been invoked in the OpsLoggerFactory
     * invocation, and Failure has the message code "FOO-012345")</p>
     * <pre>
     * map.put("A", "113");
     * logger.log(Failure, new RuntimeException("Argh"));
     * </pre>
     * will give
     * <pre>
     * 2014-10-22T10:59:19.891Z,A=113,FOO-012345,Did not do anything. java.lang.RuntimeException: Argh (file:///tmp/stacktraces/stacktrace_7rSxGtIroLrznTg8bt1BrQ.txt)
     * </pre>
     * @param supplier the map supplier.  (for example: <code>()-&gt;map</code>)
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setCorrelationIdSupplier(Supplier<Map<String,String>> supplier) {
        this.correlationIdSupplier = Optional.ofNullable(supplier);
        return this;
    }

    /**
     * Enable/disable asynchronous logging.
     *
     * When disabled, the log(...) method call does not return until the message has been written to the target
     * file/output stream.
     *
     * When enabled, the log(...) method call pushes the log message object to an internal queue, and returns
     * immediately.  The queue is emptied in order by a background thread. This can be very useful for keeping response
     * times low, but risks losing the log message objects still in the queue if the Java Virtual Machine is for any
     * reason abruptly terminated.
     *
     * If this method is not called, asynchronous logging is disabled.
     *
     * @param async true=async, false=sync
     * @return <code>this</code> for further configuration
     */
    public OpsLoggerFactory setAsync(boolean async) {
        this.async = async;
        return this;
    }

    /**
     * Build and return the OpsLogger corresponding to the configuration provided.
     *
     * @param <T> LogMessage enum of all possible logger objects.
     * @return ready to use OpsLogger
     * @throws IOException if a problem occurs creating parent directories for log files and/or stack traces
     */
    public <T extends Enum<T> & LogMessage> OpsLogger<T> build() throws IOException {
        InfrastructureFactory infrastructureFactory = new InfrastructureFactory(logfilePath, loggerOutput, storeStackTracesInFilesystem, stackTraceStoragePath, correlationIdSupplier, errorHandler);
        if (async) {
            return asyncOpsLoggerFactory.build(infrastructureFactory);
        }
        return basicOpsLoggerFactory.build(infrastructureFactory);
    }

    private void validateParametersForSetDestination(PrintStream destination) {
        Objects.requireNonNull(destination, "Destination must not be null");
    }

    private void validateParametersForSetStackTraceStoragePath(Path directory) {
        Objects.requireNonNull(directory, "path must not be null");
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("path must be a directory");
        }
    }

    private void validateParametersForSetPath(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must not be a directory");
        }
    }

    //region test hooks for spying on internal factories

    void setAsyncOpsLoggerFactory(AsyncOpsLoggerFactory asyncOpsLoggerFactory) {
        this.asyncOpsLoggerFactory = asyncOpsLoggerFactory;
    }

    void setBasicOpsLoggerFactory(BasicOpsLoggerFactory basicOpsLoggerFactory) {
        this.basicOpsLoggerFactory = basicOpsLoggerFactory;
    }

    //endregion
}