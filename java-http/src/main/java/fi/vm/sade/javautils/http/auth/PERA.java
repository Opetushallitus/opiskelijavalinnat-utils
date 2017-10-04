package fi.vm.sade.javautils.http.auth;

import org.apache.http.client.methods.HttpRequestBase;

public class PERA {

    public static final String X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS = "X-Kutsuketju.Aloittaja.KayttajaTunnus";
    public static final String X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS = "X-Palvelukutsu.Lahettaja.KayttajaTunnus";
    public static final String X_PALVELUKUTSU_LAHETTAJA_PROXY_AUTH = "X-Palvelukutsu.Lahettaja.ProxyAuth"; // ei perassa

    public static void setKayttajaHeaders(HttpRequestBase req, String currentUser, String callAsUser) {
        req.setHeader(X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS, currentUser);
        req.setHeader(X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS, callAsUser);
    }

    public static void setProxyKayttajaHeaders(ProxyAuthenticator.Callback callback, String currentUser) {
        callback.setRequestHeader(X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS, currentUser);
        callback.setRequestHeader(X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS, currentUser);
        callback.setRequestHeader(X_PALVELUKUTSU_LAHETTAJA_PROXY_AUTH, "true");
    }

}
