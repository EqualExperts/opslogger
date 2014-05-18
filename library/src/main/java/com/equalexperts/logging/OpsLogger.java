package com.equalexperts.logging;

import java.io.Closeable;

public interface OpsLogger<T extends Enum<T> & LogMessage> extends Closeable {
    void log(T message, Object... details);
    void log(T message, Throwable cause, Object... details);
}