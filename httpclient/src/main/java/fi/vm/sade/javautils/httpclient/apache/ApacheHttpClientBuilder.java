package fi.vm.sade.javautils.httpclient.apache;

import fi.vm.sade.javautils.httpclient.OphHttpClient;
import fi.vm.sade.properties.OphProperties;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

/**
 * Helper methods for HttpClientBuilder.
 *
 * 1. By default uses closable HttpClientBuilder, use createCachingClient() or setHttpClientBuilder() to initialize another kind of apache httpclient httpBuilder
 *
 * 2. Call helper methods to configure httpBuilder
 *
 * 3. Call build() and create OphHttpClient
 */
public class ApacheHttpClientBuilder {
    // default to non-caching closableClient
    private HttpClientBuilder httpBuilder = HttpClientBuilder.create();
    private CookieStore cookieStore = null;

    public ApacheOphHttpClient build() {
        disableRedirectHandling();
        return new ApacheOphHttpClient(this);
    }

    public ApacheHttpClientBuilder createClosableClient() {
        httpBuilder = HttpClientBuilder.create();
        return this;
    }

    public ApacheHttpClientBuilder createCachingClient() {
        final int maxCacheEntries = 50 * 1000; // 50000
        final int maxObjectSize = 10 * 1024 * 1024; // 10MB (oppilaitosnumero -koodisto is ~7,5MB)
        return createCachingClient(maxCacheEntries, maxObjectSize);
    }

    public ApacheHttpClientBuilder createCachingClient(int maxCacheEntries, int maxObjectSize) {
        CachingHttpClientBuilder builder = CachingHttpClientBuilder.create();
        CacheConfig cacheConfig = CacheConfig.custom().
                setMaxCacheEntries(maxCacheEntries).
                setMaxObjectSize(maxObjectSize).build();
        builder.setCacheConfig(cacheConfig);
        this.httpBuilder = builder;
        return this;
    }

    public ApacheHttpClientBuilder setHttpClientBuilder(HttpClientBuilder httpBuilder) {
        this.httpBuilder = httpBuilder;
        return this;
    }

    public ApacheHttpClientBuilder setDefaultConfiguration(int timeoutMs, long connectionTimeToLiveSec) {
        setPoolingConnectionManager(connectionTimeToLiveSec, 100, 1000);
        setRequestTimeouts(timeoutMs);
        setSocketConfig(timeoutMs);
        setCookieStore();
        return this;
    }

    public ApacheHttpClientBuilder setSocketConfig(int timeoutMs) {
        SocketConfig socketConfig = SocketConfig.custom().
                setSoKeepAlive(true).
                setSoTimeout(timeoutMs).
                setTcpNoDelay(true).
                build();
        httpBuilder.setDefaultSocketConfig(socketConfig);
        return this;
    }

    public ApacheHttpClientBuilder setRequestTimeouts(int timeoutMs) {
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectionRequestTimeout(timeoutMs).
                setConnectTimeout(timeoutMs).
                setSocketTimeout(timeoutMs).
                build();
        httpBuilder.setDefaultRequestConfig(requestConfig);
        return this;
    }

    public ApacheHttpClientBuilder setPoolingConnectionManager(long connectionTimeToLiveSec, int defaultMaxPerRoute, int maxTotal) {
        // multithread support + max connections
        PoolingHttpClientConnectionManager connectionManager;
        connectionManager = new PoolingHttpClientConnectionManager(connectionTimeToLiveSec, TimeUnit.MILLISECONDS);
        connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute); // default 2
        connectionManager.setMaxTotal(maxTotal); // default 20
        httpBuilder.setConnectionManager(connectionManager);
        return this;
    }

    public ApacheHttpClientBuilder setCookieStore() {
        cookieStore = new BasicCookieStore();
        httpBuilder.setDefaultCookieStore(cookieStore);
        return this;
    }

    public ApacheHttpClientBuilder disableRedirectHandling() {
        httpBuilder.disableRedirectHandling();
        return this;
    }

    public OphHttpClient buildOphClient(String clientSubsystemCode, OphProperties urlProperties) {
        return new OphHttpClient(build(), clientSubsystemCode, urlProperties);
    }

    public HttpClientBuilder getHttpBuilder() {
        return httpBuilder;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }
}
