package fi.vm.sade.javautils.http;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class RemoteAddressFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nop
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            chain.doFilter(new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getRemoteAddr() {
                    return HttpServletRequestUtils.getRemoteAddress(httpRequest);
                }
            }, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // nop
    }

}
