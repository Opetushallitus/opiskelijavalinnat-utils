package fi.vm.sade.authentication.cas;

import java.io.IOException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example Java client to authenticate against CAS using REST services.
 * Please ensure you have followed the necessary setup found on the <a
 * href="http://www.ja-sig.org/wiki/display/CASUM/RESTful+API">wiki</a>.
 *
 * @author Antti Salonen
 * @author <a href="mailto:jieryn@gmail.com">jesse lauren farinacci</a>
 * @since 3.4.2
 */
public final class CasClient {
    private static final Logger logger = LoggerFactory.getLogger(CasClient.class);

    public static final String CAS_URL_SUFFIX = "/v1/tickets";
    public static final String SERVICE_URL_SUFFIX = "/j_spring_cas_security_check";
        
    private CasClient() {
        // static-only access
    }

    /** get cas service ticket, throws runtime exception if fails */
    public static String getTicket(String server, final String username, final String password, String service) {
        return getTicket(server, username, password, service, true);
    }

    /** get cas service ticket, throws runtime exception if fails */
    public static String getTicket(String server, final String username, final String password, String service, boolean addSuffix) {
    	
    	logger.debug("getTicket for server:{}, username:{}, service::{} ", new Object[]{server, username, service});
    	
    	server = checkUrl(server, CAS_URL_SUFFIX);
        if(addSuffix)
    	    service = checkUrl(service, SERVICE_URL_SUFFIX);

    	notNull(server, "server must not be null");
        notNull(username, "username must not be null");
        notNull(password, "password must not be null");
        notNull(service, "service must not be null");

        String ticketGrantingTicket = getTicketGrantingTicket(server, username, password);
        return getServiceTicket(server, ticketGrantingTicket, service);
    }

    private static String getServiceTicket(final String server, final String ticketGrantingTicket, final String service) {

        logger.info("getServiceTicket: server:'{}', ticketGrantingTicket:'{}', service:'{}'", new Object[]{server, ticketGrantingTicket, service});

        final HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(server + "/" + ticketGrantingTicket);
        post.setRequestBody(new NameValuePair[]{
                new NameValuePair("service", service)});

        try {
            client.executeMethod(post);
            
            printTraceResponse(post, client);
            
            final String response = post.getResponseBodyAsString();
            switch (post.getStatusCode()) {
                case HttpStatus.SC_OK:
                    logger.info("serviceTicket found: {}", response);
                    return response;
                default:
                    logger.warn("Invalid response code ({}) from CAS server!", post.getStatusLine());
                    logger.info("Response (1k): " + response.substring(0, Math.min(1024, response.length())));
                    throw new RuntimeException("failed to get CAS service ticket, response code: "+post.getStatusLine()+", server: "+server+", tgt: "+ticketGrantingTicket+", service: "+service);
            }
        } catch (final IOException e) {
            throw new RuntimeException("failed to get CAS service ticket, server: "+server+", tgt: "+ticketGrantingTicket+", service: "+service+", cause: "+e, e);
        } finally {
            post.releaseConnection();
        }
    }

