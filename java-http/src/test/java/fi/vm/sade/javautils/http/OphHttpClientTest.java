package fi.vm.sade.javautils.http;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import fi.vm.sade.javautils.http.auth.CasAuthenticator;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OphHttpClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void authenticatesWithCas() {
        stubFor(post("/v1/tickets")
            .willReturn(created()
                .withHeader("Location", "http://localhost:8089/cas/login/TGT-123452-123412-34")));
        stubFor(post("/v1/tickets/TGT-123452-123412-34")
            .willReturn(ok()
                .withBody("ST-123155124-134252345-3245345")));
        stubFor(get("/requestpath")
            .willReturn(ok()
                .withBody("responsee")));

        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username("casuser")
                .password("cassword")
                .webCasUrl("http://localhost:8089")
                .casServiceUrl("http://myservice")
                .build();

        OphHttpClient client = new OphHttpClient.Builder("callerid").authenticator(authenticator).build();
        OphHttpRequest request = OphHttpRequest.Builder.get("http://localhost:8089/requestpath").build();
        String response = client.<String>execute(request)
                .expectedStatus(200)
                .mapWith(body -> body)
                .orElseThrow();

        verify(postRequestedFor(urlEqualTo("/v1/tickets"))
            .withHeader("Content-Type", matching("application/x-www-form-urlencoded; charset=UTF-8"))
            .withHeader("Caller-id", matching("CasClient"))
            .withHeader("CSRF", matching("CSRF"))
            .withHeader("Cookie", matching("CSRF=CSRF"))
            .withRequestBody(matching("username=casuser&password=cassword")));
        verify(postRequestedFor(urlEqualTo("/v1/tickets/TGT-123452-123412-34"))
            .withHeader("Caller-id", matching("CasClient"))
            .withHeader("CSRF", matching("CSRF"))
            .withHeader("Cookie", matching("CSRF=CSRF"))
            .withHeader("Content-Type", matching("application/x-www-form-urlencoded; charset=UTF-8"))
            .withRequestBody(matching("service=http%3A%2F%2Fmyservice%2Fj_spring_cas_security_check")));
        verify(getRequestedFor(urlEqualTo("/requestpath"))
            .withHeader("Caller-id", matching("callerid"))
            .withHeader("CSRF", matching("CachingRestClient"))
            .withHeader("Cookie", matching("CSRF=CachingRestClient"))
            .withHeader("CasSecurityTicket", matching("ST-123155124-134252345-3245345")));
        assertEquals("responsee", response);
    }
}
