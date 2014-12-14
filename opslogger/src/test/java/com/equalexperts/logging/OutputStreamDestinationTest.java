package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.*;

public class OutputStreamDestinationTest {

    @Rule
    public RestoreSystemStreamsFixture systemStreamsFixture =  new RestoreSystemStreamsFixture();

    private final TestPrintStream output = new TestPrintStream();
    private final StackTraceProcessor processor = new SimpleStackTraceProcessor();
    private final OutputStreamDestination<TestMessages> destination = new OutputStreamDestination<>(output, processor);

    @Test
    public void publish_shouldPublishAFormattedLogRecord() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
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

    @SuppressWarnings("ConstantConditions")
    @Test
    public void class_shouldImplementAsyncOpsLoggerDestination() throws Exception {
        assertTrue(destination instanceof AsyncOpsLogger.Destination);
    }

    @Test
    public void beginBatch_shouldDoNothing() throws Exception {
        destination.beginBatch();

        assertEquals("", this.output.toString());
    }

    @Test
    public void endBatch_shouldDoNothing() throws Exception {
        destination.endBatch();

        assertEquals("", this.output.toString());
    }

    private static enum TestMessages implements LogMessage {
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