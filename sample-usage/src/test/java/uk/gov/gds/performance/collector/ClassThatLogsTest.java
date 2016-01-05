package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerTestDouble;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ClassThatLogsTest {
    private final OpsLogger<CollectorLogMessages> mockLogger = OpsLoggerTestDouble.withSpyFunction(Mockito::spy);
    private final ClassThatLogs theClass = new ClassThatLogs(mockLogger);

    @Test
    public void foo_shouldLogASuccessMessage() throws Exception {
        theClass.foo();

        verify(mockLogger).log(CollectorLogMessages.SUCCESS, 42);
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    public void bar_shouldLogAnUnknownErrorMessageAndThrowAnException() throws Exception {
        try {
            theClass.bar();
            fail("expected an exception");
        } catch (RuntimeException e) {
            verify(mockLogger).logThrowable(CollectorLogMessages.UNKNOWN_ERROR, e);
        }
    }

    @Test
    public void baz_shouldNotLogAnyMessages() throws Exception {
        theClass.baz();
        verifyZeroInteractions(mockLogger);
    }

    @Test
    public void logContextsAcrossThreads_shouldCarryDiagnosticContextAcrossThreads() throws Exception {
        String jobId = theClass.logContextsAcrossThreads();

        OpsLogger<CollectorLogMessages> nestedLogger = mockLogger.with(new ClassThatLogs.LocalContext(jobId));
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 1);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 2);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 3);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 4);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 5);
        verifyNoMoreInteractions(nestedLogger);

        nestedLogger = mockLogger.with(new ClassThatLogs.LocalContext("fred"));
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 6);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 7);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 8);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 9);
        verify(nestedLogger).log(CollectorLogMessages.SUCCESS, 10);
        verifyNoMoreInteractions(nestedLogger);
    }
}
