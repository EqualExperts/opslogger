package com.equalexperts.logging;

import java.io.PrintStream;

/**
 * A Destination which formats LogicalLogRecords with the provided stackTraceProcessor and prints it to <code>output</code>.  Also knows that if output is System.out or System.err, it should not be closed when done.
 */

class OutputStreamDestination<T extends Enum<T> & LogMessage> implements AsyncOpsLogger.Destination<T> {
    private final PrintStream output;
    private final StackTraceProcessor stackTraceProcessor;

    OutputStreamDestination(PrintStream output, StackTraceProcessor stackTraceProcessor) {
        this.output = output;
        this.stackTraceProcessor = stackTraceProcessor;
    }

    @Override
    public void beginBatch() throws Exception {

    }

    @Override
    public void publish(LogicalLogRecord<T> record) throws Exception {
        output.println(record.format(stackTraceProcessor));
    }

    @Override
    public void endBatch() throws Exception {

    }

    @Override
    public void close() throws Exception {
        if (!streamIsSpecial()) {
            output.close();
        }
    }

    private boolean streamIsSpecial() {
        return (output == System.out) || (output == System.err);
    }

    PrintStream getOutput() {
        return output;
    }

    StackTraceProcessor getStackTraceProcessor() {
        return stackTraceProcessor;
    }
}
