package com.equalexperts.logging;

import java.io.*;

/*
    Test-friendly OutputStream implementations that track
    their constructor arguments
 */

class TestFriendlyPrintStream extends PrintStream {

    private final TestFriendlyFileOutputStream out;
    private final boolean autoFlush;

    public TestFriendlyPrintStream(TestFriendlyFileOutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        this.out = out;
        this.autoFlush = autoFlush;
    }

    TestFriendlyFileOutputStream getOut() {
        return out;
    }

    boolean getAutoFlush() {
        return autoFlush;
    }
}

class TestFriendlyFileOutputStream extends FileOutputStream {

    private final File file;
    private final boolean append;

    public TestFriendlyFileOutputStream(File file, boolean append) throws FileNotFoundException {
        super(file, append);
        this.file = file;
        this.append = append;
    }

    File getFile() {
        return file;
    }

    boolean getAppend() {
        return append;
    }
}