package com.equalexperts.logging;

import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

public class LogicalLogRecordTest {

    public static final StackTraceProcessor PROCESSOR_SHOULD_NOT_BE_CALLED = (t, out) -> fail("should not be called");

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullTimestamp() throws Exception {
        try {
            new LogicalLogRecord<>(null, TestMessages.Foo, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter timestamp"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullMessage() throws Exception {
        try {
            new LogicalLogRecord<TestMessages>(Instant.now(), null, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter message"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullCause() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), TestMessages.Foo, null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter cause"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenNullDetails() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), TestMessages.Foo, Optional.empty(), (Object[]) null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter details"));
        }
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage_givenNoThrowableSet() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42", result);
    }

    @Test
    public void format_shouldIncludeMilliseconds_whenTheTimestampIsAnEvenSecond() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.000Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.000Z,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage_givenAThrowable() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Throwable expectedThrowable = new RuntimeException();
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, TestMessages.Bar, Optional.of(expectedThrowable), 42);

        StackTraceProcessor processor = (t, out) -> {
            assertSame(expectedThrowable, t);
            out.append("#EXCEPTION_HERE#");
        };

        String result = record.format(processor);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42 #EXCEPTION_HERE#", result);
    }

    @Test
    public void class_shouldBeImmutable() throws Exception {
        assertInstancesOf(LogicalLogRecord.class, areImmutable());
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