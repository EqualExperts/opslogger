package com.equalexperts.logging;

import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractFileChannelProviderTest {
    protected static final Set<StandardOpenOption> CREATE_AND_APPEND = EnumSet.of(CREATE, APPEND);

    protected static Path createMockPath() {
        Path result = mock(Path.class);
        FileSystem mockFileSystem = mock(FileSystem.class);
        when(result.getFileSystem()).thenReturn(mockFileSystem);
        when(mockFileSystem.provider()).thenReturn(mock(FileSystemProvider.class));
        return result;
    }

    protected void ensureAssociated(Writer writer, FileChannel channel) throws Exception {
        Class<? extends Writer> implementationClass = writer.getClass();
        for (Field field : implementationClass.getDeclaredFields()) {
            if (!field.getType().isAssignableFrom(channel.getClass())) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(writer);
            if (channel == value) {
                return;
            }
        }
        throw new AssertionError("The provided writer is not associated with the FileChannel");
    }

    protected void ensureUtf8Charset(Writer writer) throws Exception {
        Class<? extends Writer> implementationClass = writer.getClass();
        for (Field field : implementationClass.getDeclaredFields()) {
            if (!field.getType().isAssignableFrom(Charset.class)) {
                continue;
            }
            field.setAccessible(true);
            assertSame(StandardCharsets.UTF_8, field.get(writer));
            return;
        }
    }

    protected boolean isOpen(Writer writer) throws Exception {
        Class<? extends Writer> implementationClass = writer.getClass();
        for (Field field : implementationClass.getDeclaredFields()) {
            if (!field.getName().equals("isOpen")) {
                continue;
            }
            field.setAccessible(true);
            return (boolean) field.get(writer);
        }
        throw new RuntimeException("field not found");
    }
}
