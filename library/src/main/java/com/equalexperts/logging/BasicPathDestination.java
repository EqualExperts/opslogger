package com.equalexperts.logging;

import java.nio.channels.FileLock;
import java.util.concurrent.locks.Lock;

class BasicPathDestination<T extends Enum<T> & LogMessage> implements BasicOpsLogger.Destination<T> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Lock lock;
    private final RefreshableFileChannelProvider fileChannelProvider;
    private final StackTraceProcessor stackTraceProcessor;

    public BasicPathDestination(Lock lock, RefreshableFileChannelProvider fileChannelProvider, StackTraceProcessor stackTraceProcessor) {

        this.lock = lock;
        this.fileChannelProvider = fileChannelProvider;
        this.stackTraceProcessor = stackTraceProcessor;
    }

    @Override
    public void publish(LogicalLogRecord<T> record) throws Exception {
        String physicalRecord = record.format(stackTraceProcessor);
        lock.lock();
        try {
            RefreshableFileChannelProvider.Result result = fileChannelProvider.getChannel(record.getTimestamp());
            FileLock fileLock = result.channel.lock();
            try {
                result.writer.append(physicalRecord);
                result.writer.append(LINE_SEPARATOR);
                result.writer.flush();
            } finally {
                fileLock.release();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        fileChannelProvider.close();
    }
}
