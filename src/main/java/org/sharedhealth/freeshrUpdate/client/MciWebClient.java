package org.sharedhealth.freeshrUpdate.client;

import org.apache.log4j.Logger;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.identity.IdentityServiceClient;
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
    private Logger log = Logger.getLogger(MciWebClient.class);

    @Autowired
    public MciWebClient(IdentityServiceClient identityServiceClient, ShrUpdateConfiguration properties) {
        this.identityServiceClient = identityServiceClient;
        this.properties = properties;
    }

    public String get(URI url) throws URISyntaxException, IOException {
        log.debug("Reading from " + url);
        Map<String, String> headers = getMCIIdentityHeaders(identityServiceClient.getOrCreateToken(), properties);
        headers.put("Accept", "application/atom+xml");
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
