package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;

public interface Destination<T extends Enum<T> & LogMessage> extends AutoCloseable {
    void beginBatch() throws Exception;

    void publish(LogicalLogRecord<T> record) throws Exception;

    void endBatch() throws Exception;
}
