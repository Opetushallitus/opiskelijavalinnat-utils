package fi.vm.sade.javautils.http;

import fi.vm.sade.javautils.http.refactor.PERA;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

@Slf4j
class LogUtil {

    private static final String CAS_SECURITY_TICKET = "CasSecurityTicket";
    private boolean allowUrlLogging;
    private int timeoutMs;

    LogUtil(boolean allowUrlLogging, int timeoutMs) {
        this.allowUrlLogging = allowUrlLogging;
        this.timeoutMs = timeoutMs;
    }

    void logAndThrowHttpException(HttpRequestBase req, HttpResponse response, final String msg) throws HttpException {
        String message = msg + ", " + info(req, response);
        log.error(message);
        throw new HttpException(req, response, message);
    }

    String info(HttpUriRequest req, HttpResponse response, boolean wasJustAuthenticated, boolean isRedirCas, boolean wasRedirCas, int retry) {
        return info(req, response)
                + ", isredircas: " + isRedirCas
                + ", wasredircas: " + wasRedirCas
                + ", wasJustAuthenticated: " + wasJustAuthenticated
                + ", retry: " + retry;
    }

    String info(HttpUriRequest req, HttpResponse response) {
        return "url: " + (allowUrlLogging ? req.getURI() : "hidden")
                + ", method: " + req.getMethod()
                + ", status: " + (response != null && response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : "?")
                + ", userInfo: " + getUserInfo(req)
                + ", timeoutMs: " + timeoutMs;
    }

    private String getUserInfo(HttpUriRequest req) {
        return header(req, "current", PERA.X_KUTSUKETJU_ALOITTAJA_KAYTTAJA_TUNNUS)
                + header(req, "caller", PERA.X_PALVELUKUTSU_LAHETTAJA_KAYTTAJA_TUNNUS)
                + header(req, "proxy", PERA.X_PALVELUKUTSU_LAHETTAJA_PROXY_AUTH)
                + header(req, "ticket", CAS_SECURITY_TICKET);
    }

    private String header(HttpUriRequest req, String info, String name) {
        Header[] headers = req.getHeaders(name);
        StringBuilder res = new StringBuilder();
        if (headers != null && headers.length > 0) {
            res.append("|").append(info).append(":");
            for (Header header : headers) {
                res.append(header.getValue());
            }
        }
        return res.toString();
    }


}
