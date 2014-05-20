package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

public class BasicOpsLoggerTest {

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Rule
    public RestoreSystemStreamsFixture systemStreamsFixture =  new RestoreSystemStreamsFixture();

    private final TestPrintStream output = new TestPrintStream();
    private final Clock fixedClock = Clock.fixed(Instant.parse("2014-02-01T14:57:12.500Z"), ZoneOffset.UTC);
    private final OpsLogger<TestMessages> logger = new BasicOpsLogger<>(output, fixedClock);

    @Test
    public void log_shouldWriteATimestampedCodedLogMessageToThePrintStream_givenALogMessageInstance() throws Exception {
        logger.log(TestMessages.Foo);

        assertEquals("2014-02-01T14:57:12.500Z CODE-Foo: An event of some kind occurred\n", output.toString());
    }

    @Test
    public void log_shouldFormatTheLogMessage_givenALogMessageInstance() throws Exception {
        logger.log(TestMessages.Bar, 2, "custom");

        assertEquals("2014-02-01T14:57:12.500Z CODE-Bar: An event with 2 custom messages\n", output.toString());
    }

    @Test
    public void log_shouldWriteATimestampedCodedFormattedLogMessageWithStacktraceToThePrintStream_givenALogMessageInstanceAndAThrowable() throws Exception {
        RuntimeException theException = new RuntimeException("theException");

        logger.log(TestMessages.Bar, theException, 1, "silly");

        TestPrintStream expectedOutput = new TestPrintStream();
        expectedOutput.print("2014-02-01T14:57:12.500Z CODE-Bar: An event with 1 silly messages ");
        theException.printStackTrace(expectedOutput);

        assertEquals(expectedOutput.toString(), output.toString());
    }

    @Test
    public void close_shouldCloseThePrintStream() throws Exception {
        logger.close();

        assertTrue(output.isClosed());
    }

    @Test
    public void close_shouldNotCloseThePrintStream_whenThePrintStreamIsSystemOut() throws Exception {
        System.setOut(output);

        logger.close();

        assertFalse(output.isClosed());
    }

    @Test
    public void close_shouldNotCloseThePrintStream_whenThePrintStreamIsSystemErr() throws Exception {
        System.setErr(output);

        logger.close();

        assertFalse(output.isClosed());
    }

    static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "An event of some kind occurred"),
        Bar("CODE-Bar", "An event with %d %s messages");

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

    static class TestPrintStream extends PrintStream {
        private TestPrintStream() {
            super(new ByteArrayOutputStream(), true);
        }
        private boolean closed = false;

        @Override
        public String toString() {
            ByteArrayOutputStream out = (ByteArrayOutputStream) super.out;
            return new String(out.toByteArray());
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}