package com.equalexperts.logging;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BasicOpsLoggerTest {
    private Clock fixedClock = Clock.fixed(Instant.parse("2014-02-01T14:57:12.500Z"), ZoneOffset.UTC);
    @Mock private BasicOpsLogger.Destination<TestMessages> destination;
    @Mock private Supplier<String[]> correlationIdSupplier;
    @Mock private Consumer<Throwable> exceptionConsumer;
    @Captor private ArgumentCaptor<LogicalLogRecord<TestMessages>> captor;

    private OpsLogger<TestMessages> logger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        logger = new BasicOpsLogger<>(fixedClock, correlationIdSupplier, destination, exceptionConsumer);
    }

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstance() throws Exception {
        String[] expectedCorrelationIds = new String[]{"foo", "bar"};
        when(correlationIdSupplier.get()).thenReturn(expectedCorrelationIds);
        doNothing().when(destination).publish(captor.capture());

        logger.log(TestMessages.Bar, 64, "Hello, World");

        verify(destination).publish(any());
        verify(correlationIdSupplier).get();
        verifyNoMoreInteractions(destination, correlationIdSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertArrayEquals(expectedCorrelationIds, record.getCorrelationIds());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertFalse(record.getCause().isPresent());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecord() throws Exception {
        logger.log(null);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingCorrelationIds() throws Exception {
        Error expectedThrowable = new Error();
        when(correlationIdSupplier.get()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecord() throws Exception {
        RuntimeException expectedException = new NullPointerException();
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndAThrowable() throws Exception {
        String[] expectedCorrelationIds = new String[]{"foo", "bar"};
        when(correlationIdSupplier.get()).thenReturn(expectedCorrelationIds);
        doNothing().when(destination).publish(captor.capture());
        RuntimeException expectedException = new RuntimeException("expected");

        logger.log(TestMessages.Bar, expectedException, 64, "Hello, World");

        verify(destination).publish(any());
        verify(correlationIdSupplier).get();
        verifyNoMoreInteractions(destination, correlationIdSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertArrayEquals(expectedCorrelationIds, record.getCorrelationIds());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertSame(expectedException, record.getCause().get());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecordWithAThrowable() throws Exception {
        logger.log(TestMessages.Foo, (Throwable) null);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecordWithAThrowable() throws Exception {
        Exception expectedException = new IOException("Couldn't write to the output stream");
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo, new NullPointerException());

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingCorrelationIdsWithAThrowable() throws Exception {
        Error expectedThrowable = new Error();
        when(correlationIdSupplier.get()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void close_shouldCloseTheDestination() throws Exception {
        logger.close();

        verify(destination).close();
    }

    private static enum TestMessages implements LogMessage {
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
}