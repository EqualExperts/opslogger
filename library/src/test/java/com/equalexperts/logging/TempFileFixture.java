package com.equalexperts.logging;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempFileFixture implements TestRule {
    private final List<Path> tempFiles = new ArrayList<>();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    for (Path path : tempFiles) {
                        File file = path.toFile();
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }
            }
        };
    }

    public Path createTempFile(String suffix) throws IOException {
        return register(Files.createTempFile("", suffix));
    }

    public Path createTempDirectory() throws IOException {
        return register(Files.createTempDirectory(null));
    }

    public Path createTempFileThatDoesNotExist(String suffix) throws IOException {
        Path result = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + suffix);
        return register(result);
    }

    public Path createTempDirectoryThatDoesNotExist() throws IOException {
        Path result = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        return register(result);
    }

    public Path register(Path path) {
        path.toFile().deleteOnExit();
        tempFiles.add(path);
        return path;
    }
}

