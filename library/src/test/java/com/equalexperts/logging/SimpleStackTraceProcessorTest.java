package com.equalexperts.logging;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SimpleStackTraceProcessorTest {

    private final StackTraceProcessor processor = new SimpleStackTraceProcessor();

    @Test
    public void process_shouldReturnTheStackTraceAsAMultilineString() throws Exception {
        String expectedMessage = "blah blah blah";
        Throwable expectedException = new RuntimeException(expectedMessage);
        String expectedProcessedMessage = getExceptionPrintout(expectedException);

        String processedException = processor.process(expectedException);

        assertEquals(expectedProcessedMessage, processedException);
        assertThat(processedException, CoreMatchers.containsString("\n"));
        assertThat(processedException, CoreMatchers.containsString(expectedMessage));
    }

    private String getExceptionPrintout(Throwable expectedException) {
        TestPrintStream testPrintStream = new TestPrintStream();
        expectedException.printStackTrace(testPrintStream);
        return testPrintStream.toString();
    }
}
