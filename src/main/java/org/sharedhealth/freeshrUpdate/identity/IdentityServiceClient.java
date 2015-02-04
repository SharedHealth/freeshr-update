package org.sharedhealth.freeshrUpdate.identity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sharedhealth.freeshrUpdate.client.WebClient;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class IdentityServiceClient {
    private ShrUpdateProperties properties;
    private IdentityStore identityStore;

    @Autowired
    public IdentityServiceClient(ShrUpdateProperties properties, IdentityStore identityStore) {
        this.properties = properties;
        this.identityStore = identityStore;
    }

    public IdentityToken getOrCreateToken() throws IOException {
        IdentityToken token = identityStore.getToken();
        if (token == null) {
            Identity identity = new Identity(properties.getMciUser(), properties.getMciPassword());
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("accept", "application/json");
            String response = new WebClient().post(getIdentityServerUrl(), identity, headers);
            token = readFrom(response, IdentityToken.class);
            identityStore.setToken(token);
        }
        return token;
    }


    private static <T> T readFrom(String content, Class<T> returnType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(content, returnType);

    }

    public void clearToken() {
        identityStore.clearToken();
    }

    private String getIdentityServerUrl() {
        return properties.getIdentityServerBaseUrl() + "/login";
    }
}
