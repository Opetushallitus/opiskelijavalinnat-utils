package fi.vm.sade.javautils.httpclient.apache;

import fi.vm.sade.javautils.httpclient.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.util.HashMap;

class ApacheHttpClientRequestAdapter implements OphHttpClientProxyRequest {
    private final OphRequestParameters requestParameters;
    private final HashMap<String, Boolean> csrfCookiesCreateForHost = new HashMap<>();
    private HttpClient httpClient;
    private CookieStore cookieStore;

    ApacheHttpClientRequestAdapter(OphRequestParameters requestParameters, HttpClient httpClient, CookieStore cookieStore) {
        this.requestParameters = requestParameters;
        this.httpClient = httpClient;
        this.cookieStore = cookieStore;
    }

    @Override
    public <R> R execute(final OphHttpResponseHandler<? extends R> handler) throws IOException {
        return httpClient.execute(createRequest(requestParameters), response -> handler.handleResponse(new ApacheOphHttpResponse(requestParameters, response)));
    }

    /**
     * Should not be used. Use execute() instead because it closes connection automatically.
     */
    @Override
    public OphHttpResponse handleManually() throws IOException {
        return new ApacheOphHttpResponse(requestParameters, httpClient.execute(createRequest(requestParameters)));
    }

    private HttpRequestBase createRequest(OphRequestParameters requestParameters) {
        HttpRequestBase request = getHttpClientRequest(requestParameters.method, requestParameters.url);
        if(requestParameters.dataWriter != null) {
            DataWriterEntity entity = new DataWriterEntity(requestParameters.dataWriterCharset, requestParameters.dataWriter);
            entity.setChunked(true);
            entity.setContentType(requestParameters.contentType + "; charset=" + requestParameters.dataWriterCharset);
            ((HttpEntityEnclosingRequestBase)request).setEntity(entity);
        }
        if(!OphHttpClient.CSRF_SAFE_VERBS.contains(requestParameters.method)) {
            ensureCSRFCookie(request);
        }
        for(String header : requestParameters.headers.keySet()) {
            for(String value: requestParameters.headers.get(header)) {
                request.setHeader(header, value);
            }
        }
        return request;
    }

    private void ensureCSRFCookie(HttpRequestBase req) {
        String host = req.getURI().getHost();
        if (!csrfCookiesCreateForHost.containsKey(host) && cookieStore != null) {
            synchronized (csrfCookiesCreateForHost) {
                if (!csrfCookiesCreateForHost.containsKey(host)) {
                    csrfCookiesCreateForHost.put(host, true);
                    BasicClientCookie cookie = new BasicClientCookie("CSRF", "CSRF");
                    cookie.setDomain(host);
                    cookie.setPath("/");
                    cookieStore.addCookie(cookie);
                }
            }
        }
    }

    private static HttpRequestBase getHttpClientRequest(String method, String url) {
        switch (method) {
            case OphHttpClient.Method.GET: return new HttpGet(url);
            case OphHttpClient.Method.HEAD: return new HttpHead(url);
            case OphHttpClient.Method.OPTIONS: return new HttpOptions(url);
            case OphHttpClient.Method.POST: return new HttpPost(url);
            case OphHttpClient.Method.PUT: return new HttpPut(url);
            case OphHttpClient.Method.PATCH: return new HttpPatch(url);
            case OphHttpClient.Method.DELETE: return new HttpDelete(url);
        }
        throw new RuntimeException("Unsupported HTTP method: " + method + " for url: " + url);
    }
}
