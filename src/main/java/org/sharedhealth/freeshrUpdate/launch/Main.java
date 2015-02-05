package org.sharedhealth.freeshrUpdate.launch;

import org.sharedhealth.freeshrUpdate.schedule.Update;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
public class Main {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext springContext =
                new AnnotationConfigApplicationContext();
        springContext.scan("org.sharedhealth.freeshrUpdate");
        springContext.refresh();
        springContext.getBean(Update.class).start();
    }
}
