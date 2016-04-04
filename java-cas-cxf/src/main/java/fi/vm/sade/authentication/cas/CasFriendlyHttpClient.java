package fi.vm.sade.authentication.cas;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultHttpClient enhanced with CAS specific CasRedirectStrategy and
 * HttpRequestInterceptor (to catch and store the original request and params).
 * CasRedirectStrategy is where all the magic happens.
 * @author Jouni Stam
 *
 */
public class CasFriendlyHttpClient extends DefaultHttpClient {

    private static final Logger log = LoggerFactory.getLogger(CasFriendlyHttpClient.class);
    private static final String SPRING_CAS_SUFFIX = "j_spring_cas_security_check";
    public static CasRedirectStrategy casRedirectStrategy = new CasRedirectStrategy();

    /**
     * Constructor sets CasRedirectStategy and adds request interceptor which sets
     * CasRedirectStrategy.ATTRIBUTE_ORIGINAL_REQUEST and CasRedirectStrategy.ATTRIBUTE_ORIGINAL_REQUEST_PARAMS
     * attributes to the HttpContext. 
     */
    public CasFriendlyHttpClient() {

        // Let the super constructor do its work first
        super();

        // Set redirect strategy
        this.setRedirectStrategy(casRedirectStrategy);

        // Adds an interceptor
        this.addRequestInterceptor(new HttpRequestInterceptor() {

            /**
             * Takes the first request as original request and stores it later use (after login).
             */
            @Override
            public void process(HttpRequest request, HttpContext context)
                    throws HttpException, IOException {
                logHeaders(request);

                if(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_ORIGINAL_REQUEST) == null) {
                    log.debug("Started with original request: " + request.getRequestLine().getUri());

                    // TODO Add session Id cookie from cache if available
                    CasFriendlyCache cache = (CasFriendlyCache)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CACHE);
                    if(cache != null) {
                        //						String sessionId = cache.getSessionId("any", targetServiceUrl, userName);
                    }

                    // Target service URL is the service name used for authentication
                    String targetUrl = CasRedirectStrategy.resolveUrl(request);
                    String targetServiceUrl = resolveTargetServiceUrl(targetUrl);

                    // Do not change serviceUrl for authenticate only requests if serviceUrl is set
                    if(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_SERVICE_URL) == null 
                            || !(new Boolean(true).equals(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_AUTHENTICATE_ONLY))))
                        setTargetServiceUrl(context, targetServiceUrl);
                    context.setAttribute(CasRedirectStrategy.ATTRIBUTE_ORIGINAL_REQUEST, request);
                    context.setAttribute(CasRedirectStrategy.ATTRIBUTE_ORIGINAL_REQUEST_PARAMS, request.getParams());
                }
            }
        });

        // Not really necessary, only for logging purposes
        this.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                log.debug("Response: " + response.getStatusLine().getStatusCode());
            }
        });
    }

    /**
     * Creates a CAS client request based on intercepted CXF message.
     * @param message
     * @throws IOException 
     */
    public static HttpUriRequest createRequest(Message message, boolean authenticateOnly, HttpContext context) throws IOException {
        // Original out message (request)
        Message outMessage = message.getExchange().getOutMessage();
        String method = (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
        String url = (String)outMessage.get(Message.ENDPOINT_ADDRESS);

        HttpUriRequest uriRequest = null;

        if(authenticateOnly) {
            // Authenticate only, original URL must not be requested!
            context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_AUTHENTICATE_ONLY, new Boolean(authenticateOnly));
            // Using j_spring_cas_security_check as the starting point
            url = resolveTargetServiceUrl(url);
            // Just the authentication request
            uriRequest = new HttpGet(url);
        } else {
            if(method.equalsIgnoreCase("POST")) {
                uriRequest = new HttpPost(url);
                InputStream is = (InputStream)message.getExchange().get(CasFriendlyCxfInterceptor.ORIGINAL_POST_BODY_INPUTSTREAM);
                String mimeType = (String) outMessage.get(Message.CONTENT_TYPE);
                String charset = (String) outMessage.get(Message.ENCODING);
                if (charset == null && ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
                    charset = "UTF-8";
                }
                if(is != null) {
                    ((HttpPost) uriRequest).setEntity(new StringEntity(IOUtils.toString(is, charset), ContentType.create(mimeType, charset)));
                }
            } else if(method.equalsIgnoreCase("GET")) {
                uriRequest = new HttpGet(url);
            } else if(method.equalsIgnoreCase("DELETE")) {
                uriRequest = new HttpDelete(url);
            } else if(method.equalsIgnoreCase("PUT")) {
                uriRequest = new HttpPut(url);
                InputStream is = (InputStream)message.getExchange().get(CasFriendlyCxfInterceptor.ORIGINAL_POST_BODY_INPUTSTREAM);
                String mimeType = (String) outMessage.get(Message.CONTENT_TYPE);
                String charset = (String) outMessage.get(Message.ENCODING);
                if (charset == null && ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
                    charset = "UTF-8";
                }
                if(is != null) {
                    ((HttpPut) uriRequest).setEntity(new StringEntity(IOUtils.toString(is, charset), ContentType.create(mimeType, charset)));
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, List<String>> headers = (Map<String, List<String>>) outMessage.get(Message.PROTOCOL_HEADERS);
            for (String one : headers.keySet()) {
                List<String> values = headers.get(one);
                // Just add the first value
                uriRequest.addHeader(one, values.get(0));
            }
        }

        return uriRequest;

    }

    /**
     * Creates a context with principal set.
     * @param principal
     * @return
     */
    public HttpContext createHttpContext(AttributePrincipal principal, CasFriendlyCache cache) {
        HttpContext context = super.createHttpContext();
        // Add principal attribute
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_PRINCIPAL, principal);
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CACHE, cache);
        return context;
    }

    /**
     * Creates a context with principal mock from given login and password.
     * @param principal
     * @return
     */
    public HttpContext createHttpContext(String login, String password, CasFriendlyCache cache) {
        HttpContext context = super.createHttpContext();
        // Add credentials
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_LOGIN, login);
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_PASSWORD, password);
        return context;
    }

    private static void logHeaders(HttpRequest request) {
        log.debug("Request headers: ");
        for(Header one:request.getAllHeaders()) {
            log.debug(one.getName() + ": " + one.getValue());
        }
    }

    /**
     * Resolves target service URL from message's endpoint address.
     * @param message
     * @return
     * @throws MalformedURLException
     */
    public static String resolveTargetServiceUrl(String targetUrl) throws MalformedURLException {
        URL url = new URL(targetUrl);
        String port = ((url.getPort() > 0)?(":" + url.getPort()):"");
        String[] folders = url.getPath().split("/");
        String path = "/";
        if(folders.length > 0)
            path += folders[1] + "/";
        String finalUrl = url.getProtocol() + "://" + 
                url.getHost() + port + path + SPRING_CAS_SUFFIX;
        return finalUrl.toString();
    }

    public static void setTargetServiceUrl(HttpContext context, String serviceUrl) {
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_SERVICE_URL, serviceUrl);
    }
}
