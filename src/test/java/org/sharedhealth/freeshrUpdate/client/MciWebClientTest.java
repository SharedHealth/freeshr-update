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

public class MciWebClientTest {

    @Mock
    ShrUpdateConfiguration properties;

    @Mock
    IdentityServiceClient identityServiceClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldClearIdentityTokenIfUnauthorized() throws Exception {
        MciWebClient mciWebClient = new MciWebClient(identityServiceClient);
        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .withHeader("X-Auth-Token", equalTo("baz"))
                .withHeader("Accept", equalTo("application/atom+xml"))
                .willReturn(aResponse()
                        .withStatus(401)));

        mciWebClient.get(URI.create
                ("http://localhost:9997/api/v1/feed/patients?last_marker=foo"));
        verify(identityServiceClient, times(1)).clearToken();
    }

    @Test
    public void shouldGetResponse() throws Exception {
        MciWebClient mciWebClient = new MciWebClient(identityServiceClient);
        when(identityServiceClient.getOrCreateToken()).thenReturn(new IdentityToken("baz"));
        String body = asString("feeds/patientUpdatesFeed.xml");
        givenThat(get(urlEqualTo("/api/v1/feed/patients?last_marker=foo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)));

        String response = mciWebClient.get(URI.create
                ("http://localhost:9997/api/v1/feed/patients?last_marker=foo"));
        assertNotNull(response);
        assertEquals(response, body);
    }
}