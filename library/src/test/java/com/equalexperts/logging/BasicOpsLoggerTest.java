package com.equalexperts.logging;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BasicOpsLoggerTest {

    @SuppressWarnings("unchecked") private final BasicOpsLogger.Destination<TestMessages> mockDestination = (BasicOpsLogger.Destination<TestMessages>) mock(BasicOpsLogger.Destination.class);
    @SuppressWarnings("unchecked") private final Consumer<Throwable> exceptionConsumer = (Consumer<Throwable>) mock(Consumer.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2014-02-01T14:57:12.500Z"), ZoneOffset.UTC);
    private final OpsLogger<TestMessages> logger = new BasicOpsLogger<>(fixedClock, mockDestination, exceptionConsumer);

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstance() throws Exception {
        ArgumentCaptor<LogicalLogRecord<TestMessages>> captor = createLogicalLogRecordArgumentCaptor();
        doNothing().when(mockDestination).publish(captor.capture());

        logger.log(TestMessages.Bar, 64, "Hello, World");

        verify(mockDestination).publish(any());
        verifyNoMoreInteractions(mockDestination);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
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
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecord() throws Exception {
        RuntimeException expectedException = new NullPointerException();
        doThrow(expectedException).when(mockDestination).publish(any());

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndAThrowable() throws Exception {
        ArgumentCaptor<LogicalLogRecord<TestMessages>> captor = createLogicalLogRecordArgumentCaptor();
        doNothing().when(mockDestination).publish(captor.capture());
        RuntimeException expectedException = new RuntimeException("expected");

        logger.log(TestMessages.Bar, expectedException, 64, "Hello, World");

        verify(mockDestination).publish(any());
        verifyNoMoreInteractions(mockDestination);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
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
        doThrow(expectedException).when(mockDestination).publish(any());

        logger.log(TestMessages.Foo, new NullPointerException());

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void close_shouldCloseTheDestination() throws Exception {
        logger.close();

        verify(mockDestination).close();
    }

    static enum TestMessages implements LogMessage {
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

    /*
        ArgumentCaptor and generic types DO NOT work well together
     */
    private static <T extends Enum<T> & LogMessage> ArgumentCaptor<LogicalLogRecord<T>> createLogicalLogRecordArgumentCaptor() {
        @SuppressWarnings("unchecked")
        Class<LogicalLogRecord<T>> clazz = (Class<LogicalLogRecord<T>>)(Class) LogicalLogRecord.class;
        return ArgumentCaptor.forClass(clazz);
    }
}