package org.sharedhealth.freeshrUpdate.launch;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(UpdaterConfig.class)
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("see something");
//        SpringApplication.run(Main.class, args);
    }
}
