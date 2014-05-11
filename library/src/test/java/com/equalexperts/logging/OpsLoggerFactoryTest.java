package com.equalexperts.logging;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;

import static org.junit.Assert.*;

public class OpsLoggerFactoryTest {
    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToSystemOut_whenNoConfigurationIsPerformed() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .build(TestMessages.class);

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertSame(System.out, basicLogger.getOutput());
        assertEquals(Clock.systemUTC(), basicLogger.getClock());
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToTheCorrectPrintStream_whenAPrintStreamIsSet() throws Exception {
        PrintStream expectedPrintStream = new PrintStream(new ByteArrayOutputStream());

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .loggingTo(expectedPrintStream)
                .build(TestMessages.class);

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertSame(expectedPrintStream, basicLogger.getOutput());
        assertEquals(Clock.systemUTC(), basicLogger.getClock());
    }

    static enum TestMessages implements LogMessage {
        ; //don't actually need any messages for these tests

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
