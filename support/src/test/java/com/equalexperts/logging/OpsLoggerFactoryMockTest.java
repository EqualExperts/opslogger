package com.equalexperts.logging;

import org.junit.Test;

import java.util.IllegalFormatException;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class OpsLoggerFactoryMockTest {

    private final OpsLogger<TestMessages> logger = OpsLoggerMockFactory.mockLogger(TestMessages.class);

    @Test
    public void mockLogger_shouldWorkWithMockitoVerification_givenACallToLog() throws Exception {

        logger.log(TestMessages.Foo);

        verify(logger).log(TestMessages.Foo);
    }

    @Test
    public void mockLogger_shouldThrowAnExceptionWhenNotEnoughFormatStringArgumentsAreProvided_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.Bar);
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void mockLogger_shouldThrowAnExceptionWhenTooManyFormatStringArgumentsAreProvided_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.Bar, "Foo", "Bar");
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //this exception is expected
            assertEquals("Too many format string arguments provided", e.getMessage());
        }
    }

    @Test
    public void mockLogger_shouldCorrectlyAllowLogMessagesWithTwoOrMoreFormatStringArguments_givenACallToLog() throws Exception {
        logger.log(TestMessages.MessageWithMultipleArguments, "Foo", "Bar");

        verify(logger).log(TestMessages.MessageWithMultipleArguments, "Foo", "Bar");
    }

    @Test
    public void mockLogger_shouldEnforceANonNullLogMessage_givenACallToLog() throws Exception {
        try {
            logger.log(null);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("LogMessage must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonNullMessageCode_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullCode);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonEmptyMessageCode_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyCode);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonNullMessageFormat_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullFormat);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonEmptyMessageFormat_givenACallToLog() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyFormat);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldThrowAnExceptionWhenNotEnoughFormatStringArgumentsAreProvided_givenACallToLogThatIncludesAThrowable() throws Exception {

        try {
            logger.log(TestMessages.Bar, new RuntimeException());
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void mockLogger_shouldThrowAnExceptionWhenTooManyFormatStringArgumentsAreProvided_givenACallToLogThatIncludesAThrowable() throws Exception {

        try {
            logger.log(TestMessages.Bar, new RuntimeException(), "Foo", "Bar");
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //this exception is expected
            assertEquals("Too many format string arguments provided", e.getMessage());
        }
    }

    @Test
    public void mockLogger_shouldCorrectlyAllowLogMessagesWithTwoOrMoreFormatStringArguments_givenACallToLogThatIncludesAThrowable() throws Exception {
        RuntimeException expectedException = new RuntimeException();
        logger.log(TestMessages.MessageWithMultipleArguments, expectedException, "Foo", "Bar");

        verify(logger).log(TestMessages.MessageWithMultipleArguments, expectedException, "Foo", "Bar");
    }

    @Test
    public void mockLogger_shouldEnforceANonNullLogMessage_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(null, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("LogMessage must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonNullThrowable_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(TestMessages.Bar, null, "a");
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("Throwable instance must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonNullMessageCode_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullCode, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonEmptyMessageCode_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyCode, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonNullMessageFormat_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullFormat, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void mockLogger_shouldEnforceANonEmptyMessageFormat_givenACallToLogThatIncludesAThrowable() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyFormat, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "No Fields"),
        Bar("CODE-Bar", "One Field: %s"),
        InvalidNullCode(null, "Blah"),
        InvalidEmptyCode("", "Blah"),
        InvalidNullFormat("CODE-InvalidNullFormat", null),
        InvalidEmptyFormat("CODE-InvalidEmptyFormat", ""),
        MessageWithMultipleArguments("CODE-MultipleArguments", "Multiple Format String Arguments: %s %s");

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
