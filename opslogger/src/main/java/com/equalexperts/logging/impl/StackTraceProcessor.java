package com.equalexperts.logging.impl;

/**
 * A processor that turns a throwable (and stack trace) into a representation
 * (normally single-line) suitable for a log file.
 */
public interface StackTraceProcessor {
    void process(Throwable throwable, StringBuilder output) throws Exception;
}