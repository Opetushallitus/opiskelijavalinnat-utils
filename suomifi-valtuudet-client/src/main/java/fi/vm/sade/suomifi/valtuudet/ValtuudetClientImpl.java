package fi.vm.sade.suomifi.valtuudet;

import fi.vm.sade.javautils.httpclient.OphHttpClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

public class ValtuudetClientImpl implements ValtuudetClient {

    private static final String AUTHORIZATION_HEADER = "X-AsiointivaltuudetAuthorization";
    private final static String MAC_ALGORITHM = "HmacSHA256";

    private final OphHttpClient httpClient;
    private final JsonDeserializer jsonDeserializer;
    private final ValtuudetProperties properties;
    private final Supplier<String> requestIdProvider;
    private final Supplier<Instant> instantProvider;

    public ValtuudetClientImpl(OphHttpClient httpClient, JsonDeserializer jsonDeserializer,
                               ValtuudetProperties properties) {
        this(httpClient, jsonDeserializer, properties, () -> UUID.randomUUID().toString(), () -> Instant.now());
    }

    protected ValtuudetClientImpl(OphHttpClient httpClient, JsonDeserializer jsonDeserializer,
                                  ValtuudetProperties properties,
                                  Supplier<String> requestIdProvider, Supplier<Instant> instantProvider) {
        this.httpClient = httpClient;
        this.jsonDeserializer = jsonDeserializer;
        this.properties = properties;
        this.requestIdProvider = requestIdProvider;
        this.instantProvider = instantProvider;
    }

    public SessionDto createSession(ValtuudetType type, String nationalIdentificationNumber) {
        String path = String.format("/service/%s/user/register/%s/%s?requestId=%s",
                type.path, properties.getClientId(), nationalIdentificationNumber,
                encodeQueryParam(requestIdProvider.get()));
        String url = properties.getHost() + path;

        SessionDto session = httpClient.get(url)
                .header(AUTHORIZATION_HEADER, getChecksum(path, instantProvider.get()))
                .doNotSendOphHeaders()
                .expectStatus(200)
                .execute(response -> jsonDeserializer.deserialize(response.asText(), SessionDto.class));
        assert session.sessionId != null;
        assert session.userId != null;
        return session;
    }

    public String getRedirectUrl(String userId, String callbackUrl, String language) {
        String path = String.format("/oauth/authorize?client_id=%s&response_type=code&redirect_uri=%s&user=%s&lang=%s",
                encodeQueryParam(properties.getClientId()), encodeQueryParam(callbackUrl),
                encodeQueryParam(userId), encodeQueryParam(language));
        return properties.getHost() + path;
    }

    public String getAccessToken(String code, String callbackUrl) {
        String path = String.format("/oauth/token?grant_type=authorization_code&redirect_uri=%s&code=%s",
                encodeQueryParam(callbackUrl), encodeQueryParam(code));
        String url = properties.getHost() + path;

        TokenDto token = httpClient.post(url)
                .header("Authorization", "Basic " + getCredentials())
                .doNotSendOphHeaders()
                .expectStatus(200)
                .execute(response -> jsonDeserializer.deserialize(response.asText(), TokenDto.class));
        assert token != null;
        String accessToken = token.access_token;
        assert accessToken != null;
        return accessToken;
    }

    public PersonDto getSelectedPerson(String sessionId, String accessToken) {
        String path = String.format("/service/hpa/api/delegate/%s?requestId=%s",
                sessionId, encodeQueryParam(requestIdProvider.get()));
        String url = properties.getHost() + path;

        PersonDto person = httpClient.get(url)
                .header("Authorization", "Bearer " + accessToken)
                .header(AUTHORIZATION_HEADER, getChecksum(path, instantProvider.get()))
                .doNotSendOphHeaders()
                .expectStatus(200)
                .execute(response -> jsonDeserializer.deserialize(response.asText(), PersonDto[].class))[0];
        assert person != null;
        assert person.personId != null;
        return person;
    }

    private AuthorizationDto getAuthorizationToPerson(String sessionId, String accessToken, String nationalIdentificationNumber) {
        String path = String.format("/service/hpa/api/authorization/%s/%s?requestId=%s",
                sessionId, nationalIdentificationNumber, encodeQueryParam(requestIdProvider.get()));
        String url = properties.getHost() + path;

        AuthorizationDto authorization = httpClient.get(url)
                .header("Authorization", "Bearer " + accessToken)
                .header(AUTHORIZATION_HEADER, getChecksum(path, instantProvider.get()))
                .doNotSendOphHeaders()
                .expectStatus(200)
                .execute(response -> jsonDeserializer.deserialize(response.asText(), AuthorizationDto.class));
        assert authorization != null;
        assert authorization.result != null;
        return authorization;
    }

    public boolean isAuthorizedToPerson(String sessionId, String accessToken, String nationalIdentificationNumber) {
        AuthorizationDto authorization = getAuthorizationToPerson(sessionId, accessToken, nationalIdentificationNumber);
        return "ALLOWED".equals(authorization.result);
    }

    public OrganisationDto getSelectedOrganisation(String sessionId, String accessToken) {
        String path = String.format("/service/ypa/api/organizationRoles/%s?requestId=%s",
                sessionId, encodeQueryParam(requestIdProvider.get()));
        String url = properties.getHost() + path;

        OrganisationDto organisation = httpClient.get(url)
                .header("Authorization", "Bearer " + accessToken)
                .header(AUTHORIZATION_HEADER, getChecksum(path, instantProvider.get()))
                .doNotSendOphHeaders()
                .expectStatus(200)
                .execute(response -> jsonDeserializer.deserialize(response.asText(), OrganisationDto[].class))[0];
        assert organisation != null;
        assert organisation.identifier != null;
        return organisation;
    }

    private String getCredentials() {
        String credentials = properties.getClientId() + ":" + properties.getOauthPassword();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String getChecksum(String path, Instant instant) {
        String timestamp = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
        return properties.getClientId() + " " + timestamp + " " + hash(path + " " + timestamp, properties.getApiKey());
    }

    private String hash(String data, String key) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));
            return new String(Base64.getEncoder().encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
