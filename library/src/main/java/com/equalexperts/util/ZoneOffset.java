package com.equalexperts.util;

public final class ZoneOffset {
    public final long offsetMillis;

    public ZoneOffset(long offsetMillis) {
        this.offsetMillis = offsetMillis;
    }

    public static final ZoneOffset UTC = new ZoneOffset(0); // TODO compute based on defaults
}
