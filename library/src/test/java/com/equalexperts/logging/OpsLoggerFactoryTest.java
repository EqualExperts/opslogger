package com.equalexperts.logging;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;

import static com.equalexperts.logging.PrintStreamTestUtils.*;
import static java.nio.file.StandardOpenOption.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

public class OpsLoggerFactoryTest {

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Rule
    public RestoreSystemStreamsFixture systemStreamsFixture =  new RestoreSystemStreamsFixture();

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToSystemOut_whenNoConfigurationIsPerformed() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertSame(System.out, basicLogger.getOutput());
        ensureCorrectlyConfigured(basicLogger);
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToTheCorrectPrintStream_whenAPrintStreamIsSet() throws Exception {
        PrintStream expectedPrintStream = new PrintStream(new ByteArrayOutputStream());

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setDestination(expectedPrintStream)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertSame(expectedPrintStream, basicLogger.getOutput());
        ensureCorrectlyConfigured(basicLogger);
    }

    @Test
    public void build_shouldReturnABasicOpsLoggerConfiguredToAutoFlushAndAppendToTheRightFile_whenAPathIsSet() throws Exception {
        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        OutputStream expectedOutputStream = mock(OutputStream.class);
        when(Files.newOutputStream(expectedPath, CREATE, APPEND)).thenReturn(expectedOutputStream);
        //using a mock so that we can be sure the stream is created with the correct I/O mode

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(expectedPath)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        ensureCorrectlyConfigured(basicLogger);

        PrintStream loggerOutputStream = basicLogger.getOutput();
        assertEquals(true, getAutoFlush(loggerOutputStream));

        OutputStream actualOutputStream = getBackingOutputStream(loggerOutputStream);
        assertSame(expectedOutputStream, actualOutputStream);
    }

    @Test
    public void build_shouldCreateAllNecessaryParentDirectories_whenAPathWithParentsThatDoNotExistIsSet() throws Exception {
        Path grandParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path parent = tempFiles.register(grandParent.resolve(UUID.randomUUID().toString()));
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        //preconditions
        assertFalse(Files.exists(grandParent));
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(logFile));

        //execute
        new OpsLoggerFactory()
                .setPath(logFile)
                .<TestMessages>build();

