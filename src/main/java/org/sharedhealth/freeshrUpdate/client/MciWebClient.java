package org.sharedhealth.freeshrUpdate.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateProperties;
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

    private ShrUpdateProperties properties;
    private IdentityServiceClient identityServiceClient;
    private Logger log = Logger.getLogger(MciWebClient.class);

    @Autowired
    public MciWebClient(ShrUpdateProperties properties, IdentityServiceClient identityServiceClient) {
        this.properties = properties;
        this.identityServiceClient = identityServiceClient;
    }

    public String getMCIUpdateFeedContent(final String marker) throws URISyntaxException, IOException {
        URI mciURI = getMciURI(marker);
        log.debug("Reading from " + mciURI);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/atom+xml");
        headers.put("X-Auth-Token", identityServiceClient.getOrCreateToken().toString());
        String response = null;
        try {
            response = new WebClient().get(mciURI, headers);
        } catch (ConnectionException e) {
            log.error(String.format("Could not fetch MCI patient updates after marker: [%s]", marker), e);
            if (e.getErrorCode() == 401) {
                identityServiceClient.clearToken();
            }
        }
        return response;
    }

    private URI getMciURI(String marker) throws URISyntaxException {
        String mciBaseUrl = properties.getMciBaseUrl();
        if (StringUtils.isBlank(marker)) return new URI(mciBaseUrl);
        return new URI(mciBaseUrl + "?last_marker=" + marker);
    }
}
