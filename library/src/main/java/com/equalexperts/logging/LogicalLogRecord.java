package com.equalexperts.logging;

import java.time.Instant;
import java.util.Formatter;

class LogicalLogRecord<T extends Enum<T> & LogMessage> {
    private final Instant timestamp;
    private final T message;
    private final Object[] details;
    private final Throwable cause;

    LogicalLogRecord(Instant timestamp, T message, Throwable cause, Object... details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.cause = cause;
    }

    public String format(StackTraceProcessor processor) throws Exception {
        StringBuilder result = new StringBuilder(timestamp.toString());
        result.append(",");
        result.append(message.getMessageCode());
        result.append(",");
        new Formatter(result).format(message.getMessagePattern(), details);
        if (cause != null) {
            result.append(" "); //the gap between the basic message and the stack trace
            processor.process(cause, result);
        }
        return result.toString();
    }
}
