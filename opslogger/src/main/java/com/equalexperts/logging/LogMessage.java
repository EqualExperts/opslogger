package com.equalexperts.logging;

public interface LogMessage {
    /**
     * unique code for this particular log message.  Example "QA001"
     */

    String getMessageCode();

    /**
     * message pattern for this log message. Example "%d out of %d processed."
     */

    String getMessagePattern();
}
