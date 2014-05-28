package com.equalexperts.logging;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SimpleStackTraceProcessorTest {

    private final StackTraceProcessor processor = new SimpleStackTraceProcessor();

    @Test
    public void process_shouldPrintTheStackTraceAsAMultilineString_givenAThrowableAndAnAppendable() throws Exception {
        String expectedMessage = "blah blah blah";
        Throwable expectedException = new RuntimeException(expectedMessage);
        String expectedProcessedMessage = getExceptionPrintout(expectedException);
        StringBuilder actualOutput = new StringBuilder();

        processor.process(expectedException, actualOutput);

        assertEquals(expectedProcessedMessage, actualOutput.toString());
        assertThat(actualOutput.toString(), CoreMatchers.containsString("\n"));
        assertThat(actualOutput.toString(), CoreMatchers.containsString(expectedMessage));
    }

    private String getExceptionPrintout(Throwable expectedException) {
        TestPrintStream testPrintStream = new TestPrintStream();
        expectedException.printStackTrace(testPrintStream);
        return testPrintStream.toString();
    }
}
