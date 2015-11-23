package org.sharedhealth.freeshrUpdate.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;
import org.sharedhealth.freeshrUpdate.identity.IdentityServiceClient;
import org.sharedhealth.freeshrUpdate.identity.IdentityToken;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.helpers.ResourceHelper.asString;
import static org.sharedhealth.freeshrUpdate.utils.Headers.*;

public class MciWebClientTest {

    @Mock
    IdentityServiceClient identityServiceClient;

    @Mock
    private ShrUpdateConfiguration properties;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);
    private MciWebClient mciWebClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mciWebClient = new MciWebClient(identityServiceClient, properties);
    }

    @Test
    public void shouldGetResponse() throws Exception {
        String clientId = "12345";
        String clientEmail = "email@gmail.com";
        String body = asString("feeds/patientUpdatesFeed.xml");

        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        when(properties.getIdpClientId()).thenReturn(clientId);
        when(properties.getIdpClientEmail()).thenReturn(clientEmail);

        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .withHeader("Accept", equalTo("application/atom+xml"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo("baz"))
                .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                .withHeader(FROM_KEY, equalTo(clientEmail))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)));

        String response = mciWebClient.get(URI.create("http://localhost:9997/api/v1/feed/patients?last_marker=foo"));
        assertNotNull(response);
        assertEquals(response, body);
    }

    @Test
    public void shouldClearIdentityTokenIfUnauthorized() throws Exception {
        String clientId = "12345";
        String clientEmail = "email@gmail.com";

        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        when(properties.getIdpClientId()).thenReturn(clientId);
        when(properties.getIdpClientEmail()).thenReturn(clientEmail);

        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .withHeader("Accept", equalTo("application/atom+xml"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo("baz"))
                .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                .withHeader(FROM_KEY, equalTo(clientEmail))
                .willReturn(aResponse()
                        .withStatus(401)));

        mciWebClient.get(URI.create
                ("http://localhost:9997/api/v1/feed/patients?last_marker=foo"));

        verify(identityServiceClient, times(1)).clearToken();
    }
}