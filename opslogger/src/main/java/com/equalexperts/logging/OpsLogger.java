package com.equalexperts.logging;

public interface OpsLogger<T extends Enum<T> & LogMessage> extends AutoCloseable {
    void log(T message, Object... details);
    void log(T message, Throwable cause, Object... details);
}