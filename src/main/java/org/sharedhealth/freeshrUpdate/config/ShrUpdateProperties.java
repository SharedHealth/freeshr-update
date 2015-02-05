package org.sharedhealth.freeshrUpdate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ShrUpdateProperties {
    public static final String MCI_MARKER = "MCI_MARKER";

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
    private String cassandraPort;

    @Value("${CASSANDRA_TIMEOUT}")
    private String cassandraTimeout;

    @Value("${UPDATE_INTERVAL_SECONDS}")
    private String updateIntervalInSeconds;

    @Value("${IDENTITY_SERVER_BASE_URL}")
    private String identityServerBaseUrl;

    @Value("${MARKER_FILE}")
    private String markerFilePath;



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

    public String getMarkerFilePath() {
        return markerFilePath;
    }

}
