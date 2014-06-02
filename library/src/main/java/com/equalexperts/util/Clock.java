package com.equalexperts.util;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

public abstract class Clock {

    public abstract Instant instant();

    public static class SystemClock extends Clock {
        private final DateTimeZone tz;

        public SystemClock(DateTimeZone tz) {
            this.tz = tz;
        }

        public Instant instant() {
            return new Instant(tz.convertUTCToLocal(System.currentTimeMillis()));
        }
    }

    private static final Clock systemUTC = new SystemClock(DateTimeZone.UTC);

    public static Clock systemUTC() {
        return systemUTC;
    }

    public static Clock systemDefaultZone() {
        return new SystemClock(DateTimeZone.getDefault());
    }

    public static Clock fixed(final Instant requiredInstant, final DateTimeZone tz) {
        return new Clock() {
            final Instant adjustedInstant = new Instant(tz.convertUTCToLocal(requiredInstant.getMillis()));

            public Instant instant() {
                return adjustedInstant;
            }
        };
    }
}
