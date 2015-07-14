package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;

import static uk.gov.gds.performance.collector.CollectorLogMessages.SUCCESS;
import static uk.gov.gds.performance.collector.CollectorLogMessages.UNKNOWN_ERROR;

public class ClassThatLogs {
    private final OpsLogger<CollectorLogMessages> logger;

    public ClassThatLogs(OpsLogger<CollectorLogMessages> logger) {
        this.logger = logger;
    }

    public void foo() {
        logger.log(SUCCESS, 42);
    }

    public void bar() {
        RuntimeException e = new RuntimeException();
        logger.log(UNKNOWN_ERROR, e);
        throw e;
    }

    public void baz() {

    }
}
