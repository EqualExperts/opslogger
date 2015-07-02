package com.equalexperts.logging;

/**
 * To be implemented by enumerations of log messages in applications.
 */
public interface LogMessage {
    /**
     * @return unique code for this particular log message.  Example "QA001"
     */

    String getMessageCode();

    /**
     * @return message pattern for this log message. Example "%d out of %d processed."
     */

    String getMessagePattern();
}
