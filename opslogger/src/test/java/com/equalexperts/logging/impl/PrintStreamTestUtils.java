package com.equalexperts.logging.impl;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.stream.Stream;

public class PrintStreamTestUtils {
    /**
     * Uses reflection to return the OutputStream wrapped by a PrintStream
     */
    public static OutputStream getBackingOutputStream(PrintStream stream) throws Exception {
        Field field = getInternalField(FilterOutputStream.class, "out");
        return (OutputStream) field.get(stream);
    }

    /**
     * Uses reflection to determine the autoFlush setting of a PrintStream
     */
    public static boolean getAutoFlush(PrintStream stream) throws Exception {
        Field field = getInternalField(PrintStream.class, "autoFlush");
        return (boolean) field.get(stream);
    }

    /**
     * Obtains a reference to a non-public field and makes it accessible
     */
    private static Field getInternalField(Class<?> cls, String fieldName) {
        Field field = Stream.of(cls.getDeclaredFields())
                .filter(f -> f.getName().equals(fieldName))
                .findFirst().get();
        field.setAccessible(true);
        return field;
    }
}
