package com.equalexperts.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A StackTraceProcessor implementation that just includes the entire
 * stack trace as a multi-line string.
 */
class SimpleStackTraceProcessor implements StackTraceProcessor {
    @Override
    public String process(Throwable throwable) {
        StringWriter out = new StringWriter();
        throwable.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
