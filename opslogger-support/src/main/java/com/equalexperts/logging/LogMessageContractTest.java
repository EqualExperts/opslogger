package com.equalexperts.logging;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.equalexperts.logging.EnumContractRunner.EnumField;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

@RunWith(EnumContractRunner.class)
public abstract class LogMessageContractTest<T extends Enum<T> & LogMessage> {

    @SuppressWarnings("UnusedDeclaration")
    @EnumField
    private T enumValue;

    @Test
    public void getMessageCode_shouldReturnAUniqueValue() throws Exception {
        if (enumValue.getMessageCode() == null) {
            return; //don't test duplication for null values — too complicated anyway
        }
        if (enumValue.getMessageCode().equals("")) {
            return; //don't test duplication for empty string values — too complicated anyway
        }

        List<String> otherLogMessagesWithThisCode = stream(enumValue.getDeclaringClass().getEnumConstants())
                .filter(t -> t != enumValue)
                .filter(t -> enumValue.getMessageCode().equalsIgnoreCase(t.getMessageCode()))
                .map(this::formatForErrorMessage)
                .collect(Collectors.toList());

        if (!otherLogMessagesWithThisCode.isEmpty()) {
            fail(enumValue.name() + " has the same code as " + String.join(",", otherLogMessagesWithThisCode));
        }
    }

    @Test
    public void getMessageCode_shouldNotBeNull() throws Exception {
        assertNotNull(enumValue.getMessageCode());
    }

    @Test
    public void getMessageCode_shouldNotBeEmptyString() throws Exception {
        if("".equals(enumValue.getMessageCode())) {
            fail("A proper message code is required");
        }
    }

    @Test
    public void getMessagePattern_shouldNotBeNull() throws Exception {
        assertNotNull(enumValue.getMessagePattern());
    }

    @Test
    public void getMessagePattern_shouldNotBeAnEmptyString() throws Exception {
        if("".equals(enumValue.getMessagePattern())) {
            fail("A proper message pattern is required");
        }
    }

    @Test
    public void enumInstance_shouldBeImmutable() throws Exception {
        assertInstancesOf(enumValue.getClass(), areImmutable());
    }
    
    @Test
    public void messageCodes_shouldAllBeTheSameLength() throws Exception {
        int mostCommonLength = getMostCommonLength(enumValue.getDeclaringClass().getEnumConstants());
        assertEquals(enumValue.name() + " has a different length than " + mostCommonLength + ", the most common length", mostCommonLength, enumValue.getMessageCode().length());
    }

    private int getMostCommonLength(T[] constants) {

        //calculate a histogram of messageCodeLengths
        Map<Integer, Long> codeLengthHistogram = stream(constants)
                .map(c -> c.getMessageCode().length())
                .collect(Collectors.groupingBy(identity(), Collectors.counting()));

        //most common count
        return codeLengthHistogram.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().intValue()))
                .map(Map.Entry::getKey)
                .get();
    }

    private String formatForErrorMessage(T value) {
        return value.getClass().getSimpleName() + "." + value.name();
    }
}