package com.equalexperts.logging.impl;

import com.equalexperts.logging.LogMessage;
import com.equalexperts.logging.RestoreSystemStreamsFixture;
import com.equalexperts.logging.TempFileFixture;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InfrastructureFactoryTest {

    @Rule
    public final RestoreSystemStreamsFixture systemStreamsFixture = new RestoreSystemStreamsFixture();

    @Rule
    public final TempFileFixture tempFiles = new TempFileFixture();

    private static final Optional<Path> SAMPLE_LOGFILE_PATH = Optional.empty();
    private static final Optional<PrintStream> SAMPLE_LOGGER_OUTPUT = Optional.of(System.out);
    private static final Optional<Boolean> SAMPLE_STORE_STACK_TRACES_IN_FILESYSTEM = Optional.of(false);
    private static final Optional<Path> SAMPLE_STACK_TRACE_STORAGE_PATH = Optional.empty();
    private static final Optional<Supplier<Map<String, String>>> SAMPLE_CORRELATION_ID_SUPPLIER = Optional.empty();
    private static final Optional<Consumer<Throwable>> SAMPLE_ERROR_HANDLER = Optional.of(e -> {});

    @Test
    public void emptyCorrelationIdSupplier_shouldAlwaysProduceAnEmptyMap() throws Exception {
        Map<String, String> defaultCorrelationIds = InfrastructureFactory.EMPTY_CORRELATION_ID_SUPPLIER.get();
        assertNotNull(defaultCorrelationIds);
        assertEquals(0, defaultCorrelationIds.size());
    }

    @Test
    public void defaultErrorHandler_shouldPrintAStackTraceToSystemError() throws Exception {
        Throwable mockThrowable = mock(Throwable.class);

        InfrastructureFactory.DEFAULT_ERROR_HANDLER.accept(mockThrowable);

        verify(mockThrowable).printStackTrace(same(System.err));
    }

    @Test
    public void configureCorrelationIdSupplier_shouldReturnTheProvidedSupplier_whenOneIsProvided() throws Exception {
        Supplier<Map<String, String>> expectedSupplier = HashMap::new; //don't use Collections.emptyMap, because that's the default

        InfrastructureFactory factory = new InfrastructureFactory(
                SAMPLE_LOGFILE_PATH,
                SAMPLE_LOGGER_OUTPUT,
                SAMPLE_STORE_STACK_TRACES_IN_FILESYSTEM,
                SAMPLE_STACK_TRACE_STORAGE_PATH,
                Optional.of(expectedSupplier),
                SAMPLE_ERROR_HANDLER);

        Supplier<Map<String, String>> actualSupplier = factory.configureCorrelationIdSupplier();

        assertSame(expectedSupplier, actualSupplier);
    }

    @Test
    public void configureCorrelationIdSupplier_shouldReturnTheDefaultSupplier_whenOneIsNotProvided() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                SAMPLE_LOGFILE_PATH,
                SAMPLE_LOGGER_OUTPUT,
                SAMPLE_STORE_STACK_TRACES_IN_FILESYSTEM,
                SAMPLE_STACK_TRACE_STORAGE_PATH,
                Optional.empty(),
                SAMPLE_ERROR_HANDLER);

        Supplier<Map<String, String>> actualSupplier = factory.configureCorrelationIdSupplier();

        assertSame(InfrastructureFactory.EMPTY_CORRELATION_ID_SUPPLIER, actualSupplier);
    }

    @Test
    public void configureErrorHandler_shouldReturnTheProvidedErrorHAndler_whenOneIsProvided() throws Exception {
        Consumer<Throwable> expectedErrorHandler = t -> {};

        InfrastructureFactory factory = new InfrastructureFactory(
                SAMPLE_LOGFILE_PATH,
                SAMPLE_LOGGER_OUTPUT,
                SAMPLE_STORE_STACK_TRACES_IN_FILESYSTEM,
                SAMPLE_STACK_TRACE_STORAGE_PATH,
                SAMPLE_CORRELATION_ID_SUPPLIER,
                Optional.of(expectedErrorHandler));

        Consumer<Throwable> actualErrorHandler = factory.configureErrorHandler();

        assertSame(expectedErrorHandler, actualErrorHandler);
    }

    @Test
    public void configureErrorHandler_shouldReturnTheDefaultErrorHAndle_whenOneIsNotProvided() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                SAMPLE_LOGFILE_PATH,
                SAMPLE_LOGGER_OUTPUT,
                SAMPLE_STORE_STACK_TRACES_IN_FILESYSTEM,
                SAMPLE_STACK_TRACE_STORAGE_PATH,
                SAMPLE_CORRELATION_ID_SUPPLIER,
                Optional.empty());

        Consumer<Throwable> actualErrorHandler = factory.configureErrorHandler();

        assertSame(InfrastructureFactory.DEFAULT_ERROR_HANDLER, actualErrorHandler);
    }

    @Test
    public void configureDestination_shouldCreateASimpleStackTraceProcessor_whenLoggingToAPathAndStoringStackTracesInTheFileSystemIsExplicitlyDisabled() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(tempFiles.createTempFile(".log")),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();

        assertThat(stackTraceProcessor, instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void configureDestination_shouldCreateASimpleStackTraceProcessor_whenLoggingToAStreamAndStoringStackTracesInTheFileSystemIsExplicitlyDisabled() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();

        assertThat(stackTraceProcessor, instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void configureDestination_shouldStoreStackTracesInTheFileSystem_whenLoggingToAPathAndAStacktraceStoragePathHasBeenExplicitlyProvided() throws Exception {
        Path expectedStackTraceStoragePath = tempFiles.createTempDirectory();

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(tempFiles.createTempFile(".log")),
                Optional.empty(),
                Optional.of(true), //will always be true when a path is provided
                Optional.of(expectedStackTraceStoragePath),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();

        assertThat(stackTraceProcessor, instanceOf(FilesystemStackTraceProcessor.class));
        FilesystemStackTraceProcessor fs = (FilesystemStackTraceProcessor) stackTraceProcessor;
        assertSame(expectedStackTraceStoragePath, fs.getDestination());
    }

    @Test
    public void configureDestination_shouldStoreStackTracesInTheFileSystem_whenLoggingToAStreamAndAStacktraceStoragePathHasBeenExplicitlyProvided() throws Exception {
        Path expectedStackTraceStoragePath = tempFiles.createTempDirectory();

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.of(true), //will always be true when a path is provided
                Optional.of(expectedStackTraceStoragePath),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();

        assertThat(stackTraceProcessor, instanceOf(FilesystemStackTraceProcessor.class));
        FilesystemStackTraceProcessor fs = (FilesystemStackTraceProcessor) stackTraceProcessor;
        assertSame(expectedStackTraceStoragePath, fs.getDestination());
    }

    @Test
    public void configureDestination_shouldStoreStackTracesInTheSameDirectoryAsTheLogFile_whenLoggingToAPathAndStoringStackTracesHasNotBeenExplicitlyConfigured() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(logFile),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();

        assertThat(stackTraceProcessor, instanceOf(FilesystemStackTraceProcessor.class));
        FilesystemStackTraceProcessor fs = (FilesystemStackTraceProcessor) stackTraceProcessor;
        assertEquals(logFile.getParent(), fs.getDestination());
    }

    @Test
    public void configureDestination_shouldNotStoreStackTracesInTheFileSystem_whenLoggingToAStreamAndStoringStackTracesHasNotExplicitlyBeenConfigured() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.empty(),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        StackTraceProcessor stackTraceProcessor = factory.<TestMessages>configureDestination().getStackTraceProcessor();
        assertThat(stackTraceProcessor, instanceOf(SimpleStackTraceProcessor.class));
    }

    @Test
    public void configureDestination_shouldThrowAnIllegalStateException_whenNoLogfileIsProvidedAndStoringStackTracesInTheFileSystemHasBeenEnabledAndNoDestinationHasBeenProvided() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.of(true),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        try {
            factory.configureDestination();
            fail("expected an IllegalStateException");
        } catch (IllegalStateException ignore) {
        }
    }

    @Test
    public void configureDestination_shouldCreateTheStackTraceStoragePath_whenItDoesNotExistAndIsNotASymlink() throws Exception {
        Path storagePathParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path storagePath = storagePathParent.resolve(UUID.randomUUID().toString());

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.of(true),
                Optional.of(storagePath),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        factory.configureDestination();

        assertTrue("storagePathParent must exist", Files.isDirectory(storagePathParent));
        assertTrue("storagePath must exist", Files.isDirectory(storagePath));
    }

    @Test
    public void configureDestination_shouldNotCreateTheStackTraceStoragePath_whenItIsASymlink() throws Exception {
        Path actualDestination = tempFiles.createTempDirectory();
        Path symLinkDestination = tempFiles.createTempDirectoryThatDoesNotExist();
        Files.createSymbolicLink(symLinkDestination, actualDestination);

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(System.err),
                Optional.of(true),
                Optional.of(symLinkDestination),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        factory.configureDestination(); //a FileAlreadyExistsException will be thrown if the code doesn't do the right thing
    }

    @Test
    public void configureDestination_shouldLogToSystemOut_whenNoPathOrStreamIsProvided() throws Exception {
        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        Destination<TestMessages> destination = factory.configureDestination();
        assertThat(destination, instanceOf(OutputStreamDestination.class));
        OutputStreamDestination osd = (OutputStreamDestination) destination;
        assertSame(System.out, osd.getOutput());
    }

    @Test
    public void configureDestination_shouldLogToTheProvidedPrintStream_givenAPrintStreamIsProvided() throws Exception {
        PrintStream ps = new PrintStream(new ByteArrayOutputStream());

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.empty(),
                Optional.of(ps),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        Destination<TestMessages> destination = factory.configureDestination();
        assertThat(destination, instanceOf(OutputStreamDestination.class));
        OutputStreamDestination osd = (OutputStreamDestination) destination;
        assertSame(ps, osd.getOutput());
    }

    @Test
    public void configureDestination_shouldLogToTheProvidedPath_whenALogfilePathIsProvided() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(logFile),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        Destination<TestMessages> destination = factory.configureDestination();
        assertThat(destination, instanceOf(PathDestination.class));
        PathDestination psd = (PathDestination) destination;
        assertSame(logFile, psd.getProvider().getPath());
    }

    @Test
    public void configureDestination_shouldCorrectlyConfigureAPathDestinationAndFileChannelProvider_whenLoggingToAPath() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(logFile),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        Destination<TestMessages> destination = factory.configureDestination();
        assertThat(destination, instanceOf(PathDestination.class));

        PathDestination psd = (PathDestination) destination;
        FileChannelProvider provider = psd.getProvider();
        assertSame(logFile, provider.getPath());
        assertSame(ActiveRotationRegistry.getSingletonInstance(), psd.getActiveRotationRegistry());
    }

    @Test
    public void configureDestination_shouldCreateTheLogFileParentDirectories_whenLoggingToAPathThatIsNotASymlink() throws Exception {
        Path grandParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path parent = grandParent.resolve(UUID.randomUUID().toString());
        Path logFile = parent.resolve("foo.log");

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(logFile),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        factory.configureDestination();
        assertTrue("grand parent directory must be created", Files.isDirectory(grandParent)); //ensures nested creation is used
        assertTrue("parent directory must be created", Files.isDirectory(parent));
    }

    @Test
    public void configureDestination_shouldNotCreateParentDirectories_givenAParentDirectoryThatIsASymlink() throws Exception {
        Path exists = tempFiles.createTempDirectory();
        Path symLinkParent = tempFiles.createTempDirectoryThatDoesNotExist();
        Files.createSymbolicLink(symLinkParent, exists);
        Path logFile = symLinkParent.resolve("foo.log");

        InfrastructureFactory factory = new InfrastructureFactory(
                Optional.of(logFile),
                Optional.empty(),
                Optional.of(false),
                Optional.empty(),
                SAMPLE_CORRELATION_ID_SUPPLIER,
                SAMPLE_ERROR_HANDLER);

        factory.configureDestination(); //a FileAlreadyExistsException will be thrown if the code doesn't do the right thing
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