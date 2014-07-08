package com.equalexperts.logging;

import java.time.Instant;
import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;

class LogicalLogRecord<T extends Enum<T> & LogMessage> {
    private final Instant timestamp;
    private final T message;
    private final Optional<Throwable> cause;
    private final Object[] details;

    LogicalLogRecord(Instant timestamp, T message, Optional<Throwable> cause, Object... details) {
        this.timestamp = Objects.requireNonNull(timestamp, "parameter timestamp must not be null");
        this.message = Objects.requireNonNull(message, "parameter message must not be null");
        this.cause = Objects.requireNonNull(cause, "parameter cause must not be null");
        this.details = Objects.requireNonNull(details, "parameter details must not be null");
    }

    public String format(StackTraceProcessor processor) throws Exception {
        StringBuilder result = new StringBuilder(timestamp.toString());
        result.append(",");
        result.append(message.getMessageCode());
        result.append(",");
        new Formatter(result).format(message.getMessagePattern(), details);
        if (cause.isPresent()) {
            result.append(" "); //the gap between the basic message and the stack trace
            processor.process(cause.get(), result);
        }
        return result.toString();
    }
}
