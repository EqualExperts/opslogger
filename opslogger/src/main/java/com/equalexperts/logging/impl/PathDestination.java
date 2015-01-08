package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.concurrent.CountDownLatch;

/**
 * Writes batches of log records to a path.
 *
 * A file lock is acquired and held during the batch and released afterwards.
 * This allows external log rotation to work.
 * @param <T>
 */
public class PathDestination<T extends Enum<T> & LogMessage> implements Destination<T>, ActiveRotationSupport {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final FileChannelProvider provider;
    private final StackTraceProcessor processor;
    private final ActiveRotationRegistry registry;
    private FileChannelProvider.Result currentChannel;
    private FileLock currentLock;
    private volatile CountDownLatch latch = new CountDownLatch(0);

    public PathDestination(FileChannelProvider provider, StackTraceProcessor processor, ActiveRotationRegistry registry) {
        this.provider = provider;
        this.processor = processor;
        this.registry = registry;
    }

    @Override
    public void beginBatch() throws Exception {
        closeAnyOpenBatch();
        latch = new CountDownLatch(1);
        currentChannel = provider.getChannel();
        currentLock = currentChannel.channel.lock();
    }

    @Override
    public void publish(LogicalLogRecord<T> record) throws Exception {
        String physicalRecord = record.format(processor);
        currentChannel.writer.write(physicalRecord + LINE_SEPARATOR); //one call avoids a partial flush
    }

    @Override
    public void endBatch() throws Exception {
        closeAnyOpenBatch();
    }

    private void closeAnyOpenBatch() throws IOException {
        latch.countDown();
        if (currentChannel != null) {
            currentChannel.writer.flush();
            currentLock.release();
            currentChannel.writer.close();
            currentLock = null;
            currentChannel = null;
        }
    }

    @Override
    public void close() throws Exception {
        closeAnyOpenBatch();
        registry.remove(this);
    }

    @Override
    public void refreshFileHandles() throws InterruptedException {
        latch.await();
    }

    public FileChannelProvider getProvider() {
        return provider;
    }

    public StackTraceProcessor getStackTraceProcessor() {
        return processor;
    }
}
