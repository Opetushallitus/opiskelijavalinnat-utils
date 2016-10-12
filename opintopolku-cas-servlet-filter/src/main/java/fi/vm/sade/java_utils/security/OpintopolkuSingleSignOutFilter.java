package fi.vm.sade.java_utils.security;

import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.session.SingleSignOutFilter;

import javax.servlet.*;
import java.io.IOException;
import java.nio.file.Paths;

public class OpintopolkuSingleSignOutFilter implements Filter {
    private final static String WEB_URL_CAS = "web.url.cas";
    private SingleSignOutFilter singleSignOutFilter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final String userHome = System.getProperty("user.home");
        OphProperties ophProperties = new OphProperties()
                .addFiles(Paths.get(userHome, "/oph-configuration/common.properties").toString());
        this.singleSignOutFilter = new SingleSignOutFilter();
        this.singleSignOutFilter.setCasServerUrlPrefix(ophProperties.require(WEB_URL_CAS));
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
