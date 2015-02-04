package org.sharedhealth.freeshrUpdate.config;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SHRUpdateConfig {
    private String mciSchema;
    private String mciHost;
    private String mciPort;
    private String mciUser;
    private String mciPassword;
    private String cassandraKeySpace;
    private String cassandraHost;
    private String cassandraPort;
    private String cassandraTimeout;
    private String updateIntervalInSeconds;

    public String getMciSchema() {
        return mciSchema;
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

    public SHRUpdateConfig() {
        Map<String, String> env = System.getenv();
        updateIntervalInSeconds = env.get("UPDATE_INTERVAL_SECONDS");
    }
}
