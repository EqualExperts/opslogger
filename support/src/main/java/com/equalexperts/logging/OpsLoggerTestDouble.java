package com.equalexperts.logging;

import org.hamcrest.CoreMatchers;

import java.util.Arrays;
import java.util.MissingFormatArgumentException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * An OpsLogger implementation that validates arguments, but doesn't actually
 * log anything. Very useful for unit tests.
 */
public class OpsLoggerTestDouble <T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    @Override
    public void log(T message, Object... details) {
        validate(message);
        checkForTooManyFormatStringArguments(message, details);
        //noinspection ResultOfMethodCallIgnored
        String.format(message.getMessagePattern(), details);
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        validate(message);
        assertNotNull("Throwable instance must be provided", cause);
        checkForTooManyFormatStringArguments(message, details);
        //noinspection ResultOfMethodCallIgnored
        String.format(message.getMessagePattern(), details);
    }

    private void checkForTooManyFormatStringArguments(T message, Object[] formatStringArguments) {
        if (formatStringArguments.length > 1) {
            /*
                Check for too many arguments by removing one, and expecting "not enough arguments" to happen.
             */
            try {
                //noinspection ResultOfMethodCallIgnored
                String.format(message.getMessagePattern(), Arrays.copyOfRange(formatStringArguments, 0, formatStringArguments.length - 1));
                throw new IllegalArgumentException("Too many format string arguments provided");
            } catch (MissingFormatArgumentException expected) {
                //expected
            }
        }
    }

    private void validate(T message) {
        assertNotNull("LogMessage must be provided", message);
        assertNotNull("MessageCode must be provided", message.getMessageCode());
        assertThat("MessageCode must be provided", message.getMessageCode(), CoreMatchers.not(""));
        assertNotNull("MessagePattern must be provided", message.getMessagePattern());
        assertThat("MessagePattern must be provided", message.getMessagePattern(), CoreMatchers.not(""));
    }
}
