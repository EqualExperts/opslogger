package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class LogicalLogRecordTest {

    static final StackTraceProcessor PROCESSOR_SHOULD_NOT_BE_CALLED = (t, out) -> fail("should not be called");
    static final DiagnosticContext SAMPLE_DIAGNOSTIC_CONTEXT = new DiagnosticContext(emptyMap());

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullTimestamp() throws Exception {
        try {
            new LogicalLogRecord<>(null, SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Foo, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter timestamp"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullDiagnosticContext() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter diagnosticContext"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullMessage() throws Exception {
        try {
            new LogicalLogRecord<TestMessages>(Instant.now(), SAMPLE_DIAGNOSTIC_CONTEXT, null, Optional.empty());
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter message"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenANullCause() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Foo, null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter cause"));
        }
    }

    @Test
    public void constructor_shouldThrowANullPointerException_givenNullDetails() throws Exception {
        try {
            new LogicalLogRecord<>(Instant.now(), SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Foo, Optional.empty(), (Object[]) null);
            fail("expected an exception");
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), containsString("parameter details"));
        }
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42", result);
    }

    @Test
    public void format_shouldIncludeMilliseconds_whenTheTimestampIsAnEvenSecond() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.000Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Foo, Optional.empty());

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.000Z,CODE-Foo,An event of some kind occurred", result);
    }

    @Test
    public void format_shouldProduceAnAppropriatelyFormattedMessage_givenAThrowable() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");
        Throwable expectedThrowable = new RuntimeException();
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, SAMPLE_DIAGNOSTIC_CONTEXT, TestMessages.Bar, Optional.of(expectedThrowable), 42);

        StackTraceProcessor processor = (t, out) -> {
            assertSame(expectedThrowable, t);
            out.append("#EXCEPTION_HERE#");
        };

        String result = record.format(processor);

        assertEquals("2014-04-01T13:37:00.123Z,CODE-Bar,A Bar event occurred, with argument 42 #EXCEPTION_HERE#", result);
    }

    @Test
    public void format_shouldIncludeTheDiagnosticContextInTheFormattedMessage() throws Exception {
        Instant instant = Instant.parse("2014-04-01T13:37:00.123Z");

        Map<String, String> correlationIds = new HashMap<>();
        correlationIds.put("user", "joeUser");
        DiagnosticContext dc = spy(new DiagnosticContext(correlationIds));

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(instant, dc, TestMessages.Bar, Optional.empty(), 42);

        String result = record.format(PROCESSOR_SHOULD_NOT_BE_CALLED);

        assertEquals("2014-04-01T13:37:00.123Z,user=joeUser,CODE-Bar,A Bar event occurred, with argument 42", result);
        verify(dc).printContextInformation(any());
    }

    private enum TestMessages implements LogMessage {
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