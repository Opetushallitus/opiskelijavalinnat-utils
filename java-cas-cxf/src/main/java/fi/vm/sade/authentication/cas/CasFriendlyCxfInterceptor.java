package fi.vm.sade.authentication.cas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.protocol.HttpContext;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Interceptor for handling CAS redirects and authentication transparently when needed.
 * Development mode provided that Spring Security's authentication manager has
 * erase-credientals = false: <authentication-manager alias = "authenticationManager"  erase-credentials = "false">
 * NOTE! If configured to use proxy authentication, re-authentication can only happen as long as the TGT is alive
 * (by default 2h, max 8h). Sessions can outlive this, but re-authentication will fail in case it is needed.
 * If using given username and password, there is no "timeout" for re-authentication.
 * CallerService should be set to give Caller-Id for logging purposes.
 * @author Jouni Stam
 *
 * !!! THREAD SAFETY !!!
 * !!! http://cxf.apache.org/docs/jax-rs-client-api.html !!!
 *
 */
public class CasFriendlyCxfInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {

    private static final Logger log = LoggerFactory.getLogger(CasFriendlyCxfInterceptor.class);

    public static final String ORIGINAL_POST_BODY_INPUTSTREAM = CasFriendlyCxfInterceptor.class.getName() + ".postBodyStream";
    public static final String ORIGINAL_POST_BODY_LENGTH = CasFriendlyCxfInterceptor.class.getName() + ".postBodyLength";

    public static final String HEADER_COOKIE = "Cookie";
    public static final String HEADER_COOKIE_SEPARATOR = "; ";

    @Autowired
    CasFriendlyCache sessionCache;

    @Value("${auth.mode:cas}")
    private String authMode;
    
    private String sessionCookieName = "JSESSIONID";
    private String casSessionCookieName = "CASTGC";
    private String callerService = "any";

    private boolean useBasicAuthentication = false;

    // Application as a user
    private String appClientUsername;
    private String appClientPassword;
    
    private long maxWaitTimeMillis = 3000;
    private boolean sessionRequired = true;
    
    private boolean useBlockingConcurrent = false;
    
    private boolean useSessionPerUser = true;

    public CasFriendlyCxfInterceptor() {
        // Intercept in receive phase
        super(Phase.PRE_PROTOCOL);
    }

    /**
     * Invoked on in- and outbound (if interceptor is registered for both). 
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        boolean inbound = (Boolean)message.get(Message.INBOUND_MESSAGE);
        if(inbound) 
            this.handleInbound(message);
        else
            this.handleOutbound(message);
    }

    private void prepareOutMessage(final Message message) throws Fault {
        try {
            OutputStream os = message.getContent ( OutputStream.class );
            CacheAndWriteOutputStream cwos = new CacheAndWriteOutputStream (os);
            message.setContent ( OutputStream.class, cwos );
            
            cwos.registerCallback ( new CachedOutputStreamCallback() {
                @Override
                public void onClose ( CachedOutputStream cos ) {
                    try {
                        if ( cos != null ) {
//                            System.out.println ("Response XML in out Interceptor : " + IOUtils.toString ( cos.getInputStream ( ) ));
                            message.getExchange().put(CasFriendlyCxfInterceptor.ORIGINAL_POST_BODY_INPUTSTREAM, cos.getInputStream());
                            message.getExchange().put(CasFriendlyCxfInterceptor.ORIGINAL_POST_BODY_LENGTH, cos.size());
                        }
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFlush ( CachedOutputStream cos ) {
                }    
            });
        } catch(Exception ex) {
            throw new Fault(ex);
        }
    }
    
    /**
     * Invoked on outbound (request).
     * @param message
     * @throws Fault
     */
    public void handleOutbound(Message message) throws Fault {
        log.debug("Outbound message intercepted.");
        HttpURLConnection conn = resolveConnection(message);
        Authentication auth = this.getAuthentication();
        try {
            String callerService = this.getCallerService();
            
            if(callerService == null || callerService.trim().length() <= 0) {
                log.warn("CallerService is not set. Set callerService property to a distinctive name for this service.");
            }
            String targetServiceUrl = resolveTargetServiceUrl(message);
            log.debug("Outbound target URL: " + targetServiceUrl);
            String sessionId = null;
            // Try to get userName based on authentication
            String userName = (this.getAppClientUsername() != null)?this.getAppClientUsername():((auth != null)?auth.getName():null);
            if(userName != null) {
                log.debug("Outbound username: " + userName);
                sessionId = this.getSessionIdFromCache(callerService, targetServiceUrl, userName);
                log.debug("Outbound sessionId from cache: " + sessionId);
                if(sessionId == null && this.isUseBlockingConcurrent()) {
                    log.debug("Outbound uses blocking (useBlockingConcurrent == true).");
                    // Block multiple requests if necessary, lock if no concurrent running
                    this.sessionCache.waitOrFlagForRunningRequest(callerService, targetServiceUrl, userName, this.getMaxWaitTimeMillis(), true);
                    // Might be available now
                    sessionId = this.getSessionIdFromCache(callerService, targetServiceUrl, userName);
                    log.debug("Outbound sessionId from cache after blocking: " + sessionId);
                }
                // Set sessionId if possible before making the request
                if(sessionId != null) 
                    setSessionCookie(conn, sessionId);
                else if(this.isSessionRequired()) {
                    // Do this proactively only if session is required.
                    log.debug("Outbound requiring sessionId, doing proactive authentication.");
    
                    // Do CAS or DEV authentication
                    this.doAuthentication(message, targetServiceUrl, false);
    
                    // Might be available now
                    sessionId = this.getSessionIdFromCache(callerService, targetServiceUrl, userName);
                    
                    log.debug("Outbound sessionId after authentication process: " + sessionId);
    
                    // Set sessionId if possible before making the request
                    if(sessionId != null) 
                        setSessionCookie(conn, sessionId);
                }

            } else {
                log.debug("No outbound username available. Continuing as unauthenticated.");
            }

            // Set message body to exchange for further use in Inbound
            prepareOutMessage(message);

        } catch(Exception ex) {
            log.error("Unable process outbound message in interceptor.", ex);
            throw new Fault(ex);
        }
    }

