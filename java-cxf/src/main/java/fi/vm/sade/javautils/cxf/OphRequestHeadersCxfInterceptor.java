package fi.vm.sade.javautils.cxf;

import java.net.HttpURLConnection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;

/**
 * Interceptor for adding Caller-Id header to all requests. Interceptor must be registered for all 
 * services, in xml like following:
 *
 * <bean id="ophRequestHeaders" class="fi.vm.sade.javautils.cxf.OphRequestHeadersCxfInterceptor">
 *   <property name="clientSubSystemCode" value="viestintapalvelu.ryhmasahkoposti-service.backend"/>
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

    private String clientSubSystemCode = null;

    public OphRequestHeadersCxfInterceptor() {
        // Intercept before sending
        super(Phase.PRE_PROTOCOL);
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
        
        if(clientSubSystemCode != null) {
            conn.setRequestProperty("clientSubSystemCode", clientSubSystemCode);
        }
        else {
            throw new RuntimeException("Missing clientSubSystemCode. Set clientSubSystemCode for OphRequestHeadersCxfInterceptor.");
        }
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

    public String getClientSubSystemCode() {
        return clientSubSystemCode;
    }

    public void setClientSubSystemCode(String clientSubSystemCode) {
        this.clientSubSystemCode = clientSubSystemCode;
    }

}
