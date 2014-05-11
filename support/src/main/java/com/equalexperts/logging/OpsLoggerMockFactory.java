package com.equalexperts.logging;

import static org.mockito.Mockito.spy;

/**
 * A factory that creates Mockito mocks of an OpsLogger instance and configures them to perform
 * useful checks that ensure you are logging correctly.
 */
public class OpsLoggerMockFactory {

    public static <T extends Enum<T> & LogMessage> OpsLogger<T> mockLogger(@SuppressWarnings("UnusedParameters") Class<T> logMessagesClass) {
        return spy(new OpsLoggerTestDouble<>());
    }
}
