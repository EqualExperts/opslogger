package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerFactory;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import java.nio.file.Paths;

public class DaggerMain {
    @Module(injects=ClassThatLogs.class)
    public static class SampleApplicationModule {
        @Provides
        OpsLogger<CollectorLogMessage> logger() {
            return new OpsLoggerFactory()
                    .setDestination(System.out)
                    .setStackTraceStoragePath(Paths.get("/tmp/stacktraces"))
                    .build();
        }

        @Provides
        ClassThatLogs classThatLogs(OpsLogger<CollectorLogMessage> logger) {
            return new ClassThatLogs(logger);
        }
    }

    public static void main(String... args) {
        ObjectGraph graph = ObjectGraph.create(new SampleApplicationModule());
        ClassThatLogs classThatLogs = graph.get(ClassThatLogs.class);
        classThatLogs.foo();
    }
}
