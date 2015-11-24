package org.sharedhealth.freeshrUpdate.client;

import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.identity.IdentityServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.sharedhealth.freeshrUpdate.utils.Headers.getMCIIdentityHeaders;

@Component
public class MciWebClient {
    private IdentityServiceClient identityServiceClient;
    private ShrUpdateConfiguration properties;
    private Logger log = LoggerFactory.getLogger(MciWebClient.class);

    @Autowired
    public MciWebClient(IdentityServiceClient identityServiceClient, ShrUpdateConfiguration properties) {
        this.identityServiceClient = identityServiceClient;
        this.properties = properties;
    }

    public String getFeed(URI url) throws IOException {
        return getResponse(url, "application/atom+xml");
    }

    private String getResponse(URI url, String contentType) throws IOException {
        log.debug("Reading from " + url);
        Map<String, String> headers = getMCIIdentityHeaders(identityServiceClient.getOrCreateToken(), properties);
        headers.put("Accept", contentType);
        String response = null;
        try {
            response = new WebClient().get(url, headers);
        } catch (ConnectionException e) {
            log.error(String.format("Could not fetch. Exception: %s", e));
            if (e.getErrorCode() == 401) {
                log.error("Unauthorized, clearing token.");
                identityServiceClient.clearToken();
            }else{
                throw new RuntimeException(e);
            }
        }
        return response;
    }

    public String getPatient(String healthId) throws IOException, URISyntaxException {
        return getResponse(properties.getMciPatientPath(healthId), "application/json");
    }


}
