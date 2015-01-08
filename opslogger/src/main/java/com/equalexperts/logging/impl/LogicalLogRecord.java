package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

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
    private final Map<String,String> correlationIds;

    public LogicalLogRecord(Instant timestamp, Map<String,String> correlationIds, T message, Optional<Throwable> cause, Object... details) {
        this.timestamp = Objects.requireNonNull(timestamp, "parameter timestamp must not be null");
        this.correlationIds = makeSafeCopy(correlationIds);
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
        String formattedCorrelationIds = correlationIds.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
        result.append(formattedCorrelationIds);
        if (!formattedCorrelationIds.isEmpty()) {
            result.append(",");
        }
    }

    private static Map<String, String> makeSafeCopy(Map<String, String> correlationIds) {
        Optional<Map<String, String>> optional = Optional.ofNullable(correlationIds);
        //copy into a new map to prevent mutation after the fact
        //use a LinkedHashMap to preserve order
        optional = optional.map(LinkedHashMap::new);
        optional = optional.map(Collections::unmodifiableMap);
        return optional.orElse(Collections.emptyMap()); //emptyMap is immutable
    }

    Instant getTimestamp() {
        return timestamp;
    }

    Map<String,String> getCorrelationIds() {
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
