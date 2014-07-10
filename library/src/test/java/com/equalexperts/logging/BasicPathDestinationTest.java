package com.equalexperts.logging;

import org.junit.Before;
import org.junit.Rule;
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

import static com.equalexperts.logging.RefreshableFileChannelProvider.Result;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class BasicPathDestinationTest {

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Mock private Lock lock;
    @Mock private RefreshableFileChannelProvider fileChannelProvider;
    @Mock private StackTraceProcessor stackTraceProcessor;

    private BasicOpsLogger.Destination<TestMessages> destination;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        destination = new BasicPathDestination<>(lock, fileChannelProvider, stackTraceProcessor);
    }

    @Test
    public void publish_shouldFormatTheLogRecordAndWriteItToTheFile() throws Exception {
        StringWriter sw = new StringWriter();
        constructResult(sw, null);

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());
        record = spy(record); //use a spy so we can verify at the bottom

        destination.publish(record);

        verify(record).format(stackTraceProcessor);
        assertEquals(record.format(stackTraceProcessor) + System.getProperty("line.separator"), sw.toString());
    }

    @Test
    public void publish_shouldFlushTheWriterAfterWriting() throws Exception {
        Writer writer = mock(Writer.class);
        constructResult(writer, null);

        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty());

        destination.publish(record);

        InOrder inOrder = Mockito.inOrder(writer);
        inOrder.verify(writer, atLeastOnce()).append(Mockito.isA(String.class));
        inOrder.verify(writer).flush();
        inOrder.verifyNoMoreInteractions();

    }

    @Test
    public void publish_shouldUseTheLogRecordTimestampToObtainTheFileChannel() throws Exception {
        Instant logRecordTimestamp = Instant.parse("2013-12-29T18:15:00.123Z");
        LogicalLogRecord<TestMessages> record = new LogicalLogRecord<>(logRecordTimestamp, null, TestMessages.Foo, Optional.empty());
        constructResult(null, null);

        destination.publish(record);

        verify(fileChannelProvider).getChannel(logRecordTimestamp);
        verifyNoMoreInteractions(fileChannelProvider);
    }

    @Test
    public void publish_shouldObtainAThreadAndFileLockWhenWritingToTheFile() throws Exception {
        LogicalLogRecord<TestMessages> record = spy(new LogicalLogRecord<>(Instant.now(), null, TestMessages.Foo, Optional.empty()));

        Writer writer = mock(Writer.class);
        FileChannel channel = mock(FileChannel.class);
        constructResult(writer, channel);
        FileLock fileLock = mock(FileLock.class);
        when(channel.lock()).thenReturn(fileLock);

        destination.publish(record);

        /*
            order is important:
                thread lock, then file lock,
                then write,
                then release file lock, then release thread lock
         */
        InOrder order = inOrder(record, lock, channel, fileLock, writer);
        order.verify(record).format(stackTraceProcessor); //can happen outside the critical section
        order.verify(lock).lock();
        order.verify(channel).lock();
        order.verify(writer, atLeast(0)).write(isA(String.class));
        order.verify(writer).flush();
        order.verify(fileLock).release();
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
        when(fileChannelProvider.getChannel(any())).thenThrow(expectedException);

        try {
            destination.publish(record);
            fail("expected an exception");
        } catch (Exception e) {
            assertSame(expectedException, e);
        }

        InOrder order = inOrder(fileChannelProvider, lock);
        order.verify(fileChannelProvider).getChannel(any());
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

        try {
            destination.publish(record);
            fail("expected an exception");
        } catch (Exception e) {
            assertSame(expectedException, e);
        }

        InOrder order = inOrder(writer, fileLock, lock);
        order.verify(writer, atLeastOnce()).append(isA(String.class));
        order.verify(fileLock).release();
        order.verify(lock).unlock();
    }

    @Test
    public void close_shouldCloseTheFileChannelProvider() throws Exception {
        destination.close();

        verify(fileChannelProvider).close();
    }

    private void constructResult(Writer writer, FileChannel channel) throws IOException {
        if (writer == null) {
            writer = mock(Writer.class);
        }
        if (channel == null) {
            channel = mock(FileChannel.class);
            when(channel.lock()).thenReturn(mock(FileLock.class));
        }
        Result expectedResult = new Result(channel, writer, Instant.now());
        when(fileChannelProvider.getChannel(isA(Instant.class))).thenReturn(expectedResult);
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
