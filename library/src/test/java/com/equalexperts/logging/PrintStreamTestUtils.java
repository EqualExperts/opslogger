package com.equalexperts.logging;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

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
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals(fieldName)) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalArgumentException(fieldName + " not found.");
    }
}
