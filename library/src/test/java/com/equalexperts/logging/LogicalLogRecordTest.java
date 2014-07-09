package com.equalexperts.logging;

import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class LogicalLogRecordTest {

    static final StackTraceProcessor PROCESSOR_SHOULD_NOT_BE_CALLED = (t, out) -> fail("should not be called");
    static final String[] NO_CORRELATION_IDS = new String[]{};

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
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, new String[]{"joeUser"}, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,joeUser,CODE-Bar,A Bar event occurred, with argument 42", result);
    }

    @Test
    public void format_shouldTreatNullCorrelationIdsLikeAnEmptyArrayOfCorrelationIds() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, null, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldConvertNullAndEmptyCorrelationIdsToDashes() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, new String[]{null, "joeUser", ""}, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,-,joeUser,-,CODE-Foo,An event of some kind occurred", result);
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