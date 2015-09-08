package org.sharedhealth.freeshrUpdate.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"org.sharedhealth.freeshrUpdate"})
@Import({SHRCassandraConfig.class})
public class ShrUpdateConfig {
}
