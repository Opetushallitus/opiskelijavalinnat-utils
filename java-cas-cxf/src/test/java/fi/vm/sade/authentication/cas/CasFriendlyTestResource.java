package fi.vm.sade.authentication.cas;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.sade.authentication.cas.CasFriendlyHttpClient;

/**
 * @author Jouni Stam
 */
@Path("/casfriendly")
public class CasFriendlyTestResource {

    private static Map<String, Integer> testCaseCounts = new HashMap<String, Integer>();
    
    
    @Path("/protected2/test")
    @GET
    @Produces("text/plain")
    public Response protectedTestGet(@Context HttpServletRequest request, 
            @HeaderParam (value="Testcase-Id") String testCaseId,
            @QueryParam (value="ticket") String ticket) {
        return this.protectedGet(request, testCaseId, ticket);
    }
    
    @Path("/protected")
    @GET
    @Produces("text/plain")
    public Response protectedGet(@Context HttpServletRequest request, 
            @HeaderParam (value="Testcase-Id") String testCaseId,
            @QueryParam (value="ticket") String ticket) {
        try {
            HttpSession session = request.getSession(false);
            if(session == null)
                return Response.status(302).location(new URI(CasFriendlyCxfInterceptorTest.getUrl(createCasLocation(request)))).build();
            else {
                session = request.getSession(true);
                return Response
                    .ok("ok " + getAndIncreaseTestCaseCount(request.getRequestURI() + testCaseId))
                    .build();
            }
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/protected2/test")
    @POST
    @Produces("text/plain")
    public Response protectedTestPost(@Context HttpServletRequest request,
            @FormParam (value="TESTNAME") String name,
            @HeaderParam (value="Testcase-Id") String testCaseId,
            @QueryParam (value="ticket") String ticket) {
        return this.protectedPost(request, name, testCaseId, ticket);
    }
    
    @Path("/protected")
    @POST
    @Produces("text/plain")
    public Response protectedPost(@Context HttpServletRequest request,
            @FormParam (value="TESTNAME") String name,
            @HeaderParam (value="Testcase-Id") String testCaseId,
            @QueryParam (value="ticket") String ticket) {
        try {
            if(!"TESTVALUE".equals(name)) {
                System.err.println("BODY MISSING!");
                throw new Exception("Post body missing.");
            }
            HttpSession session = request.getSession(false);
            if(session == null)
                return Response.status(302).location(new URI(CasFriendlyCxfInterceptorTest.getUrl(createCasLocation(request)))).build();
            else {
                session = request.getSession(true);
                return Response
                    .ok("ok " + getAndIncreaseTestCaseCount(request.getRequestURI() + testCaseId))
                    .build();
            }
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
    
    @Path("/unprotected")
    @GET
    @Produces("text/plain")
    public Response unprotectedGet(@Context HttpServletRequest request,
            @HeaderParam (value="Testcase-Id") String testCaseId) {
        HttpSession session = request.getSession(true);
        return Response
                .ok("ok " + getAndIncreaseTestCaseCount(request.getRequestURI() + testCaseId))
                .build();
    }

    @Path("/j_spring_cas_security_check")
    @GET
    @Produces("text/plain")
    public Response casCheckGet(@Context HttpServletRequest request, 
            @HeaderParam (value="Testcase-Id") String testCaseId,
            @QueryParam (value="ticket") String ticket) throws URISyntaxException {
        if(ticket == null || !ticket.equals(CasFriendlyCasMockResource.fakeSt))
            return Response.status(401).build();
        else {
            HttpSession session = request.getSession(true);
            // TODO CAS actually redirects to the original service stored in session
            // This is to simulate the "worst" case (possibility to cause endless loop)
            return Response.status(302).location(
                    new URI(getFullURL(request))).build();
        }
    }

    private static String createCasLocation(HttpServletRequest request) throws UnsupportedEncodingException, MalformedURLException {
        String fullUrl = getFullURL(request);
        fullUrl = CasFriendlyHttpClient.resolveTargetServiceUrl(fullUrl);
        return "/cas/login?service=" + URLEncoder.encode(fullUrl, "UTF-8");
    }
    
    private static String getFullURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }
    
    private static int getAndIncreaseTestCaseCount(String testCaseId) {
        Integer value = testCaseCounts.get(testCaseId);
        if(value == null)
            value = new Integer(0);
        value++;
        testCaseCounts.put(testCaseId, value);
        return value;
    }
}
