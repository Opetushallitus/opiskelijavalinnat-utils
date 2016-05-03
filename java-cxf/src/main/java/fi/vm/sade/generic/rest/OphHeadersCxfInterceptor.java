package fi.vm.sade.generic.rest;

import java.net.HttpURLConnection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor for adding Caller-Id header to all requests. Interceptor must be registered for all 
 * services, in xml like following:
 * <bean id="callerIdInterceptor" class="fi.vm.sade.generic.rest.OphHeadersCxfInterceptor">
 *   <property name="clientSubSystemCode" value="${caller.id}"/>
 * </bean>
 *
 *  <cxf:bus>
 *      <cxf:outInterceptors>
 *          <ref bean="callerIdInterceptor"/>
 *     </cxf:outInterceptors>
 *  </cxf:bus>
 */
public class OphHeadersCxfInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {

    private static final Logger log = LoggerFactory.getLogger(OphHeadersCxfInterceptor.class);

    private String clientSubSystemCode = null;

    public OphHeadersCxfInterceptor() {
        // Intercept in receive phase
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
        log.debug("Inbound message intercepted for Caller-Id insertion.");

        HttpURLConnection conn = resolveConnection(message);
        
        if(clientSubSystemCode != null)
            conn.setRequestProperty("clientSubSystemCode", clientSubSystemCode);
        else
            log.warn("Missing Caller-Id clientSubSystemCode. Set clientSubSystemCode for OphHeadersCxfInterceptor.");
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
        HttpURLConnection conn = (HttpURLConnection)message.getExchange().getOutMessage().get(HTTPConduit.KEY_HTTP_CONNECTION);
        return conn;
    }

    public String getClientSubSystemCode() {
        return clientSubSystemCode;
    }

    public void setClientSubSystemCode(String clientSubSystemCode) {
        this.clientSubSystemCode = clientSubSystemCode;
    }

}
