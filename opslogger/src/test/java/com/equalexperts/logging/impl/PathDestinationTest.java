package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.equalexperts.logging.impl.FileChannelProvider.Result;
import static java.util.Collections.emptyMap;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PathDestinationTest {

    private StringWriter writer = spy(new StringWriter());
    private FileChannel channel = mock(FileChannel.class);
    private FileLock lock = mock(FileLock.class);
    private FileChannelProvider provider = mock(FileChannelProvider.class);
    private StackTraceProcessor processor = mock(StackTraceProcessor.class);
    private ActiveRotationRegistry registry = mock(ActiveRotationRegistry.class);
    private PathDestination<TestMessages> destination = new PathDestination<>(provider, processor, registry);

    @Before
    public void setup() throws Exception {
        constructResult(writer, channel);
        doReturn(lock).when(channel).lock();
    }

    @Test
    public void beginBatch_shouldOpenAFileChannelAndLockTheFile() throws Exception {

        destination.beginBatch();

        verify(provider).getChannel();
        verify(channel).lock();
        verifyZeroInteractions(lock);
    }

    @Test
    public void publish_shouldFormatTheLogRecordAndWriteItToTheFile() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), new DiagnosticContext(emptyMap()), TestMessages.Foo, Optional.empty());
        record = spy(record); //use a spy so we can verify at the bottom
        destination.beginBatch();

        destination.publish(record);

        verify(record).format(processor);
        verify(writer, times(1)).write(isA(String.class)); //write in one pass to avoid a partial flush
        assertEquals(record.format(processor) + System.getProperty("line.separator"), writer.toString());
    }

    @Test
    public void publish_shouldNotFlushTheWriter() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), new DiagnosticContext(emptyMap()), TestMessages.Foo, Optional.empty());
        destination.beginBatch();

        destination.publish(record);

        verify(writer, never()).flush();
    }

    @Test
    public void endBatch_shouldFlushTheWriterReleaseTheFileLockAndCloseTheFileChannelAndWriter() throws Exception {
        destination.beginBatch();

        destination.endBatch();

        InOrder order = inOrder(writer, lock);
        order.verify(writer).flush();
        order.verify(lock).release();
        order.verify(writer).close();
    }

    @Test
    public void beginBatch_shouldCloseAndReopenFileChannelsAndLocks_whenThePreviousBatchWasNotEnded() throws Exception {
        reset(provider);
        Result firstResult = new Result(channel, writer);
        Result secondResult = new Result(mock(FileChannel.class), spy(new StringWriter()));
        FileLock secondLock = mock(FileLock.class);
        when(provider.getChannel()).thenReturn(firstResult, secondResult);
        doReturn(secondLock).when(secondResult.channel).lock();
        destination.beginBatch();

        destination.beginBatch();
        verify(writer).flush();
        verify(lock).release();
        verify(writer).close();
        verify(secondResult.channel).lock();
        verifyZeroInteractions(secondLock);
        verifyZeroInteractions(secondResult.writer);
    }

    @Test
    public void close_shouldReleaseTheFileLockAndCloseTheFileChannel_whenABatchIsOpen() throws Exception {
        destination.beginBatch();

        destination.close();

        InOrder order = inOrder(writer, lock);
        order.verify(writer).flush();
        order.verify(lock).release();
        order.verify(writer).close();
    }

    @Test
    public void close_shouldNotManipulateABatch_whenABatchIsNotOpen() throws Exception {
        destination.close();
    }

    @Test
    public void close_shouldRemoveThePathDestinationFromTheActiveRotationRegistry() throws Exception {
        destination.close();

        verify(registry).remove(destination);
    }

    @Test
    public void class_shouldImplementDestination() throws Exception {
        assertThat(destination, instanceOf(Destination.class));
    }

    @Test
    public void class_shouldImplementActiveRotationSupport() throws Exception {
        assertThat(destination, instanceOf(ActiveRotationSupport.class));
    }

    @Test
    public void refreshFileHandles_shouldReturnImmediately_whenTheDestinationHasNeverBeenUsed() throws Exception {
        ActiveRotationSupport ars = destination;

        ars.refreshFileHandles();
    }

    @Test
    public void refreshFileHandles_shouldReturnImmediately_whenTheDestinationIsNotCurrentlyInUse() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), new DiagnosticContext(emptyMap()), TestMessages.Foo, Optional.empty());
        destination.beginBatch();
        destination.publish(record);
        destination.endBatch();
        ActiveRotationSupport ars = destination;

        ars.refreshFileHandles();
    }

    @Test
    public void refreshFileHandles_shouldBlockUntilABatchIsClosed_givenAnOpenBatch() throws Exception {
        destination.beginBatch();
        AtomicBoolean callReturned = new AtomicBoolean(false);

        CountDownLatch startupLatch = new CountDownLatch(1);
        Thread rotationThread = new Thread(() -> {
            startupLatch.countDown();
            ActiveRotationSupport ars = destination;
            try {
                ars.refreshFileHandles();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            callReturned.set(true);
        });
        rotationThread.setDaemon(true);
        rotationThread.start();
        startupLatch.await(); //wait until the thread has started

        try {
            //the call to refreshFileHandles should not have returned
            Thread.sleep(100L);
            assertFalse(callReturned.get());

            destination.endBatch();

            rotationThread.join(100L); //now the call should return
            assertTrue(callReturned.get());
        } finally {
            if (rotationThread.isAlive()) {
                //need to call this to violently abort the thread if the thread is still alive at the end of the test
                //noinspection deprecation
                rotationThread.stop();
            }
        }
    }

    private void constructResult(Writer writer, FileChannel channel) throws IOException {
        Result expectedResult = new Result(channel, writer);
        when(provider.getChannel()).thenReturn(expectedResult);
    }

    private enum TestMessages implements LogMessage {
        Foo("CODE-Foo", "An event of some kind occurred");

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
