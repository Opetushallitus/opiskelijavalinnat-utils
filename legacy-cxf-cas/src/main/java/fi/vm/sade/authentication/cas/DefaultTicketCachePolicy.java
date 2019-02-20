package fi.vm.sade.authentication.cas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Ticket cache policy that keeps cached ticket in user's http session context
 * (if using from spring webapp), otherwise in global (not static though) context
 * (with configurable expiration time).
 *
 * @author Antti Salonen
 */
public class DefaultTicketCachePolicy extends TicketCachePolicy {

    private static class TicketInfo {
        public final String ticket;
        public final Long loaded;
        public TicketInfo(String ticket, Long loaded) {
            this.ticket = ticket;
            this.loaded = loaded;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DefaultTicketCachePolicy.class);
    private int globalTicketsTimeToLiveSeconds = 10*60; // 10 min default
    private Map<String, TicketInfo> globalTickets = new HashMap<>();

    @Override
    protected String getTicketFromCache(String cacheKey) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        String cachedTicket = null;
        if (requestAttributes != null) {
            cachedTicket = (String) requestAttributes.getAttribute(cacheKey, RequestAttributes.SCOPE_SESSION);
        } else {
            TicketInfo ticketInfo = globalTickets.get(cacheKey);
            if (ticketInfo != null) {
                // expire?
                if (System.currentTimeMillis() - ticketInfo.loaded > globalTicketsTimeToLiveSeconds * 1000) {
                    globalTickets.remove(cacheKey);
                    log.info("expired ticket from global expiring cache, cacheKey: " + cacheKey);
                }
                else {
                    // do not return ticket to second user before 1s in order to prevent concurrent CAS validate calls with same new ticket
                    while (System.currentTimeMillis() - ticketInfo.loaded < 1000) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ignored) {}
                    }
                    cachedTicket = ticketInfo.ticket;
                }
            }
        }
        return cachedTicket;
    }


    @Override
    protected void putTicketToCache(String cacheKey, String ticket) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(cacheKey, ticket, RequestAttributes.SCOPE_SESSION);
            log.info("cached ticket to httpsession, cacheKey: "+cacheKey+", ticket: "+ticket);
        } else {
            if(ticket == null) {
                globalTickets.remove(cacheKey);
                log.info("removed ticket for cacheKey: "+cacheKey);
            } else {
                globalTickets.put(cacheKey, new TicketInfo(ticket, System.currentTimeMillis()));
                log.info("cached ticket to global expiring cache, cacheKey: "+cacheKey+", ticket: "+ticket);
            }
        }
    }

    public void setGlobalTicketsTimeToLiveSeconds(int globalTicketsTimeToLiveSeconds) {
        this.globalTicketsTimeToLiveSeconds = globalTicketsTimeToLiveSeconds;
    }
}
