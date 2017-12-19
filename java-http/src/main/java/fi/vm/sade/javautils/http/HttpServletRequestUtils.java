package fi.vm.sade.javautils.http;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class HttpServletRequestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletRequestUtils.class);
    private static final Set<String> HARMLESS_URLS = parseHarmlessUrlsFromSystemProperty();

    private static Set<String> parseHarmlessUrlsFromSystemProperty() {
        String property = System.getProperty("fi.vm.sade.javautils.http.HttpServletRequestUtils.HARMLESS_URLS");
        if (StringUtils.isBlank(property)) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(property.split(",")));
    }

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
        String requestURI = httpServletRequest.getRequestURI();
        if (!HARMLESS_URLS.contains(requestURI)) {
            LOGGER.warn(String.format("X-Real-IP or X-Forwarded-For was not set. Are we not running behind a load balancer? Request URI is '%s'", requestURI));
        }
        return httpServletRequest.getRemoteAddr();
    }
}
