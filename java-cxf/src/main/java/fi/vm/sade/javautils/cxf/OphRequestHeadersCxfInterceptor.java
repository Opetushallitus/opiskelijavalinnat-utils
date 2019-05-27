package fi.vm.sade.javautils.cxf;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (callerId == null) {
            throw new IllegalArgumentException("Missing callerId. Set callerId for OphRequestHeadersCxfInterceptor.");
        }
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
        addHeader(message, "Caller-Id", callerId);
        addHeader(message, "CSRF", "CSRF");
        appendToHeader(message, "Cookie", "CSRF=CSRF", "; ");
    }

    private void addHeader(Message message, String name, String value) {
        resolveHeaders(message).put(name, Collections.singletonList(value));
    }

    private void appendToHeader(Message message, String headerName, String valueToAppend, String separator) {
        Map<String, List<String>> headers = resolveHeaders(message);
        List<String> originalValues = headers.getOrDefault(headerName, new LinkedList<>());
        if (originalValues.isEmpty()) {
            headers.put(headerName, Collections.singletonList(valueToAppend));
            return;
        }
        headers.put(headerName, originalValues.stream().map(original -> {
            if (original == null) {
                return valueToAppend;
            } else {
                return original + separator + valueToAppend;
            }
        }).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> resolveHeaders(Message message) {
        Map<String, List<String>> outHeaders = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (outHeaders == null) {
            outHeaders = new HashMap<>();
        }
        return outHeaders;
    }

    public String getCallerId() {
        return callerId;
    }
}
