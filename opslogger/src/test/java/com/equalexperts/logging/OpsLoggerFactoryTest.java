package com.equalexperts.logging;

import com.equalexperts.logging.impl.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.*;
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

    private final OpsLoggerFactory factory = new OpsLoggerFactory();

    private final BasicOpsLoggerFactory basicOpsLoggerFactoryMock = mock(BasicOpsLoggerFactory.class);

    private final AsyncOpsLoggerFactory asyncOpsLoggerFactoryMock = mock(AsyncOpsLoggerFactory.class);

    @Before
    public void setup() throws Exception {
        factory.setBasicOpsLoggerFactory(basicOpsLoggerFactoryMock);
        factory.setAsyncOpsLoggerFactory(asyncOpsLoggerFactoryMock);

        //new instances every time the mocks are called
        when(basicOpsLoggerFactoryMock.build(Mockito.any())).thenAnswer(invocation -> createMockBasicOpsLogger());
        when(asyncOpsLoggerFactoryMock.build(Mockito.any())).thenAnswer(invocation -> createMockAsyncOpsLogger());
    }

    @Test
    public void build_shouldReturnACorrectlyConfiguredBasicOpsLoggerToSystemOut_whenNoConfigurationIsPerformed() throws Exception {
        /*
            Construct a whole new instances without mocks just as a sanity check.
            This ensures that everything will work when the test accessors aren't manipulated at all.
        */

        OpsLogger<TestMessages> logger = new OpsLoggerFactory().build();

        BasicOpsLogger<TestMessages> basicLogger = (BasicOpsLogger<TestMessages>) logger;
        assertThat(basicLogger.getDestination(), instanceOf(OutputStreamDestination.class));
        assertEquals(InfrastructureFactory.EMPTY_CONTEXT_SUPPLIER, basicLogger.getContextSupplier());
        OutputStreamDestination<TestMessages> destination = (OutputStreamDestination<TestMessages>) basicLogger.getDestination();
        assertSame(System.out, destination.getOutput());
        assertThat(destination.getStackTraceProcessor(), instanceOf(SimpleStackTraceProcessor.class));
        ensureCorrectlyConfigured(basicLogger);
    }

    @Test
    public void build_shouldDelegateToTheBasicOpsLoggerFactory_whenTheDefaultAsyncValueIsUsed() throws Exception {
        BasicOpsLogger<TestMessages> expectedResult = createMockBasicOpsLogger();
        when(basicOpsLoggerFactoryMock.build(Mockito.any())).thenAnswer(invocation -> expectedResult);

        OpsLogger<TestMessages> result = factory.build();

        verify(basicOpsLoggerFactoryMock).build(any(InfrastructureFactory.class));
        verifyZeroInteractions(asyncOpsLoggerFactoryMock);
        assertSame(expectedResult, result);
    }

    @Test
    public void build_shouldDelegateToTheBasicOpsLoggerFactory_whenAsyncIsSetToFalse() throws Exception {
        BasicOpsLogger<TestMessages> expectedResult = createMockBasicOpsLogger();
        when(basicOpsLoggerFactoryMock.build(Mockito.any())).thenAnswer(invocation -> expectedResult);

        OpsLogger<TestMessages> result = factory.setAsync(false).build();

        verify(basicOpsLoggerFactoryMock).build(any(InfrastructureFactory.class));
        verifyZeroInteractions(asyncOpsLoggerFactoryMock);
        assertSame(expectedResult, result);
    }

    @Test
    public void build_shouldDelegateToTheAsyncOpsLoggerFactory_whenAsyncIsSetToTrue() throws Exception {
        AsyncOpsLogger<TestMessages> expectedResult = createMockAsyncOpsLogger();
        when(asyncOpsLoggerFactoryMock.build(Mockito.any())).thenAnswer(invocation -> expectedResult);

        OpsLogger<TestMessages> result = factory.setAsync(true).build();

        verify(asyncOpsLoggerFactoryMock).build(any(InfrastructureFactory.class));
        verifyZeroInteractions(basicOpsLoggerFactoryMock);
        assertSame(expectedResult, result);
    }

    @Test
    public void build_shouldPassTheProvidedLogfilePathToTheInternalFactory() throws Exception {
        Path logfile = tempFiles.createTempFile(".log");

        factory
            .setPath(logfile)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertEquals(logfile, capturedFactory.getLogfilePath().get());
        assertFalse(capturedFactory.getLoggerOutput().isPresent()); //and an output stream should not be provided
    }

    @Test
    public void build_shouldPassTheProvidedDestinationToTheInternalFactory() throws Exception {
        PrintStream destination = new PrintStream(new ByteArrayOutputStream());

        factory
            .setDestination(destination)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertSame(destination, capturedFactory.getLoggerOutput().get());
        assertFalse(capturedFactory.getLogfilePath().isPresent()); //and a logfile path should not be provided
    }

    @Test
    public void build_shouldPassTheStacktraceStorageSettingToTheInternalFactory() throws Exception {
        factory
            .setStoreStackTracesInFilesystem(true)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertTrue(capturedFactory.getStoreStackTracesInFilesystem().get());
    }

    @Test
    public void build_shouldPassTheStacktraceStoragePathToTheInternalFactory() throws Exception {
        Path storagePath = tempFiles.createTempDirectory();

        factory
            .setStackTraceStoragePath(storagePath)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertTrue(capturedFactory.getStoreStackTracesInFilesystem().get());
    }

    @Test
    public void build_shouldPassTheProvidedErrorHandlerToTheInternalFactory() throws Exception {
        Consumer<Throwable> errorHandler = t -> {};

        factory
            .setErrorHandler(errorHandler)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertSame(errorHandler, capturedFactory.getErrorHandler().get());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void build_shouldConvertTheProvidedCorrelationIdSupplierIntoAContextSupplierAndPassItToTheInternalFactory() throws Exception {
        ContextSupplier unExpectedSupplier = TreeMap::new;
        HashMap<String, String> expectedContext = new HashMap<>();

        @SuppressWarnings("unchecked")
        Supplier<Map<String, String>> mockSupplier = (Supplier<Map<String, String>>) mock(Supplier.class);
        when(mockSupplier.get()).thenReturn(expectedContext);

        factory
            .setContextSupplier(unExpectedSupplier)
            .setCorrelationIdSupplier(mockSupplier)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        ContextSupplier contextSupplier = capturedFactory.getContextSupplier().get();
        Map<String, String> actualContext = contextSupplier.getMessageContext();
        assertSame(expectedContext, actualContext);
        verify(mockSupplier).get();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void build_shouldPassTheProvidedContextSupplierToTheInternalFactory() throws Exception {
        Supplier<Map<String,String>> oldSupplier = TreeMap::new;
        ContextSupplier expectedSupplier = HashMap::new;

        factory
            .setCorrelationIdSupplier(oldSupplier)
            .setContextSupplier(expectedSupplier)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertSame(expectedSupplier, capturedFactory.getContextSupplier().get());
    }

    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes") //empty optional isn't typed
    @Test
    public void build_shouldPassSensibleDefaultsToTheFactory_givenNothingChosen() throws Exception {
        factory.build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        //empty optionals tell the InfrastructureFactory to choose a sensible default
        assertEquals(Optional.empty(), capturedFactory.getLoggerOutput());
        assertEquals(Optional.empty(), capturedFactory.getLogfilePath());
        assertEquals(Optional.empty(), capturedFactory.getStoreStackTracesInFilesystem());
        assertEquals(Optional.empty(), capturedFactory.getStackTraceStoragePath());
        assertEquals(Optional.empty(), capturedFactory.getErrorHandler());
        assertEquals(Optional.empty(), capturedFactory.getContextSupplier());
    }

    @Test
    public void build_shouldReuseInstances_whenNoChangesHaveBeenMade() throws Exception {
        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();

        assertSame(first, second);
    }

    @Test
    public void setDestination_shouldClearTheCachedInstance() throws Exception {
        PrintStream destination = new PrintStream(new ByteArrayOutputStream());
        factory.setDestination(destination);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setDestination(destination).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setPath_shouldClearTheCachedInstance() throws Exception {
        Path logFile = tempFiles.createTempFile(".log");
        factory.setPath(logFile);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setPath(logFile).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setStoreStackTracesInFilesystem_shouldClearTheCachedInstance() throws Exception {
        factory.setStoreStackTracesInFilesystem(false);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setStoreStackTracesInFilesystem(false).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setStackTraceStoragePath_shouldClearTheCachedInstance() throws Exception {
        Path directory = tempFiles.createTempDirectory();
        factory.setStackTraceStoragePath(directory);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setStackTraceStoragePath(directory).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setErrorHandler_shouldClearTheCachedInstance() throws Exception {
        Consumer<Throwable> errorHandler = t -> {};
        factory.setErrorHandler(errorHandler);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setErrorHandler(errorHandler).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setCorrelationIdSupplier_shouldClearTheCachedInstance() throws Exception {
        Supplier<Map<String, String>> correlationIdSupplier = Collections::emptyMap;
        factory.setCorrelationIdSupplier(correlationIdSupplier);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setCorrelationIdSupplier(correlationIdSupplier).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setCorrelationIdSupplier_shouldWorkAsExpected_givenNull() throws Exception {
        factory.setCorrelationIdSupplier(null).build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertFalse("should pass an empty optional", capturedFactory.getContextSupplier().isPresent());
    }

    @Test
    public void setContextSupplier_shouldClearTheCachedInstance() throws Exception {
        ContextSupplier contextSupplier = Collections::emptyMap;
        factory.setContextSupplier(contextSupplier);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setContextSupplier(contextSupplier).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setAsync_shouldClearTheCachedInstance() throws Exception {
        factory.setAsync(false);

        OpsLogger<TestMessages> first = factory.build();
        OpsLogger<TestMessages> second = factory.build();
        OpsLogger<TestMessages> third = factory.setAsync(false).build(); //even with the same argument

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @Test
    public void setStoreStackTracesInFilesystem_shouldClearTheStackTraceStoragePath_givenFalse() throws Exception {
        Path originalStackTraceDestination = tempFiles.createTempDirectoryThatDoesNotExist();

        factory
            .setPath(tempFiles.createTempFileThatDoesNotExist(".log"))
            .setStackTraceStoragePath(originalStackTraceDestination)
            .setStoreStackTracesInFilesystem(false)
            .setStoreStackTracesInFilesystem(true)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertNotEquals(originalStackTraceDestination, capturedFactory.getStackTraceStoragePath());
    }
    
    @Test
    public void setStoreStackTracesInFileSystem_shouldWorkIfItIsCalledBeforeAPathIsSet() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();

        factory
            .setStoreStackTracesInFilesystem(true)
            .setDestination(System.out)
            .setStackTraceStoragePath(parent)
            .build();

        InfrastructureFactory capturedFactory = captureProvidedInfrastructureFactory();

        assertEquals(parent, capturedFactory.getStackTraceStoragePath().get());
    }

    @Test
    public void setStackTraceStoragePath_shouldThrowAnException_givenNull() throws Exception {

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

        factory.setStackTraceStoragePath(directory);
    }

    @Test
    public void setStackTraceStoragePath_shouldNotCreateAnyDirectories_whenBuildIsNotCalled() throws Exception {
        Path parent = tempFiles.createTempDirectoryThatDoesNotExist();
        Path child = tempFiles.register(parent.resolve("child"));

        //preconditions
        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(child));

        factory.setStackTraceStoragePath(child);

        assertFalse(Files.exists(parent));
        assertFalse(Files.exists(child));
    }

    @Test
    public void setPath_shouldThrowAnException_givenANullPath() throws Exception {

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

    private void ensureCorrectlyConfigured(BasicOpsLogger<TestMessages> logger) {
        assertEquals(Clock.systemUTC(), logger.getClock());
        assertEquals(InfrastructureFactory.DEFAULT_ERROR_HANDLER, logger.getErrorHandler());
        assertThat(logger.getLock(), instanceOf(ReentrantLock.class));
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

    @SuppressWarnings("unchecked")
    private static BasicOpsLogger<TestMessages> createMockBasicOpsLogger() {
        return Mockito.mock(BasicOpsLogger.class);
    }

    @SuppressWarnings("unchecked")
    private static AsyncOpsLogger<TestMessages> createMockAsyncOpsLogger() {
        return Mockito.mock(AsyncOpsLogger.class);
    }

    /**
     * Obtains the InfrastructureFactory instance passed to either the basic or async internal factory
     */
    private InfrastructureFactory captureProvidedInfrastructureFactory() throws IOException {
        ArgumentCaptor<InfrastructureFactory> captor = ArgumentCaptor.forClass(InfrastructureFactory.class);
        verify(basicOpsLoggerFactoryMock, Mockito.atMost(1)).build(captor.capture());
        verify(asyncOpsLoggerFactoryMock, Mockito.atMost(1)).build(captor.capture());
        return captor.getValue();
    }

}
