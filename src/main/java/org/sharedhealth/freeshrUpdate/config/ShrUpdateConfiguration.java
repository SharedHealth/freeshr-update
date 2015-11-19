package org.sharedhealth.freeshrUpdate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.URI;

@Configuration
public class ShrUpdateConfiguration {

    @Value("${MCI_SERVER_URL}")
    private String mciServerUrl;

    @Value("${PATIENT_UPDATE_FEED_CONTEXT_PATH}")
    private String patientUpdateFeedContextPath;

    @Value("${CASSANDRA_KEYSPACE}")
    private String cassandraKeySpace;

    @Value("${CASSANDRA_HOST}")
    private String cassandraHost;

    @Value("${CASSANDRA_PORT}")
    private int cassandraPort;

    @Value("${CASSANDRA_USER}")
    private String cassandraUser;

    @Value("${CASSANDRA_PASSWORD}")
    private String cassandraPassword;

    @Value("${CASSANDRA_TIMEOUT}")
    private int cassandraTimeout;

    @Value("${FHIR_DOCUMENT_SCHEMA_VERSION}")
    private String fhirDocumentSchemaVersion;

    @Value("${UPDATE_INTERVAL_SECONDS}")
    private String updateIntervalInSeconds;

    @Value("${RETRY_UPDATE_INTERVAL_SECONDS}")
    private String retryUpdateIntervalInSeconds;

    @Value("${IDP_SERVER_LOGIN_URL}")
    private String idpServerLoginUrl;

    @Value("${IDP_CLIENT_ID}")
    private String idpClientId;

    @Value("${IDP_AUTH_TOKEN}")
    private String idpClientAuthToken;

    @Value("${IDP_CLIENT_EMAIL}")
    private String idpClientEmail;

    @Value("${IDP_CLIENT_PASSWORD}")
    private String idpClientPassword;

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

    public String getIdpServerLoginUrl() {
        return idpServerLoginUrl;
    }

    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public String getCassandraUser() {
        return cassandraUser;
    }

    public String getCassandraPassword() {
        return cassandraPassword;
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

    public int getRetryUpdateIntervalInSeconds() {
        return Integer.parseInt(retryUpdateIntervalInSeconds);
    }

    public URI getMciPatientUpdateFeedUrl() {
        return URI.create(mciServerUrl + patientUpdateFeedContextPath);
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

    public String getIdpClientAuthToken() {
        return idpClientAuthToken;
    }

    public String getIdpClientId() {
        return idpClientId;
    }

    public String getIdpClientEmail() {
        return idpClientEmail;
    }

    public String getIdpClientPassword() {
        return idpClientPassword;
    }

    public String getFhirDocumentSchemaVersion() {
        return fhirDocumentSchemaVersion;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
