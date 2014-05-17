package uk.gov.gds.performance.collector;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String... args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:/applicationContext.xml");
        ClassThatLogs classThatLogs = context.getBean("classThatLogs", ClassThatLogs.class);
        classThatLogs.foo();
    }
}