package com.equalexperts.logging;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static com.equalexperts.logging.FileChannelProvider.Result;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class BasicPathDestinationTest {
    @Mock private Lock lock;
    @Mock private FileChannelProvider fileChannelProvider;
    @Mock private StackTraceProcessor stackTraceProcessor;

    private BasicOpsLogger.Destination<TestMessages> destination;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        destination = new BasicPathDestination<>(lock, fileChannelProvider, stackTraceProcessor);
    }

    @Test
    public void publish_shouldFormatTheLogRecordAndWriteItToTheFile() throws Exception {
        StringWriter sw = spy(new StringWriter());
        constructResult(sw, null);

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
        record = spy(record); //use a spy so we can verify at the bottom

        destination.publish(record);

        verify(record).format(stackTraceProcessor);
        verify(sw, times(1)).write(isA(String.class)); //write in one pass to avoid a partial flush
        assertEquals(record.format(stackTraceProcessor) + System.getProperty("line.separator"), sw.toString());
    }

    @Test
    public void publish_shouldFlushTheWriterAfterWriting() throws Exception {
        Writer writer = mock(Writer.class);
        constructResult(writer, null);

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());

        destination.publish(record);

        //flush should be the very last call to the writer
        InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer).flush();
        inOrder.verify(writer).close();
        inOrder.verifyNoMoreInteractions();

    }

    @Test
    public void publish_shouldObtainAThreadLockFileChannelAndFileLockWhenWritingToTheFile() throws Exception {
        LogicalLogRecord<TestMessages> record = spy(new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty()));

        Writer writer = spy(new StringWriter()); //use a spy so append/write doesn't matter
        FileChannel channel = mock(FileChannel.class);
        Result result = constructResult(writer, channel);
        FileLock fileLock = mock(FileLock.class);
        when(channel.lock()).thenReturn(fileLock);

        destination.publish(record);

        /*
            order is important:
                thread lock, then file lock,
                then (write/append and) flush,
                then release file lock, then release thread lock
         */
        InOrder order = inOrder(record, lock, channel, fileLock, writer, result, fileChannelProvider);
        order.verify(record).format(stackTraceProcessor); //should happen outside the critical section
        order.verify(lock).lock();
        order.verify(fileChannelProvider).getChannel();
        order.verify(channel).lock();
        order.verify(writer, atLeastOnce()).write(isA(String.class));
        order.verify(writer).flush();
        order.verify(fileLock).release();
        order.verify(result).close();
        order.verify(lock).unlock();
    }

    @Test
    public void publish_shouldReleaseTheThreadLock_whenTheFileLockCouldNotBeObtained() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
        FileChannel fileChannel = mock(FileChannel.class);
        constructResult(null, fileChannel);

        IOException expectedException = new IOException();
        when(fileChannel.lock()).thenThrow(expectedException);

        try {
            destination.publish(record);
            fail("expected an exception");
        } catch (Exception e) {
            assertSame(expectedException, e);
        }

        InOrder order = inOrder(fileChannel, lock);
        order.verify(fileChannel).lock();
        order.verify(lock).unlock();
    }

    @Test
    public void publish_shouldReleaseTheThreadLock_whenTheChannelCouldNotBeObtained() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());

        IOException expectedException = new IOException();
        when(fileChannelProvider.getChannel()).thenThrow(expectedException);

        try {
            destination.publish(record);
            fail("expected an exception");
        } catch (Exception e) {
            assertSame(expectedException, e);
        }

        InOrder order = inOrder(fileChannelProvider, lock);
        order.verify(fileChannelProvider).getChannel();
        order.verify(lock).unlock();
    }

    @Test
    public void publish_shouldReleaseTheThreadAndFileLocks_givenAProblemInteractingWithTheWriter() throws Exception {
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());

        Writer writer = mock(Writer.class);
        FileChannel channel = mock(FileChannel.class);
        constructResult(writer, channel);

        FileLock fileLock = mock(FileLock.class);
        when(channel.lock()).thenReturn(fileLock);

        IOException expectedException = new IOException();
        when(writer.append(isA(String.class))).thenThrow(expectedException);
        doThrow(expectedException).when(writer).write(isA(String.class));

        try {
            destination.publish(record);
            fail("expected an exception");
        } catch (Exception e) {
            assertSame(expectedException, e);
        }

        verify(lock).unlock();
    }

    @Test
    public void close_shouldDoNothing() throws Exception {
        destination.close();

        verifyZeroInteractions(fileChannelProvider);
    }

    private Result constructResult(Writer writer, FileChannel channel) throws IOException {
        if (writer == null) {
            writer = mock(Writer.class);
        }
        if (channel == null) {
            channel = mock(FileChannel.class);
            when(channel.lock()).thenReturn(mock(FileLock.class));
        }
        Result expectedResult = spy(new Result(channel, writer));
        when(fileChannelProvider.getChannel()).thenReturn(expectedResult);
        return expectedResult;
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
