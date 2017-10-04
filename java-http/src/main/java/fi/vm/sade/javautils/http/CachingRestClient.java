package fi.vm.sade.javautils.http;

// import fi.vm.sade.generic.healthcheck.HealthChecker;
import fi.vm.sade.javautils.http.auth.ProxyAuthenticator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.*;

@Getter
@Slf4j
public class CachingRestClient { //implements HealthChecker {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_CACHE_ENTRIES = 50 * 1000; // 50000
    private static final int MAX_OBJECT_SIZE = 10 * 1024 * 1024; // 10MB (oppilaitosnumero-koodisto is ~7,5MB)
    private static final int DEFAULT_TIMEOUT_MS = 5 * 60 * 1000; // 5min
    private static final long DEFAULT_CONNECTION_TTL_SEC = 60; // infran palomuuri katkoo monta minuuttia makaavat connectionit
    private static final String CACHE_RESPONSE_STATUS = "http.cache.response.status";

    // Checked
    private JsonParser jsonParser;
    private LogUtil logUtil;
    private HttpClient cachingClient;
    private CookieProxy cookieProxy;
    private CasAuthenticator casAuthenticator;

    // Unchecked
    private ThreadLocal<HttpContext> localContext = ThreadLocal.withInitial(BasicHttpContext::new);

    private Object cacheStatus;  // used in tests

    //private String webCasUrl;
    //private String username;
    //private String password;
    //private String casService;
    //protected String serviceAsAUserTicket;

    private String requiredVersionRegex;
    private boolean reuseConnections = true;

    private String clientSubSystemCode;