        //assert
        assertTrue(Files.exists(grandParent));
        assertTrue(Files.exists(parent));
        assertTrue(Files.exists(logFile));
    }

    @Test
    public void build_shouldSetASimpleStackTraceProcessor_whenAPrintStreamIsSetAndAStackTraceProcessorIsNotConfigured() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setDestination(new PrintStream(new ByteArrayOutputStream()))
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldSetASimpleStackTraceProcessor_whenNoConfigurationIsPerformed() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldSetAFileSystemStackTraceProcessorToAParentPath_whenAPathIsSetAndAStackTraceProcessorIsNotConfigured() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(logFile)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        FilesystemStackTraceProcessor stackTraceProcessor = (FilesystemStackTraceProcessor) basicLogger.getStackTraceProcessor();

        assertEquals(parent, stackTraceProcessor.getDestination());
    }

    @Test
    public void build_shouldConfigureASimpleStackTraceProcessor_whenSetStoreStackTracesInFilesystemIsCalledWithFalse() throws Exception {
        Path logFile = tempFiles.createTempFileThatDoesNotExist(".log");

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(logFile)
                .setStoreStackTracesInFilesystem(false)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldConfigureAFileSystemStackTraceProcessor_whenSetStoreStackTraceStoragePathIsCalled() throws Exception {
        Path stackTraceStorage = tempFiles.createTempDirectoryThatDoesNotExist();

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setStackTraceStoragePath(stackTraceStorage)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) basicLogger.getStackTraceProcessor();
        assertEquals(stackTraceStorage, processor.getDestination());
    }

    @Test
    public void build_shouldEnsureThatParentDirectoriesExist_whenSetStoreStackTraceStoragePathIsCalled() throws Exception {
        Path stackTraceStorage = tempFiles.createTempDirectoryThatDoesNotExist();

        new OpsLoggerFactory()
                .setStackTraceStoragePath(stackTraceStorage)
                .<TestMessages>build();

        assertTrue(Files.exists(stackTraceStorage));
        assertTrue(Files.isDirectory(stackTraceStorage));
    }

    @Test
    public void build_shouldThrowAnException_whenSetStoreStackTracesInFileSystemIsSetToTrueNoPathsHaveBeenProvided() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory()
                .setDestination(System.out)
                .setStoreStackTracesInFilesystem(true);

        try {
            factory.<TestMessages>build();
            fail("expected an exception");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Cannot store stack traces in the filesystem without providing a path"));
        }
    }

    @Test
    public void build_shouldConstructABasicOpsLoggerWithTheCorrectErrorHandler_whenACustomErrorHandlerHasBeenSet() throws Exception {
        Consumer<Throwable> expectedErrorHandler = (t) -> {};
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setErrorHandler(expectedErrorHandler)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        assertSame(expectedErrorHandler, basicLogger.getErrorHandler());
    }

    @Test
    public void setStoreStackTracesInFilesystem_shouldClearTheStackTraceStoragePath_givenFalse() throws Exception {
        Path originalStackTraceDestination = tempFiles.createTempDirectoryThatDoesNotExist();

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(tempFiles.createTempFileThatDoesNotExist(".log"))
                .setStackTraceStoragePath(originalStackTraceDestination)
                .setStoreStackTracesInFilesystem(false)
                .setStoreStackTracesInFilesystem(true)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) basicLogger.getStackTraceProcessor();
        assertNotEquals(originalStackTraceDestination, processor.getDestination());
    }

    @Test
    public void setStoreStackTracesInFileSystem_shouldWorkIfItIsCalledBeforeAPathIsSet() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setStoreStackTracesInFilesystem(true)
                .setDestination(System.out)
                .setStackTraceStoragePath(parent)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) basicLogger.getStackTraceProcessor();
        assertEquals(parent, processor.getDestination());
    }

    @Test
    public void setStackTraceStoragePath_shouldThrowAnException_givenNull() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setStackTraceStoragePath(null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("must not be null"));
        }
    }

    @Test
    public void setStackTraceStoragePath_shouldThrowAnException_givenAPathThatExistsAndIsNotADirectory() throws Exception {
        Path file = tempFiles.createTempFile(".txt");
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setStackTraceStoragePath(file);
            fail("expected an exception");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("must be a directory"));
        }
    }

    @Test
    public void setStackTraceStoragePath_shouldNotThrowAnException_givenAPathThatExistsAndIsADirectory() throws Exception {
        Path directory = tempFiles.createTempDirectory();

        new OpsLoggerFactory()
                .setStackTraceStoragePath(directory);
    }

    @Test
    public void setStackTraceStoragePath_shouldNotCreateAnyDirectories_whenBuildIsNotCalled() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path child = tempFiles.register(parent.resolve("child"));

        //preconditions
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(child));

        new OpsLoggerFactory()
                .setStackTraceStoragePath(child);

        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(child));
    }

    @Test
    public void setPath_shouldThrowAnException_givenANullPath() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setPath(null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("must not be null"));
        }
    }

    @Test
    public void setPath_shouldThrowAnException_givenAPathThatIsADirectory() throws Exception {
        Path directory = Paths.get(System.getProperty("java.io.tmpdir"));
        assertTrue("precondition: must be a directory", Files.isDirectory(directory));
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setPath(directory);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("must not be a directory"));
        }
    }

    @Test
    public void setPath_shouldNotCreateAFileOrParentDirectory_whenBuildIsNotCalled() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        //preconditions
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(logFile));

        //execute
        new OpsLoggerFactory()
                .setPath(logFile);

        //assert
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(logFile));
    }

    @Test
    public void setDestination_shouldThrowAnException_givenANullPrintStream() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setDestination(null);
            fail("Expected an exception");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("must not be null"));
        }
    }

    @Test
    public void factoryShouldWorkWithSpring() throws Exception {
        //expose the temp file path into spring via a parent context
        StaticApplicationContext parentContext = new StaticApplicationContext();
        parentContext.getBeanFactory().registerSingleton("logFilePath", tempFiles.createTempFileThatDoesNotExist(".log"));
        parentContext.getBeanFactory().registerSingleton("stackTracePath", tempFiles.createTempDirectoryThatDoesNotExist());
        parentContext.refresh();
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"classpath:/applicationContext.xml"}, false, parentContext);

        context.refresh();

        context.close();
    }

    @Test
    public void defaultErrorHandler_shouldPrintTheThrowableToStandardError() throws Exception {
        ByteArrayOutputStream actualSystemErrContents = new ByteArrayOutputStream();
        System.setErr(new PrintStream(actualSystemErrContents));
        Throwable expected = new RuntimeException().fillInStackTrace();

        OpsLoggerFactory.DEFAULT_ERROR_HANDLER.accept(expected);

        ByteArrayOutputStream expectedSystemErrContents = new ByteArrayOutputStream();
        expected.printStackTrace(new PrintStream(expectedSystemErrContents));

        assertArrayEquals(expectedSystemErrContents.toByteArray(), actualSystemErrContents.toByteArray());
    }

    void ensureCorrectlyConfigured(BasicOpsLogger<TestMessages> basicLogger) {
        assertEquals(Clock.systemUTC(), basicLogger.getClock());
        assertEquals(OpsLoggerFactory.DEFAULT_ERROR_HANDLER, basicLogger.getErrorHandler());
    }

    static enum TestMessages implements LogMessage {
        ; //don't actually need any messages for these tests

        //region LogMessage implementation guts
        private final String messageCode;
        private final String messagePattern;

        TestMessages(String messageCode, String messagePattern) {
            this.messageCode = messageCode;
            this.messagePattern = messagePattern;
        }

        @Override
        public String getMessageCode() {
            return messageCode;
        }

        @Override
        public String getMessagePattern() {
            return messagePattern;
        }
        //endregion
    }
}
