package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BasicOutputStreamDestinationTest {

    @Rule
    public RestoreSystemStreamsFixture systemStreamsFixture =  new RestoreSystemStreamsFixture();

    private final TestPrintStream output = new TestPrintStream();
    private final StackTraceProcessor processor = new SimpleStackTraceProcessor();
    private final BasicOpsLogger.Destination<TestMessages> destination = new BasicOutputStreamDestination<>(output, processor);

    @Test
    public void publish_shouldPublishAFormattedLogRecord() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), TestMessages.Foo, Optional.empty());
        String expectedMessage = record.format(processor) + System.getProperty("line.separator");

        destination.publish(record);

        assertEquals(expectedMessage, output.toString());
    }

    @Test
    public void close_shouldCloseThePrintStream() throws Exception {
        destination.close();

        assertTrue(output.isClosed());
    }

    @Test
    public void close_shouldNotCloseThePrintStream_whenThePrintStreamIsSystemOut() throws Exception {
        System.setOut(output);

        destination.close();

        assertFalse(output.isClosed());
    }

    @Test
    public void close_shouldNotCloseThePrintStream_whenThePrintStreamIsSystemErr() throws Exception {
        System.setErr(output);

        destination.close();

        assertFalse(output.isClosed());
    }

    static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "An event of some kind occurred");

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