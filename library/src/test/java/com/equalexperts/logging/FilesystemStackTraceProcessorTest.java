package com.equalexperts.logging;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import static java.nio.file.StandardOpenOption.*;

public class FilesystemStackTraceProcessorTest {

    private final Path mockDestinationDirectory = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
    private final ThrowableFingerprintCalculator fingerprintCalculator = mock(ThrowableFingerprintCalculator.class);
    private final StackTraceProcessor processor = new FilesystemStackTraceProcessor(mockDestinationDirectory, fingerprintCalculator);

    @Test
    public void process_shouldStoreTheStackTraceInAFileBasedOnTheFingerPrint() throws Exception {
        //setup is hairy — mocking a lot of file IO
        Throwable expectedException = new RuntimeException("blah!");
        String expectedFingerprint = "12345";
        String expectedFilename = "stacktrace_RuntimeException_" + expectedFingerprint + ".txt";
        String expectedStacktraceUri = "file:///tmp/log/" + expectedFilename;
        String expectedMessage = expectedException.getMessage() + " (" + expectedStacktraceUri + ")";

        when(fingerprintCalculator.calculateFingerprint(expectedException)).thenReturn(expectedFingerprint);

        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(mockDestinationDirectory.resolve(expectedFilename)).thenReturn(expectedPath);
        when(expectedPath.toUri()).thenReturn(new URI(expectedStacktraceUri));
        pretendMockPathDoesNotExist(expectedPath);

        ByteArrayOutputStream simulatedFileOutputStream = new ByteArrayOutputStream();
        when(Files.newOutputStream(expectedPath, CREATE_NEW, WRITE)).thenReturn(simulatedFileOutputStream);

        TestPrintStream expectedFileContents = new TestPrintStream();
        expectedException.printStackTrace(expectedFileContents);

        StringBuilder output = new StringBuilder();

        //execute
        processor.process(expectedException, output);

        //assert
        verify(expectedPath.getFileSystem().provider()).newOutputStream(expectedPath, CREATE_NEW, WRITE);
        assertEquals(expectedFileContents.toString(), new String(simulatedFileOutputStream.toByteArray()));
        assertEquals(expectedMessage, output.toString());
    }

    @Test
    public void process_shouldReuseTheURIOfTheExistingFile_whenItAlreadyExists() throws Exception {
        //setup is hairy — mocking a lot of file IO
        Throwable expectedException = new RuntimeException("blah!");
        String expectedFingerprint = "12345";
        String expectedFilename = "stacktrace_RuntimeException_" + expectedFingerprint + ".txt";
        String expectedStacktraceUri = "file:///tmp/log/" + expectedFilename;
        String expectedMessage = expectedException.getMessage() + " (" + expectedStacktraceUri + ")";

        when(fingerprintCalculator.calculateFingerprint(expectedException)).thenReturn(expectedFingerprint);

        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(mockDestinationDirectory.resolve(expectedFilename)).thenReturn(expectedPath);
        when(expectedPath.toUri()).thenReturn(new URI(expectedStacktraceUri));

        StringBuilder output = new StringBuilder();

        //execute
        processor.process(expectedException, output);

        //assert
        assertEquals(expectedMessage, output.toString());
        verify(expectedPath.getFileSystem().provider()).checkAccess(expectedPath);
        verifyNoMoreInteractions(expectedPath.getFileSystem().provider());
    }

    @Test
    public void process_shouldReuseTheURIOfTheExistingFile_whenItAlreadyExistsInARaceCondition() throws Exception {
        //setup is hairy — mocking a lot of file IO
        Throwable expectedException = new RuntimeException("blah!");
        String expectedFingerprint = "12345";
        String expectedFilename = "stacktrace_RuntimeException_" + expectedFingerprint + ".txt";
        String expectedStacktraceUri = "file:///tmp/log/" + expectedFilename;
        String expectedMessage = expectedException.getMessage() + " (" + expectedStacktraceUri + ")";

        when(fingerprintCalculator.calculateFingerprint(expectedException)).thenReturn(expectedFingerprint);

        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(mockDestinationDirectory.resolve(expectedFilename)).thenReturn(expectedPath);
        when(expectedPath.toUri()).thenReturn(new URI(expectedStacktraceUri));
        pretendMockPathDoesNotExist(expectedPath);

        //filesystem reports that the file doesn't exist, but fail when creating (simulate a race condition)
        when(Files.newOutputStream(expectedPath, CREATE_NEW, WRITE)).thenThrow(new FileAlreadyExistsException("blah"));

        TestPrintStream expectedFileContents = new TestPrintStream();
        expectedException.printStackTrace(expectedFileContents);

        StringBuilder output = new StringBuilder();

        //execute
        processor.process(expectedException, output);

        //assert
        verify(expectedPath.getFileSystem().provider()).checkAccess(expectedPath);
        verify(expectedPath.getFileSystem().provider()).newOutputStream(expectedPath, CREATE_NEW, WRITE);
        verifyNoMoreInteractions(expectedPath.getFileSystem().provider());
        assertEquals(expectedMessage, output.toString());
    }

    private void pretendMockPathDoesNotExist(Path expectedPath) throws Exception {
        FileSystemProvider mockProvider = expectedPath.getFileSystem().provider();
        doThrow(NoSuchFileException.class).when(mockProvider).checkAccess(expectedPath); //notExists check

        //for exists, simulate a path not existing by failing to read attributes
        when(Files.readAttributes(expectedPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenThrow(new IOException("File does not exist"));
    }
}
