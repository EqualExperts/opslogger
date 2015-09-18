package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BasicOpsLoggerTest {
    private Clock fixedClock = Clock.fixed(Instant.parse("2014-02-01T14:57:12.500Z"), ZoneOffset.UTC);
    @Mock private Destination<TestMessages> destination;
    @Mock private DiagnosticContextSupplier diagnosticContextSupplier;
    @Mock private Consumer<Throwable> exceptionConsumer;
    @Mock private Lock lock;
    @Captor private ArgumentCaptor<LogicalLogRecord<TestMessages>> captor;

    private OpsLogger<TestMessages> logger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        logger = new BasicOpsLogger<>(fixedClock, diagnosticContextSupplier, destination, lock, exceptionConsumer);
    }

    //region tests for log(Message, Object...)

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstance() throws Exception {
        Map<String,String> expectedCorrelationIds = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(expectedCorrelationIds);
        doNothing().when(destination).publish(captor.capture());

        logger.log(TestMessages.Bar, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(expectedCorrelationIds, record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertFalse(record.getCause().isPresent());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldObtainAndReleaseALockAndBeginAndEndADestinationBatch_givenALogMessageInstance() throws Exception {
        logger.log(TestMessages.Foo);

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecord() throws Exception {
        logger.log(null);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldNotAcquireALockOrInteractWithTheDestination_givenAProblemCreatingTheLogRecord() throws Exception {
        logger.log(null);

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingCorrelationIds() throws Exception {
        Error expectedThrowable = new Error();
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldNotAcquireALockOrInteractWithTheDestination_givenAProblemObtainingCorrelationIds() throws Exception {
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(new RuntimeException());

        logger.log(TestMessages.Foo);

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecord() throws Exception {
        RuntimeException expectedException = new NullPointerException();
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldEndTheBatchAndReleaseTheLock_givenAProblemPublishingALogRecord() throws Exception {
        doThrow(new RuntimeException()).when(destination).publish(any());

        logger.log(TestMessages.Foo);

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    //endregion

    //region tests for log(Message, Throwable, Object...)

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndAThrowable() throws Exception {
        Map<String, String> expectedCorrelationIds = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(expectedCorrelationIds);
        doNothing().when(destination).publish(captor.capture());
        RuntimeException expectedException = new RuntimeException("expected");

        logger.log(TestMessages.Bar, expectedException, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(expectedCorrelationIds, record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertSame(expectedException, record.getCause().get());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldObtainAndReleaseALockAndBeginAndEndADestinationBatch_givenALogMessageInstanceAndAThrowable() throws Exception {
        logger.log(TestMessages.Foo, new RuntimeException());

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecordWithAThrowable() throws Exception {
        logger.log(TestMessages.Foo, (Throwable) null);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldNotObtainALockOrInteractWithTheDestination_givenAProblemCreatingTheLogRecordWithAThrowable() throws Exception {
        logger.log(null, new Throwable());

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecordWithAThrowable() throws Exception {
        Exception expectedException = new IOException("Couldn't write to the output stream");
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo, new NullPointerException());

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldEndTheBatchAndReleaseTheLock_givenAProblemPublishingTheLogRecordWithAThrowable() throws Exception {
        doThrow(new RuntimeException()).when(destination).publish(any());

        logger.log(TestMessages.Foo, new Error());

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingCorrelationIdsWithAThrowable() throws Exception {
        Error expectedThrowable = new Error();
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldNotObtainALockOrInteractWithTheDestination_givenAProblemObtainingCorrelationIdsWithAThrowable() throws Exception {
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(new RuntimeException());

        logger.log(TestMessages.Foo, new Exception());

        verifyZeroInteractions(lock, destination);
    }

    //endregion

    //region tests for log(Message, DiagnosticContextSupplier, Object...)

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndADiagnosticContextSupplier() throws Exception {
        Map<String,String> globalContext = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(globalContext);
        doNothing().when(destination).publish(captor.capture());
        Map<String, String> localContext = generateCorrelationIds();

        logger.log(TestMessages.Bar, () -> localContext, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(merge(globalContext, localContext), record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertFalse(record.getCause().isPresent());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndANullDiagnosticContextSupplier() throws Exception {
        Map<String,String> globalContext = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(globalContext);
        doNothing().when(destination).publish(captor.capture());

        logger.log(TestMessages.Bar, (DiagnosticContextSupplier) null, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(globalContext, record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertFalse(record.getCause().isPresent());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldObtainAndReleaseALockAndBeginAndEndADestinationBatch_givenALogMessageInstanceAndADiagnosticContextSupplier() throws Exception {
        logger.log(TestMessages.Foo, Collections::emptyMap);

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecordAndADiagnosticContextSupplier() throws Exception {
        logger.log(null, Collections::emptyMap);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldNotAcquireALockOrInteractWithTheDestination_givenAProblemCreatingTheLogRecordAndADiagnosticContextSupplier() throws Exception {
        logger.log(null, Collections::emptyMap);

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingTheGlobalDiagnosticContextAndADiagnosticContextSupplier() throws Exception {
        Error expectedThrowable = new Error();
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo, Collections::emptyMap);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingTheLocalDiagnosticContextAndADiagnosticContextSupplier() throws Exception {
        Error expectedThrowable = new Error();
        DiagnosticContextSupplier dcs = () -> { throw expectedThrowable; };

        logger.log(TestMessages.Foo, dcs);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldNotAcquireALockOrInteractWithTheDestination_givenAProblemObtainingDiagnosticContextsAndADiagnosticContextSupplier() throws Exception {


        logger.log(TestMessages.Foo, () -> {throw new RuntimeException();});

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecordAndADiagnosticContextSupplier() throws Exception {
        RuntimeException expectedException = new NullPointerException();
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo, Collections::emptyMap);

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldEndTheBatchAndReleaseTheLock_givenAProblemPublishingALogRecordAndADiagnosticContextSupplier() throws Exception {
        doThrow(new RuntimeException()).when(destination).publish(any());

        logger.log(TestMessages.Foo, Collections::emptyMap);

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    //endregion

    //region tests for log(Message, Throwable, Object...)

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndAThrowableAndADiagnosticContextSupplier() throws Exception {
        Map<String, String> globalContext = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(globalContext);

        Map<String, String> localContext = generateCorrelationIds();

        doNothing().when(destination).publish(captor.capture());
        RuntimeException expectedException = new RuntimeException("expected");

        logger.log(TestMessages.Bar, () -> localContext, expectedException, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(merge(globalContext, localContext), record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertSame(expectedException, record.getCause().get());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldWriteALogicalLogRecordToTheDestination_givenALogMessageInstanceAndAThrowableAndANullDiagnosticContextSupplier() throws Exception {
        Map<String, String> globalContext = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(globalContext);
        doNothing().when(destination).publish(captor.capture());
        RuntimeException expectedException = new RuntimeException("expected");

        logger.log(TestMessages.Bar, (DiagnosticContextSupplier) null, expectedException, 64, "Hello, World");

        verify(destination, times(1)).publish(any());
        verify(diagnosticContextSupplier).getMessageContext();
        verifyNoMoreInteractions(diagnosticContextSupplier);

        LogicalLogRecord<TestMessages> record = captor.getValue();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(globalContext, record.getDiagnosticContext().getMergedContext());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertSame(expectedException, record.getCause().get());
        assertArrayEquals(new Object[]{64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldObtainAndReleaseALockAndBeginAndEndADestinationBatch_givenALogMessageInstanceAndAThrowableAndADiagnosticContextSupplier() throws Exception {
        logger.log(TestMessages.Foo, Collections::emptyMap, new RuntimeException());

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecordWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        logger.log(TestMessages.Foo, Collections::emptyMap, (Throwable) null);

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldNotObtainALockOrInteractWithTheDestination_givenAProblemCreatingTheLogRecordWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        logger.log(null, Collections::emptyMap, new Throwable());

        verifyZeroInteractions(lock, destination);
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemPublishingALogRecordWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        Exception expectedException = new IOException("Couldn't write to the output stream");
        doThrow(expectedException).when(destination).publish(any());

        logger.log(TestMessages.Foo, Collections::emptyMap, new NullPointerException());

        verify(exceptionConsumer).accept(Mockito.same(expectedException));
    }

    @Test
    public void log_shouldEndTheBatchAndReleaseTheLock_givenAProblemPublishingTheLogRecordWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        doThrow(new RuntimeException()).when(destination).publish(any());

        logger.log(TestMessages.Foo, Collections::emptyMap, new Error());

        InOrder inOrder = inOrder(lock, destination);
        inOrder.verify(lock).lock();
        inOrder.verify(destination).beginBatch();
        inOrder.verify(destination).publish(any());
        inOrder.verify(destination).endBatch();
        inOrder.verify(lock).unlock();
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingTheGlobalDiagnosticContextWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        Error expectedThrowable = new Error();
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo, Collections::emptyMap, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingTheLocalDiagnosticContextWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        Error expectedThrowable = new Error();
        DiagnosticContextSupplier dcs = () -> { throw expectedThrowable; };

        logger.log(TestMessages.Foo, dcs, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldNotObtainALockOrInteractWithTheDestination_givenAProblemObtainingADiagnosticContextWithAThrowableAndADiagnosticContextSupplier() throws Exception {
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(new RuntimeException());

        logger.log(TestMessages.Foo, Collections::emptyMap, new Exception());

        verifyZeroInteractions(lock, destination);
    }

    //endregion

    @Test
    public void with_shouldReturnANewBasicOpsLoggerWithAnOverriddenDiagnosticContextSupplier_givenADiagnosticContextSupplier() throws Exception {
        DiagnosticContextSupplier localSupplier = Collections::emptyMap;
        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;

        BasicOpsLogger<TestMessages> result = basicLogger.with(localSupplier);

        assertNotSame(basicLogger, result);
        assertSame(basicLogger.getClock(), result.getClock());
        assertSame(basicLogger.getErrorHandler(), result.getErrorHandler());
        assertSame(basicLogger.getDestination(), result.getDestination());
        assertSame(basicLogger.getLock(), result.getLock());
        assertSame(localSupplier, result.getDiagnosticContextSupplier());
        assertNotSame(basicLogger.getDiagnosticContextSupplier(), result.getDiagnosticContextSupplier());
    }

    @Test
    public void close_shouldCloseTheDestination() throws Exception {
        logger.close();

        verify(destination).close();
    }

    @Test
    public void close_shouldIgnoreCalls_givenANestedLoggerCreatedByWith() throws Exception {
        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        BasicOpsLogger<TestMessages> nested = basicLogger.with(Collections::emptyMap);

        nested.close();

        verifyZeroInteractions(destination);
    }

    private Map<String, String> generateCorrelationIds() {
        Map<String, String> result = new HashMap<>();
        result.put("foo", UUID.randomUUID().toString());
        result.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        result.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        return result;
    }

    @SafeVarargs
    private static Map<String, String> merge(Map<String,String>... contexts) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Stream.of(contexts).forEachOrdered(result::putAll);
        return result;
    }

    private enum TestMessages implements LogMessage {
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