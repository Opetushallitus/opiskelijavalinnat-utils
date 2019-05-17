package fi.vm.sade.suomifi.valtuudet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.javautils.httpclient.OphHttpClient;
import fi.vm.sade.javautils.httpclient.apache.ApacheOphHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static fi.vm.sade.suomifi.valtuudet.TestResourceHelper.loadAsString;
import static net.jadler.Jadler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasItem;

public class ValtuudetClientTest {

    private static final String TEST_HETU = "080297-915A";
    private static final String TEST_CLIENT_ID = "ed4b7ae7";
    private static final String TEST_API_KEY = "3ba56df8-88b8-4805-9b04-2f8e7a61";
    private static final String TEST_OAUTH_PASSWORD = "abc-api-key";
    private static final String TEST_REQUEST_ID = "02fd35dc-99e6-477b-b6e2-03f02cbf3666";
    private static final Instant TEST_INSTANT = Instant.parse("2017-02-09T10:29:42.09Z");
    private static final String TEST_CHECKSUM_PREFIX = "ed4b7ae7 2017-02-09T10:29:42.09Z";
    private static final String TEST_CHECKSUM_HPA = String.format("%s z7X+xWtrvth1L7Ql6B/4xZ0iQ1VjToWX4TnHVLo8RGo=", TEST_CHECKSUM_PREFIX);
    private static final String TEST_CHECKSUM_YPA = String.format("%s z1xDqaSjDwnVniJvis0SqMXeQEBtgBgzozhQqftmY9U=", TEST_CHECKSUM_PREFIX);
    private static final String TEST_AUTHORIZATION_HEADER = "Basic ZWQ0YjdhZTc6YWJjLWFwaS1rZXk=";

    private ValtuudetClient client;

    @Before
    public void setup() {
        initJadler();

        OphHttpClient httpClient = ApacheOphHttpClient.createDefaultOphClient("test", null);
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ValtuudetProperties properties = ValtuudetPropertiesImpl.builder()
                .host("http://localhost:" + port())
                .clientId(TEST_CLIENT_ID)
                .apiKey(TEST_API_KEY)
                .oauthPassword(TEST_OAUTH_PASSWORD)
                .build();
        client = new ValtuudetClientImpl(httpClient, objectMapper::readValue, properties,
                () -> TEST_REQUEST_ID, () -> TEST_INSTANT);
    }

    @After
    public void cleanup() {
        closeJadler();
    }

    @Test
    public void hpa() {
        String callbackUrl = "http://localhost:8080/callback?parameter=1&another=2";
        String testCode = "code123";
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(String.format("/service/hpa/user/register/%s/%s", TEST_CLIENT_ID, TEST_HETU))
                .havingQueryStringEqualTo(String.format("requestId=%s", TEST_REQUEST_ID))
                .havingHeaderEqualTo("X-AsiointivaltuudetAuthorization", TEST_CHECKSUM_HPA)
                .respond()
                .withStatus(200)
                .withBody(loadAsString("session.json"));
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/oauth/token")
                .havingQueryStringEqualTo(String.format("grant_type=authorization_code&redirect_uri=%s&code=%s", "http%3A%2F%2Flocalhost%3A8080%2Fcallback%3Fparameter%3D1%26another%3D2", testCode))
                .havingHeaderEqualTo("Authorization", TEST_AUTHORIZATION_HEADER)
                .respond()
                .withStatus(200)
                .withBody(loadAsString("token.json"));
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/service/hpa/api/delegate/sessionId123")
                .havingQueryStringEqualTo(String.format("requestId=%s", TEST_REQUEST_ID))
                .havingHeader("X-AsiointivaltuudetAuthorization", hasItem(startsWith(TEST_CHECKSUM_PREFIX)))
                .havingHeaderEqualTo("Authorization", "Bearer accessToken123")
                .respond()
                .withStatus(200)
                .withBody(loadAsString("people.json"));
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/service/hpa/api/authorization/sessionId123/120508A950F")
                .havingQueryStringEqualTo(String.format("requestId=%s", TEST_REQUEST_ID))
                .havingHeader("X-AsiointivaltuudetAuthorization", hasItem(startsWith(TEST_CHECKSUM_PREFIX)))
                .havingHeaderEqualTo("Authorization", "Bearer accessToken123")
                .respond()
                .withStatus(200)
                .withBody(loadAsString("authorization-allowed.json"));

        SessionDto session = client.createSession(ValtuudetType.PERSON, TEST_HETU);
        String redirectUrl = client.getRedirectUrl(session.userId, callbackUrl, "fi");
        String accessToken = client.getAccessToken(testCode, callbackUrl);
        PersonDto person = client.getSelectedPerson(session.sessionId, accessToken);
        boolean authorized = client.isAuthorizedToPerson(session.sessionId, accessToken, person.personId);

        assertThat(redirectUrl).endsWith("oauth/authorize?client_id=" + TEST_CLIENT_ID + "&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcallback%3Fparameter%3D1%26another%3D2&user=userId123&lang=fi");
        assertThat(person).returns("120508A950F", t -> t.personId);
        assertThat(authorized).isTrue();
    }

    @Test
    public void ypa() {
        String callbackUrl = "http://localhost:8080/callback?parameter=1&another=2";
        String testCode = "code123";
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(String.format("/service/ypa/user/register/%s/%s", TEST_CLIENT_ID, TEST_HETU))
                .havingQueryStringEqualTo(String.format("requestId=%s", TEST_REQUEST_ID))
                .havingHeaderEqualTo("X-AsiointivaltuudetAuthorization", TEST_CHECKSUM_YPA)
                .respond()
                .withStatus(200)
                .withBody(loadAsString("session.json"));
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo("/oauth/token")
                .havingQueryStringEqualTo(String.format("grant_type=authorization_code&redirect_uri=%s&code=%s", "http%3A%2F%2Flocalhost%3A8080%2Fcallback%3Fparameter%3D1%26another%3D2", testCode))
                .havingHeaderEqualTo("Authorization", TEST_AUTHORIZATION_HEADER)
                .respond()
                .withStatus(200)
                .withBody(loadAsString("token.json"));
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/service/ypa/api/organizationRoles/sessionId123")
                .havingQueryStringEqualTo(String.format("requestId=%s", TEST_REQUEST_ID))
                .havingHeader("X-AsiointivaltuudetAuthorization", hasItem(startsWith(TEST_CHECKSUM_PREFIX)))
                .havingHeaderEqualTo("Authorization", "Bearer accessToken123")
                .respond()
                .withStatus(200)
                .withBody(loadAsString("organisations.json"));

        SessionDto session = client.createSession(ValtuudetType.ORGANISATION, TEST_HETU);
        String redirectUrl = client.getRedirectUrl(session.userId, callbackUrl, "fi");
        String accessToken = client.getAccessToken(testCode, callbackUrl);
        OrganisationDto organisation = client.getSelectedOrganisation(session.sessionId, accessToken);

        assertThat(redirectUrl).endsWith("oauth/authorize?client_id=" + TEST_CLIENT_ID + "&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcallback%3Fparameter%3D1%26another%3D2&user=userId123&lang=fi");
        assertThat(organisation).returns("2305162-8", t -> t.identifier);
    }

}
