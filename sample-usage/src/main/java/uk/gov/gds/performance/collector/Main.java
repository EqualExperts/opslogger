package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerFactory;

import java.nio.file.Paths;

public class Main {
    public static void main(String... args) throws Exception {
        OpsLogger<CollectorLogMessages> logger = new OpsLoggerFactory()
                .setDestination(System.out)
                .setStackTraceStoragePath(Paths.get("/tmp/stacktraces"))
                .build();

        ClassThatLogs cls = new ClassThatLogs(logger);
        cls.foo();
    }
}
