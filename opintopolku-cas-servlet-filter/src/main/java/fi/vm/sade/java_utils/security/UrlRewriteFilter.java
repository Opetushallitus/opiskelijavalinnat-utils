package fi.vm.sade.java_utils.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites urls so that they can be handled with standard spring security cas classes.
 * Eg rewrites custom oph CasSecurityHeader as http parameter.
 *
 * @author Antti Salonen
 */
// todo: cas todo rethink - tätä ei tarvita kuin auth.mode=dev yhteydessä kehitysympäristössä !!!
public class UrlRewriteFilter implements Filter {

    public static final String ALREADY_PROCESSED = UrlRewriteFilter.class.getName()+"_alreadyProcessed";

    private final static Logger logger = LoggerFactory.getLogger(UrlRewriteFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;

            //System.out.println("\nUrlRewriteFilter.doFilter: "+request.getRequestURL().append("?").append(request.getQueryString()));

            if (!"true".equals(request.getAttribute(ALREADY_PROCESSED))) {
                String casTicketHeader = request.getHeader("CasSecurityTicket");
                String oldDeprecatedSecurity_REMOVE_username = request.getHeader("oldDeprecatedSecurity_REMOVE_username");
                String oldDeprecatedSecurity_REMOVE_authorities = request.getHeader("oldDeprecatedSecurity_REMOVE_authorities");
                //System.out.println("UrlRewriteFilter.doFilter, casTicketHeader: "+casTicketHeader+", oldDeprecatedSecurity_REMOVE_username: "+oldDeprecatedSecurity_REMOVE_username);
                if ("oldDeprecatedSecurity_REMOVE".equals(casTicketHeader)) { // todo: tukee mock autentikointia, cas todo: pois tuotannosta yms
                    Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
                    if (oldDeprecatedSecurity_REMOVE_authorities != null) {
                        for (String authority : oldDeprecatedSecurity_REMOVE_authorities.split(",")) {
                            authorities.add(new SimpleGrantedAuthority(authority));
                        }
                    }
                    PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(oldDeprecatedSecurity_REMOVE_username, oldDeprecatedSecurity_REMOVE_username, authorities);
                    logger.warn("MOCK service call, request: {}, authentication: {}", request.getRequestURL().append("?").append(request.getQueryString()), authentication);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    forward(request, servletResponse, "oldDeprecatedSecurity_REMOVE", "true"); // huom! forwardin jälkeen ei ajeta enää springin securityputkea
                    return;
                } else if (casTicketHeader != null) {
                    /* tämän jälkeen ei ajeta casfiltteriä?!?!??!
                    forward(request, servletResponse, "ticket", casTicketHeader);
                    return;
                    */
                }

            }

        }

        // if no rewrite has taken place continue as normal
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void forward(HttpServletRequest request, ServletResponse servletResponse, final String paramName, final String paramValue) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith(request.getContextPath())) {
            path = path.substring(request.getContextPath().length());
        }
        if (path.contains("?")) {
            path += "&";
        } else {
            path += "?";
        }
        path += paramName + "=" + paramValue;
        logger.debug("WARNING - UrlRewriteFilter forward to: {}", path);
        request.setAttribute(ALREADY_PROCESSED, "true");
        request.getRequestDispatcher(path).forward(request, servletResponse);
    }

    @Override
    public void destroy() {
    }

}
