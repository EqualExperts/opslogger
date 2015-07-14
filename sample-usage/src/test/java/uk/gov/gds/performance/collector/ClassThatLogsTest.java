package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerTestDouble;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ClassThatLogsTest {
    private final OpsLogger<CollectorLogMessages> mockLogger = spy(new OpsLoggerTestDouble<>());
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
            verify(mockLogger).log(CollectorLogMessages.UNKNOWN_ERROR, e);
        }
    }

    @Test
    public void baz_shouldNotLogAnyMessages() throws Exception {
        theClass.baz();
        verifyZeroInteractions(mockLogger);
    }
}
