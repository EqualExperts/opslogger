package com.equalexperts.logging.impl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ThrowableFingerprintCalculator {
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /* Return the Base64 representation of the MD5 hash for the printed stack trace of <code>t</code>. */

    public String calculateFingerprint(Throwable t) {
        MessageDigest md5 = getMD5Instance();
        PrintStream ps = new PrintStream(new DigestOutputStream(new DoNothingOutputStream(), md5));
        t.printStackTrace(ps);
        return base64Encoder.encodeToString(md5.digest());
    }

    private MessageDigest getMD5Instance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignore) {
            throw new AssertionError("Every JDK provides an MD5 MessageDigest implementation");
        }
    }

    private static class DoNothingOutputStream extends OutputStream {
        @Override
        public void write(byte[] b, int off, int len) {}

        @Override
        public void write(int b) {}
    }
}
