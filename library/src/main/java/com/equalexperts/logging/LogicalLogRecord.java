package com.equalexperts.logging;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

class LogicalLogRecord<T extends Enum<T> & LogMessage> {

    private static final DateTimeFormatter ISO_ALWAYS_WITH_MILLISECONDS = new DateTimeFormatterBuilder()
            .parseStrict()
            .parseCaseInsensitive()
            .appendInstant(3)
            .toFormatter();

    private final Instant timestamp;
    private final T message;
    private final Optional<Throwable> cause;
    private final Object[] details;
    private final String[] correlationIds;

    LogicalLogRecord(Instant timestamp, String[] correlationIds, T message, Optional<Throwable> cause, Object... details) {
        this.timestamp = Objects.requireNonNull(timestamp, "parameter timestamp must not be null");
        this.correlationIds = correlationIds;
        this.message = Objects.requireNonNull(message, "parameter message must not be null");
        this.cause = Objects.requireNonNull(cause, "parameter cause must not be null");
        this.details = Objects.requireNonNull(details, "parameter details must not be null");
    }

    public String format(StackTraceProcessor processor) throws Exception {
        StringBuilder result = new StringBuilder();
        ISO_ALWAYS_WITH_MILLISECONDS.formatTo(timestamp, result);
        result.append(",");
        formatCorrelationIds(result);
        result.append(message.getMessageCode());
        result.append(",");
        new Formatter(result).format(message.getMessagePattern(), details);
        if (cause.isPresent()) {
            result.append(" "); //the gap between the basic message and the stack trace
            processor.process(cause.get(), result);
        }
        return result.toString();
    }

    private void formatCorrelationIds(StringBuilder result) {
        String[] correlationIds = Optional.ofNullable(this.correlationIds).orElse(new String[]{});
        Stream.of(correlationIds)
                .map(this::convertNullCorrelationIdsToDashes)
                .map(s -> s + ",")
                .forEach(result::append);
    }

    private String convertNullCorrelationIdsToDashes(String correlationId) {
        return Optional.ofNullable(correlationId)
                .filter(s -> !s.isEmpty())
                .orElse("-");
    }

    Instant getTimestamp() {
        return timestamp;
    }

    String[] getCorrelationIds() {
        return correlationIds;
    }

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
