package org.sharedhealth.freeshrUpdate.identity;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.freeshrUpdate.config.ShrUpdateConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.freeshrUpdate.utils.Headers.CLIENT_ID_KEY;
import static org.sharedhealth.freeshrUpdate.utils.Headers.X_AUTH_TOKEN_KEY;

public class IdentityServiceClientTest {
    @Mock
    private ShrUpdateConfiguration properties;
    @Mock
    private IdentityStore identityStore;
    private IdentityServiceClient identityServiceClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        identityServiceClient = new IdentityServiceClient(properties, identityStore);
    }

    @Test
    public void shouldGetTheTokenFromIdentityStore() throws Exception {
        IdentityToken identityToken = new IdentityToken("xyz");
        when(identityStore.getToken()).thenReturn(identityToken);
        IdentityToken token = identityServiceClient.getOrCreateToken();
        assertEquals(token, identityToken);
        verify(identityStore, never()).setToken(any(IdentityToken.class));
    }

    @Test
    public void shouldCreateTheTokenWhenNotFoundInIdentityStore() throws Exception {
        String clientId = "12345";
        String clientAuthToken = "abc";
        IdentityToken identityToken = new IdentityToken("xyz");
        String response = "{\"access_token\" : \"" + identityToken.toString() + "\"}";

        when(identityStore.getToken()).thenReturn(null);
        when(properties.getIdpServerSigninUrl()).thenReturn("http://localhost:9997/signin");
        when(properties.getIdpClientAuthToken()).thenReturn(clientAuthToken);
        when(properties.getIdpClientId()).thenReturn(clientId);

        givenThat(post(urlEqualTo("/signin"))
                        .withHeader("accept", equalTo("application/json"))
                        .withHeader(CLIENT_ID_KEY, equalTo(clientId))
                        .withHeader(X_AUTH_TOKEN_KEY, equalTo(clientAuthToken))
                        .willReturn(aResponse()
                                        .withStatus(HttpStatus.SC_OK)
                                        .withBody(response)
                        )
        );

        IdentityToken token = identityServiceClient.getOrCreateToken();
        assertEquals(token.toString(), identityToken.toString());
        verify(identityStore, times(1)).setToken(token);
    }
}