package com.equalexperts.logging;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.equalexperts.logging.EnumContractRunner.EnumField;
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

        List<String> otherLogMessagesWithThisCode = new ArrayList<>();
        T[] enumConstants = enumValue.getDeclaringClass().getEnumConstants();
        for (T t : enumConstants) {
            if (t != enumValue && enumValue.getMessageCode().equalsIgnoreCase(t.getMessageCode())) {
                otherLogMessagesWithThisCode.add(formatForErrorMessage(t));
            }
        }

        if (!otherLogMessagesWithThisCode.isEmpty()) {
            fail(enumValue.name() + " has the same code as " + join(",", otherLogMessagesWithThisCode));
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

    private String formatForErrorMessage(T value) {
        return value.getClass().getSimpleName() + "." + value.name();
    }

    private String join(String separator, Collection<String> stuff) {
        String sep = "";
        final StringBuilder b = new StringBuilder();
        for (Object x : stuff) {
            b.append(sep).append(x);
            sep = separator;
        }
        return b.toString();
    }
}
