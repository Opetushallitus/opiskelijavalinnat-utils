package fi.vm.sade.javautils.http.auth;

import fi.vm.sade.authentication.cas.CasClient;
import fi.vm.sade.javautils.http.refactor.PERA;
import fi.vm.sade.javautils.http.refactor.ProxyAuthenticator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

@Slf4j
@Getter
@Setter
public class CasAuthenticator implements Authenticator {

    private static final String CAS_SECURITY_TICKET = "CasSecurityTicket";
    private ProxyAuthenticator proxyAuthenticator;

    private String webCasUrl;
    private String username;
    private String password;
    private String casServiceUrl;
    private String serviceAsAUserTicket;
    private String proxyAuthMode;

    private boolean useProxyAuthentication = false;

    public CasAuthenticator(Builder builder) {
        webCasUrl = builder.webCasUrl;
        setCasServiceUrl(builder.casServiceUrl);
        username = builder.username;
        password = builder.password;
    }

    @Override
    public synchronized boolean authenticate(final HttpRequestBase req) throws IOException {
        if (useServiceAsAUserAuthentication()) {
            if (serviceAsAUserTicket == null) {
                checkNotNull(getUsername(), "username");
                checkNotNull(getPassword(), "password");
                checkNotNull(getWebCasUrl(), "webCasUrl");
                checkNotNull(getCasServiceUrl(), "casService");
                serviceAsAUserTicket = obtainNewCasServiceAsAUserTicket();
                log.info("got new serviceAsAUser ticket, service: " + getCasServiceUrl() + ", ticket: " + getServiceAsAUserTicket());
            }
            req.setHeader(CAS_SECURITY_TICKET, serviceAsAUserTicket);
            PERA.setKayttajaHeaders(req, getCurrentUser(), getUsername());
            log.debug("set serviceAsAUser ticket to header, service: " + getCasServiceUrl() + ", ticket: " + getServiceAsAUserTicket() + ", currentUser: " + getCurrentUser() + ", callAsUser: " + getUsername());
            return true;
        } else if (useProxyAuthentication) {
            checkNotNull(getWebCasUrl(), "webCasUrl");
            checkNotNull(getCasServiceUrl(), "casService");
            if (proxyAuthenticator == null) {
                proxyAuthenticator = new ProxyAuthenticator();
            }
            final boolean[] gotNewProxyTicket = {false};
            proxyAuthenticator.proxyAuthenticate(getCasServiceUrl(), getProxyAuthMode(), new ProxyAuthenticator.Callback() {
                @Override
                public void setRequestHeader(String key, String value) {
                    req.setHeader(key, value);
                    log.debug("set http header: " + key + "=" + value);
                }

                @Override
                public void gotNewTicket(Authentication authentication, String proxyTicket) {
                    log.info("got new proxy ticket, service: " + getCasServiceUrl() + ", ticket: " + proxyTicket);
                    gotNewProxyTicket[0] = true;
                }
            });
            return gotNewProxyTicket[0];
        }

        return false;
    }

    @Override
    public String getUrlPrefix() {
        return getCasServiceUrl();
    }

    private void checkNotNull(String value, String name) {
        if (value == null) throw new NullPointerException("OphHttpClient." + name + " is null, and guess what, it shouldn't!");
    }

    private boolean useServiceAsAUserAuthentication() {
        return username != null;
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext() != null ? SecurityContextHolder.getContext().getAuthentication() : null;
        return authentication != null ? authentication.getName() : null;
    }

    private String obtainNewCasServiceAsAUserTicket() throws IOException {
        return CasClient.getTicket(webCasUrl + "/v1/tickets", username, password, getCasServiceUrl());
    }

    /** will force to get new ticket next time */
    public void clearTicket() {
        synchronized (this) {
            serviceAsAUserTicket = null;
            if (useProxyAuthentication && proxyAuthenticator != null) {
                proxyAuthenticator.clearTicket(getCasServiceUrl());
            }
        }
    }

    public void setWebCasUrl(String webCasUrl) {
        clearTicket();
        this.webCasUrl = webCasUrl;
    }

    public void setUsername(String username) {
        clearTicket();
        this.username = username;
    }

    public void setPassword(String password) {
        clearTicket();
        this.password = password;
    }

    public void setCasServiceUrl(String url) {
        //clearTicket();
        if (url != null) {
            url = url.replace("/j_spring_cas_security_check", "");
        }
        this.casServiceUrl = url;
    }

    public static class Builder{
        String webCasUrl;
        String username;
        String password;
        String casServiceUrl;

        String proxyAuthMode;

        public Builder() {
            proxyAuthMode = "dev";
        }

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
            this.casServiceUrl = url;
            return this;
        }

        public CasAuthenticator build() {
            return new CasAuthenticator(this);
        }
    }
}
