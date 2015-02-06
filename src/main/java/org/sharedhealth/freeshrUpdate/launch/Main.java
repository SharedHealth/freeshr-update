package org.sharedhealth.freeshrUpdate.launch;

import org.sharedhealth.freeshrUpdate.config.DatabaseConfig;
import org.sharedhealth.freeshrUpdate.schedule.Update;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext springContext =
                new AnnotationConfigApplicationContext();
        springContext.scan("org.sharedhealth.freeshrUpdate");
        springContext.refresh();
        springContext.getBean(Update.class).start();
    }
}
