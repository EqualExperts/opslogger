package com.equalexperts.logging;

interface Destination<T extends Enum<T> & LogMessage> extends AutoCloseable {
    void beginBatch() throws Exception;

    void publish(LogicalLogRecord<T> record) throws Exception;

    void endBatch() throws Exception;
}
