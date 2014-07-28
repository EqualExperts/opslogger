package com.equalexperts.logging;

import java.io.PrintStream;

class OutputStreamDestination<T extends Enum<T> & LogMessage> implements BasicOpsLogger.Destination<T> {
    private final PrintStream output;
    private final StackTraceProcessor stackTraceProcessor;

    OutputStreamDestination(PrintStream output, StackTraceProcessor stackTraceProcessor) {
        this.output = output;
        this.stackTraceProcessor = stackTraceProcessor;
    }

    @Override
    public void publish(LogicalLogRecord<T> record) throws Exception {
        output.println(record.format(stackTraceProcessor));
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
