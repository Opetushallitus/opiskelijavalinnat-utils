package fi.vm.sade.javautils.httpclient;

import fi.vm.sade.properties.OphProperties;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApacheOphHttpClient extends OphHttpClientProxy {
    private CloseableHttpClient httpclient;
    private final HashMap<String, Boolean> csrfCookiesCreateForHost = new HashMap<>();
    private CookieStore cookieStore;

    private ApacheOphHttpClient(ApacheHttpClientBuilder config) {
        this.httpclient = config.httpBuilder.build();
        cookieStore = config.cookieStore;
    }

    public static OphHttpClient createDefaultOphHttpClient(String clientSubsystemCode, OphProperties urlProperties, int timeoutMs, long connectionTimeToLiveSec) {
        return new ApacheHttpClientBuilder()
                .createClosableClient()
                .setDefaultConfiguration(timeoutMs, connectionTimeToLiveSec)
                .buildOphClient(clientSubsystemCode, urlProperties);
    }

    public static ApacheHttpClientBuilder createCustomBuilder() {
        return new ApacheHttpClientBuilder();
    }

    /**
     * Helper methods for HttpClientBuilder.
     *
     * 1. Use createClosableClient(), createCachingClient() or setHttpClientBuilder() to initialize httpBuilder
     *
     * 2. Call helper methods to configure httpBuilder
     *
     * 3. Call build() and create OphHttpClient
     */
    public static class ApacheHttpClientBuilder {
        public HttpClientBuilder httpBuilder = null;
        public CookieStore cookieStore = null;

        public ApacheOphHttpClient build() {
            disableRedirectHandling();
            return new ApacheOphHttpClient(this);
        }

        public ApacheHttpClientBuilder createClosableClient() {
            httpBuilder = HttpClientBuilder.create();
            return this;
        }

        // good values could be 50 * 1000 and 10 * 1024 * 1024
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
    }

    @Override
    public OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters) {
        return new ApacheHttpClientRequestAdapter(requestParameters);
    }

    private class ApacheHttpClientRequestAdapter implements OphHttpClientProxyRequest {
        private final OphRequestParameters requestParameters;

        ApacheHttpClientRequestAdapter(OphRequestParameters requestParameters) {
            this.requestParameters = requestParameters;
        }

        @Override
        public <R> R execute(final OphHttpResponseHandler<? extends R> handler) throws IOException {
            ResponseHandler<R> responseHandler = new ResponseHandler<R>() {
                @Override
                public R handleResponse(final HttpResponse response) throws IOException {
                    return handler.handleResponse(new ApacheOphHttpResponse(requestParameters, response));
                }
            };

            return httpclient.execute(createRequest(requestParameters), responseHandler);
        }

        /**
         * Should not be used. Use execute() instead because it closes connection automatically.
         */
        @Override
        public OphHttpResponse handleManually() throws IOException {
            return new ApacheOphHttpResponse(requestParameters, httpclient.execute(createRequest(requestParameters)));
        }

        private HttpRequestBase createRequest(OphRequestParameters requestParameters) {
            HttpRequestBase request = getHttpClientRequest(requestParameters.method, requestParameters.url);
            if(requestParameters.dataWriter != null) {
                ((HttpEntityEnclosingRequestBase)request).setEntity(new DataWriter(requestParameters.dataWriterCharset, requestParameters.dataWriter));
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

    private class ApacheOphHttpResponse implements OphHttpResponse {
        private final String url;
        private HttpResponse response;

        ApacheOphHttpResponse(OphRequestParameters requestParameters, HttpResponse response) {
            this.url = requestParameters.url;
            this.response = response;
        }

        @Override
        public InputStream asInputStream() {
            try {
                return response.getEntity().getContent();
            } catch (IOException e) {
                throw new RuntimeException("Url: " + url, e);
            }
        }

        @Override
        public String asText() {
            try {
                return OphHttpClient.toString(asInputStream());
            } catch (IOException e) {
                throw new RuntimeException("Url: " + url, e);
            }
        }

        @Override
        public void close() {
            try {
                ((CloseableHttpResponse)response).close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing connection: " + url, e);
            }
        }

        @Override
        public int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public List<String> getHeaderValues(String key) {
            List<String> ret = new ArrayList<>();
            for(Header h: response.getHeaders(key)) {
                ret.add(h.getValue());
            }
            return ret;
        }

        @Override
        public List<String> getHeaderKeys() {
            List<String> ret = new ArrayList<>();
            for(Header h: response.getAllHeaders()) {
                if(!ret.contains(h.getName())) {
                    ret.add(h.getName());
                }
            }
            return ret;
        }
    }

    private class DataWriter extends AbstractHttpEntity {
        private OphRequestPostWriter dataWriter;
        private String charsetName;

        DataWriter(String charsetName, OphRequestPostWriter dataWriter) {
            this.dataWriter = dataWriter;
            this.charsetName = charsetName;
        }

        public boolean isRepeatable() {
            return false;
        }

        public long getContentLength() {
            return -1;
        }

        public boolean isStreaming() {
            return false;
        }

        public InputStream getContent() throws IOException {
            // Should be implemented as well but is irrelevant for this case
            throw new UnsupportedOperationException();
        }

        public void writeTo(final OutputStream outstream) throws IOException {
            Writer writer = new OutputStreamWriter(outstream, charsetName);
            dataWriter.writeTo(writer);
            writer.flush();
        }
    }
}
