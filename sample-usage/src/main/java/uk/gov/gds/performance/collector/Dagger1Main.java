package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerFactory;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Singleton;
import java.nio.file.Paths;

/** Example using Dagger 1.
 *
 * Intellij IDEA must have annotation processing explicitly enabled:
 * https://www.jetbrains.com/idea/help/compiler-annotation-processors.html
 *
 */
public class Dagger1Main {
    @Module(injects=ClassThatLogs.class)
    public static class SampleApplicationModule {
        @Provides
        @Singleton
        OpsLogger<CollectorLogMessages> getLogger() {
            return new OpsLoggerFactory()
                    .setDestination(System.out)
                    .setStackTraceStoragePath(Paths.get("/tmp/stacktraces"))
                    .build();
        }

        @Provides
        ClassThatLogs classThatLogs(OpsLogger<CollectorLogMessages> logger) {
            return new ClassThatLogs(logger);
        }
    }

    public static void main(String... args) {
        ObjectGraph graph = ObjectGraph.create(new SampleApplicationModule());
        ClassThatLogs classThatLogs = graph.get(ClassThatLogs.class);

        classThatLogs.foo();
    }
}
