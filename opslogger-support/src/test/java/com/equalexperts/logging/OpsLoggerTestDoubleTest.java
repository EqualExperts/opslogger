package com.equalexperts.logging;

import org.junit.Test;
import org.mutabilitydetector.unittesting.MutabilityAssertionError;

import java.util.IllegalFormatException;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

public class OpsLoggerTestDoubleTest {
    private final OpsLogger<TestMessages> logger = new OpsLoggerTestDouble<>();

    @Test
    public void log_shouldAllowValidCalls() throws Exception {
        logger.log(TestMessages.Foo);
    }

    @Test
    public void log_shouldThrowAnException_givenAnInvalidFormatStringWithTheRightArguments() throws Exception {
        try {
            logger.log(TestMessages.BadFormatString, 42);
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void log_shouldThrowAnException_whenNotEnoughFormatStringArgumentsAreProvided() throws Exception {
        try {
            logger.log(TestMessages.Bar);
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void log_shouldThrowAnExceptionWhenTooManyFormatStringArgumentsAreProvided() throws Exception {
        try {
            logger.log(TestMessages.Bar, "Foo", "Bar");
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //this exception is expected
            assertEquals("Too many format string arguments provided", e.getMessage());
        }
    }

    @Test
    public void log_shouldCorrectlyAllowLogMessagesWithTwoOrMoreFormatStringArguments() throws Exception {
        logger.log(TestMessages.MessageWithMultipleArguments, "Foo", "Bar");
    }

    @Test
    public void log_shouldThrowAnException_givenANullLogMessage() throws Exception {
        try {
            logger.log(null);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("LogMessage must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenANullMessageCode() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullCode);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAnEmptyMessageCode() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyCode);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenANullMessagePattern() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullFormat);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAnEmptyMessagePattern() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyFormat);
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }
    
    @Test
    public void log_shouldThrowAnException_givenAMutableFormatStringArgument() throws Exception {
        try {
            logger.log(TestMessages.MessageWithMultipleArguments, "foo", new StringBuilder("bar"));
            fail("expected an exception");
        } catch (MutabilityAssertionError e) {
            assertThat(e.getMessage(), containsString("StringBuilder"));
        }
    }

    @Test
    public void log_shouldAllowValidCalls_givenAThrowable() throws Exception {
        logger.log(TestMessages.Foo, new RuntimeException());
    }

    @Test
    public void log_shouldThrowAnException_givenAnInvalidFormatStringWithTheRightArgumentsAndAThrowable() throws Exception {
        try {
            logger.log(TestMessages.BadFormatString, new RuntimeException(), 42);
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void log_shouldThrowAnException_givenNotEnoughFormatStringArgumentsAndAThrowable() throws Exception {

        try {
            logger.log(TestMessages.Bar, new RuntimeException());
            fail("expected an exception");
        } catch (IllegalFormatException e) {
            //this exception is expected
        }
    }

    @Test
    public void log_shouldThrowAnException_givenTooManyFormatStringArgumentsAndAThrowable() throws Exception {

        try {
            logger.log(TestMessages.Bar, new RuntimeException(), "Foo", "Bar");
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            //this exception is expected
            assertEquals("Too many format string arguments provided", e.getMessage());
        }
    }

    @Test
    public void log_shouldAllowCorrectLogMessages_givenTwoOrMoreFormatStringArgumentsAndThrowable() throws Exception {
        logger.log(TestMessages.MessageWithMultipleArguments, new RuntimeException(), "Foo", "Bar");
    }

    @Test
    public void log_shouldThrowAnException_givenANullLogMessageAndThrowable() throws Exception {
        try {
            logger.log(null, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("LogMessage must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenANullThrowable() throws Exception {
        try {
            logger.log(TestMessages.Bar, null, "a");
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("Throwable instance must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAThrowableAndNullMessageCode() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullCode, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAThrowableAndAnEmptyMessageCode() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyCode, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessageCode must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAThrowableAndNullMessageFormat() throws Exception {
        try {
            logger.log(TestMessages.InvalidNullFormat, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAThrowableAndAnEmptyMessageFormat() throws Exception {
        try {
            logger.log(TestMessages.InvalidEmptyFormat, new RuntimeException());
            fail("expected an exception");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("MessagePattern must be provided"));
        }
    }

    @Test
    public void log_shouldThrowAnException_givenAThrowableAndAMutableFormatStringArgument() throws Exception {
        try {
            logger.log(TestMessages.MessageWithMultipleArguments, new RuntimeException(), "foo", new StringBuilder("bar"));
            fail("expected an exception");
        } catch (MutabilityAssertionError e) {
            assertThat(e.getMessage(), containsString("StringBuilder"));
        }
    }

    @Test
    public void close_shouldThrowAnException() throws Exception {
        /*
            Application code shouldn't normally close a real logger, so throw an Exception in the test double
            to discourage it
         */
        try {
            logger.close();
            fail("Expected an exception");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("OpsLogger instances should not be closed by application code."));
        }
    }

    static enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "No Fields"),
        Bar("CODE-Bar", "One Field: %s"),
        BadFormatString("CODE-BFS", "%++d"),
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
