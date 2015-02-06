package org.sharedhealth.freeshrUpdate.client;

import org.apache.log4j.Logger;
import org.sharedhealth.freeshrUpdate.identity.IdentityServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Component
public class MciWebClient {
    private IdentityServiceClient identityServiceClient;
    private Logger log = Logger.getLogger(MciWebClient.class);

    @Autowired
    public MciWebClient(IdentityServiceClient identityServiceClient) {
        this.identityServiceClient = identityServiceClient;
    }

    public String get(URI url) throws URISyntaxException, IOException {
        log.debug("Reading from " + url);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/atom+xml");
        headers.put("X-Auth-Token", identityServiceClient.getOrCreateToken().toString());
        String response = null;
        try {
            response = new WebClient().get(url, headers);
        } catch (ConnectionException e) {
            log.error(String.format("Could not fetch. Exception: %s", e));
            if (e.getErrorCode() == 401) {
                log.error("Unauthorized, clearing token.");
                identityServiceClient.clearToken();
            }
        }
        return response;
    }

}
