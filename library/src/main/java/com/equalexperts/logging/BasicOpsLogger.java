package com.equalexperts.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Formatter;
import java.util.function.Consumer;

class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final PrintStream output;
    private final Clock clock;
    private final SimpleStackTraceProcessor stackTraceProcessor;
    private final Consumer<Throwable> errorHandler;

    BasicOpsLogger(PrintStream output, Clock clock, SimpleStackTraceProcessor stackTraceProcessor, Consumer<Throwable> errorHandler) {
        this.output = output;
        this.clock = clock;
        this.stackTraceProcessor = stackTraceProcessor;
        this.errorHandler = errorHandler;
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
        try {
            StringBuilder result = buildBasicLogMessage(message, details);
            result.append(" "); //the gap between the basic message and the stack trace
            stackTraceProcessor.process(cause, result);
            output.println(result);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
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

    Consumer<Throwable> getErrorHandler() { return errorHandler; }
}