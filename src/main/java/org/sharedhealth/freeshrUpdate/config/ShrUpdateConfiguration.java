package org.sharedhealth.freeshrUpdate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.URI;

@Configuration
public class ShrUpdateConfiguration {

    @Value("${MCI_SCHEME}")
    private String mciScheme;

    @Value("${MCI_HOST}")
    private String mciHost;

    @Value("${MCI_PORT}")
    private String mciPort;

    @Value("${MCI_USER}")
    private String mciUser;

    @Value("${MCI_PASSWORD}")
    private String mciPassword;

    @Value("${MCI_CONTEXT_PATH}")
    private String mciContextPath;

    @Value("${CASSANDRA_KEYSPACE}")
    private String cassandraKeySpace;

    @Value("${CASSANDRA_HOST}")
    private String cassandraHost;

    @Value("${CASSANDRA_PORT}")
    private int cassandraPort;

    @Value("${CASSANDRA_TIMEOUT}")
    private int cassandraTimeout;

    @Value("${UPDATE_INTERVAL_SECONDS}")
    private String updateIntervalInSeconds;

    @Value("${IDENTITY_SERVER_BASE_URL}")
    private String identityServerBaseUrl;

    @Value("${DATABASE_URL}")
    private String dbUrl;

    @Value("${DATABASE_USER}")
    private String dbUser;

    @Value("${DATABASE_PASSWORD}")
    private String dbPassword;

    @Value("${DATABASE_DRIVER}")
    private String dbDriver;

    @Value("${DATABASE_CON_POOL_SIZE}")
    private String dbConnectionPoolSize;

    @Value("${DATABASE_CHANGELOG_FILE}")
    private String dbChangeLogFile;


    public String getIdentityServerBaseUrl() {
        return identityServerBaseUrl;
    }

    public String getMciUser() {
        return mciUser;
    }

    public String getMciPassword() {
        return mciPassword;
    }

    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

    public int getCassandraTimeout() {
        return cassandraTimeout;
    }

    public int getUpdateIntervalInSeconds() {
        return Integer.parseInt(updateIntervalInSeconds);
    }

    public URI getMciBaseUrl() {
        return URI.create(mciScheme + "://" + mciHost + ":" + mciPort + "/" + mciContextPath);
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbConnectionPoolSize() {
        return dbConnectionPoolSize;
    }

    public String getDbChangeLogFile() {
        return dbChangeLogFile;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
