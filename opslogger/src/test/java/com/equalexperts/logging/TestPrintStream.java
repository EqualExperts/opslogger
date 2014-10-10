package com.equalexperts.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class TestPrintStream extends PrintStream {
    TestPrintStream() {
        super(new ByteArrayOutputStream(), true);
    }

    private boolean closed = false;

    @Override
    public String toString() {
        ByteArrayOutputStream out = (ByteArrayOutputStream) super.out;
        return new String(out.toByteArray());
    }

    @Override
    public void close() {
        closed = true;
        super.close();
    }

    public boolean isClosed() {
        return closed;
    }
}
