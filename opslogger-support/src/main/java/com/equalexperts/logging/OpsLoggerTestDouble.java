package com.equalexperts.logging;

import org.hamcrest.CoreMatchers;
import org.mutabilitydetector.AnalysisResult;
import org.mutabilitydetector.Configurations;
import org.mutabilitydetector.IsImmutable;
import org.mutabilitydetector.locations.Dotted;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areEffectivelyImmutable;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

/**
 * An OpsLogger implementation that validates arguments, but doesn't actually
 * log anything. Very useful for unit tests.
 */
public class OpsLoggerTestDouble <T extends Enum<T> & LogMessage> implements OpsLogger<T> {
    private final Function<OpsLogger<T>, OpsLogger<T>> nestedLoggerDecorator;
    private final Map<Map<String, String>, OpsLogger<T>> nestedLoggers = new ConcurrentHashMap<>();

    public OpsLoggerTestDouble() {
        this(Function.identity());
    }

    OpsLoggerTestDouble(Function<OpsLogger<T>, OpsLogger<T>> nestedLoggerDecorator) {
        this.nestedLoggerDecorator = nestedLoggerDecorator;
    }

    public static <T extends Enum<T> & LogMessage> OpsLogger<T> withSpyFunction(Function<OpsLogger<T>, OpsLogger<T>> spyFunction) {
        return spyFunction.apply(new OpsLoggerTestDouble<>(spyFunction));
    }

    @Override
    public void log(T message, Object... details) {
        validate(message);
        ensureImmutableDetails(details);
        checkForTooManyFormatStringArguments(message.getMessagePattern(), details);
        validateFormatString(message.getMessagePattern(), details);
    }

    @Override
    public void log(T message, Throwable cause, Object... details) {
        validate(message);
        ensureImmutableDetails(details);
        assertNotNull("Throwable instance must be provided", cause);
        checkForTooManyFormatStringArguments(message.getMessagePattern(), details);
        validateFormatString(message.getMessagePattern(), details);
    }

    @Override
    public void close() throws IOException {
        throw new IllegalStateException("OpsLogger instances should not be closed by application code.");
    }

    @Override
    public OpsLogger<T> with(DiagnosticContextSupplier contextSupplier) {
        return nestedLoggers.computeIfAbsent(contextSupplier.getMessageContext(), k -> createNestedLogger());
    }

    Function<OpsLogger<T>, OpsLogger<T>> getNestedLoggerDecorator() {
        return nestedLoggerDecorator;
    }

    private OpsLogger<T> createNestedLogger() {
        return nestedLoggerDecorator.apply(new OpsLoggerTestDouble<>(nestedLoggerDecorator));
    }

    private void validateFormatString(String pattern, Object... details) {
        //noinspection ResultOfMethodCallIgnored
        String.format(pattern, details);
    }

    private void checkForTooManyFormatStringArguments(String pattern, Object... details) {
        if (details.length > 1) {
            /*
                Check for too many arguments by removing one, and expecting "not enough arguments" to happen.
             */
            try {
                //noinspection ResultOfMethodCallIgnored
                String.format(pattern, Arrays.copyOfRange(details, 0, details.length - 1));
                throw new IllegalArgumentException("Too many format string arguments provided");
            } catch (MissingFormatArgumentException expected) {
                //expected
            }
        }
    }

    private void validate(T message) {
        assertNotNull("LogMessage must be provided", message);
        assertNotNull("MessageCode must be provided", message.getMessageCode());
        assertThat("MessageCode must be provided", message.getMessageCode(), CoreMatchers.not(""));
        assertNotNull("MessagePattern must be provided", message.getMessagePattern());
        assertThat("MessagePattern must be provided", message.getMessagePattern(), CoreMatchers.not(""));
    }

    private void ensureImmutableDetails(Object... details) {
        for (Object o : details) {
            Class<?> aClass = o.getClass();
            if (!IMMUTABLE_CLASSES_FROM_THE_JDK.contains(aClass)) {
                assertInstancesOf(aClass, anyOf(areEffectivelyImmutable(), areImmutable()));
            }
        }
    }

    /**
     * The immutability detector maintains this list, but itself only uses it on fields.
     * We need to pass these classes themselves.
     */
    private static final Set<Class> IMMUTABLE_CLASSES_FROM_THE_JDK;

    static {
        try {
            Set<Class> temp = new HashSet<>();
            for (Map.Entry<Dotted, AnalysisResult> entry : Configurations.JDK_CONFIGURATION.hardcodedResults().entrySet()) {
                if (entry.getValue().isImmutable == IsImmutable.IMMUTABLE) {
                    temp.add(Class.forName(entry.getKey().toString()));
                }
            }
            IMMUTABLE_CLASSES_FROM_THE_JDK = Collections.unmodifiableSet(temp);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
