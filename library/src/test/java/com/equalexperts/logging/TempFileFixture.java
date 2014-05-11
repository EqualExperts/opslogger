package com.equalexperts.logging;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempFileFixture implements TestRule {
    private final List<File> tempFiles = new ArrayList<>();

    @Override
    public Statement apply(Statement base, Description description) {
        return statement(base, () -> tempFiles.stream()
                .filter(File::exists)
                .forEach(File::delete));
    }

    public File createTempFileThatDoesNotExist(String suffix) throws IOException {
        File result = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + suffix);
        return register(result);
    }

    public File register(File file) {
        file.deleteOnExit();
        tempFiles.add(file);
        return file;
    }

    /*
        Code to make creating Statements Java 8-friendly even though Statement is an abstract class
     */

    @FunctionalInterface
    private static interface StatementClosure {
        void evaluate() throws Throwable;
    }

    private static Statement statement(Statement base, StatementClosure closure) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
                closure.evaluate();
            }
        };
    }
}

