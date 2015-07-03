package com.equalexperts.logging.impl;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.OpsLogger;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class AsyncOpsLoggerTest {

    public static final int EXPECTED_MAX_BATCH_SIZE = AsyncOpsLogger.MAX_BATCH_SIZE;
    private Clock fixedClock = Clock.fixed(Instant.parse("2014-02-01T14:57:12.500Z"), ZoneOffset.UTC);
    @Mock private Destination<TestMessages> destination;
    @Mock private DiagnosticContextSupplier diagnosticContextSupplier;
    @Mock private Consumer<Throwable> exceptionConsumer;
    @Mock private LinkedTransferQueue<Optional<LogicalLogRecord<TestMessages>>> transferQueue;
    @Mock private AsyncExecutor executor;
    @Mock private Future<?> processingThread;

    @Captor private ArgumentCaptor<Optional<LogicalLogRecord<TestMessages>>> captor;
    @Captor private ArgumentCaptor<Runnable> runnableCaptor;

    private OpsLogger<TestMessages> logger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(executor.execute(runnableCaptor.capture())).thenAnswer((i) -> processingThread);

        logger = new AsyncOpsLogger<>(fixedClock, diagnosticContextSupplier, destination, exceptionConsumer, transferQueue, executor);
    }

    @Test
    public void constructor_shouldSpawnAnAsynchronousProcessingThread() throws Exception {

        verify(executor).execute(any());
    }

    @Test
    public void log_shouldAddALogicalLogRecordToTheQueue_givenALogMessageInstance() throws Exception {
        Map<String,String> expectedCorrelationIds = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(expectedCorrelationIds);
        doNothing().when(transferQueue).put(captor.capture());

        logger.log(TestMessages.Bar, 64, "Hello, World");

        verify(transferQueue).put(captor.capture());

        LogicalLogRecord<TestMessages> record = captor.getValue().get();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(expectedCorrelationIds, record.getCorrelationIds());
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
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemAddingAMessageToTheQueue() throws Exception {
        RuntimeException expectedThrowable = new RuntimeException("blah");
        doThrow(expectedThrowable).when(transferQueue).put(any());

        logger.log(TestMessages.Foo);

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }


    @Test
    public void log_shouldAddALogicalLogRecordToTheQueue_givenALogMessageInstanceAndAThrowable() throws Exception {
        Map<String, String> expectedCorrelationIds = generateCorrelationIds();
        when(diagnosticContextSupplier.getMessageContext()).thenReturn(expectedCorrelationIds);

        Throwable expectedCause = new RuntimeException();

        doNothing().when(transferQueue).put(captor.capture());

        logger.log(TestMessages.Bar, expectedCause, 64, "Hello, World");

        verify(transferQueue).put(captor.capture());

        LogicalLogRecord<TestMessages> record = captor.getValue().get();
        assertEquals(fixedClock.instant(), record.getTimestamp());
        assertEquals(expectedCorrelationIds, record.getCorrelationIds());
        assertEquals(TestMessages.Bar, record.getMessage());
        assertNotNull(record.getCause());
        assertTrue(record.getCause().isPresent());
        assertSame(expectedCause, record.getCause().get());
        assertArrayEquals(new Object[] {64, "Hello, World"}, record.getDetails());
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemCreatingTheLogRecordAndAThrowable() throws Exception {
        logger.log(null, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.isA(NullPointerException.class));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemObtainingCorrelationIdsAndAThrowable() throws Exception {
        Error expectedThrowable = new Error();
        when(diagnosticContextSupplier.getMessageContext()).thenThrow(expectedThrowable);

        logger.log(TestMessages.Foo, new RuntimeException());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void log_shouldExposeAnExceptionToTheHandler_givenAProblemAddingAMessageToTheQueueAndAThrowable() throws Exception {
        RuntimeException expectedThrowable = new RuntimeException("blah");
        doThrow(expectedThrowable).when(transferQueue).put(any());

        logger.log(TestMessages.Foo, new Exception());

        verify(exceptionConsumer).accept(Mockito.same(expectedThrowable));
    }

    @Test
    public void close_shouldSendAStopSignalToTheProcessingThreadWaitForItToFinishAndCloseTheDestination() throws Exception {
        logger.close();

        InOrder order = inOrder(transferQueue, processingThread, destination);
        order.verify(transferQueue).put(argThat(isEmpty()));
        order.verify(processingThread).get();
        order.verify(destination).close();
    }

    @Test
    public void close_shouldCloseTheDestination_whenAnExceptionIsThrownWaitingForTheProcessingThreadToFinish() throws Exception {
        when (processingThread.get()).thenThrow(new InterruptedException());

        try {
            logger.close();
        } catch (InterruptedException ignore) {}

        verify(destination).close();
    }

    @Test
    public void processingThread_shouldTakeBatchesOfUpTo100InALoopUntilItReceivesAnEmptyOptional() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(141).stream().map(Optional::of).collect(toList());
        messages.add(130, Optional.empty()); //add in the middle of the last block — loop should process all messages in the last batch, regardless of where the signal is
        setupTransferQueueExpectations(messages, 60, 40, 42);

        runnableCaptor.getValue().run();

        InOrder order = inOrder(transferQueue);
        //expect three take/drainTo combination calls (take blocks, drainTo does not)
        order.verify(transferQueue).take();
        order.verify(transferQueue).drainTo(any(), eq(EXPECTED_MAX_BATCH_SIZE - 1));
        order.verify(transferQueue).take();
        order.verify(transferQueue).drainTo(any(), eq(EXPECTED_MAX_BATCH_SIZE - 1));
        order.verify(transferQueue).take();
        order.verify(transferQueue).drainTo(any(), eq(EXPECTED_MAX_BATCH_SIZE - 1));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void processingThread_shouldSubmitReceivedMessagesToTheDestinationInBatches() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(5).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 4, 2);

        runnableCaptor.getValue().run();

        InOrder order = inOrder(destination);
        order.verify(destination).beginBatch();
        order.verify(destination).publish(messages.get(0).get());
        order.verify(destination).publish(messages.get(1).get());
        order.verify(destination).publish(messages.get(2).get());
        order.verify(destination).publish(messages.get(3).get());
        order.verify(destination).endBatch();
        order.verify(destination).beginBatch();
        order.verify(destination).publish(messages.get(4).get());
        order.verify(destination).endBatch();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void processingThread_shouldExposeAnExceptionToTheHandlerAndContinueProcessing_givenAnException() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(2).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 1, 1, 1);

        Exception expectedException = new Exception("something went wrong");
        doThrow(expectedException).when(destination).publish(messages.get(0).get());

        runnableCaptor.getValue().run();

        verify(exceptionConsumer).accept(expectedException);
        verify(destination).publish(messages.get(1).get());
    }

    @Test
    public void processingThread_shouldSkipTheEntireBatch_whenAnExceptionIsThrownByBeginBatch() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(2).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 1, 2);

        Exception expectedException = new Exception("error starting batch");
        doThrow(expectedException).doNothing().when(destination).beginBatch();

        runnableCaptor.getValue().run();

        InOrder order = inOrder(destination, exceptionConsumer);
        order.verify(destination).beginBatch();
        order.verify(exceptionConsumer).accept(expectedException);
        order.verify(destination).beginBatch();
        order.verify(destination).publish(messages.get(1).get());
    }

    @Test
    public void processingThread_shouldCloseTheBatch_whenAnExceptionIsThrownPublishingABatchMember() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(1).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 1, 1);

        doThrow(new Exception("error starting batch")).when(destination).publish(messages.get(0).get());

        runnableCaptor.getValue().run();

        InOrder order = inOrder(destination);
        order.verify(destination).beginBatch();
        order.verify(destination).endBatch();
    }

    @Test
    public void processingThread_shouldContinueProcessingTheBatch_whenAnExceptionIsThrownPublishingABatchMember() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(4).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 4, 1); //send close in a separate batch to stop an infinite loop when the test fails

        Exception expectedException = new Exception("error starting batch");
        doThrow(expectedException).when(destination).publish(messages.get(1).get());

        runnableCaptor.getValue().run();

        verify(destination, times(4)).publish(any());
        verify(exceptionConsumer).accept(expectedException);
    }

    @Test
    public void processingThread_shouldEndEvenIfABatchErrorOccursInTheLastBatch() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = buildMessages(1).stream().map(Optional::of).collect(toList());
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 2);

        doThrow(new Exception()).when(destination).beginBatch();

        runnableCaptor.getValue().run();
    }

    @Test
    public void processingThread_shouldNotBeginOrEndAnEmptyBatch() throws Exception {
        List<Optional<LogicalLogRecord<TestMessages>>> messages = new ArrayList<>();
        messages.add(Optional.empty());
        setupTransferQueueExpectations(messages, 1);

        runnableCaptor.getValue().run();

        verifyZeroInteractions(destination);
    }

    private void setupTransferQueueExpectations(List<Optional<LogicalLogRecord<TestMessages>>> messages, int... batchSizes) throws Exception {
        int totalSize = IntStream.of(batchSizes)
                .peek((i) -> assertTrue("A TransferQueue will never return more than asked for", i <= EXPECTED_MAX_BATCH_SIZE))
                .sum();
        assertEquals("precondition: message size must equal total size of all batches", messages.size(), totalSize);

        final List<Integer> batchSizeList = IntStream.of(batchSizes).mapToObj((i) -> i).collect(toList());

        /*
            batches are retrieved in a sequence of take(), drainTo() calls,
            but we can't interleave when calls to the two mocks.
            solution: mock the takes calls first, and then the drainTo calls.
        */

        List<Optional<LogicalLogRecord<TestMessages>>> takeCallResults = new ArrayList<>();
        List<List<Optional<LogicalLogRecord<TestMessages>>>> drainCallResults = new ArrayList<>();
        int positionSoFar = 0;
        for (int i : batchSizeList) {
            takeCallResults.add(messages.get(positionSoFar));
            drainCallResults.add(messages.subList(positionSoFar + 1, positionSoFar + i));
            positionSoFar += i;
        }

        OngoingStubbing<Optional<LogicalLogRecord<TestMessages>>> takeCall = when(transferQueue.take()).thenReturn(takeCallResults.get(0));
        for (Optional<LogicalLogRecord<TestMessages>> takeResult : takeCallResults.subList(1, takeCallResults.size())) {
            takeCall = takeCall.thenReturn(takeResult);
        }

        Answer<Object> drainAnswer = invocation -> {

            assertThat("too many calls to drainTo", takeCallResults.size(), not(is(0)));
            assertThat("some sort of error in mock setup", drainCallResults.size(), is(takeCallResults.size()));

            @SuppressWarnings("unchecked")
            List<Optional<LogicalLogRecord<TestMessages>>> collection = (List<Optional<LogicalLogRecord<TestMessages>>>) invocation.getArguments()[0];
            int batchSize = batchSizeList.get(0);


            assertEquals(1, collection.size());
            assertThat(collection, CoreMatchers.hasItem(takeCallResults.get(0)));
            drainCallResults.get(0).stream().forEach(collection::add);


            takeCallResults.remove(0);
            drainCallResults.remove(0);
            return batchSize;
        };

        OngoingStubbing<Integer> drainCall = when(transferQueue.drainTo(any(), eq(EXPECTED_MAX_BATCH_SIZE - 1))).thenAnswer(drainAnswer);

        for (int i = 1; i < drainCallResults.size(); i++) {
            drainCall = drainCall.thenAnswer(drainAnswer);
        }
    }

    private List<LogicalLogRecord<TestMessages>> buildMessages(int count) {
        return IntStream.range(0, count)
                .mapToObj((i) -> constructLogicalLogMessage(TestMessages.Bar, i, "Hello"))
                .collect(toList());
    }

    private LogicalLogRecord<TestMessages> constructLogicalLogMessage(TestMessages message, Object... args) {
        return new LogicalLogRecord<>(Instant.now(), emptyMap(), message, Optional.empty(), args);
    }

    private Map<String, String> generateCorrelationIds() {
        Map<String, String> result = new HashMap<>();
        result.put("foo", "fooValue");
        result.put("bar", "barValue");
        return result;
    }

    private static <T> Matcher<Optional<T>> isEmpty() {
        return new BaseMatcher<Optional<T>>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof Optional)) {
                    return false;
                }
                Optional<?> optional = (Optional<?>) item;
                return !optional.isPresent();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("an empty optional");
            }
        };
    }

    private enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "An event occurred"),
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
