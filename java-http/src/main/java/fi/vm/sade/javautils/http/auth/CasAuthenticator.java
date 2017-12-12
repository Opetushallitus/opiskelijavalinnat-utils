package fi.vm.sade.javautils.http.auth;

import fi.vm.sade.authentication.cas.CasClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpUriRequest;

@Slf4j
@Getter
@Setter
public class CasAuthenticator implements Authenticator {

    private static final String CAS_SECURITY_TICKET = "CasSecurityTicket";
    // PERA
    public static final String X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS = "X-Kutsuketju.Aloittaja.KayttajaTunnus";
    public static final String X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS = "X-Palvelukutsu.Lahettaja.KayttajaTunnus";

    private String webCasUrl;
    private String username;
    private String password;
    private String casServiceUrl;
    private String serviceAsAUserTicket;

    public CasAuthenticator(Builder builder) {
        webCasUrl = builder.webCasUrl;
        setCasServiceUrl(builder.casServiceUrl);
        username = builder.username;
        password = builder.password;
    }

    @Override
    public void clearSession() {
        serviceAsAUserTicket = null;
    }

    @Override
    public synchronized boolean authenticate(final HttpUriRequest req) {
        if (serviceAsAUserTicket == null) {
            checkNotNull(getUsername(), "username");
            checkNotNull(getPassword(), "password");
            checkNotNull(getWebCasUrl(), "webCasUrl");
            checkNotNull(getCasServiceUrl(), "casService");
            serviceAsAUserTicket = obtainNewCasServiceAsAUserTicket();
            log.info("got new serviceAsAUser ticket, service: " + getCasServiceUrl() + ", ticket: " + getServiceAsAUserTicket());
        }
        req.setHeader(CAS_SECURITY_TICKET, serviceAsAUserTicket);
        setKayttajaHeaders(req, getUsername(), getUsername());
        log.debug("set serviceAsAUser ticket to header, service: " + getCasServiceUrl() + ", ticket: " + getServiceAsAUserTicket() + ", currentUser: " + getUsername() + ", callAsUser: " + getUsername());
        return true;
    }

    @Override
    public String getUrlPrefix() {
        return getCasServiceUrl();
    }

    private void checkNotNull(String value, String name) {
        if (value == null) throw new NullPointerException(String.format("CasAuthenticator.%s is null, and guess what, it shouldn't!", name));
    }

    private String obtainNewCasServiceAsAUserTicket() {
        return CasClient.getTicket(webCasUrl + "/v1/tickets", username, password, getCasServiceUrl());
    }

    private static void setKayttajaHeaders(HttpUriRequest req, String currentUser, String callAsUser) {
        req.setHeader(X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS, currentUser);
        req.setHeader(X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS, callAsUser);
    }

    public static class Builder{
        String webCasUrl;
        String username;
        String password;
        String casServiceUrl;

        public Builder() {}

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder webCasUrl(String casUrl) {
            this.webCasUrl = casUrl;
            return this;
        }

        public Builder casServiceUrl(String url) {
            if (url != null) {
                url = url.replace("/j_spring_cas_security_check", "");
            }
            this.casServiceUrl = url;
            return this;
        }

        public CasAuthenticator build() {
            return new CasAuthenticator(this);
        }
    }
}
