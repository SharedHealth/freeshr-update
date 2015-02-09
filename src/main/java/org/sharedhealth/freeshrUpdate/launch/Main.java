package org.sharedhealth.freeshrUpdate.launch;

import org.sharedhealth.freeshrUpdate.config.AtomClientDatabaseConfig;
import org.sharedhealth.freeshrUpdate.config.SHRCassandraConfig;
import org.sharedhealth.freeshrUpdate.schedule.Scheduler;
import org.springframework.context.annotation.*;
import rx.schedulers.Schedulers;

@Configuration
@Import({AtomClientDatabaseConfig.class, SHRCassandraConfig.class})
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext springContext =
                new AnnotationConfigApplicationContext();
        springContext.scan("org.sharedhealth.freeshrUpdate");
        springContext.refresh();
        springContext.getBean(Scheduler.class).start(Schedulers.immediate());
    }
}