    public CachingRestClient() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_CONNECTION_TTL_SEC);
    }

    public CachingRestClient(int timeoutMs) {
        this(timeoutMs, DEFAULT_CONNECTION_TTL_SEC);
    }

    public CachingRestClient(int timeoutMs, long connectionTimeToLiveSec) {
        this(timeoutMs, connectionTimeToLiveSec, true);
    }

    public CachingRestClient(int timeoutMs, long connectionTimeToLiveSec, boolean allowUrlLogging) {
        logUtil = new LogUtil(allowUrlLogging, timeoutMs);
        jsonParser = new JsonParser();
        cookieProxy = new CookieProxy();
        casAuthenticator = new CasAuthenticator();

        HttpClientBuilder builder = CachingHttpClientBuilder.create()
                .setCacheConfig(createCacheConfig())
                .setConnectionManager(createConnectionManager())
                .setKeepAliveStrategy(createKeepAliveStrategy())
                .setDefaultCookieStore(cookieProxy.getCookieStore())
                .setRedirectStrategy(createRedirectStrategy())
                .setConnectionTimeToLive(connectionTimeToLiveSec, TimeUnit.SECONDS);

        if (!reuseConnections) {
            builder = builder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
        }

        cachingClient = builder.build();

        /*
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMs);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);
        HttpConnectionParams.setSoKeepalive(httpParams, true); // prevent firewall to reset idle connections?
        */
    }

    private static ConnectionKeepAliveStrategy createKeepAliveStrategy() {
        return (response, context) -> 60 * 1000L; // Connection Keep Alive duration in ms (all hosts)
    }

    private static RedirectStrategy createRedirectStrategy() {
        return new DefaultRedirectStrategy() {
            @Override
            public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                URI locationURI = super.getLocationURI(request, response, context);
                String uri = locationURI.toString();
                if (CasUtil.isCasUrl(uri)) {
                    log.debug("Set redirected_to_cas=true, url: " + uri);
                    context.setAttribute(CasUtil.getCasAttributeName(), "true");

                    context.setAttribute(HttpClientContext.REDIRECT_LOCATIONS, new RedirectLocations());
                    //clearRedirects(); // TODO: is this really needed?
                } else { // when redirecting back to service _from_ cas
                    log.debug("Set redirected_to_cas=false, url: " + uri);
                    context.removeAttribute(CasUtil.getCasAttributeName());
                }
                return locationURI;
            }
        };
    }

    private void clearRedirects() {
        // clear redirects, because cas auth could cause same auth redirections again after new login/ticket. this will prevent CircularRedirectException
        localContext.get().setAttribute(HttpClientContext.REDIRECT_LOCATIONS, new RedirectLocations());
        log.info("Cleared redirects");
    }

    private static HttpClientConnectionManager createConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100); // default 2
        connectionManager.setMaxTotal(1000); // default 20
        return connectionManager;
    }

    private static CacheConfig createCacheConfig() {
        return CacheConfig.custom()
                .setMaxCacheEntries(MAX_CACHE_ENTRIES)
                .setMaxObjectSize(MAX_OBJECT_SIZE)
                .build();
    }


    /**
     * get REST Json resource as Java object of type resultType (deserialized with gson).
     * Returns null if error occurred while querying resource.
     */
    public <T> T get(String url, Class<? extends T> resultType) throws IOException {
        InputStream is = null;
        String response = null;
        try {
            is = get(url);
            response = IOUtils.toString(is);
            T t = jsonParser.fromJson(resultType, response);
            return t;
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    public String getAsString(String url) throws IOException {
        return IOUtils.toString(get(url));
    }

    public InputStream get(String url) throws IOException {
        HttpGet req = new HttpGet(url);
        HttpResponse response = execute(req, null, null);
        HttpEntity responseEntity = response.getEntity();
        if (responseEntity == null) {
            logUtil.logAndThrowHttpException(req, response, "request did not return any content");
        }
        return responseEntity.getContent();
    }

    public String postForLocation(String url, String content) throws IOException {
        return postForLocation(url, "application/json", content);
    }

    public String postForLocation(String url, String contentType, String content) throws IOException {
        HttpRequestBase request = new HttpPost(url);
        HttpResponse response = execute(request, contentType, content);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
            return response.getFirstHeader("Location").getValue();
        } else {
            throw new RuntimeException("post didn't result in http 201 created: " + logUtil.info(request, response));
        }
    }

    public HttpResponse post(String url, String contentType, String content) throws IOException {
        return execute(new HttpPost(url), contentType, content);
    }

    public HttpResponse put(String url, String contentType, String content) throws IOException {
        return execute(new HttpPut(url), contentType, content);
    }

    public HttpResponse delete(String url) throws IOException {
        return execute(new HttpDelete(url), null, null);
    }

    public HttpResponse execute(HttpRequestBase req, String contentType, String postOrPutContent) throws IOException {
        return execute(req, contentType, postOrPutContent, 0);
    }

    public HttpResponse execute(HttpRequestBase req, String contentType, String postOrPutContent, int retry) throws IOException {
        fixURI(req);

        if (contentType != null) {
            req.setHeader("Content-Type", contentType);
        }
        if(this.clientSubSystemCode != null) {
            req.setHeader("clientSubSystemCode", this.clientSubSystemCode);
        }

        cookieProxy.setCSRFCookies(req);

        if (postOrPutContent != null && req instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)req).setEntity(new StringEntity(postOrPutContent, UTF8));
        }

        boolean wasJustAuthenticated = false;
        try {
            wasJustAuthenticated = casAuthenticator.authenticate(req);
        } catch (ProxyAuthenticator.CasProxyAuthenticationException e) {
            if (retry == 0) {
                log.warn("Failed to CAS authenticate. Renewing proxy ticket.");
                log.debug("Failed to CAS authenticate. Renewing proxy ticket.", e);
            } else {
                log.warn("Failed second time to CAS authenticate");
                log.debug("Failed second time to CAS authenticate", e);
                // CAS didn't likely recognise TGT (One can't be completely sure since Cas20ProxyRetriever just returns null)
                throw new HttpException(req, getEmptyHttpResponse(SC_UNAUTHORIZED), e.getMessage());
            }
        }

        // do actual request
        HttpResponse response = null;
        String responseString = null;
        try {
            response = cachingClient.execute(req, localContext.get());
        } catch (Exception e) {
            log.error("error in CachingRestClient - " + logUtil.info(req, response, wasJustAuthenticated, wasJustAuthenticated, wasJustAuthenticated, retry), e);
            throw new IOException("Internal error calling " + req.getMethod() + "/" + req.getURI() + " (check logs): " + e.getMessage());
        } finally {
            // after request, wrap response entity so it can be accessed later, and release the connection
            if (response != null && response.getEntity() != null) {
                responseString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                response.setEntity(new StringEntity(responseString, "UTF-8"));
            }
            req.releaseConnection();
        }

        // logging
        boolean isRedirCas = CasUtil.isRedirectToCas(response); // this response is 302 with location header pointing to cas
        boolean wasRedirCas = CasUtil.wasRedirectedToCas(localContext.get()); // this response is from cas after 302 redirect
        boolean isHttp401 = response.getStatusLine().getStatusCode() == SC_UNAUTHORIZED;
        if (log.isDebugEnabled()) {
            log.debug(logUtil.info(req, response, wasJustAuthenticated, isRedirCas, wasRedirCas, retry));
            log.debug("    responseString: {}", responseString);
        }

        // just got new valid ticket, but still got cas login page.. something wrong with the system, target service didn't process the request/ticket correctly?
        if (retry > 0 && wasJustAuthenticated && (isRedirCas || wasRedirCas)) {
            throw new IOException("just got new valid ticket, but still got cas login page.. something wrong with the system, target service didn't process the request/ticket correctly?\n"
                    + logUtil.info(req, response, wasJustAuthenticated, isRedirCas, wasRedirCas, retry));
        }

        // authentication: was redirected to cas OR http 401 -> get ticket and retry once (but do it only once, hence 'retry')
        if (isRedirCas || wasRedirCas || isHttp401) {
            if (retry == 0) {
                log.warn("warn! got redirect to cas or 401 unauthorized, re-getting ticket and retrying request");
                // clearTicket();
                log.debug("set redirected_to_cas=false");
                localContext.get().removeAttribute(CasUtil.getCasAttributeName());
                return execute(req, contentType, postOrPutContent, 1);
            } else {
                // clearTicket();
                logUtil.logAndThrowHttpException(req, response, "Unauthorized error calling REST resource, got redirect to cas or 401 unauthorized");
            }
        }

        filterBadResponses(req, response);

        cacheStatus = localContext.get().getAttribute(CACHE_RESPONSE_STATUS);

        log.debug("{}, url: {}, contentType: {}, content: {}, status: {}, headers: {}", req.getMethod(), req.getURI(), contentType, postOrPutContent, response.getStatusLine(), Arrays.asList(response.getAllHeaders()));
        return response;
    }

    private void fixURI(HttpRequestBase req) {
        if (req.getURI().toString().startsWith("/") && casAuthenticator.getCasService() != null) { // if relative url
            try {
                req.setURI(new URIBuilder(casAuthenticator.getCasService().replace("/j_spring_cas_security_check", "") + req.getURI().toString()).build());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        String url = req.getURI().toString();
        if (req.getURI().getHost() == null) throw new NullPointerException("CachingRestClient.execute ERROR! host is null, req.uri: "+url);
    }

    private void filterBadResponses(HttpRequestBase req, HttpResponse response) throws HttpException {
        if(response.getStatusLine().getStatusCode() == SC_FORBIDDEN) {
            logUtil.logAndThrowHttpException(req, response, "Access denied error calling REST resource");
        }

        if(response.getStatusLine().getStatusCode() >= SC_INTERNAL_SERVER_ERROR) {
            logUtil.logAndThrowHttpException(req, response, "Internal error calling REST resource");
        }

        if(response.getStatusLine().getStatusCode() >= SC_NOT_FOUND) {
            logUtil.logAndThrowHttpException(req, response, "Not found error calling REST resource");
        }

        if(response.getStatusLine().getStatusCode() == SC_BAD_REQUEST) {
            logUtil.logAndThrowHttpException(req, response, "Bad request error calling REST resource");
        }
    }

    private HttpResponse getEmptyHttpResponse(int statusCode) {
        return new DefaultHttpResponseFactory()
                .newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, null), null);
    }

    /*
    @Override
    public Object checkHealth() throws Throwable {
        if (casService != null) {

            // call target service's buildversion url (if we have credentials try the secured url)
            String serviceUrl = casService.replace("/j_spring_cas_security_check", "");
            final String buildversionUrl = serviceUrl + "/buildversion.txt" + (useServiceAsAUserAuthentication() ? "?auth" : "");
            final HttpResponse result = execute(new HttpGet(buildversionUrl), null, null);

            LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>() {{
                put("url", buildversionUrl);
                put("user", useServiceAsAUserAuthentication() ? username : useProxyAuthentication ? "proxy" : "anonymous");
                put("status", result.getStatusLine().getStatusCode() == 200 ? "OK" : result.getStatusLine());
                // todo: kuormitusdata?
            }};

            // kohdepalvelun healthcheck
            try {
                Map hc = get(serviceUrl+"/healthcheck", Map.class);
                Object targetserviceStatus = hc.get("status");
                if ("OK".equals(targetserviceStatus)) {
                    map.put("targetserviceHealthcheck", "OK");
                } else {
                    throw new Exception("targetserviceHealthcheck error: "+targetserviceStatus);
                }
            } catch (HttpException e) {
                if (e.getStatusCode() == 404) {
                    map.put("targetserviceHealthcheck", "not found");
                } else {
                    throw new Exception("targetserviceHealthcheck exception: "+e.getMessage());
                }
            }

            // mikäli kohdepalvelu ok, mutta halutaan varmistaa vielä sen versio
            if (result.getStatusLine().getStatusCode() == 200 && requiredVersionRegex != null) {
                Properties buildversionProps = new Properties();
                buildversionProps.load(result.getEntity().getContent());
                String version = buildversionProps.getProperty("version");
                if (!version.matches(requiredVersionRegex)) {
                    throw new Exception("wrong version: "+version+", required: "+ requiredVersionRegex+", service: "+casService);
                }
                map.put("version", version);
            }

            return map;
        } else {
            return "nothing to check, casService not configured";
        }
    }
    */
}
