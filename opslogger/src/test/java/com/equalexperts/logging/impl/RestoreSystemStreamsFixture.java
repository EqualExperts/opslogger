package com.equalexperts.logging.impl;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.*;

/**
 * Resets standard input, output, and error at the end of tests,
 * and prevents any standard streams from being closed.
 */
public class RestoreSystemStreamsFixture implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                InputStream originalSystemIn = System.in;
                PrintStream originalSystemOut = System.out;
                PrintStream originalSystemErr = System.err;

                System.setIn(new NonCloseableInputStream(originalSystemIn));
                System.setOut(new NonCloseablePrintStream(originalSystemOut));
                System.setErr(new NonCloseablePrintStream(originalSystemErr));
                try {
                    base.evaluate();
                } finally {
                    System.setIn(originalSystemIn);
                    System.setOut(originalSystemOut);
                    System.setErr(originalSystemErr);
                }
            }
        };
    }

    private static class NonCloseableInputStream extends FilterInputStream {
        NonCloseableInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            //don't close
        }
    }

    private static class NonCloseablePrintStream extends PrintStream {
        NonCloseablePrintStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() {
            super.flush();
            //don't close
        }
    }
}
