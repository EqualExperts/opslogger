package com.equalexperts.logging;

import com.equalexperts.logging.impl.ActiveRotationRegistry;

/** OpsLogger is the interface loggers show to the application */
public interface OpsLogger<T extends Enum<T> & LogMessage> extends AutoCloseable {
    /** Log message using message.getMessagePattern as the format and details as the format arguments.
     * @param message enum to log.
     * @param details format string arguments to message.getMessagePattern()
     * */
    void log(T message, Object... details);

    /** Log message using message.getMessagePattern as the format and details as the format arguments, with
     * the processed cause added.
     * @param message enum to log.
     * @param details format string arguments to message.getMessagePattern()
     * @param cause stack trace to process and include in the log message.
     *
     * */
    void log(T message, Throwable cause, Object... details);

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