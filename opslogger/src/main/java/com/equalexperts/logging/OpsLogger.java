package com.equalexperts.logging;

import com.equalexperts.logging.impl.ActiveRotationRegistry;

/**
 * <p>OpsLogger is the interface used to log messages by the application. Instances are usually constructed as singletons
 * and injected into application classes via the application's usual dependency injection mechanism.</p>
 *
 * <p>All instances produced by {@link OpsLoggerFactory#build()} are thread-safe, and can be safely
 * accessed by multiple threads.</p>
 */
public interface OpsLogger<T extends Enum<T> & LogMessage> extends AutoCloseable {
    /** Log message using message.getMessagePattern as the format and details as the format arguments.
     * @param message enum to log.
     * @param details format string arguments to message.getMessagePattern()
     * */
    void log(T message, Object... details);

    default void log(T message, DiagnosticContextSupplier localContext, Object... details) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Log message using message.getMessagePattern as the format and details as the format arguments, with
     * the processed cause added.
     * @param message enum to log.
     * @param details format string arguments to message.getMessagePattern()
     * @param cause stack trace to process and include in the log message.
     *
     * */
    void log(T message, Throwable cause, Object... details);

    default void log(T message, DiagnosticContextSupplier localContext, Throwable cause, Object... details) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Refreshes file handles for all log files, providing active rotation support.
     * This method should be called between rotating the original file, and manipulating (archiving, compressing, etc)
     * it. The <code>postRotate</code> block in logRotate is an excellent example of when to use this method.
     *
     * This method will not return until all writing to old file handles has completed.
     *
     * Exposing this method via JMX or an administrative API some kind is the intended use case.
     */
    static void refreshFileHandles() {
        ActiveRotationRegistry.getSingletonInstance().refreshFileHandles();
    }
}