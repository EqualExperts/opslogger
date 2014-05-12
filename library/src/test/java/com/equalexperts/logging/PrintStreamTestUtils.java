package com.equalexperts.logging;

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
        Class<FilterOutputStream> cls = FilterOutputStream.class;
        Field field = Stream.of(cls.getDeclaredFields())
                .filter(f -> f.getName().equals("out"))
                .findFirst().get();
        field.setAccessible(true);

        return (OutputStream) field.get(stream);
    }

    /**
     * Uses reflection to determine the autoFlush setting of a PrintStream
     */
    public static boolean getAutoFlush(PrintStream stream) throws Exception {
        Class<PrintStream> cls = PrintStream.class;
        Field field = Stream.of(cls.getDeclaredFields())
                .filter(f -> f.getName().equals("autoFlush"))
                .findFirst().get();
        field.setAccessible(true);

        return (boolean) field.get(stream);
    }
}
