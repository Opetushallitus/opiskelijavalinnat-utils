package fi.vm.sade.authentication.cas;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CAS specific redirecting strategy to walk through CAS login transparently.
 * TGT = Ticket Granting Ticket
 * ST = Service Ticket (one timer)
 * PGT = Proxy Granting Ticket
 * PT = Proxy Ticket
 * SESSION = Service specific session (e.g. JSESSIONID) 
 * @author Jouni Stam
 *
 */
public class CasRedirectStrategy implements RedirectStrategy {

    private static final Logger log = LoggerFactory.getLogger(CasRedirectStrategy.class);

    // REST interface for CAS services
    public static final String CAS_TICKET_URL = "/cas/v1/tickets";
    public static final String CAS_PROXYTICKET_URL = "/cas/proxy";

    public static final String ATTRIBUTE_CACHE = "cache";
    public static final String ATTRIBUTE_PRINCIPAL = "principal";
    public static final String ATTRIBUTE_LOGIN = "login";
    public static final String ATTRIBUTE_PASSWORD = "password";
    public static final String ATTRIBUTE_CAS_TGT = "casTgt";
    public static final String ATTRIBUTE_ORIGINAL_REQUEST = "originalRequest";
    public static final String ATTRIBUTE_ORIGINAL_REQUEST_PARAMS = "originalRequestParams";
    public static final String ATTRIBUTE_SERVICE_URL = "serviceUrl";
    public static final String ATTRIBUTE_CAS_REQUEST_STATE = "casRequestState";
    public static final String ATTRIBUTE_CAS_SERVICE_TICKET = "casServicetTicket";
    public static final String ATTRIBUTE_CAS_AUTHENTICATE_ONLY = "authenticateOnly";

    // Pre authenticate state
    public static final String CAS_REQUEST_STATE_PREAUTH = "PRE";
    // First state when redirected to request for TGT
    public static final String CAS_REQUEST_STATE_TGT = "TGT";
    // Second state when redirected to request for ST with TGT
    public static final String CAS_REQUEST_STATE_ST = "ST";
    // Final CAS state when ready to request for SESSION with ST
    public static final String CAS_REQUEST_STATE_SESSION = "SESSION";

