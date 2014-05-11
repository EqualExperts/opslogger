package com.equalexperts.logging;

import org.junit.Test;

import java.util.MissingFormatArgumentException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OpsLoggerMockFactoryTest {

    private final OpsLogger<TestMessages> logger = OpsLoggerMockFactory.mockLogger(TestMessages.class);

    @Test
    public void mockLogger_shouldCreateAMockitoSpy() throws Exception {
        logger.log(TestMessages.Foo);
        verify(logger).log(TestMessages.Foo);
        assertTrue(mockingDetails(logger).isSpy());
    }

    @Test
    public void mockLogger_shouldReturnATestDoubleToEnforceAllOtherValidation() throws Exception {
        assertTrue(logger instanceof OpsLoggerTestDouble);

        try {
            //double-check validation is occurring by trying to log a message without arguments
            logger.log(TestMessages.Bar);
            fail("Expected a failure");
        } catch (MissingFormatArgumentException ignore) {
        }
    }


    static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "No Fields"),
        Bar("CODE-Bar", "One Field: %s");

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
