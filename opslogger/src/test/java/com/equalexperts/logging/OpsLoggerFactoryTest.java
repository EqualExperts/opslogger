package com.equalexperts.logging;

import com.equalexperts.logging.impl.*;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OpsLoggerFactoryTest {

    @Rule
    public TempFileFixture tempFiles = new TempFileFixture();

    @Rule
    public RestoreSystemStreamsFixture systemStreamsFixture =  new RestoreSystemStreamsFixture();

    @Rule
    public RestoreActiveRotationRegistryFixture registryFixture = new RestoreActiveRotationRegistryFixture();

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToSystemOut_whenNoConfigurationIsPerformed() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getDestination(), instanceOf(OutputStreamDestination.class));
        assertEquals(ConfigurationInfo.EMPTY_CORRELATION_ID_SUPPLIER, basicLogger.getCorrelationIdSupplier());
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        assertSame(System.out, destination.getOutput());
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
        ensureCorrectlyConfigured(basicLogger);
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToTheCorrectPrintStream_whenAPrintStreamIsSet() throws Exception {
        PrintStream expectedPrintStream = new PrintStream(new ByteArrayOutputStream());

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setDestination(expectedPrintStream)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getDestination(), instanceOf(OutputStreamDestination.class));
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        assertSame(expectedPrintStream, destination.getOutput());
        ensureCorrectlyConfigured(basicLogger);
    }

    @Test
    public void build_shouldReturnABasicOpsLoggerConfiguredToWriteToTheSpecifiedPath_whenAPathIsSet() throws Exception {
        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(expectedPath.toAbsolutePath()).thenReturn(expectedPath);

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(expectedPath)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        ensureCorrectlyConfigured(basicLogger);

        PathDestination<TestMessages> destination = (PathDestination<TestMessages>) basicLogger.getDestination();
        FileChannelProvider provider = destination.getProvider();
        assertSame(expectedPath, provider.getPath());
    }

    @Test
    public void build_shouldCreateAllNecessaryParentDirectories_whenAPathWithParentsThatDoNotExistIsSet() throws Exception {
        Path grandParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path parent = tempFiles.register(grandParent.resolve(UUID.randomUUID().toString()));
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        //preconditions
        assertFalse(Files.exists(grandParent));
        assertFalse(Files.exists(parent));

        //execute
        new OpsLoggerFactory()
                .setPath(logFile)
                .<TestMessages>build();

        //assert
        assertTrue(Files.exists(grandParent));
        assertTrue(Files.exists(parent));
    }

    @Test
    public void build_shouldNotComplain_whenAPathWithParentsThatDoExistIsSet() throws Exception {
        Path parent = Paths.get(System.getProperty("java.io.tmpdir"));
        Path logFile = parent.resolve(UUID.randomUUID().toString().replace("-", "") + ".log");

        //preconditions
        assertTrue(Files.exists(parent));
        assertFalse(Files.exists(logFile));

        //execute
        new OpsLoggerFactory()
                .setPath(logFile)
                .<TestMessages>build();

        //assert
        assertTrue(Files.exists(parent));
        assertFalse(Files.exists(logFile));
    }

    @Test
    public void build_shouldRegisterTheCreatedPathDestinationWithTheRegistry_whenAPathIsSet() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setPath(logFile)
                .build();

        BasicOpsLogger<TestMessages> logger = (BasicOpsLogger<TestMessages>) result;
        assertThat(logger.getDestination(), instanceOf(PathDestination.class));
        PathDestination<TestMessages> pd = (PathDestination<TestMessages>) logger.getDestination();
        assertTrue(ActiveRotationRegistry.getSingletonInstance().contains(pd));
    }

    @Test
    public void build_shouldRegisterTheCreatedPathDestinationWithTheRegistry_whenAPathIsSetAndAsyncIsSet() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setPath(logFile)
                .setAsync(true)
                .build();

        AsyncOpsLogger<TestMessages> logger = (AsyncOpsLogger<TestMessages>) result;
        assertThat(logger.getDestination(), instanceOf(PathDestination.class));
        PathDestination<TestMessages> pd = (PathDestination<TestMessages>) logger.getDestination();
        assertTrue(ActiveRotationRegistry.getSingletonInstance().contains(pd));
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredAsyncOpsLoggerToSystemOut_whenAsyncIsSet() throws Exception {
        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setAsync(true)
                .build();

        AsyncOpsLogger<TestMessages> logger = (AsyncOpsLogger<TestMessages>) result;
        assertThat(logger.getDestination(), instanceOf(OutputStreamDestination.class));
        assertEquals(ConfigurationInfo.EMPTY_CORRELATION_ID_SUPPLIER, logger.getCorrelationIdSupplier());
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) logger.getDestination();
        assertSame(System.out, destination.getOutput());
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
        ensureCorrectlyConfigured(logger);
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredAsyncOpsLoggerToTheCorrectPrintStream_whenAPrintStreamIsSetAndAsyncIsSet() throws Exception {
        PrintStream expectedPrintStream = new PrintStream(new ByteArrayOutputStream());

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setDestination(expectedPrintStream)
                .setAsync(true)
                .build();

        AsyncOpsLogger<TestMessages> logger = (AsyncOpsLogger<TestMessages>) result;
        assertThat(logger.getDestination(), instanceOf(OutputStreamDestination.class));
        assertEquals(ConfigurationInfo.EMPTY_CORRELATION_ID_SUPPLIER, logger.getCorrelationIdSupplier());
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) logger.getDestination();
        assertSame(expectedPrintStream, destination.getOutput());
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
        ensureCorrectlyConfigured(logger);

    }

    @Test
    public void build_shouldNotCreateParentDirectories_whenTheParentOfTheLogFileIsASymlink() throws Exception {
        Path actualParent = tempFiles.createTempDirectory();
        Path symLinkPath = tempFiles.createTempDirectoryThatDoesNotExist();
        Files.createSymbolicLink(symLinkPath, actualParent);
        Path logFile = symLinkPath.resolve(UUID.randomUUID().toString().replace("-", "") + ".log");

        //execute
        new OpsLoggerFactory()
                .setPath(logFile)
                .setStoreStackTracesInFilesystem(false) //otherwise an error here can cause this test to fail
                .<TestMessages>build();
    }

    @Test
    public void build_shouldReturnAnAsyncOpsLoggerConfiguredToWriteToTheSpecifiedPath_whenAPathIsSetAndAsyncIsSet() throws Exception {
        Path expectedPath = mock(Path.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(expectedPath.toAbsolutePath()).thenReturn(expectedPath);

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setAsync(true)
                .setPath(expectedPath)
                .build();

        AsyncOpsLogger<TestMessages> logger = (AsyncOpsLogger<TestMessages>) result;
        ensureCorrectlyConfigured(logger);

        PathDestination<TestMessages> destination = (PathDestination<TestMessages>) logger.getDestination();
        FileChannelProvider provider = destination.getProvider();
        assertSame(expectedPath, provider.getPath());
    }

    @Test
    public void build_shouldCreateAllNecessaryParentDirectories_whenAPathWithParentsThatDoNotExistIsSetAndAsyncIsSet() throws Exception {
        Path grandParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path parent = tempFiles.register(grandParent.resolve(UUID.randomUUID().toString()));
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        //preconditions
        assertFalse(Files.exists(grandParent));
        assertFalse(Files.exists(parent));

        //execute
        new OpsLoggerFactory()
                .setAsync(true)
                .setPath(logFile)
                .<TestMessages>build();

        //assert
        assertTrue(Files.exists(grandParent));
        assertTrue(Files.exists(parent));
    }

    @Test
    public void build_shouldNotComplain_whenAPathWithParentsThatDoExistIsSetAndAsyncIsSet() throws Exception {
        Path parent = Paths.get(System.getProperty("java.io.tmpdir"));
        Path logFile = parent.resolve(UUID.randomUUID().toString().replace("-", "") + ".log");

        //preconditions
        assertTrue(Files.exists(parent));
        assertFalse(Files.exists(logFile));

        //execute
        new OpsLoggerFactory()
                .setPath(logFile)
                .setAsync(true)
                .<TestMessages>build();

        //assert
        assertTrue(Files.exists(parent));
        assertFalse(Files.exists(logFile));
    }

    @Test
    public void build_shouldNotCreateParentDirectories_whenTheParentOfTheLogFileIsASymlinkAndAsyncIsSet() throws Exception {
        Path actualParent = tempFiles.createTempDirectory();
        Path symLinkPath = tempFiles.createTempDirectoryThatDoesNotExist();
        Files.createSymbolicLink(symLinkPath, actualParent);
        Path logFile = symLinkPath.resolve(UUID.randomUUID().toString().replace("-", "") + ".log");

        //execute
        new OpsLoggerFactory()
                .setAsync(true)
                .setPath(logFile)
                .setStoreStackTracesInFilesystem(false) //otherwise an error here can cause this test to fail
                .<TestMessages>build();
    }

    @Test
    public void build_shouldNotComplain_whenThePathIsRelativeToTheCurrentDirectoryForAnAsyncLogger() throws Exception {
        //setup
        Path relativeToCurrentDirectory = tempFiles.register(Paths.get("test.log"));

        //execute
        new OpsLoggerFactory()
                .setAsync(true)
                .setPath(relativeToCurrentDirectory)
                .<TestMessages>build();
    }

    @Test
    public void build_shouldNotComplain_whenThePathIsRelativeToTheCurrentDirectoryForASynchronousLogger() throws Exception {
        //setup
        Path relativeToCurrentDirectory = tempFiles.register(Paths.get("test.log"));

        //execute
        new OpsLoggerFactory()
                .setAsync(false)
                .setPath(relativeToCurrentDirectory)
                .<TestMessages>build();
    }

    @Test
    public void build_shouldSetASimpleStackTraceProcessor_whenAPrintStreamIsSetAndAStackTraceProcessorIsNotConfigured() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setDestination(new PrintStream(new ByteArrayOutputStream()))
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldSetASimpleStackTraceProcessor_whenNoConfigurationIsPerformed() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldSetAFileSystemStackTraceProcessorToAParentPath_whenAPathIsSetAndAStackTraceProcessorIsNotConfigured() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path logFile = tempFiles.register(parent.resolve("log.txt"));

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setPath(logFile)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        PathDestination<TestMessages> destination = (PathDestination<TestMessages>) basicLogger.getDestination();
        FilesystemStackTraceProcessor stackTraceProcessor = (FilesystemStackTraceProcessor) destination.getStackTraceProcessor();

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
        PathDestination<TestMessages> destination = (PathDestination<TestMessages>) basicLogger.getDestination();
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void build_shouldConfigureAFileSystemStackTraceProcessor_whenSetStoreStackTraceStoragePathIsCalled() throws Exception {
        Path stackTraceStorage = tempFiles.createTempDirectoryThatDoesNotExist();

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setStackTraceStoragePath(stackTraceStorage)
                .build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) destination.getStackTraceProcessor();
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
    public void build_shouldNotCreateDirectories_whenSetStoreStackTraceStoragePathIsCalledWithASymlink() throws Exception {
        Path actualParent = tempFiles.createTempDirectory();
        Path symLinkPath = tempFiles.createTempDirectoryThatDoesNotExist();
        Files.createSymbolicLink(symLinkPath, actualParent);

        //execute
        new OpsLoggerFactory() //don't log to a path, because that setting could break this
                .setStackTraceStoragePath(symLinkPath)
                .<TestMessages>build();
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
    public void build_shouldConstructABasicOpsLoggerWithTheDefaultErrorHandler_whenSetErrorHandlerIsCalledWithNull() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setErrorHandler(null)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        assertSame(ConfigurationInfo.DEFAULT_ERROR_HANDLER, basicLogger.getErrorHandler());
    }

    @Test
    public void build_shouldConstructABasicOpsLoggerWithTheCorrectCorrelationIdSupplier_whenACustomCorrelationIdSupplierHasBeenSet() throws Exception {
        Supplier<Map<String, String>> expectedCorrelationIdSupplier = HashMap::new;

        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setCorrelationIdSupplier(expectedCorrelationIdSupplier)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        assertSame(expectedCorrelationIdSupplier, basicLogger.getCorrelationIdSupplier());
    }

    @Test
    public void build_shouldConstructABasicOpsLoggerWithTheDefaultCorrelationIdSupplier_whenSetCorrelationIdSupplierIsCalledWithNull() throws Exception {
        OpsLogger<TestMessages> logger = new OpsLoggerFactory()
                .setCorrelationIdSupplier(null)
                .build();

        BasicOpsLogger basicLogger = (BasicOpsLogger) logger;
        assertSame(ConfigurationInfo.EMPTY_CORRELATION_ID_SUPPLIER, basicLogger.getCorrelationIdSupplier());
    }

    @Test
    public void build_shouldConstructAnAsyncOpsLoggerWithTheCorrectErrorHandler_whenACustomErrorHandlerHasBeenSetAndAsyncIsSet() throws Exception {
        Consumer<Throwable> expectedErrorHandler = (t) -> {};

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setErrorHandler(expectedErrorHandler)
                .setAsync(true)
                .build();

        AsyncOpsLogger logger = (AsyncOpsLogger) result;
        assertSame(expectedErrorHandler, logger.getErrorHandler());
    }

    @Test
    public void build_shouldConstructAnAsyncOpsLoggerWithTheDefaultErrorHandler_whenSetErrorHandlerIsCalledWithNullAndAsyncIsSet() throws Exception {
        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setAsync(true)
                .setErrorHandler(null)
                .build();

        AsyncOpsLogger logger = (AsyncOpsLogger) result;
        assertSame(ConfigurationInfo.DEFAULT_ERROR_HANDLER, logger.getErrorHandler());
    }

    @Test
    public void build_shouldConstructAnAsyncOpsLoggerWithTheCorrectCorrelationIdSupplier_whenACustomCorrelationIdSupplierHasBeenSetAndAsyncIsSet() throws Exception {
        Supplier<Map<String,String>> expectedCorrelationIdSupplier = TreeMap::new;

        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setCorrelationIdSupplier(expectedCorrelationIdSupplier)
                .setAsync(true)
                .build();

        AsyncOpsLogger logger = (AsyncOpsLogger) result;
        assertSame(expectedCorrelationIdSupplier, logger.getCorrelationIdSupplier());
    }

    @Test
    public void build_shouldConstructAnAsyncOpsLoggerWithTheDefaultCorrelationIdSupplier_whenSetCorrelationIdSupplierIsCalledWithNullAndAsyncIsSet() throws Exception {
        OpsLogger<TestMessages> result = new OpsLoggerFactory()
                .setCorrelationIdSupplier(null)
                .setAsync(true)
                .build();

        AsyncOpsLogger logger = (AsyncOpsLogger) result;
        assertSame(ConfigurationInfo.EMPTY_CORRELATION_ID_SUPPLIER, logger.getCorrelationIdSupplier());
    }

    @Test
    public void build_shouldConstructMultipleDifferentInstances_whenCalledTwice() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory()
                .setDestination(System.out);

        OpsLogger<TestMessages> asyncLogger = factory.setAsync(true).build();

        OpsLogger<TestMessages> syncLogger = factory.setAsync(false).build();

        assertNotSame(asyncLogger, syncLogger);
        assertThat(asyncLogger, instanceOf(AsyncOpsLogger.class));
        assertThat(syncLogger, instanceOf(BasicOpsLogger.class));
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

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        PathDestination<TestMessages> destination = (PathDestination<TestMessages>) basicLogger.getDestination();
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) destination.getStackTraceProcessor();
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

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        FilesystemStackTraceProcessor processor = (FilesystemStackTraceProcessor) destination.getStackTraceProcessor();
        assertEquals(parent, processor.getDestination());
    }

    @Test
    public void setStackTraceStoragePath_shouldThrowAnException_givenNull() throws Exception {
        OpsLoggerFactory factory = new OpsLoggerFactory();

        try {
            factory.setStackTraceStoragePath(null);
            fail("Expected an exception");
        } catch (NullPointerException expected) {
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
        } catch (NullPointerException expected) {
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
        } catch (NullPointerException expected) {
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

        ConfigurationInfo.DEFAULT_ERROR_HANDLER.accept(expected);

        ByteArrayOutputStream expectedSystemErrContents = new ByteArrayOutputStream();
        expected.printStackTrace(new PrintStream(expectedSystemErrContents));

        assertArrayEquals(expectedSystemErrContents.toByteArray(), actualSystemErrContents.toByteArray());
    }

    private void ensureCorrectlyConfigured(BasicOpsLogger<TestMessages> logger) {
        assertEquals(Clock.systemUTC(), logger.getClock());
        assertEquals(ConfigurationInfo.DEFAULT_ERROR_HANDLER, logger.getErrorHandler());
        assertThat(logger.getLock(), instanceOf(ReentrantLock.class));
    }

    private void ensureCorrectlyConfigured(AsyncOpsLogger<TestMessages> logger) {
        assertEquals(Clock.systemUTC(), logger.getClock());
        assertEquals(ConfigurationInfo.DEFAULT_ERROR_HANDLER, logger.getErrorHandler());
        assertThat(logger.getTransferQueue(), instanceOf(LinkedTransferQueue.class));
    }

    private enum TestMessages implements LogMessage {
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
