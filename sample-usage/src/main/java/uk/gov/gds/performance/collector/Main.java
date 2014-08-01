package uk.gov.gds.performance.collector;

import com.equalexperts.logging.OpsLogger;
import com.equalexperts.logging.OpsLoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class Main {
    public static void main(String... args) throws Exception {

//        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "test.log");
        //Path path = Paths.get("/private/tmp/test.log");
        Path path = Paths.get("/tmp/test.log");
        System.out.printf("Logging to %s%n", path.toString());
        OpsLogger<CollectorLogMessage> logger = new OpsLoggerFactory()
                .setPath(path)
                //.setStoreStackTracesInFilesystem(false)
                .setStackTraceStoragePath(Paths.get("/tmp/stacktraces"))
                .setAsync(false)
                .build();

        int count = Integer.MAX_VALUE;
//        int count = 10000;
//        long startTime = System.nanoTime();
        for(int i = 0; i < count; i++) {
            //Thread.sleep(10L);
            logger.log(CollectorLogMessage.SampleMessage, new RuntimeException(String.valueOf(i)), i);
        }
//        double timePerInvocation = (System.nanoTime() - startTime) / (double) count;
//        logger.close();
//        System.out.printf("%s nanoseconds per invocation%n", timePerInvocation);


//        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/applicationContext.xml");
//        ClassThatLogs classThatLogs = context.getBean("classThatLogs", ClassThatLogs.class);
//        classThatLogs.foo();
    }
}