    public static String getTicketGrantingTicket(final String server, final String username, final String password) {

        logger.info("getTicketGrantingTicket: server:'{}', user:'{}'", new Object[]{server, username});

        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(server);
        post.setRequestBody(new NameValuePair[]{
                new NameValuePair("username", username),
                new NameValuePair("password", password)});
        
        //username=battags&password=password&additionalParam1=paramvalue
        
        /*
         Response example: 
         
		Status : 201
		URI: http://centosx/cas/v1/tickets
		Request Headers: 4
		  User-Agent = Jakarta Commons-HttpClient/3.1
		  Host = centosx
		  Content-Length = 40
		  Content-Type = application/x-www-form-urlencoded
		Response Path: /cas/v1/tickets
		Response Headers: 9
		  Date = Fri, 13 Dec 2013 00:12:37 GMT
		  Server = Noelios-Restlet-Engine/1.1..1
		  Location = http://centosx/cas/v1/tickets/TGT-14-VW7KiAZdkqqO27ysCvd9rArUfnk0SLkXdifMzywUtlI4A7mdgg-cas.centosx
		  Accept-Ranges = bytes
		  Content-Type = text/html;charset=ISO-8859-1
		  Content-Length = 430
		  Cache-Control = max-age=0, public
		  Expires = Fri, 13 Dec 2013 00:12:37 GMT
		  Connection = close
		Cookies: 0
		Response Text:
		<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN"><html>
		<head><title>201 The request has been fulfilled and resulted in a new resource being created</title></head>
		<body><h1>TGT Created</h1>
		<form action="http://centosx/cas/v1/tickets/TGT-14-VW7KiAZdkqqO27ysCvd9rArUfnk0SLkXdifMzywUtlI4A7mdgg-cas.centosx" method="POST">
		Service:<input type="text" name="service" value=""><br>
		<input type="submit" value="Submit"></form>
		</body></html>
         */
        
        try {
            client.executeMethod(post);
            
            printTraceResponse(post, client);
            
            // final String response = post.getResponseBodyAsString();
            switch (post.getStatusCode()) {
                
            	case HttpStatus.SC_CREATED: {// 201
                	Header locationHeader = post.getResponseHeader("Location");
                	
                	logger.debug("locationHeader: "+locationHeader);
                	
                	if(locationHeader!=null){
                		String responseLocation = locationHeader.getValue();
                		String ticket = StringUtils.substringAfterLast(responseLocation, "/");
                	
                		logger.debug("-> ticket: "+ticket);
                		
                		return ticket;
                		
                	}
                	//final Matcher matcher = Pattern.compile(".*action=\".*/(.*?)\".*").matcher(response);
                    //if (matcher.matches()) {
                    //    return matcher.group(1);
                    //}
                    throw new RuntimeException("Successful ticket granting request, but no ticket found! server: "+server+", user: "+username);
                }
            	default:
                    throw new RuntimeException("Invalid response code from CAS server: "+post.getStatusLine()+", server: "+server+", user: "+username);
            }
        } catch (final IOException e) {
            throw new RuntimeException("error getting TGT, server: "+server+", user: "+username+", exception: "+e, e);
        } finally {
            post.releaseConnection();
        }
    }

    private static void notNull(final Object object, final String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static String makeServiceUrl(String url) {
    	return checkUrl(url, SERVICE_URL_SUFFIX);
    }
        
    private static String checkUrl(String url, final String suffix) {
    	logger.debug("url: "+url);
    	url = url.trim();
    	url = url.endsWith("/")?url.substring(0,url.length()-1):url;
    	if(!StringUtils.endsWith(url, suffix)){
    		url += suffix;
        }
    	logger.debug("-> fixed url: "+url);
    	return url;
    }
    
    private static void printTraceResponse(final HttpMethodBase method, final HttpClient client) throws IOException{	
		
    	if(!logger.isTraceEnabled()) return;
    	
		String responseTxt = method.getResponseBodyAsString();
				
		logger.debug("\n<cas-http-response>");
		logger.debug("Status : "+method.getStatusCode());
		logger.debug("URI: "+method.getURI());
		logger.debug("Request Headers: "+method.getRequestHeaders().length);
        for(Header h : method.getRequestHeaders()){
        	logger.debug("  "+h.getName()+" = "+h.getValue()); 
        }
        
        logger.debug("Response Path: "+method.getPath());
        logger.debug("Response Headers: "+method.getResponseHeaders().length);
        for(Header h : method.getResponseHeaders()){
        	logger.debug("  "+h.getName()+" = "+h.getValue()); 
        }
        
        logger.debug("Cookies: "+client.getState().getCookies().length);
        for(org.apache.commons.httpclient.Cookie c : client.getState().getCookies()){
        	logger.debug("  "+c.getName()+" = "+c.getValue()); 
        }
        logger.debug("Response Text: ");
        logger.debug(responseTxt);
        logger.debug("</cas-http-response>\n");
	}


}