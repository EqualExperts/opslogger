package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;

import java.io.Closeable;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

public class FileChannelProviderTest extends AbstractFileChannelProviderTest {
    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();
    private final Path mockPath = createMockPath();
    private final FileChannelProvider provider = new FileChannelProvider(mockPath);

    @Test
    public void getChannel_shouldReturnAResultWithAChannelAndAssociatedWriter() throws Exception {
        FileChannel testFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        when(mockPath.getFileSystem().provider().newFileChannel(same(mockPath), eq(CREATE_AND_APPEND))).thenReturn(testFileChannel);

        FileChannelProvider.Result result = provider.getChannel();

        assertSame(testFileChannel, result.channel);
        ensureAssociated(result.writer, result.channel);
        ensureUtf8Charset(result.writer);
        assertTrue(testFileChannel.isOpen());
    }

    @Test
    public void close_shouldCloseTheFileChannelAndAssociatedWriter_givenAResultReturnedByGetChannel() throws Exception {
        FileChannel testFileChannel = FileChannel.open(tempFiles.createTempFile(null), CREATE);
        when(mockPath.getFileSystem().provider().newFileChannel(same(mockPath), eq(CREATE_AND_APPEND))).thenReturn(testFileChannel);

        FileChannelProvider.Result result = provider.getChannel();
        assertThat(result, instanceOf(Closeable.class));

        assertTrue("precondition: FileChannel should be open", result.channel.isOpen());
        assertTrue("precondition: Writer should be open", isOpen(result.writer));

        result.close();

        assertFalse(result.channel.isOpen());
        assertFalse(isOpen(result.writer));
    }
}
