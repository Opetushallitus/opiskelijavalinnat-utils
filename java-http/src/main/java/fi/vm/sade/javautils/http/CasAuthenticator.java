package fi.vm.sade.javautils.http;

import fi.vm.sade.authentication.cas.CasClient;
import fi.vm.sade.javautils.http.auth.PERA;
import fi.vm.sade.javautils.http.auth.ProxyAuthenticator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

@Slf4j
public class CasAuthenticator {

    private static final String CAS_SECURITY_TICKET = "CasSecurityTicket";

    // @Value("${auth.mode:cas}")
    private String proxyAuthMode = "dev"; // FIXME
    private ProxyAuthenticator proxyAuthenticator;

    private String webCasUrl;
    private String username;
    private String password;
    @Getter
    private String casService;
    private String serviceAsAUserTicket;

    private boolean useProxyAuthentication = false;

    public CasAuthenticator() {
    }

    protected synchronized boolean authenticate(final HttpRequestBase req) throws IOException {
        if (useServiceAsAUserAuthentication()) {
            if (serviceAsAUserTicket == null) {
                checkNotNull(username, "username");
                checkNotNull(password, "password");
                checkNotNull(webCasUrl, "webCasUrl");
                checkNotNull(casService, "casService");
                serviceAsAUserTicket = obtainNewCasServiceAsAUserTicket();
                log.info("got new serviceAsAUser ticket, service: " + casService + ", ticket: " + serviceAsAUserTicket);
            }
            req.setHeader(CAS_SECURITY_TICKET, serviceAsAUserTicket);
            PERA.setKayttajaHeaders(req, getCurrentUser(), username);
            log.debug("set serviceAsAUser ticket to header, service: " + casService + ", ticket: " + serviceAsAUserTicket + ", currentUser: " + getCurrentUser() + ", callAsUser: " + username);
            return true;
        } else if (useProxyAuthentication) {
            checkNotNull(webCasUrl, "webCasUrl");
            checkNotNull(casService, "casService");
            if (proxyAuthenticator == null) {
                proxyAuthenticator = new ProxyAuthenticator();
            }
            final boolean[] gotNewProxyTicket = {false};
            proxyAuthenticator.proxyAuthenticate(casService, proxyAuthMode, new ProxyAuthenticator.Callback() {
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
        if (value == null) throw new NullPointerException("CachingRestClient."+name+" is null, and guess what, it shouldn't!");
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


}
