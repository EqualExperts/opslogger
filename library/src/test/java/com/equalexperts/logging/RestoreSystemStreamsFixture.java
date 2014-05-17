package com.equalexperts.logging;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.InputStream;
import java.io.PrintStream;

public class RestoreSystemStreamsFixture implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                InputStream originalSystemIn = System.in;
                PrintStream originalSystemOut = System.out;
                PrintStream originalSystemErr = System.err;
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
}
