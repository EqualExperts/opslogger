package com.equalexperts.logging;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static com.equalexperts.logging.EnumContractRunner.EnumField;

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
        List<T> logMessagesWithThisCode = new ArrayList<>();
        for (T value : enumValue.getDeclaringClass().getEnumConstants()) {
            if (value != enumValue && enumValue.getMessageCode().equalsIgnoreCase(value.getMessageCode())) {
                logMessagesWithThisCode.add(value);
            }
        }

        if (logMessagesWithThisCode.size() > 0) {
            fail(enumValue.name() + " has the same code as " + join(logMessagesWithThisCode));
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
    public void getMessagePattern_shouldNotBeEmptyString() throws Exception {
        if("".equals(enumValue.getMessagePattern())) {
            fail("A proper message pattern is required");
        }
    }

    private String join(List<T> values) {
        StringBuilder result = new StringBuilder();
        if (values.size() == 1) {
            append(result, values.get(0));
            return result.toString();
        }

        for(T value : values.subList(0, values.size() - 2)) {
            append(result, value);
            result.append(", ");
        }
        append(result, values.get(values.size() - 2));
        result.append(" and ");
        append(result, values.get(values.size() - 1));
        return result.toString();
    }

    private void append(StringBuilder builder, T value) {
        builder.append(value.getClass().getSimpleName());
        builder.append(".");
        builder.append(value.name());
    }
}