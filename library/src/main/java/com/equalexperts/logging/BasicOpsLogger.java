package com.equalexperts.logging;

import com.equalexperts.util.Clock;
import org.joda.time.Instant;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Formatter;

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final PrintStream output;
    private final Clock clock;
    private final SimpleStackTraceProcessor stackTraceProcessor;

    BasicOpsLogger(PrintStream output, Clock clock, SimpleStackTraceProcessor stackTraceProcessor) {
        this.output = output;
        this.clock = clock;
        this.stackTraceProcessor = stackTraceProcessor;
    }

    @Override
    public void close() throws IOException {
        if (!streamIsSpecial()) {
            output.close();
        }
    }

    @Override
    public void log(T message, Object... details) {
        StringBuilder result = buildBasicLogMessage(message, details);
        output.println(result);
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        StringBuilder result = buildBasicLogMessage(message, details);
        result.append(" "); //the gap between the basic message and the stack trace
        stackTraceProcessor.process(cause, result);
        output.println(result);
    }

    private StringBuilder buildBasicLogMessage(T message, Object[] details) {
        Instant timestamp = clock.instant();
        StringBuilder result = new StringBuilder(timestamp.toString());
        result.append(" ");
        result.append(message.getMessageCode());
        result.append(": ");
        new Formatter(result).format(message.getMessagePattern(), details);
        return result;
    }

    private boolean streamIsSpecial() {
        return (output == System.out) || (output == System.err);
    }

    PrintStream getOutput() {
        return output;
    }

    Clock getClock() {
        return clock;
    }

    StackTraceProcessor getStackTraceProcessor() {
        return stackTraceProcessor;
    }
}
