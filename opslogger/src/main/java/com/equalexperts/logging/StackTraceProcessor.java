package com.equalexperts.logging;

/**
 * A processor that turns a throwable (and stack trace) into a representation
 * (normally single-line) suitable for a log file.
 */
interface StackTraceProcessor {
    void process(Throwable throwable, StringBuilder output) throws Exception;
}