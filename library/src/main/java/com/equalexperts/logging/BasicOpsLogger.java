package com.equalexperts.logging;


import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Formatter;

public class BasicOpsLogger<T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final PrintStream output;
    final Clock clock;

    public BasicOpsLogger(PrintStream output) {
        this(output, Clock.systemUTC());
    }

    //exposed so a clock can be injected for testing
    BasicOpsLogger(PrintStream output, Clock clock) {
        this.output = output;
        this.clock = clock;
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
        output.print(result);
        cause.printStackTrace(output);
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
}