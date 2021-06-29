package fi.vm.sade.javautils.kayttooikeusclient;

import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.javautils.http.OphHttpResponse;
import fi.vm.sade.javautils.http.OphHttpResponseHandler;
import fi.vm.sade.properties.OphProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.function.Function;

import static fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl.USERDETAILS_URL_KEY;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OphUserDetailsServiceImplTest {

    private static final String USERNAME = "testuser";
    private final OphHttpClient httpClient = mock(OphHttpClient.class);
    private final OphProperties properties = mock(OphProperties.class);
    private final OphUserDetailsServiceImpl userDetailsService = new OphUserDetailsServiceImpl(httpClient, properties);

    @Test
    @SuppressWarnings("unchecked")
    public void heittaaUsernameNotFoundExceptioninKunKayttajaaEiLoydy() {
        when(properties.url(USERDETAILS_URL_KEY, USERNAME)).thenReturn("testurl");
        OphHttpResponse<UserDetails> response = (OphHttpResponse<UserDetails>) mock(OphHttpResponse.class);
        OphHttpResponseHandler<UserDetails> handler = (OphHttpResponseHandler<UserDetails>) mock(OphHttpResponseHandler.class);
        when(response.expectedStatus(200)).thenReturn(handler);
        when(handler.mapWith(any(Function.class))).thenReturn(Optional.empty());
        when(httpClient.<UserDetails>execute(any(OphHttpRequest.class))).thenReturn(response);
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(USERNAME));
    }

}
