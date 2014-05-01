package com.equalexperts.logging;

import org.hamcrest.CoreMatchers;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.MissingFormatArgumentException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * A factory that creates Mockito mocks of an OpsLogger instance and configures them to perform
 * useful checks that ensure you are logging correctly.
 */
public class OpsLoggerMockFactory {

    public static <T extends Enum<T> & LogMessage> OpsLogger<T> mockLogger(Class<T> logMessagesClass) {
        @SuppressWarnings("unchecked")
        OpsLogger<T> result = (OpsLogger<T>) Mockito.mock(OpsLogger.class);

        Mockito.doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            T logMessageInstance = validateLogMessage(arguments);

            Object[] formatStringArguments = Arrays.copyOfRange(arguments, 1, arguments.length);

            checkForTooManyFormatStringArguments(logMessageInstance, formatStringArguments);

            return String.format(logMessageInstance.getMessagePattern(), formatStringArguments);
        }).when(result).log(Mockito.any(logMessagesClass), Mockito.anyVararg());

        Mockito.doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            T logMessageInstance = validateLogMessage(arguments);

            Throwable throwableInstance = (Throwable) arguments[1];
            assertNotNull("Throwable instance must be provided", throwableInstance);

            Object[] formatStringArguments = Arrays.copyOfRange(arguments, 2, arguments.length);

            checkForTooManyFormatStringArguments(logMessageInstance, formatStringArguments);

            return String.format(logMessageInstance.getMessagePattern(), formatStringArguments);
        }).when(result).log(Mockito.any(logMessagesClass), Mockito.any(Throwable.class), Mockito.anyVararg());

        return result;
    }

    private static <T extends Enum<T> & LogMessage> void checkForTooManyFormatStringArguments(T logMessageInstance, Object[] formatStringArguments) {
        if (formatStringArguments.length > 0) {
            /*
                Check for too many arguments by removing one, and expecting "not enough arguments" to happen.
             */
            try {
                //noinspection ResultOfMethodCallIgnored
                String.format(logMessageInstance.getMessagePattern(), Arrays.copyOfRange(formatStringArguments, 0, formatStringArguments.length - 1));
                throw new IllegalArgumentException("Too many format string arguments provided");
            } catch (MissingFormatArgumentException expected) {
                //expected
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T> & LogMessage> T validateLogMessage(Object[] invocationArguments) {
        T logMessageInstance = (T) invocationArguments[0];
        assertNotNull("LogMessage must be provided", logMessageInstance);
        assertNotNull("MessageCode must be provided", logMessageInstance.getMessageCode());
        assertThat("MessageCode must be provided", logMessageInstance.getMessageCode(), CoreMatchers.not(""));
        assertNotNull("MessagePattern must be provided", logMessageInstance.getMessagePattern());
        assertThat("MessagePattern must be provided", logMessageInstance.getMessagePattern(), CoreMatchers.not(""));

        return logMessageInstance;
    }
}
