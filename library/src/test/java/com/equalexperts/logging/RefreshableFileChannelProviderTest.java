package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;

import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefreshableFileChannelProviderTest {
    static final Set<StandardOpenOption> CREATE_AND_APPEND = EnumSet.of(CREATE, APPEND);

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Test
    public void getChannel_shouldReturnAResultWithAChannelAndAssociatedWriter_whenFirstCalled() throws Exception {
        Path expectedPath = createMockPath();
        Duration maximumReuse = Duration.parse("PT5M");

        RefreshableFileChannelProvider provider = new RefreshableFileChannelProvider(expectedPath, maximumReuse);

        FileChannel testFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        when(expectedPath.getFileSystem().provider().newFileChannel(same(expectedPath), eq(CREATE_AND_APPEND))).thenReturn(testFileChannel);

        RefreshableFileChannelProvider.Result result = provider.getChannel(Instant.now());

        assertSame(testFileChannel, result.channel);
        ensureAssociated(result.writer, result.channel);
        ensureUtf8Charset(result.writer);
        assertTrue(testFileChannel.isOpen());
    }

    @Test
    public void getChannel_shouldReuseThePreviousResult_whenThePreviousResultIsNewEnough() throws Exception {
        Path expectedPath = createMockPath();
        Duration maximumReuse = Duration.parse("PT5M");

        RefreshableFileChannelProvider provider = new RefreshableFileChannelProvider(expectedPath, maximumReuse);

        //set up first call
        FileChannel firstFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        when(expectedPath.getFileSystem().provider().newFileChannel(same(expectedPath), eq(CREATE_AND_APPEND))).thenReturn(firstFileChannel);

        Instant firstCall = Instant.now().minusSeconds(3600);
        RefreshableFileChannelProvider.Result firstResult = provider.getChannel(firstCall);

        Instant timeOfSecondCall = firstCall.plus(maximumReuse); //just under the limit
        RefreshableFileChannelProvider.Result secondResult = provider.getChannel(timeOfSecondCall);

        assertSame(firstResult, secondResult);
        assertTrue(firstFileChannel.isOpen());
    }

    @Test
    public void getChannel_shouldCloseThePreviousResultAndConstructANewOne_whenThePreviousResultIsTooOld() throws Exception {
        Path expectedPath = createMockPath();
        Duration maximumReuse = Duration.parse("PT5M");

        RefreshableFileChannelProvider provider = new RefreshableFileChannelProvider(expectedPath, maximumReuse);

        //set up two file channels
        FileChannel firstFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        FileChannel secondFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        when(expectedPath.getFileSystem().provider().newFileChannel(same(expectedPath), eq(CREATE_AND_APPEND))).thenReturn(firstFileChannel, secondFileChannel);

        Instant firstCall = Instant.now().minusSeconds(3600);
        RefreshableFileChannelProvider.Result firstResult = provider.getChannel(firstCall);

        Instant timeOfSecondCall = firstCall.plus(maximumReuse).plus(1, ChronoUnit.MILLIS); //just over the limit
        RefreshableFileChannelProvider.Result secondResult = provider.getChannel(timeOfSecondCall);

        assertNotSame(firstResult, secondResult);
        assertFalse(firstFileChannel.isOpen());
        assertSame(secondFileChannel, secondResult.channel);
        ensureAssociated(secondResult.writer, secondResult.channel);
        ensureUtf8Charset(secondResult.writer);
        assertTrue(secondFileChannel.isOpen());
    }

    private void ensureAssociated(Writer writer, FileChannel channel) throws Exception {
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

    private void ensureUtf8Charset(Writer writer) throws Exception {
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

    private Path createMockPath() {
        Path result = mock(Path.class);
        FileSystem mockFileSystem = mock(FileSystem.class);
        when(result.getFileSystem()).thenReturn(mockFileSystem);
        when(mockFileSystem.provider()).thenReturn(mock(FileSystemProvider.class));
        return result;
    }
}
