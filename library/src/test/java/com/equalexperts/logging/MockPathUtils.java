package com.equalexperts.logging;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockPathUtils {
    public static Path createMockPath() {
        Path result = mock(Path.class);
        FileSystem mockFileSystem = mock(FileSystem.class);
        when(result.getFileSystem()).thenReturn(mockFileSystem);
        when(mockFileSystem.provider()).thenReturn(mock(FileSystemProvider.class));
        return result;
    }
}
