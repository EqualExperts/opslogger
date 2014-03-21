package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;

public class ClassThatLogs {
    private final OpsLogger<CollectorLogMessage> logger;

    public ClassThatLogs(OpsLogger<CollectorLogMessage> logger) {
        this.logger = logger;
    }

    public void foo() {
        logger.log(CollectorLogMessage.Success, 42);
    }

    public void bar() {
        RuntimeException e = new RuntimeException();
        logger.log(CollectorLogMessage.UnknownError, e);
        throw e;
    }

    public void baz() {

    }
}
