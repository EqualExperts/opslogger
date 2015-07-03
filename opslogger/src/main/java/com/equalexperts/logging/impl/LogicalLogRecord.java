package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class LogicalLogRecord<T extends Enum<T> & LogMessage> {

    private static final DateTimeFormatter ISO_ALWAYS_WITH_MILLISECONDS = new DateTimeFormatterBuilder()
            .parseStrict()
            .parseCaseInsensitive()
            .appendInstant(3)
            .toFormatter();

    private final Instant timestamp;
    private final T message;
    private final Optional<Throwable> cause;
    private final Object[] details;
    private final DiagnosticContext diagnosticContext;

    public LogicalLogRecord(Instant timestamp, DiagnosticContext diagnosticContext, T message, Optional<Throwable> cause, Object... details) {
        this.timestamp = requireNonNull(timestamp, "parameter timestamp must not be null");
        this.diagnosticContext = requireNonNull(diagnosticContext, "parameter diagnosticContext must not be null");
        this.message = requireNonNull(message, "parameter message must not be null");
        this.cause = requireNonNull(cause, "parameter cause must not be null");
        this.details = requireNonNull(details, "parameter details must not be null");
    }

    public String format(StackTraceProcessor processor) throws Exception {
        StringBuilder result = new StringBuilder();
        ISO_ALWAYS_WITH_MILLISECONDS.formatTo(timestamp, result);
        result.append(",");
        diagnosticContext.printContextInformation(result);
        result.append(message.getMessageCode());
        result.append(",");
        new Formatter(result).format(message.getMessagePattern(), details);
        if (cause.isPresent()) {
            result.append(" "); //the gap between the basic message and the stack trace
            processor.process(cause.get(), result);
        }
        return result.toString();
    }

    Instant getTimestamp() {
        return timestamp;
    }

    DiagnosticContext getDiagnosticContext() { return diagnosticContext; }

    T getMessage() {
        return message;
    }

    Optional<Throwable> getCause() {
        return cause;
    }

    Object[] getDetails() {
        return details;
    }
}
