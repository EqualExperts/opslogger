package uk.gov.gds.performance.collector.logging;

import org.joda.time.Instant;

import java.io.PrintStream;
import java.util.Formatter;

public class OpsLogger <T extends Enum<T> & LogMessage> {
    private final PrintStream output;

    public OpsLogger(PrintStream output) {
        this.output = output;
    }

    public void log(T message, Object... details) {
        StringBuilder result = buildBasicLogMessage(message, details);
        output.println(result);
    }

    public void log(T message, Throwable cause, Object... details) {
        StringBuilder result = buildBasicLogMessage(message, details);
        result.append(" "); //the gap between the basic message and the stack trace
        output.print(result);
        cause.printStackTrace(output);
    }

    private StringBuilder buildBasicLogMessage(T message, Object[] details) {
        Instant timestamp = new Instant();
        StringBuilder result = new StringBuilder(timestamp.toString());
        result.append(" ");
        result.append(message.getMessageCode());
        result.append(": ");
        new Formatter(result).format(message.getMessagePattern(), details);
        return result;
    }
}