    /**
     * Takes care of CAS redirects transparent to the actual request.
     * Only comes here is isRedirect() is true.
     */
    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response,
            HttpContext context) throws ProtocolException {
        // Get the redirect URL from location header or use service URL (when CAS state ST)
        String location = null;
        Header redirectLocation = response.getFirstHeader("Location");
        if(redirectLocation != null) {
            location = redirectLocation.getValue();
        } else {
            // Use service URL
//            location = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);
        }
        log.debug("Redirect location is: " + location);
        try {
            // Has location unless "redirecting" to original source
            String path = "";
            URL url = null;
            if(location != null) {
                url = new URL(location);
                path = url.getPath();
            }
            if(path.startsWith("/cas/login") || CasRedirectStrategy.CAS_REQUEST_STATE_PREAUTH.equals(context.getAttribute(ATTRIBUTE_CAS_REQUEST_STATE))) {
                String service = null;
                // Only if actually redirected
                if(location != null) {
                    // Case redirect to /cas/login ("user" must authenticate)
                    service = resolveService(location);

                    // Set the service URL to context
                    if(service != null)
                        CasFriendlyHttpClient.setTargetServiceUrl(context, service);

                } else {
                    service = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);
                }

                // TGT can be used instead of username+password
                String tgt = 
                        (String)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_TGT);
                if(tgt != null) {
                    // Use existing TGT to get ST
                    log.debug("Using TGT from HttpContext: " + tgt);
                    String tgtUrl = resolveCasTicketUrl(url) + "/" + tgt;
                    // Continue with session ticket request
                    return createSTRequest(request, response, context, tgtUrl);
                } 

                // Figure out if PGT is available and use that (expects principal in HttpContext)
                String casPgtSt = resolveProxyGrantingTicket(context, service);
                if(casPgtSt != null)
                    log.debug("Found proxy granting ticket: " + casPgtSt);
                else
                    log.debug("Not able to get proxy ticket (" + CasRedirectStrategy.ATTRIBUTE_PRINCIPAL + ")");

                if(casPgtSt != null) {
                    // Use ST by PGT
//                    return createSTRequestWithPGT(request, response, context, url, casPgt);
                    // Set ST to context
                    context.setAttribute(ATTRIBUTE_CAS_SERVICE_TICKET, casPgtSt);
                    // Continue to get session open for service
                    return createSessionRequest(request, response, context);
                } else {
                    // Last resort, use login and password from HttpContext
                    String login = (String)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_LOGIN);
                    String password = (String)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_PASSWORD);
                    return createTGTRequest(request, response, context, url, login, password);
                }

            } else if(path.startsWith("/cas/v1/tickets")) {

                // Request for service ticket
                return createSTRequest(request, response, context, location);

            } else if(context.getAttribute(ATTRIBUTE_CAS_SERVICE_TICKET) != null) {

                // We have service ticket and continue to get session 
                return createSessionRequest(request, response, context);

            } else if(CasRedirectStrategy.CAS_REQUEST_STATE_SESSION.equals(context.getAttribute(ATTRIBUTE_CAS_REQUEST_STATE))) {

                // Continue with the original request
                return createOriginalRequest(request, response, context);

            } else {
                // Not interesting, continue as any other redirect
            }
        } catch (MalformedURLException e) {
            log.warn("Failed to process redirect as CAS redirect. Stopping redirect.", e);
            return null;
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to process redirect as CAS redirect. Stopping redirect.", e);
            return null;
        }

        // The "normal" case, just continues as a normal redirect
        return new HttpGet(location);
    }

    /**
     * Response is considered to be redirect if Location header is given
     * or (CAS_TICKET_REQUEST && 200).
     */
    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response,
            HttpContext context) throws ProtocolException {
        
        Boolean authenticateOnly = (Boolean)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_AUTHENTICATE_ONLY);
        if(authenticateOnly == null)
            authenticateOnly = false;
        // Continue with original request after ST, only if not authenticateOnly request
        if(CasRedirectStrategy.CAS_REQUEST_STATE_ST.equals(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE)) &&
                response.getStatusLine().getStatusCode() == 200) {
            // We are redirecting only because of CAS process (response code is 200) not 301
            try {
                // Read ST from response body
                String serviceTicket = EntityUtils.toString(response.getEntity());
                log.debug("Service ticket is: " + serviceTicket);
                // Reset
                // Deciding redirection based on service ticket, state is not needed
                context.removeAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE);
                // Continue back to service with serviceTicket
                context.setAttribute(ATTRIBUTE_CAS_SERVICE_TICKET, serviceTicket);
                // Redirecting
                return true;
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            log.warn("CAS redirecting strategy confused. Cancelling further redirects.");
            return false;
        } else if(!authenticateOnly && CasRedirectStrategy.CAS_REQUEST_STATE_SESSION.equals(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE)) &&
                response.getStatusLine().getStatusCode() == 200) {
            // "Redirecting" back to original request
            return true;
        } else if(authenticateOnly && context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE) == null &&
                response.getStatusLine().getStatusCode() == 401) {
            // CAS login required, same situation as /cas/login redirect response
            // PRE auth state
            context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE, CasRedirectStrategy.CAS_REQUEST_STATE_PREAUTH);
            return true;
        } else if(CasRedirectStrategy.CAS_REQUEST_STATE_TGT.equals(context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE)) &&
                response.getStatusLine().getStatusCode() == 201) {
            // TGT phase
            return true;
        }else {
            // Otherwise only interested in responses with location header
            Header redirectLocation = response.getFirstHeader("Location");

            // Follow redirect only if not authenticateOnly request
            if (redirectLocation != null && !authenticateOnly) {
                // Continue with redirect
                return true;
            } else
                return false;
        }
    }

    /**
     * Creates TGT request to /cas/v1/tickets.
     * @param request
     * @param response
     * @param context
     * @param locationUrl
     * @return
     * @throws UnsupportedEncodingException 
     * @throws MalformedURLException 
     * @throws Exception
     */
    public static HttpUriRequest createTGTRequest(HttpRequest request, HttpResponse response,
            HttpContext context, URL locationUrl, String login, String password) throws UnsupportedEncodingException, MalformedURLException {

        String service = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);

        // Gets the /cas/v1/tickets full URL
        String url = null;
        if(locationUrl != null)
            url = resolveCasTicketUrl(locationUrl);
        else
            url = resolveCasTicketUrl(new URL(service));

        HttpPost casRequest = new HttpPost(url);

        // Set state attribute
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE, CasRedirectStrategy.CAS_REQUEST_STATE_TGT);
        log.debug("CAS state set to: " + context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE));

        ArrayList<BasicNameValuePair> postParameters = new ArrayList<BasicNameValuePair>();
        // Login with service's own credentials
        // TODO Do we need URLEncoding for the values?
        postParameters.add(new BasicNameValuePair("service", service));
        postParameters.add(new BasicNameValuePair("username", login));
        postParameters.add(new BasicNameValuePair("password", password));
        casRequest.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

        log.debug("Authenticating to: " + service + " using login: " + login);

        return casRequest;
    }

    /**
     * Creates ST request with PGT /cas/proxy.
     * @param request
     * @param response
     * @param context
     * @param locationUrl
     * @return
     * @throws UnsupportedEncodingException 
     * @throws Exception
     */
    public static HttpUriRequest createSTRequestWithPGT(HttpRequest request, HttpResponse response,
            HttpContext context, URL locationUrl, String casPgt) throws UnsupportedEncodingException {

        String service = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);

        String port = ((locationUrl.getPort() > 0)?(":" + locationUrl.getPort()):"");
        String url = locationUrl.getProtocol() + "://" + 
                locationUrl.getHost() + port + CAS_PROXYTICKET_URL;
        HttpPost casRequest = new HttpPost(url);

        // Set state attribute
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE, CasRedirectStrategy.CAS_REQUEST_STATE_ST);
        log.debug("CAS state set to: " + context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE));

        ArrayList<BasicNameValuePair> postParameters = new ArrayList<BasicNameValuePair>();
        // Use PGT
        postParameters.add(new BasicNameValuePair("targetService", service));
        postParameters.add(new BasicNameValuePair("pgt", casPgt));
        casRequest.setEntity(new UrlEncodedFormEntity(postParameters));

        return casRequest;
    }

    /**
     * Creates ST request back to /cas/v1/tickets with service parameter and TGT.
     * Expects locationHeader to be like https://localhost:8443/cas/
     * @param request
     * @param response
     * @param context
     * @param locationUrl
     * @return
     * @throws UnsupportedEncodingException 
     * @throws Exception
     */
    public static HttpUriRequest createSTRequest(HttpRequest request, HttpResponse response,
            HttpContext context, String locationHeader) throws UnsupportedEncodingException {
        String service = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);
        HttpPost casRequest = new HttpPost(locationHeader);

        String tgt = StringUtils.substringAfterLast(locationHeader, "/");
        if(tgt != null) {
            context.setAttribute(ATTRIBUTE_CAS_TGT, tgt);
            log.debug("Stored TGT to context: " + tgt);
        }

        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE, CasRedirectStrategy.CAS_REQUEST_STATE_ST);
        log.debug("CAS state set to: " + context.getAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE));

        ArrayList<BasicNameValuePair> postParameters = new ArrayList<BasicNameValuePair>();
        postParameters.add(new BasicNameValuePair("service", service));
        casRequest.setEntity(new UrlEncodedFormEntity(postParameters));
        return casRequest;
    }

    /**
     * Creates a request to service with ST to get a session.
     * @param request
     * @param response
     * @param context
     * @return
     * @throws Exception
     */
    public static HttpUriRequest createSessionRequest(HttpRequest request, HttpResponse response,
            HttpContext context) {
        log.debug("Setting CAS state to: " + CasRedirectStrategy.CAS_REQUEST_STATE_SESSION);
        context.setAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE, CasRedirectStrategy.CAS_REQUEST_STATE_SESSION);

        String serviceUrl = (String)context.getAttribute(ATTRIBUTE_SERVICE_URL);
        String serviceTicket = (String)context.getAttribute(ATTRIBUTE_CAS_SERVICE_TICKET);
        // Reset
        context.removeAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_SERVICE_TICKET);
        // Create URL
        if(serviceUrl.contains("?"))
            serviceUrl = serviceUrl + "&ticket=" + serviceTicket;
        else
            serviceUrl = serviceUrl + "?ticket=" + serviceTicket;

        HttpGet sessionRequest = new HttpGet(serviceUrl);

        return sessionRequest;
    }

    /**
     * Recreates the original request. Required for POST/PUT especially to keep the original
     * method and parameters in place.
     * @param request
     * @param response
     * @param context
     * @return
     * @throws UnsupportedEncodingException 
     * @throws Exception
     */
    public static HttpUriRequest createOriginalRequest(HttpRequest request, HttpResponse response,
            HttpContext context) throws UnsupportedEncodingException {
        log.debug("CAS process done. Continuing with the original request.");
        context.removeAttribute(CasRedirectStrategy.ATTRIBUTE_CAS_REQUEST_STATE);

        HttpRequest origRequest = (HttpRequest)context.getAttribute(ATTRIBUTE_ORIGINAL_REQUEST);
        String url = resolveUrl(origRequest);
        HttpUriRequest uriRequest = null;
        if(origRequest.getRequestLine().getMethod().contains("POST")) {
            uriRequest = new HttpPost(url);
            EntityEnclosingRequestWrapper origPost = (EntityEnclosingRequestWrapper)origRequest;
            ((HttpPost)uriRequest).setEntity(origPost.getEntity());
        } else if(origRequest.getRequestLine().getMethod().contains("GET")) {
            uriRequest = new HttpGet(url);
        } else if(origRequest.getRequestLine().getMethod().contains("DELETE")) {
            uriRequest = new HttpDelete(url);
        } else if(origRequest.getRequestLine().getMethod().contains("PUT")) {
            uriRequest = new HttpPut(url);
            EntityEnclosingRequestWrapper origPut = (EntityEnclosingRequestWrapper)origRequest;
            ((HttpPut)uriRequest).setEntity(origPut.getEntity());
        }
        return uriRequest;
    }

    /**
     * Resolves service from Location header from CAS request.
     * @param locationHeader
     * @return
     * @throws UnsupportedEncodingException
     */
    private static String resolveService(String locationHeader) throws UnsupportedEncodingException {
        // Set if possible
        if(locationHeader.contains("service=")) {
            String service = StringUtils.substringAfter(locationHeader, "service=");
            service = URLDecoder.decode(service,"UTF-8");
            // Get rid of session id in URL
            if(service.indexOf(";") > 0)
                service = StringUtils.substringBeforeLast(service, ";");
            return service;
        } else
            return null;
    }

    /**
     * Resolves URL from request.
     * @param request
     * @return
     */
    protected static String resolveUrl(HttpRequest request) {
        String protocol = request.getProtocolVersion().getProtocol().toLowerCase();
        String url = protocol + 
                "://" + request.getFirstHeader("Host").getValue() +
                request.getRequestLine().getUri();
        return url;
    }

    /**
     * Resolves proxy granting ticket from context that can be used for requesting 
     * new serviceticket on behalf of the user without login and password.
     * @param context
     * @return
     */
    private static String resolveProxyGrantingTicket(HttpContext context, String service) {
        AttributePrincipal principal = 
                (AttributePrincipal)context.getAttribute(CasRedirectStrategy.ATTRIBUTE_PRINCIPAL);
        if(principal != null) {
            return principal.getProxyTicketFor(service);
        } else
            return null;
    }

    /**
     * Resolves CAS tickets REST URL based on requested URL.
     * @param locationUrl
     * @return
     */
    private static String resolveCasTicketUrl(URL locationUrl) {
        String port = ((locationUrl.getPort() > 0)?(":" + locationUrl.getPort()):"");
        return locationUrl.getProtocol() + "://" + 
        locationUrl.getHost() + port + CAS_TICKET_URL;
    }
}
