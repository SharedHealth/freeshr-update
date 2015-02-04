package org.sharedhealth.freeshrUpdate.config;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SHRUpdateConfig {
    private String mciScheme;
    private String mciHost;
    private String mciPort;
    private String mciUser;
    private String mciPassword;
    private String mciContextPath;
    private String cassandraKeySpace;
    private String cassandraHost;
    private String cassandraPort;
    private String cassandraTimeout;
    private String updateIntervalInSeconds;

    public String getMciSchema() {
        return mciScheme;
    }

    public String getMciHost() {
        return mciHost;
    }

    public String getMciPort() {
        return mciPort;
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

    public String getCassandraPort() {
        return cassandraPort;
    }

    public String getCassandraTimeout() {
        return cassandraTimeout;
    }

    public int getUpdateIntervalInSeconds() {
        return Integer.parseInt(updateIntervalInSeconds);
    }

    public String getMciBaseUrl() {
        return mciScheme + "://" + mciHost + ":" + mciPort + "/" + mciContextPath;
    }

    public SHRUpdateConfig() {
        Map<String, String> env = System.getenv();
        updateIntervalInSeconds = env.get("UPDATE_INTERVAL_SECONDS");
        mciScheme = env.get("MCI_SCHEME");
        mciHost = env.get("MCI_HOST");
        mciPort = env.get("MCI_PORT");
        mciContextPath = env.get("MCI_CONTEXT_PATH");
        mciUser = env.get("MCI_USER");
        mciPassword = env.get("MCI_PASSWORD");
        cassandraHost = env.get("CASSANDRA_HOST");
        cassandraPort = env.get("CASSANDRA_PORT");
        cassandraKeySpace = env.get("CASSANDRA_KEYSPACE");
        cassandraTimeout = env.get("CASSANDRA_TIMEOUT");
    }
}
