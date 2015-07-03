package com.equalexperts.logging.impl;


import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AsyncOpsLoggerFactoryTest {
    private OutputStreamDestination<TestMessages> expectedDestination = new OutputStreamDestination<>(System.out, new SimpleStackTraceProcessor());
    private Consumer<Throwable> expectedErrorHandler = t -> {};
    private DiagnosticContextSupplier expectedDiagnosticContextSupplier = HashMap::new;

    private InfrastructureFactory infrastructure = mock(InfrastructureFactory.class);
    private AsyncExecutor mockAsyncExecutor = mock(AsyncExecutor.class);

    private AsyncOpsLoggerFactory factory = new AsyncOpsLoggerFactory();

    @Before
    public void setup() throws IOException {
        factory.setAsyncExecutor(mockAsyncExecutor);
        when(infrastructure.<TestMessages>configureDestination()).thenReturn(expectedDestination);
        when(infrastructure.configureContextSupplier()).thenReturn(expectedDiagnosticContextSupplier);
        when(infrastructure.configureErrorHandler()).thenReturn(expectedErrorHandler);
    }

    @Test
    public void build_shouldConstructACorrectlyConfiguredAsyncOpsLogger() throws Exception {

        AsyncOpsLogger<TestMessages> result = factory.build(infrastructure);

        assertEquals(Clock.systemUTC(), result.getClock());
        assertSame(expectedDiagnosticContextSupplier, result.getDiagnosticContextSupplier());
        assertSame(expectedDestination, result.getDestination());
        assertSame(expectedErrorHandler, result.getErrorHandler());
        assertNotNull(result.getTransferQueue());
        verify(mockAsyncExecutor).execute(any(Runnable.class));
    }

    @Test
    public void build_shouldUseANewLinkedTransferQueueForEachConstructedOpsLogger() throws Exception {
        AsyncOpsLogger<TestMessages> firstResult = factory.build(infrastructure);
        AsyncOpsLogger<TestMessages> secondResult = factory.build(infrastructure);

        assertNotNull(firstResult.getTransferQueue());
        assertNotNull(secondResult.getTransferQueue());
        assertNotSame(firstResult, secondResult);
    }

    private enum TestMessages implements LogMessage {
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