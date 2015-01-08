package com.equalexperts.logging.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A StackTraceProcessor implementation that just includes the entire
 * stack trace as a multi-line string.
 */
public class SimpleStackTraceProcessor implements StackTraceProcessor {
    @Override
    public void process(Throwable throwable, StringBuilder out) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        out.append(stripLastCharacter(sw));
    }

    String stripLastCharacter(StringWriter sw) {
        String result = sw.toString();
        return result.substring(0, result.length() - 1);
    }
}
