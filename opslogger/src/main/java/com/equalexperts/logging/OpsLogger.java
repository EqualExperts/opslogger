package com.equalexperts.logging;

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
}