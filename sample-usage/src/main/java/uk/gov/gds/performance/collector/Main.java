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
        System.out.printf("Logging to %s", path.toString());
        OpsLogger<CollectorLogMessage> logger = new OpsLoggerFactory()
                .setPath(path)
                .setStoreStackTracesInFilesystem(false)
                .build();

        for(int i = 0; i < Integer.MAX_VALUE; i++) {
            Thread.sleep(10L);
            logger.log(CollectorLogMessage.SampleMessage, i);
        }

//        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/applicationContext.xml");
//        ClassThatLogs classThatLogs = context.getBean("classThatLogs", ClassThatLogs.class);
//        classThatLogs.foo();
    }
}