package com.equalexperts.util;

import org.joda.time.Instant;

public abstract class Clock {

    public abstract Instant instant();

    public static class SystemClock extends Clock {
        private final long offsetFromUTC;

        public SystemClock(long offsetFromUTC) {
            this.offsetFromUTC = offsetFromUTC;
        }

        public Instant instant() {
            return new Instant(System.currentTimeMillis() + offsetFromUTC);
        }
    }

    private static final Clock systemDefault = new SystemClock(0);
    private static final Clock systemUTC = new SystemClock(0); // TODO UTC correction

    public static Clock systemDefaultZone() {
        return systemDefault;
    }

    public static Clock systemUTC() {
        return systemUTC;
    }

    public static Clock fixed(final Instant requiredInstant, final ZoneOffset offset) {
        return new Clock() {
            final Instant adjustedInstant = new Instant(requiredInstant.getMillis() + offset.offsetMillis);

            public Instant instant() {
                return adjustedInstant;
            }
        };
    }
}
