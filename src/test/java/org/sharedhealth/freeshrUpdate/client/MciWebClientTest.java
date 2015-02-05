package org.sharedhealth.freeshrUpdate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateProperties;
import org.sharedhealth.freeshrUpdate.identity.IdentityServiceClient;
import org.sharedhealth.freeshrUpdate.identity.IdentityToken;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MciWebClientTest {

    @Mock
    ShrUpdateProperties properties;

    @Mock
    IdentityServiceClient identityServiceClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldGetFeedFromMarkerOnwards() throws Exception {
        MciWebClient mciWebClient = new MciWebClient(properties, identityServiceClient);
        when(properties.getMciBaseUrl()).thenReturn("http://localhost:9997/api/v1/feed/patients");
        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml+atom")
                        .withBody("bar")));

        String mciUpdateFeedContent = mciWebClient.getMCIUpdateFeedContent("foo");
        assertEquals("bar", mciUpdateFeedContent);

    }

    @Test
    public void shouldGetFeedFromBeginningIfMarkerNotFound() throws Exception {
        MciWebClient mciWebClient = new MciWebClient(properties, identityServiceClient);
        when(properties.getMciBaseUrl()).thenReturn("http://localhost:9997/api/v1/feed/patients");
        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml+atom")
                        .withBody("bar")));

        givenThat(get(urlEqualTo("/api/v1/feed/patients"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml+atom")
                        .withBody("boom")));

        String mciUpdateFeedContent = mciWebClient.getMCIUpdateFeedContent("");
        assertEquals("boom", mciUpdateFeedContent);
    }
}