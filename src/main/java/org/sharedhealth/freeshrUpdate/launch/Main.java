package org.sharedhealth.freeshrUpdate.launch;

import org.sharedhealth.freeshrUpdate.config.AtomClientDatabaseConfig;
import org.sharedhealth.freeshrUpdate.config.SHRCassandraConfig;
import org.sharedhealth.freeshrUpdate.config.ScheduleConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AtomClientDatabaseConfig.class, SHRCassandraConfig.class, ScheduleConfig.class})
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext springContext = new AnnotationConfigApplicationContext();
        springContext.scan("org.sharedhealth.freeshrUpdate");
        springContext.refresh();
    }
}
