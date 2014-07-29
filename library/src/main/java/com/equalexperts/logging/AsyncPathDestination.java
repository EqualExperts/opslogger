package com.equalexperts.logging;

import java.io.IOException;
import java.nio.channels.FileLock;

class AsyncPathDestination<T extends Enum<T> & LogMessage> implements AsyncOpsLogger.Destination<T> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final FileChannelProvider provider;
    private final StackTraceProcessor processor;
    private FileChannelProvider.Result currentChannel;
    private FileLock currentLock;

    public AsyncPathDestination(FileChannelProvider provider, StackTraceProcessor processor) {

        this.provider = provider;
        this.processor = processor;
    }

    @Override
    public void beginBatch() throws Exception {
        closeAnyOpenBatch();
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
    }
}
