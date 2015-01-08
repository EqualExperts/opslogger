package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class LogicalLogRecordTest {

    static final StackTraceProcessor PROCESSOR_SHOULD_NOT_BE_CALLED = (t, out) -> fail("should not be called");
    static final Map<String,String> NO_CORRELATION_IDS = emptyMap();

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullTimestamp() throws Exception {
        try {
            new LogicalLogRecord<>(null, NO_CORRELATION_IDS, TestMessages.Foo, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter timestamp"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullMessage() throws Exception {
        try {
            new LogicalLogRecord<TestMessages>(Instant.now(), NO_CORRELATION_IDS, null, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter message"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullCause() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), NO_CORRELATION_IDS, TestMessages.Foo, null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter cause"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenNullDetails() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), NO_CORRELATION_IDS, TestMessages.Foo, Optional.empty(), (Object[]) null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter details"));
        }
    }

    @Test
    public void constructor_shouldCreateAReadOnlyCopyOfTheCorrelationIdMapThatPreservesOrder() throws Exception {
        LinkedHashMap<String, String> correlationIds = new LinkedHashMap<>();
        //put 10 random entries in to be sure the map is in insertion order
        range(0, 10).forEach(i -> correlationIds.put(randomUUID().toString(), valueOf(i)));

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), correlationIds, TestMessages.Foo, Optional.empty());

        assertNotSame(correlationIds, record.getCorrelationIds());
        assertNotEquals(correlationIds.getClass(), record.getCorrelationIds().getClass());
        assertEquals(unmodifiableMap(correlationIds).getClass(), record.getCorrelationIds().getClass());
        assertEquals(correlationIds, record.getCorrelationIds());
        List<String> expectedOrder = correlationIds.entrySet().stream().map(Map.Entry::getValue).collect(toList());
        List<String> actualOrder = record.getCorrelationIds().entrySet().stream().map(Map.Entry::getValue).collect(toList());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, NO_CORRELATION_IDS, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42", result);
    }

    @Test
    public void format_shouldIncludeMilliseconds_whenTheTimestampIsAnEvenSecond() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.000Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, NO_CORRELATION_IDS, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.000Z,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage_givenAThrowable() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Throwable expectedThrowable = new RuntimeException();
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, NO_CORRELATION_IDS, TestMessages.Bar, Optional.of(expectedThrowable), 42);

        StackTraceProcessor processor = (t, out) -> {
            assertSame(expectedThrowable, t);
            out.append("#EXCEPTION_HERE#");
        };

        String result = record.format(processor);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42 #EXCEPTION_HERE#", result);
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage_whenACorrelationIdIsProvided() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Map<String, String> correlationIds = new HashMap<>();
        correlationIds.put("user", "joeUser");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, correlationIds, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,user=joeUser,CODE-Bar,A Bar event occurred, with argument 42", result);
    }

    @Test
    public void format_shouldTreatNullCorrelationIdsLikeAnEmptyMapOfCorrelationIds() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, null, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldExcludeNullAndEmptyCorrelationIds() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Map<String, String> correlationIds = new HashMap<>();
        correlationIds.put("user", "joeUser");
        correlationIds.put("foo", null);
        correlationIds.put("bar", "");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, correlationIds, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,user=joeUser,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldSeparateMultipleCorrelationIdsWithSemicolons() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Map<String, String> correlationIds = new LinkedHashMap<>();
        correlationIds.put("user", "joeUser");
        correlationIds.put("foo", "bar");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, correlationIds, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,user=joeUser;foo=bar,CODE-Foo,An event of some kind occurred", result);
    }

    private static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "An event of some kind occurred"),
        Bar("CODE-Bar", "A Bar event occurred, with argument %d");

        //region LogMessage implementation guts
        private final String messageCode;
        private final String messagePattern;

        TestMessages(String messageCode, String messagePattern) {
            this.messageCode = messageCode;
            this.messagePattern = messagePattern;
        }

        @Override
        public String getMessageCode() {
            return messageCode;
        }

        @Override
        public String getMessagePattern() {
            return messagePattern;
        }
        //endregion
    }
}