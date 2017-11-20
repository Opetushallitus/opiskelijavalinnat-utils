package fi.vm.sade.javautils.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Predicate;

public class HttpServletRequestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletRequestUtils.class);
    public static String getRemoteAddress(HttpServletRequest httpServletRequest) {
        String xRealIp = httpServletRequest.getHeader("X-Real-IP");
        Predicate<String> isNotBlank = (String txt) -> txt != null && !txt.isEmpty();
        if (isNotBlank.test(xRealIp)) {
            return xRealIp;
        }
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (isNotBlank.test(xForwardedFor)) {
            if (xForwardedFor.contains(",")) {
                LOGGER.error("Could not find X-Real-IP header, but X-Forwarded-For contains multiple values: {}, " +
                        "this can cause problems", xForwardedFor);
            }
            return xForwardedFor;
        }
        LOGGER.warn("X-Real-IP or X-Forwarded-For was not set. Are we not running behind a load balancer?");
        return httpServletRequest.getRemoteAddr();
    }
}
