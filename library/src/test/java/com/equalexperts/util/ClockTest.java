package com.equalexperts.util;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClockTest {

    @Test
    public void testFixedClockShouldBeRepeatable() throws Exception {
        Instant instant = new Instant(123456789L);
        Clock f1 = Clock.fixed(instant, DateTimeZone.UTC);
        Clock f2 = Clock.fixed(instant, DateTimeZone.forOffsetHours(8));
        Instant f1a = f1.instant();
        Thread.sleep(100);
        Instant f1b = f1.instant();
        assertEquals(f1a, f1b);
        Instant f2a = f2.instant();
        assertEquals(f2a, f1a.withDurationAdded(Duration.standardHours(8), 1));
    }

    @Test
    public void testSystemClockShouldRiseEachInvocation() throws Exception {
        Clock s1 = Clock.systemUTC();
        Instant s1a = s1.instant();
        Thread.sleep(100);
        Instant s1b = s1.instant();
        Thread.sleep(100);
        Instant s1c = s1.instant();
        assertTrue(s1a.getMillis() < s1b.getMillis());
        assertTrue(s1b.getMillis() < s1c.getMillis());
    }
}
