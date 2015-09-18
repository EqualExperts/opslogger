package uk.gov.gds.performance.collector;

import com.equalexperts.logging.DiagnosticContextSupplier;
import com.equalexperts.logging.OpsLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

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

    public String logContextsAcrossThreads() {
        LocalContext context = new LocalContext(UUID.randomUUID().toString());
        IntStream.rangeClosed(1, 5).parallel().forEach(i -> logger.with(context).log(SUCCESS, i));
        IntStream.rangeClosed(6, 10).parallel().forEach(i -> logger.with(new LocalContext("fred")).log(SUCCESS, i));
        return context.getJobId();
    }

    public void baz() {

    }

    static class LocalContext implements DiagnosticContextSupplier {
        private final String jobId;

        public LocalContext(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public Map<String, String> getMessageContext() {
            Map<String,String> context = new HashMap<>();
            context.put("jobId", jobId);
            return context;
        }

        public String getJobId() {
            return jobId;
        }
    }
}