    /**
     * Invoked on inbound (response).
     * @param message
     * @throws Fault
     */
    public void handleInbound(Message message) throws Fault {
        log.debug("Inbound message intercepted.");

        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS);

        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        log.debug("Original response code: " + responseCode);

        List<String> locationHeader = headers.get("Location");
        String location = null;
        if(locationHeader != null) 
            location = locationHeader.get(0);
        if(location != null) {
            log.debug("Redirect proposed: " + location);
            try {
                URL url = new URL(location);
                String path = url.getPath();
                // We are only interested in CAS redirects
                if(path.startsWith("/cas/login")) {
                    // Set back original message
                    message = message.getExchange().getOutMessage();
                    // Do CAS authentication request (multiple requests)
                    String targetServiceUrl = resolveTargetServiceUrl(message);
                    // CAS auth is the only option as redirect is to cas/login
                    this.doAuthentication(message, targetServiceUrl, true);

                }
            } catch(Exception ex) {
                log.warn("Error while calling for CAS.", ex);
            }
        }
    }

    /**
     * Does CAS authentication procedure and goes back to the original request after that
     * to get the response from original source after authentication.
     * @param message
     * @return Returns true if message was filled with the response, false otherwise.
     * @throws ClientProtocolException
     * @throws IOException
     */
   private boolean doCasAuthentication(Message message, String targetServiceUrl, String login, String password, boolean casRedirect) throws Exception {
        String userName = null;
        HttpUriRequest request = null;

        try {
            // Follow redirects in a separate CasFriendlyHttpClient request chain
            CasFriendlyHttpClient casClient = new CasFriendlyHttpClient();

            Authentication auth = this.getAuthentication();

            HttpContext context = null;

            userName = (login != null)?login:auth.getName();

            if(casRedirect) {
                // Remove invalid session from cache
                sessionCache.removeSessionId(this.getCallerService(), targetServiceUrl, userName);
            }

            // Create http context with login+password or with PGT
            if(login != null && password != null) {
                context = casClient.createHttpContext(
                        login, password, this.sessionCache);
            } else if(auth != null && auth instanceof CasAuthenticationToken) {
                CasAuthenticationToken token = (CasAuthenticationToken)auth;
                AttributePrincipal principal = token.getAssertion().getPrincipal();
                if(principal != null)
                    context = casClient.createHttpContext(principal, this.sessionCache);
            } else
                return false;

            request = CasFriendlyHttpClient.createRequest(message, !casRedirect, context);

            HttpResponse response = casClient.execute(request, context);

            // Set session ids
            CookieStore cookieStore = (CookieStore)context.getAttribute(ClientContext.COOKIE_STORE);
            String sessionId = resolveSessionId(cookieStore, this.getSessionCookieName());
            // Not available with REST service
//            String casSessionId = resolveSessionId(cookieStore, this.getCasSessionCookieName());
            if(sessionId != null) {
                // Set to cache
                setSessionIdToCache(this.getCallerService(), targetServiceUrl, userName, sessionId);
                log.debug("Session cached: " + sessionCache.getSessionId(this.getCallerService(), targetServiceUrl, userName));
            }

            // Set values back to message from response
            if(casRedirect && response != null) {
                fillMessage(message, response);
                return true;
            } else
                return false;

        } finally {
            // Release connection
            if(request != null && request instanceof HttpRequestBase)
                ((HttpRequestBase)request).releaseConnection();
            // Release request for someone else
            this.releaseRequest(this.getCallerService(), targetServiceUrl, userName);
        }
    }

    /**
     * Do authentication, DEV or CAS.
     * @param message
     * @param targetServiceUrl
     * @param casRedirect True means response was cas/login redirect, false otherwise.
     * @throws Exception
     */
    private boolean doAuthentication(Message message, String targetServiceUrl, boolean casRedirect) throws Exception {
        if(this.isDevMode()) {
            return this.doDevAuthentication(message, targetServiceUrl, this.getAppClientUsername(), this.getAppClientPassword(), casRedirect);
        } else {
            return this.doCasAuthentication(message, targetServiceUrl, this.getAppClientUsername(), this.getAppClientPassword(), casRedirect);
        }
    }

    /**
     * Does DEV mode authentication. In practice sets Basic authentication headers to intercepted message.
     * ONLY FOR DEV MODE!!!
     * @param message
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    private boolean doDevAuthentication(Message message, String targetServiceUrl, String login, String password, boolean casRedirect) throws Exception {
        String userName = login;

        try {
            Authentication auth = this.getAuthentication();

            UsernamePasswordAuthenticationToken token = null;

            if(login == null && password == null) {
                // Take from authentication credentials
                if(auth instanceof UsernamePasswordAuthenticationToken) {
                    token = (UsernamePasswordAuthenticationToken) auth;
                    login = token.getName();
                    password = token.getCredentials().toString();
                }
            }

            if(!casRedirect && this.isUseBasicAuthentication()) {
                HttpURLConnection conn = ((HttpURLConnection) message.get("http.connection"));
                // Applies to outbound only
                if(conn != null && login != null && password != null) {
                    // Set to Authorization header
                    conn.setRequestProperty("Authorization", "Basic "
                            + getBasicAuthenticationEncoding(login, password));
                }
                return false;
            } else {
                // Do "normal" cas login with login and password
                return this.doCasAuthentication(message, targetServiceUrl, login, password, casRedirect);
            }
        } finally {
            // Release request for someone else
            this.releaseRequest(this.getCallerService(), targetServiceUrl, userName);
        }
    }

    /**
     * Base64 encoding for basic authentication.
     * @param username
     * @param password
     * @return
     */
    private String getBasicAuthenticationEncoding(String username, String password) {
        String userPassword = username + ":" + password;
        return new String(Base64.encodeBase64(userPassword.getBytes()));
    }

    /**
     * Recreates message based on response. The response may still be a redirect to cas login if login
     * was not accepted. This will be the final result in any case.
     * @param message
     * @param response
     * @return
     * @throws IOException 
     * @throws IllegalStateException 
     */
    private static void fillMessage(Message message, HttpResponse response) throws IllegalStateException, IOException {
        // Set body from final request. Overwrites the original response body.
        Message inMessage = message.getExchange().getInMessage();
        if (inMessage != null) {
            if (response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                CachedOutputStream bos = new CachedOutputStream();
                IOUtils.copy(is, bos);
                bos.flush();
                bos.close();
                inMessage.setContent(InputStream.class, bos.getInputStream());
            }

            // Set status code from final request.
            inMessage.put(Message.RESPONSE_CODE, new Integer(response.getStatusLine().getStatusCode()));

            // Set headers
            Header[] headers = response.getAllHeaders();
            @SuppressWarnings("unchecked")
            Map<String, List<String>> protocolHeaders = (Map<String, List<String>>) inMessage.get(Message.PROTOCOL_HEADERS);
            for (Header one : headers) {
                protocolHeaders.put(one.getName(), Arrays.asList(one.getValue()));
            }
        }

        // Set status code from final request.
        message.getExchange().put(Message.RESPONSE_CODE, new Integer(response.getStatusLine().getStatusCode()));
    }

    /**
     * Sets session Id to cache.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     * @param sessionId
     */
    protected void setSessionIdToCache(String callerService, String targetServiceUrl, String userName, String sessionId) {
        // Use userName or sessionId as key for session cache
        String keyForSessionCache = userName;
        if(!this.isUseSessionPerUser())
            keyForSessionCache = this.getClientSessionId();
        sessionCache.setSessionId(callerService, targetServiceUrl, keyForSessionCache, sessionId);
    }

    /**
     * Gets session Id from cache if any available for this user.
     * @return
     */
    protected String getSessionIdFromCache(String callerService, String targetServiceUrl, String userName) {
        // Use userName or sessionId as key for session cache
        String keyForSessionCache = userName;
        if(!this.isUseSessionPerUser())
            keyForSessionCache = this.getClientSessionId();
        return sessionCache.getSessionId(callerService, targetServiceUrl, keyForSessionCache);
    }

    /**
     * Releases concurrent requests. 
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     */
    protected void releaseRequest(String callerService, String targetServiceUrl, String userName) {
        // Use userName or sessionId as key for session cache
        String keyForSessionCache = userName;
        if(!this.isUseSessionPerUser())
            keyForSessionCache = this.getClientSessionId();
        if(targetServiceUrl != null && keyForSessionCache != null)
            this.sessionCache.releaseRequest(callerService, targetServiceUrl, keyForSessionCache);
    }

    /**
     * Takes the session cookie value from response.
     * @param response
     * @return Returns session Id or null if not set.
     */
    private String resolveSessionId(CookieStore cookieStore, String cookieName) {
        // Get from cookie store
        for(Cookie cookie:cookieStore.getCookies()) {
            if(cookie.getName().equals(cookieName))
                return cookie.getValue();
        }
        return null;
    }

    /**
     * Invoked on error.
     */
    @Override
    @SuppressWarnings ("unchecked")
    public void handleFault(Message message) {
        log.debug("Handle fault: " + message);
        try {
            String targetServiceUrl = resolveTargetServiceUrl(message);
            Authentication auth = this.getAuthentication();
            String userName = (auth != null)?auth.getName():this.getAppClientUsername();
            this.releaseRequest(this.getCallerService(), targetServiceUrl, userName);
        } catch(Exception ex) {
            log.warn("Unable to release request in handleFault.", ex);
        }
        super.handleFault((T)message);
    }

    /**
     * Gets authentication object if available, otherwise returns null.
     * @return
     */
    protected Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.isAuthenticated())
            return authentication;
        else
            return null;
    }

    /**
     * Gets the connection from given message.
     * @param message
     * @return
     */
    private static HttpURLConnection resolveConnection(Message message) {
        HttpURLConnection conn = (HttpURLConnection)message.getExchange().getOutMessage().get(HTTPConduit.KEY_HTTP_CONNECTION);
        return conn;
    }

    /**
     * Resolves target service URL from message's endpoint address.
     * @param message
     * @return
     * @throws MalformedURLException
     */
    private String resolveTargetServiceUrl(Message message) throws MalformedURLException {
        String targetUrl = (String)message.get(Message.ENDPOINT_ADDRESS);
        if(targetUrl == null)
            targetUrl = (String)message.getExchange().getOutMessage().get(Message.ENDPOINT_ADDRESS);
        return CasFriendlyHttpClient.resolveTargetServiceUrl(targetUrl);
    }

    private void setSessionCookie(HttpURLConnection conn, String id) {
        String cookieHeader = conn.getRequestProperty(HEADER_COOKIE);
        List<HttpCookie> cookies = null;
        if(cookieHeader != null)
            cookies = HttpCookie.parse(cookieHeader);
        else
            cookies = new ArrayList<HttpCookie>();
        for(HttpCookie one:cookies) {
            if(this.getSessionCookieName().equals(one.getName())) {
                cookies.remove(one);
                break;
            }	
        }
        cookies.add(new HttpCookie(this.getSessionCookieName(), id));
        cookieHeader = toCookieString(cookies);
        log.debug("Injecting cached session id: " + id);
        conn.setRequestProperty(HEADER_COOKIE, cookieHeader);
    }

    private static String toCookieString(List<HttpCookie> cookies) {
        StringBuilder cookieString = new StringBuilder();
        for (HttpCookie httpCookie : cookies) {
            cookieString.append(cookieString.length() > 0 ? HEADER_COOKIE_SEPARATOR : "").append(httpCookie.getName()).append("=").append(httpCookie.getValue());
        }
        return cookieString.toString();
    }

    /**
     * True if authentication is set to be in dev mode, false otherwise.
     */
    public boolean isDevMode() {
        return "dev".equalsIgnoreCase(authMode);
    }

    /**
     * Gets client's sessionId from request context holder.
     * @return
     */
    private String getClientSessionId() {
        try {
            
//          return RequestContextHolder.currentRequestAttributes().getSessionId();
            return ((WebAuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails())
            .getSessionId();
        } catch(Exception ex) {
            log.error("Unable to get session ID for caching and 'useSessionPerUser' is true.", ex);
            return null;
        }

    }
    
    /**
     * Caller service is the distinctive name of the service. Used to keep sessions service specific if needed.  
     * @return
     */
    public String getCallerService() {
        return callerService;
    }

    public void setCallerService(String callerService) {
        this.callerService = callerService;
    }

    /**
     * Cookie name for sessionId.
     * @return
     */
    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    /**
     * Cache for sessions. Should be autowired by default.
     * @return
     */
    public CasFriendlyCache getCache() {
        return sessionCache;
    }

    public void setCache(CasFriendlyCache cache) {
        this.sessionCache = cache;
    }

    /**
     * Maximum wait time for concurrent requests. No blocking if <= 0.
     * @return
     */
    public long getMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    public void setMaxWaitTimeMillis(long maxWaitTimeMillis) {
        this.maxWaitTimeMillis = maxWaitTimeMillis;
    }

    /**
     * If session is required, it is proactively fetched in the outbound phase already. Otherwise only 
     * fetched in case of /cas/login redirect.
     * @return
     */
    public boolean isSessionRequired() {
        return sessionRequired;
    }

    public void setSessionRequired(boolean sessionRequired) {
        this.sessionRequired = sessionRequired;
    }

    public String getAppClientUsername() {
        return appClientUsername;
    }

    public void setAppClientUsername(String appClientUsername) {
        if(appClientUsername != null && appClientUsername.length() > 0)
            this.appClientUsername = appClientUsername;
        else
            this.appClientUsername = null;
    }

    public String getAppClientPassword() {
        return appClientPassword;
    }

    public void setAppClientPassword(String appClientPassword) {
        if(appClientPassword != null && appClientPassword.length() > 0)
            this.appClientPassword = appClientPassword;
        else
            this.appClientPassword = null;
    }

    public boolean isUseBasicAuthentication() {
        return useBasicAuthentication;
    }

    public void setUseBasicAuthentication(boolean useBasicAuthentication) {
        this.useBasicAuthentication = useBasicAuthentication;
    }

    /**
     * If true, tries to block concurrent requests to the same target with same user.
     * @return
     */
    public boolean isUseBlockingConcurrent() {
        return useBlockingConcurrent;
    }

    public void setUseBlockingConcurrent(boolean useBlockingConcurrent) {
        this.useBlockingConcurrent = useBlockingConcurrent;
    }

    /**
     * Cookie name for CAS.
     * @return
     */
    public String getCasSessionCookieName() {
        return casSessionCookieName;
    }

    public void setCasSessionCookieName(String casSessionCookieName) {
        this.casSessionCookieName = casSessionCookieName;
    }

    /**
     * Uses session per user if true, otherwise session per client session. If true, all services
     * must be stateless and sessionId must only be used for authentication. True by default.
     * @return
     */
    public boolean isUseSessionPerUser() {
        return useSessionPerUser;
    }

    public void setUseSessionPerUser(boolean useSessionPerUser) {
        this.useSessionPerUser = useSessionPerUser;
    }

    /**
     * Authmode is either dev or cas. Dev is used for development purposes only and must not
     * be used in production. Default is cas (or null).
     * @return
     */
    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

}
