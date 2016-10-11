package fi.vm.sade.java_utils.security;

import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.session.SingleSignOutFilter;

import javax.servlet.*;
import java.io.IOException;

public class OpintopolkuSingleSignOutFilter implements Filter {
    private final static String WEB_URL_CAS = "web.url.cas";
    private final SingleSignOutFilter singleSignOutFilter;

    public OpintopolkuSingleSignOutFilter() {
        OphProperties ophProperties = new OphProperties();
        this.singleSignOutFilter = new SingleSignOutFilter();
        this.singleSignOutFilter.setCasServerUrlPrefix(ophProperties.getProperty(WEB_URL_CAS));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.singleSignOutFilter.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.singleSignOutFilter.doFilter(request,response,chain);
    }

    @Override
    public void destroy() {
        this.singleSignOutFilter.destroy();
    }
}
