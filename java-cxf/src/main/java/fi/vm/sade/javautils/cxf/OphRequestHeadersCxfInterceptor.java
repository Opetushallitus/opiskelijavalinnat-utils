package fi.vm.sade.javautils.cxf;

import java.net.HttpURLConnection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.util.Assert;

/**
 * Interceptor for adding Caller-Id header to all requests. Interceptor must be registered for all 
 * services, in xml like following:
 *
 * <bean id="ophRequestHeaders" class="fi.vm.sade.javautils.cxf.OphRequestHeadersCxfInterceptor">
 *   <constructor-arg index="0" value="1.2.246.562.10.00000000001.ryhmasahkoposti-service.backend"/>
 * </bean>
 *
 *  <cxf:bus>
 *      <cxf:outInterceptors>
 *          <ref bean="ophRequestHeaders"/>
 *     </cxf:outInterceptors>
 *  </cxf:bus>
 *
 *  <jaxrs-client:client>
 *      <jaxrs-client:outInterceptors>
 *          <ref bean="ophRequestHeaders"/>
 *      </jaxrs-client:outInterceptors>
 *  </jaxrs-client:client>
 */
public class OphRequestHeadersCxfInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {
    private final String callerId;

    public OphRequestHeadersCxfInterceptor(String callerId) {
        // Intercept before sending
        super(Phase.PRE_PROTOCOL);
        Assert.notNull(callerId, "Missing callerId. Set callerId for OphRequestHeadersCxfInterceptor.");
        this.callerId = callerId;
    }

    /**
     * Invoked on in- and outbound (if interceptor is registered for both, which makes no sense). 
     */
    public void handleMessage(Message message) throws Fault {
        this.handleOutbound(message.getExchange().getOutMessage());
    }

    /**
     * Invoked on outbound (request).
     * @param message
     * @throws Fault
     */
    public void handleOutbound(Message message) throws Fault {
        HttpURLConnection conn = resolveConnection(message);
        conn.setRequestProperty("Caller-Id", callerId);
        conn.setRequestProperty("CSRF", "CSRF");
        String cookieString  = conn.getRequestProperty("Cookie");
        if(cookieString != null) {
            conn.setRequestProperty("Cookie", cookieString + ";CSRF=CSRF");
        } else {
            conn.setRequestProperty("Cookie", "CSRF=CSRF");
        }
    }

    /**
     * Resolve connection from message.
     */
    private static HttpURLConnection resolveConnection(Message message) {
        return (HttpURLConnection)message.getExchange().getOutMessage().get(HTTPConduit.KEY_HTTP_CONNECTION);
    }

    public String getCallerId() {
        return callerId;
    }
}
