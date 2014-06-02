package com.equalexperts.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class Base64Test {
    // Test vectors are from RFC4648
    // http://tools.ietf.org/html/rfc4648

    @Test
    public void testRFC4648Vectors() {
        assertMatch("", "");
        assertMatch("f", "Zg==");
        assertMatch("fo", "Zm8=");
        assertMatch("foo", "Zm9v");
        assertMatch("foob", "Zm9vYg==");
        assertMatch("fooba", "Zm9vYmE=");
        assertMatch("foobar", "Zm9vYmFy");
    }

    private void assertMatch(String string, String b64Expected) {
        String encoded = Base64.encodeBytes(string.getBytes(StandardCharsets.UTF_8));
        assertEquals(b64Expected, encoded);
    }
}
