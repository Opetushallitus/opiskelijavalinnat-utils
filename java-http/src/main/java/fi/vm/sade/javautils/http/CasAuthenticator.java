package fi.vm.sade.javautils.http;

import fi.vm.sade.authentication.cas.CasClient;
import fi.vm.sade.javautils.http.auth.PERA;
import fi.vm.sade.javautils.http.auth.ProxyAuthenticator;
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
public class CasAuthenticator {

    private static final String CAS_SECURITY_TICKET = "CasSecurityTicket";
    private ProxyAuthenticator proxyAuthenticator;

    private String webCasUrl;
    private String username;
    private String password;
    private String casService;
    private String serviceAsAUserTicket;
    private String proxyAuthMode;

    private boolean useProxyAuthentication = false;

    public CasAuthenticator(Builder builder) {
        webCasUrl = builder.webCasUrl;
        casService = builder.casService;
        username = builder.username;
        password = builder.password;
    }

    protected synchronized boolean authenticate(final HttpRequestBase req) throws IOException {
        if (useServiceAsAUserAuthentication()) {
            if (serviceAsAUserTicket == null) {
                checkNotNull(getUsername(), "username");
                checkNotNull(getPassword(), "password");
                checkNotNull(getWebCasUrl(), "webCasUrl");
                checkNotNull(getCasService(), "casService");
                serviceAsAUserTicket = obtainNewCasServiceAsAUserTicket();
                log.info("got new serviceAsAUser ticket, service: " + getCasService() + ", ticket: " + getServiceAsAUserTicket());
            }
            req.setHeader(CAS_SECURITY_TICKET, serviceAsAUserTicket);
            PERA.setKayttajaHeaders(req, getCurrentUser(), getUsername());
            log.debug("set serviceAsAUser ticket to header, service: " + getCasService() + ", ticket: " + getServiceAsAUserTicket() + ", currentUser: " + getCurrentUser() + ", callAsUser: " + getUsername());
            return true;
        } else if (useProxyAuthentication) {
            checkNotNull(getWebCasUrl(), "webCasUrl");
            checkNotNull(getCasService(), "casService");
            if (proxyAuthenticator == null) {
                proxyAuthenticator = new ProxyAuthenticator();
            }
            final boolean[] gotNewProxyTicket = {false};
            proxyAuthenticator.proxyAuthenticate(getCasService(), getProxyAuthMode(), new ProxyAuthenticator.Callback() {
                @Override
                public void setRequestHeader(String key, String value) {
                    req.setHeader(key, value);
                    log.debug("set http header: " + key + "=" + value);
                }

                @Override
                public void gotNewTicket(Authentication authentication, String proxyTicket) {
                    log.info("got new proxy ticket, service: " + casService + ", ticket: " + proxyTicket);
                    gotNewProxyTicket[0] = true;
                }
            });
            return gotNewProxyTicket[0];
        }

        return false;
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
        return CasClient.getTicket(webCasUrl + "/v1/tickets", username, password, casService);
    }

    /** will force to get new ticket next time */
    public void clearTicket() {
        synchronized (this) {
            serviceAsAUserTicket = null;
            if (useProxyAuthentication && proxyAuthenticator != null) {
                proxyAuthenticator.clearTicket(casService);
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

    public void setCasService(String casService) {
        clearTicket();
        this.casService = casService;
    }

    public static class Builder{
        String webCasUrl;
        String username;
        String password;
        String casService;

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

        public Builder casService(String casService) {
            this.casService = casService;
            return this;
        }

        public CasAuthenticator build() {
            return new CasAuthenticator(this);
        }
    }
}
