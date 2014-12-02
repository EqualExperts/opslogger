package com.equalexperts.logging;

import java.nio.channels.FileLock;
import java.util.concurrent.locks.Lock;

/** LogRecord focused Destination.  Before writing each log record to the destination a file lock is acquired,
 * and released afterwards.  This allows external log rotation to work.
 * @param <T>
 */
class BasicPathDestination<T extends Enum<T> & LogMessage> implements BasicOpsLogger.Destination<T> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Lock lock;
    private final FileChannelProvider fileChannelProvider;
    private final StackTraceProcessor stackTraceProcessor;

    public BasicPathDestination(Lock lock, FileChannelProvider fileChannelProvider, StackTraceProcessor stackTraceProcessor) {
        this.lock = lock;
        this.fileChannelProvider = fileChannelProvider;
        this.stackTraceProcessor = stackTraceProcessor;
    }

    @Override
    public void publish(LogicalLogRecord<T> record) throws Exception {
        String physicalRecord = record.format(stackTraceProcessor);
        lock.lock();
        try {
            try (FileChannelProvider.Result result = fileChannelProvider.getChannel();
                 FileLock ignore = result.channel.lock()) {
                result.writer.write(physicalRecord + LINE_SEPARATOR);
                result.writer.flush();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
    }

    public Lock getLock() {
        return lock;
    }

    public FileChannelProvider getFileChannelProvider() {
        return fileChannelProvider;
    }

    public StackTraceProcessor getStackTraceProcessor() {
        return stackTraceProcessor;
    }
}
