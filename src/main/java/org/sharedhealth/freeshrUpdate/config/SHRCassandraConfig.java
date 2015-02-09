package org.sharedhealth.freeshrUpdate.config;

import com.datastax.driver.core.SocketOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = "org.freeshr.infrastructure.persistence")
public class SHRCassandraConfig extends AbstractCassandraConfiguration {

    @Autowired
    private ShrUpdateConfiguration configuration;

    @Override
    protected String getKeyspaceName() {
        return configuration.getCassandraKeySpace();
    }

    @Override
    protected String getContactPoints() {
        return configuration.getCassandraHost();
    }

    @Override
    protected int getPort() {
        return configuration.getCassandraPort();
    }

    @Override
    protected SocketOptions getSocketOptions() {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setConnectTimeoutMillis(configuration.getCassandraTimeout());
        socketOptions.setReadTimeoutMillis(configuration.getCassandraTimeout());
        return socketOptions;
    }

    @Bean(name = "SHRCassandraTemplate")
    public CqlOperations CassandraTemplate() throws Exception {
        return new CqlTemplate(session().getObject());
    }

}
