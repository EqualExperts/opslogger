package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicOpsLoggerFactoryTest {

    private OutputStreamDestination<TestMessages> expectedDestination = new OutputStreamDestination<>(System.out, new SimpleStackTraceProcessor());
    private Consumer<Throwable> expectedErrorHandler = t -> {};
    private Supplier<Map<String, String>> expectedCorrelationIdSupplier = HashMap::new;

    private InfrastructureFactory infrastructure = mock(InfrastructureFactory.class);

    private BasicOpsLoggerFactory factory = new BasicOpsLoggerFactory();

    @Before
    public void setup() throws IOException {
        when(infrastructure.<TestMessages>configureDestination()).thenReturn(expectedDestination);
        when(infrastructure.configureCorrelationIdSupplier()).thenReturn(expectedCorrelationIdSupplier);
        when(infrastructure.configureErrorHandler()).thenReturn(expectedErrorHandler);
    }

    @Test
    public void build_shouldConstructACorrectlyConfiguredBasicOpsLogger() throws Exception {


        BasicOpsLogger<TestMessages> result = factory.build(infrastructure);

        assertEquals(Clock.systemUTC(), result.getClock());
        assertSame(expectedCorrelationIdSupplier, result.getCorrelationIdSupplier());
        assertSame(expectedDestination, result.getDestination());
        assertNotNull(result.getLock());
        assertThat(result.getLock(), instanceOf(ReentrantLock.class));
        assertSame(expectedErrorHandler, result.getErrorHandler());
    }

    @Test
    public void build_shouldUseADifferentLockForEachConstructedOpsLogger() throws Exception {

        BasicOpsLogger<TestMessages> firstResult = factory.build(infrastructure);
        BasicOpsLogger<TestMessages> secondResult = factory.build(infrastructure);

        assertNotNull(firstResult.getLock());
        assertNotNull(secondResult.getLock());
        assertNotEquals(firstResult.getLock(), secondResult.getLock());
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