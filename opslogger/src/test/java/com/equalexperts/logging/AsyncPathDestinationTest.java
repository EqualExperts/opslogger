package com.equalexperts.logging;

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

import static com.equalexperts.logging.FileChannelProvider.Result;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AsyncPathDestinationTest {

    private StringWriter writer = spy(new StringWriter());
    private FileChannel channel = mock(FileChannel.class);
    private FileLock lock = mock(FileLock.class);
    private FileChannelProvider provider = mock(FileChannelProvider.class);
    private StackTraceProcessor processor = mock(StackTraceProcessor.class);
    private AsyncOpsLogger.Destination<TestMessages> destination = new AsyncPathDestination<>(provider, processor);

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
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
        record = spy(record); //use a spy so we can verify at the bottom
        destination.beginBatch();

        destination.publish(record);

        verify(record).format(processor);
        verify(writer, times(1)).write(isA(String.class)); //write in one pass to avoid a partial flush
        assertEquals(record.format(processor) + System.getProperty("line.separator"), writer.toString());
    }

    @Test
    public void publish_shouldNotFlushTheWriter() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
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
    public void close_shouldDoNothing_whenABatchIsNotOpen() throws Exception {
        destination.close();
    }

    private void constructResult(Writer writer, FileChannel channel) throws IOException {
        Result expectedResult = new Result(channel, writer);
        when(provider.getChannel()).thenReturn(expectedResult);
    }

    private static enum TestMessages implements LogMessage {
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
