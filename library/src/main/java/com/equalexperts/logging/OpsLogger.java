package com.equalexperts.logging;

public interface OpsLogger<T extends Enum<T> & LogMessage> {
    void log(T message, Object... details);
    void log(T message, Throwable cause, Object... details);
}
