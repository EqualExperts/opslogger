package com.equalexperts.logging;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SimpleStackTraceProcessorTest {

    private final StackTraceProcessor processor = new SimpleStackTraceProcessor();

    @Test
    public void process_shouldPrintTheStackTraceAsAMultilineString_givenAThrowable() throws Exception {
        String expectedMessage = "blah blah blah";
        Throwable expectedException = new RuntimeException(expectedMessage);
        String expectedProcessedMessage = getExceptionPrintout(expectedException);
        StringBuilder actualOutput = new StringBuilder();

        processor.process(expectedException, actualOutput);

        assertEquals(expectedProcessedMessage, actualOutput.toString());
        assertThat(actualOutput.toString(), containsString("\n"));
        assertThat(actualOutput.toString(), containsString(expectedMessage));
    }

    @Test
    public void process_shouldStripAnEndingNewLineFromOutput_givenAThrowable() throws Exception {
        Throwable expectedException = new RuntimeException();
        StringBuilder output = new StringBuilder();

        processor.process(expectedException, output);

        assertThat(output.toString(), not(endsWith("\n")));
    }

    private String getExceptionPrintout(Throwable expectedException) {
        TestPrintStream testPrintStream = new TestPrintStream();
        expectedException.printStackTrace(testPrintStream);
        String result = testPrintStream.toString();
        return result.substring(0, result.length() - 1); //strip last character
    }
}
