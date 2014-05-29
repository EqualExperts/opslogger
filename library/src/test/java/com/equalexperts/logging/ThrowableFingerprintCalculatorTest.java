package com.equalexperts.logging;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThrowableFingerprintCalculatorTest {
    private final ThrowableFingerprintCalculator calculator = new ThrowableFingerprintCalculator();

    @Test
    public void calculateFingerprint_shouldGenerateTheSameFingerprint_givenTwoIdenticalThrowables() throws Exception {
        //ensure the exceptions are identical by giving them the same message, same class, and same stack trace
        String exceptionMessage = "message";
        Throwable firstException = new RuntimeException(exceptionMessage);
        Throwable secondException = new RuntimeException(exceptionMessage);
        firstException.setStackTrace(constructCustomStackTrace());
        secondException.setStackTrace(constructCustomStackTrace());

        String firstFingerprint = calculator.calculateFingerprint(firstException);
        String secondFingerprint = calculator.calculateFingerprint(secondException);

        assertEquals(firstFingerprint, secondFingerprint);
    }

    @Test
    public void calculateFingerprint_shouldReturnTheSameFingerprint_givenTheSameExceptionTwice() throws Exception {
        Throwable t = new RuntimeException();
        String expectedFingerprint = calculator.calculateFingerprint(t);

        String actualFingerprint = calculator.calculateFingerprint(t);

        assertEquals(expectedFingerprint, actualFingerprint);
    }

    @Test
    public void calculateFingerprint_shouldReturnADifferentFingerprint_whenTheStackTraceChanges() throws Exception {
        Throwable t = new RuntimeException();
        String originalFingerprint = calculator.calculateFingerprint(t);

        t.setStackTrace(constructCustomStackTrace());
        String newFingerprint = calculator.calculateFingerprint(t);

        assertNotEquals(originalFingerprint, newFingerprint);
    }

    @Test
    public void calculateFingerprint_shouldReturnDifferentFingerprints_forDifferentThrowableTypes() throws Exception {
        //same message and stack trace to create throwable instances that differ only by type
        String message = "foo";
        Throwable a = new Exception(message);
        a.setStackTrace(constructCustomStackTrace());
        Throwable b = new RuntimeException(message);
        b.setStackTrace(constructCustomStackTrace());

        String fingerprintA = calculator.calculateFingerprint(a);
        String fingerprintB = calculator.calculateFingerprint(b);

        assertNotEquals(fingerprintA, fingerprintB);
    }

    @Test
    public void calculateFingerprint_shouldReturnDifferentFingerprints_givenDifferentMessages() throws Exception {
        //same class and stack trace to create throwable instances that differ only by message
        Throwable a = new RuntimeException("a");
        a.setStackTrace(constructCustomStackTrace());
        Throwable b = new RuntimeException("b");
        b.setStackTrace(constructCustomStackTrace());

        String fingerprintA = calculator.calculateFingerprint(a);
        String fingerprintB = calculator.calculateFingerprint(b);

        assertNotEquals(fingerprintA, fingerprintB);
    }

    @Test
    public void calculateFingerprint_shouldReturnADifferentFingerprint_givenADifferentCause() throws Exception {
        Throwable t = new RuntimeException();
        String originalFingerprint = calculator.calculateFingerprint(t);
        t.initCause(new RuntimeException());

        String modifiedFingerprint = calculator.calculateFingerprint(t);

        assertNotEquals(originalFingerprint, modifiedFingerprint);
    }
    
    @Test
    public void calculateFingerprint_shouldReturnADifferentFingerprint_givenAChangeInSuppressedExceptions() throws Exception {
        Throwable t = new RuntimeException();
        String originalFingerprint = calculator.calculateFingerprint(t);
        t.addSuppressed(new RuntimeException());

        String modifiedFingerprint = calculator.calculateFingerprint(t);

        assertNotEquals(originalFingerprint, modifiedFingerprint);
    }

    private StackTraceElement[] constructCustomStackTrace() {
        return new StackTraceElement[]{
                new StackTraceElement("org,example.Foo", "baz", "Foo.java", 128),
                new StackTraceElement("org,example.Foo", "bar", "Foo.java", 67),
                new StackTraceElement("org,example.Foo", "foo", "Foo.java", 42),
                new StackTraceElement("org,example.Foo", "main", "Foo.java", 21)
        };
    }
}