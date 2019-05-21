package fi.vm.sade.javautils.httpclient.apache;

import fi.vm.sade.javautils.httpclient.*;
import fi.vm.sade.properties.OphProperties;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;

public class ApacheOphHttpClient extends OphHttpClientProxy {
    private static final int DEFAULT_TIMEOUT_IN_MS = 10000;
    private static final long DEFAULT_TIME_TO_LIVE_IN_SEC = 60L;

    private CloseableHttpClient httpClient;
    private CookieStore cookieStore;

    public ApacheOphHttpClient(ApacheHttpClientBuilder builder) {
        httpClient = builder.getHttpBuilder().build();
        cookieStore = builder.getCookieStore();
    }

    public static OphHttpClient createDefaultOphClient(String callerId, OphProperties urlProperties) {
        return createDefaultOphClient(callerId, urlProperties, DEFAULT_TIMEOUT_IN_MS, DEFAULT_TIME_TO_LIVE_IN_SEC);
    }

    public static OphHttpClient createDefaultOphClient(String callerId, OphProperties urlProperties, int timeoutMs, long connectionTimeToLiveSec) {
        return new ApacheHttpClientBuilder()
                .createClosableClient()
                .setDefaultConfiguration(timeoutMs, connectionTimeToLiveSec)
                .buildOphClient(callerId, urlProperties);
    }

    public static ApacheHttpClientBuilder createCustomBuilder() {
        return new ApacheHttpClientBuilder();
    }

    public CookieStore getCookieStore() {
        return this.cookieStore;
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters) {
        return new ApacheHttpClientRequestAdapter(requestParameters, httpClient, cookieStore);
    }
}
