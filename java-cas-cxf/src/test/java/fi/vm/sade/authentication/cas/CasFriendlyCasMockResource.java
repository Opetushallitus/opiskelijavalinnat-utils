package fi.vm.sade.authentication.cas;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

/**
 * @author Jouni Stam
 */
@Path("/cas")
public class CasFriendlyCasMockResource {

    public final static String fakeTgt = "TGT-whatever";
    public final static String fakeSt = "ST-1-FFDFHDSJKHSDFJKSDHFJKRUEYREWUIFSD2132";
    
    @Path("/v1/tickets")
    @POST
    @Produces("text/plain")
    public Response requestTgt(@Context HttpServletRequest request, 
            @FormParam (value="username") String userName,
            @FormParam (value="password") String password) {
        try {
            String nextUrl = "/cas/v1/tickets/" + fakeTgt;
            if(!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(password)
                    && !"deny".equals(userName)) {
                return Response
                        .status(201)
                        .location(new URI(CasFriendlyCxfInterceptorTest.getUrl(nextUrl)))
                        .build();
            } else
                return Response.status(400).build();
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        }
    }
    
    @Path("/v1/tickets/{tgtId}")
    @POST
    @Produces("text/plain")
    public Response requestSt(@Context HttpServletRequest request, 
            @FormParam (value="service") String service,
            @PathParam (value="tgtId") String tgtId) {
        if(tgtId != null && tgtId.equals(fakeTgt)) {
            return Response
                    .ok(fakeSt)
                    .build();
        } else
            return Response.status(400).build();
    }
}
