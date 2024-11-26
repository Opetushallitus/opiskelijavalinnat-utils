package fi.vm.sade.java_utils.security;

import fi.vm.sade.properties.OphProperties;
import org.apereo.cas.client.Protocol;
import org.apereo.cas.client.session.SingleSignOutFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.nio.file.Paths;

public class OpintopolkuSingleSignOutFilter implements Filter {
    private final static String WEB_URL_CAS = "web.url.cas";
    private SingleSignOutFilter singleSignOutFilter;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String userHome = System.getProperty("user.home");
        final OphProperties ophProperties = new OphProperties()
                .addFiles(Paths.get(userHome, "/oph-configuration/common.properties").toString());
        ophProperties.require(WEB_URL_CAS);
        this.singleSignOutFilter = new SingleSignOutFilter();
        this.singleSignOutFilter.setIgnoreInitConfiguration(true);
        SingleSignOutFilter.setArtifactParameterName(Protocol.CAS2.getArtifactParameterName());
        SingleSignOutFilter.setLogoutParameterName("logoutRequest");
        SingleSignOutFilter.setRelayStateParameterName("RelayState");
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
