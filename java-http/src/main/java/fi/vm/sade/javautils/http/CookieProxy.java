package fi.vm.sade.javautils.http;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.HashMap;

class CookieProxy {

    private static final String CSRF = "CachingRestClient";
    private HashMap<String, Boolean> csrfCookiesCreateForHost = new HashMap<>();
    private CookieStore cookieStore;

    CookieProxy() {
        cookieStore = new BasicCookieStore();
    }

    void setCSRFCookies(HttpRequestBase req) {
        req.setHeader("CSRF", CSRF);
        ensureCSRFCookie(req);
    }

    CookieStore getCookieStore() {
        return cookieStore;
    }

    private void ensureCSRFCookie(HttpRequestBase req) {
        String host = req.getURI().getHost();
        if (!csrfCookiesCreateForHost.containsKey(host)) {
            synchronized (csrfCookiesCreateForHost) {
                if (!csrfCookiesCreateForHost.containsKey(host)) {
                    csrfCookiesCreateForHost.put(host, true);
                    BasicClientCookie cookie = new BasicClientCookie("CSRF", CSRF);
                    cookie.setDomain(host);
                    cookie.setPath("/");
                    cookieStore.addCookie(cookie);
                }
            }
        }
    }

}
